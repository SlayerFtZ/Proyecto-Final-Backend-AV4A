package com.inventaria.api.shared.error;

import java.time.LocalDateTime;
import java.util.List;

public record ApiError(
        LocalDateTime timestamp,
        int status,
        String codigo,
        String mensaje,
        List<ApiErrorDetail> detalles,
        String ruta
) {}
