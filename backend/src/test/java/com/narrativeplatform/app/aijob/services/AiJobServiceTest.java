package com.narrativeplatform.app.aijob.services;

import com.narrativeplatform.app.aijob.models.enums.AiJobStatusType;
import com.narrativeplatform.app.aijob.models.enums.AiJobType;
import com.narrativeplatform.app.aijob.repositories.AiJobRepository;
import com.narrativeplatform.app.auth.models.entities.UserEntity;
import com.narrativeplatform.app.chronicle.models.entities.ChronicleEntity;
import com.narrativeplatform.app.chronicle.models.enums.ChronicleStatusType;
import com.narrativeplatform.app.chronicle.models.enums.ChronicleType;
import com.narrativeplatform.app.party.models.entities.PartyEntity;
import com.narrativeplatform.shared.exceptions.AiNotConfiguredException;
import com.narrativeplatform.shared.exceptions.ConflictException;
import com.narrativeplatform.shared.integrations.OpenAiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiJobServiceTest {
    @Mock
    private AiJobRepository aiJobRepository;
    @Mock
    private OpenAiClient openAiClient;

    private AiJobService aiJobService;
    private ChronicleEntity chronicle;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        aiJobService = new AiJobService(aiJobRepository, openAiClient);
        final var party = new PartyEntity("Test Party", "test-party", null, null, null);
        user = new UserEntity("author", "Author", "hash");
        user.setId(UUID.randomUUID());
        chronicle = new ChronicleEntity(party, user, ChronicleType.GAME, ChronicleStatusType.IN_PROGRESS, "Title");
        chronicle.setId(UUID.randomUUID());
    }

    @Test
    void enqueueAutomaticIsSilentNoOpWhenAJobOfThatTypeIsAlreadyActive() {
        when(aiJobRepository.existsByChronicleIdAndJobTypeAndStatusIn(eq(chronicle.getId()), eq(AiJobType.CANON_MAP_GENERATION), any()))
                .thenReturn(true);

        assertDoesNotThrow(() -> aiJobService.enqueueAutomatic(chronicle, AiJobType.CANON_MAP_GENERATION, user));

        verify(aiJobRepository, never()).save(any());
        assertEquals(ChronicleStatusType.IN_PROGRESS, chronicle.getStatus());
    }

    @Test
    void enqueueAutomaticNeverTouchesChronicleStatus() {
        when(aiJobRepository.existsByChronicleIdAndJobTypeAndStatusIn(any(), any(), any())).thenReturn(false);

        aiJobService.enqueueAutomatic(chronicle, AiJobType.STORY_SYNOPSIS_GENERATION, user);

        verify(aiJobRepository).save(any());
        assertEquals(ChronicleStatusType.IN_PROGRESS, chronicle.getStatus());
    }

    @Test
    void enqueueStillThrowsWhenAnAdaptationJobIsAlreadyActive() {
        when(aiJobRepository.existsByChronicleIdAndJobTypeAndStatusIn(chronicle.getId(), AiJobType.STORY_ADAPTATION_GENERATION,
                Set.of(AiJobStatusType.PENDING, AiJobStatusType.PROCESSING))).thenReturn(true);

        assertThrows(ConflictException.class, () -> aiJobService.enqueue(chronicle, user));
    }

    @Test
    void enqueueSetsChronicleToAiPending() {
        when(aiJobRepository.existsByChronicleIdAndJobTypeAndStatusIn(any(), any(), any())).thenReturn(false);

        aiJobService.enqueue(chronicle, user);

        assertEquals(ChronicleStatusType.AI_PENDING, chronicle.getStatus());
    }

    @Test
    void enqueueRequestedThrowsWhenAiIsNotConfigured() {
        doThrow(new AiNotConfiguredException()).when(openAiClient).requireConfigured();

        assertThrows(AiNotConfiguredException.class, () -> aiJobService.enqueueRequested(chronicle, user));
        verifyNoInteractions(aiJobRepository);
    }
}
