package com.narrativeplatform.app.canon.services;

import com.narrativeplatform.app.aijob.models.enums.AiJobType;
import com.narrativeplatform.app.aijob.services.AiJobService;
import com.narrativeplatform.app.auth.models.entities.UserEntity;
import com.narrativeplatform.app.canon.models.entities.CanonCategoryEntity;
import com.narrativeplatform.app.canon.models.entities.CanonMapGenerationCategoryEntity;
import com.narrativeplatform.app.canon.models.entities.CanonMapGenerationEntity;
import com.narrativeplatform.app.canon.repositories.CanonCategoryRepository;
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
    private CanonCategoryRepository canonCategoryRepository;
    @Mock
    private AiJobService aiJobService;

    private CanonMapGenerationService service;
    private ChronicleEntity chronicle;
    private PartyEntity party;

    @BeforeEach
    void setUp() {
        service = new CanonMapGenerationService(
                canonMapGenerationRepository, canonMapGenerationCategoryRepository,
                canonTagRepository, canonTagSourceRepository, canonCategoryRepository, aiJobService
        );
        party = new PartyEntity("Test Party", "test-party", null, null, null);
        party.setId(UUID.randomUUID());
        final var creator = new UserEntity("author", "Author", "hash");
        creator.setId(UUID.randomUUID());
        chronicle = new ChronicleEntity(party, creator, ChronicleType.GAME, ChronicleStatusType.AI_PENDING, "Title");
        chronicle.setId(UUID.randomUUID());
    }

    @Test
    void enqueueGenerationSnapshotsTheCurrentCategoriesAndEnqueuesTheJob() {
        final var categories = List.of(new CanonCategoryEntity(party, "Pessoas", "Gente da história.", "#7665a7", 0));
        when(canonCategoryRepository.findAllByPartyIdOrderByDisplayOrderAsc(party.getId())).thenReturn(categories);
        when(canonMapGenerationRepository.countByChronicleId(chronicle.getId())).thenReturn(0L);
        when(canonMapGenerationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.enqueueGeneration(chronicle);

        final var categoryCaptor = ArgumentCaptor.forClass(List.class);
        verify(canonMapGenerationCategoryRepository).saveAll(categoryCaptor.capture());
        final List<CanonMapGenerationCategoryEntity> savedCategories = categoryCaptor.getValue();
        assertEquals(1, savedCategories.size());
        assertEquals("Pessoas", savedCategories.getFirst().getName());
        assertEquals("#7665a7", savedCategories.getFirst().getColor());
        verify(aiJobService).enqueueAutomatic(chronicle, AiJobType.CANON_MAP_GENERATION, chronicle.getCreator());
    }

    @Test
    void mutatingEditingOrDeletingTheLiveCategoryAfterwardsDoesNotAffectTheAlreadyPersistedSnapshot() {
        final var liveCategory = new CanonCategoryEntity(party, "Pessoas", "Gente da história.", "#7665a7", 0);
        when(canonCategoryRepository.findAllByPartyIdOrderByDisplayOrderAsc(party.getId())).thenReturn(List.of(liveCategory));
        when(canonMapGenerationRepository.countByChronicleId(chronicle.getId())).thenReturn(0L);
        when(canonMapGenerationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        final var categoryCaptor = ArgumentCaptor.forClass(List.class);

        service.enqueueGeneration(chronicle);
        verify(canonMapGenerationCategoryRepository).saveAll(categoryCaptor.capture());
        final CanonMapGenerationCategoryEntity snapshot = (CanonMapGenerationCategoryEntity) categoryCaptor.getValue().getFirst();

        liveCategory.setColor("#000000");
        liveCategory.setName("Renomeado");

        assertEquals("#7665a7", snapshot.getColor());
        assertEquals("Pessoas", snapshot.getName());
    }

    @Test
    void generationCreatesAVersionOneMoreThanTheExistingCount() {
        when(canonCategoryRepository.findAllByPartyIdOrderByDisplayOrderAsc(any())).thenReturn(List.of());
        when(canonMapGenerationRepository.countByChronicleId(chronicle.getId())).thenReturn(2L);
        final var generationCaptor = ArgumentCaptor.forClass(CanonMapGenerationEntity.class);
        when(canonMapGenerationRepository.save(generationCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        service.enqueueGeneration(chronicle);

        assertEquals(3, generationCaptor.getValue().getVersionNumber());
    }

    @Test
    void enqueueGenerationWithNoLiveCategoriesStillCreatesTheGeneration() {
        when(canonCategoryRepository.findAllByPartyIdOrderByDisplayOrderAsc(party.getId())).thenReturn(List.of());
        when(canonMapGenerationRepository.countByChronicleId(chronicle.getId())).thenReturn(0L);
        when(canonMapGenerationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> service.enqueueGeneration(chronicle));

        verify(canonMapGenerationCategoryRepository).saveAll(List.of());
        verify(aiJobService).enqueueAutomatic(chronicle, AiJobType.CANON_MAP_GENERATION, chronicle.getCreator());
    }
}
