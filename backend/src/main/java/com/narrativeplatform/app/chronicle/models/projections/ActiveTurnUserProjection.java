package com.narrativeplatform.app.chronicle.models.projections;

import java.util.UUID;

public interface ActiveTurnUserProjection {
    UUID getChronicleId();

    UUID getUserId();
}
