package com.narrativeplatform.app.aijob.services;

import com.narrativeplatform.app.aijob.models.commands.AiGenerationResult;
import com.narrativeplatform.shared.integrations.OpenAiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiJobWorker {
    private final AiJobStateService aiJobStateService;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    public void processOne(final UUID jobId) {
        final var command = aiJobStateService.claim(jobId).orElse(null);
        if (command == null) {
            return;
        }
        try {
            final var generated = openAiClient.generate(command.prompt());
            final var parsed = parseGeneratedText(generated.text(), command.fallbackTitle());
            aiJobStateService.complete(jobId, new AiGenerationResult(
                    parsed.title(),
                    parsed.story(),
                    generated.model(),
                    generated.inputTokens(),
                    generated.outputTokens()
            ));
        } catch (final RuntimeException exception) {
            aiJobStateService.fail(jobId, exception.getMessage());
        }
    }

    private ParsedStory parseGeneratedText(final String text, final String fallbackTitle) {
        try {
            final JsonNode node = objectMapper.readTree(text);
            final var title = node.path("title").asText(fallbackTitle);
            final var story = node.path("story").asText();
            if (story.isBlank()) {
                return new ParsedStory(fallbackTitle, text);
            }
            return new ParsedStory(title, story);
        } catch (final Exception ignored) {
            return new ParsedStory(fallbackTitle, text);
        }
    }

    private record ParsedStory(String title, String story) {
    }
}
