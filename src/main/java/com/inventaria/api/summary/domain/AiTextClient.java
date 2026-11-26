package com.inventaria.api.summary.domain;

public interface AiTextClient {

    /**
     * Calls the configured AI text model. Throws AiUnavailableException on any failure.
     */
    String generateText(String systemPrompt, String data);
}
