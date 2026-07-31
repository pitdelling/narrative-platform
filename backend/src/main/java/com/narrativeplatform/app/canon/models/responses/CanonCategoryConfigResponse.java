package com.narrativeplatform.app.canon.models.responses;

import java.util.UUID;

public record CanonCategoryConfigResponse(
        UUID id,
        String name,
        String description,
        String color,
        int displayOrder
) {
}
