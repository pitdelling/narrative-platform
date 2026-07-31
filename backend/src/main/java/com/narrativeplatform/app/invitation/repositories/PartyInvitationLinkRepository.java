package com.narrativeplatform.app.invitation.repositories;

import com.narrativeplatform.app.invitation.models.entities.PartyInvitationLinkEntity;
import com.narrativeplatform.app.party.models.enums.PartyRoleType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PartyInvitationLinkRepository extends JpaRepository<PartyInvitationLinkEntity, UUID> {
    @EntityGraph(attributePaths = {"party", "createdBy"})
    Optional<PartyInvitationLinkEntity> findByTokenHash(String tokenHash);

    @EntityGraph(attributePaths = {"party", "createdBy"})
    @Query("select l from PartyInvitationLinkEntity l where l.party.id = :partyId and l.targetRole = :targetRole")
    Optional<PartyInvitationLinkEntity> findByPartyIdAndTargetRole(
            @Param("partyId") UUID partyId,
            @Param("targetRole") PartyRoleType targetRole
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from PartyInvitationLinkEntity l where l.party.id = :partyId and l.targetRole = :targetRole")
    Optional<PartyInvitationLinkEntity> findForUpdateByPartyIdAndTargetRole(
            @Param("partyId") UUID partyId,
            @Param("targetRole") PartyRoleType targetRole
    );
}
