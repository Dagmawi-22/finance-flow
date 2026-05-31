package com.financeflow.wallet;

import com.financeflow.domain.WalletRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@EnabledIf("com.financeflow.support.DockerSupport#isAvailable")
@Transactional
class WalletOperationsIntegrationTest {

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
    WalletRepository walletRepository;

    String aliceWalletId;
    String bobWalletId;

    @BeforeEach
    void setUpUsers() throws Exception {
        aliceWalletId = createUserAndGetWalletId("alice-ops@example.com");
        bobWalletId = createUserAndGetWalletId("bob-ops@example.com");
    }

    @Test
    void depositIncreasesBalance() throws Exception {
        mockMvc.perform(post("/api/v1/wallets/{id}/deposits", aliceWalletId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": 10000 }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(10000));

        mockMvc.perform(get("/api/v1/wallets/{id}", aliceWalletId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(10000));
    }

    @Test
    void withdrawDecreasesBalance() throws Exception {
        deposit(aliceWalletId, 5000);

        mockMvc.perform(post("/api/v1/wallets/{id}/withdrawals", aliceWalletId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": 2000 }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(3000));
    }

    @Test
    void withdrawWithInsufficientFundsReturns422() throws Exception {
        deposit(aliceWalletId, 1000);

        mockMvc.perform(post("/api/v1/wallets/{id}/withdrawals", aliceWalletId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": 2000 }
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void transferMovesFundsBetweenWallets() throws Exception {
        deposit(aliceWalletId, 10000);

        mockMvc.perform(post("/api/v1/wallets/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fromWalletId": "%s",
                                  "toWalletId": "%s",
                                  "amount": 3500
                                }
                                """.formatted(aliceWalletId, bobWalletId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromBalance").value(6500))
                .andExpect(jsonPath("$.toBalance").value(3500));

        assertThat(walletRepository.findById(java.util.UUID.fromString(aliceWalletId)).orElseThrow().getBalance())
                .isEqualTo(6500);
        assertThat(walletRepository.findById(java.util.UUID.fromString(bobWalletId)).orElseThrow().getBalance())
                .isEqualTo(3500);
    }

    @Test
    void transferToSameWalletReturns400() throws Exception {
        deposit(aliceWalletId, 1000);

        mockMvc.perform(post("/api/v1/wallets/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fromWalletId": "%s",
                                  "toWalletId": "%s",
                                  "amount": 500
                                }
                                """.formatted(aliceWalletId, aliceWalletId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownWalletReturns404() throws Exception {
        var missing = "00000000-0000-0000-0000-000000000099";

        mockMvc.perform(post("/api/v1/wallets/{id}/deposits", missing)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": 100 }
                                """))
                .andExpect(status().isNotFound());
    }

    private String createUserAndGetWalletId(String email) throws Exception {
        var result = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "password123"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();

        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.wallet.id");
    }

    private void deposit(String walletId, long amount) throws Exception {
        mockMvc.perform(post("/api/v1/wallets/{id}/deposits", walletId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": %d }
                                """.formatted(amount)))
                .andExpect(status().isOk());
    }
}
