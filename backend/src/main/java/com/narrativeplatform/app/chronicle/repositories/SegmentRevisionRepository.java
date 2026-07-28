package com.narrativeplatform.app.chronicle.repositories;

import com.narrativeplatform.app.chronicle.models.entities.SegmentRevisionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SegmentRevisionRepository extends JpaRepository<SegmentRevisionEntity, UUID> {
}
