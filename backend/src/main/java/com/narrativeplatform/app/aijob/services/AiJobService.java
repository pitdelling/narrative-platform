package com.narrativeplatform.app.aijob.services;

import com.narrativeplatform.app.aijob.models.entities.AiJobEntity;
import com.narrativeplatform.app.aijob.models.enums.AiJobStatusType;
import com.narrativeplatform.app.aijob.models.enums.AiJobType;
import com.narrativeplatform.app.aijob.repositories.AiJobRepository;
import com.narrativeplatform.app.auth.models.entities.UserEntity;
import com.narrativeplatform.app.chronicle.models.entities.ChronicleEntity;
import com.narrativeplatform.app.chronicle.models.enums.ChronicleStatusType;
import com.narrativeplatform.shared.exceptions.ConflictException;
import com.narrativeplatform.shared.integrations.OpenAiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiJobService {
    private static final String IDEMPOTENCY_KEY_SEPARATOR = ":";
    private static final Set<AiJobStatusType> ACTIVE_STATUSES = Set.of(
            AiJobStatusType.PENDING,
            AiJobStatusType.PROCESSING
    );

    private final AiJobRepository aiJobRepository;
    private final OpenAiClient openAiClient;

    @Transactional
    public void enqueue(final ChronicleEntity chronicle, final UserEntity requestedBy) {
        enqueueInternal(chronicle, requestedBy, AiJobType.STORY_ADAPTATION_GENERATION);
    }

    @Transactional
    public void enqueueRequested(final ChronicleEntity chronicle, final UserEntity requestedBy) {
        openAiClient.requireConfigured();
        enqueueInternal(chronicle, requestedBy, AiJobType.STORY_ADAPTATION_GENERATION);
    }

    /**
     * Used for the canon map and synopsis tasks created automatically right after a game
     * chronicle completes. Unlike {@link #enqueue}/{@link #enqueueRequested}, this never
     * throws: an already-active job of the same type is logged and silently skipped, so it
     * can never roll back the transaction that just marked the game as finished.
     */
    @Transactional
    public void enqueueAutomatic(final ChronicleEntity chronicle, final AiJobType jobType, final UserEntity requestedBy) {
        if (aiJobRepository.existsByChronicleIdAndJobTypeAndStatusIn(chronicle.getId(), jobType, ACTIVE_STATUSES)) {
            log.debug("Skipped automatic enqueue of {} for chronicle {}: a job of this type is already active.", jobType, chronicle.getId());
            return;
        }
        final var idempotencyKey = chronicle.getId() + IDEMPOTENCY_KEY_SEPARATOR + jobType + IDEMPOTENCY_KEY_SEPARATOR + UUID.randomUUID();
        aiJobRepository.save(new AiJobEntity(chronicle, requestedBy, idempotencyKey, jobType));
        log.debug("Enqueued automatic AI job {} for chronicle {}.", jobType, chronicle.getId());
    }

    public void requireConfigured() {
        openAiClient.requireConfigured();
    }

    @Transactional
    public boolean enqueueIfIdle(final ChronicleEntity chronicle, final UserEntity requestedBy, final AiJobType jobType) {
        if (aiJobRepository.existsByChronicleIdAndJobTypeAndStatusIn(chronicle.getId(), jobType, ACTIVE_STATUSES)) {
            log.debug("Skipped enqueue of {} for chronicle {}: a job of this type is already active.", jobType, chronicle.getId());
            return false;
        }
        final var idempotencyKey = chronicle.getId() + IDEMPOTENCY_KEY_SEPARATOR + jobType + IDEMPOTENCY_KEY_SEPARATOR + UUID.randomUUID();
        aiJobRepository.save(new AiJobEntity(chronicle, requestedBy, idempotencyKey, jobType));
        if (jobType == AiJobType.STORY_ADAPTATION_GENERATION) {
            chronicle.setStatus(ChronicleStatusType.AI_PENDING);
        }
        log.debug("Enqueued AI job {} for chronicle {} requested by user {}.", jobType, chronicle.getId(), requestedBy.getId());
        return true;
    }

    private void enqueueInternal(final ChronicleEntity chronicle, final UserEntity requestedBy, final AiJobType jobType) {
        if (aiJobRepository.existsByChronicleIdAndJobTypeAndStatusIn(chronicle.getId(), jobType, ACTIVE_STATUSES)) {
            log.debug("Rejected AI job enqueue for chronicle {}: a job is already active.", chronicle.getId());
            throw new ConflictException("An AI generation is already pending for this chronicle.");
        }
        final var idempotencyKey = chronicle.getId() + IDEMPOTENCY_KEY_SEPARATOR + jobType + IDEMPOTENCY_KEY_SEPARATOR + UUID.randomUUID();
        aiJobRepository.save(new AiJobEntity(chronicle, requestedBy, idempotencyKey, jobType));
        chronicle.setStatus(ChronicleStatusType.AI_PENDING);
        log.debug("Enqueued AI job for chronicle {} requested by user {}.", chronicle.getId(), requestedBy.getId());
    }
}
