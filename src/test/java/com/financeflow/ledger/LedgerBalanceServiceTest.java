package com.financeflow.ledger;

import com.financeflow.domain.LedgerDirection;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LedgerBalanceServiceTest {

    @Test
    void signedDeltaCreditsIncreaseBalanceAndDebitsDecrease() {
        assertThat(LedgerBalanceService.signedDelta(LedgerDirection.CREDIT, 500)).isEqualTo(500);
        assertThat(LedgerBalanceService.signedDelta(LedgerDirection.DEBIT, 500)).isEqualTo(-500);
    }
}
