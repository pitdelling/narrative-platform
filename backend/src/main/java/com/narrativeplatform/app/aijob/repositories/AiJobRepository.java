package com.narrativeplatform.app.aijob.repositories;

import com.narrativeplatform.app.aijob.models.entities.AiJobEntity;
import com.narrativeplatform.app.aijob.models.enums.AiJobStatusType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AiJobRepository extends JpaRepository<AiJobEntity, UUID> {
    @EntityGraph(attributePaths = {"chronicle", "requestedBy"})
    List<AiJobEntity> findTop5ByStatusOrderByCreatedAtAsc(AiJobStatusType status);

    boolean existsByChronicleIdAndStatusIn(UUID chronicleId, Collection<AiJobStatusType> statuses);

    @EntityGraph(attributePaths = "chronicle")
    List<AiJobEntity> findAllByStatusAndStartedAtBefore(AiJobStatusType status, Instant startedAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from AiJobEntity j join fetch j.chronicle where j.id = :jobId")
    java.util.Optional<AiJobEntity> findForUpdate(@Param("jobId") UUID jobId);
}
