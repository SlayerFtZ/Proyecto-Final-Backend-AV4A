package com.inventaria.api.shared.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ia")
public class IaProperties {

    private String apiKey = "";
    private String baseUrl = "";
    private String modelTexto = "";
    private String modelVision = "";
    private int timeoutSegundos = 20;
    private int maxTokens = 500;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank()
                && baseUrl != null && !baseUrl.isBlank();
    }
}
