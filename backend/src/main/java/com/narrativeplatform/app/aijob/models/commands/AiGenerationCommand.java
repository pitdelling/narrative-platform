package com.narrativeplatform.app.aijob.models.commands;

import java.util.UUID;

public record AiGenerationCommand(
        UUID jobId,
        UUID chronicleId,
        String fallbackTitle,
        String prompt
) {
}
