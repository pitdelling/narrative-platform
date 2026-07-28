package com.narrativeplatform.app.invitation.models.responses;

import com.narrativeplatform.app.invitation.models.enums.InviteChannelType;
import java.time.Instant;
import java.util.UUID;

public record InviteResponse(UUID id, UUID partyId, String partyName, InviteChannelType channel, String recipientContact, String inviteUrl, String whatsappUrl, Instant expiresAt, boolean emailSent) {
}
