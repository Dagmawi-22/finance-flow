package com.financeflow.ledger;

import java.util.UUID;

public class BalanceReconciliationException extends RuntimeException {

    public BalanceReconciliationException(UUID walletId, long materializedBalance, long ledgerBalance) {
        super("Wallet %s balance mismatch: materialized=%d, ledger=%d"
                .formatted(walletId, materializedBalance, ledgerBalance));
    }
}
