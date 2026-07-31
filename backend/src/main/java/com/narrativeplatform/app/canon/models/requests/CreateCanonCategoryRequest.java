package com.narrativeplatform.app.canon.models.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCanonCategoryRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 500) String description,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String color
) {
}
