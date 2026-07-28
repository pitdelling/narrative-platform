package com.narrativeplatform.app.chronicle.models.entities;

import com.narrativeplatform.app.auth.models.entities.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Table(name = "written_story_documents")
public class WrittenStoryDocumentEntity {
    @Id
    @Column(name = "chronicle_id")
    private java.util.UUID chronicleId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "chronicle_id")
    @ToString.Exclude
    private ChronicleEntity chronicle;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content = "";

    @Column(name = "content_version", nullable = false)
    private long contentVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locked_by")
    @ToString.Exclude
    private UserEntity lockedBy;

    @Column(name = "lock_token_hash", length = 64)
    @ToString.Exclude
    private String lockTokenHash;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "lock_expires_at")
    private Instant lockExpiresAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public WrittenStoryDocumentEntity(final ChronicleEntity chronicle) {
        this.chronicle = chronicle;
    }

    public boolean hasActiveLock(final Instant now) {
        return lockedBy != null && lockExpiresAt != null && lockExpiresAt.isAfter(now);
    }
}
