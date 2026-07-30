package com.narrativeplatform.app.aijob.models.commands;

public record AiRawGenerationResult(
        String text,
        String model,
        Integer inputTokens,
        Integer outputTokens
) {
}
