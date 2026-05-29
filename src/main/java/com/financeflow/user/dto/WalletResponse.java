package com.financeflow.user.dto;

import com.financeflow.domain.WalletStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class WalletResponse {
    UUID id;
    String currency;
    WalletStatus status;
    Instant createdAt;
}
