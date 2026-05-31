package com.financeflow.limits;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "financeflow.limits")
public record TransactionLimitProperties(
        long maxPerTransaction,
        long maxDailyOutgoing
) {
}
