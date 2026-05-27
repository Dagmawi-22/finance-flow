package com.financeflow;

import com.financeflow.auth.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class FinanceFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceFlowApplication.class, args);
    }
}
