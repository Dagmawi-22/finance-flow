package com.financeflow.wallet;

import com.financeflow.domain.Wallet;
import com.financeflow.domain.WalletRepository;
import com.financeflow.domain.WalletStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SystemWalletProvisioner implements ApplicationRunner {

    private final WalletRepository walletRepository;

    @Value("${financeflow.wallet.default-currency}")
    private String defaultCurrency;

    @Override
    public void run(ApplicationArguments args) {
        walletRepository.findBySystemTrueAndCurrency(defaultCurrency)
                .orElseGet(() -> {
                    var wallet = new Wallet();
                    wallet.setSystem(true);
                    wallet.setCurrency(defaultCurrency);
                    wallet.setStatus(WalletStatus.ACTIVE);
                    wallet.setLabel("SYSTEM");
                    return walletRepository.save(wallet);
                });
    }
}
