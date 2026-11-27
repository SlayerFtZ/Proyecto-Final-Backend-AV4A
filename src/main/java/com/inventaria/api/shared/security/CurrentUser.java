package com.inventaria.api.shared.security;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** Da acceso al IdUsuario del usuario autenticado en la petición actual. */
@Component
public class CurrentUser {

    public Integer getId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return Integer.valueOf(jwtAuth.getToken().getSubject());
        }
        throw new AuthenticationCredentialsNotFoundException("No hay un usuario autenticado");
    }
}
