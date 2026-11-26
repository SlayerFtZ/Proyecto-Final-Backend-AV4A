package com.inventaria.api.products.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ProductTypeConverter implements AttributeConverter<ProductType, String> {

    @Override
    public String convertToDatabaseColumn(ProductType type) {
        return type == null ? null : type.getDbValue();
    }

    @Override
    public ProductType convertToEntityAttribute(String dbValue) {
        return dbValue == null ? null : ProductType.fromDbValue(dbValue);
    }
}
