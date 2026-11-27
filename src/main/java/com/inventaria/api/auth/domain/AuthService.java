package com.inventaria.api.auth.domain;

import com.inventaria.api.shared.security.JwtService;
import com.inventaria.api.users.domain.User;
import com.inventaria.api.users.infrastructure.UserJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UserJpaRepository users;
    private final JwtService jwtService;

    public AuthService(GoogleTokenVerifier googleTokenVerifier,
                       UserJpaRepository users,
                       JwtService jwtService) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.users = users;
        this.jwtService = jwtService;
    }

    /**
     * Valida el id_token de Google, registra al usuario si es nuevo
     * (o actualiza sus datos si ya existe) y emite el JWT propio de la API.
     */
    @Transactional
    public AuthResult loginWithGoogle(String idToken) {
        GoogleUserInfo info = googleTokenVerifier.verify(idToken);
        LocalDateTime now = LocalDateTime.now();

        User user = users.findByGoogleSub(info.sub())
                .orElseGet(() -> {
                    User nuevo = new User();
                    nuevo.setGoogleSub(info.sub());
                    nuevo.setRegisteredAt(now);
                    return nuevo;
                });

        // Se refrescan en cada login por si el usuario los cambió en Google.
        user.setEmail(info.email());
        user.setName(info.name());
        user.setPhotoUrl(info.picture());
        user.setLastAccessAt(now);

        user = users.save(user);

        JwtService.IssuedToken token = jwtService.issue(user.getId(), user.getEmail());
        return new AuthResult(token.value(), token.expiresAt(), user);
    }
}
