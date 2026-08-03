package com.narrativeplatform.app.storyvote.repositories;

import com.narrativeplatform.app.storyvote.models.entities.StoryVoteAllocationEntity;
import com.narrativeplatform.app.storyvote.models.projections.StoryVoteTotalProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StoryVoteAllocationRepository extends JpaRepository<StoryVoteAllocationEntity, UUID> {
    Optional<StoryVoteAllocationEntity> findByChronicleIdAndMembershipIdAndVoteDate(UUID chronicleId, UUID membershipId, LocalDate voteDate);

    List<StoryVoteAllocationEntity> findAllByPartyIdAndMembershipIdAndVoteDate(UUID partyId, UUID membershipId, LocalDate voteDate);

    @Query("""
            select a.chronicle.id as chronicleId, sum(a.units) as totalVotes
            from StoryVoteAllocationEntity a
            where a.chronicle.id in :chronicleIds
            group by a.chronicle.id
            """)
    List<StoryVoteTotalProjection> sumUnitsByChronicleIds(@Param("chronicleIds") Collection<UUID> chronicleIds);
}
