package com.narrativeplatform.app.storyvote.services;

import com.narrativeplatform.app.auth.models.entities.UserEntity;
import com.narrativeplatform.app.chronicle.models.entities.ChronicleEntity;
import com.narrativeplatform.app.chronicle.models.enums.ChronicleStatusType;
import com.narrativeplatform.app.chronicle.models.enums.ChronicleType;
import com.narrativeplatform.app.party.models.entities.PartyEntity;
import com.narrativeplatform.app.party.models.entities.PartyMemberEntity;
import com.narrativeplatform.app.party.models.enums.MemberStatusType;
import com.narrativeplatform.app.party.models.enums.PartyRoleType;
import com.narrativeplatform.app.party.repositories.PartyMemberRepository;
import com.narrativeplatform.app.party.services.PartyAccessService;
import com.narrativeplatform.app.storyvote.models.entities.StoryVoteAllocationEntity;
import com.narrativeplatform.app.storyvote.models.entities.StoryVoteDailyBudgetEntity;
import com.narrativeplatform.app.storyvote.models.projections.PublishedChronicleProjection;
import com.narrativeplatform.app.storyvote.models.projections.StoryVoteTotalProjection;
import com.narrativeplatform.app.storyvote.repositories.StoryVoteAllocationRepository;
import com.narrativeplatform.app.storyvote.repositories.StoryVoteDailyBudgetRepository;
import com.narrativeplatform.app.storyvote.repositories.StoryVoteQueryRepository;
import com.narrativeplatform.security.AuthenticatedUser;
import com.narrativeplatform.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoryVoteQueryServiceTest {

    private static final LocalDate TODAY = LocalDate.parse("2026-07-31");

    private record PublishedChronicleFixture(UUID chronicleId, Instant publishedAt) implements PublishedChronicleProjection {
        @Override
        public UUID getChronicleId() {
            return chronicleId;
        }

        @Override
        public Instant getPublishedAt() {
            return publishedAt;
        }
    }

    private record TotalFixture(UUID chronicleId, long totalVotes) implements StoryVoteTotalProjection {
        @Override
        public UUID getChronicleId() {
            return chronicleId;
        }

        @Override
        public long getTotalVotes() {
            return totalVotes;
        }
    }

    @Mock
    private PartyMemberRepository memberRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private StoryVoteDailyBudgetRepository budgetRepository;
    @Mock
    private StoryVoteAllocationRepository allocationRepository;
    @Mock
    private StoryVoteQueryRepository storyVoteQueryRepository;

    private PartyAccessService partyAccessService;
    private StoryVoteQueryService queryService;

    private PartyEntity party;
    private UserEntity user;
    private PartyMemberEntity membership;
    private UUID partyId;

    @BeforeEach
    void setUp() {
        partyAccessService = new PartyAccessService(memberRepository, currentUserService);
        queryService = new StoryVoteQueryService(
                partyAccessService, budgetRepository, allocationRepository, storyVoteQueryRepository, fixedClock()
        );

        partyId = UUID.randomUUID();
        user = new UserEntity("voter", "Voter", "hash");
        user.setId(UUID.randomUUID());
        party = new PartyEntity("Test Party", "test-party", null, null, user);
        party.setId(partyId);
        membership = new PartyMemberEntity(party, user, PartyRoleType.PLAYER, MemberStatusType.ACTIVE);
        membership.setId(UUID.randomUUID());

        when(currentUserService.require()).thenReturn(new AuthenticatedUser(user.getId(), user.getUsername()));
        when(memberRepository.findByPartyIdAndUserId(partyId, user.getId())).thenReturn(Optional.of(membership));
    }

    private Clock fixedClock() {
        return Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).plusHours(10).toInstant(), ZoneOffset.UTC);
    }

    @Test
    void dailyStateWithNoExistingBudgetReturnsFullRemaining() {
        when(budgetRepository.findByPartyIdAndMembershipIdAndVoteDate(partyId, membership.getId(), TODAY)).thenReturn(Optional.empty());
        when(allocationRepository.findAllByPartyIdAndMembershipIdAndVoteDate(partyId, membership.getId(), TODAY)).thenReturn(List.of());

        final var state = queryService.getDailyState(partyId);

        assertEquals(TODAY, state.dateUtc());
        assertEquals(2, state.dailyLimit());
        assertEquals(0, state.usedUnits());
        assertEquals(2, state.remainingUnits());
        assertTrue(state.allocations().isEmpty());
    }

    @Test
    void dailyStateReflectsExistingBudgetAndAllocations() {
        final var chronicleId = UUID.randomUUID();
        final var budget = new StoryVoteDailyBudgetEntity(party, membership, TODAY);
        budget.setUsedUnits((short) 1);
        when(budgetRepository.findByPartyIdAndMembershipIdAndVoteDate(partyId, membership.getId(), TODAY)).thenReturn(Optional.of(budget));
        final var chronicle = new ChronicleEntity(party, user, ChronicleType.GAME, ChronicleStatusType.PUBLISHED, "Story");
        chronicle.setId(chronicleId);
        final var allocation = new StoryVoteAllocationEntity(party, chronicle, membership, TODAY, (short) 1);
        when(allocationRepository.findAllByPartyIdAndMembershipIdAndVoteDate(partyId, membership.getId(), TODAY)).thenReturn(List.of(allocation));

        final var state = queryService.getDailyState(partyId);

        assertEquals(1, state.usedUnits());
        assertEquals(1, state.remainingUnits());
        assertEquals(1, state.allocations().size());
        assertEquals(chronicleId, state.allocations().get(0).chronicleId());
        assertEquals(1, state.allocations().get(0).units());
    }

    @Test
    void summaryUsesSumOfUnitsForTotalsAndTreatsAbsenceAsZero() {
        final var chronicleA = UUID.randomUUID();
        final var chronicleB = UUID.randomUUID();
        when(storyVoteQueryRepository.findPublishedChronicles(partyId)).thenReturn(List.of(
                new PublishedChronicleFixture(chronicleA, Instant.parse("2026-07-30T10:00:00Z")),
                new PublishedChronicleFixture(chronicleB, Instant.parse("2026-07-29T10:00:00Z"))
        ));
        // Only chronicleA has allocations; chronicleB is absent from the sum result entirely.
        when(allocationRepository.sumUnitsByChronicleIds(List.of(chronicleA, chronicleB)))
                .thenReturn(List.of(new TotalFixture(chronicleA, 14L)));
        when(allocationRepository.findAllByPartyIdAndMembershipIdAndVoteDate(partyId, membership.getId(), TODAY)).thenReturn(List.of());
        when(budgetRepository.findByPartyIdAndMembershipIdAndVoteDate(partyId, membership.getId(), TODAY)).thenReturn(Optional.empty());

        final var summary = queryService.getSummary(partyId);

        assertEquals(2, summary.size());
        final var storyA = summary.stream().filter(s -> s.chronicleId().equals(chronicleA)).findFirst().orElseThrow();
        final var storyB = summary.stream().filter(s -> s.chronicleId().equals(chronicleB)).findFirst().orElseThrow();
        assertEquals(14, storyA.totalVotes());
        assertEquals(1L, storyA.rank());
        assertEquals(0, storyB.totalVotes());
        // A story with zero votes is excluded from the ranking entirely — never gets a rank of its own.
        assertNull(storyB.rank());
    }

    @Test
    void tiedStoriesShareTheSameDenseRankWithNoGapAfterThem() {
        final var higherVotes = UUID.randomUUID();
        final var tiedA = UUID.randomUUID();
        final var tiedB = UUID.randomUUID();
        final var tiedC = UUID.randomUUID();
        final var lowerVotes = UUID.randomUUID();
        when(storyVoteQueryRepository.findPublishedChronicles(partyId)).thenReturn(List.of(
                new PublishedChronicleFixture(higherVotes, Instant.parse("2026-07-01T00:00:00Z")),
                new PublishedChronicleFixture(tiedA, Instant.parse("2026-07-10T00:00:00Z")),
                new PublishedChronicleFixture(tiedB, Instant.parse("2026-07-15T00:00:00Z")),
                new PublishedChronicleFixture(tiedC, Instant.parse("2026-07-20T00:00:00Z")),
                new PublishedChronicleFixture(lowerVotes, Instant.parse("2026-07-05T00:00:00Z"))
        ));
        when(allocationRepository.sumUnitsByChronicleIds(List.of(higherVotes, tiedC, tiedB, tiedA, lowerVotes)))
                .thenReturn(List.of(
                        new TotalFixture(higherVotes, 10L),
                        new TotalFixture(tiedA, 5L),
                        new TotalFixture(tiedB, 5L),
                        new TotalFixture(tiedC, 5L),
                        new TotalFixture(lowerVotes, 2L)
                ));
        when(allocationRepository.findAllByPartyIdAndMembershipIdAndVoteDate(partyId, membership.getId(), TODAY)).thenReturn(List.of());
        when(budgetRepository.findByPartyIdAndMembershipIdAndVoteDate(partyId, membership.getId(), TODAY)).thenReturn(Optional.empty());

        final var summary = queryService.getSummary(partyId);
        final var byId = summary.stream().collect(Collectors.toMap(s -> s.chronicleId(), s -> s));

        assertEquals(1L, byId.get(higherVotes).rank());
        // All three tied at 5 votes share rank 2 — not 2/3/4.
        assertEquals(2L, byId.get(tiedA).rank());
        assertEquals(2L, byId.get(tiedB).rank());
        assertEquals(2L, byId.get(tiedC).rank());
        // Dense rank: no gap is left for the ties — the next distinct total is rank 3, not rank 5.
        assertEquals(3L, byId.get(lowerVotes).rank());
    }

    @Test
    void summaryReflectsCurrentUserVotesTodayAndRemainingUnits() {
        final var chronicleId = UUID.randomUUID();
        when(storyVoteQueryRepository.findPublishedChronicles(partyId))
                .thenReturn(List.of(new PublishedChronicleFixture(chronicleId, Instant.now())));
        when(allocationRepository.sumUnitsByChronicleIds(List.of(chronicleId)))
                .thenReturn(List.of(new TotalFixture(chronicleId, 3L)));
        final var chronicle = new ChronicleEntity(party, user, ChronicleType.GAME, ChronicleStatusType.PUBLISHED, "Story");
        chronicle.setId(chronicleId);
        final var myAllocation = new StoryVoteAllocationEntity(party, chronicle, membership, TODAY, (short) 1);
        when(allocationRepository.findAllByPartyIdAndMembershipIdAndVoteDate(partyId, membership.getId(), TODAY)).thenReturn(List.of(myAllocation));
        final var budget = new StoryVoteDailyBudgetEntity(party, membership, TODAY);
        budget.setUsedUnits((short) 1);
        when(budgetRepository.findByPartyIdAndMembershipIdAndVoteDate(partyId, membership.getId(), TODAY)).thenReturn(Optional.of(budget));

        final var summary = queryService.getSummary(partyId);

        assertEquals(1, summary.size());
        assertEquals(1, summary.get(0).currentUserVotesToday());
        assertEquals(1, summary.get(0).currentUserRemainingVotesToday());
        assertTrue(summary.get(0).canVote());
    }
}
