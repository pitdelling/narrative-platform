package com.narrativeplatform.app.aijob.models.commands;

import com.narrativeplatform.app.aijob.models.enums.AiJobType;

import java.util.UUID;

public record AiGenerationCommand(
        UUID jobId,
        UUID chronicleId,
        AiJobType jobType,
        String fallbackTitle,
        String prompt
) {
}
