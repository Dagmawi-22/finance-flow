package com.financeflow.limits;

public class DailyLimitExceededException extends RuntimeException {

    public DailyLimitExceededException(long amount, long maxDaily, long alreadyUsed) {
        super("Amount " + amount + " would exceed daily outgoing limit of " + maxDaily
                + " (already used " + alreadyUsed + " today)");
    }
}
