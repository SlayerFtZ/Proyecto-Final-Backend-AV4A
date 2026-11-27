package com.inventaria.api.shared.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final SecretKey secretKey;
    private final String issuer;

    public SecurityConfig(@Value("${app.security.jwt-secret}") String jwtSecretBase64,
                          @Value("${app.security.jwt-issuer}") String issuer) {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecretBase64.trim());
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe ser Base64 de al menos 32 bytes (256 bits)");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        this.issuer = issuer;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   SecurityErrorHandler errorHandler) throws Exception {
        http
                // API REST sin sesiones ni cookies: CSRF no aplica.
                .csrf(csrf -> csrf.disable())
                // Reutiliza las reglas de tu CorsConfig (WebMvcConfigurer).
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ── Públicos ──────────────────────────────────────────
                        .requestMatchers(HttpMethod.POST, "/api/auth/google").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // Página de error interna de Spring (evita que un 500 se vea como 401).
                        .requestMatchers("/error").permitAll()
                        // ── Todo lo demás requiere JWT ────────────────────────
                        .anyRequest().authenticated()
                )
                // 401/403 con el mismo formato ApiError del resto de la API.
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                // Valida el header "Authorization: Bearer <jwt>" con el JwtDecoder de abajo.
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler));

        return http.build();
    }

    /** Firma los JWT que emite la API. */
    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }

    /** Valida los JWT que llegan en cada petición (firma, expiración y emisor). */
    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }
}
