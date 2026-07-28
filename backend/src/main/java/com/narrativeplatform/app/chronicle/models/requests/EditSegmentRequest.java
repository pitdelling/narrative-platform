package com.narrativeplatform.app.chronicle.models.requests;

import com.narrativeplatform.shared.constants.DomainConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EditSegmentRequest(
        @NotBlank @Size(max = DomainConstants.MAX_SEGMENT_LENGTH) String content,
        @Size(max = 600) String reason
) {
}
