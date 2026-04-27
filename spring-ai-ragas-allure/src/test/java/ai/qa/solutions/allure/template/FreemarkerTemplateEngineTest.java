package ai.qa.solutions.allure.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.qa.solutions.allure.listener.RenderConfig;
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
        @DisplayName("should include methodology when methodology section is enabled explicitly")
        void shouldIncludeMethodology() {
            final EvaluationReportData data = createTestData();
            final RenderConfig methodologyOn = new RenderConfig(true, true, true, true, true, true, true);

            final String html = engine.renderHtml(data, methodologyOn);

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

    @Nested
    @DisplayName("section toggles — HTML")
    class HtmlSectionToggles {

        @Test
        @DisplayName("withSummary=false should omit summary section but keep others")
        void withSummaryFalseShouldOmitSummary() {
            final EvaluationReportData data = createTestData();
            final RenderConfig config = new RenderConfig(true, true, false, true, true, true, true);

            final String html = engine.renderHtml(data, config);

            assertThat(html).doesNotContain("id=\"block-summary\"");
            assertThat(html).contains("id=\"block-methodology\"");
            assertThat(html).contains("id=\"block-execution\"");
        }

        @Test
        @DisplayName("withExplanation=false should omit explanation section but keep others")
        void withExplanationFalseShouldOmitExplanation() {
            final EvaluationReportData data = createTestData();
            final RenderConfig config = new RenderConfig(true, true, true, false, true, true, true);

            final String html = engine.renderHtml(data, config);

            assertThat(html).doesNotContain("id=\"block-explanation\"");
            assertThat(html).contains("id=\"block-summary\"");
            assertThat(html).contains("id=\"block-methodology\"");
            assertThat(html).contains("id=\"block-execution\"");
        }

        @Test
        @DisplayName("withMethodology=false should omit methodology section but keep others")
        void withMethodologyFalseShouldOmitMethodology() {
            final EvaluationReportData data = createTestData();
            final RenderConfig config = new RenderConfig(true, true, true, true, false, true, true);

            final String html = engine.renderHtml(data, config);

            assertThat(html).doesNotContain("id=\"block-methodology\"");
            assertThat(html).doesNotContain("Test methodology");
            assertThat(html).contains("id=\"block-summary\"");
            assertThat(html).contains("id=\"block-execution\"");
        }

        @Test
        @DisplayName("withExecutionLog=false should omit execution log section but keep others")
        void withExecutionLogFalseShouldOmitExecutionLog() {
            final EvaluationReportData data = createTestData();
            final RenderConfig config = new RenderConfig(true, true, true, true, true, false, true);

            final String html = engine.renderHtml(data, config);

            assertThat(html).doesNotContain("id=\"block-execution\"");
            assertThat(html).contains("id=\"block-summary\"");
            assertThat(html).contains("id=\"block-methodology\"");
        }

        @Test
        @DisplayName("withExcludedModels=false should NOT affect HTML output (no excluded-models block in HTML)")
        void withExcludedModelsFalseShouldNotAffectHtml() {
            final EvaluationReportData data = createTestData();
            final RenderConfig configOn = new RenderConfig(true, true, true, true, true, true, true);
            final RenderConfig configOff = new RenderConfig(true, true, true, true, true, true, false);

            final String htmlOn = engine.renderHtml(data, configOn);
            final String htmlOff = engine.renderHtml(data, configOff);

            assertThat(htmlOn).isEqualTo(htmlOff);
        }
    }

    @Nested
    @DisplayName("section toggles — Markdown")
    class MarkdownSectionToggles {

        @Test
        @DisplayName("withSummary=false should omit summary section but keep others")
        void withSummaryFalseShouldOmitSummary() {
            final EvaluationReportData data = createTestData();
            final RenderConfig config = new RenderConfig(true, true, false, true, true, true, true);

            final String md = engine.renderMarkdown(data, config);

            assertThat(md).doesNotContain("## Summary");
            assertThat(md).contains("## Methodology");
            assertThat(md).contains("## Execution Log");
        }

        @Test
        @DisplayName("withExplanation=false should omit explanation section but keep others")
        void withExplanationFalseShouldOmitExplanation() {
            final EvaluationReportData data = createTestData();
            final RenderConfig config = new RenderConfig(true, true, true, false, true, true, true);

            final String md = engine.renderMarkdown(data, config);

            assertThat(md).doesNotContain("## Score Explanation");
            assertThat(md).contains("## Summary");
            assertThat(md).contains("## Methodology");
            assertThat(md).contains("## Execution Log");
        }

        @Test
        @DisplayName("withMethodology=false should omit methodology section but keep others")
        void withMethodologyFalseShouldOmitMethodology() {
            final EvaluationReportData data = createTestData();
            final RenderConfig config = new RenderConfig(true, true, true, true, false, true, true);

            final String md = engine.renderMarkdown(data, config);

            assertThat(md).doesNotContain("## Methodology");
            assertThat(md).doesNotContain("# Test methodology");
            assertThat(md).contains("## Summary");
            assertThat(md).contains("## Execution Log");
        }

        @Test
        @DisplayName("withExecutionLog=false should omit execution log section but keep others")
        void withExecutionLogFalseShouldOmitExecutionLog() {
            final EvaluationReportData data = createTestData();
            final RenderConfig config = new RenderConfig(true, true, true, true, true, false, true);

            final String md = engine.renderMarkdown(data, config);

            assertThat(md).doesNotContain("## Execution Log");
            assertThat(md).contains("## Summary");
            assertThat(md).contains("## Methodology");
        }

        @Test
        @DisplayName("withExcludedModels=false should omit excluded-models section but keep others")
        void withExcludedModelsFalseShouldOmitExcludedModels() {
            final EvaluationReportData data = createTestDataWithExclusions();
            final RenderConfig config = new RenderConfig(true, true, true, true, true, true, false);

            final String md = engine.renderMarkdown(data, config);

            assertThat(md).doesNotContain("## Excluded Models");
            assertThat(md).contains("## Summary");
            assertThat(md).contains("## Methodology");
            assertThat(md).contains("## Execution Log");
        }
    }

    @Nested
    @DisplayName("default RenderConfig (methodology=false)")
    class DefaultRenderConfig {

        @Test
        @DisplayName("renderHtml with default config should omit methodology section (regression for breaking change)")
        void renderHtmlShouldOmitMethodologyByDefault() {
            final EvaluationReportData data = createTestData();

            final String html = engine.renderHtml(data, RenderConfig.defaults());

            assertThat(html).doesNotContain("id=\"block-methodology\"");
            assertThat(html).doesNotContain("Test methodology");
            // Other sections still rendered
            assertThat(html).contains("id=\"block-summary\"");
            assertThat(html).contains("id=\"block-execution\"");
        }

        @Test
        @DisplayName(
                "renderMarkdown with default config should omit methodology section (regression for breaking change)")
        void renderMarkdownShouldOmitMethodologyByDefault() {
            final EvaluationReportData data = createTestData();

            final String md = engine.renderMarkdown(data, RenderConfig.defaults());

            assertThat(md).doesNotContain("## Methodology");
            assertThat(md).doesNotContain("# Test methodology");
            assertThat(md).contains("## Summary");
            assertThat(md).contains("## Execution Log");
        }

        @Test
        @DisplayName("no-arg renderHtml should also omit methodology by default")
        void noArgRenderHtmlShouldOmitMethodology() {
            final EvaluationReportData data = createTestData();

            final String html = engine.renderHtml(data);

            assertThat(html).doesNotContain("id=\"block-methodology\"");
            assertThat(html).doesNotContain("Test methodology");
        }

        @Test
        @DisplayName("no-arg renderMarkdown should also omit methodology by default")
        void noArgRenderMarkdownShouldOmitMethodology() {
            final EvaluationReportData data = createTestData();

            final String md = engine.renderMarkdown(data);

            assertThat(md).doesNotContain("## Methodology");
            assertThat(md).doesNotContain("# Test methodology");
        }
    }

    private EvaluationReportData createTestData() {
        return baseBuilder().build();
    }

    private EvaluationReportData createTestDataWithExclusions() {
        return baseBuilder()
                .exclusions(List.of(ai.qa.solutions.allure.model.ModelExclusionData.builder()
                        .modelId("model-3")
                        .failedStepName("InvokeLlm")
                        .errorMessage("rate limited")
                        .stackTrace("at com.example.Foo.bar(Foo.java:1)")
                        .build()))
                .build();
    }

    private EvaluationReportData.EvaluationReportDataBuilder baseBuilder() {
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
                        .build());
    }
}
