package com.financeflow.ledger;

import com.financeflow.domain.LedgerDirection;
import com.financeflow.domain.LedgerEntryRepository;
import com.financeflow.domain.TransactionRepository;
import com.financeflow.domain.TransactionStatus;
import com.financeflow.domain.TransactionType;
import com.financeflow.support.AuthTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static com.financeflow.support.AuthTestSupport.bearerToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@EnabledIf("com.financeflow.support.DockerSupport#isAvailable")
@Transactional
class LedgerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("financeflow_test")
            .withUsername("financeflow")
            .withPassword("financeflow");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    LedgerEntryRepository ledgerEntryRepository;

    String walletId;
    String token;

    @BeforeEach
    void setUp() throws Exception {
        var result = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "ledger-user@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        walletId = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.wallet.id");
        token = AuthTestSupport.login(mockMvc, "ledger-user@example.com", "password123");
    }

    @Test
    void depositCreatesTransactionAndLedgerEntry() throws Exception {
        mockMvc.perform(post("/api/v1/wallets/{id}/deposits", walletId)
                        .with(bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": 5000 }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").isNotEmpty());

        assertThat(transactionRepository.count()).isEqualTo(1);
        var transaction = transactionRepository.findAll().getFirst();
        assertThat(transaction.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.COMPLETED);

        assertThat(ledgerEntryRepository.count()).isEqualTo(1);
        var entry = ledgerEntryRepository.findAll().getFirst();
        assertThat(entry.getDirection()).isEqualTo(LedgerDirection.CREDIT);
        assertThat(entry.getAmount()).isEqualTo(5000);
        assertThat(entry.getWallet().getId()).isEqualTo(UUID.fromString(walletId));
    }

    @Test
    void transferCreatesOneTransactionAndTwoLedgerEntries() throws Exception {
        var bobWallet = createSecondWallet();

        mockMvc.perform(post("/api/v1/wallets/{id}/deposits", walletId)
                        .with(bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": 10000 }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/wallets/transfers")
                        .with(bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fromWalletId": "%s",
                                  "toWalletId": "%s",
                                  "amount": 3000
                                }
                                """.formatted(walletId, bobWallet)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").isNotEmpty());

        assertThat(transactionRepository.count()).isEqualTo(2);
        var transferTx = transactionRepository.findAll().stream()
                .filter(t -> t.getType() == TransactionType.TRANSFER)
                .findFirst()
                .orElseThrow();
        assertThat(transferTx.getStatus()).isEqualTo(TransactionStatus.COMPLETED);

        var transferEntries = ledgerEntryRepository.findAll().stream()
                .filter(e -> e.getTransaction().getId().equals(transferTx.getId()))
                .toList();
        assertThat(transferEntries).hasSize(2);
        assertThat(transferEntries.stream().map(e -> e.getDirection()).toList())
                .containsExactlyInAnyOrder(LedgerDirection.DEBIT, LedgerDirection.CREDIT);
    }

    private String createSecondWallet() throws Exception {
        var result = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "ledger-bob@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.wallet.id");
    }
}
