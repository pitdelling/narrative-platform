package com.narrativeplatform.app.canon.services;

import com.narrativeplatform.app.canon.models.enums.TagBasisType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CanonMapValidationServiceTest {
    private CanonMapValidationService validationService;
    private Set<String> validCategoryNames;
    private Set<Integer> validPositions;

    @BeforeEach
    void setUp() {
        validationService = new CanonMapValidationService(new ObjectMapper());
        validCategoryNames = Set.of("pessoas", "lugares");
        validPositions = Set.of(1, 2, 3);
    }

    @Test
    void acceptsAFullyValidMultiCategoryPayload() {
        final var json = """
                {
                  "Pessoas": [
                    {"name": "Lucinda", "summary": "An adventurer.", "visualDescription": "Travel-worn clothes.",
                     "personalityDescription": "Bold.", "visualBasis": "INFERRED", "personalityBasis": "CREATIVE_FILL",
                     "sourceSegmentPositions": [1, 2]}
                  ],
                  "Lugares": [
                    {"name": "Main deck", "summary": "Ship's open deck.", "visualDescription": "Wet planks.",
                     "visualBasis": "EXPLICIT", "sourceSegmentPositions": [1]}
                  ]
                }
                """;

        final var result = validationService.validate(json, validCategoryNames, validPositions);

        assertEquals(1, result.get("pessoas").size());
        final var lucinda = result.get("pessoas").getFirst();
        assertEquals(TagBasisType.INFERRED, lucinda.visualBasis());
        assertEquals(TagBasisType.CREATIVE_FILL, lucinda.personalityBasis());
        assertEquals(1, result.get("lugares").size());
        assertNull(result.get("lugares").getFirst().personalityDescription());
    }

    @Test
    void missingConfiguredCategoryBecomesAnEmptyListRatherThanAnError() {
        final var json = """
                {"Pessoas": []}
                """;

        final var result = validationService.validate(json, validCategoryNames, validPositions);

        assertTrue(result.get("pessoas").isEmpty());
        assertTrue(result.get("lugares").isEmpty());
    }

    @Test
    void matchesCategoryNamesCaseAndWhitespaceInsensitively() {
        final var json = """
                {"  PESSOAS  ": [{"name": "Lucinda", "summary": "s", "visualDescription": "z", "visualBasis": "EXPLICIT", "sourceSegmentPositions": [1]}]}
                """;

        final var result = validationService.validate(json, validCategoryNames, validPositions);

        assertEquals(1, result.get("pessoas").size());
    }

    @Test
    void rejectsUnknownCategoryKey() {
        final var json = """
                {"Clima": []}
                """;
        assertThrows(CanonMapValidationException.class, () -> validationService.validate(json, validCategoryNames, validPositions));
    }

    @Test
    void rejectsCategoryNotConfiguredForThisParty() {
        final var json = """
                {"Magias": [{"name": "x", "summary": "y", "visualDescription": "z", "visualBasis": "EXPLICIT", "sourceSegmentPositions": [1]}]}
                """;
        assertThrows(CanonMapValidationException.class, () -> validationService.validate(json, validCategoryNames, validPositions));
    }

    @Test
    void rejectsItemMissingRequiredField() {
        final var json = """
                {"Pessoas": [{"name": "Lucinda", "visualDescription": "z", "visualBasis": "EXPLICIT", "sourceSegmentPositions": [1]}]}
                """;
        assertThrows(CanonMapValidationException.class, () -> validationService.validate(json, validCategoryNames, validPositions));
    }

    @Test
    void rejectsInvalidVisualBasis() {
        final var json = """
                {"Pessoas": [{"name": "Lucinda", "summary": "s", "visualDescription": "z", "visualBasis": "MAYBE", "sourceSegmentPositions": [1]}]}
                """;
        assertThrows(CanonMapValidationException.class, () -> validationService.validate(json, validCategoryNames, validPositions));
    }

    @Test
    void rejectsSourcePositionNotInTheValidSet() {
        final var json = """
                {"Pessoas": [{"name": "Lucinda", "summary": "s", "visualDescription": "z", "visualBasis": "EXPLICIT", "sourceSegmentPositions": [99]}]}
                """;
        assertThrows(CanonMapValidationException.class, () -> validationService.validate(json, validCategoryNames, validPositions));
    }

    @Test
    void rejectsItemWithNoSourcePositions() {
        final var json = """
                {"Pessoas": [{"name": "Lucinda", "summary": "s", "visualDescription": "z", "visualBasis": "EXPLICIT", "sourceSegmentPositions": []}]}
                """;
        assertThrows(CanonMapValidationException.class, () -> validationService.validate(json, validCategoryNames, validPositions));
    }

    @Test
    void rejectsPersonalityDescriptionWithoutPersonalityBasis() {
        final var json = """
                {"Pessoas": [{"name": "Lucinda", "summary": "s", "visualDescription": "z", "visualBasis": "EXPLICIT",
                 "personalityDescription": "Bold.", "sourceSegmentPositions": [1]}]}
                """;
        assertThrows(CanonMapValidationException.class, () -> validationService.validate(json, validCategoryNames, validPositions));
    }

    @Test
    void rejectsHtmlLikeContent() {
        final var json = """
                {"Pessoas": [{"name": "<b>Lucinda</b>", "summary": "s", "visualDescription": "z", "visualBasis": "EXPLICIT", "sourceSegmentPositions": [1]}]}
                """;
        assertThrows(CanonMapValidationException.class, () -> validationService.validate(json, validCategoryNames, validPositions));
    }

    @Test
    void rejectsDuplicateNormalizedNameWithinACategory() {
        final var json = """
                {"Pessoas": [
                    {"name": "Lucinda", "summary": "s1", "visualDescription": "z1", "visualBasis": "EXPLICIT", "sourceSegmentPositions": [1]},
                    {"name": "  lucinda ", "summary": "s2", "visualDescription": "z2", "visualBasis": "EXPLICIT", "sourceSegmentPositions": [2]}
                ]}
                """;
        assertThrows(CanonMapValidationException.class, () -> validationService.validate(json, validCategoryNames, validPositions));
    }

    @Test
    void rejectsNonObjectJson() {
        assertThrows(CanonMapValidationException.class, () -> validationService.validate("[]", validCategoryNames, validPositions));
    }

    @Test
    void rejectsInvalidJson() {
        assertThrows(CanonMapValidationException.class, () -> validationService.validate("not json at all", validCategoryNames, validPositions));
    }
}
