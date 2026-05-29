package com.financeflow.user;

import com.financeflow.domain.User;
import com.financeflow.domain.UserRepository;
import com.financeflow.domain.UserRole;
import com.financeflow.domain.UserStatus;
import com.financeflow.domain.WalletRepository;
import com.financeflow.user.dto.CreateUserRequest;
import com.financeflow.user.dto.UserResponse;
import com.financeflow.user.event.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher events;

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

        events.publishEvent(new UserCreatedEvent(user));

        var wallet = walletRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new IllegalStateException("Wallet not provisioned for user " + user.getId()));

        return userMapper.toResponse(user, wallet);
    }
}
