package com.inventaria.api.scanner.domain;

import java.util.List;

public interface AiVisionClient {

    /**
     * Sends an image to the vision model and returns a product suggestion.
     * Throws AiUnavailableException on any failure.
     */
    ProductSuggestionResponse analyze(byte[] image, String contentType, List<String> categories);
}
