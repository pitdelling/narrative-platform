package com.narrativeplatform.shared.integrations;

import com.narrativeplatform.configuration.AppProperties;
import com.narrativeplatform.shared.exceptions.AiNotConfiguredException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.util.Map;

@Component
public class OpenAiClient {
    private final AppProperties.OpenAi properties;
    private final RestClient restClient;

    public OpenAiClient(final AppProperties appProperties, final RestClient.Builder builder) {
        this.properties = appProperties.openai();
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
    }

    public boolean configured() {
        return properties.configured();
    }

    public void requireConfigured() {
        if (configured()) {
            return;
        }
        throw new AiNotConfiguredException();
    }

    public GeneratedText generate(final String prompt, final String model) {
        requireConfigured();
        final Map<String, Object> body = Map.of(
                "model", model,
                "input", prompt
        );
        final var response = restClient.post()
                .uri("/responses")
                .header("Authorization", "Bearer " + properties.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        if (response == null) {
            throw new IllegalStateException("OpenAI returned an empty response.");
        }
        final var outputText = extractOutputText(response);
        final var usage = response.path("usage");
        return new GeneratedText(
                outputText,
                usage.path("input_tokens").isNumber() ? usage.path("input_tokens").asInt() : null,
                usage.path("output_tokens").isNumber() ? usage.path("output_tokens").asInt() : null,
                model
        );
    }

    private String extractOutputText(final JsonNode response) {
        for (final var output : response.path("output")) {
            for (final var content : output.path("content")) {
                if ("output_text".equals(content.path("type").asText()) && content.hasNonNull("text")) {
                    return content.path("text").asText();
                }
            }
        }
        if (response.hasNonNull("output_text")) {
            return response.path("output_text").asText();
        }
        throw new IllegalStateException("OpenAI response did not contain output text.");
    }

    public record GeneratedText(String text, Integer inputTokens, Integer outputTokens, String model) {
    }
}
