package com.narrativeplatform.app.canon.models.entities;

import com.narrativeplatform.app.canon.models.enums.CanonCategoryType;
import com.narrativeplatform.app.canon.models.enums.TagColorType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Snapshot of one {@link PartyAiTagSettingEntity} row at the moment a {@link CanonMapGenerationEntity}
 * was created. Copied by value and never re-read from the live party settings, so a later change
 * to the party's tag configuration never retroactively alters an already-generated canon map.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Table(name = "canon_map_generation_categories", uniqueConstraints = @UniqueConstraint(columnNames = {"generation_id", "category"}))
public class CanonMapGenerationCategoryEntity {
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

    @Column(nullable = false)
    private boolean enabled;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TagColorType color;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public CanonMapGenerationCategoryEntity(
            final CanonMapGenerationEntity generation,
            final CanonCategoryType category,
            final boolean enabled,
            final TagColorType color,
            final int displayOrder
    ) {
        this.generation = generation;
        this.category = category;
        this.enabled = enabled;
        this.color = color;
        this.displayOrder = displayOrder;
    }
}
