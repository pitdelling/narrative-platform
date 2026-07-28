package com.narrativeplatform.app.chronicle.models.entities;

import com.narrativeplatform.app.chronicle.models.responses.GameSegmentResponse;

import com.narrativeplatform.app.auth.models.entities.UserEntity;
import com.narrativeplatform.app.chronicle.models.enums.SegmentSizeType;
import com.narrativeplatform.app.chronicle.models.enums.SegmentStatusType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Table(name = "game_segments", uniqueConstraints = {
        @UniqueConstraint(columnNames = "turn_id"),
        @UniqueConstraint(columnNames = {"run_id", "sequence_number"})
})
public class GameSegmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "turn_id", unique = true)
    @ToString.Exclude
    private GameTurnEntity turn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id")
    @ToString.Exclude
    private GameRunEntity run;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    @ToString.Exclude
    private UserEntity author;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @Column(name = "cycle_number", nullable = false)
    private short cycleNumber;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SegmentStatusType status;

    @Column(name = "disabled_reason", length = 600)
    private String disabledReason;

    @CreationTimestamp
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public GameSegmentResponse toVisibleResponse() {
        return new GameSegmentResponse(id, sequenceNumber, cycleNumber, author.getId(), author.getDisplayName(), status, disabledReason, true, content, sizeType(), submittedAt);
    }

    public GameSegmentResponse toHiddenResponse() {
        return new GameSegmentResponse(id, sequenceNumber, cycleNumber, author.getId(), author.getDisplayName(), status, null, false, null, sizeType(), submittedAt);
    }

    public GameSegmentResponse toPublicDisabledResponse() {
        return new GameSegmentResponse(id, sequenceNumber, cycleNumber, author.getId(), author.getDisplayName(), status, disabledReason, true, null, sizeType(), submittedAt);
    }

    public GameSegmentEntity(final GameTurnEntity turn, final String content) {
        this.turn = turn;
        this.run = turn.getRun();
        this.author = turn.getUser();
        this.sequenceNumber = turn.getSequenceNumber();
        this.cycleNumber = turn.getCycleNumber();
        this.content = content;
        this.status = SegmentStatusType.ACTIVE;
    }

    public SegmentSizeType sizeType() {
        final var normalizedContent = content == null ? "" : content;
        final var lineBreakCount = normalizedContent.chars().filter(character -> character == '\n').count();
        final var visualWeight = normalizedContent.length() + Math.toIntExact(lineBreakCount * 120);
        if (visualWeight <= 400) return SegmentSizeType.SHORT;
        if (visualWeight <= 1200) return SegmentSizeType.MEDIUM;
        if (visualWeight <= 3000) return SegmentSizeType.LONG;
        return SegmentSizeType.EXTRA_LONG;
    }
}
