package com.inventaria.api.products.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum ProductType {
    PERISHABLE("Perecedero"),
    ELECTRONIC("Electronico"),
    CLOTHING("Ropa");

    private final String dbValue;

    ProductType(String dbValue) {
        this.dbValue = dbValue;
    }

    @JsonValue
    public String getDbValue() {
        return dbValue;
    }

    @JsonCreator
    public static ProductType fromDbValue(String value) {
        return Arrays.stream(values())
                .filter(t -> t.dbValue.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tipo de producto inválido: '" + value +
                        "'. Valores válidos: Perecedero, Electronico, Ropa"));
    }
}
