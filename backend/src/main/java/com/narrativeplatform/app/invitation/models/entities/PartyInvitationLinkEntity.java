package com.narrativeplatform.app.invitation.models.entities;

import com.narrativeplatform.app.invitation.models.responses.InvitePreviewResponse;
import com.narrativeplatform.app.invitation.models.responses.PartyInvitationLinkResponse;

import com.narrativeplatform.app.auth.models.entities.UserEntity;
import com.narrativeplatform.app.party.models.entities.PartyEntity;
import com.narrativeplatform.app.party.models.enums.PartyRoleType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * One reusable, multi-use invitation link per (party, target role). A party has one link that
 * registers new members as {@code PLAYER} and a separate one that registers them directly as
 * {@code SPECTATOR}, enforced by {@code UNIQUE(party_id, target_role)} rather than the single-row-
 * per-party {@code party_id} primary key this table used before spectator invites existed.
 *
 * <p>Security trade-off: unlike the retired one-use invite (hash-only, shown once at creation),
 * this link must be redisplayed on demand to any authorized viewer, so the raw {@code token} is
 * persisted alongside its {@code token_hash} rather than hash-only. This makes {@code token} a
 * bearer secret at rest: anyone with read access to this table can reconstruct a working invite
 * link without going through the hash-comparison path. Mitigations: the public resolve/accept
 * path only ever queries by {@code token_hash}; regeneration invalidates the previous token
 * immediately, capping any leak's exposure window; both fields are excluded from {@code toString()}
 * so no accidental log statement can print them; and resolution failures never echo the token.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Table(name = "party_invitation_links", uniqueConstraints = @UniqueConstraint(columnNames = {"party_id", "target_role"}))
public class PartyInvitationLinkEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "party_id")
    @ToString.Exclude
    private PartyEntity party;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_role", nullable = false, length = 30)
    private PartyRoleType targetRole;

    @Column(nullable = false, unique = true, length = 64)
    @ToString.Exclude
    private String token;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    @ToString.Exclude
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by")
    @ToString.Exclude
    private UserEntity createdBy;

    @Column(name = "rotated_at", nullable = false)
    private Instant rotatedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PartyInvitationLinkEntity(
            final PartyEntity party,
            final PartyRoleType targetRole,
            final String token,
            final String tokenHash,
            final UserEntity createdBy
    ) {
        this.party = party;
        this.targetRole = targetRole;
        this.token = token;
        this.tokenHash = tokenHash;
        this.createdBy = createdBy;
        this.rotatedAt = Instant.now();
    }

    public UUID getPartyId() {
        return party.getId();
    }

    public void rotate(final String token, final String tokenHash, final UserEntity actor) {
        this.token = token;
        this.tokenHash = tokenHash;
        this.createdBy = actor;
        this.rotatedAt = Instant.now();
    }

    public PartyInvitationLinkResponse toResponse(final String inviteUrl) {
        return new PartyInvitationLinkResponse(getPartyId(), inviteUrl);
    }

    public InvitePreviewResponse toPreviewResponse() {
        return new InvitePreviewResponse(getPartyId(), party.getName(), createdBy.getDisplayName(), targetRole);
    }
}
