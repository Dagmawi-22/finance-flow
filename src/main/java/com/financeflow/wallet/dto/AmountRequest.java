package com.financeflow.wallet.dto;

import jakarta.validation.constraints.Positive;

public record AmountRequest(
        @Positive Long amount
) {
}
