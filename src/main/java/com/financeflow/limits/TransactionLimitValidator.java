package com.financeflow.limits;

import com.financeflow.domain.LedgerEntryRepository;
import com.financeflow.domain.Wallet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
public class TransactionLimitValidator {

    private final TransactionLimitProperties limits;
    private final LedgerEntryRepository ledgerEntryRepository;

    public void validateAmount(long amount) {
        if (amount > limits.maxPerTransaction()) {
            throw new MaxTransactionAmountExceededException(amount, limits.maxPerTransaction());
        }
    }

    public void validateOutgoing(Wallet wallet, long amount) {
        validateAmount(amount);

        var startOfDay = LocalDate.now(ZoneOffset.UTC)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);
        var usedToday = ledgerEntryRepository.sumOutgoingDebitsSince(wallet.getId(), startOfDay);

        if (usedToday + amount > limits.maxDailyOutgoing()) {
            throw new DailyLimitExceededException(amount, limits.maxDailyOutgoing(), usedToday);
        }
    }
}
