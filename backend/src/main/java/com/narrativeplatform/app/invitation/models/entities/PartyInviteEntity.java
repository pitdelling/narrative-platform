package com.narrativeplatform.app.invitation.models.entities;

import com.narrativeplatform.app.invitation.models.responses.InvitePreviewResponse;
import com.narrativeplatform.app.invitation.models.responses.InviteResponse;

import com.narrativeplatform.app.auth.models.entities.UserEntity;
import com.narrativeplatform.app.invitation.models.enums.InviteChannelType;
import com.narrativeplatform.app.party.models.entities.PartyEntity;
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
@Table(name = "party_invites")
public class PartyInviteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "party_id")
    @ToString.Exclude
    private PartyEntity party;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by")
    @ToString.Exclude
    private UserEntity createdBy;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    @ToString.Exclude
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InviteChannelType channel;

    @Column(name = "recipient_contact", length = 320)
    private String recipientContact;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consumed_by")
    @ToString.Exclude
    private UserEntity consumedBy;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public InviteResponse toResponse(final String inviteUrl, final String whatsappUrl, final boolean emailSent) {
        return new InviteResponse(id, party.getId(), party.getName(), channel, recipientContact, inviteUrl, whatsappUrl, expiresAt, emailSent);
    }

    public InvitePreviewResponse toPreviewResponse() {
        return new InvitePreviewResponse(party.getId(), party.getName(), createdBy.getDisplayName(), expiresAt);
    }

    public PartyInviteEntity(
            final PartyEntity party,
            final UserEntity createdBy,
            final String tokenHash,
            final InviteChannelType channel,
            final String recipientContact,
            final Instant expiresAt
    ) {
        this.party = party;
        this.createdBy = createdBy;
        this.tokenHash = tokenHash;
        this.channel = channel;
        this.recipientContact = recipientContact;
        this.expiresAt = expiresAt;
    }

    public boolean isAvailable(final Instant now) {
        return consumedAt == null && revokedAt == null && expiresAt.isAfter(now);
    }
}
