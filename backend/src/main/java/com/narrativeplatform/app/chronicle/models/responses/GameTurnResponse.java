package com.narrativeplatform.app.chronicle.models.responses;

import com.narrativeplatform.app.chronicle.models.enums.GameTurnStatusType;
import java.time.Instant;
import java.util.UUID;

public record GameTurnResponse(UUID id, int sequenceNumber, short cycleNumber, int positionInCycle, UUID userId, String author, GameTurnStatusType status, Instant startedAt, Instant expiresAt) {
}
