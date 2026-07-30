package com.narrativeplatform.app.aijob.services;

import com.narrativeplatform.app.aijob.models.enums.AiJobStatusType;
import com.narrativeplatform.app.aijob.repositories.AiJobRepository;
import com.narrativeplatform.shared.integrations.OpenAiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
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
        if (jobIds.isEmpty()) return;
        log.debug("AI job sweep picked up {} pending job(s).", jobIds.size());
        for (final var jobId : jobIds) {
            aiJobWorker.processOne(jobId);
        }
    }
}
