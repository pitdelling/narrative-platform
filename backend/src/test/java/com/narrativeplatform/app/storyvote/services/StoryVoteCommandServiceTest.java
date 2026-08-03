package com.narrativeplatform.app.storyvote.services;

import com.narrativeplatform.app.auth.models.entities.UserEntity;
import com.narrativeplatform.app.chronicle.models.entities.ChronicleEntity;
import com.narrativeplatform.app.chronicle.models.enums.ChronicleStatusType;
import com.narrativeplatform.app.chronicle.models.enums.ChronicleType;
import com.narrativeplatform.app.chronicle.repositories.ChronicleRepository;
import com.narrativeplatform.app.chronicle.services.ChronicleAccessService;
import com.narrativeplatform.app.party.models.entities.PartyEntity;
import com.narrativeplatform.app.party.models.entities.PartyMemberEntity;
import com.narrativeplatform.app.party.models.enums.MemberStatusType;
import com.narrativeplatform.app.party.models.enums.PartyRoleType;
import com.narrativeplatform.app.party.repositories.PartyMemberRepository;
import com.narrativeplatform.app.party.services.PartyAccessService;
import com.narrativeplatform.app.storyvote.models.entities.StoryVoteAllocationEntity;
import com.narrativeplatform.app.storyvote.models.entities.StoryVoteDailyBudgetEntity;
import com.narrativeplatform.app.storyvote.models.responses.DailyStoryVoteStateResponse;
import com.narrativeplatform.app.storyvote.repositories.StoryVoteAllocationRepository;
import com.narrativeplatform.app.storyvote.repositories.StoryVoteDailyBudgetRepository;
import com.narrativeplatform.security.AuthenticatedUser;
import com.narrativeplatform.security.CurrentUserService;
import com.narrativeplatform.shared.exceptions.ForbiddenException;
import com.narrativeplatform.shared.exceptions.InvalidStoryVoteUnitsException;
import com.narrativeplatform.shared.exceptions.NotFoundException;
import com.narrativeplatform.shared.exceptions.StoryNotCompletedException;
import com.narrativeplatform.shared.exceptions.StoryVoteBudgetExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link PartyAccessService} and {@link ChronicleAccessService} are constructed for real
 * (wired to mocked repositories), not mocked themselves, so the authorization tests exercise
 * the actual business rules rather than a stand-in told to throw — same approach as
 * {@code InvitationServiceTest}. {@link StoryVoteQueryService} is mocked: response-shape
 * correctness is covered separately in {@code StoryVoteQueryServiceTest}.
 */
@ExtendWith(MockitoExtension.class)
class StoryVoteCommandServiceTest {

    private static final LocalDate DAY_1 = LocalDate.parse("2026-07-31");
    private static final LocalDate DAY_2 = LocalDate.parse("2026-08-01");

    @Mock
    private ChronicleRepository chronicleRepository;
    @Mock
    private PartyMemberRepository memberRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private StoryVoteDailyBudgetRepository budgetRepository;
    @Mock
    private StoryVoteAllocationRepository allocationRepository;
    @Mock
    private StoryVoteQueryService storyVoteQueryService;

    private PartyAccessService partyAccessService;
    private ChronicleAccessService chronicleAccessService;
    private StoryVoteCommandService commandService;

    private PartyEntity party;
    private UserEntity creator;
    private UUID partyId;
    private UUID chronicleId;

    @BeforeEach
    void setUp() {
        partyAccessService = new PartyAccessService(memberRepository, currentUserService);
        chronicleAccessService = new ChronicleAccessService(chronicleRepository, partyAccessService);
        commandService = new StoryVoteCommandService(
                chronicleAccessService, budgetRepository, allocationRepository, storyVoteQueryService, fixedClock(DAY_1)
        );

        partyId = UUID.randomUUID();
        chronicleId = UUID.randomUUID();
        creator = user("creator", "Creator");
        party = party(partyId, creator);

        // lenient(): most tests reject the vote before this final read-back call is reached.
        lenient().when(storyVoteQueryService.buildDailyState(any(), any(), any()))
                .thenReturn(new DailyStoryVoteStateResponse(DAY_1, 2, 0, 2, List.of()));
    }

    private Clock fixedClock(final LocalDate date) {
        return Clock.fixed(date.atStartOfDay(ZoneOffset.UTC).plusHours(10).toInstant(), ZoneOffset.UTC);
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
        final var entity = new PartyMemberEntity(party, user, role, status);
        entity.setId(UUID.randomUUID());
        return entity;
    }

    private ChronicleEntity chronicle(final ChronicleStatusType status) {
        return chronicle(status, party);
    }

    private ChronicleEntity chronicle(final ChronicleStatusType status, final PartyEntity ownerParty) {
        final var entity = new ChronicleEntity(ownerParty, creator, ChronicleType.GAME, status, "Test Story");
        entity.setId(chronicleId);
        entity.setPublishedAt(status == ChronicleStatusType.PUBLISHED ? Instant.now() : null);
        return entity;
    }

    private void currentUserIs(final UserEntity user) {
        when(currentUserService.require()).thenReturn(new AuthenticatedUser(user.getId(), user.getUsername()));
    }

    private void memberIs(final PartyMemberEntity membership) {
        currentUserIs(membership.getUser());
        when(memberRepository.findByPartyIdAndUserId(partyId, membership.getUser().getId())).thenReturn(Optional.of(membership));
    }

    // Budget starts at 2 remaining units per day; the first vote of the day creates the row lazily.
    @Test
    void appliesOneUnitToAStory() {
        final var membership = membership(creator, PartyRoleType.PLAYER, MemberStatusType.ACTIVE);
        memberIs(membership);
        when(chronicleRepository.findOneById(chronicleId)).thenReturn(Optional.of(chronicle(ChronicleStatusType.PUBLISHED)));
        when(budgetRepository.findForUpdateByPartyIdAndMembershipIdAndVoteDate(partyId, membership.getId(), DAY_1))
                .thenReturn(Optional.empty());
        final var newBudget = new StoryVoteDailyBudgetEntity(party, membership, DAY_1);
        when(budgetRepository.saveAndFlush(any())).thenReturn(newBudget);
        when(allocationRepository.findByChronicleIdAndMembershipIdAndVoteDate(chronicleId, membership.getId(), DAY_1))
                .thenReturn(Optional.empty());

        commandService.setVote(partyId, chronicleId, 1);

        assertEquals(1, newBudget.getUsedUnits());
        verify(allocationRepository).save(argThat(a -> a.getUnits() == 1));
    }

    @Test
    void appliesTwoUnitsToTheSameStory() {
        final var membership = membership(creator, PartyRoleType.PLAYER, MemberStatusType.ACTIVE);
        memberIs(membership);
        when(chronicleRepository.findOneById(chronicleId)).thenReturn(Optional.of(chronicle(ChronicleStatusType.PUBLISHED)));
        when(budgetRepository.findForUpdateByPartyIdAndMembershipIdAndVoteDate(partyId, membership.getId(), DAY_1))
                .thenReturn(Optional.empty());
        final var newBudget = new StoryVoteDailyBudgetEntity(party, membership, DAY_1);
        when(budgetRepository.saveAndFlush(any())).thenReturn(newBudget);
        when(allocationRepository.findByChronicleIdAndMembershipIdAndVoteDate(chronicleId, membership.getId(), DAY_1))
                .thenReturn(Optional.empty());

        commandService.setVote(partyId, chronicleId, 2);

        assertEquals(2, newBudget.getUsedUnits());
        verify(allocationRepository).save(argThat(a -> a.getUnits() == 2));
    }

    // Dividing the daily budget between two stories: two sequential PUTs against the same budget row.
    @Test
    void dividesBudgetBetweenTwoStories() {
        final var membership = membership(creator, PartyRoleType.PLAYER, MemberStatusType.ACTIVE);
        memberIs(membership);
        final var chronicleA = chronicleId;
        final var chronicleB = UUID.randomUUID();
        when(chronicleRepository.findOneById(chronicleA)).thenReturn(Optional.of(chronicle(ChronicleStatusType.PUBLISHED)));
        final var storyB = new ChronicleEntity(party, creator, ChronicleType.GAME, ChronicleStatusType.PUBLISHED, "Story B");
        storyB.setId(chronicleB);
        storyB.setPublishedAt(Instant.now());
        when(chronicleRepository.findOneById(chronicleB)).thenReturn(Optional.of(storyB));

        final var budget = new StoryVoteDailyBudgetEntity(party, membership, DAY_1);
        when(budgetRepository.findForUpdateByPartyIdAndMembershipIdAndVoteDate(partyId, membership.getId(), DAY_1))
                .thenReturn(Optional.of(budget));
        when(allocationRepository.findByChronicleIdAndMembershipIdAndVoteDate(chronicleA, membership.getId(), DAY_1))
                .thenReturn(Optional.empty());
        when(allocationRepository.findByChronicleIdAndMembershipIdAndVoteDate(chronicleB, membership.getId(), DAY_1))
                .thenReturn(Optional.empty());

        commandService.setVote(partyId, chronicleA, 1);
        commandService.setVote(partyId, chronicleB, 1);

        assertEquals(2, budget.getUsedUnits());
        verify(allocationRepository, times(2)).save(argThat(a -> a.getUnits() == 1));
    }

    // Requesting more than the remaining budget is rejected without touching any allocation.
    @Test
    void rejectsWhenExceedingRemainingBudget() {
        final var membership = membership(creator, PartyRoleType.PLAYER, MemberStatusType.ACTIVE);
        memberIs(membership);
        when(chronicleRepository.findOneById(chronicleId)).thenReturn(Optional.of(chronicle(ChronicleStatusType.PUBLISHED)));
        final var budget = new StoryVoteDailyBudgetEntity(party, membership, DAY_1);
        budget.setUsedUnits((short) 2);
        when(budgetRepository.findForUpdateByPartyIdAndMembershipIdAndVoteDate(partyId, membership.getId(), DAY_1))
                .thenReturn(Optional.of(budget));
        when(allocationRepository.findByChronicleIdAndMembershipIdAndVoteDate(chronicleId, membership.getId(), DAY_1))
                .thenReturn(Optional.empty());

        assertThrows(StoryVoteBudgetExceededException.class, () -> commandService.setVote(partyId, chronicleId, 1));

        assertEquals(2, budget.getUsedUnits());
        verify(allocationRepository, never()).save(any());
        verify(allocationRepository, never()).delete(any());
    }

    @Test
    void reducesFromTwoToOne() {
        final var membership = membership(creator, PartyRoleType.PLAYER, MemberStatusType.ACTIVE);
        memberIs(membership);
        when(chronicleRepository.findOneById(chronicleId)).thenReturn(Optional.of(chronicle(ChronicleStatusType.PUBLISHED)));
        final var budget = new StoryVoteDailyBudgetEntity(party, membership, DAY_1);
        budget.setUsedUnits((short) 2);
        when(budgetRepository.findForUpdateByPartyIdAndMembershipIdAndVoteDate(partyId, membership.getId(), DAY_1))
                .thenReturn(Optional.of(budget));
        final var existingAllocation = new StoryVoteAllocationEntity(party, chronicle(ChronicleStatusType.PUBLISHED), membership, DAY_1, (short) 2);
        when(allocationRepository.findByChronicleIdAndMembershipIdAndVoteDate(chronicleId, membership.getId(), DAY_1))
                .thenReturn(Optional.of(existingAllocation));

        commandService.setVote(partyId, chronicleId, 1);

        assertEquals(1, budget.getUsedUnits());
        assertEquals(1, existingAllocation.getUnits());
        verify(allocationRepository, never()).save(any());
        verify(allocationRepository, never()).delete(any());
    }

    @Test
    void removesVoteBySettingItToZero() {
        final var membership = membership(creator, PartyRoleType.PLAYER, MemberStatusType.ACTIVE);
        memberIs(membership);
        when(chronicleRepository.findOneById(chronicleId)).thenReturn(Optional.of(chronicle(ChronicleStatusType.PUBLISHED)));
        final var budget = new StoryVoteDailyBudgetEntity(party, membership, DAY_1);
        budget.setUsedUnits((short) 1);
        when(budgetRepository.findForUpdateByPartyIdAndMembershipIdAndVoteDate(partyId, membership.getId(), DAY_1))
                .thenReturn(Optional.of(budget));
        final var existingAllocation = new StoryVoteAllocationEntity(party, chronicle(ChronicleStatusType.PUBLISHED), membership, DAY_1, (short) 1);
        when(allocationRepository.findByChronicleIdAndMembershipIdAndVoteDate(chronicleId, membership.getId(), DAY_1))
                .thenReturn(Optional.of(existingAllocation));

        commandService.setVote(partyId, chronicleId, 0);

        assertEquals(0, budget.getUsedUnits());
        verify(allocationRepository).delete(existingAllocation);
        verify(allocationRepository, never()).save(any());
    }

    // Redistribution is two sequential idempotent PUTs, not one atomic call.
    @Test
    void redistributesWithinTheSameDay() {
        final var membership = membership(creator, PartyRoleType.PLAYER, MemberStatusType.ACTIVE);
        memberIs(membership);
        final var chronicleA = chronicleId;
        final var chronicleB = UUID.randomUUID();
        when(chronicleRepository.findOneById(chronicleA)).thenReturn(Optional.of(chronicle(ChronicleStatusType.PUBLISHED)));
        final var storyB = new ChronicleEntity(party, creator, ChronicleType.GAME, ChronicleStatusType.PUBLISHED, "Story B");
        storyB.setId(chronicleB);
        storyB.setPublishedAt(Instant.now());
        when(chronicleRepository.findOneById(chronicleB)).thenReturn(Optional.of(storyB));

        final var budget = new StoryVoteDailyBudgetEntity(party, membership, DAY_1);
        budget.setUsedUnits((short) 2);
        when(budgetRepository.findForUpdateByPartyIdAndMembershipIdAndVoteDate(partyId, membership.getId(), DAY_1))
                .thenReturn(Optional.of(budget));
        final var allocationA = new StoryVoteAllocationEntity(party, chronicle(ChronicleStatusType.PUBLISHED), membership, DAY_1, (short) 2);
        when(allocationRepository.findByChronicleIdAndMembershipIdAndVoteDate(chronicleA, membership.getId(), DAY_1))
                .thenReturn(Optional.of(allocationA));
        when(allocationRepository.findByChronicleIdAndMembershipIdAndVoteDate(chronicleB, membership.getId(), DAY_1))
                .thenReturn(Optional.empty());

        commandService.setVote(partyId, chronicleA, 0);
        commandService.setVote(partyId, chronicleB, 2);

        assertEquals(2, budget.getUsedUnits());
        verify(allocationRepository).delete(allocationA);
        verify(allocationRepository).save(argThat(a -> a.getUnits() == 2));
    }

    @Test
    void settingTheSameValueTwiceIsIdempotent() {
        final var membership = membership(creator, PartyRoleType.PLAYER, MemberStatusType.ACTIVE);
        memberIs(membership);
        when(chronicleRepository.findOneById(chronicleId)).thenReturn(Optional.of(chronicle(ChronicleStatusType.PUBLISHED)));
        final var budget = new StoryVoteDailyBudgetEntity(party, membership, DAY_1);
        budget.setUsedUnits((short) 1);
        when(budgetRepository.findForUpdateByPartyIdAndMembershipIdAndVoteDate(partyId, membership.getId(), DAY_1))
                .thenReturn(Optional.of(budget));
        final var existingAllocation = new StoryVoteAllocationEntity(party, chronicle(ChronicleStatusType.PUBLISHED), membership, DAY_1, (short) 1);
        when(allocationRepository.findByChronicleIdAndMembershipIdAndVoteDate(chronicleId, membership.getId(), DAY_1))
                .thenReturn(Optional.of(existingAllocation));

        assertDoesNotThrow(() -> commandService.setVote(partyId, chronicleId, 1));

        assertEquals(1, budget.getUsedUnits());
        assertEquals(1, existingAllocation.getUnits());
    }

    @Test
    void memberCanVoteOnTheirOwnStory() {
        final var membership = membership(creator, PartyRoleType.PLAYER, MemberStatusType.ACTIVE);
        memberIs(membership);
        final var ownStory = new ChronicleEntity(party, creator, ChronicleType.GAME, ChronicleStatusType.PUBLISHED, "Own Story");
        ownStory.setId(chronicleId);
        ownStory.setPublishedAt(Instant.now());
        when(chronicleRepository.findOneById(chronicleId)).thenReturn(Optional.of(ownStory));
        when(budgetRepository.findForUpdateByPartyIdAndMembershipIdAndVoteDate(partyId, membership.getId(), DAY_1))
                .thenReturn(Optional.empty());
        when(budgetRepository.saveAndFlush(any())).thenReturn(new StoryVoteDailyBudgetEntity(party, membership, DAY_1));
        when(allocationRepository.findByChronicleIdAndMembershipIdAndVoteDate(chronicleId, membership.getId(), DAY_1))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> commandService.setVote(partyId, chronicleId, 1));
    }

    // No rollover: a different UTC day is a structurally different budget row.
    @Test
    void noRolloverAcrossUtcDays() {
        final var membership = membership(creator, PartyRoleType.PLAYER, MemberStatusType.ACTIVE);
        memberIs(membership);
        when(chronicleRepository.findOneById(chronicleId)).thenReturn(Optional.of(chronicle(ChronicleStatusType.PUBLISHED)));

        final var day2CommandService = new StoryVoteCommandService(
                chronicleAccessService, budgetRepository, allocationRepository, storyVoteQueryService, fixedClock(DAY_2)
        );
        when(budgetRepository.findForUpdateByPartyIdAndMembershipIdAndVoteDate(partyId, membership.getId(), DAY_2))
                .thenReturn(Optional.empty());
        final var day2Budget = new StoryVoteDailyBudgetEntity(party, membership, DAY_2);
        when(budgetRepository.saveAndFlush(any())).thenReturn(day2Budget);
        when(allocationRepository.findByChronicleIdAndMembershipIdAndVoteDate(chronicleId, membership.getId(), DAY_2))
                .thenReturn(Optional.empty());

        day2CommandService.setVote(partyId, chronicleId, 2);

        assertEquals(2, day2Budget.getUsedUnits());
        verify(budgetRepository, never()).findForUpdateByPartyIdAndMembershipIdAndVoteDate(partyId, membership.getId(), DAY_1);
    }

    // Concurrent first-vote-of-the-day race: mirrors InvitationService.getOrCreateForUpdate.
    @Test
    void concurrentBudgetCreationRaceIsHandledWithoutDuplication() {
        final var membership = membership(creator, PartyRoleType.PLAYER, MemberStatusType.ACTIVE);
        memberIs(membership);
        when(chronicleRepository.findOneById(chronicleId)).thenReturn(Optional.of(chronicle(ChronicleStatusType.PUBLISHED)));

        final var wonTheRace = new StoryVoteDailyBudgetEntity(party, membership, DAY_1);
        when(budgetRepository.findForUpdateByPartyIdAndMembershipIdAndVoteDate(partyId, membership.getId(), DAY_1))
                .thenReturn(Optional.empty(), Optional.of(wonTheRace));
        when(budgetRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));
        when(allocationRepository.findByChronicleIdAndMembershipIdAndVoteDate(chronicleId, membership.getId(), DAY_1))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> commandService.setVote(partyId, chronicleId, 1));

        assertEquals(1, wonTheRace.getUsedUnits());
        verify(budgetRepository, times(1)).saveAndFlush(any());
        verify(budgetRepository, times(2)).findForUpdateByPartyIdAndMembershipIdAndVoteDate(partyId, membership.getId(), DAY_1);
    }

    @Test
    void activeOwnerNarratorAndSpectatorCanVote() {
        for (final var role : new PartyRoleType[] {PartyRoleType.OWNER, PartyRoleType.NARRATOR, PartyRoleType.PLAYER, PartyRoleType.SPECTATOR}) {
            reset(chronicleRepository, memberRepository, currentUserService, budgetRepository, allocationRepository);
            final var voter = user("voter-" + role, "Voter " + role);
            final var membership = membership(voter, role, MemberStatusType.ACTIVE);
            memberIs(membership);
            when(chronicleRepository.findOneById(chronicleId)).thenReturn(Optional.of(chronicle(ChronicleStatusType.PUBLISHED)));
            when(budgetRepository.findForUpdateByPartyIdAndMembershipIdAndVoteDate(partyId, membership.getId(), DAY_1))
                    .thenReturn(Optional.empty());
            when(budgetRepository.saveAndFlush(any())).thenReturn(new StoryVoteDailyBudgetEntity(party, membership, DAY_1));
            when(allocationRepository.findByChronicleIdAndMembershipIdAndVoteDate(chronicleId, membership.getId(), DAY_1))
                    .thenReturn(Optional.empty());

            assertDoesNotThrow(() -> commandService.setVote(partyId, chronicleId, 1), role + " should be able to vote");
        }
    }

    @Test
    void disabledMemberCannotVote() {
        final var membership = membership(creator, PartyRoleType.PLAYER, MemberStatusType.DISABLED);
        memberIs(membership);

        assertThrows(ForbiddenException.class, () -> commandService.setVote(partyId, chronicleId, 1));
        verifyNoInteractions(chronicleRepository, budgetRepository, allocationRepository);
    }

    @Test
    void removedMemberCannotVote() {
        final var membership = membership(creator, PartyRoleType.PLAYER, MemberStatusType.REMOVED);
        memberIs(membership);

        assertThrows(ForbiddenException.class, () -> commandService.setVote(partyId, chronicleId, 1));
        verifyNoInteractions(chronicleRepository, budgetRepository, allocationRepository);
    }

    @Test
    void outsiderCannotVote() {
        currentUserIs(creator);
        when(memberRepository.findByPartyIdAndUserId(partyId, creator.getId())).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> commandService.setVote(partyId, chronicleId, 1));
        verifyNoInteractions(chronicleRepository, budgetRepository, allocationRepository);
    }

    @Test
    void storyFromAnotherPartyIsRejected() {
        final var membership = membership(creator, PartyRoleType.PLAYER, MemberStatusType.ACTIVE);
        memberIs(membership);
        final var otherParty = party(UUID.randomUUID(), creator);
        when(chronicleRepository.findOneById(chronicleId)).thenReturn(Optional.of(chronicle(ChronicleStatusType.PUBLISHED, otherParty)));

        assertThrows(NotFoundException.class, () -> commandService.setVote(partyId, chronicleId, 1));
        verifyNoInteractions(budgetRepository, allocationRepository);
    }

    @Test
    void storyInProgressIsRejected() {
        final var membership = membership(creator, PartyRoleType.PLAYER, MemberStatusType.ACTIVE);
        memberIs(membership);
        when(chronicleRepository.findOneById(chronicleId)).thenReturn(Optional.of(chronicle(ChronicleStatusType.IN_PROGRESS)));

        assertThrows(StoryNotCompletedException.class, () -> commandService.setVote(partyId, chronicleId, 1));
        verifyNoInteractions(budgetRepository, allocationRepository);
    }

    @Test
    void negativeUnitsAreRejected() {
        assertThrows(InvalidStoryVoteUnitsException.class, () -> commandService.setVote(partyId, chronicleId, -1));
        verifyNoInteractions(chronicleRepository, memberRepository, budgetRepository, allocationRepository);
    }

    @Test
    void unitsAboveTwoAreRejected() {
        assertThrows(InvalidStoryVoteUnitsException.class, () -> commandService.setVote(partyId, chronicleId, 3));
        verifyNoInteractions(chronicleRepository, memberRepository, budgetRepository, allocationRepository);
    }

    @Test
    void nullUnitsAreRejected() {
        assertThrows(InvalidStoryVoteUnitsException.class, () -> commandService.setVote(partyId, chronicleId, null));
        verifyNoInteractions(chronicleRepository, memberRepository, budgetRepository, allocationRepository);
    }
}
