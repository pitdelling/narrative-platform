package com.narrativeplatform.app.canon.services;

import com.narrativeplatform.app.canon.models.enums.CanonCategoryType;
import com.narrativeplatform.app.canon.models.enums.TagBasisType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CanonMapValidationServiceTest {
    private CanonMapValidationService validationService;
    private Set<CanonCategoryType> enabled;
    private Set<Integer> validPositions;

    @BeforeEach
    void setUp() {
        validationService = new CanonMapValidationService(new ObjectMapper());
        enabled = Set.of(CanonCategoryType.PERSON, CanonCategoryType.PLACE);
        validPositions = Set.of(1, 2, 3);
    }

    @Test
    void acceptsAFullyValidMultiCategoryPayload() {
        final var json = """
                {
                  "PERSON": [
                    {"name": "Lucinda", "summary": "An adventurer.", "visualDescription": "Travel-worn clothes.",
                     "personalityDescription": "Bold.", "visualBasis": "INFERRED", "personalityBasis": "CREATIVE_FILL",
                     "sourceSegmentPositions": [1, 2]}
                  ],
                  "PLACE": [
                    {"name": "Main deck", "summary": "Ship's open deck.", "visualDescription": "Wet planks.",
                     "visualBasis": "EXPLICIT", "sourceSegmentPositions": [1]}
                  ]
                }
                """;

        final var result = validationService.validate(json, enabled, validPositions);

        assertEquals(1, result.get(CanonCategoryType.PERSON).size());
        final var lucinda = result.get(CanonCategoryType.PERSON).getFirst();
        assertEquals(TagBasisType.INFERRED, lucinda.visualBasis());
        assertEquals(TagBasisType.CREATIVE_FILL, lucinda.personalityBasis());
        assertEquals(1, result.get(CanonCategoryType.PLACE).size());
        assertNull(result.get(CanonCategoryType.PLACE).getFirst().personalityDescription());
    }

    @Test
    void missingEnabledCategoryBecomesAnEmptyListRatherThanAnError() {
        final var json = """
                {"PERSON": []}
                """;

        final var result = validationService.validate(json, enabled, validPositions);

        assertTrue(result.get(CanonCategoryType.PERSON).isEmpty());
        assertTrue(result.get(CanonCategoryType.PLACE).isEmpty());
    }

    @Test
    void rejectsUnknownCategoryKey() {
        final var json = """
                {"WEATHER": []}
                """;
        assertThrows(CanonMapValidationException.class, () -> validationService.validate(json, enabled, validPositions));
    }

    @Test
    void rejectsDisabledButPresentCategory() {
        final var json = """
                {"SPELL": [{"name": "x", "summary": "y", "visualDescription": "z", "visualBasis": "EXPLICIT", "sourceSegmentPositions": [1]}]}
                """;
        assertThrows(CanonMapValidationException.class, () -> validationService.validate(json, enabled, validPositions));
    }

    @Test
    void rejectsItemMissingRequiredField() {
        final var json = """
                {"PERSON": [{"name": "Lucinda", "visualDescription": "z", "visualBasis": "EXPLICIT", "sourceSegmentPositions": [1]}]}
                """;
        assertThrows(CanonMapValidationException.class, () -> validationService.validate(json, enabled, validPositions));
    }

    @Test
    void rejectsInvalidVisualBasis() {
        final var json = """
                {"PERSON": [{"name": "Lucinda", "summary": "s", "visualDescription": "z", "visualBasis": "MAYBE", "sourceSegmentPositions": [1]}]}
                """;
        assertThrows(CanonMapValidationException.class, () -> validationService.validate(json, enabled, validPositions));
    }

    @Test
    void rejectsSourcePositionNotInTheValidSet() {
        final var json = """
                {"PERSON": [{"name": "Lucinda", "summary": "s", "visualDescription": "z", "visualBasis": "EXPLICIT", "sourceSegmentPositions": [99]}]}
                """;
        assertThrows(CanonMapValidationException.class, () -> validationService.validate(json, enabled, validPositions));
    }

    @Test
    void rejectsItemWithNoSourcePositions() {
        final var json = """
                {"PERSON": [{"name": "Lucinda", "summary": "s", "visualDescription": "z", "visualBasis": "EXPLICIT", "sourceSegmentPositions": []}]}
                """;
        assertThrows(CanonMapValidationException.class, () -> validationService.validate(json, enabled, validPositions));
    }

    @Test
    void rejectsPersonalityDescriptionWithoutPersonalityBasis() {
        final var json = """
                {"PERSON": [{"name": "Lucinda", "summary": "s", "visualDescription": "z", "visualBasis": "EXPLICIT",
                 "personalityDescription": "Bold.", "sourceSegmentPositions": [1]}]}
                """;
        assertThrows(CanonMapValidationException.class, () -> validationService.validate(json, enabled, validPositions));
    }

    @Test
    void rejectsHtmlLikeContent() {
        final var json = """
                {"PERSON": [{"name": "<b>Lucinda</b>", "summary": "s", "visualDescription": "z", "visualBasis": "EXPLICIT", "sourceSegmentPositions": [1]}]}
                """;
        assertThrows(CanonMapValidationException.class, () -> validationService.validate(json, enabled, validPositions));
    }

    @Test
    void rejectsDuplicateNormalizedNameWithinACategory() {
        final var json = """
                {"PERSON": [
                    {"name": "Lucinda", "summary": "s1", "visualDescription": "z1", "visualBasis": "EXPLICIT", "sourceSegmentPositions": [1]},
                    {"name": "  lucinda ", "summary": "s2", "visualDescription": "z2", "visualBasis": "EXPLICIT", "sourceSegmentPositions": [2]}
                ]}
                """;
        assertThrows(CanonMapValidationException.class, () -> validationService.validate(json, enabled, validPositions));
    }

    @Test
    void rejectsNonObjectJson() {
        assertThrows(CanonMapValidationException.class, () -> validationService.validate("[]", enabled, validPositions));
    }

    @Test
    void rejectsInvalidJson() {
        assertThrows(CanonMapValidationException.class, () -> validationService.validate("not json at all", enabled, validPositions));
    }
}
