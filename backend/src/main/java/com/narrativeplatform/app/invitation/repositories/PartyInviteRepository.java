package com.narrativeplatform.app.invitation.repositories;

import com.narrativeplatform.app.invitation.models.entities.PartyInviteEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface PartyInviteRepository extends JpaRepository<PartyInviteEntity, UUID> {
    @EntityGraph(attributePaths = {"party", "createdBy", "consumedBy"})
    Optional<PartyInviteEntity> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from PartyInviteEntity i join fetch i.party join fetch i.createdBy left join fetch i.consumedBy where i.tokenHash = :tokenHash")
    Optional<PartyInviteEntity> findForUpdateByTokenHash(@Param("tokenHash") String tokenHash);
}
