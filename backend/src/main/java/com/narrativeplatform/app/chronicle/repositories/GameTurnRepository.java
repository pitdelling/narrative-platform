package com.narrativeplatform.app.chronicle.repositories;

import com.narrativeplatform.app.chronicle.models.entities.GameTurnEntity;
import com.narrativeplatform.app.chronicle.models.enums.GameTurnStatusType;
import com.narrativeplatform.app.chronicle.models.projections.ActiveTurnUserProjection;
import com.narrativeplatform.app.chronicle.models.projections.ChronicleTurnProgressProjection;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameTurnRepository extends JpaRepository<GameTurnEntity, UUID> {
    @EntityGraph(attributePaths = "user")
    List<GameTurnEntity> findAllByRunIdOrderBySequenceNumberAsc(UUID runId);

    @EntityGraph(attributePaths = {"user", "run", "run.chronicle"})
    Optional<GameTurnEntity> findByRunIdAndSequenceNumber(UUID runId, int sequenceNumber);

    @EntityGraph(attributePaths = {"user", "run", "run.chronicle"})
    List<GameTurnEntity> findAllByStatusAndExpiresAtBefore(GameTurnStatusType status, Instant expiresAt);

    @EntityGraph(attributePaths = "user")
    List<GameTurnEntity> findAllByRunIdAndUserId(UUID runId, UUID userId);

    @Query("""
            select t.run.chronicle.id as chronicleId,
                   sum(case when t.status = com.narrativeplatform.app.chronicle.models.enums.GameTurnStatusType.WAITING
                              or t.status = com.narrativeplatform.app.chronicle.models.enums.GameTurnStatusType.ACTIVE
                            then 0 else 1 end) as completedTurns,
                   count(t) as totalTurns
            from GameTurnEntity t
            where t.run.chronicle.id in :chronicleIds
            group by t.run.chronicle.id
            """)
    List<ChronicleTurnProgressProjection> findProgressByChronicleIds(@Param("chronicleIds") Collection<UUID> chronicleIds);

    @Query("""
            select t.run.chronicle.id as chronicleId, t.user.id as userId
            from GameTurnEntity t
            where t.status = com.narrativeplatform.app.chronicle.models.enums.GameTurnStatusType.ACTIVE
              and t.run.chronicle.id in :chronicleIds
            """)
    List<ActiveTurnUserProjection> findActiveTurnUsersByChronicleIds(@Param("chronicleIds") Collection<UUID> chronicleIds);
}
