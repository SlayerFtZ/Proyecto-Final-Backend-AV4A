package com.inventaria.api.reports.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ProductDetailRow {
    Integer getIdProducto();
    String getCodigoBarras();
    String getNombre();
    String getDescripcion();
    BigDecimal getPrecio();
    Integer getStock();
    Integer getStockMinimo();
    String getTipo();
    LocalDate getFechaCaducidad();
    Integer getMesesGarantia();
    String getTalla();
    Integer getIdCategoria();
    String getCategoriaNombre();
    Integer getIdProveedor();
    String getProveedorNombre();
}
