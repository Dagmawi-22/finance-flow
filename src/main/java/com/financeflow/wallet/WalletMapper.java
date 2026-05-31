package com.financeflow.wallet;

import com.financeflow.domain.Wallet;
import com.financeflow.wallet.dto.WalletBalanceResponse;
import org.springframework.stereotype.Component;

@Component
public class WalletMapper {

    public WalletBalanceResponse toBalanceResponse(Wallet wallet) {
        return WalletBalanceResponse.builder()
                .id(wallet.getId())
                .currency(wallet.getCurrency())
                .balance(wallet.getBalance())
                .status(wallet.getStatus())
                .build();
    }
}
