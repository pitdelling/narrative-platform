package com.narrativeplatform.app.canon.repositories;

import com.narrativeplatform.app.canon.models.entities.PartyAiTagSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PartyAiTagSettingRepository extends JpaRepository<PartyAiTagSettingEntity, UUID> {
    List<PartyAiTagSettingEntity> findAllByPartyIdOrderByDisplayOrderAsc(UUID partyId);
}
