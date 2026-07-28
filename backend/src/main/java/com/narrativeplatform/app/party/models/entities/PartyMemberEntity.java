package com.narrativeplatform.app.party.models.entities;

import com.narrativeplatform.app.party.models.responses.PartyMemberResponse;
import com.narrativeplatform.app.party.models.responses.PartySummaryResponse;

import com.narrativeplatform.app.auth.models.entities.UserEntity;
import com.narrativeplatform.app.party.models.enums.MemberStatusType;
import com.narrativeplatform.app.party.models.enums.PartyRoleType;
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
@Table(name = "party_members", uniqueConstraints = @UniqueConstraint(columnNames = {"party_id", "user_id"}))
public class PartyMemberEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "party_id")
    @ToString.Exclude
    private PartyEntity party;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PartyRoleType role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MemberStatusType status;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PartySummaryResponse toSummaryResponse() {
        return new PartySummaryResponse(
                party.getId(), party.getName(), party.getSlug(), party.getDescription(), party.getImageUrl(), role
        );
    }

    public PartyMemberResponse toMemberResponse() {
        return new PartyMemberResponse(user.getId(), user.getUsername(), user.getDisplayName(), role, status);
    }

    public PartyMemberEntity(
            final PartyEntity party,
            final UserEntity user,
            final PartyRoleType role,
            final MemberStatusType status
    ) {
        this.party = party;
        this.user = user;
        this.role = role;
        this.status = status;
    }
}
