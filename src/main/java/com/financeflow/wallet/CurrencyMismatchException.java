package com.financeflow.wallet;

public class CurrencyMismatchException extends RuntimeException {

    public CurrencyMismatchException(String fromCurrency, String toCurrency) {
        super("Currency mismatch: " + fromCurrency + " vs " + toCurrency);
    }
}
