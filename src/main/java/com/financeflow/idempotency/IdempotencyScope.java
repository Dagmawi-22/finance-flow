package com.financeflow.idempotency;

public record IdempotencyScope(String value) {

    public static IdempotencyScope deposit(java.util.UUID walletId) {
        return new IdempotencyScope("deposit:" + walletId);
    }

    public static IdempotencyScope withdrawal(java.util.UUID walletId) {
        return new IdempotencyScope("withdrawal:" + walletId);
    }

    public static IdempotencyScope transfer(java.util.UUID fromWalletId, java.util.UUID toWalletId) {
        return new IdempotencyScope("transfer:" + fromWalletId + ":" + toWalletId);
    }

    String cacheKey(String idempotencyKey) {
        return "idempotency:" + value + ":" + idempotencyKey;
    }

    String lockKey(String idempotencyKey) {
        return "idempotency:lock:" + value + ":" + idempotencyKey;
    }
}
