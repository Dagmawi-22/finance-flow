package com.financeflow.ledger;

import com.financeflow.domain.LedgerDirection;
import com.financeflow.domain.LedgerEntryRepository;
import com.financeflow.domain.Wallet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerBalanceServiceTest {

    @Mock
    LedgerEntryRepository ledgerEntryRepository;

    @InjectMocks
    LedgerBalanceService ledgerBalanceService;

    @Test
    void signedDeltaCreditsIncreaseBalanceAndDebitsDecrease() {
        assertThat(LedgerBalanceService.signedDelta(LedgerDirection.CREDIT, 500)).isEqualTo(500);
        assertThat(LedgerBalanceService.signedDelta(LedgerDirection.DEBIT, 500)).isEqualTo(-500);
    }

    @Test
    void assertConsistentWithLedgerThrowsOnMismatch() {
        var wallet = new Wallet();
        wallet.setId(UUID.randomUUID());
        wallet.setBalance(100);
        when(ledgerEntryRepository.computeBalanceFromCompletedEntries(wallet.getId())).thenReturn(50L);

        assertThatThrownBy(() -> ledgerBalanceService.assertConsistentWithLedger(wallet))
                .isInstanceOf(BalanceReconciliationException.class);
    }
}
