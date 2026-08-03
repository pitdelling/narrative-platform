package com.narrativeplatform.app.storyvote.models.projections;

import java.util.UUID;

public interface StoryVoteTotalProjection {
    UUID getChronicleId();

    long getTotalVotes();
}
