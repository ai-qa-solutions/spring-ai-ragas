package ai.qa.solutions.allure.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ChartData")
class ChartDataTest {

    @Nested
    @DisplayName("hasHeatmapData")
    class HasHeatmapData {

        @Test
        @DisplayName("should return true when all heatmap data is present")
        void shouldReturnTrueWhenAllDataPresent() {
            final ChartData chartData = ChartData.builder()
                    .heatmapRowLabels(List.of("model-1", "model-2"))
                    .heatmapColLabels(List.of("stmt-1", "stmt-2", "stmt-3"))
                    .heatmapValues(List.of(List.of(1, 0, 1), List.of(1, 1, 0)))
                    .build();

            assertThat(chartData.hasHeatmapData()).isTrue();
        }

        @Test
        @DisplayName("should return false when heatmap values are empty")
        void shouldReturnFalseWhenValuesEmpty() {
            final ChartData chartData = ChartData.builder()
                    .heatmapRowLabels(List.of("model-1"))
                    .heatmapColLabels(List.of("stmt-1"))
                    .heatmapValues(List.of())
                    .build();

            assertThat(chartData.hasHeatmapData()).isFalse();
        }

        @Test
        @DisplayName("should return false when row labels are empty")
        void shouldReturnFalseWhenRowLabelsEmpty() {
            final ChartData chartData = ChartData.builder()
                    .heatmapRowLabels(List.of())
                    .heatmapColLabels(List.of("stmt-1"))
                    .heatmapValues(List.of(List.of(1)))
                    .build();

            assertThat(chartData.hasHeatmapData()).isFalse();
        }
    }

    @Nested
    @DisplayName("hasTimelineData")
    class HasTimelineData {

        @Test
        @DisplayName("should return true when timeline entries exist")
        void shouldReturnTrueWhenEntriesExist() {
            final ChartData chartData = ChartData.builder()
                    .timelineEntries(List.of(ChartData.TimelineEntry.builder()
                            .modelId("model-1")
                            .stepName("Step1")
                            .stepType("LLM")
                            .durationMs(100)
                            .build()))
                    .build();

            assertThat(chartData.hasTimelineData()).isTrue();
        }

        @Test
        @DisplayName("should return false when no timeline entries")
        void shouldReturnFalseWhenNoEntries() {
            final ChartData chartData = ChartData.builder().build();

            assertThat(chartData.hasTimelineData()).isFalse();
        }
    }

    @Nested
    @DisplayName("hasScoreData")
    class HasScoreData {

        @Test
        @DisplayName("should return true when score entries exist")
        void shouldReturnTrueWhenEntriesExist() {
            final ChartData chartData = ChartData.builder()
                    .scoreEntries(List.of(ChartData.ScoreEntry.builder()
                            .modelId("model-1")
                            .score(0.85)
                            .build()))
                    .build();

            assertThat(chartData.hasScoreData()).isTrue();
        }

        @Test
        @DisplayName("should return false when no score entries")
        void shouldReturnFalseWhenNoEntries() {
            final ChartData chartData = ChartData.builder().build();

            assertThat(chartData.hasScoreData()).isFalse();
        }
    }

    @Nested
    @DisplayName("getHeatmapWidth")
    class GetHeatmapWidth {

        @Test
        @DisplayName("should calculate correct width based on columns")
        void shouldCalculateCorrectWidth() {
            final ChartData chartData = ChartData.builder()
                    .heatmapColLabels(List.of("col1", "col2", "col3"))
                    .build();

            // 150 (label) + 3 * 30 (cells) + 20 (margin) = 260
            assertThat(chartData.getHeatmapWidth()).isEqualTo(260);
        }
    }

    @Nested
    @DisplayName("getHeatmapHeight")
    class GetHeatmapHeight {

        @Test
        @DisplayName("should calculate correct height based on rows")
        void shouldCalculateCorrectHeight() {
            final ChartData chartData = ChartData.builder()
                    .heatmapRowLabels(List.of("row1", "row2"))
                    .build();

            // 50 (header) + 2 * 30 (cells) + 20 (margin) = 130
            assertThat(chartData.getHeatmapHeight()).isEqualTo(130);
        }
    }

    @Nested
    @DisplayName("TimelineEntry")
    class TimelineEntryTest {

        @Test
        @DisplayName("should build with all fields")
        void shouldBuildWithAllFields() {
            final ChartData.TimelineEntry entry = ChartData.TimelineEntry.builder()
                    .modelId("claude-3")
                    .stepName("GenerateStatements")
                    .stepType("LLM")
                    .startOffsetMs(100)
                    .durationMs(500)
                    .success(true)
                    .build();

            assertThat(entry.getModelId()).isEqualTo("claude-3");
            assertThat(entry.getStepName()).isEqualTo("GenerateStatements");
            assertThat(entry.getStepType()).isEqualTo("LLM");
            assertThat(entry.getStartOffsetMs()).isEqualTo(100);
            assertThat(entry.getDurationMs()).isEqualTo(500);
            assertThat(entry.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("ScoreEntry")
    class ScoreEntryTest {

        @Test
        @DisplayName("should build with default excluded=false")
        void shouldBuildWithDefaultExcluded() {
            final ChartData.ScoreEntry entry =
                    ChartData.ScoreEntry.builder().modelId("gpt-4").score(0.92).build();

            assertThat(entry.getModelId()).isEqualTo("gpt-4");
            assertThat(entry.getScore()).isEqualTo(0.92);
            assertThat(entry.isExcluded()).isFalse();
        }

        @Test
        @DisplayName("should build with excluded=true")
        void shouldBuildWithExcludedTrue() {
            final ChartData.ScoreEntry entry = ChartData.ScoreEntry.builder()
                    .modelId("failed-model")
                    .score(0.0)
                    .excluded(true)
                    .build();

            assertThat(entry.isExcluded()).isTrue();
        }
    }
}
