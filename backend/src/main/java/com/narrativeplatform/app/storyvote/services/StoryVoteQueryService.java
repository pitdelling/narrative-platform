package com.narrativeplatform.app.storyvote.services;

import com.narrativeplatform.app.party.services.PartyAccessService;
import com.narrativeplatform.app.storyvote.models.entities.StoryVoteAllocationEntity;
import com.narrativeplatform.app.storyvote.models.projections.PublishedChronicleProjection;
import com.narrativeplatform.app.storyvote.models.projections.StoryVoteTotalProjection;
import com.narrativeplatform.app.storyvote.models.responses.DailyStoryVoteStateResponse;
import com.narrativeplatform.app.storyvote.models.responses.StoryVoteAllocationResponse;
import com.narrativeplatform.app.storyvote.models.responses.StoryVoteSummaryResponse;
import com.narrativeplatform.app.storyvote.repositories.StoryVoteAllocationRepository;
import com.narrativeplatform.app.storyvote.repositories.StoryVoteDailyBudgetRepository;
import com.narrativeplatform.app.storyvote.repositories.StoryVoteQueryRepository;
import com.narrativeplatform.shared.constants.DomainConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoryVoteQueryService {
    private final PartyAccessService partyAccessService;
    private final StoryVoteDailyBudgetRepository budgetRepository;
    private final StoryVoteAllocationRepository allocationRepository;
    private final StoryVoteQueryRepository storyVoteQueryRepository;
    private final Clock clock;

    public DailyStoryVoteStateResponse getDailyState(final UUID partyId) {
        final var membership = partyAccessService.requireActiveMember(partyId);
        return buildDailyState(partyId, membership.getId(), LocalDate.now(clock));
    }

    public DailyStoryVoteStateResponse buildDailyState(final UUID partyId, final UUID membershipId, final LocalDate date) {
        final var usedUnits = budgetRepository.findByPartyIdAndMembershipIdAndVoteDate(partyId, membershipId, date)
                .map(budget -> (int) budget.getUsedUnits())
                .orElse(0);
        final var allocations = allocationRepository.findAllByPartyIdAndMembershipIdAndVoteDate(partyId, membershipId, date).stream()
                .map(allocation -> new StoryVoteAllocationResponse(allocation.getChronicle().getId(), allocation.getUnits()))
                .toList();
        return new DailyStoryVoteStateResponse(
                date, DomainConstants.STORY_VOTE_DAILY_LIMIT, usedUnits,
                DomainConstants.STORY_VOTE_DAILY_LIMIT - usedUnits, allocations
        );
    }

    public List<StoryVoteSummaryResponse> getSummary(final UUID partyId) {
        final var membership = partyAccessService.requireActiveMember(partyId);
        final var date = LocalDate.now(clock);
        final var published = storyVoteQueryRepository.findPublishedChronicles(partyId);
        if (published.isEmpty()) {
            return List.of();
        }

        final var chronicleIds = published.stream().map(PublishedChronicleProjection::getChronicleId).toList();
        final var totalsByChronicle = allocationRepository.sumUnitsByChronicleIds(chronicleIds).stream()
                .collect(Collectors.toMap(StoryVoteTotalProjection::getChronicleId, StoryVoteTotalProjection::getTotalVotes));
        final var votesTodayByChronicle = allocationRepository.findAllByPartyIdAndMembershipIdAndVoteDate(partyId, membership.getId(), date).stream()
                .collect(Collectors.toMap(allocation -> allocation.getChronicle().getId(), StoryVoteAllocationEntity::getUnits));
        final var usedUnitsToday = budgetRepository.findByPartyIdAndMembershipIdAndVoteDate(partyId, membership.getId(), date)
                .map(budget -> (int) budget.getUsedUnits())
                .orElse(0);
        final var remainingUnitsToday = DomainConstants.STORY_VOTE_DAILY_LIMIT - usedUnitsToday;

        final var ranked = published.stream()
                .sorted(Comparator
                        .comparingLong((PublishedChronicleProjection chronicle) -> totalsByChronicle.getOrDefault(chronicle.getChronicleId(), 0L))
                        .reversed()
                        .thenComparing(PublishedChronicleProjection::getPublishedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(PublishedChronicleProjection::getChronicleId))
                .toList();

        final var summaries = new ArrayList<StoryVoteSummaryResponse>(ranked.size());
        Long currentRank = null;
        long lastVotes = -1;
        for (final var chronicle : ranked) {
            final var totalVotes = totalsByChronicle.getOrDefault(chronicle.getChronicleId(), 0L);
            final Long rank;
            if (totalVotes == 0) {
                // Stories with no votes are excluded from the ranking entirely (never a rank of their own).
                rank = null;
            } else if (currentRank == null || totalVotes != lastVotes) {
                currentRank = currentRank == null ? 1L : currentRank + 1;
                rank = currentRank;
            } else {
                // Dense rank: a tie on totalVotes shares the same position, no gap for the next one.
                rank = currentRank;
            }
            lastVotes = totalVotes;

            final var votesToday = votesTodayByChronicle.getOrDefault(chronicle.getChronicleId(), (short) 0);
            summaries.add(new StoryVoteSummaryResponse(
                    chronicle.getChronicleId(), totalVotes, rank, votesToday, remainingUnitsToday, true, null
            ));
        }
        return summaries;
    }
}
