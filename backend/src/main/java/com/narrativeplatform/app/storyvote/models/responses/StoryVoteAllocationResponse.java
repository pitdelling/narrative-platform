package com.narrativeplatform.app.storyvote.models.responses;

import java.util.UUID;

public record StoryVoteAllocationResponse(
        UUID chronicleId,
        int units
) {
}
