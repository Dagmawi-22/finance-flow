package com.financeflow.wallet;

import com.financeflow.domain.Wallet;
import com.financeflow.domain.WalletRepository;
import com.financeflow.domain.WalletStatus;
import com.financeflow.user.event.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WalletProvisioningListener {

    private final WalletRepository walletRepository;

    @Value("${financeflow.wallet.default-currency}")
    private String defaultCurrency;

    @EventListener
    public void onUserCreated(UserCreatedEvent event) {
        var wallet = new Wallet();
        wallet.setUser(event.user());
        wallet.setCurrency(defaultCurrency);
        wallet.setStatus(WalletStatus.ACTIVE);
        walletRepository.save(wallet);
    }
}
