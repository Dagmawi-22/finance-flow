package com.financeflow.idempotency;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "financeflow.idempotency")
public class IdempotencyProperties {

    private int ttlHours = 24;
    private int lockSeconds = 30;

    public Duration ttl() {
        return Duration.ofHours(ttlHours);
    }

    public Duration lockTtl() {
        return Duration.ofSeconds(lockSeconds);
    }
}
