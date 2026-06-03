package com.financeflow.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    @Query("""
            SELECT COALESCE(SUM(le.amount), 0)
            FROM LedgerEntry le
            JOIN le.transaction t
            WHERE le.wallet.id = :walletId
              AND le.direction = com.financeflow.domain.LedgerDirection.DEBIT
              AND t.status = com.financeflow.domain.TransactionStatus.COMPLETED
              AND t.type IN (
                  com.financeflow.domain.TransactionType.WITHDRAWAL,
                  com.financeflow.domain.TransactionType.TRANSFER
              )
              AND le.createdAt >= :since
            """)
    long sumOutgoingDebitsSince(@Param("walletId") UUID walletId, @Param("since") Instant since);

    @Query("""
            SELECT COALESCE(SUM(
                CASE WHEN le.direction = com.financeflow.domain.LedgerDirection.CREDIT
                     THEN le.amount
                     ELSE -le.amount
                END
            ), 0)
            FROM LedgerEntry le
            JOIN le.transaction t
            WHERE le.wallet.id = :walletId
              AND t.status = com.financeflow.domain.TransactionStatus.COMPLETED
            """)
    long computeBalanceFromCompletedEntries(@Param("walletId") UUID walletId);
}
