package com.narrativeplatform.app.canon.repositories;

import com.narrativeplatform.app.canon.models.entities.CanonCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CanonCategoryRepository extends JpaRepository<CanonCategoryEntity, UUID> {
    List<CanonCategoryEntity> findAllByPartyIdOrderByDisplayOrderAsc(UUID partyId);

    Optional<CanonCategoryEntity> findByIdAndPartyId(UUID id, UUID partyId);
}
