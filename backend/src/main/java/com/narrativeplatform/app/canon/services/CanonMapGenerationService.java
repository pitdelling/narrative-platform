package com.narrativeplatform.app.canon.services;

import com.narrativeplatform.app.aijob.models.enums.AiJobType;
import com.narrativeplatform.app.aijob.services.AiJobService;
import com.narrativeplatform.app.canon.models.entities.CanonMapGenerationCategoryEntity;
import com.narrativeplatform.app.canon.models.entities.CanonMapGenerationEntity;
import com.narrativeplatform.app.canon.models.entities.CanonTagEntity;
import com.narrativeplatform.app.canon.models.enums.CanonCategoryType;
import com.narrativeplatform.app.canon.models.responses.CanonCategoryResponse;
import com.narrativeplatform.app.canon.models.responses.CanonMapResponse;
import com.narrativeplatform.app.canon.models.responses.CanonTagResponse;
import com.narrativeplatform.app.canon.repositories.CanonMapGenerationCategoryRepository;
import com.narrativeplatform.app.canon.repositories.CanonMapGenerationRepository;
import com.narrativeplatform.app.canon.repositories.CanonTagRepository;
import com.narrativeplatform.app.canon.repositories.CanonTagSourceRepository;
import com.narrativeplatform.app.chronicle.models.entities.ChronicleEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CanonMapGenerationService {
    private final CanonMapGenerationRepository canonMapGenerationRepository;
    private final CanonMapGenerationCategoryRepository canonMapGenerationCategoryRepository;
    private final CanonTagRepository canonTagRepository;
    private final CanonTagSourceRepository canonTagSourceRepository;
    private final PartyAiTagSettingsService partyAiTagSettingsService;
    private final AiJobService aiJobService;

    @Transactional
    public void enqueueGeneration(final ChronicleEntity chronicle) {
        final var settings = partyAiTagSettingsService.getOrCreateDefaults(chronicle.getParty().getId());
        final var version = Math.toIntExact(canonMapGenerationRepository.countByChronicleId(chronicle.getId()) + 1);
        final var generation = canonMapGenerationRepository.save(new CanonMapGenerationEntity(chronicle, version));
        final var categorySnapshots = settings.stream()
                .map(setting -> new CanonMapGenerationCategoryEntity(
                        generation, setting.getCategory(), setting.isEnabled(), setting.getColor(), setting.getDisplayOrder()
                ))
                .toList();
        canonMapGenerationCategoryRepository.saveAll(categorySnapshots);
        aiJobService.enqueueAutomatic(chronicle, AiJobType.CANON_MAP_GENERATION, chronicle.getCreator());
    }

    public CanonMapResponse getForChronicle(final UUID chronicleId) {
        final var generation = canonMapGenerationRepository.findFirstByChronicleIdOrderByVersionNumberDesc(chronicleId).orElse(null);
        if (generation == null) {
            return null;
        }
        final var categorySnapshots = canonMapGenerationCategoryRepository.findAllByGenerationIdOrderByDisplayOrderAsc(generation.getId());
        final var tags = canonTagRepository.findAllByGenerationIdOrderByCategoryAscDisplayOrderAsc(generation.getId());
        final var positionsByTagId = new HashMap<UUID, List<Integer>>();
        for (final var position : canonTagSourceRepository.findPositionsByGenerationId(generation.getId())) {
            positionsByTagId.computeIfAbsent(position.getTagId(), key -> new ArrayList<>()).add(position.getSequenceNumber());
        }
        positionsByTagId.values().forEach(Collections::sort);

        final Map<CanonCategoryType, List<CanonTagEntity>> tagsByCategory = tags.stream()
                .collect(Collectors.groupingBy(CanonTagEntity::getCategory, LinkedHashMap::new, Collectors.toList()));

        final var categories = categorySnapshots.stream()
                .map(snapshot -> new CanonCategoryResponse(
                        snapshot.getCategory(), snapshot.isEnabled(), snapshot.getColor(), snapshot.getDisplayOrder(),
                        tagsByCategory.getOrDefault(snapshot.getCategory(), List.of()).stream()
                                .map(tag -> new CanonTagResponse(
                                        tag.getId(), tag.getName(), tag.getSummary(), tag.getVisualDescription(),
                                        tag.getPersonalityDescription(), tag.getVisualBasis(), tag.getPersonalityBasis(),
                                        positionsByTagId.getOrDefault(tag.getId(), List.of())
                                ))
                                .toList()
                ))
                .toList();

        return new CanonMapResponse(generation.getStatus(), generation.getCompletedAt(), generation.getErrorMessage(), categories);
    }
}
