package com.financeflow.ledger;

import com.financeflow.domain.LedgerDirection;
import com.financeflow.domain.LedgerEntry;
import com.financeflow.domain.LedgerEntryRepository;
import com.financeflow.domain.Transaction;
import com.financeflow.domain.TransactionRepository;
import com.financeflow.domain.TransactionStatus;
import com.financeflow.domain.TransactionType;
import com.financeflow.domain.Wallet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final LedgerBalanceService ledgerBalanceService;

    public LedgerResult recordDeposit(Wallet wallet, long amount) {
        var transaction = createCompletedTransaction(TransactionType.DEPOSIT);
        var balance = postEntry(transaction, wallet, LedgerDirection.CREDIT, amount);
        return new LedgerResult(transaction.getId(), balance);
    }

    public LedgerResult recordWithdrawal(Wallet wallet, long amount) {
        var transaction = createCompletedTransaction(TransactionType.WITHDRAWAL);
        var balance = postEntry(transaction, wallet, LedgerDirection.DEBIT, amount);
        return new LedgerResult(transaction.getId(), balance);
    }

    public TransferLedgerResult recordTransfer(Wallet from, Wallet to, long amount) {
        var transaction = createCompletedTransaction(TransactionType.TRANSFER);
        var fromBalance = postEntry(transaction, from, LedgerDirection.DEBIT, amount);
        var toBalance = postEntry(transaction, to, LedgerDirection.CREDIT, amount);
        return new TransferLedgerResult(transaction.getId(), fromBalance, toBalance);
    }

    private Transaction createCompletedTransaction(TransactionType type) {
        var now = Instant.now();
        var transaction = new Transaction();
        transaction.setType(type);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCompletedAt(now);
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
}
