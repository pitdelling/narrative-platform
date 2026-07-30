package com.narrativeplatform.app.chronicle.services;

import com.narrativeplatform.app.aijob.services.AiJobService;
import com.narrativeplatform.app.auth.models.entities.UserEntity;
import com.narrativeplatform.app.auth.repositories.UserRepository;
import com.narrativeplatform.app.canon.services.CanonMapGenerationService;
import com.narrativeplatform.app.chronicle.models.entities.ChronicleEntity;
import com.narrativeplatform.app.chronicle.models.entities.GameRunEntity;
import com.narrativeplatform.app.chronicle.models.entities.GameTurnEntity;
import com.narrativeplatform.app.chronicle.models.enums.ChronicleStatusType;
import com.narrativeplatform.app.chronicle.models.enums.ChronicleType;
import com.narrativeplatform.app.chronicle.models.enums.GameRunStatusType;
import com.narrativeplatform.app.chronicle.models.enums.GameTurnStatusType;
import com.narrativeplatform.app.chronicle.models.requests.PublishGameSegmentRequest;
import com.narrativeplatform.app.chronicle.repositories.*;
import com.narrativeplatform.app.party.models.entities.PartyEntity;
import com.narrativeplatform.app.party.models.entities.PartyMemberEntity;
import com.narrativeplatform.app.party.models.enums.MemberStatusType;
import com.narrativeplatform.app.party.models.enums.PartyRoleType;
import com.narrativeplatform.app.party.repositories.PartyMemberRepository;
import com.narrativeplatform.app.party.services.PartyAccessService;
import com.narrativeplatform.configuration.AppProperties;
import com.narrativeplatform.security.AuthenticatedUser;
import com.narrativeplatform.security.CurrentUserService;
import com.narrativeplatform.shared.exceptions.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameChronicleServiceTest {
    @Mock
    private ChronicleRepository chronicleRepository;
    @Mock
    private GameRunRepository gameRunRepository;
    @Mock
    private GameTurnRepository gameTurnRepository;
    @Mock
    private GameDraftRepository gameDraftRepository;
    @Mock
    private GameSegmentRepository gameSegmentRepository;
    @Mock
    private GeneratedStoryRepository generatedStoryRepository;
    @Mock
    private SegmentRevisionRepository segmentRevisionRepository;
    @Mock
    private PartyMemberRepository partyMemberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ChronicleAccessService chronicleAccessService;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private AiJobService aiJobService;
    @Mock
    private CanonMapGenerationService canonMapGenerationService;
    @Mock
    private ChronicleSynopsisService chronicleSynopsisService;

    private PartyAccessService partyAccessService;
    private GameChronicleService service;

    private UUID partyId;
    private UUID chronicleId;
    private ChronicleEntity chronicle;
    private GameRunEntity run;
    private GameTurnEntity lastTurn;
    private UserEntity player;

    @BeforeEach
    void setUp() {
        partyAccessService = new PartyAccessService(partyMemberRepository, currentUserService);
        final var properties = new AppProperties(
                "http://localhost:3000", "http://localhost:3000", "test-secret",
                168, 24, 24, 10,
                new AppProperties.OpenAi("", "gpt-5-mini", "https://api.openai.com/v1")
        );
        service = new GameChronicleService(
                chronicleRepository, gameRunRepository, gameTurnRepository, gameDraftRepository,
                gameSegmentRepository, generatedStoryRepository, segmentRevisionRepository,
                partyMemberRepository, userRepository, partyAccessService, chronicleAccessService,
                currentUserService, properties, aiJobService, canonMapGenerationService, chronicleSynopsisService
        );

        partyId = UUID.randomUUID();
        chronicleId = UUID.randomUUID();
        final var party = new PartyEntity("Test Party", "test-party", null, null, null);
        party.setId(partyId);
        player = new UserEntity("player", "Player", "hash");
        player.setId(UUID.randomUUID());
        chronicle = new ChronicleEntity(party, player, ChronicleType.GAME, ChronicleStatusType.IN_PROGRESS, "Title");
        chronicle.setId(chronicleId);

        run = new GameRunEntity(chronicle, (short) 1, 1);
        run.setId(UUID.randomUUID());
        run.setCurrentSequence(1);

        lastTurn = new GameTurnEntity(run, player, (short) 1, 1, 1);
        lastTurn.setId(UUID.randomUUID());
        lastTurn.setStatus(GameTurnStatusType.ACTIVE);
        lastTurn.setStartedAt(Instant.now());
        lastTurn.setExpiresAt(Instant.now().plusSeconds(3600));

        final var membership = new PartyMemberEntity(party, player, PartyRoleType.PLAYER, MemberStatusType.ACTIVE);
        when(chronicleAccessService.requireMember(partyId, chronicleId))
                .thenReturn(new ChronicleAccessService.AccessContext(chronicle, membership));
        when(currentUserService.require()).thenReturn(new AuthenticatedUser(player.getId(), player.getUsername()));
    }

    @Test
    void publishingTheLastTurnCompletesTheRunAndEnqueuesAllThreeArtifactsExactlyOnce() {
        when(gameRunRepository.findForUpdateByChronicleId(chronicleId)).thenReturn(Optional.of(run));
        when(gameTurnRepository.findByRunIdAndSequenceNumber(run.getId(), 1)).thenReturn(Optional.of(lastTurn));
        when(gameTurnRepository.findByRunIdAndSequenceNumber(run.getId(), 2)).thenReturn(Optional.empty());

        service.publish(partyId, chronicleId, new PublishGameSegmentRequest("The end of the tale."));

        assertEquals(GameRunStatusType.COMPLETED, run.getStatus());
        assertEquals(ChronicleStatusType.AI_PENDING, chronicle.getStatus());
        verify(aiJobService, times(1)).enqueue(chronicle, chronicle.getCreator());
        verify(canonMapGenerationService, times(1)).enqueueGeneration(chronicle);
        verify(chronicleSynopsisService, times(1)).enqueueGeneration(chronicle);
    }

    @Test
    void aRetriedPublishAfterCompletionNeverReachesCompleteRunAgain() {
        when(gameRunRepository.findForUpdateByChronicleId(chronicleId)).thenReturn(Optional.of(run));
        when(gameTurnRepository.findByRunIdAndSequenceNumber(run.getId(), 1)).thenReturn(Optional.of(lastTurn));
        when(gameTurnRepository.findByRunIdAndSequenceNumber(run.getId(), 2)).thenReturn(Optional.empty());

        service.publish(partyId, chronicleId, new PublishGameSegmentRequest("The end of the tale."));
        reset(aiJobService, canonMapGenerationService, chronicleSynopsisService);

        assertThrows(ConflictException.class,
                () -> service.publish(partyId, chronicleId, new PublishGameSegmentRequest("A retried duplicate submission.")));
        verifyNoInteractions(aiJobService, canonMapGenerationService, chronicleSynopsisService);
    }
}
