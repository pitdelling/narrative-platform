package com.narrativeplatform.app.storyvote.models.responses;

import java.time.LocalDate;
import java.util.List;

public record DailyStoryVoteStateResponse(
        LocalDate dateUtc,
        int dailyLimit,
        int usedUnits,
        int remainingUnits,
        List<StoryVoteAllocationResponse> allocations
) {
}
