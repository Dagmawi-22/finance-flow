package com.financeflow.user;

import com.financeflow.domain.User;
import com.financeflow.domain.Wallet;
import com.financeflow.user.dto.UserResponse;
import com.financeflow.user.dto.WalletResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user, Wallet wallet) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .wallet(toWalletResponse(wallet))
                .build();
    }

    private WalletResponse toWalletResponse(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .currency(wallet.getCurrency())
                .balance(wallet.getBalance())
                .status(wallet.getStatus())
                .createdAt(wallet.getCreatedAt())
                .build();
    }
}
