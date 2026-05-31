package com.financeflow.api;

import com.financeflow.wallet.WalletService;
import com.financeflow.wallet.dto.AmountRequest;
import com.financeflow.wallet.dto.TransferRequest;
import com.financeflow.wallet.dto.TransferResponse;
import com.financeflow.wallet.dto.WalletBalanceResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/{walletId}")
    public WalletBalanceResponse getWallet(@PathVariable UUID walletId) {
        return walletService.getWallet(walletId);
    }

    @PostMapping("/{walletId}/deposits")
    public WalletBalanceResponse deposit(
            @PathVariable UUID walletId,
            @Valid @RequestBody AmountRequest request) {
        return walletService.deposit(walletId, request);
    }

    @PostMapping("/{walletId}/withdrawals")
    public WalletBalanceResponse withdraw(
            @PathVariable UUID walletId,
            @Valid @RequestBody AmountRequest request) {
        return walletService.withdraw(walletId, request);
    }

    @PostMapping("/transfers")
    public TransferResponse transfer(@Valid @RequestBody TransferRequest request) {
        return walletService.transfer(request);
    }
}
