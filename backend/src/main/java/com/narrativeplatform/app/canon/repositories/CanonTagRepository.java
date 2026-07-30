package com.narrativeplatform.app.canon.repositories;

import com.narrativeplatform.app.canon.models.entities.CanonTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CanonTagRepository extends JpaRepository<CanonTagEntity, UUID> {
    List<CanonTagEntity> findAllByGenerationIdOrderByCategoryAscDisplayOrderAsc(UUID generationId);
}
