package com.narrativeplatform.app.chronicle.repositories;

import com.narrativeplatform.app.chronicle.models.entities.WrittenStoryDocumentEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface WrittenStoryDocumentRepository extends JpaRepository<WrittenStoryDocumentEntity, UUID> {
    @EntityGraph(attributePaths = {"chronicle", "lockedBy"})
    Optional<WrittenStoryDocumentEntity> findByChronicleId(UUID chronicleId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from WrittenStoryDocumentEntity d left join fetch d.lockedBy where d.chronicle.id = :chronicleId")
    Optional<WrittenStoryDocumentEntity> findForUpdate(@Param("chronicleId") UUID chronicleId);
}
