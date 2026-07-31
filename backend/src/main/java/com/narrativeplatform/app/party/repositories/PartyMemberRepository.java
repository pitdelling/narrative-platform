package com.narrativeplatform.app.party.repositories;

import com.narrativeplatform.app.party.models.entities.PartyMemberEntity;
import com.narrativeplatform.app.party.models.enums.MemberStatusType;
import com.narrativeplatform.app.party.models.enums.PartyRoleType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PartyMemberRepository extends JpaRepository<PartyMemberEntity, UUID> {
    @EntityGraph(attributePaths = {"party", "party.owner"})
    List<PartyMemberEntity> findAllByUserIdAndStatusOrderByJoinedAtAsc(UUID userId, MemberStatusType status);

    @EntityGraph(attributePaths = {"party", "party.owner", "user"})
    Optional<PartyMemberEntity> findByPartyIdAndUserId(UUID partyId, UUID userId);

    @EntityGraph(attributePaths = "user")
    List<PartyMemberEntity> findAllByPartyIdAndStatusNotOrderByJoinedAtAsc(UUID partyId, MemberStatusType status);

    @EntityGraph(attributePaths = "user")
    List<PartyMemberEntity> findAllByPartyIdAndStatusOrderByJoinedAtAsc(UUID partyId, MemberStatusType status);

    @EntityGraph(attributePaths = "user")
    List<PartyMemberEntity> findAllByPartyIdAndStatusAndRoleNotOrderByJoinedAtAsc(UUID partyId, MemberStatusType status, PartyRoleType role);
}
