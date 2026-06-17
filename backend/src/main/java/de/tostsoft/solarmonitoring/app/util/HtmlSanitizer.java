package de.tostsoft.solarmonitoring.app.util;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Component;

@Component
public class HtmlSanitizer {

    private static final int MAX_DESCRIPTION_LENGTH = 300;

    private static final PolicyFactory DESCRIPTION_POLICY = new HtmlPolicyBuilder()
        .allowElements("p", "br", "b", "i", "u", "strong", "em")
        .allowElements("ul", "ol", "li")
        .allowElements("a")
        .allowAttributes("href").onElements("a")
        .allowUrlProtocols("http", "https")
        .requireRelNofollowOnLinks()
        .toFactory();

    public String sanitizeDescription(String description) {
        if (description == null) {
            return null;
        }

        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException(
                "Description exceeds maximum length of " + MAX_DESCRIPTION_LENGTH + " characters"
            );
        }

        String sanitized = DESCRIPTION_POLICY.sanitize(description);

        return sanitized.trim();
    }
}
