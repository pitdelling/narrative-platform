package com.narrativeplatform.app.chronicle.models.requests;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGameChronicleRequest(
        @NotBlank @Size(max = 160) String title,
        @Min(1) @Max(3) short cycleCount,
        @NotBlank @Size(max = 10000) String initialContent
) {
}
