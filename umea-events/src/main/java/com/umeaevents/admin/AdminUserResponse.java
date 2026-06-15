package com.umeaevents.admin;

import com.umeaevents.user.User;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String email,
        String role,
        boolean active,
        OffsetDateTime createdAt
) {
    public static AdminUserResponse from(User u) {
        return new AdminUserResponse(u.getId(), u.getEmail(), u.getRole().name(), u.isActive(), u.getCreatedAt());
    }
}
