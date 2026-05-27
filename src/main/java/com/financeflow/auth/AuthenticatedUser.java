package com.financeflow.auth;

import com.financeflow.domain.UserRole;

import java.util.UUID;

public record AuthenticatedUser(
        UUID id,
        String email,
        UserRole role
) {
}
