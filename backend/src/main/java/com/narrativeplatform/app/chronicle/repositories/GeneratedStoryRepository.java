package com.narrativeplatform.app.chronicle.repositories;

import com.narrativeplatform.app.chronicle.models.entities.GeneratedStoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GeneratedStoryRepository extends JpaRepository<GeneratedStoryEntity, UUID> {
    long countByChronicleId(UUID chronicleId);

    List<GeneratedStoryEntity> findAllByChronicleIdOrderByVersionNumberDesc(UUID chronicleId);
}
