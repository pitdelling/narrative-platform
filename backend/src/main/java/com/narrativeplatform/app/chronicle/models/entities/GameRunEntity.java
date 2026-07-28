package com.narrativeplatform.app.chronicle.models.entities;

import com.narrativeplatform.app.chronicle.models.enums.GameRunStatusType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Table(name = "game_runs")
public class GameRunEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chronicle_id", unique = true)
    @ToString.Exclude
    private ChronicleEntity chronicle;

    @Column(name = "cycle_count", nullable = false)
    private short cycleCount;

    @Column(name = "participant_count", nullable = false)
    private int participantCount;

    @Column(name = "current_sequence", nullable = false)
    private int currentSequence = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GameRunStatusType status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public GameRunEntity(final ChronicleEntity chronicle, final short cycleCount, final int participantCount) {
        this.chronicle = chronicle;
        this.cycleCount = cycleCount;
        this.participantCount = participantCount;
        this.status = GameRunStatusType.IN_PROGRESS;
        this.startedAt = Instant.now();
    }
}
