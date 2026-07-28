package com.narrativeplatform.app.chronicle.models.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Table(name = "game_drafts")
public class GameDraftEntity {
    @Id
    @Column(name = "turn_id")
    private java.util.UUID turnId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "turn_id")
    @ToString.Exclude
    private GameTurnEntity turn;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content = "";

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public GameDraftEntity(final GameTurnEntity turn, final String content) {
        this.turn = turn;
        this.content = content;
    }
}
