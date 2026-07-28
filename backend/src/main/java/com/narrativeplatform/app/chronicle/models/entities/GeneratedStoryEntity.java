package com.narrativeplatform.app.chronicle.models.entities;

import com.narrativeplatform.app.chronicle.models.responses.GeneratedStoryResponse;

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
@Table(name = "generated_stories", uniqueConstraints = @UniqueConstraint(columnNames = {"chronicle_id", "version_number"}))
public class GeneratedStoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chronicle_id")
    @ToString.Exclude
    private ChronicleEntity chronicle;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public GeneratedStoryResponse toResponse() {
        return new GeneratedStoryResponse(id, versionNumber, title, content, model, createdAt);
    }

    public GeneratedStoryEntity(
            final ChronicleEntity chronicle,
            final int versionNumber,
            final String title,
            final String content,
            final String model,
            final Integer inputTokens,
            final Integer outputTokens
    ) {
        this.chronicle = chronicle;
        this.versionNumber = versionNumber;
        this.title = title;
        this.content = content;
        this.model = model;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
    }
}
