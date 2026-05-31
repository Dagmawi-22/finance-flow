package com.financeflow.ledger;

import java.util.UUID;

public record TransferLedgerResult(
        UUID transactionId,
        long fromBalance,
        long toBalance
) {
}
