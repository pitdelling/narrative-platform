package com.narrativeplatform.app.chronicle.models.responses;

import com.narrativeplatform.app.chronicle.models.enums.ChronicleStatusType;

import java.util.List;
import java.util.UUID;

public record GameChronicleDetailResponse(
        UUID id,
        String title,
        ChronicleStatusType status,
        short cycleCount,
        int currentSequence,
        int totalTurns,
        UUID currentUserId,
        boolean currentUserTurn,
        boolean narrator,
        int revealSeconds,
        String currentDraft,
        GeneratedStoryResponse generatedStory,
        List<GameTurnResponse> turns,
        List<GameSegmentResponse> segments
) {
}
