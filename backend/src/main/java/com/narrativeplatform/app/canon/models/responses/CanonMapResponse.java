package com.narrativeplatform.app.canon.models.responses;

import com.narrativeplatform.app.canon.models.enums.CanonMapStatusType;

import java.time.Instant;
import java.util.List;

public record CanonMapResponse(
        CanonMapStatusType status,
        Instant completedAt,
        String errorMessage,
        List<CanonCategoryResponse> categories
) {
}
