package com.financeflow.user;

import com.financeflow.domain.User;
import com.financeflow.domain.UserRepository;
import com.financeflow.domain.UserRole;
import com.financeflow.domain.UserStatus;
import com.financeflow.domain.Wallet;
import com.financeflow.domain.WalletRepository;
import com.financeflow.domain.WalletStatus;
import com.financeflow.user.dto.CreateUserRequest;
import com.financeflow.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Value("${financeflow.wallet.default-currency}")
    private String defaultCurrency;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        var email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }

        var user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        var wallet = new Wallet();
        wallet.setUser(user);
        wallet.setCurrency(defaultCurrency);
        wallet.setStatus(WalletStatus.ACTIVE);
        walletRepository.save(wallet);

        return userMapper.toResponse(user, wallet);
    }
}
