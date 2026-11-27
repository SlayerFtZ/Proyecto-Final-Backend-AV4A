package com.inventaria.api.auth.domain;

import com.inventaria.api.users.domain.User;

import java.time.Instant;

public record AuthResult(
        String accessToken,
        Instant expiresAt,
        User user
) {}
