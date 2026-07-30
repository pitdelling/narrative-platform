package com.narrativeplatform.app.canon.models.entities;

import com.narrativeplatform.app.canon.models.enums.CanonMapStatusType;
import com.narrativeplatform.app.chronicle.models.entities.ChronicleEntity;
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
@Table(name = "canon_map_generations", uniqueConstraints = @UniqueConstraint(columnNames = {"chronicle_id", "version_number"}))
public class CanonMapGenerationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chronicle_id")
    @ToString.Exclude
    private ChronicleEntity chronicle;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CanonMapStatusType status;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public CanonMapGenerationEntity(final ChronicleEntity chronicle, final int versionNumber) {
        this.chronicle = chronicle;
        this.versionNumber = versionNumber;
        this.status = CanonMapStatusType.PENDING;
    }
}
