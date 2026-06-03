package com.financeflow.ledger;

import com.financeflow.domain.LedgerDirection;
import com.financeflow.domain.LedgerEntry;
import com.financeflow.domain.LedgerEntryRepository;
import com.financeflow.domain.Transaction;
import com.financeflow.domain.TransactionRepository;
import com.financeflow.domain.TransactionStatus;
import com.financeflow.domain.TransactionType;
import com.financeflow.domain.Wallet;
import com.financeflow.wallet.SystemWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final LedgerBalanceService ledgerBalanceService;
    private final SystemWalletService systemWalletService;

    public LedgerResult recordDeposit(Wallet wallet, long amount, TransactionContext context) {
        var system = systemWalletService.requireForUpdate(wallet.getCurrency());
        var transaction = createCompletedTransaction(TransactionType.DEPOSIT, context);
        postEntry(transaction, system, LedgerDirection.DEBIT, amount);
        var balance = postEntry(transaction, wallet, LedgerDirection.CREDIT, amount);
        assertBalancedTransaction(transaction);
        return new LedgerResult(transaction.getId(), balance);
    }

    public LedgerResult recordWithdrawal(Wallet wallet, long amount, TransactionContext context) {
        var system = systemWalletService.requireForUpdate(wallet.getCurrency());
        var transaction = createCompletedTransaction(TransactionType.WITHDRAWAL, context);
        var balance = postEntry(transaction, wallet, LedgerDirection.DEBIT, amount);
        postEntry(transaction, system, LedgerDirection.CREDIT, amount);
        assertBalancedTransaction(transaction);
        return new LedgerResult(transaction.getId(), balance);
    }

    public TransferLedgerResult recordTransfer(Wallet from, Wallet to, long amount, TransactionContext context) {
        var transaction = createCompletedTransaction(TransactionType.TRANSFER, context);
        var fromBalance = postEntry(transaction, from, LedgerDirection.DEBIT, amount);
        var toBalance = postEntry(transaction, to, LedgerDirection.CREDIT, amount);
        assertBalancedTransaction(transaction);
        return new TransferLedgerResult(transaction.getId(), fromBalance, toBalance);
    }

    private Transaction createCompletedTransaction(TransactionType type, TransactionContext context) {
        var now = Instant.now();
        var transaction = new Transaction();
        transaction.setType(type);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCompletedAt(now);
        if (context != null) {
            transaction.setIdempotencyKey(context.idempotencyKey());
            transaction.setReference(context.reference());
            if (context.metadata() != null && !context.metadata().isEmpty()) {
                transaction.setMetadata(new HashMap<>(context.metadata()));
            }
        }
        return transactionRepository.save(transaction);
    }

    private long postEntry(Transaction transaction, Wallet wallet, LedgerDirection direction, long amount) {
        var entry = new LedgerEntry();
        entry.setTransaction(transaction);
        entry.setWallet(wallet);
        entry.setDirection(direction);
        entry.setAmount(amount);
        entry.setCurrency(wallet.getCurrency());
        ledgerEntryRepository.save(entry);
        return ledgerBalanceService.applyEntry(wallet, direction, amount);
    }

    private void assertBalancedTransaction(Transaction transaction) {
        var entries = ledgerEntryRepository.findByTransaction_Id(transaction.getId());
        long debits = entries.stream()
                .filter(entry -> entry.getDirection() == LedgerDirection.DEBIT)
                .mapToLong(LedgerEntry::getAmount)
                .sum();
        long credits = entries.stream()
                .filter(entry -> entry.getDirection() == LedgerDirection.CREDIT)
                .mapToLong(LedgerEntry::getAmount)
                .sum();
        if (debits != credits) {
            throw new UnbalancedTransactionException(transaction.getId(), debits, credits);
        }
    }
}
