package com.narrativeplatform.app.storyvote.models.projections;

import java.time.Instant;
import java.util.UUID;

public interface PublishedChronicleProjection {
    UUID getChronicleId();

    Instant getPublishedAt();
}
