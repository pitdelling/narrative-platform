package com.narrativeplatform.app.chronicle.services;

import com.narrativeplatform.app.auth.models.entities.UserEntity;
import com.narrativeplatform.app.chronicle.models.entities.ChronicleEntity;
import com.narrativeplatform.app.chronicle.models.enums.ChronicleStatusType;
import com.narrativeplatform.app.chronicle.models.enums.ChronicleType;
import com.narrativeplatform.app.chronicle.repositories.ChronicleRepository;
import com.narrativeplatform.app.chronicle.repositories.GameTurnRepository;
import com.narrativeplatform.app.party.models.entities.PartyEntity;
import com.narrativeplatform.app.party.services.PartyAccessService;
import com.narrativeplatform.security.AuthenticatedUser;
import com.narrativeplatform.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * {@code publishedAt} was added to {@link com.narrativeplatform.app.chronicle.models.responses.ChronicleCardResponse}
 * to give the frontend a real completion timestamp for "newest/oldest" sorting (Console "Lista, ranking e badges") —
 * this is the first test written for {@link ChronicleService#list(UUID)}, which had no coverage before.
 */
@ExtendWith(MockitoExtension.class)
class ChronicleServiceTest {
    @Mock
    private ChronicleRepository chronicleRepository;
    @Mock
    private GameTurnRepository gameTurnRepository;
    @Mock
    private PartyAccessService partyAccessService;
    @Mock
    private ChronicleAccessService chronicleAccessService;
    @Mock
    private CurrentUserService currentUserService;

    private ChronicleService service;
    private PartyEntity party;
    private UserEntity creator;
    private UUID partyId;

    @BeforeEach
    void setUp() {
        service = new ChronicleService(chronicleRepository, gameTurnRepository, partyAccessService, chronicleAccessService, currentUserService);

        partyId = UUID.randomUUID();
        creator = new UserEntity("author", "Author", "hash");
        creator.setId(UUID.randomUUID());
        party = new PartyEntity("Test Party", "test-party", null, null, creator);
        party.setId(partyId);

        when(currentUserService.require()).thenReturn(new AuthenticatedUser(creator.getId(), creator.getUsername()));
    }

    @Test
    void publishedChronicleCardIncludesPublishedAt() {
        final var chronicle = new ChronicleEntity(party, creator, ChronicleType.WRITTEN, ChronicleStatusType.PUBLISHED, "Title");
        chronicle.setId(UUID.randomUUID());
        final var publishedAt = Instant.parse("2026-07-31T10:00:00Z");
        chronicle.setPublishedAt(publishedAt);
        when(chronicleRepository.findAllByPartyIdAndStatusNotOrderByUpdatedAtDesc(partyId, ChronicleStatusType.ARCHIVED))
                .thenReturn(List.of(chronicle));

        final var cards = service.list(partyId);

        assertEquals(1, cards.size());
        assertEquals(publishedAt, cards.get(0).publishedAt());
    }

    @Test
    void inProgressChronicleCardHasNoPublishedAt() {
        final var chronicle = new ChronicleEntity(party, creator, ChronicleType.GAME, ChronicleStatusType.IN_PROGRESS, "Title");
        chronicle.setId(UUID.randomUUID());
        when(chronicleRepository.findAllByPartyIdAndStatusNotOrderByUpdatedAtDesc(partyId, ChronicleStatusType.ARCHIVED))
                .thenReturn(List.of(chronicle));
        when(gameTurnRepository.findProgressByChronicleIds(List.of(chronicle.getId()))).thenReturn(List.of());
        when(gameTurnRepository.findActiveTurnUsersByChronicleIds(List.of(chronicle.getId()))).thenReturn(List.of());

        final var cards = service.list(partyId);

        assertEquals(1, cards.size());
        assertNull(cards.get(0).publishedAt());
    }

    @Test
    void archivedChronicleIsExcludedFromTheList() {
        when(chronicleRepository.findAllByPartyIdAndStatusNotOrderByUpdatedAtDesc(partyId, ChronicleStatusType.ARCHIVED))
                .thenReturn(List.of());

        final var cards = service.list(partyId);

        assertEquals(0, cards.size());
    }
}
