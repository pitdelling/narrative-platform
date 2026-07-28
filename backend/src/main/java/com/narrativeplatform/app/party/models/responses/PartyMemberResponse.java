package com.narrativeplatform.app.party.models.responses;

import com.narrativeplatform.app.party.models.enums.MemberStatusType;
import com.narrativeplatform.app.party.models.enums.PartyRoleType;
import java.util.UUID;

public record PartyMemberResponse(UUID userId, String username, String displayName, PartyRoleType role, MemberStatusType status) {
}
