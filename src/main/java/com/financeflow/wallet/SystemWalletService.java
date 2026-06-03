package com.financeflow.wallet;

import com.financeflow.domain.Wallet;
import com.financeflow.domain.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SystemWalletService {

    private final WalletRepository walletRepository;

    @Transactional
    public Wallet requireForUpdate(String currency) {
        return walletRepository.findSystemWalletByCurrencyForUpdate(currency)
                .orElseThrow(() -> new IllegalStateException("System wallet missing for currency " + currency));
    }
}
