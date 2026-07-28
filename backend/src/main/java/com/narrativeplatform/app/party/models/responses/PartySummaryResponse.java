package com.narrativeplatform.app.party.models.responses;

import com.narrativeplatform.app.party.models.enums.PartyRoleType;
import java.util.UUID;

public record PartySummaryResponse(UUID id, String name, String slug, String description, String imageUrl, PartyRoleType role) {
}
