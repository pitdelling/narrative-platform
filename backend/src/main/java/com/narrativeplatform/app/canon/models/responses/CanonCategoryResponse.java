package com.narrativeplatform.app.canon.models.responses;

import com.narrativeplatform.app.canon.models.enums.CanonCategoryType;
import com.narrativeplatform.app.canon.models.enums.TagColorType;

import java.util.List;

public record CanonCategoryResponse(
        CanonCategoryType category,
        boolean enabled,
        TagColorType color,
        int displayOrder,
        List<CanonTagResponse> tags
) {
}
