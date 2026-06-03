package com.financeflow.api;

import com.financeflow.wallet.WalletService;
import com.financeflow.wallet.dto.AmountRequest;
import com.financeflow.wallet.dto.TransactionHistoryResponse;
import com.financeflow.wallet.dto.TransferRequest;
import com.financeflow.wallet.dto.TransferResponse;
import com.financeflow.wallet.dto.WalletBalanceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Wallets")
@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @Operation(summary = "Get wallet balance")
    @GetMapping("/{walletId}")
    public WalletBalanceResponse getWallet(@PathVariable UUID walletId) {
        return walletService.getWallet(walletId);
    }

    @Operation(summary = "List wallet transactions")
    @GetMapping("/{walletId}/transactions")
    public TransactionHistoryResponse getTransactions(
            @PathVariable UUID walletId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return walletService.getTransactionHistory(walletId, page, size);
    }

    @Operation(summary = "Deposit funds")
    @PostMapping("/{walletId}/deposits")
    public WalletBalanceResponse deposit(
            @PathVariable UUID walletId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody AmountRequest request) {
        return walletService.deposit(walletId, request, idempotencyKey);
    }

    @Operation(summary = "Withdraw funds")
    @PostMapping("/{walletId}/withdrawals")
    public WalletBalanceResponse withdraw(
            @PathVariable UUID walletId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody AmountRequest request) {
        return walletService.withdraw(walletId, request, idempotencyKey);
    }

    @Operation(summary = "Transfer funds between wallets")
    @PostMapping("/transfers")
    public TransferResponse transfer(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {
        return walletService.transfer(request, idempotencyKey);
    }
}
