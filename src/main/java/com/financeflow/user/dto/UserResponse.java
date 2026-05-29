package com.financeflow.user.dto;

import com.financeflow.domain.UserRole;
import com.financeflow.domain.UserStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class UserResponse {
    UUID id;
    String email;
    UserRole role;
    UserStatus status;
    Instant createdAt;
    WalletResponse wallet;
}
