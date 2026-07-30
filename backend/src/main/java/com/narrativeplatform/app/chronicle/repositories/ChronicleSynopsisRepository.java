package com.narrativeplatform.app.chronicle.repositories;

import com.narrativeplatform.app.chronicle.models.entities.ChronicleSynopsisEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ChronicleSynopsisRepository extends JpaRepository<ChronicleSynopsisEntity, UUID> {
    Optional<ChronicleSynopsisEntity> findFirstByChronicleIdOrderByVersionNumberDesc(UUID chronicleId);

    long countByChronicleId(UUID chronicleId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ChronicleSynopsisEntity s join fetch s.chronicle where s.id = :id")
    Optional<ChronicleSynopsisEntity> findForUpdate(@Param("id") UUID id);
}
