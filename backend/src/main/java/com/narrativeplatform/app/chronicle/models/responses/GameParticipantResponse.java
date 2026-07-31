package com.narrativeplatform.app.chronicle.models.responses;

import com.narrativeplatform.app.chronicle.models.enums.GameParticipantStatusType;
import com.narrativeplatform.app.chronicle.models.enums.RemovedByType;

import java.util.UUID;

public record GameParticipantResponse(
        UUID userId,
        String displayName,
        GameParticipantStatusType status,
        RemovedByType removedByType
) {
}
