package com.narrativeplatform.app.chronicle.repositories;

import com.narrativeplatform.app.chronicle.models.entities.GameSegmentEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameSegmentRepository extends JpaRepository<GameSegmentEntity, UUID> {
    @EntityGraph(attributePaths = {"author", "turn"})
    List<GameSegmentEntity> findAllByRunIdOrderBySequenceNumberAsc(UUID runId);

    Optional<GameSegmentEntity> findByRunIdAndSequenceNumber(UUID runId, int sequenceNumber);
}
