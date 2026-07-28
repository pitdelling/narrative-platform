package com.narrativeplatform.app.chronicle.models.entities;

import com.narrativeplatform.app.auth.models.entities.UserEntity;
import com.narrativeplatform.app.chronicle.models.enums.SegmentStatusType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Table(name = "segment_revisions")
public class SegmentRevisionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "segment_id")
    @ToString.Exclude
    private GameSegmentEntity segment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by")
    @ToString.Exclude
    private UserEntity changedBy;

    @Column(name = "previous_content", nullable = false, columnDefinition = "TEXT")
    private String previousContent;

    @Column(name = "new_content", columnDefinition = "TEXT")
    private String newContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = false, length = 30)
    private SegmentStatusType previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 30)
    private SegmentStatusType newStatus;

    @Column(length = 600)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public SegmentRevisionEntity(
            final GameSegmentEntity segment,
            final UserEntity changedBy,
            final String previousContent,
            final String newContent,
            final SegmentStatusType previousStatus,
            final SegmentStatusType newStatus,
            final String reason
    ) {
        this.segment = segment;
        this.changedBy = changedBy;
        this.previousContent = previousContent;
        this.newContent = newContent;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reason = reason;
    }
}
