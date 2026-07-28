package com.narrativeplatform.app.invitation.models.responses;

import java.time.Instant;
import java.util.UUID;

public record InvitePreviewResponse(UUID partyId, String partyName, String invitedBy, Instant expiresAt) {
}
