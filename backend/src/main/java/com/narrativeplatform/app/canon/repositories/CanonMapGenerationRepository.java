package com.narrativeplatform.app.canon.repositories;

import com.narrativeplatform.app.canon.models.entities.CanonMapGenerationEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CanonMapGenerationRepository extends JpaRepository<CanonMapGenerationEntity, UUID> {
    Optional<CanonMapGenerationEntity> findFirstByChronicleIdOrderByVersionNumberDesc(UUID chronicleId);

    long countByChronicleId(UUID chronicleId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from CanonMapGenerationEntity g join fetch g.chronicle where g.id = :id")
    Optional<CanonMapGenerationEntity> findForUpdate(@Param("id") UUID id);
}
