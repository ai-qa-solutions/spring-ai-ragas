package ai.qa.solutions.allure.template;

import static org.assertj.core.api.Assertions.assertThat;

import ai.qa.solutions.allure.model.ChartData;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SvgChartGenerator")
class SvgChartGeneratorTest {

    private SvgChartGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new SvgChartGenerator();
    }

    @Nested
    @DisplayName("generateHeatmap")
    class GenerateHeatmap {

        @Test
        @DisplayName("should generate valid SVG for heatmap")
        void shouldGenerateValidSvg() {
            final ChartData chartData = ChartData.builder()
                    .heatmapRowLabels(List.of("model-1", "model-2"))
                    .heatmapColLabels(List.of("stmt-1", "stmt-2", "stmt-3"))
                    .heatmapValues(List.of(List.of(1, 0, 1), List.of(1, 1, 0)))
                    .build();

            final String svg = generator.generateHeatmap(chartData);

            assertThat(svg).startsWith("<svg");
            assertThat(svg).endsWith("</svg>");
            assertThat(svg).contains("xmlns=\"http://www.w3.org/2000/svg\"");
            assertThat(svg).contains("class=\"heatmap-chart\"");
        }

        @Test
        @DisplayName("should include row labels")
        void shouldIncludeRowLabels() {
            final ChartData chartData = ChartData.builder()
                    .heatmapRowLabels(List.of("claude-3", "gpt-4"))
                    .heatmapColLabels(List.of("s1"))
                    .heatmapValues(List.of(List.of(1), List.of(0)))
                    .build();

            final String svg = generator.generateHeatmap(chartData);

            assertThat(svg).contains("claude-3");
            assertThat(svg).contains("gpt-4");
        }

        @Test
        @DisplayName("should use correct colors for pass/fail")
        void shouldUseCorrectColors() {
            final ChartData chartData = ChartData.builder()
                    .heatmapRowLabels(List.of("m1"))
                    .heatmapColLabels(List.of("s1", "s2"))
                    .heatmapValues(List.of(List.of(1, 0)))
                    .build();

            final String svg = generator.generateHeatmap(chartData);

            assertThat(svg).contains("cell-pass");
            assertThat(svg).contains("cell-fail");
        }

        @Test
        @DisplayName("should return empty string when no data")
        void shouldReturnEmptyWhenNoData() {
            final ChartData chartData = ChartData.builder().build();

            final String svg = generator.generateHeatmap(chartData);

            assertThat(svg).isEmpty();
        }

        @Test
        @DisplayName("should truncate long labels")
        void shouldTruncateLongLabels() {
            final ChartData chartData = ChartData.builder()
                    .heatmapRowLabels(List.of("this-is-a-very-long-model-name-that-should-be-truncated"))
                    .heatmapColLabels(List.of("s1"))
                    .heatmapValues(List.of(List.of(1)))
                    .build();

            final String svg = generator.generateHeatmap(chartData);

            assertThat(svg).contains("...");
        }
    }

    @Nested
    @DisplayName("generateTimeline")
    class GenerateTimeline {

        @Test
        @DisplayName("should generate valid SVG for timeline")
        void shouldGenerateValidSvg() {
            final ChartData chartData = ChartData.builder()
                    .timelineEntries(List.of(
                            ChartData.TimelineEntry.builder()
                                    .modelId("model-1")
                                    .stepName("Step1")
                                    .stepType("LLM")
                                    .startOffsetMs(0)
                                    .durationMs(500)
                                    .success(true)
                                    .build(),
                            ChartData.TimelineEntry.builder()
                                    .modelId("model-2")
                                    .stepName("Step1")
                                    .stepType("LLM")
                                    .startOffsetMs(0)
                                    .durationMs(400)
                                    .success(true)
                                    .build()))
                    .maxDurationMs(500)
                    .build();

            final String svg = generator.generateTimeline(chartData);

            assertThat(svg).startsWith("<svg");
            assertThat(svg).endsWith("</svg>");
            assertThat(svg).contains("class=\"timeline-chart\"");
        }

        @Test
        @DisplayName("should include legend")
        void shouldIncludeLegend() {
            final ChartData chartData = ChartData.builder()
                    .timelineEntries(List.of(ChartData.TimelineEntry.builder()
                            .modelId("model-1")
                            .stepName("Step1")
                            .stepType("LLM")
                            .durationMs(100)
                            .success(true)
                            .build()))
                    .maxDurationMs(100)
                    .build();

            final String svg = generator.generateTimeline(chartData);

            assertThat(svg).contains("LLM");
            assertThat(svg).contains("Embedding");
            assertThat(svg).contains("Compute");
        }

        @Test
        @DisplayName("should use error color for failed entries")
        void shouldUseErrorColorForFailed() {
            final ChartData chartData = ChartData.builder()
                    .timelineEntries(List.of(ChartData.TimelineEntry.builder()
                            .modelId("model-1")
                            .stepName("Step1")
                            .stepType("LLM")
                            .durationMs(100)
                            .success(false)
                            .build()))
                    .maxDurationMs(100)
                    .build();

            final String svg = generator.generateTimeline(chartData);

            assertThat(svg).contains("bar-error");
        }

        @Test
        @DisplayName("should return empty string when no data")
        void shouldReturnEmptyWhenNoData() {
            final ChartData chartData = ChartData.builder().build();

            final String svg = generator.generateTimeline(chartData);

            assertThat(svg).isEmpty();
        }
    }

    @Nested
    @DisplayName("generateBarChart")
    class GenerateBarChart {

        @Test
        @DisplayName("should generate valid SVG for bar chart")
        void shouldGenerateValidSvg() {
            final ChartData chartData = ChartData.builder()
                    .scoreEntries(List.of(
                            ChartData.ScoreEntry.builder()
                                    .modelId("model-1")
                                    .score(0.85)
                                    .build(),
                            ChartData.ScoreEntry.builder()
                                    .modelId("model-2")
                                    .score(0.72)
                                    .build()))
                    .build();

            final String svg = generator.generateBarChart(chartData);

            assertThat(svg).startsWith("<svg");
            assertThat(svg).endsWith("</svg>");
            assertThat(svg).contains("class=\"bar-chart\"");
        }

        @Test
        @DisplayName("should use correct color classes based on score")
        void shouldUseCorrectColorClasses() {
            final ChartData chartData = ChartData.builder()
                    .scoreEntries(List.of(
                            ChartData.ScoreEntry.builder()
                                    .modelId("excellent")
                                    .score(0.95)
                                    .build(),
                            ChartData.ScoreEntry.builder()
                                    .modelId("good")
                                    .score(0.75)
                                    .build(),
                            ChartData.ScoreEntry.builder()
                                    .modelId("moderate")
                                    .score(0.55)
                                    .build(),
                            ChartData.ScoreEntry.builder()
                                    .modelId("poor")
                                    .score(0.25)
                                    .build()))
                    .build();

            final String svg = generator.generateBarChart(chartData);

            assertThat(svg).contains("bar-excellent");
            assertThat(svg).contains("bar-good");
            assertThat(svg).contains("bar-moderate");
            assertThat(svg).contains("bar-poor");
        }

        @Test
        @DisplayName("should use excluded color for excluded models")
        void shouldUseExcludedColor() {
            final ChartData chartData = ChartData.builder()
                    .scoreEntries(List.of(ChartData.ScoreEntry.builder()
                            .modelId("excluded-model")
                            .score(0.0)
                            .excluded(true)
                            .build()))
                    .build();

            final String svg = generator.generateBarChart(chartData);

            assertThat(svg).contains("bar-excluded");
        }

        @Test
        @DisplayName("should include score labels")
        void shouldIncludeScoreLabels() {
            final ChartData chartData = ChartData.builder()
                    .scoreEntries(List.of(ChartData.ScoreEntry.builder()
                            .modelId("model-1")
                            .score(0.8765)
                            .build()))
                    .build();

            final String svg = generator.generateBarChart(chartData);

            assertThat(svg).contains("0.8765");
        }

        @Test
        @DisplayName("should return empty string when no data")
        void shouldReturnEmptyWhenNoData() {
            final ChartData chartData = ChartData.builder().build();

            final String svg = generator.generateBarChart(chartData);

            assertThat(svg).isEmpty();
        }
    }
}
