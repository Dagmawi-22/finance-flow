package com.financeflow.wallet.dto;

import com.financeflow.domain.WalletStatus;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class WalletBalanceResponse {
    UUID id;
    String currency;
    long balance;
    WalletStatus status;
}
