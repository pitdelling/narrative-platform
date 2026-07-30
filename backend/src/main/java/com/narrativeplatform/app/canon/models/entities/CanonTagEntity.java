package com.narrativeplatform.app.canon.models.entities;

import com.narrativeplatform.app.canon.models.enums.CanonCategoryType;
import com.narrativeplatform.app.canon.models.enums.TagBasisType;
import com.narrativeplatform.app.canon.models.enums.TagColorType;
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
@Table(name = "canon_tags", uniqueConstraints = @UniqueConstraint(columnNames = {"generation_id", "category", "normalized_name"}))
public class CanonTagEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generation_id")
    @ToString.Exclude
    private CanonMapGenerationEntity generation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CanonCategoryType category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TagColorType color;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 160)
    private String normalizedName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "visual_description", nullable = false, columnDefinition = "TEXT")
    private String visualDescription;

    @Column(name = "personality_description", columnDefinition = "TEXT")
    private String personalityDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "visual_basis", nullable = false, length = 20)
    private TagBasisType visualBasis;

    @Enumerated(EnumType.STRING)
    @Column(name = "personality_basis", length = 20)
    private TagBasisType personalityBasis;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public CanonTagEntity(
            final CanonMapGenerationEntity generation,
            final CanonCategoryType category,
            final TagColorType color,
            final int displayOrder,
            final String name,
            final String normalizedName,
            final String summary,
            final String visualDescription,
            final String personalityDescription,
            final TagBasisType visualBasis,
            final TagBasisType personalityBasis
    ) {
        this.generation = generation;
        this.category = category;
        this.color = color;
        this.displayOrder = displayOrder;
        this.name = name;
        this.normalizedName = normalizedName;
        this.summary = summary;
        this.visualDescription = visualDescription;
        this.personalityDescription = personalityDescription;
        this.visualBasis = visualBasis;
        this.personalityBasis = personalityBasis;
    }
}
