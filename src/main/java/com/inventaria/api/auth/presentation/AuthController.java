package com.inventaria.api.auth.presentation;

import com.inventaria.api.auth.domain.AuthResult;
import com.inventaria.api.auth.domain.AuthService;
import com.inventaria.api.users.presentation.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Login con Google y emisión del JWT de la API")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Recibe el id_token que la app obtuvo de Google y devuelve el JWT de la API.
     * Si el usuario no existe, se registra en este momento.
     * Un id_token inválido responde 401 (InvalidGoogleTokenException → GlobalExceptionHandler).
     */
    @PostMapping("/google")
    @Operation(summary = "Login con Google: recibe el id_token y devuelve el JWT de la API (registra al usuario si es nuevo)")
    public AuthResponse loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        AuthResult result = authService.loginWithGoogle(request.idToken());
        long expiresIn = Duration.between(Instant.now(), result.expiresAt()).getSeconds();

        return new AuthResponse(
                result.accessToken(),
                "Bearer",
                expiresIn,
                result.expiresAt(),
                UserResponse.from(result.user())
        );
    }
}
