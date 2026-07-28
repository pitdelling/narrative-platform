package com.narrativeplatform.app.chronicle.repositories;

import com.narrativeplatform.app.chronicle.models.entities.GameDraftEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GameDraftRepository extends JpaRepository<GameDraftEntity, UUID> {
}
