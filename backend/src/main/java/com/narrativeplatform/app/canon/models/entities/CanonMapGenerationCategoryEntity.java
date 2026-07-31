package com.narrativeplatform.app.canon.models.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Snapshot of one {@link CanonCategoryEntity} row at the moment a {@link CanonMapGenerationEntity}
 * was created. Copied by value and never re-read from the live party categories, so a later change
 * to the party's category configuration never retroactively alters an already-generated canon map.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Table(name = "canon_map_generation_categories")
public class CanonMapGenerationCategoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generation_id")
    @ToString.Exclude
    private CanonMapGenerationEntity generation;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 7)
    private String color;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public CanonMapGenerationCategoryEntity(
            final CanonMapGenerationEntity generation,
            final String name,
            final String description,
            final String color,
            final int displayOrder
    ) {
        this.generation = generation;
        this.name = name;
        this.description = description;
        this.color = color;
        this.displayOrder = displayOrder;
    }
}
