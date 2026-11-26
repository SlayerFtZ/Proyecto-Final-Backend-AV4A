package com.inventaria.api.reports.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface SaleDetailRow {
    Integer getIdVenta();
    LocalDateTime getFecha();
    BigDecimal getTotal();
    Integer getIdDetalle();
    Integer getCantidad();
    BigDecimal getPrecioUnitario();
    Integer getIdProducto();
    String getProductoNombre();
    String getCodigoBarras();
}
