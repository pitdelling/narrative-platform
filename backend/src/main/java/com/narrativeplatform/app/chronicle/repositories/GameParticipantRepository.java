package com.narrativeplatform.app.chronicle.repositories;

import com.narrativeplatform.app.chronicle.models.entities.GameParticipantEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameParticipantRepository extends JpaRepository<GameParticipantEntity, UUID> {
    @EntityGraph(attributePaths = "user")
    Optional<GameParticipantEntity> findByRunIdAndUserId(UUID runId, UUID userId);

    @EntityGraph(attributePaths = {"user", "removedByUser"})
    List<GameParticipantEntity> findAllByRunIdOrderByCreatedAtAsc(UUID runId);
}
