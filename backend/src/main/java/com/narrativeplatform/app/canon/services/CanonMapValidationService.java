package com.narrativeplatform.app.canon.services;

import com.narrativeplatform.app.canon.models.enums.TagBasisType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Pure parsing/validation for the canon map AI output — no persistence. Rejects the whole
 * generation atomically on the first violation; never returns a partially valid structure.
 */
@Service
@RequiredArgsConstructor
public class CanonMapValidationService {
    private static final int MAX_NAME_LENGTH = 160;
    private static final Pattern HTML_LIKE = Pattern.compile("<[a-zA-Z/]");

    private final ObjectMapper objectMapper;

    public Map<String, List<ValidatedCanonTag>> validate(
            final String rawText,
            final Set<String> validCategoryNames,
            final Set<Integer> validSequenceNumbers
    ) {
        final JsonNode root;
        try {
            root = objectMapper.readTree(rawText);
        } catch (final Exception exception) {
            throw new CanonMapValidationException("AI output was not valid JSON.");
        }
        if (!root.isObject()) {
            throw new CanonMapValidationException("AI output was not a JSON object.");
        }

        final Map<String, List<ValidatedCanonTag>> result = new LinkedHashMap<>();
        final var fieldNames = root.propertyNames();
        for (final var fieldName : fieldNames) {
            final var normalizedCategory = normalize(fieldName);
            if (!validCategoryNames.contains(normalizedCategory)) {
                throw new CanonMapValidationException("AI output contained an unknown category: " + fieldName + ".");
            }
            final var value = root.path(fieldName);
            if (!value.isArray()) {
                throw new CanonMapValidationException("Category " + fieldName + " must map to an array.");
            }
            result.put(normalizedCategory, validateItems(fieldName, value, validSequenceNumbers));
        }

        for (final var category : validCategoryNames) {
            result.putIfAbsent(category, List.of());
        }
        return result;
    }

    private List<ValidatedCanonTag> validateItems(
            final String category,
            final JsonNode items,
            final Set<Integer> validSequenceNumbers
    ) {
        final var validated = new ArrayList<ValidatedCanonTag>();
        final var seenNormalizedNames = new HashSet<String>();
        for (final var item : items) {
            final var name = textOrNull(item, "name");
            if (name == null || name.isBlank() || name.length() > MAX_NAME_LENGTH) {
                throw new CanonMapValidationException("An item in category " + category + " is missing a valid name.");
            }
            final var summary = textOrNull(item, "summary");
            if (summary == null || summary.isBlank()) {
                throw new CanonMapValidationException("Item \"" + name + "\" is missing a summary.");
            }
            final var visualDescription = textOrNull(item, "visualDescription");
            if (visualDescription == null || visualDescription.isBlank()) {
                throw new CanonMapValidationException("Item \"" + name + "\" is missing a visual description.");
            }
            final var visualBasis = basisOrNull(item, "visualBasis");
            if (visualBasis == null) {
                throw new CanonMapValidationException("Item \"" + name + "\" has an invalid or missing visualBasis.");
            }
            final var personalityDescription = textOrNull(item, "personalityDescription");
            final var personalityBasis = basisOrNull(item, "personalityBasis");
            final var hasPersonalityDescription = personalityDescription != null && !personalityDescription.isBlank();
            if (hasPersonalityDescription != (personalityBasis != null)) {
                throw new CanonMapValidationException("Item \"" + name + "\" has an inconsistent personality description/basis pair.");
            }
            if (containsMarkup(name) || containsMarkup(summary) || containsMarkup(visualDescription)
                    || (personalityDescription != null && containsMarkup(personalityDescription))) {
                throw new CanonMapValidationException("Item \"" + name + "\" contains HTML or markup, which is not allowed.");
            }
            final var positions = positionsOrNull(item, "sourceSegmentPositions");
            if (positions == null || positions.isEmpty()) {
                throw new CanonMapValidationException("Item \"" + name + "\" has no source segment positions.");
            }
            for (final var position : positions) {
                if (!validSequenceNumbers.contains(position)) {
                    throw new CanonMapValidationException("Item \"" + name + "\" references a segment position that is not part of this story.");
                }
            }
            final var normalizedName = normalize(name);
            if (!seenNormalizedNames.add(normalizedName)) {
                throw new CanonMapValidationException("Category " + category + " contains a duplicate item: \"" + name + "\".");
            }
            validated.add(new ValidatedCanonTag(
                    name, normalizedName, summary, visualDescription,
                    hasPersonalityDescription ? personalityDescription : null,
                    visualBasis, hasPersonalityDescription ? personalityBasis : null,
                    positions
            ));
        }
        return validated;
    }

    private String textOrNull(final JsonNode item, final String field) {
        final var node = item.path(field);
        return node.isString() ? node.asString() : null;
    }

    private TagBasisType basisOrNull(final JsonNode item, final String field) {
        final var node = item.path(field);
        if (!node.isString()) {
            return null;
        }
        try {
            return TagBasisType.valueOf(node.asString());
        } catch (final IllegalArgumentException exception) {
            return null;
        }
    }

    private List<Integer> positionsOrNull(final JsonNode item, final String field) {
        final var node = item.path(field);
        if (!node.isArray() || node.isEmpty()) {
            return null;
        }
        final var positions = new ArrayList<Integer>();
        for (final var position : node) {
            if (!position.isNumber()) {
                return null;
            }
            positions.add(position.asInt());
        }
        return positions;
    }

    private boolean containsMarkup(final String value) {
        return HTML_LIKE.matcher(value).find();
    }

    public static String normalize(final String name) {
        return name.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    public record ValidatedCanonTag(
            String name,
            String normalizedName,
            String summary,
            String visualDescription,
            String personalityDescription,
            TagBasisType visualBasis,
            TagBasisType personalityBasis,
            List<Integer> sourceSegmentPositions
    ) {
    }
}
