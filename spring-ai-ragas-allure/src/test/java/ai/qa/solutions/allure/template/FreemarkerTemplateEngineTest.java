package ai.qa.solutions.allure.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.qa.solutions.allure.model.ChartData;
import ai.qa.solutions.allure.model.EvaluationReportData;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("FreemarkerTemplateEngine")
class FreemarkerTemplateEngineTest {

    private FreemarkerTemplateEngine engine;

    @BeforeEach
    void setUp() {
        engine = new FreemarkerTemplateEngine();
    }

    @Nested
    @DisplayName("renderHtml")
    class RenderHtml {

        @Test
        @DisplayName("should render HTML report")
        void shouldRenderHtmlReport() {
            final EvaluationReportData data = createTestData();

            final String html = engine.renderHtml(data);

            assertThat(html).isNotEmpty();
            assertThat(html).contains("<!DOCTYPE html>");
            assertThat(html).contains("<html");
            assertThat(html).contains("</html>");
            assertThat(html).contains("Faithfulness");
            assertThat(html).contains("What is AI?");
        }

        @Test
        @DisplayName("should include score information")
        void shouldIncludeScoreInfo() {
            final EvaluationReportData data = createTestData();

            final String html = engine.renderHtml(data);

            assertThat(html).contains("85.00%");
        }

        @Test
        @DisplayName("should include methodology")
        void shouldIncludeMethodology() {
            final EvaluationReportData data = createTestData();

            final String html = engine.renderHtml(data);

            assertThat(html).contains("Test methodology");
        }
    }

    @Nested
    @DisplayName("renderMarkdown")
    class RenderMarkdown {

        @Test
        @DisplayName("should render Markdown report")
        void shouldRenderMarkdownReport() {
            final EvaluationReportData data = createTestData();

            final String markdown = engine.renderMarkdown(data);

            assertThat(markdown).isNotEmpty();
            assertThat(markdown).contains("Faithfulness");
            assertThat(markdown).contains("What is AI?");
        }

        @Test
        @DisplayName("should use markdown formatting")
        void shouldUseMarkdownFormatting() {
            final EvaluationReportData data = createTestData();

            final String markdown = engine.renderMarkdown(data);

            // Should contain markdown headers
            assertThat(markdown).containsPattern("#+\\s+");
        }
    }

    @Nested
    @DisplayName("render with invalid template")
    class RenderWithInvalidTemplate {

        @Test
        @DisplayName("should throw TemplateRenderException for non-existent template")
        void shouldThrowForNonExistentTemplate() {
            final EvaluationReportData data = createTestData();

            assertThatThrownBy(() -> engine.render("non-existent-template.ftl", data))
                    .isInstanceOf(FreemarkerTemplateEngine.TemplateRenderException.class)
                    .hasMessageContaining("Failed to render template");
        }
    }

    private EvaluationReportData createTestData() {
        return EvaluationReportData.builder()
                .metricName("Faithfulness")
                .metricDescription("Measures factual consistency")
                .userInput("What is AI?")
                .response("AI is artificial intelligence")
                .reference("Artificial intelligence is a field of computer science")
                .retrievedContexts(List.of("Context 1", "Context 2"))
                .startTime(Instant.now())
                .endTime(Instant.now().plusSeconds(5))
                .totalDuration(Duration.ofSeconds(5))
                .modelIds(List.of("model-1", "model-2"))
                .embeddingModelIds(List.of())
                .excludedModels(List.of())
                .aggregatedScore(0.85)
                .modelScores(Map.of("model-1", 0.9, "model-2", 0.8))
                .steps(List.of())
                .exclusions(List.of())
                .methodologyHtml("<p>Test methodology</p>")
                .methodologyMarkdown("# Test methodology")
                .configJson("{\"param\": \"value\"}")
                .language("en")
                .chartData(ChartData.builder()
                        .scoreEntries(List.of(
                                ChartData.ScoreEntry.builder()
                                        .modelId("model-1")
                                        .score(0.9)
                                        .excluded(false)
                                        .build(),
                                ChartData.ScoreEntry.builder()
                                        .modelId("model-2")
                                        .score(0.8)
                                        .excluded(false)
                                        .build()))
                        .timelineEntries(List.of())
                        .heatmapRowLabels(List.of())
                        .heatmapColLabels(List.of())
                        .heatmapValues(List.of())
                        .build())
                .build();
    }
}
