package com.narrativeplatform.app.chronicle.models.entities;

import com.narrativeplatform.app.chronicle.models.enums.SynopsisStatusType;
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
@Table(name = "chronicle_synopses", uniqueConstraints = @UniqueConstraint(columnNames = {"chronicle_id", "version_number"}))
public class ChronicleSynopsisEntity {
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
    private SynopsisStatusType status;

    @Column(length = 240)
    private String content;

    @Column(length = 100)
    private String model;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public ChronicleSynopsisEntity(final ChronicleEntity chronicle, final int versionNumber) {
        this.chronicle = chronicle;
        this.versionNumber = versionNumber;
        this.status = SynopsisStatusType.PENDING;
    }
}
