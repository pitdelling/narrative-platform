package com.narrativeplatform.app.canon.models.entities;

import com.narrativeplatform.app.canon.models.enums.CanonCategoryType;
import com.narrativeplatform.app.canon.models.enums.TagColorType;
import com.narrativeplatform.app.canon.models.responses.TagSettingResponseItem;
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
@Table(name = "party_ai_tag_settings", uniqueConstraints = @UniqueConstraint(columnNames = {"party_id", "category"}))
public class PartyAiTagSettingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "party_id")
    @ToString.Exclude
    private PartyEntity party;

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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PartyAiTagSettingEntity(
            final PartyEntity party,
            final CanonCategoryType category,
            final boolean enabled,
            final TagColorType color,
            final int displayOrder
    ) {
        this.party = party;
        this.category = category;
        this.enabled = enabled;
        this.color = color;
        this.displayOrder = displayOrder;
    }

    public TagSettingResponseItem toResponse() {
        return new TagSettingResponseItem(category, enabled, color, displayOrder);
    }
}
