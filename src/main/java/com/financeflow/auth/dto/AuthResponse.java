package com.financeflow.auth.dto;

import com.financeflow.domain.UserRole;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class AuthResponse {
    String accessToken;
    String tokenType;
    long expiresInSeconds;
    UUID userId;
    String email;
    UserRole role;
}
