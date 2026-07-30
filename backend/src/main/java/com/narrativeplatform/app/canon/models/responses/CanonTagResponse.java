package com.narrativeplatform.app.canon.models.responses;

import com.narrativeplatform.app.canon.models.enums.TagBasisType;

import java.util.List;
import java.util.UUID;

public record CanonTagResponse(
        UUID id,
        String name,
        String summary,
        String visualDescription,
        String personalityDescription,
        TagBasisType visualBasis,
        TagBasisType personalityBasis,
        List<Integer> sourceSegmentPositions
) {
}
