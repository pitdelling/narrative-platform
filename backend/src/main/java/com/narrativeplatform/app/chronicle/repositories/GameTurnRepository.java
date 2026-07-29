package com.narrativeplatform.app.chronicle.repositories;

import com.narrativeplatform.app.chronicle.models.entities.GameTurnEntity;
import com.narrativeplatform.app.chronicle.models.enums.GameTurnStatusType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
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
}
