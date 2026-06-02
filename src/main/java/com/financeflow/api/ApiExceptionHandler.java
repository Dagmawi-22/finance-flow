package com.financeflow.api;

import com.financeflow.auth.AccountSuspendedException;
import com.financeflow.auth.InvalidCredentialsException;
import com.financeflow.limits.DailyLimitExceededException;
import com.financeflow.limits.MaxTransactionAmountExceededException;
import com.financeflow.user.DuplicateEmailException;
import com.financeflow.wallet.CurrencyMismatchException;
import com.financeflow.wallet.WalletAccessDeniedException;
import com.financeflow.wallet.InsufficientFundsException;
import com.financeflow.wallet.InvalidTransferException;
import com.financeflow.wallet.WalletNotActiveException;
import com.financeflow.wallet.WalletNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(DuplicateEmailException.class)
    ProblemDetail handleDuplicateEmail(DuplicateEmailException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(AccountSuspendedException.class)
    ProblemDetail handleAccountSuspended(AccountSuspendedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(WalletAccessDeniedException.class)
    ProblemDetail handleWalletAccessDenied(WalletAccessDeniedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(WalletNotFoundException.class)
    ProblemDetail handleWalletNotFound(WalletNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InsufficientFundsException.class)
    ProblemDetail handleInsufficientFunds(InsufficientFundsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler({MaxTransactionAmountExceededException.class, DailyLimitExceededException.class})
    ProblemDetail handleTransactionLimits(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(WalletNotActiveException.class)
    ProblemDetail handleWalletNotActive(WalletNotActiveException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({CurrencyMismatchException.class, InvalidTransferException.class})
    ProblemDetail handleBadTransfer(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        var detail = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }
}
