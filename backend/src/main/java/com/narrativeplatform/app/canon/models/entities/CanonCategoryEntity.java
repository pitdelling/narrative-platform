package com.narrativeplatform.app.canon.models.entities;

import com.narrativeplatform.app.canon.models.responses.CanonCategoryConfigResponse;
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
@Table(name = "canon_categories")
public class CanonCategoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "party_id")
    @ToString.Exclude
    private PartyEntity party;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 7)
    private String color;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CanonCategoryEntity(
            final PartyEntity party,
            final String name,
            final String description,
            final String color,
            final int displayOrder
    ) {
        this.party = party;
        this.name = name;
        this.description = description;
        this.color = color;
        this.displayOrder = displayOrder;
    }

    public CanonCategoryConfigResponse toResponse() {
        return new CanonCategoryConfigResponse(id, name, description, color, displayOrder);
    }
}
