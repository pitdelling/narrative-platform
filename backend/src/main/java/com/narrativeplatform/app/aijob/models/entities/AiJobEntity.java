package com.narrativeplatform.app.aijob.models.entities;

import com.narrativeplatform.app.aijob.models.enums.AiJobStatusType;
import com.narrativeplatform.app.aijob.models.enums.AiJobType;
import com.narrativeplatform.app.auth.models.entities.UserEntity;
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
@Table(name = "ai_jobs")
public class AiJobEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chronicle_id")
    @ToString.Exclude
    private ChronicleEntity chronicle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by")
    @ToString.Exclude
    private UserEntity requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AiJobStatusType status;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 30)
    private AiJobType jobType;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 120)
    private String idempotencyKey;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public AiJobEntity(
            final ChronicleEntity chronicle,
            final UserEntity requestedBy,
            final String idempotencyKey,
            final AiJobType jobType
    ) {
        this.chronicle = chronicle;
        this.requestedBy = requestedBy;
        this.idempotencyKey = idempotencyKey;
        this.jobType = jobType;
        this.status = AiJobStatusType.PENDING;
    }
}
