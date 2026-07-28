package com.narrativeplatform.app.invitation.models.requests;

import com.narrativeplatform.app.invitation.models.enums.InviteChannelType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateInviteRequest(
        @NotNull InviteChannelType channel,
        @Size(max = 320) String recipientContact
) {
}
