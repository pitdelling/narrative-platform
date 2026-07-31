package com.narrativeplatform.app.chronicle.models.entities;

import com.narrativeplatform.app.auth.models.entities.UserEntity;
import com.narrativeplatform.app.chronicle.models.enums.GameParticipantStatusType;
import com.narrativeplatform.app.chronicle.models.enums.RemovedByType;
import com.narrativeplatform.app.chronicle.models.responses.GameParticipantResponse;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Table(name = "game_participants", uniqueConstraints = @UniqueConstraint(columnNames = {"run_id", "user_id"}))
public class GameParticipantEntity {
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GameParticipantStatusType status;

    @Enumerated(EnumType.STRING)
    @Column(name = "removed_by_type", length = 20)
    private RemovedByType removedByType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "removed_by_user_id")
    @ToString.Exclude
    private UserEntity removedByUser;

    @Column(name = "left_at")
    private Instant leftAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public GameParticipantEntity(final GameRunEntity run, final UserEntity user) {
        this.run = run;
        this.user = user;
        this.status = GameParticipantStatusType.ACTIVE;
        this.createdAt = Instant.now();
    }

    public void markLeft(final RemovedByType removedByType, final UserEntity removedByUser) {
        this.status = GameParticipantStatusType.LEFT;
        this.removedByType = removedByType;
        this.removedByUser = removedByUser;
        this.leftAt = Instant.now();
    }

    public void markRejoined() {
        this.status = GameParticipantStatusType.ACTIVE;
        this.removedByType = null;
        this.removedByUser = null;
        this.leftAt = null;
    }

    public GameParticipantResponse toResponse() {
        return new GameParticipantResponse(user.getId(), user.getDisplayName(), status, removedByType);
    }
}
