package com.narrativeplatform.app.auth.models.responses;

import java.util.UUID;

public record AuthResponse(String token, UUID userId, String username, String displayName) {
}
