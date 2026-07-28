package com.narrativeplatform.app.chronicle.models.responses;

import com.narrativeplatform.app.chronicle.models.enums.ChronicleStatusType;
import com.narrativeplatform.app.chronicle.models.enums.ChronicleType;
import java.time.Instant;
import java.util.UUID;

public record ChronicleCardResponse(UUID id, ChronicleType type, ChronicleStatusType status, String title, String preview, String creatorName, Instant updatedAt, boolean published) {
}
