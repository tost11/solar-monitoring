package de.tostsoft.solarmonitoring.app.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HtmlSanitizerTest {

    private HtmlSanitizer htmlSanitizer;

    @BeforeEach
    void setUp() {
        htmlSanitizer = new HtmlSanitizer();
    }

    @Test
    void shouldAllowBasicFormattingTags() {
        String input = "<b>Bold</b> <i>Italic</i> <u>Underline</u> <strong>Strong</strong> <em>Emphasis</em>";
        String result = htmlSanitizer.sanitizeDescription(input);

        assertThat(result).isEqualTo(input);
    }

    @Test
    void shouldAllowParagraphsAndLineBreaks() {
        String input = "<p>Paragraph 1</p><p>Paragraph 2</p>Line<br>Break";
        String result = htmlSanitizer.sanitizeDescription(input);

        assertThat(result).isEqualTo("<p>Paragraph 1</p><p>Paragraph 2</p>Line<br />Break");
    }

    @Test
    void shouldAllowLists() {
        String input = "<ul><li>Item 1</li><li>Item 2</li></ul><ol><li>First</li><li>Second</li></ol>";
        String result = htmlSanitizer.sanitizeDescription(input);

        assertThat(result).isEqualTo(input);
    }

    @Test
    void shouldAllowHttpAndHttpsLinks() {
        String input = "<a href=\"http://example.com\">HTTP Link</a> <a href=\"https://secure.com\">HTTPS Link</a>";
        String result = htmlSanitizer.sanitizeDescription(input);

        assertThat(result).contains("href=\"http://example.com\"");
        assertThat(result).contains("href=\"https://secure.com\"");
        assertThat(result).contains("rel=\"nofollow\"");
    }

    @Test
    void shouldRemoveScriptTags() {
        String input = "<script>alert('XSS')</script>Hello";
        String result = htmlSanitizer.sanitizeDescription(input);

        assertThat(result).isEqualTo("Hello");
        assertThat(result).doesNotContain("script");
        assertThat(result).doesNotContain("alert");
    }

    @Test
    void shouldRemoveEventHandlers() {
        String input = "<div onclick=\"alert('XSS')\">Click me</div>";
        String result = htmlSanitizer.sanitizeDescription(input);

        assertThat(result).doesNotContain("onclick");
        assertThat(result).doesNotContain("alert");
    }

    @Test
    void shouldRemoveJavascriptProtocol() {
        String input = "<a href=\"javascript:alert('XSS')\">Dangerous Link</a>";
        String result = htmlSanitizer.sanitizeDescription(input);

        assertThat(result).doesNotContain("javascript:");
        assertThat(result).doesNotContain("alert");
    }

    @Test
    void shouldRemoveIframes() {
        String input = "<iframe src=\"http://evil.com\"></iframe>Safe content";
        String result = htmlSanitizer.sanitizeDescription(input);

        assertThat(result).isEqualTo("Safe content");
        assertThat(result).doesNotContain("iframe");
    }

    @Test
    void shouldRemoveStyleTags() {
        String input = "<style>body { background: red; }</style>Content";
        String result = htmlSanitizer.sanitizeDescription(input);

        assertThat(result).isEqualTo("Content");
        assertThat(result).doesNotContain("style");
    }

    @Test
    void shouldTrimWhitespace() {
        String input = "  <p>Content</p>  ";
        String result = htmlSanitizer.sanitizeDescription(input);

        assertThat(result).isEqualTo("<p>Content</p>");
    }

    @Test
    void shouldReturnNullForNullInput() {
        String result = htmlSanitizer.sanitizeDescription(null);

        assertThat(result).isNull();
    }

    @Test
    void shouldReturnEmptyStringForEmptyInput() {
        String result = htmlSanitizer.sanitizeDescription("");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldThrowExceptionForTooLongDescription() {
        String longDescription = "a".repeat(301);

        assertThatThrownBy(() -> htmlSanitizer.sanitizeDescription(longDescription))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Description exceeds maximum length of 300 characters");
    }

    @Test
    void shouldAllowDescriptionAtMaxLength() {
        String maxLengthDescription = "a".repeat(300);
        String result = htmlSanitizer.sanitizeDescription(maxLengthDescription);

        assertThat(result).hasSize(300);
    }

    @Test
    void shouldHandleMixedSafeAndUnsafeContent() {
        String input = "<b>Bold text</b><script>alert('XSS')</script><i>Italic</i>";
        String result = htmlSanitizer.sanitizeDescription(input);

        assertThat(result).contains("<b>Bold text</b>");
        assertThat(result).contains("<i>Italic</i>");
        assertThat(result).doesNotContain("script");
        assertThat(result).doesNotContain("alert");
    }

    @Test
    void shouldRemoveDataProtocolInLinks() {
        String input = "<a href=\"data:text/html,<script>alert('XSS')</script>\">Link</a>";
        String result = htmlSanitizer.sanitizeDescription(input);

        assertThat(result).doesNotContain("data:");
        assertThat(result).doesNotContain("script");
    }

    @Test
    void shouldRemoveEmbedAndObjectTags() {
        String input = "<embed src=\"evil.swf\"><object data=\"evil.swf\"></object>Safe";
        String result = htmlSanitizer.sanitizeDescription(input);

        assertThat(result).isEqualTo("Safe");
        assertThat(result).doesNotContain("embed");
        assertThat(result).doesNotContain("object");
    }
}
