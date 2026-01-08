package ai.qa.solutions.allure.methodology;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("MethodologyLoader")
class MethodologyLoaderTest {

    @Nested
    @DisplayName("toFileName")
    class ToFileName {

        private final MethodologyLoader loader = new MethodologyLoader();

        @Test
        @DisplayName("should convert simple metric name to lowercase")
        void shouldConvertSimpleNameToLowercase() {
            assertThat(loader.toFileName("Faithfulness")).isEqualTo("faithfulness");
        }

        @Test
        @DisplayName("should convert CamelCase to kebab-case")
        void shouldConvertCamelCaseToKebabCase() {
            assertThat(loader.toFileName("ContextPrecision")).isEqualTo("context-precision");
        }

        @Test
        @DisplayName("should handle multiple capital letters")
        void shouldHandleMultipleCapitalLetters() {
            assertThat(loader.toFileName("ResponseRelevancy")).isEqualTo("response-relevancy");
        }

        @Test
        @DisplayName("should strip Metric suffix")
        void shouldStripMetricSuffix() {
            assertThat(loader.toFileName("FaithfulnessMetric")).isEqualTo("faithfulness");
            assertThat(loader.toFileName("ContextPrecisionMetric")).isEqualTo("context-precision");
        }

        @Test
        @DisplayName("should handle null input")
        void shouldHandleNullInput() {
            assertThat(loader.toFileName(null)).isEqualTo("unknown");
        }

        @Test
        @DisplayName("should handle blank input")
        void shouldHandleBlankInput() {
            assertThat(loader.toFileName("  ")).isEqualTo("unknown");
        }
    }

    @Nested
    @DisplayName("loadMethodologyHtml")
    class LoadMethodologyHtml {

        @Test
        @DisplayName("should load existing methodology as HTML")
        void shouldLoadExistingMethodology() {
            final MethodologyLoader loader = new MethodologyLoader("en");

            final String html = loader.loadMethodologyHtml("Faithfulness");

            assertThat(html).isNotEmpty();
            assertThat(html).contains("<h1>");
            assertThat(html).contains("Faithfulness");
        }

        @Test
        @DisplayName("should return fallback for non-existent methodology")
        void shouldReturnFallbackForNonExistent() {
            final MethodologyLoader loader = new MethodologyLoader("en");

            final String html = loader.loadMethodologyHtml("NonExistentMetric");

            assertThat(html).contains("NonExistentMetric");
            assertThat(html).contains("not available");
        }

        @Test
        @DisplayName("should cache loaded methodologies")
        void shouldCacheLoadedMethodologies() {
            final MethodologyLoader loader = new MethodologyLoader("en");

            final String html1 = loader.loadMethodologyHtml("Faithfulness");
            final String html2 = loader.loadMethodologyHtml("Faithfulness");

            assertThat(html1).isSameAs(html2);
        }
    }

    @Nested
    @DisplayName("loadMethodologyMarkdown")
    class LoadMethodologyMarkdown {

        @Test
        @DisplayName("should load existing methodology as Markdown")
        void shouldLoadExistingMethodology() {
            final MethodologyLoader loader = new MethodologyLoader("en");

            final String markdown = loader.loadMethodologyMarkdown("ContextPrecision");

            assertThat(markdown).isNotEmpty();
            assertThat(markdown).contains("# Context Precision");
            assertThat(markdown).contains("## Description");
        }

        @Test
        @DisplayName("should load Russian methodology when available")
        void shouldLoadRussianMethodology() {
            final MethodologyLoader loader = new MethodologyLoader("ru");

            final String markdown = loader.loadMethodologyMarkdown("Faithfulness");

            assertThat(markdown).isNotEmpty();
            assertThat(markdown).contains("Достоверность");
        }
    }

    @Nested
    @DisplayName("getLanguage")
    class GetLanguage {

        @Test
        @DisplayName("should return configured language")
        void shouldReturnConfiguredLanguage() {
            final MethodologyLoader loader = new MethodologyLoader("ru");

            assertThat(loader.getLanguage()).isEqualTo("ru");
        }

        @Test
        @DisplayName("should return default language when null passed")
        void shouldReturnDefaultWhenNull() {
            final MethodologyLoader loader = new MethodologyLoader(null);

            assertThat(loader.getLanguage()).isEqualTo("en");
        }
    }

    @Nested
    @DisplayName("clearCache")
    class ClearCache {

        @Test
        @DisplayName("should clear cached methodologies")
        void shouldClearCache() {
            final MethodologyLoader loader = new MethodologyLoader("en");

            final String html1 = loader.loadMethodologyHtml("Faithfulness");
            loader.clearCache();
            final String html2 = loader.loadMethodologyHtml("Faithfulness");

            // After clear, should load again (not same instance)
            assertThat(html1).isEqualTo(html2);
            assertThat(html1).isNotSameAs(html2);
        }
    }
}
