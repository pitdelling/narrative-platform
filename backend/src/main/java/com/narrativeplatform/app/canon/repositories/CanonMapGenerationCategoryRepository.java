package com.narrativeplatform.app.canon.repositories;

import com.narrativeplatform.app.canon.models.entities.CanonMapGenerationCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CanonMapGenerationCategoryRepository extends JpaRepository<CanonMapGenerationCategoryEntity, UUID> {
    List<CanonMapGenerationCategoryEntity> findAllByGenerationIdOrderByDisplayOrderAsc(UUID generationId);
}
