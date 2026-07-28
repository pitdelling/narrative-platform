package com.narrativeplatform.app.chronicle.models.requests;

import com.narrativeplatform.shared.constants.DomainConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublishGameSegmentRequest(
        @NotBlank @Size(max = DomainConstants.MAX_SEGMENT_LENGTH) String content
) {
}
