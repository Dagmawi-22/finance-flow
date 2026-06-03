package com.financeflow.wallet;

import com.financeflow.domain.WalletRepository;
import com.financeflow.domain.WalletStatus;
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

import static com.financeflow.support.AuthTestSupport.bearerToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@EnabledIf("com.financeflow.support.DockerSupport#isAvailable")
@Transactional
class WalletStatusIntegrationTest {

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

    String walletId;
    String token;

    @BeforeEach
    void setUp() throws Exception {
        var result = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "frozen-user@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        walletId = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.wallet.id");
        token = AuthTestSupport.login(mockMvc, "frozen-user@example.com", "password123");
    }

    @Test
    void depositOnFrozenWalletReturns409() throws Exception {
        var wallet = walletRepository.findById(java.util.UUID.fromString(walletId)).orElseThrow();
        wallet.setStatus(WalletStatus.FROZEN);
        walletRepository.saveAndFlush(wallet);

        mockMvc.perform(post("/api/v1/wallets/{id}/deposits", walletId)
                        .with(bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": 1000 }
                                """))
                .andExpect(status().isConflict());
    }
}
