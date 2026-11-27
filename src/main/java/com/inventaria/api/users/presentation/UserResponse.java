package com.inventaria.api.users.presentation;

import com.inventaria.api.users.domain.User;

import java.time.LocalDateTime;

public record UserResponse(
        Integer id,
        String email,
        String name,
        String photoUrl,
        LocalDateTime registeredAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhotoUrl(),
                user.getRegisteredAt()
        );
    }
}
