package com.narrativeplatform.app.chronicle.models.requests;

import com.narrativeplatform.shared.constants.DomainConstants;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaveGameDraftRequest(
        @NotNull @Size(max = DomainConstants.MAX_SEGMENT_LENGTH) String content
) {
}
