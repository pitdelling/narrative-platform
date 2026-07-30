package com.narrativeplatform.app.aijob.services;

import com.narrativeplatform.app.aijob.models.commands.AiRawGenerationResult;
import com.narrativeplatform.app.aijob.models.entities.AiJobEntity;
import com.narrativeplatform.app.aijob.models.enums.AiJobStatusType;
import com.narrativeplatform.app.aijob.models.enums.AiJobType;
import com.narrativeplatform.app.aijob.repositories.AiJobRepository;
import com.narrativeplatform.app.auth.models.entities.UserEntity;
import com.narrativeplatform.app.canon.services.CanonMapValidationException;
import com.narrativeplatform.app.chronicle.models.entities.ChronicleEntity;
import com.narrativeplatform.app.chronicle.models.entities.GameRunEntity;
import com.narrativeplatform.app.chronicle.models.enums.ChronicleStatusType;
import com.narrativeplatform.app.chronicle.models.enums.ChronicleType;
import com.narrativeplatform.app.chronicle.repositories.GameRunRepository;
import com.narrativeplatform.app.chronicle.repositories.GameSegmentRepository;
import com.narrativeplatform.app.party.models.entities.PartyEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiJobStateServiceTest {
    @Mock
    private AiJobRepository aiJobRepository;
    @Mock
    private GameRunRepository gameRunRepository;
    @Mock
    private GameSegmentRepository gameSegmentRepository;
    @Mock
    private AiJobTypeHandler adaptationHandler;
    @Mock
    private AiJobTypeHandler canonMapHandler;
    @Mock
    private AiJobTypeHandler synopsisHandler;

    private AiJobStateService aiJobStateService;
    private ChronicleEntity chronicle;

    @BeforeEach
    void setUp() {
        lenient().when(adaptationHandler.jobType()).thenReturn(AiJobType.STORY_ADAPTATION_GENERATION);
        lenient().when(canonMapHandler.jobType()).thenReturn(AiJobType.CANON_MAP_GENERATION);
        lenient().when(synopsisHandler.jobType()).thenReturn(AiJobType.STORY_SYNOPSIS_GENERATION);
        aiJobStateService = new AiJobStateService(
                aiJobRepository, gameRunRepository, gameSegmentRepository,
                List.of(adaptationHandler, canonMapHandler, synopsisHandler)
        );

        final var party = new PartyEntity("Test Party", "test-party", null, null, null);
        final var creator = new UserEntity("author", "Author", "hash");
        creator.setId(UUID.randomUUID());
        chronicle = new ChronicleEntity(party, creator, ChronicleType.GAME, ChronicleStatusType.AI_PENDING, "Title");
        chronicle.setId(UUID.randomUUID());
    }

    private AiJobEntity job(final AiJobType jobType, final AiJobStatusType status, final int attemptCount) {
        final var entity = new AiJobEntity(chronicle, chronicle.getCreator(), "key", jobType);
        entity.setId(UUID.randomUUID());
        entity.setStatus(status);
        entity.setAttemptCount(attemptCount);
        return entity;
    }

    @Test
    void claimOnlyDelegatesToTheMatchingHandler() {
        final var job = job(AiJobType.CANON_MAP_GENERATION, AiJobStatusType.PENDING, 0);
        when(aiJobRepository.findForUpdate(job.getId())).thenReturn(Optional.of(job));
        final var run = new GameRunEntity(chronicle, (short) 1, 2);
        run.setId(UUID.randomUUID());
        when(gameRunRepository.findByChronicleId(chronicle.getId())).thenReturn(Optional.of(run));
        when(gameSegmentRepository.findAllByRunIdOrderBySequenceNumberAsc(run.getId())).thenReturn(List.of());
        when(canonMapHandler.buildPrompt(eq(chronicle), any())).thenReturn("prompt");

        final var command = aiJobStateService.claim(job.getId()).orElseThrow();

        assertEquals(AiJobType.CANON_MAP_GENERATION, command.jobType());
        assertEquals(AiJobStatusType.PROCESSING, job.getStatus());
        assertEquals(1, job.getAttemptCount());
        verify(canonMapHandler).onClaimed(job);
        verifyNoInteractions(adaptationHandler, synopsisHandler);
    }

    @Test
    void claimReturnsEmptyWhenJobIsNotPending() {
        final var job = job(AiJobType.STORY_ADAPTATION_GENERATION, AiJobStatusType.PROCESSING, 1);
        when(aiJobRepository.findForUpdate(job.getId())).thenReturn(Optional.of(job));

        assertTrue(aiJobStateService.claim(job.getId()).isEmpty());
        verifyNoInteractions(adaptationHandler, gameRunRepository);
    }

    @Test
    void completeDelegatesOnlyToTheOwningHandlerAndLeavesOthersUntouched() {
        final var job = job(AiJobType.STORY_SYNOPSIS_GENERATION, AiJobStatusType.PROCESSING, 1);
        when(aiJobRepository.findForUpdate(job.getId())).thenReturn(Optional.of(job));
        final var raw = new AiRawGenerationResult("{}", "gpt-5-mini", 10, 20);

        aiJobStateService.complete(job.getId(), raw);

        assertEquals(AiJobStatusType.COMPLETED, job.getStatus());
        verify(synopsisHandler).onCompleted(job, raw);
        verifyNoInteractions(adaptationHandler, canonMapHandler);
    }

    @Test
    void completeRoutesCanonMapValidationFailureThroughTheNormalFailurePath() {
        final var job = job(AiJobType.CANON_MAP_GENERATION, AiJobStatusType.PROCESSING, 1);
        when(aiJobRepository.findForUpdate(job.getId())).thenReturn(Optional.of(job));
        final var raw = new AiRawGenerationResult("not json", "gpt-5-mini", 1, 1);
        doThrow(new CanonMapValidationException("bad output")).when(canonMapHandler).onCompleted(job, raw);

        aiJobStateService.complete(job.getId(), raw);

        assertEquals(AiJobStatusType.PENDING, job.getStatus());
        verify(canonMapHandler).onFailed(job, "bad output", false);
        verifyNoInteractions(adaptationHandler, synopsisHandler);
    }

    @Test
    void failMarksPermanentAfterMaxAttemptsAndOnlyTouchesItsOwnHandler() {
        final var job = job(AiJobType.STORY_ADAPTATION_GENERATION, AiJobStatusType.PROCESSING, 3);
        when(aiJobRepository.findForUpdate(job.getId())).thenReturn(Optional.of(job));

        aiJobStateService.fail(job.getId(), "boom");

        assertEquals(AiJobStatusType.FAILED, job.getStatus());
        verify(adaptationHandler).onFailed(job, "boom", true);
        verifyNoInteractions(canonMapHandler, synopsisHandler);
    }

    @Test
    void failRetriesWhenBelowMaxAttempts() {
        final var job = job(AiJobType.CANON_MAP_GENERATION, AiJobStatusType.PROCESSING, 1);
        when(aiJobRepository.findForUpdate(job.getId())).thenReturn(Optional.of(job));

        aiJobStateService.fail(job.getId(), "temporary");

        assertEquals(AiJobStatusType.PENDING, job.getStatus());
        verify(canonMapHandler).onFailed(job, "temporary", false);
    }

    @Test
    void recoverStaleJobsHandlesEachJobIndependentlyByType() {
        final var staleAdaptation = job(AiJobType.STORY_ADAPTATION_GENERATION, AiJobStatusType.PROCESSING, 1);
        final var staleCanonMap = job(AiJobType.CANON_MAP_GENERATION, AiJobStatusType.PROCESSING, 3);
        when(aiJobRepository.findAllByStatusAndStartedAtBefore(eq(AiJobStatusType.PROCESSING), any()))
                .thenReturn(List.of(staleAdaptation, staleCanonMap));

        aiJobStateService.recoverStaleJobs();

        assertEquals(AiJobStatusType.PENDING, staleAdaptation.getStatus());
        verify(adaptationHandler).onRecoveredAsPending(staleAdaptation);

        assertEquals(AiJobStatusType.FAILED, staleCanonMap.getStatus());
        verify(canonMapHandler).onFailed(eq(staleCanonMap), any(), eq(true));
        verifyNoInteractions(synopsisHandler);
    }
}
