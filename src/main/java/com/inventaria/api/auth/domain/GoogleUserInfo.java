package com.inventaria.api.auth.domain;

/** Datos del usuario extraídos de un id_token de Google ya verificado. */
public record GoogleUserInfo(
        String sub,
        String email,
        String name,
        String picture
) {}
