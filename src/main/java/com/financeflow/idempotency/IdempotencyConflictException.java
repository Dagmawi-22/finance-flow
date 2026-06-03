package com.financeflow.idempotency;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException() {
        super("Idempotency key is already in use");
    }
}
