package com.narrativeplatform.app.aijob.services;

import com.narrativeplatform.app.aijob.models.commands.AiGenerationCommand;
import com.narrativeplatform.app.aijob.models.commands.AiRawGenerationResult;
import com.narrativeplatform.app.aijob.models.entities.AiJobEntity;
import com.narrativeplatform.app.aijob.models.enums.AiJobStatusType;
import com.narrativeplatform.app.aijob.models.enums.AiJobType;
import com.narrativeplatform.app.aijob.repositories.AiJobRepository;
import com.narrativeplatform.app.canon.services.CanonMapValidationException;
import com.narrativeplatform.app.chronicle.models.enums.SegmentStatusType;
import com.narrativeplatform.app.chronicle.repositories.GameRunRepository;
import com.narrativeplatform.app.chronicle.repositories.GameSegmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Thin dispatcher over the three AI job types. Owns queue mechanics only (locking, transaction
 * boundaries, attempt counting, stale-job recovery) — what a claimed/completed/failed job of a
 * given type actually does is delegated to its {@link AiJobTypeHandler}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiJobStateService {
    private static final int MAX_ATTEMPTS = 3;
    private static final int MAX_ERROR_LENGTH = 1_000;
    private static final int STALE_PROCESSING_MINUTES = 15;

    private final AiJobRepository aiJobRepository;
    private final GameRunRepository gameRunRepository;
    private final GameSegmentRepository gameSegmentRepository;
    private final List<AiJobTypeHandler> handlers;

    @Transactional
    public void recoverStaleJobs() {
        final var cutoff = Instant.now().minus(STALE_PROCESSING_MINUTES, ChronoUnit.MINUTES);
        final var staleJobs = aiJobRepository.findAllByStatusAndStartedAtBefore(AiJobStatusType.PROCESSING, cutoff);
        if (!staleJobs.isEmpty()) log.debug("Recovering {} stale AI job(s).", staleJobs.size());
        for (final var job : staleJobs) {
            final var handler = handlerFor(job.getJobType());
            if (job.getAttemptCount() >= MAX_ATTEMPTS) {
                job.setStatus(AiJobStatusType.FAILED);
                job.setCompletedAt(Instant.now());
                job.setErrorMessage("AI processing was interrupted too many times.");
                handler.onFailed(job, job.getErrorMessage(), true);
                log.warn("AI job {} ({}) for chronicle {} failed permanently after too many stale interruptions.",
                        job.getId(), job.getJobType(), job.getChronicle().getId());
                continue;
            }
            job.setStatus(AiJobStatusType.PENDING);
            job.setErrorMessage("AI processing was interrupted and queued again.");
            job.setStartedAt(null);
            handler.onRecoveredAsPending(job);
        }
    }

    @Transactional
    public Optional<AiGenerationCommand> claim(final UUID jobId) {
        final var job = aiJobRepository.findForUpdate(jobId).orElse(null);
        if (job == null || job.getStatus() != AiJobStatusType.PENDING) {
            return Optional.empty();
        }
        job.setStatus(AiJobStatusType.PROCESSING);
        job.setStartedAt(Instant.now());
        job.setAttemptCount(job.getAttemptCount() + 1);
        final var handler = handlerFor(job.getJobType());
        handler.onClaimed(job);
        log.debug("Claimed AI job {} ({}) for chronicle {} (attempt {}).",
                job.getId(), job.getJobType(), job.getChronicle().getId(), job.getAttemptCount());

        final var run = gameRunRepository.findByChronicleId(job.getChronicle().getId())
                .orElseThrow(() -> new IllegalStateException("Game run not found."));
        final var activeSegments = gameSegmentRepository.findAllByRunIdOrderBySequenceNumberAsc(run.getId()).stream()
                .filter(segment -> segment.getStatus() != SegmentStatusType.DISABLED)
                .toList();
        return Optional.of(new AiGenerationCommand(
                job.getId(),
                job.getChronicle().getId(),
                job.getJobType(),
                job.getChronicle().getTitle(),
                handler.buildPrompt(job.getChronicle(), activeSegments)
        ));
    }

    @Transactional
    public void complete(final UUID jobId, final AiRawGenerationResult raw) {
        final var job = aiJobRepository.findForUpdate(jobId)
                .orElseThrow(() -> new IllegalStateException("AI job not found."));
        if (job.getStatus() != AiJobStatusType.PROCESSING) {
            return;
        }
        try {
            handlerFor(job.getJobType()).onCompleted(job, raw);
        } catch (final CanonMapValidationException exception) {
            log.warn("AI job {} ({}) for chronicle {} produced an invalid result: {}",
                    job.getId(), job.getJobType(), job.getChronicle().getId(), exception.getMessage());
            applyFailure(job, exception.getMessage());
            return;
        }
        job.setStatus(AiJobStatusType.COMPLETED);
        job.setCompletedAt(Instant.now());
        job.setErrorMessage(null);
        log.info("AI job {} ({}) completed for chronicle {}.", job.getId(), job.getJobType(), job.getChronicle().getId());
    }

    @Transactional
    public void fail(final UUID jobId, final String errorMessage) {
        final var job = aiJobRepository.findForUpdate(jobId).orElse(null);
        if (job == null || job.getStatus() != AiJobStatusType.PROCESSING) {
            return;
        }
        applyFailure(job, errorMessage);
    }

    private void applyFailure(final AiJobEntity job, final String errorMessage) {
        job.setErrorMessage(truncate(errorMessage, MAX_ERROR_LENGTH));
        final var handler = handlerFor(job.getJobType());
        if (job.getAttemptCount() >= MAX_ATTEMPTS) {
            job.setStatus(AiJobStatusType.FAILED);
            job.setCompletedAt(Instant.now());
            handler.onFailed(job, job.getErrorMessage(), true);
            log.warn("AI job {} ({}) for chronicle {} failed permanently after {} attempt(s): {}",
                    job.getId(), job.getJobType(), job.getChronicle().getId(), job.getAttemptCount(), errorMessage);
            return;
        }
        job.setStatus(AiJobStatusType.PENDING);
        handler.onFailed(job, job.getErrorMessage(), false);
        log.debug("AI job {} ({}) for chronicle {} failed attempt {}, will retry: {}",
                job.getId(), job.getJobType(), job.getChronicle().getId(), job.getAttemptCount(), errorMessage);
    }

    private AiJobTypeHandler handlerFor(final AiJobType jobType) {
        return handlers.stream()
                .filter(handler -> handler.jobType() == jobType)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No handler registered for AI job type " + jobType + "."));
    }

    private String truncate(final String value, final int max) {
        if (value == null) {
            return "Unknown AI generation error.";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
