package com.narrativeplatform.app.aijob.models.commands;

public record AiGenerationResult(
        String title,
        String story,
        String model,
        Integer inputTokens,
        Integer outputTokens
) {
}
