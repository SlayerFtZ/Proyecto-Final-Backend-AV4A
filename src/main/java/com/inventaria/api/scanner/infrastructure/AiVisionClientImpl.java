package com.inventaria.api.scanner.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventaria.api.scanner.domain.AiVisionClient;
import com.inventaria.api.scanner.domain.ProductSuggestionResponse;
import com.inventaria.api.shared.config.IaProperties;
import com.inventaria.api.shared.error.AiUnavailableException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;
import java.util.Set;

@Component
public class AiVisionClientImpl implements AiVisionClient {

    private static final Set<String> VALID_TYPES = Set.of("Perecedero", "Electronico", "Ropa");

    private static final String PROMPT_TEMPLATE =
            "Analiza la imagen del producto. Responde ÚNICAMENTE con un JSON válido " +
            "(sin bloques de código markdown) usando exactamente esta estructura:\n" +
            "{\n" +
            "  \"suggestedName\": \"nombre del producto en español\",\n" +
            "  \"suggestedCategory\": \"una categoría de esta lista o null: %s\",\n" +
            "  \"productType\": \"Perecedero | Electronico | Ropa  (o null si ninguno aplica)\",\n" +
            "  \"description\": \"descripción breve en español, máx. 100 palabras\"\n" +
            "}";

    private final RestClient restClient;
    private final IaProperties props;
    private final ObjectMapper objectMapper;

    public AiVisionClientImpl(RestClient restClient, IaProperties props, ObjectMapper objectMapper) {
        this.restClient   = restClient;
        this.props        = props;
        this.objectMapper = objectMapper;
    }

    @Override
    public ProductSuggestionResponse analyze(byte[] image, String contentType, List<String> categories) {
        if (!props.isConfigured()) {
            throw new AiUnavailableException("IA no configurada (api-key o base-url no establecidos)");
        }
        try {
            String base64  = Base64.getEncoder().encodeToString(image);
            String dataUrl = "data:" + contentType + ";base64," + base64;
            String prompt  = String.format(PROMPT_TEMPLATE, categories);

            List<Object> content = List.of(
                    new TextContent("text", prompt),
                    new ImageContent("image_url", new ImageUrl(dataUrl))
            );

            VisionRequest request = new VisionRequest(
                    props.getModelVision(),
                    List.of(new VisionMessage("user", content)),
                    props.getMaxTokens()
            );

            VisionResponseBody response = restClient.post()
                    .uri(props.getBaseUrl() + "/chat/completions")
                    .header("Authorization", "Bearer " + props.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(VisionResponseBody.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new AiUnavailableException("Respuesta vacía del servicio de IA de visión");
            }

            return parseResponse(response.choices().get(0).message().content());

        } catch (AiUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new AiUnavailableException("Error al comunicarse con el servicio de IA de visión: " + e.getMessage());
        }
    }

    // ── parsing helpers ───────────────────────────────────────────────────────

    private ProductSuggestionResponse parseResponse(String rawText) {
        try {
            String json = stripMarkdown(rawText);
            JsonNode node = objectMapper.readTree(json);

            String suggestedName     = textOrNull(node, "suggestedName");
            String suggestedCategory = textOrNull(node, "suggestedCategory");
            String description       = textOrNull(node, "description");
            String productType       = normalizeType(textOrNull(node, "productType"));

            return new ProductSuggestionResponse(suggestedName, suggestedCategory, null, productType, description);

        } catch (Exception e) {
            throw new AiUnavailableException("No se pudo interpretar la respuesta de la IA: " + e.getMessage());
        }
    }

    private String stripMarkdown(String text) {
        return text.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").trim();
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode child = node.get(field);
        if (child == null || child.isNull()) return null;
        String value = child.asText().trim();
        return (value.isBlank() || "null".equalsIgnoreCase(value)) ? null : value;
    }

    private String normalizeType(String raw) {
        if (raw == null) return null;
        for (String valid : VALID_TYPES) {
            if (valid.equalsIgnoreCase(raw.trim())) return valid;
        }
        return null;
    }

    // ── private request / response models ────────────────────────────────────

    private record VisionRequest(
            String model,
            List<VisionMessage> messages,
            @JsonProperty("max_tokens") int maxTokens
    ) {}

    private record VisionMessage(String role, List<Object> content) {}

    private record TextContent(String type, String text) {}

    private record ImageContent(String type, @JsonProperty("image_url") ImageUrl imageUrl) {}

    private record ImageUrl(String url) {}

    private record VisionResponseBody(List<VisionChoice> choices) {}

    private record VisionChoice(AssistantMessage message) {}

    private record AssistantMessage(String role, String content) {}
}
