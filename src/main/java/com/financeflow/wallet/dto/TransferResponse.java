package com.financeflow.wallet.dto;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class TransferResponse {
    UUID fromWalletId;
    UUID toWalletId;
    long amount;
    long fromBalance;
    long toBalance;
}
