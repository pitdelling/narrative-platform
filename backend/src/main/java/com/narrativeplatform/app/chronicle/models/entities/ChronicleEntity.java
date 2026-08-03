package com.narrativeplatform.app.chronicle.models.entities;

import com.narrativeplatform.app.chronicle.models.responses.ChronicleCardResponse;

import com.narrativeplatform.app.auth.models.entities.UserEntity;
import com.narrativeplatform.app.chronicle.models.enums.ChronicleStatusType;
import com.narrativeplatform.app.chronicle.models.enums.ChronicleType;
import com.narrativeplatform.app.party.models.entities.PartyEntity;
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
@Table(name = "chronicles")
public class ChronicleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "party_id")
    @ToString.Exclude
    private PartyEntity party;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id")
    @ToString.Exclude
    private UserEntity creator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChronicleType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChronicleStatusType status;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(name = "generated_preview", length = 600)
    private String generatedPreview;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_generated_story_id")
    @ToString.Exclude
    private GeneratedStoryEntity currentGeneratedStory;

    @Column(length = 240)
    private String synopsis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_synopsis_id")
    @ToString.Exclude
    private ChronicleSynopsisEntity currentSynopsis;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Version
    @Column(nullable = false)
    private long version;

    public ChronicleCardResponse toCardResponse(final Integer completedTurns, final Integer totalTurns, final Boolean awaitingCurrentUser) {
        final var preview = status == ChronicleStatusType.PUBLISHED && synopsis != null ? synopsis : generatedPreview;
        return new ChronicleCardResponse(
                id, type, status, title, preview, creator.getDisplayName(), createdAt, updatedAt, publishedAt,
                status == ChronicleStatusType.PUBLISHED, completedTurns, totalTurns, awaitingCurrentUser
        );
    }

    public ChronicleEntity(
            final PartyEntity party,
            final UserEntity creator,
            final ChronicleType type,
            final ChronicleStatusType status,
            final String title
    ) {
        this.party = party;
        this.creator = creator;
        this.type = type;
        this.status = status;
        this.title = title;
    }
}
