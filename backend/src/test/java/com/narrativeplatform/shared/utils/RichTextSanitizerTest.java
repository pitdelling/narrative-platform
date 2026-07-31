package com.narrativeplatform.shared.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RichTextSanitizerTest {

    @Test
    void sanitizeKeepsEveryAllowedTag() {
        final var input = "<p>Hello <b>bold</b> <i>italic</i> <u>underline</u></p><ul><li>one</li></ul><ol><li>two</li></ol>";
        final var sanitized = RichTextSanitizer.sanitize(input);
        for (final var tag : new String[]{"<p>", "<b>", "<i>", "<u>", "<ul>", "<li>", "<ol>"}) {
            assertTrue(sanitized.contains(tag), "expected " + tag + " to survive sanitization");
        }
        assertTrue(sanitized.contains("bold") && sanitized.contains("italic") && sanitized.contains("underline"));
    }

    @Test
    void sanitizeStripsScriptAndDisallowedAttributes() {
        final var sanitized = RichTextSanitizer.sanitize("<p onclick=\"evil()\">Safe</p><script>alert(1)</script><img src=x onerror=alert(1)>");
        assertFalse(sanitized.toLowerCase().contains("script"));
        assertFalse(sanitized.contains("onclick"));
        assertFalse(sanitized.contains("<img"));
        assertTrue(sanitized.contains("Safe"));
    }

    @Test
    void sanitizeKeepsAColorOnlyStyleOnSpan() {
        final var sanitized = RichTextSanitizer.sanitize("<span style=\"color:#ff0000\">red</span>");
        assertTrue(sanitized.contains("color:#ff0000"));
        assertTrue(sanitized.contains("red"));
    }

    @Test
    void sanitizeStripsNonColorStyleDeclarationsFromSpan() {
        final var sanitized = RichTextSanitizer.sanitize("<span style=\"position:fixed;top:0\">moved</span>");
        assertFalse(sanitized.contains("position"));
    }

    @Test
    void sanitizeStripsInvalidDeclarationsButKeepsColorWhenBothArePresent() {
        final var sanitized = RichTextSanitizer.sanitize("<span style=\"color:#ffffff;position:fixed\">text</span>");
        assertFalse(sanitized.contains("position"));
        assertTrue(sanitized.contains("color:#ffffff"));
    }

    @Test
    void sanitizeKeepsColorAndBackgroundColorTogetherOnTheSameSpan() {
        final var sanitized = RichTextSanitizer.sanitize("<span style=\"color: #ff0000; background-color: #00ff00\">text</span>");
        assertTrue(sanitized.contains("color: #ff0000"));
        assertTrue(sanitized.contains("background-color: #00ff00"));
    }

    @Test
    void sanitizeKeepsALoneBackgroundColorOnSpan() {
        final var sanitized = RichTextSanitizer.sanitize("<span style=\"background-color: #123456\">text</span>");
        assertTrue(sanitized.contains("background-color: #123456"));
    }

    @Test
    void sanitizeKeepsColorWhenPrecededByGarbageFromNestedSpanMerging() {
        final var sanitized = RichTextSanitizer.sanitize("<span style=\"undefined;color: #ff0000\">text</span>");
        assertTrue(sanitized.contains("color: #ff0000"));
        assertFalse(sanitized.contains("undefined"));
    }

    @Test
    void sanitizeKeepsColorAndBackgroundColorInRgbFormAsProducedByBrowserStyleSerialization() {
        final var sanitized = RichTextSanitizer.sanitize(
                "<p><span style=\"color: rgb(255, 0, 0);\">yukut</span>,uy, <span style=\"background-color: rgb(255, 0, 0);\">ytjyru </span>r </p>");
        assertTrue(sanitized.contains("color: rgb(255, 0, 0)"));
        assertTrue(sanitized.contains("background-color: rgb(255, 0, 0)"));
        assertTrue(sanitized.contains("yukut"));
        assertTrue(sanitized.contains("ytjyru"));
    }

    @Test
    void sanitizeKeepsMarginLeftOnAParagraph() {
        final var sanitized = RichTextSanitizer.sanitize("<p style=\"margin-left: 4rem\">indented</p>");
        assertTrue(sanitized.contains("margin-left: 4rem"));
    }

    @Test
    void sanitizeStripsOutOfPatternValuesWithoutDroppingAValidCoLocatedDeclaration() {
        final var colorSanitized = RichTextSanitizer.sanitize("<span style=\"color: red; background-color: #00ff00\">text</span>");
        assertFalse(colorSanitized.contains("color: red"));
        assertTrue(colorSanitized.contains("background-color: #00ff00"));

        final var marginSanitized = RichTextSanitizer.sanitize("<p style=\"margin-left: 999px; color: #ff0000\">text</p>");
        assertFalse(marginSanitized.contains("999px"));
    }

    @Test
    void sanitizeKeepsTheStrikethroughTag() {
        final var sanitized = RichTextSanitizer.sanitize("<s>gone</s>");
        assertTrue(sanitized.contains("<s>"));
        assertTrue(sanitized.contains("gone"));
    }

    @Test
    void sanitizeKeepsSmartListDataKindOnOrderedList() {
        final var sanitized = RichTextSanitizer.sanitize("<ol data-kind=\"alpha\"><li>a</li></ol>");
        assertTrue(sanitized.contains("data-kind=\"alpha\""));
    }

    @Test
    void sanitizeStripsAnUnrecognizedDataKindValue() {
        final var sanitized = RichTextSanitizer.sanitize("<ol data-kind=\"roman\"><li>a</li></ol>");
        assertFalse(sanitized.contains("data-kind"));
    }

    @Test
    void toPlainTextPreservesLineBreaksAcrossParagraphsListItemsAndBreaks() {
        final var plainText = RichTextSanitizer.toPlainText("<p>a</p><p>b</p><ul><li>c</li><li>d</li></ul>line<br>break");
        assertEquals("a\nb\nc\nd\nline\nbreak", plainText);
    }

    @Test
    void toPlainTextStripsAllTagsAndUnescapesEntities() {
        final var plainText = RichTextSanitizer.toPlainText("<p><b>Bold</b> &amp; <i>italic</i></p>");
        assertEquals("Bold & italic", plainText);
    }

    @Test
    void toPlainTextHandlesNullAndBlankInput() {
        assertEquals("", RichTextSanitizer.toPlainText(null));
        assertEquals("", RichTextSanitizer.toPlainText("   "));
    }

    @Test
    void sanitizeHandlesNullInput() {
        assertEquals("", RichTextSanitizer.sanitize(null));
    }
}
