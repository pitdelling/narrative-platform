package com.narrativeplatform.app.invitation.models.responses;

import java.util.UUID;

public record PartyInvitationLinkResponse(UUID partyId, String inviteUrl) {
}
