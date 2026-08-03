package com.narrativeplatform.app.storyvote.repositories;

import com.narrativeplatform.app.storyvote.models.entities.StoryVoteDailyBudgetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface StoryVoteDailyBudgetRepository extends JpaRepository<StoryVoteDailyBudgetEntity, UUID> {
    Optional<StoryVoteDailyBudgetEntity> findByPartyIdAndMembershipIdAndVoteDate(UUID partyId, UUID membershipId, LocalDate voteDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from StoryVoteDailyBudgetEntity b where b.party.id = :partyId and b.membership.id = :membershipId and b.voteDate = :voteDate")
    Optional<StoryVoteDailyBudgetEntity> findForUpdateByPartyIdAndMembershipIdAndVoteDate(
            @Param("partyId") UUID partyId,
            @Param("membershipId") UUID membershipId,
            @Param("voteDate") LocalDate voteDate
    );
}
