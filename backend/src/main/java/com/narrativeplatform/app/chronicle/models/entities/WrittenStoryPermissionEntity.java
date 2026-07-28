package com.narrativeplatform.app.chronicle.models.entities;

import com.narrativeplatform.app.auth.models.entities.UserEntity;
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
@Table(name = "written_story_permissions", uniqueConstraints = @UniqueConstraint(columnNames = {"chronicle_id", "user_id"}))
public class WrittenStoryPermissionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chronicle_id")
    @ToString.Exclude
    private ChronicleEntity chronicle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "granted_by")
    @ToString.Exclude
    private UserEntity grantedBy;

    @CreationTimestamp
    @Column(name = "granted_at", nullable = false, updatable = false)
    private Instant grantedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public WrittenStoryPermissionEntity(final ChronicleEntity chronicle, final UserEntity user, final UserEntity grantedBy) {
        this.chronicle = chronicle;
        this.user = user;
        this.grantedBy = grantedBy;
    }
}
