package com.financeflow.wallet;

import com.financeflow.auth.CurrentUser;
import com.financeflow.domain.LedgerEntry;
import com.financeflow.domain.LedgerEntryRepository;
import com.financeflow.domain.Wallet;
import com.financeflow.domain.WalletRepository;
import com.financeflow.domain.WalletStatus;
import com.financeflow.idempotency.IdempotencyScope;
import com.financeflow.idempotency.IdempotencyService;
import com.financeflow.ledger.LedgerService;
import com.financeflow.ledger.TransactionContext;
import com.financeflow.limits.TransactionLimitValidator;
import com.financeflow.wallet.dto.AmountRequest;
import com.financeflow.wallet.dto.TransactionHistoryItem;
import com.financeflow.wallet.dto.TransactionHistoryResponse;
import com.financeflow.wallet.dto.TransferRequest;
import com.financeflow.wallet.dto.TransferResponse;
import com.financeflow.wallet.dto.WalletBalanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final WalletMapper walletMapper;
    private final CurrentUser currentUser;
    private final LedgerService ledgerService;
    private final TransactionLimitValidator transactionLimitValidator;
    private final IdempotencyService idempotencyService;

    @Transactional(readOnly = true)
    public WalletBalanceResponse getWallet(UUID walletId) {
        return walletMapper.toBalanceResponse(requireOwnedWallet(walletId));
    }

    @Transactional(readOnly = true)
    public TransactionHistoryResponse getTransactionHistory(UUID walletId, int page, int size) {
        requireOwnedWallet(walletId);
        var pageable = PageRequest.of(page, size);
        var entries = ledgerEntryRepository.findByWalletIdOrderByTransactionCreatedAtDesc(walletId, pageable);
        var items = entries.getContent().stream()
                .map(this::toHistoryItem)
                .toList();
        return TransactionHistoryResponse.builder()
                .items(items)
                .page(entries.getNumber())
                .size(entries.getSize())
                .totalElements(entries.getTotalElements())
                .totalPages(entries.getTotalPages())
                .build();
    }

    @Transactional
    public WalletBalanceResponse deposit(UUID walletId, AmountRequest request, String idempotencyKey) {
        return idempotencyService.execute(
                idempotencyKey,
                IdempotencyScope.deposit(walletId),
                WalletBalanceResponse.class,
                () -> {
                    var response = performDeposit(walletId, request, idempotencyKey);
                    idempotencyService.attachResponse(response.getTransactionId(), response);
                    return response;
                });
    }

    @Transactional
    public WalletBalanceResponse withdraw(UUID walletId, AmountRequest request, String idempotencyKey) {
        return idempotencyService.execute(
                idempotencyKey,
                IdempotencyScope.withdrawal(walletId),
                WalletBalanceResponse.class,
                () -> {
                    var response = performWithdrawal(walletId, request, idempotencyKey);
                    idempotencyService.attachResponse(response.getTransactionId(), response);
                    return response;
                });
    }

    @Transactional
    public TransferResponse transfer(TransferRequest request, String idempotencyKey) {
        return idempotencyService.execute(
                idempotencyKey,
                IdempotencyScope.transfer(request.fromWalletId(), request.toWalletId()),
                TransferResponse.class,
                () -> {
                    var response = performTransfer(request, idempotencyKey);
                    idempotencyService.attachResponse(response.getTransactionId(), response);
                    return response;
                });
    }

    private WalletBalanceResponse performDeposit(UUID walletId, AmountRequest request, String idempotencyKey) {
        var wallet = requireOwnedActiveWalletForUpdate(walletId);
        transactionLimitValidator.validateAmount(request.amount());
        var context = toContext(idempotencyKey, request.reference(), request.metadata());
        var result = ledgerService.recordDeposit(wallet, request.amount(), context);
        return walletMapper.toBalanceResponse(wallet, result.transactionId());
    }

    private WalletBalanceResponse performWithdrawal(UUID walletId, AmountRequest request, String idempotencyKey) {
        var wallet = requireOwnedActiveWalletForUpdate(walletId);
        transactionLimitValidator.validateOutgoing(wallet, request.amount());
        var context = toContext(idempotencyKey, request.reference(), request.metadata());
        var result = ledgerService.recordWithdrawal(wallet, request.amount(), context);
        return walletMapper.toBalanceResponse(wallet, result.transactionId());
    }

    private TransferResponse performTransfer(TransferRequest request, String idempotencyKey) {
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
        var context = toContext(idempotencyKey, request.reference(), request.metadata());
        var result = ledgerService.recordTransfer(from, to, request.amount(), context);

        return TransferResponse.builder()
                .transactionId(result.transactionId())
                .fromWalletId(from.getId())
                .toWalletId(to.getId())
                .amount(request.amount())
                .fromBalance(result.fromBalance())
                .toBalance(result.toBalance())
                .build();
    }

    private TransactionContext toContext(String idempotencyKey, String reference, java.util.Map<String, Object> metadata) {
        return new TransactionContext(idempotencyKey, reference, metadata);
    }

    private TransactionHistoryItem toHistoryItem(LedgerEntry entry) {
        var transaction = entry.getTransaction();
        return TransactionHistoryItem.builder()
                .transactionId(transaction.getId())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .direction(entry.getDirection())
                .amount(entry.getAmount())
                .currency(entry.getCurrency())
                .reference(transaction.getReference())
                .createdAt(transaction.getCreatedAt())
                .completedAt(transaction.getCompletedAt())
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
        if (wallet.isSystem() || wallet.getUser() == null || !wallet.getUser().getId().equals(userId)) {
            throw new WalletAccessDeniedException(wallet.getId());
        }
    }
}
