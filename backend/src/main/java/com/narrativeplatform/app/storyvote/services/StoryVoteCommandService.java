package com.narrativeplatform.app.storyvote.services;

import com.narrativeplatform.app.chronicle.models.enums.ChronicleStatusType;
import com.narrativeplatform.app.chronicle.services.ChronicleAccessService;
import com.narrativeplatform.app.party.models.entities.PartyEntity;
import com.narrativeplatform.app.party.models.entities.PartyMemberEntity;
import com.narrativeplatform.app.storyvote.models.entities.StoryVoteAllocationEntity;
import com.narrativeplatform.app.storyvote.models.entities.StoryVoteDailyBudgetEntity;
import com.narrativeplatform.app.storyvote.models.responses.DailyStoryVoteStateResponse;
import com.narrativeplatform.app.storyvote.repositories.StoryVoteAllocationRepository;
import com.narrativeplatform.app.storyvote.repositories.StoryVoteDailyBudgetRepository;
import com.narrativeplatform.shared.constants.DomainConstants;
import com.narrativeplatform.shared.exceptions.InvalidStoryVoteUnitsException;
import com.narrativeplatform.shared.exceptions.StoryNotCompletedException;
import com.narrativeplatform.shared.exceptions.StoryVoteBudgetExceededException;
import com.narrativeplatform.shared.exceptions.StoryVoteConcurrencyConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoryVoteCommandService {
    private final ChronicleAccessService chronicleAccessService;
    private final StoryVoteDailyBudgetRepository budgetRepository;
    private final StoryVoteAllocationRepository allocationRepository;
    private final StoryVoteQueryService storyVoteQueryService;
    private final Clock clock;

    @Transactional
    public DailyStoryVoteStateResponse setVote(final UUID partyId, final UUID chronicleId, final Integer units) {
        if (units == null || units < 0 || units > DomainConstants.STORY_VOTE_DAILY_LIMIT) {
            throw new InvalidStoryVoteUnitsException("Units must be 0, 1 or 2.");
        }

        final var context = chronicleAccessService.requireMember(partyId, chronicleId);
        final var chronicle = context.chronicle();
        if (chronicle.getStatus() != ChronicleStatusType.PUBLISHED) {
            throw new StoryNotCompletedException("Only completed stories can receive votes.");
        }

        final var membership = context.membership();
        final var date = LocalDate.now(clock);

        try {
            final var budget = getOrCreateBudgetForUpdate(chronicle.getParty(), membership, date);
            final var existingAllocation = allocationRepository.findByChronicleIdAndMembershipIdAndVoteDate(chronicleId, membership.getId(), date);
            final var currentUnits = existingAllocation.map(allocation -> (int) allocation.getUnits()).orElse(0);
            final var delta = units - currentUnits;
            final var newUsedUnits = budget.getUsedUnits() + delta;

            if (newUsedUnits < 0 || newUsedUnits > DomainConstants.STORY_VOTE_DAILY_LIMIT) {
                throw new StoryVoteBudgetExceededException("Not enough remaining votes today for this party.");
            }

            if (units == 0) {
                existingAllocation.ifPresent(allocationRepository::delete);
            } else if (existingAllocation.isPresent()) {
                existingAllocation.get().setUnits(units.shortValue());
            } else {
                allocationRepository.save(new StoryVoteAllocationEntity(chronicle.getParty(), chronicle, membership, date, units.shortValue()));
            }
            budget.setUsedUnits((short) newUsedUnits);

            return storyVoteQueryService.buildDailyState(partyId, membership.getId(), date);
        } catch (final ConcurrencyFailureException concurrencyFailure) {
            throw new StoryVoteConcurrencyConflictException();
        }
    }

    /**
     * Mirrors {@code InvitationService.getOrCreateForUpdate}: the unique constraint on
     * (party_id, membership_id, vote_date) makes a second budget row for the same day
     * structurally impossible, and the pessimistic write lock taken by
     * {@code findForUpdateByPartyIdAndMembershipIdAndVoteDate} serializes concurrent
     * callers so a second request always observes the first one's committed row instead
     * of racing to insert a duplicate.
     */
    private StoryVoteDailyBudgetEntity getOrCreateBudgetForUpdate(
            final PartyEntity party, final PartyMemberEntity membership, final LocalDate date
    ) {
        return budgetRepository.findForUpdateByPartyIdAndMembershipIdAndVoteDate(party.getId(), membership.getId(), date)
                .orElseGet(() -> {
                    try {
                        return budgetRepository.saveAndFlush(new StoryVoteDailyBudgetEntity(party, membership, date));
                    } catch (final DataIntegrityViolationException raceLost) {
                        return budgetRepository.findForUpdateByPartyIdAndMembershipIdAndVoteDate(party.getId(), membership.getId(), date)
                                .orElseThrow(() -> raceLost);
                    }
                });
    }
}
