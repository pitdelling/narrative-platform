package com.narrativeplatform.app.chronicle.repositories;

import com.narrativeplatform.app.chronicle.models.entities.GameRunEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface GameRunRepository extends JpaRepository<GameRunEntity, UUID> {
    @EntityGraph(attributePaths = {"chronicle", "chronicle.party", "chronicle.creator", "chronicle.currentGeneratedStory"})
    Optional<GameRunEntity> findByChronicleId(UUID chronicleId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from GameRunEntity r join fetch r.chronicle c join fetch c.party where c.id = :chronicleId")
    Optional<GameRunEntity> findForUpdateByChronicleId(@Param("chronicleId") UUID chronicleId);
}
