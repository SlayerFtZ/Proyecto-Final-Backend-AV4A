package com.inventaria.api.summary.infrastructure;

public interface LowStockRow {
    Integer getIdProducto();
    String getNombre();
    Integer getStock();
    Integer getStockMinimo();
    String getTipo();
}
