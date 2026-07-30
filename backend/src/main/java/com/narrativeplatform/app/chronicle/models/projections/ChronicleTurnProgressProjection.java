package com.narrativeplatform.app.chronicle.models.projections;

import java.util.UUID;

public interface ChronicleTurnProgressProjection {
    UUID getChronicleId();

    long getCompletedTurns();

    long getTotalTurns();
}
