package com.narrativeplatform.app.aijob.services;

import com.narrativeplatform.app.aijob.models.commands.AiRawGenerationResult;
import com.narrativeplatform.shared.integrations.OpenAiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiJobWorker {
    private final AiJobStateService aiJobStateService;
    private final OpenAiClient openAiClient;

    public void processOne(final UUID jobId, final String model) {
        final var command = aiJobStateService.claim(jobId).orElse(null);
        if (command == null) {
            return;
        }
        try {
            log.debug("Requesting AI generation for job {} ({}, chronicle {}).", jobId, command.jobType(), command.chronicleId());
            final var generated = openAiClient.generate(command.prompt(), model);
            aiJobStateService.complete(jobId, new AiRawGenerationResult(
                    generated.text(), generated.model(), generated.inputTokens(), generated.outputTokens()
            ));
        } catch (final RuntimeException exception) {
            log.warn("AI generation failed for job {}: {}", jobId, exception.getMessage());
            aiJobStateService.fail(jobId, exception.getMessage());
        }
    }
}
