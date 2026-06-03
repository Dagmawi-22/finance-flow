package com.financeflow.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Map;
import java.util.UUID;

public record TransferRequest(
        @NotNull UUID fromWalletId,
        @NotNull UUID toWalletId,
        @Positive @Schema(example = "5000") long amount,
        @Schema(example = "Rent") String reference,
        Map<String, Object> metadata
) {
}
