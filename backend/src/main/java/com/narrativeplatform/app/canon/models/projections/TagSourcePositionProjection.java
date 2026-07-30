package com.narrativeplatform.app.canon.models.projections;

import java.util.UUID;

public interface TagSourcePositionProjection {
    UUID getTagId();

    int getSequenceNumber();
}
