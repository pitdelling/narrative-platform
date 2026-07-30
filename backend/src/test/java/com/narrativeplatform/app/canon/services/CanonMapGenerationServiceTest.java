package com.narrativeplatform.app.canon.services;

import com.narrativeplatform.app.aijob.models.enums.AiJobType;
import com.narrativeplatform.app.aijob.services.AiJobService;
import com.narrativeplatform.app.auth.models.entities.UserEntity;
import com.narrativeplatform.app.canon.models.entities.CanonMapGenerationCategoryEntity;
import com.narrativeplatform.app.canon.models.entities.CanonMapGenerationEntity;
import com.narrativeplatform.app.canon.models.entities.PartyAiTagSettingEntity;
import com.narrativeplatform.app.canon.models.enums.CanonCategoryType;
import com.narrativeplatform.app.canon.models.enums.TagColorType;
import com.narrativeplatform.app.canon.repositories.CanonMapGenerationCategoryRepository;
import com.narrativeplatform.app.canon.repositories.CanonMapGenerationRepository;
import com.narrativeplatform.app.canon.repositories.CanonTagRepository;
import com.narrativeplatform.app.canon.repositories.CanonTagSourceRepository;
import com.narrativeplatform.app.chronicle.models.entities.ChronicleEntity;
import com.narrativeplatform.app.chronicle.models.enums.ChronicleStatusType;
import com.narrativeplatform.app.chronicle.models.enums.ChronicleType;
import com.narrativeplatform.app.party.models.entities.PartyEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CanonMapGenerationServiceTest {
    @Mock
    private CanonMapGenerationRepository canonMapGenerationRepository;
    @Mock
    private CanonMapGenerationCategoryRepository canonMapGenerationCategoryRepository;
    @Mock
    private CanonTagRepository canonTagRepository;
    @Mock
    private CanonTagSourceRepository canonTagSourceRepository;
    @Mock
    private PartyAiTagSettingsService partyAiTagSettingsService;
    @Mock
    private AiJobService aiJobService;

    private CanonMapGenerationService service;
    private ChronicleEntity chronicle;
    private PartyEntity party;

    @BeforeEach
    void setUp() {
        service = new CanonMapGenerationService(
                canonMapGenerationRepository, canonMapGenerationCategoryRepository,
                canonTagRepository, canonTagSourceRepository, partyAiTagSettingsService, aiJobService
        );
        party = new PartyEntity("Test Party", "test-party", null, null, null);
        party.setId(UUID.randomUUID());
        final var creator = new UserEntity("author", "Author", "hash");
        creator.setId(UUID.randomUUID());
        chronicle = new ChronicleEntity(party, creator, ChronicleType.GAME, ChronicleStatusType.AI_PENDING, "Title");
        chronicle.setId(UUID.randomUUID());
    }

    @Test
    void enqueueGenerationSnapshotsTheCurrentSettingsAndEnqueuesTheJob() {
        final var settings = List.of(new PartyAiTagSettingEntity(party, CanonCategoryType.PERSON, true, TagColorType.VIOLET, 1));
        when(partyAiTagSettingsService.getOrCreateDefaults(party.getId())).thenReturn(settings);
        when(canonMapGenerationRepository.countByChronicleId(chronicle.getId())).thenReturn(0L);
        when(canonMapGenerationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.enqueueGeneration(chronicle);

        final var categoryCaptor = ArgumentCaptor.forClass(List.class);
        verify(canonMapGenerationCategoryRepository).saveAll(categoryCaptor.capture());
        final List<CanonMapGenerationCategoryEntity> savedCategories = categoryCaptor.getValue();
        assertEquals(1, savedCategories.size());
        assertEquals(TagColorType.VIOLET, savedCategories.getFirst().getColor());
        verify(aiJobService).enqueueAutomatic(chronicle, AiJobType.CANON_MAP_GENERATION, chronicle.getCreator());
    }

    @Test
    void mutatingTheLiveSettingAfterwardsDoesNotAffectTheAlreadyPersistedSnapshot() {
        final var liveSetting = new PartyAiTagSettingEntity(party, CanonCategoryType.PERSON, true, TagColorType.VIOLET, 1);
        when(partyAiTagSettingsService.getOrCreateDefaults(party.getId())).thenReturn(List.of(liveSetting));
        when(canonMapGenerationRepository.countByChronicleId(chronicle.getId())).thenReturn(0L);
        when(canonMapGenerationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        final var categoryCaptor = ArgumentCaptor.forClass(List.class);

        service.enqueueGeneration(chronicle);
        verify(canonMapGenerationCategoryRepository).saveAll(categoryCaptor.capture());
        final CanonMapGenerationCategoryEntity snapshot = (CanonMapGenerationCategoryEntity) categoryCaptor.getValue().getFirst();

        liveSetting.setColor(TagColorType.SLATE);
        liveSetting.setEnabled(false);

        assertEquals(TagColorType.VIOLET, snapshot.getColor());
        assertTrue(snapshot.isEnabled());
    }

    @Test
    void generationCreatesAVersionOneMoreThanTheExistingCount() {
        when(partyAiTagSettingsService.getOrCreateDefaults(any())).thenReturn(List.of());
        when(canonMapGenerationRepository.countByChronicleId(chronicle.getId())).thenReturn(2L);
        final var generationCaptor = ArgumentCaptor.forClass(CanonMapGenerationEntity.class);
        when(canonMapGenerationRepository.save(generationCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        service.enqueueGeneration(chronicle);

        assertEquals(3, generationCaptor.getValue().getVersionNumber());
    }
}
