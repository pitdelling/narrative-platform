package com.narrativeplatform.app.aijob.services;

import com.narrativeplatform.app.aijob.models.enums.AiJobStatusType;
import com.narrativeplatform.app.aijob.repositories.AiJobRepository;
import com.narrativeplatform.shared.integrations.OpenAiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiJobProcessor {
    private static final int PROCESS_INTERVAL_MILLISECONDS = 15_000;

    private final AiJobRepository aiJobRepository;
    private final OpenAiClient openAiClient;
    private final AiJobWorker aiJobWorker;
    private final AiJobStateService aiJobStateService;

    @Scheduled(fixedDelay = PROCESS_INTERVAL_MILLISECONDS)
    public void processPending() {
        aiJobStateService.recoverStaleJobs();
        if (!openAiClient.configured()) {
            return;
        }
        final var jobIds = aiJobRepository.findTop5ByStatusOrderByCreatedAtAsc(AiJobStatusType.PENDING)
                .stream()
                .map(job -> job.getId())
                .toList();
        for (final var jobId : jobIds) {
            aiJobWorker.processOne(jobId);
        }
    }
}
