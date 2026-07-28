package com.narrativeplatform.app.party.models.requests;

import com.narrativeplatform.app.party.models.enums.PartyRoleType;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(@NotNull PartyRoleType role) {
}
