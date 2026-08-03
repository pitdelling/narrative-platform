package com.narrativeplatform.app.storyvote.models.responses;

import java.util.UUID;

public record StoryVoteSummaryResponse(
        UUID chronicleId,
        long totalVotes,
        Long rank,
        int currentUserVotesToday,
        int currentUserRemainingVotesToday,
        boolean canVote,
        String reason
) {
}
