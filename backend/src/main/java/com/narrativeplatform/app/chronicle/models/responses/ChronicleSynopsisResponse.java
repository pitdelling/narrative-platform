package com.narrativeplatform.app.chronicle.models.responses;

import com.narrativeplatform.app.chronicle.models.enums.SynopsisStatusType;

import java.time.Instant;

public record ChronicleSynopsisResponse(SynopsisStatusType status, String content, Instant completedAt, String errorMessage) {
}
