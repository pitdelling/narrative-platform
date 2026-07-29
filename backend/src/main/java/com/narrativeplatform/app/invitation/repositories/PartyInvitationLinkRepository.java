package com.narrativeplatform.app.invitation.repositories;

import com.narrativeplatform.app.invitation.models.entities.PartyInvitationLinkEntity;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from PartyInvitationLinkEntity l where l.partyId = :partyId")
    Optional<PartyInvitationLinkEntity> findForUpdateById(@Param("partyId") UUID partyId);
}
