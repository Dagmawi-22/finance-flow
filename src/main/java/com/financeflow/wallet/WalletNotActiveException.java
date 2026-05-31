package com.financeflow.wallet;

import com.financeflow.domain.WalletStatus;

import java.util.UUID;

public class WalletNotActiveException extends RuntimeException {

    public WalletNotActiveException(UUID walletId, WalletStatus status) {
        super("Wallet " + walletId + " is not active (status: " + status + ")");
    }
}
