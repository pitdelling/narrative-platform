package com.narrativeplatform.app.chronicle.models.responses;

import java.time.Instant;

public record WrittenStoryLockResponse(
        boolean acquired,
        String lockToken,
        String lockedBy,
        Instant expiresAt
) {
}
