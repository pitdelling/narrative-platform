package com.narrativeplatform.app.canon.models.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateAiTagSettingsRequest(@NotNull @Valid List<TagSettingItemRequest> settings) {
}
