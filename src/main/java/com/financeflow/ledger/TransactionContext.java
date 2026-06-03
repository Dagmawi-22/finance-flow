package com.financeflow.ledger;

import java.util.Map;

public record TransactionContext(
        String idempotencyKey,
        String reference,
        Map<String, Object> metadata
) {
    public static TransactionContext empty() {
        return new TransactionContext(null, null, null);
    }
}
