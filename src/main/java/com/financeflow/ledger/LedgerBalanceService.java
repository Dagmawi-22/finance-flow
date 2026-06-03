package com.financeflow.ledger;

import com.financeflow.domain.LedgerDirection;
import com.financeflow.domain.LedgerEntryRepository;
import com.financeflow.domain.Wallet;
import com.financeflow.wallet.InsufficientFundsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Service
@RequiredArgsConstructor
public class LedgerBalanceService {

    private final LedgerEntryRepository ledgerEntryRepository;

    public long applyEntry(Wallet wallet, LedgerDirection direction, long amount) {
        var newBalance = wallet.getBalance() + signedDelta(direction, amount);
        if (newBalance < 0) {
            throw new InsufficientFundsException();
        }
        wallet.setBalance(newBalance);
        return newBalance;
    }

    public long computeBalanceFromLedger(UUID walletId) {
        return ledgerEntryRepository.computeBalanceFromCompletedEntries(walletId);
    }

    public void assertConsistentWithLedger(Wallet wallet) {
        var computed = computeBalanceFromLedger(wallet.getId());
        if (wallet.getBalance() != computed) {
            throw new BalanceReconciliationException(wallet.getId(), wallet.getBalance(), computed);
        }
    }

    static long signedDelta(LedgerDirection direction, long amount) {
        return direction == LedgerDirection.CREDIT ? amount : -amount;
    }
}
