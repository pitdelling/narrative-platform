package com.narrativeplatform.app.invitation.models.requests;

import jakarta.validation.constraints.NotBlank;

public record AcceptInviteRequest(@NotBlank String token) {
}
