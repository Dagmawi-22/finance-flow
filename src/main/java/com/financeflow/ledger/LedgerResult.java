package com.financeflow.ledger;

import java.util.UUID;

public record LedgerResult(
        UUID transactionId,
        long balance
) {
}
