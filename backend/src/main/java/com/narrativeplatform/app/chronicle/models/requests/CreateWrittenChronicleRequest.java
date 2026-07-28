package com.narrativeplatform.app.chronicle.models.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record CreateWrittenChronicleRequest(
        @NotBlank @Size(max = 160) String title,
        Set<UUID> editorIds
) {
}
