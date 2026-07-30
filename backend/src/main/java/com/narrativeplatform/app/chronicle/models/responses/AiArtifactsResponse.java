package com.narrativeplatform.app.chronicle.models.responses;

import com.narrativeplatform.app.canon.models.responses.CanonMapResponse;
import com.narrativeplatform.app.chronicle.models.enums.ChronicleStatusType;

public record AiArtifactsResponse(
        boolean aiConfigured,
        AdaptationArtifactResponse adaptation,
        CanonMapResponse canonMap,
        ChronicleSynopsisResponse synopsis
) {
    public record AdaptationArtifactResponse(ChronicleStatusType status, GeneratedStoryResponse currentStory) {
    }
}
