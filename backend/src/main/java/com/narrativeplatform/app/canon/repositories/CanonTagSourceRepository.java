package com.narrativeplatform.app.canon.repositories;

import com.narrativeplatform.app.canon.models.entities.CanonTagSourceEntity;
import com.narrativeplatform.app.canon.models.projections.TagSourcePositionProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CanonTagSourceRepository extends JpaRepository<CanonTagSourceEntity, UUID> {
    @Query("""
            select s.tag.id as tagId, s.segment.sequenceNumber as sequenceNumber
            from CanonTagSourceEntity s
            where s.tag.generation.id = :generationId
            """)
    List<TagSourcePositionProjection> findPositionsByGenerationId(@Param("generationId") UUID generationId);
}
