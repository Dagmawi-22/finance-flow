package com.financeflow.auth;

import com.financeflow.auth.dto.AuthResponse;
import com.financeflow.auth.dto.LoginRequest;
import com.financeflow.domain.UserRepository;
import com.financeflow.domain.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        var email = request.email().trim().toLowerCase();
        var user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AccountSuspendedException();
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        var token = jwtService.createToken(user);
        var expiresInSeconds = jwtProperties.expirationHours() * ChronoUnit.HOURS.getDuration().toSeconds();

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresInSeconds(expiresInSeconds)
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
