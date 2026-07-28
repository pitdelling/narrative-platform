package com.narrativeplatform.app.chronicle.models.entities;

import com.narrativeplatform.app.chronicle.models.responses.GameTurnResponse;

import com.narrativeplatform.app.auth.models.entities.UserEntity;
import com.narrativeplatform.app.chronicle.models.enums.GameTurnStatusType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Table(name = "game_turns", uniqueConstraints = @UniqueConstraint(columnNames = {"run_id", "sequence_number"}))
public class GameTurnEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id")
    @ToString.Exclude
    private GameRunEntity run;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    private UserEntity user;

    @Column(name = "cycle_number", nullable = false)
    private short cycleNumber;

    @Column(name = "position_in_cycle", nullable = false)
    private int positionInCycle;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GameTurnStatusType status;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skipped_by")
    @ToString.Exclude
    private UserEntity skippedBy;

    public GameTurnResponse toResponse() {
        return new GameTurnResponse(id, sequenceNumber, cycleNumber, positionInCycle, user.getId(), user.getDisplayName(), status, startedAt, expiresAt);
    }

    public GameTurnEntity(
            final GameRunEntity run,
            final UserEntity user,
            final short cycleNumber,
            final int positionInCycle,
            final int sequenceNumber
    ) {
        this.run = run;
        this.user = user;
        this.cycleNumber = cycleNumber;
        this.positionInCycle = positionInCycle;
        this.sequenceNumber = sequenceNumber;
        this.status = GameTurnStatusType.WAITING;
    }
}
