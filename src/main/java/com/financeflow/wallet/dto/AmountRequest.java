package com.financeflow.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

import java.util.Map;

public record AmountRequest(
        @Positive @Schema(example = "10000") long amount,
        @Schema(example = "Salary") String reference,
        Map<String, Object> metadata
) {
}
