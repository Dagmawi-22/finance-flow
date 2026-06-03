package com.financeflow.ledger;

import java.util.UUID;

public class UnbalancedTransactionException extends RuntimeException {

    public UnbalancedTransactionException(UUID transactionId, long debits, long credits) {
        super("Transaction %s is unbalanced: debits=%d, credits=%d".formatted(transactionId, debits, credits));
    }
}
