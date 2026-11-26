package com.inventaria.api.reports.domain;

public interface LowStockBySupplierRow {
    Integer getIdProveedor();
    String getProveedorNombre();
    String getTelefono();
    String getCorreo();
    Integer getIdProducto();
    String getCodigoBarras();
    String getProductoNombre();
    Integer getStock();
    Integer getStockMinimo();
    String getTipo();
}
