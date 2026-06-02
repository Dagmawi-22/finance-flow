package com.financeflow.wallet;

import com.financeflow.auth.CurrentUser;
import com.financeflow.domain.Wallet;
import com.financeflow.domain.WalletRepository;
import com.financeflow.domain.WalletStatus;
import com.financeflow.ledger.LedgerService;
import com.financeflow.limits.TransactionLimitValidator;
import com.financeflow.wallet.dto.AmountRequest;
import com.financeflow.wallet.dto.TransferRequest;
import com.financeflow.wallet.dto.TransferResponse;
import com.financeflow.wallet.dto.WalletBalanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletMapper walletMapper;
    private final CurrentUser currentUser;
    private final LedgerService ledgerService;
    private final TransactionLimitValidator transactionLimitValidator;

    @Transactional(readOnly = true)
    public WalletBalanceResponse getWallet(UUID walletId) {
        return walletMapper.toBalanceResponse(requireOwnedWallet(walletId));
    }

    @Transactional
    public WalletBalanceResponse deposit(UUID walletId, AmountRequest request) {
        var wallet = requireOwnedActiveWalletForUpdate(walletId);
        transactionLimitValidator.validateAmount(request.amount());
        var result = ledgerService.recordDeposit(wallet, request.amount());
        return walletMapper.toBalanceResponse(wallet, result.transactionId());
    }

    @Transactional
    public WalletBalanceResponse withdraw(UUID walletId, AmountRequest request) {
        var wallet = requireOwnedActiveWalletForUpdate(walletId);
        transactionLimitValidator.validateOutgoing(wallet, request.amount());
        var result = ledgerService.recordWithdrawal(wallet, request.amount());
        return walletMapper.toBalanceResponse(wallet, result.transactionId());
    }

    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        if (request.fromWalletId().equals(request.toWalletId())) {
            throw new InvalidTransferException("Cannot transfer to the same wallet");
        }

        var userId = currentUser.requireId();

        var firstId = request.fromWalletId().compareTo(request.toWalletId()) < 0
                ? request.fromWalletId()
                : request.toWalletId();
        var secondId = firstId.equals(request.fromWalletId())
                ? request.toWalletId()
                : request.fromWalletId();

        var first = requireActiveWalletForUpdate(firstId);
        var second = requireActiveWalletForUpdate(secondId);

        var from = first.getId().equals(request.fromWalletId()) ? first : second;
        var to = first.getId().equals(request.toWalletId()) ? first : second;

        requireOwnership(from, userId);

        if (!from.getCurrency().equals(to.getCurrency())) {
            throw new CurrencyMismatchException(from.getCurrency(), to.getCurrency());
        }

        transactionLimitValidator.validateOutgoing(from, request.amount());
        var result = ledgerService.recordTransfer(from, to, request.amount());

        return TransferResponse.builder()
                .transactionId(result.transactionId())
                .fromWalletId(from.getId())
                .toWalletId(to.getId())
                .amount(request.amount())
                .fromBalance(result.fromBalance())
                .toBalance(result.toBalance())
                .build();
    }

    private Wallet requireOwnedWallet(UUID walletId) {
        var wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));
        requireOwnership(wallet, currentUser.requireId());
        return wallet;
    }

    private Wallet requireOwnedActiveWalletForUpdate(UUID walletId) {
        var wallet = requireActiveWalletForUpdate(walletId);
        requireOwnership(wallet, currentUser.requireId());
        return wallet;
    }

    private Wallet requireActiveWalletForUpdate(UUID walletId) {
        var wallet = walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new WalletNotActiveException(walletId, wallet.getStatus());
        }
        return wallet;
    }

    private void requireOwnership(Wallet wallet, UUID userId) {
        if (!wallet.getUser().getId().equals(userId)) {
            throw new WalletAccessDeniedException(wallet.getId());
        }
    }
}
