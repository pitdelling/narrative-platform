package com.narrativeplatform.app.invitation.services;

import com.narrativeplatform.app.auth.models.entities.UserEntity;
import com.narrativeplatform.app.auth.repositories.UserRepository;
import com.narrativeplatform.app.chronicle.services.GameChronicleService;
import com.narrativeplatform.app.invitation.models.entities.PartyInvitationLinkEntity;
import com.narrativeplatform.app.invitation.repositories.PartyInvitationLinkRepository;
import com.narrativeplatform.app.party.models.entities.PartyEntity;
import com.narrativeplatform.app.party.models.entities.PartyMemberEntity;
import com.narrativeplatform.app.party.models.enums.MemberStatusType;
import com.narrativeplatform.app.party.models.enums.PartyRoleType;
import com.narrativeplatform.app.party.repositories.PartyMemberRepository;
import com.narrativeplatform.app.party.services.PartyAccessService;
import com.narrativeplatform.configuration.AppProperties;
import com.narrativeplatform.security.AuthenticatedUser;
import com.narrativeplatform.security.CurrentUserService;
import com.narrativeplatform.shared.exceptions.ForbiddenException;
import com.narrativeplatform.shared.exceptions.NotFoundException;
import com.narrativeplatform.shared.utils.TokenUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Covers the required scenarios for the reusable per-party invitation link.
 *
 * <p>{@link PartyAccessService} is constructed for real (wired to mocked repositories),
 * not mocked itself, so the authorization tests exercise the actual business rule rather
 * than a stand-in that was simply told to throw.
 *
 * <p>Scenario 9 ("concurrent regeneration leaves exactly one active link") is covered here
 * only as the race-handling *mechanism* inside {@code getOrCreateForUpdate} — the true
 * "only one row can ever exist" guarantee is structural (party_id is the primary key) and
 * documented in {@link PartyInvitationLinkEntity} and {@link InvitationService}, not
 * re-proven by a live concurrent-transaction test (no Testcontainers/new dependency, per
 * the project owner's explicit choice).
 */
@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock
    private PartyInvitationLinkRepository linkRepository;
    @Mock
    private PartyMemberRepository memberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private GameChronicleService gameChronicleService;
    @Mock
    private CurrentUserService currentUserService;

    private PartyAccessService partyAccessService;
    private InvitationService invitationService;

    private PartyEntity party;
    private UserEntity owner;
    private UUID partyId;

    @BeforeEach
    void setUp() {
        partyAccessService = new PartyAccessService(memberRepository, currentUserService);
        final var properties = new AppProperties(
                "http://localhost:3000", "http://localhost:3000", "test-secret",
                168, 24, 24, 10,
                new AppProperties.OpenAi("", "gpt-5-mini", "https://api.openai.com/v1")
        );
        invitationService = new InvitationService(
                linkRepository, memberRepository, userRepository, partyAccessService,
                gameChronicleService, currentUserService, properties
        );

        partyId = UUID.randomUUID();
        owner = user("owner", "Owner");
        party = party(partyId, owner);
    }

    private PartyEntity party(final UUID id, final UserEntity owner) {
        final var entity = new PartyEntity("Test Party", "test-party", null, null, owner);
        entity.setId(id);
        return entity;
    }

    private UserEntity user(final String username, final String displayName) {
        final var entity = new UserEntity(username, displayName, "hash");
        entity.setId(UUID.randomUUID());
        return entity;
    }

    private PartyMemberEntity membership(final UserEntity user, final PartyRoleType role, final MemberStatusType status) {
        return new PartyMemberEntity(party, user, role, status);
    }

    /**
     * The production constructor deliberately leaves {@code partyId} null so Spring Data JPA's
     * save() calls persist() instead of merge() (see {@link PartyInvitationLinkEntity}'s
     * Javadoc) — Hibernate only fills it in via @MapsId once a real EntityManager persists the
     * row, which never happens in this pure-Mockito test. Set it explicitly here instead.
     */
    private PartyInvitationLinkEntity link(final String rawToken, final UserEntity actor) {
        final var entity = new PartyInvitationLinkEntity(party, rawToken, TokenUtils.sha256(rawToken), actor);
        entity.setPartyId(party.getId());
        return entity;
    }

    private void currentUserIs(final UserEntity user) {
        when(currentUserService.require()).thenReturn(new AuthenticatedUser(user.getId(), user.getUsername()));
    }

    // 1. Active member can retrieve the link — corrected to narrator/owner-only.
    @Test
    void narratorCanRetrieveTheLink() {
        currentUserIs(owner);
        final var membership = membership(owner, PartyRoleType.NARRATOR, MemberStatusType.ACTIVE);
        when(memberRepository.findByPartyIdAndUserId(partyId, owner.getId())).thenReturn(Optional.of(membership));
        final var link = link("raw-token", owner);
        when(linkRepository.findById(partyId)).thenReturn(Optional.of(link));

        final var response = invitationService.getCurrentLink(partyId);

        assertEquals(partyId, response.partyId());
        assertTrue(response.inviteUrl().endsWith("/invite/raw-token"));
    }

    @Test
    void ordinaryPlayerCannotRetrieveTheLink() {
        currentUserIs(owner);
        final var membership = membership(owner, PartyRoleType.PLAYER, MemberStatusType.ACTIVE);
        when(memberRepository.findByPartyIdAndUserId(partyId, owner.getId())).thenReturn(Optional.of(membership));

        assertThrows(ForbiddenException.class, () -> invitationService.getCurrentLink(partyId));
        verifyNoInteractions(linkRepository);
    }

    // 2. Narrator (and owner) can regenerate.
    @Test
    void narratorCanRegenerate() {
        currentUserIs(owner);
        final var membership = membership(owner, PartyRoleType.NARRATOR, MemberStatusType.ACTIVE);
        when(memberRepository.findByPartyIdAndUserId(partyId, owner.getId())).thenReturn(Optional.of(membership));
        final var link = link("old-token", owner);
        when(linkRepository.findForUpdateById(partyId)).thenReturn(Optional.of(link));

        invitationService.regenerateLink(partyId);

        assertNotEquals("old-token", link.getToken());
    }

    @Test
    void ownerCanRegenerate() {
        currentUserIs(owner);
        final var membership = membership(owner, PartyRoleType.OWNER, MemberStatusType.ACTIVE);
        when(memberRepository.findByPartyIdAndUserId(partyId, owner.getId())).thenReturn(Optional.of(membership));
        final var link = link("old-token", owner);
        when(linkRepository.findForUpdateById(partyId)).thenReturn(Optional.of(link));

        assertDoesNotThrow(() -> invitationService.regenerateLink(partyId));
    }

    // 3. Ordinary member cannot regenerate.
    @Test
    void ordinaryPlayerCannotRegenerate() {
        currentUserIs(owner);
        final var membership = membership(owner, PartyRoleType.PLAYER, MemberStatusType.ACTIVE);
        when(memberRepository.findByPartyIdAndUserId(partyId, owner.getId())).thenReturn(Optional.of(membership));

        assertThrows(ForbiddenException.class, () -> invitationService.regenerateLink(partyId));
        verifyNoInteractions(linkRepository);
    }

    // 4. Disabled member cannot retrieve (or regenerate).
    @Test
    void disabledMemberCannotRetrieve() {
        currentUserIs(owner);
        final var membership = membership(owner, PartyRoleType.NARRATOR, MemberStatusType.DISABLED);
        when(memberRepository.findByPartyIdAndUserId(partyId, owner.getId())).thenReturn(Optional.of(membership));

        assertThrows(ForbiddenException.class, () -> invitationService.getCurrentLink(partyId));
    }

    // 5. Removed member cannot retrieve (or regenerate).
    @Test
    void removedMemberCannotRetrieve() {
        currentUserIs(owner);
        final var membership = membership(owner, PartyRoleType.NARRATOR, MemberStatusType.REMOVED);
        when(memberRepository.findByPartyIdAndUserId(partyId, owner.getId())).thenReturn(Optional.of(membership));

        assertThrows(ForbiddenException.class, () -> invitationService.getCurrentLink(partyId));
    }

    // 6. Old link stops working after regeneration.
    @Test
    void oldLinkStopsWorkingAfterRegeneration() {
        currentUserIs(owner);
        final var membership = membership(owner, PartyRoleType.NARRATOR, MemberStatusType.ACTIVE);
        when(memberRepository.findByPartyIdAndUserId(partyId, owner.getId())).thenReturn(Optional.of(membership));
        final var oldRawToken = "old-raw-token";
        final var oldHash = TokenUtils.sha256(oldRawToken);
        final var link = link(oldRawToken, owner);
        when(linkRepository.findForUpdateById(partyId)).thenReturn(Optional.of(link));

        invitationService.regenerateLink(partyId);

        assertNotEquals(oldHash, link.getTokenHash());

        when(linkRepository.findByTokenHash(oldHash)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> invitationService.preview(oldRawToken));
    }

    // 7. New link works more than once.
    @Test
    void newLinkWorksMoreThanOnce() {
        final var rawToken = "reusable-token";
        final var link = link(rawToken, owner);
        when(linkRepository.findByTokenHash(TokenUtils.sha256(rawToken))).thenReturn(Optional.of(link));

        final var userA = user("alice", "Alice");
        final var userB = user("bob", "Bob");
        when(userRepository.findById(userA.getId())).thenReturn(Optional.of(userA));
        when(userRepository.findById(userB.getId())).thenReturn(Optional.of(userB));
        when(memberRepository.findByPartyIdAndUserId(partyId, userA.getId())).thenReturn(Optional.empty());
        when(memberRepository.findByPartyIdAndUserId(partyId, userB.getId())).thenReturn(Optional.empty());

        invitationService.acceptForUser(rawToken, userA.getId());
        invitationService.acceptForUser(rawToken, userB.getId());

        verify(memberRepository, times(1)).save(argThat(m -> m.getUser() == userA));
        verify(memberRepository, times(1)).save(argThat(m -> m.getUser() == userB));
        verify(gameChronicleService).insertPartyMemberIntoActiveRuns(partyId, userA.getId());
        verify(gameChronicleService).insertPartyMemberIntoActiveRuns(partyId, userB.getId());
    }

    // 8. Duplicate membership acceptance is idempotent.
    @Test
    void duplicateAcceptanceIsIdempotent() {
        final var rawToken = "reusable-token";
        final var link = link(rawToken, owner);
        when(linkRepository.findByTokenHash(TokenUtils.sha256(rawToken))).thenReturn(Optional.of(link));

        final var newcomer = user("newcomer", "Newcomer");
        when(userRepository.findById(newcomer.getId())).thenReturn(Optional.of(newcomer));
        final var nowActiveMembership = membership(newcomer, PartyRoleType.PLAYER, MemberStatusType.ACTIVE);
        when(memberRepository.findByPartyIdAndUserId(partyId, newcomer.getId()))
                .thenReturn(Optional.empty(), Optional.of(nowActiveMembership));

        invitationService.acceptForUser(rawToken, newcomer.getId());
        assertDoesNotThrow(() -> invitationService.acceptForUser(rawToken, newcomer.getId()));

        verify(memberRepository, times(1)).save(any());
        verify(gameChronicleService, times(1)).insertPartyMemberIntoActiveRuns(partyId, newcomer.getId());
    }

    // 9. Concurrent regeneration: race-handling mechanism inside getOrCreateForUpdate.
    @Test
    void concurrentRegenerationRaceIsHandledWithoutDuplication() {
        currentUserIs(owner);
        final var membership = membership(owner, PartyRoleType.NARRATOR, MemberStatusType.ACTIVE);
        when(memberRepository.findByPartyIdAndUserId(partyId, owner.getId())).thenReturn(Optional.of(membership));

        final var wonTheRace = link("concurrent-token", owner);
        when(linkRepository.findForUpdateById(partyId))
                .thenReturn(Optional.empty(), Optional.of(wonTheRace));
        when(linkRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertDoesNotThrow(() -> invitationService.regenerateLink(partyId));

        assertNotEquals("concurrent-token", wonTheRace.getToken());
        verify(linkRepository, times(1)).saveAndFlush(any());
        verify(linkRepository, times(2)).findForUpdateById(partyId);
    }
}
