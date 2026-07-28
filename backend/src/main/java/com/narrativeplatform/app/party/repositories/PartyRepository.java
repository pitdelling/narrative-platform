package com.narrativeplatform.app.party.repositories;

import com.narrativeplatform.app.party.models.entities.PartyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PartyRepository extends JpaRepository<PartyEntity, UUID> {
    boolean existsBySlug(String slug);
}
