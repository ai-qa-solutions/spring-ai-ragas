package ai.qa.solutions.execution.visual;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AsciiGanttChart Tests")
class AsciiGanttChartTest {

    @Nested
    @DisplayName("render")
    class Render {

        @Test
        @DisplayName("Should return 'No execution data' for null list")
        void shouldReturnNoDataForNullList() {
            // When
            final AsciiGanttChart.Result result = AsciiGanttChart.render(null);

            // Then
            assertThat(result.text()).isEqualTo("No execution data");
            assertThat(result.maxTimeMs()).isZero();
            assertThat(result.itemCount()).isZero();
        }

        @Test
        @DisplayName("Should return 'No execution data' for empty list")
        void shouldReturnNoDataForEmptyList() {
            // When
            final AsciiGanttChart.Result result = AsciiGanttChart.render(List.of());

            // Then
            assertThat(result.text()).isEqualTo("No execution data");
            assertThat(result.maxTimeMs()).isZero();
            assertThat(result.itemCount()).isZero();
        }

        @Test
        @DisplayName("Should render single item chart")
        void shouldRenderSingleItemChart() {
            // Given
            final List<AsciiGanttChart.Item> items =
                    List.of(new AsciiGanttChart.Item("model-1", Duration.ofMillis(1000)));

            // When
            final AsciiGanttChart.Result result = AsciiGanttChart.render(items);

            // Then
            assertThat(result.itemCount()).isEqualTo(1);
            assertThat(result.maxTimeMs()).isEqualTo(1000);
            assertThat(result.text()).contains("model-1");
            assertThat(result.text()).contains("1000ms");
        }

        @Test
        @DisplayName("Should render multiple items sorted alphabetically")
        void shouldRenderMultipleItemsSortedAlphabetically() {
            // Given
            final List<AsciiGanttChart.Item> items = List.of(
                    new AsciiGanttChart.Item("zebra-model", Duration.ofMillis(500)),
                    new AsciiGanttChart.Item("alpha-model", Duration.ofMillis(1000)),
                    new AsciiGanttChart.Item("beta-model", Duration.ofMillis(750)));

            // When
            final AsciiGanttChart.Result result = AsciiGanttChart.render(items);

            // Then
            assertThat(result.itemCount()).isEqualTo(3);
            assertThat(result.maxTimeMs()).isEqualTo(1000);

            final String text = result.text();
            final int alphaPos = text.indexOf("alpha-model");
            final int betaPos = text.indexOf("beta-model");
            final int zebraPos = text.indexOf("zebra-model");

            assertThat(alphaPos).isLessThan(betaPos);
            assertThat(betaPos).isLessThan(zebraPos);
        }

        @Test
        @DisplayName("Should handle custom width")
        void shouldHandleCustomWidth() {
            // Given
            final List<AsciiGanttChart.Item> items =
                    List.of(new AsciiGanttChart.Item("test-model", Duration.ofMillis(1000)));

            // When
            final AsciiGanttChart.Result result = AsciiGanttChart.render(items, 50);

            // Then
            assertThat(result.text()).isNotEmpty();
            assertThat(result.itemCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should use correct maxTimeMs from longest duration")
        void shouldUseCorrectMaxTimeFromLongestDuration() {
            // Given
            final List<AsciiGanttChart.Item> items = List.of(
                    new AsciiGanttChart.Item("fast", Duration.ofMillis(100)),
                    new AsciiGanttChart.Item("slow", Duration.ofMillis(5000)),
                    new AsciiGanttChart.Item("medium", Duration.ofMillis(2000)));

            // When
            final AsciiGanttChart.Result result = AsciiGanttChart.render(items);

            // Then
            assertThat(result.maxTimeMs()).isEqualTo(5000);
        }
    }

    @Nested
    @DisplayName("Item record")
    class ItemRecord {

        @Test
        @DisplayName("durationMs should return duration in milliseconds")
        void durationMsShouldReturnDurationInMilliseconds() {
            // Given
            final AsciiGanttChart.Item item = new AsciiGanttChart.Item("model", Duration.ofSeconds(5));

            // When/Then
            assertThat(item.durationMs()).isEqualTo(5000);
        }

        @Test
        @DisplayName("durationMs should return 0 for null duration")
        void durationMsShouldReturn0ForNullDuration() {
            // Given
            final AsciiGanttChart.Item item = new AsciiGanttChart.Item("model", null);

            // When/Then
            assertThat(item.durationMs()).isZero();
        }

        @Test
        @DisplayName("Should preserve model ID")
        void shouldPreserveModelId() {
            // Given
            final AsciiGanttChart.Item item = new AsciiGanttChart.Item("my-special-model", Duration.ofMillis(100));

            // When/Then
            assertThat(item.modelId()).isEqualTo("my-special-model");
        }
    }

    @Nested
    @DisplayName("Result record")
    class ResultRecord {

        @Test
        @DisplayName("Should contain all fields")
        void shouldContainAllFields() {
            // Given
            final AsciiGanttChart.Result result = new AsciiGanttChart.Result("chart text", 5000L, 3);

            // Then
            assertThat(result.text()).isEqualTo("chart text");
            assertThat(result.maxTimeMs()).isEqualTo(5000L);
            assertThat(result.itemCount()).isEqualTo(3);
        }
    }
}
