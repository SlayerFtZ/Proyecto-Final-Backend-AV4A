package com.inventaria.api.summary.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.inventaria.api.shared.config.IaProperties;
import com.inventaria.api.shared.error.AiUnavailableException;
import com.inventaria.api.summary.domain.AiTextClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class AiTextClientImpl implements AiTextClient {

    private final RestClient restClient;
    private final IaProperties props;

    public AiTextClientImpl(RestClient restClient, IaProperties props) {
        this.restClient = restClient;
        this.props = props;
    }

    @Override
    public String generateText(String systemPrompt, String data) {
        if (!props.isConfigured()) {
            throw new AiUnavailableException("IA no configurada (api-key o base-url no establecidos)");
        }
        try {
            AiRequest request = new AiRequest(
                    props.getModelTexto(),
                    List.of(new AiMessage("system", systemPrompt), new AiMessage("user", data)),
                    props.getMaxTokens()
            );

            AiResponseBody response = restClient.post()
                    .uri(props.getBaseUrl() + "/chat/completions")
                    .header("Authorization", "Bearer " + props.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AiResponseBody.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new AiUnavailableException("Respuesta vacía del servicio de IA");
            }
            return response.choices().get(0).message().content();

        } catch (AiUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new AiUnavailableException("Error al comunicarse con el servicio de IA: " + e.getMessage());
        }
    }

    // ── private request / response models ────────────────────────────────────

    private record AiRequest(
            String model,
            List<AiMessage> messages,
            @JsonProperty("max_tokens") int maxTokens
    ) {}

    private record AiMessage(String role, String content) {}

    private record AiResponseBody(List<AiChoice> choices) {}

    private record AiChoice(AiMessage message) {}
}
