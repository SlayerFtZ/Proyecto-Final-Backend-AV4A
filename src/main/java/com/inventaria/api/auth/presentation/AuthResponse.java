package com.inventaria.api.auth.presentation;

import com.inventaria.api.users.presentation.UserResponse;

import java.time.Instant;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,      // segundos de vida restantes del token
        Instant expiresAt,
        UserResponse user
) {}
