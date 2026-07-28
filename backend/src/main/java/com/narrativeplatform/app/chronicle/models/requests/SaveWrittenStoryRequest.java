package com.narrativeplatform.app.chronicle.models.requests;

import com.narrativeplatform.shared.constants.DomainConstants;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaveWrittenStoryRequest(
        @NotNull @Size(max = DomainConstants.MAX_WRITTEN_STORY_LENGTH) String content,
        @Min(0) long expectedVersion
) {
}
