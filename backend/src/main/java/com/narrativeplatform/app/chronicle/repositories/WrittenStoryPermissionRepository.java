package com.narrativeplatform.app.chronicle.repositories;

import com.narrativeplatform.app.chronicle.models.entities.WrittenStoryPermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;
import java.util.UUID;

public interface WrittenStoryPermissionRepository extends JpaRepository<WrittenStoryPermissionEntity, UUID> {
    Optional<WrittenStoryPermissionEntity> findByChronicleIdAndUserId(UUID chronicleId, UUID userId);
    boolean existsByChronicleIdAndUserIdAndRevokedAtIsNull(UUID chronicleId, UUID userId);

    @EntityGraph(attributePaths = "user")
    java.util.List<WrittenStoryPermissionEntity> findAllByChronicleIdAndRevokedAtIsNull(UUID chronicleId);
}
