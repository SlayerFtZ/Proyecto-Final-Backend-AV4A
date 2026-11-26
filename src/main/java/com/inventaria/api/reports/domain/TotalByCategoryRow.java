package com.inventaria.api.reports.domain;

import java.math.BigDecimal;

public interface TotalByCategoryRow {
    Integer getIdCategoria();
    String getCategoria();
    Long getUnidadesVendidas();
    BigDecimal getTotalVendido();
}
