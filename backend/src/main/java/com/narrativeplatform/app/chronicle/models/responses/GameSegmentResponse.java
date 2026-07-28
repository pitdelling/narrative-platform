package com.narrativeplatform.app.chronicle.models.responses;

import com.narrativeplatform.app.chronicle.models.enums.SegmentSizeType;
import com.narrativeplatform.app.chronicle.models.enums.SegmentStatusType;
import java.time.Instant;
import java.util.UUID;

public record GameSegmentResponse(UUID id, int sequenceNumber, short cycleNumber, UUID authorId, String author, SegmentStatusType status, String disabledReason, boolean visible, String content, SegmentSizeType size, Instant submittedAt) {
}
