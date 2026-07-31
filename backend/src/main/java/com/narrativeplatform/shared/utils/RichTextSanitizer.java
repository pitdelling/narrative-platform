package com.narrativeplatform.shared.utils;

import lombok.experimental.UtilityClass;
import org.jsoup.parser.Parser;
import org.owasp.html.AttributePolicy;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import java.util.ArrayList;
import java.util.regex.Pattern;

@UtilityClass
public class RichTextSanitizer {
    // Browsers re-serialize an inline `style` attribute through their own CSSOM when a rich-text
    // editor round-trips content through the DOM, which canonicalizes hex colors (what the
    // <input type="color"> picker always emits) into "rgb(r, g, b)" form. Both forms must be
    // accepted or every color set through the editor gets silently dropped on save.
    private static final String COLOR_VALUE = "(#[0-9a-fA-F]{6}|rgb\\(\\s*\\d{1,3}\\s*,\\s*\\d{1,3}\\s*,\\s*\\d{1,3}\\s*\\))";
    private static final Pattern[] ALLOWED_STYLE_DECLARATIONS = {
            Pattern.compile("color:\\s*" + COLOR_VALUE),
            Pattern.compile("background-color:\\s*" + COLOR_VALUE),
            Pattern.compile("margin-left:\\s*\\d+(\\.\\d+)?rem"),
    };
    private static final Pattern BLOCK_BREAK = Pattern.compile("(?i)</p>\\s*|<br\\s*/?>\\s*|</li>\\s*");
    private static final Pattern ANY_TAG = Pattern.compile("<[^>]+>");

    // Splits the style attribute into individual declarations and keeps only the ones
    // matching the allow-list above, instead of requiring the whole attribute value to
    // match a single pattern. A full-value match is brittle: any extra/reordered
    // declaration (e.g. combining color with the new background-color, or the
    // "undefined;color: ..." garbage the editor can produce when re-parsing a nested
    // colored span) would otherwise drop the entire attribute instead of just the
    // offending declaration.
    private static final AttributePolicy STYLE_POLICY = (elementName, attributeName, value) -> {
        final var kept = new ArrayList<String>();
        for (final var rawDeclaration : value.split(";")) {
            final var declaration = rawDeclaration.strip();
            if (!declaration.isEmpty() && isAllowedDeclaration(declaration)) {
                kept.add(declaration);
            }
        }
        return kept.isEmpty() ? null : String.join("; ", kept);
    };

    private static boolean isAllowedDeclaration(final String declaration) {
        for (final var pattern : ALLOWED_STYLE_DECLARATIONS) {
            if (pattern.matcher(declaration).matches()) return true;
        }
        return false;
    }

    private static final PolicyFactory POLICY = new HtmlPolicyBuilder()
            .allowElements("b", "strong", "i", "em", "u", "s", "ul", "ol", "li", "p", "br", "span")
            .allowAttributes("style").matching(STYLE_POLICY).onElements("span", "p")
            .allowAttributes("data-kind").matching(Pattern.compile("decimal|alpha")).onElements("ol")
            .toFactory();

    public String sanitize(final String html) {
        return html == null ? "" : POLICY.sanitize(html);
    }

    public String toPlainText(final String html) {
        if (html == null || html.isBlank()) return "";
        final var withBreaks = BLOCK_BREAK.matcher(html).replaceAll("\n");
        final var stripped = ANY_TAG.matcher(withBreaks).replaceAll("");
        return Parser.unescapeEntities(stripped, false).strip();
    }
}
