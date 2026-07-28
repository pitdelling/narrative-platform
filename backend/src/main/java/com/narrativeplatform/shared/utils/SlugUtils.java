package com.narrativeplatform.shared.utils;

import lombok.experimental.UtilityClass;

import java.text.Normalizer;
import java.util.Locale;

@UtilityClass
public class SlugUtils {
    public String slugify(final String input) {
        final var normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? "party" : normalized;
    }
}
