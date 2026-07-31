package com.narrativeplatform.app.invitation.models.responses;

import com.narrativeplatform.app.party.models.enums.PartyRoleType;

import java.util.UUID;

public record InvitePreviewResponse(UUID partyId, String partyName, String invitedBy, PartyRoleType targetRole) {
}
