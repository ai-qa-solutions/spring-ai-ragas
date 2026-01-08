package ai.qa.solutions.allure.methodology;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 * Loader for metric methodology documentation.
 * <p>
 * Loads methodology descriptions from classpath resources in Markdown format
 * and converts them to HTML for display in reports.
 * <p>
 * Methodology files are located at:
 * {@code /ai/qa/solutions/allure/methodology/{language}/{metric-name}.md}
 */
@Slf4j
public class MethodologyLoader {

    private static final String BASE_PATH = "/ai/qa/solutions/allure/methodology/";
    private static final String DEFAULT_LANGUAGE = "en";

    private final String language;
    private final Map<String, String> htmlCache = new ConcurrentHashMap<>();
    private final Map<String, String> markdownCache = new ConcurrentHashMap<>();
    private final Parser markdownParser;
    private final HtmlRenderer htmlRenderer;

    /**
     * Creates a MethodologyLoader with the specified language.
     *
     * @param language the language code ("en" or "ru")
     */
    public MethodologyLoader(final String language) {
        this.language = language != null ? language : DEFAULT_LANGUAGE;
        final List<Extension> extensions = List.of(TablesExtension.create());
        this.markdownParser = Parser.builder().extensions(extensions).build();
        this.htmlRenderer = HtmlRenderer.builder().extensions(extensions).build();
    }

    /**
     * Creates a MethodologyLoader with default language (English).
     */
    public MethodologyLoader() {
        this(DEFAULT_LANGUAGE);
    }

    /**
     * Loads methodology as HTML for the given metric.
     *
     * @param metricName the metric name (e.g., "Faithfulness", "ContextPrecision")
     * @return methodology HTML content or fallback message
     */
    public String loadMethodologyHtml(final String metricName) {
        final String cacheKey = language + "/" + metricName + "/html";
        return htmlCache.computeIfAbsent(cacheKey, k -> convertToHtml(loadMarkdown(metricName)));
    }

    /**
     * Loads methodology as Markdown for the given metric.
     *
     * @param metricName the metric name (e.g., "Faithfulness", "ContextPrecision")
     * @return methodology Markdown content or fallback message
     */
    public String loadMethodologyMarkdown(final String metricName) {
        final String cacheKey = language + "/" + metricName + "/md";
        return markdownCache.computeIfAbsent(cacheKey, k -> loadMarkdown(metricName));
    }

    private String loadMarkdown(final String metricName) {
        final String fileName = toFileName(metricName);
        final String path = BASE_PATH + language + "/" + fileName + ".md";

        try (final InputStream is = getClass().getResourceAsStream(path)) {
            if (is != null) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (final IOException e) {
            log.warn("Failed to load methodology for metric '{}' at path '{}': {}", metricName, path, e.getMessage());
        }

        // Try fallback to English
        if (!DEFAULT_LANGUAGE.equals(language)) {
            final String fallbackPath = BASE_PATH + DEFAULT_LANGUAGE + "/" + fileName + ".md";
            try (final InputStream is = getClass().getResourceAsStream(fallbackPath)) {
                if (is != null) {
                    log.debug("Using English fallback for metric '{}' (requested language: {})", metricName, language);
                    return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            } catch (final IOException e) {
                log.warn("Failed to load fallback methodology for metric '{}': {}", metricName, e.getMessage());
            }
        }

        return generateFallbackMethodology(metricName);
    }

    private String convertToHtml(final String markdown) {
        final Node document = markdownParser.parse(markdown);
        return htmlRenderer.render(document);
    }

    /**
     * Converts metric name to file name.
     * <p>
     * Examples:
     * <ul>
     *   <li>"Faithfulness" -&gt; "faithfulness"</li>
     *   <li>"ContextPrecision" -&gt; "context-precision"</li>
     *   <li>"ResponseRelevancy" -&gt; "response-relevancy"</li>
     * </ul>
     *
     * @param metricName the metric name
     * @return normalized file name
     */
    String toFileName(final String metricName) {
        if (metricName == null || metricName.isBlank()) {
            return "unknown";
        }

        // Handle "Metric" suffix
        String name = metricName;
        if (name.endsWith("Metric")) {
            name = name.substring(0, name.length() - 6);
        }

        // Convert CamelCase to kebab-case
        return name.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }

    private String generateFallbackMethodology(final String metricName) {
        return String.format(
                """
            # %s

            ## Description

            Methodology documentation is not available for this metric.

            ## Score Interpretation

            | Range | Interpretation |
            |-------|----------------|
            | 0.8 - 1.0 | Excellent |
            | 0.6 - 0.8 | Good |
            | 0.4 - 0.6 | Moderate |
            | 0.0 - 0.4 | Poor |
            """,
                metricName);
    }

    /**
     * Gets the configured language.
     *
     * @return the language code
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Clears the cache (useful for testing).
     */
    public void clearCache() {
        htmlCache.clear();
        markdownCache.clear();
    }
}
