package com.narrativeplatform.app.chronicle.models.responses;

import java.time.Instant;
import java.util.UUID;

public record GeneratedStoryResponse(UUID id, int version, String title, String content, String model, Instant createdAt) {
}
