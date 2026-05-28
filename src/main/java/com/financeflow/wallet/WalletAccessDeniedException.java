package com.financeflow.wallet;

import java.util.UUID;

public class WalletAccessDeniedException extends RuntimeException {

    public WalletAccessDeniedException(UUID walletId) {
        super("You do not have access to wallet " + walletId);
    }
}
