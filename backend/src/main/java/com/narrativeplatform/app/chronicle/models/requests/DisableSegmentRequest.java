package com.narrativeplatform.app.chronicle.models.requests;

import jakarta.validation.constraints.Size;

public record DisableSegmentRequest(@Size(max = 600) String reason) {
}
