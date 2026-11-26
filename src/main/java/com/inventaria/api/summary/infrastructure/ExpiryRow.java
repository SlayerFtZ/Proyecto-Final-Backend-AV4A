package com.inventaria.api.summary.infrastructure;

import java.time.LocalDate;

public interface ExpiryRow {
    Integer getIdProducto();
    String getNombre();
    LocalDate getFechaCaducidad();
    Integer getStock();
}
