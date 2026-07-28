package com.narrativeplatform.app.party.models.entities;

import com.narrativeplatform.app.party.models.enums.PartyRoleType;
import com.narrativeplatform.app.party.models.responses.PartyDetailResponse;
import com.narrativeplatform.app.party.models.responses.PartyMemberResponse;

import java.util.List;

import com.narrativeplatform.app.auth.models.entities.UserEntity;
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
@Table(name = "parties")
public class PartyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 140)
    private String slug;

    @Column(length = 1000)
    private String description;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    @ToString.Exclude
    private UserEntity owner;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PartyDetailResponse toDetailResponse(final PartyRoleType currentUserRole, final List<PartyMemberResponse> members) {
        return new PartyDetailResponse(id, name, slug, description, imageUrl, owner.getId(), currentUserRole, members);
    }

    public PartyEntity(
            final String name,
            final String slug,
            final String description,
            final String imageUrl,
            final UserEntity owner
    ) {
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.imageUrl = imageUrl;
        this.owner = owner;
    }
}
