package com.inventaria.api.products.presentation;

import java.util.List;

public record BatchResponse(int received, int inserted, List<BatchErrorItem> errors) {}
