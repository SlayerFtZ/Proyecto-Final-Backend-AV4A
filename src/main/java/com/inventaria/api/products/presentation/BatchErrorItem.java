package com.inventaria.api.products.presentation;

public record BatchErrorItem(int row, String barcode, String message) {}
