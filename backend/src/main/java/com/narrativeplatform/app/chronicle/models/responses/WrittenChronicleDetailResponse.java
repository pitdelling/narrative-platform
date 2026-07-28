package com.narrativeplatform.app.chronicle.models.responses;

import com.narrativeplatform.app.chronicle.models.enums.ChronicleStatusType;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record WrittenChronicleDetailResponse(
        UUID id,
        String title,
        ChronicleStatusType status,
        String content,
        long contentVersion,
        boolean canEdit,
        String lockedBy,
        Instant lockExpiresAt,
        Set<UUID> editorIds
) {
}
