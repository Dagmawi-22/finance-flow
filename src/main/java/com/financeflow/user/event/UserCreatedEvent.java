package com.financeflow.user.event;

import com.financeflow.domain.User;

public record UserCreatedEvent(User user) {
}
