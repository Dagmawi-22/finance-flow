package com.financeflow.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "financeflow.jwt")
public record JwtProperties(
        String secret,
        int expirationHours
) {
}
