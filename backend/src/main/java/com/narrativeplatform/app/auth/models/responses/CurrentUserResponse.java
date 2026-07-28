package com.narrativeplatform.app.auth.models.responses;

import java.util.UUID;

public record CurrentUserResponse(UUID id, String username, String displayName) {
}
