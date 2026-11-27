package com.inventaria.api.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventaria.api.shared.error.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Responde los 401/403 de Spring Security con el mismo formato ApiError
 * que usa GlobalExceptionHandler, para que la app reciba siempre el mismo JSON.
 */
@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public SecurityErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Sin token, token inválido o expirado → 401. */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        write(request, response, HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO",
                "Se requiere un token válido. Inicia sesión de nuevo.");
    }

    /** Autenticado pero sin permiso → 403. */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        write(request, response, HttpStatus.FORBIDDEN, "ACCESO_DENEGADO",
                "No tienes permiso para realizar esta operación.");
    }

    private void write(HttpServletRequest request, HttpServletResponse response,
                       HttpStatus status, String code, String message) throws IOException {
        ApiError error = new ApiError(
                LocalDateTime.now(),
                status.value(),
                code,
                message,
                null,
                request.getRequestURI()
        );
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
