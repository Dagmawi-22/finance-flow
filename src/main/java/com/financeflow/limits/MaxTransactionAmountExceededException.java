package com.financeflow.limits;

public class MaxTransactionAmountExceededException extends RuntimeException {

    public MaxTransactionAmountExceededException(long amount, long maxAllowed) {
        super("Amount " + amount + " exceeds max per transaction of " + maxAllowed);
    }
}
