package com.financeflow.ledger;

import com.financeflow.domain.LedgerDirection;
import com.financeflow.domain.LedgerEntry;
import com.financeflow.domain.LedgerEntryRepository;
import com.financeflow.domain.Transaction;
import com.financeflow.domain.TransactionRepository;
import com.financeflow.domain.TransactionStatus;
import com.financeflow.domain.TransactionType;
import com.financeflow.domain.Wallet;
import com.financeflow.wallet.InsufficientFundsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public LedgerResult recordDeposit(Wallet wallet, long amount) {
        var transaction = createCompletedTransaction(TransactionType.DEPOSIT);
        postEntry(transaction, wallet, LedgerDirection.CREDIT, amount);
        wallet.setBalance(wallet.getBalance() + amount);
        return new LedgerResult(transaction.getId(), wallet.getBalance());
    }

    public LedgerResult recordWithdrawal(Wallet wallet, long amount) {
        ensureSufficientBalance(wallet, amount);
        var transaction = createCompletedTransaction(TransactionType.WITHDRAWAL);
        postEntry(transaction, wallet, LedgerDirection.DEBIT, amount);
        wallet.setBalance(wallet.getBalance() - amount);
        return new LedgerResult(transaction.getId(), wallet.getBalance());
    }

    public TransferLedgerResult recordTransfer(Wallet from, Wallet to, long amount) {
        ensureSufficientBalance(from, amount);
        var transaction = createCompletedTransaction(TransactionType.TRANSFER);
        postEntry(transaction, from, LedgerDirection.DEBIT, amount);
        postEntry(transaction, to, LedgerDirection.CREDIT, amount);
        from.setBalance(from.getBalance() - amount);
        to.setBalance(to.getBalance() + amount);
        return new TransferLedgerResult(transaction.getId(), from.getBalance(), to.getBalance());
    }

    private Transaction createCompletedTransaction(TransactionType type) {
        var now = Instant.now();
        var transaction = new Transaction();
        transaction.setType(type);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCompletedAt(now);
        return transactionRepository.save(transaction);
    }

    private void postEntry(Transaction transaction, Wallet wallet, LedgerDirection direction, long amount) {
        var entry = new LedgerEntry();
        entry.setTransaction(transaction);
        entry.setWallet(wallet);
        entry.setDirection(direction);
        entry.setAmount(amount);
        entry.setCurrency(wallet.getCurrency());
        ledgerEntryRepository.save(entry);
    }

    private void ensureSufficientBalance(Wallet wallet, long amount) {
        if (wallet.getBalance() < amount) {
            throw new InsufficientFundsException();
        }
    }
}
