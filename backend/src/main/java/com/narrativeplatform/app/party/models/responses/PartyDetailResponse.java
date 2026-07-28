package com.narrativeplatform.app.party.models.responses;

import com.narrativeplatform.app.party.models.enums.PartyRoleType;
import java.util.List;
import java.util.UUID;

public record PartyDetailResponse(UUID id, String name, String slug, String description, String imageUrl, UUID ownerId, PartyRoleType currentUserRole, List<PartyMemberResponse> members) {
}
