package com.financeflow.wallet.dto;

import com.financeflow.domain.LedgerDirection;
import com.financeflow.domain.TransactionStatus;
import com.financeflow.domain.TransactionType;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class TransactionHistoryItem {
    UUID transactionId;
    TransactionType type;
    TransactionStatus status;
    LedgerDirection direction;
    long amount;
    String currency;
    String reference;
    Instant createdAt;
    Instant completedAt;
}
