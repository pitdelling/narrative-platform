package com.narrativeplatform.app.aijob.services;

import com.narrativeplatform.app.aijob.models.commands.AiRawGenerationResult;
import com.narrativeplatform.app.aijob.models.entities.AiJobEntity;
import com.narrativeplatform.app.aijob.models.enums.AiJobType;
import com.narrativeplatform.app.chronicle.models.entities.ChronicleEntity;
import com.narrativeplatform.app.chronicle.models.entities.GameSegmentEntity;

import java.util.List;

/**
 * Type-specific behaviour for one of the three AI tasks created after a game chronicle
 * completes. {@link AiJobStateService} owns the shared queue mechanics (locking, transaction
 * boundaries, attempt counting); each handler only decides what a claimed/completed/failed
 * job of its own type actually does.
 *
 * <p>Only {@link com.narrativeplatform.app.aijob.models.enums.AiJobType#STORY_ADAPTATION_GENERATION}'s
 * handler is allowed to mutate {@link ChronicleEntity#getStatus()} — the canon map and synopsis
 * handlers must only ever touch their own artifact entity's status, so the three tasks stay
 * independently observable.
 */
public interface AiJobTypeHandler {
    AiJobType jobType();

    void onClaimed(AiJobEntity job);

    String buildPrompt(ChronicleEntity chronicle, List<GameSegmentEntity> activeSegments);

    /**
     * Parses and persists the raw model output for this job type. The canon map handler may
     * throw an unchecked validation exception when the output fails structural/semantic
     * validation — {@link AiJobStateService} treats that exactly like a generation failure.
     */
    void onCompleted(AiJobEntity job, AiRawGenerationResult raw);

    void onFailed(AiJobEntity job, String errorMessage, boolean permanent);

    void onRecoveredAsPending(AiJobEntity job);
}
