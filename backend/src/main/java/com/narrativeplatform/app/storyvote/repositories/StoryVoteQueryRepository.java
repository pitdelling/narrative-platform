package com.narrativeplatform.app.storyvote.repositories;

import com.narrativeplatform.app.chronicle.models.entities.ChronicleEntity;
import com.narrativeplatform.app.storyvote.models.projections.PublishedChronicleProjection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface StoryVoteQueryRepository extends Repository<ChronicleEntity, UUID> {
    @Query("""
            select c.id as chronicleId, c.publishedAt as publishedAt
            from ChronicleEntity c
            where c.party.id = :partyId
              and c.status = com.narrativeplatform.app.chronicle.models.enums.ChronicleStatusType.PUBLISHED
            """)
    List<PublishedChronicleProjection> findPublishedChronicles(@Param("partyId") UUID partyId);
}
