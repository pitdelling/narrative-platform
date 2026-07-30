package com.narrativeplatform.app.canon.models.requests;

import com.narrativeplatform.app.canon.models.enums.CanonCategoryType;
import com.narrativeplatform.app.canon.models.enums.TagColorType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TagSettingItemRequest(
        @NotNull CanonCategoryType category,
        boolean enabled,
        @NotNull TagColorType color,
        @Min(0) int displayOrder
) {
}
