package ai.qa.solutions.metric.prompt;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class PromptTemplateResolver {

    private static final String RESOURCE_BASE = "ai/qa/solutions/prompts";

    /**
     * Resolves prompt template with fallback chain:
     * classpath resource by language → classpath resource "en" → hardcoded default.
     */
    public static String resolve(final String metricKey, final String language, final String hardcodedDefault) {
        // Try requested language
        final String resolved = loadFromClasspath(metricKey, language);
        if (resolved != null) {
            return resolved;
        }

        // Fallback to English
        if (!"en".equals(language)) {
            final String english = loadFromClasspath(metricKey, "en");
            if (english != null) {
                log.debug("Prompt for '{}' not found for language '{}', using English", metricKey, language);
                return english;
            }
        }

        // Final fallback to hardcoded default
        return hardcodedDefault;
    }

    private static String loadFromClasspath(final String metricKey, final String language) {
        final String path = RESOURCE_BASE + "/" + language + "/" + metricKey + ".txt";
        try (final InputStream is =
                Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                return null;
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final Exception e) {
            log.warn("Failed to load prompt resource '{}': {}", path, e.getMessage());
            return null;
        }
    }
}
