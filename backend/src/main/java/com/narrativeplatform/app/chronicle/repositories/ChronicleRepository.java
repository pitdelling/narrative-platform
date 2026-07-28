package com.narrativeplatform.app.chronicle.repositories;

import com.narrativeplatform.app.chronicle.models.entities.ChronicleEntity;
import com.narrativeplatform.app.chronicle.models.enums.ChronicleStatusType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChronicleRepository extends JpaRepository<ChronicleEntity, UUID> {
    @EntityGraph(attributePaths = {"creator", "party", "currentGeneratedStory"})
    List<ChronicleEntity> findAllByPartyIdAndStatusNotOrderByUpdatedAtDesc(UUID partyId, ChronicleStatusType status);

    @EntityGraph(attributePaths = {"creator", "party", "currentGeneratedStory"})
    Optional<ChronicleEntity> findOneById(UUID id);
}
