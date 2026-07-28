package com.narrativeplatform.app.chronicle.models.requests;

import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public record UpdateEditorsRequest(@NotNull Set<UUID> editorIds) {
}
