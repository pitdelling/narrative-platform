package com.narrativeplatform.app.chronicle.services;

import com.narrativeplatform.app.canon.services.CanonMapGenerationService;
import com.narrativeplatform.app.chronicle.models.enums.ChronicleType;
import com.narrativeplatform.app.chronicle.models.responses.AiArtifactsResponse;
import com.narrativeplatform.shared.exceptions.BadRequestException;
import com.narrativeplatform.shared.integrations.OpenAiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiArtifactsQueryService {
    private final ChronicleAccessService chronicleAccessService;
    private final CanonMapGenerationService canonMapGenerationService;
    private final ChronicleSynopsisService chronicleSynopsisService;
    private final OpenAiClient openAiClient;

    public AiArtifactsResponse get(final UUID partyId, final UUID chronicleId) {
        final var context = chronicleAccessService.requireMember(partyId, chronicleId);
        final var chronicle = context.chronicle();
        if (chronicle.getType() != ChronicleType.GAME) {
            throw new BadRequestException("This chronicle is not a game chronicle.");
        }
        final var adaptation = new AiArtifactsResponse.AdaptationArtifactResponse(
                chronicle.getStatus(),
                chronicle.getCurrentGeneratedStory() == null ? null : chronicle.getCurrentGeneratedStory().toResponse()
        );
        return new AiArtifactsResponse(
                openAiClient.configured(),
                adaptation,
                canonMapGenerationService.getForChronicle(chronicleId),
                chronicleSynopsisService.getForChronicle(chronicleId)
        );
    }
}
