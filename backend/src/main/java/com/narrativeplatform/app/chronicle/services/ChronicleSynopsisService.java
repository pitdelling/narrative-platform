package com.narrativeplatform.app.chronicle.services;

import com.narrativeplatform.app.aijob.models.enums.AiJobType;
import com.narrativeplatform.app.aijob.services.AiJobService;
import com.narrativeplatform.app.chronicle.models.entities.ChronicleEntity;
import com.narrativeplatform.app.chronicle.models.entities.ChronicleSynopsisEntity;
import com.narrativeplatform.app.chronicle.models.enums.SynopsisStatusType;
import com.narrativeplatform.app.chronicle.models.responses.ChronicleSynopsisResponse;
import com.narrativeplatform.app.chronicle.repositories.ChronicleSynopsisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChronicleSynopsisService {
    private final ChronicleSynopsisRepository chronicleSynopsisRepository;
    private final AiJobService aiJobService;

    @Transactional
    public void enqueueGeneration(final ChronicleEntity chronicle) {
        final var version = Math.toIntExact(chronicleSynopsisRepository.countByChronicleId(chronicle.getId()) + 1);
        chronicleSynopsisRepository.save(new ChronicleSynopsisEntity(chronicle, version));
        aiJobService.enqueueAutomatic(chronicle, AiJobType.STORY_SYNOPSIS_GENERATION, chronicle.getCreator());
    }

    @Transactional
    public boolean enqueueGenerationIfIdle(final ChronicleEntity chronicle) {
        final var latest = chronicleSynopsisRepository.findFirstByChronicleIdOrderByVersionNumberDesc(chronicle.getId()).orElse(null);
        if (latest != null && (latest.getStatus() == SynopsisStatusType.PENDING || latest.getStatus() == SynopsisStatusType.PROCESSING)) {
            return false;
        }
        enqueueGeneration(chronicle);
        return true;
    }

    public ChronicleSynopsisResponse getForChronicle(final UUID chronicleId) {
        final var synopsis = chronicleSynopsisRepository.findFirstByChronicleIdOrderByVersionNumberDesc(chronicleId).orElse(null);
        if (synopsis == null) {
            return null;
        }
        return new ChronicleSynopsisResponse(synopsis.getStatus(), synopsis.getContent(), synopsis.getCompletedAt(), synopsis.getErrorMessage());
    }
}
