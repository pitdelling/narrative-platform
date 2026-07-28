package com.narrativeplatform.app.auth.models.requests;

import com.narrativeplatform.shared.constants.DomainConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterNarratorRequest(
        @NotBlank @Size(max = DomainConstants.MAX_USERNAME_LENGTH)
        @Pattern(regexp = DomainConstants.USERNAME_PATTERN, message = "Use only letters, numbers, underscores and hyphens.")
        String username,
        @NotBlank @Size(max = 80) String displayName,
        @NotBlank @Size(min = DomainConstants.MIN_PASSWORD_LENGTH, max = 200) String password
) {
}
