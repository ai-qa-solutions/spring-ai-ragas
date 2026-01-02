package ai.qa.solutions.execution.visual;

import static org.assertj.core.api.Assertions.assertThat;

import ai.qa.solutions.execution.visual.AsciiRenderBarsAuto.Item;
import ai.qa.solutions.execution.visual.AsciiRenderBarsAuto.Result;
import ai.qa.solutions.execution.visual.AsciiRenderBarsAuto.Summary;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AsciiRenderBarsAuto Tests")
class AsciiRenderBarsAutoTest {

    @Nested
    @DisplayName("Item Creation")
    class ItemCreation {

        @Test
        @DisplayName("Should create item with label and value")
        void shouldCreateItemWithLabelAndValue() {
            // When
            final Item item = new Item("model-1", 0.85);

            // Then
            assertThat(item.label()).isEqualTo("model-1");
            assertThat(item.value()).isEqualTo(0.85);
        }

        @Test
        @DisplayName("Should handle null label")
        void shouldHandleNullLabel() {
            // When
            final Item item = new Item(null, 0.5);

            // Then
            assertThat(item.label()).isEmpty();
            assertThat(item.value()).isEqualTo(0.5);
        }
    }

    @Nested
    @DisplayName("Empty or Null Input")
    class EmptyOrNullInput {

        @Test
        @DisplayName("Should handle null items")
        void shouldHandleNullItems() {
            // When
            final Result result = AsciiRenderBarsAuto.renderBarsAuto(null, 100, 10);

            // Then
            assertThat(result.text()).isEqualTo("(no data)");
            assertThat(result.summary().min()).isNaN();
            assertThat(result.summary().max()).isNaN();
            assertThat(result.summary().avg()).isNaN();
        }

        @Test
        @DisplayName("Should handle empty items list")
        void shouldHandleEmptyItemsList() {
            // When
            final Result result = AsciiRenderBarsAuto.renderBarsAuto(List.of(), 100, 10);

            // Then
            assertThat(result.text()).isEqualTo("(no data)");
            assertThat(result.summary().min()).isNaN();
        }
    }

    @Nested
    @DisplayName("Basic Rendering")
    class BasicRendering {

        @Test
        @DisplayName("Should render chart for single item")
        void shouldRenderChartForSingleItem() {
            // Given
            final List<Item> items = List.of(new Item("model-1", 0.75));

            // When
            final Result result = AsciiRenderBarsAuto.renderBarsAuto(items, 80, 0);

            // Then
            assertThat(result.text()).isNotBlank();
            assertThat(result.text()).contains("model-1");
            assertThat(result.text()).contains("0.75");
        }

        @Test
        @DisplayName("Should render chart for multiple items")
        void shouldRenderChartForMultipleItems() {
            // Given
            final List<Item> items =
                    List.of(new Item("model-1", 0.5), new Item("model-2", 0.7), new Item("model-3", 0.9));

            // When
            final Result result = AsciiRenderBarsAuto.renderBarsAuto(items, 100, 0);

            // Then
            assertThat(result.text()).isNotBlank();
            assertThat(result.text()).contains("model-1");
            assertThat(result.text()).contains("model-2");
            assertThat(result.text()).contains("model-3");
            assertThat(result.text()).contains("0.50");
            assertThat(result.text()).contains("0.70");
            assertThat(result.text()).contains("0.90");
        }
    }

    @Nested
    @DisplayName("Summary Calculation")
    class SummaryCalculation {

        @Test
        @DisplayName("Should calculate correct summary statistics")
        void shouldCalculateCorrectSummaryStatistics() {
            // Given
            final List<Item> items = List.of(new Item("low", 0.2), new Item("mid", 0.5), new Item("high", 0.8));

            // When
            final Result result = AsciiRenderBarsAuto.renderBarsAuto(items, 100, 0);
            final Summary summary = result.summary();

            // Then
            assertThat(summary.min()).isEqualTo(0.2);
            assertThat(summary.max()).isEqualTo(0.8);
            assertThat(summary.avg()).isCloseTo(0.5, org.assertj.core.data.Offset.offset(0.01));
            assertThat(summary.minLabel()).isEqualTo("low");
            assertThat(summary.maxLabel()).isEqualTo("high");
        }

        @Test
        @DisplayName("Should handle identical values")
        void shouldHandleIdenticalValues() {
            // Given
            final List<Item> items = List.of(new Item("a", 0.5), new Item("b", 0.5), new Item("c", 0.5));

            // When
            final Result result = AsciiRenderBarsAuto.renderBarsAuto(items, 100, 0);
            final Summary summary = result.summary();

            // Then
            assertThat(summary.min()).isEqualTo(0.5);
            assertThat(summary.max()).isEqualTo(0.5);
            assertThat(summary.avg()).isEqualTo(0.5);
        }
    }

    @Nested
    @DisplayName("Chart Dimensions")
    class ChartDimensions {

        @Test
        @DisplayName("Should use auto height when height is 0")
        void shouldUseAutoHeightWhenHeightIs0() {
            // Given
            final List<Item> items = List.of(new Item("a", 0.5), new Item("b", 0.7));

            // When
            final Result result = AsciiRenderBarsAuto.renderBarsAuto(items, 100, 0);

            // Then
            assertThat(result.text()).isNotBlank();
            assertThat(result.text().lines().count()).isGreaterThan(2);
        }

        @Test
        @DisplayName("Should use specified width")
        void shouldUseSpecifiedWidth() {
            // Given
            final List<Item> items = List.of(new Item("test", 0.5));

            // When
            final Result result = AsciiRenderBarsAuto.renderBarsAuto(items, 50, 0);

            // Then
            assertThat(result.text()).isNotBlank();
            final long maxLineLength =
                    result.text().lines().mapToInt(String::length).max().orElse(0);
            assertThat(maxLineLength).isLessThanOrEqualTo(50);
        }
    }

    @Nested
    @DisplayName("Label Handling")
    class LabelHandling {

        @Test
        @DisplayName("Should truncate long labels")
        void shouldTruncateLongLabels() {
            // Given
            final String longLabel = "very-long-model-name-that-should-be-truncated";
            final List<Item> items = List.of(new Item(longLabel, 0.5));

            // When
            final Result result = AsciiRenderBarsAuto.renderBarsAuto(items, 80, 0);

            // Then
            assertThat(result.text()).isNotBlank();
            assertThat(result.text()).contains("…"); // Ellipsis character
        }

        @Test
        @DisplayName("Should handle empty labels")
        void shouldHandleEmptyLabels() {
            // Given
            final List<Item> items = List.of(new Item("", 0.5));

            // When
            final Result result = AsciiRenderBarsAuto.renderBarsAuto(items, 80, 0);

            // Then
            assertThat(result.text()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("Value Range Handling")
    class ValueRangeHandling {

        @Test
        @DisplayName("Should handle very small values")
        void shouldHandleVerySmallValues() {
            // Given
            final List<Item> items = List.of(new Item("a", 0.001), new Item("b", 0.002));

            // When
            final Result result = AsciiRenderBarsAuto.renderBarsAuto(items, 100, 0);

            // Then
            assertThat(result.text()).isNotBlank();
            assertThat(result.summary().min()).isEqualTo(0.001);
            assertThat(result.summary().max()).isEqualTo(0.002);
        }

        @Test
        @DisplayName("Should handle values close to 1.0")
        void shouldHandleValuesCloseTo1() {
            // Given
            final List<Item> items = List.of(new Item("a", 0.99), new Item("b", 1.0));

            // When
            final Result result = AsciiRenderBarsAuto.renderBarsAuto(items, 100, 0);

            // Then
            assertThat(result.text()).isNotBlank();
            assertThat(result.summary().min()).isEqualTo(0.99);
            assertThat(result.summary().max()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should handle negative values")
        void shouldHandleNegativeValues() {
            // Given
            final List<Item> items = List.of(new Item("a", -0.5), new Item("b", 0.5));

            // When
            final Result result = AsciiRenderBarsAuto.renderBarsAuto(items, 100, 0);

            // Then
            assertThat(result.text()).isNotBlank();
            assertThat(result.summary().min()).isEqualTo(-0.5);
            assertThat(result.summary().max()).isEqualTo(0.5);
        }
    }

    @Nested
    @DisplayName("Chart Elements")
    class ChartElements {

        @Test
        @DisplayName("Should include min and max values in chart")
        void shouldIncludeMinAndMaxValuesInChart() {
            // Given
            final List<Item> items = List.of(new Item("low", 0.2), new Item("high", 0.8));

            // When
            final Result result = AsciiRenderBarsAuto.renderBarsAuto(items, 100, 0);

            // Then
            assertThat(result.text()).contains("0.20"); // min
            assertThat(result.text()).contains("0.80"); // max
        }

        @Test
        @DisplayName("Should render bars with block character")
        void shouldRenderBarsWithBlockCharacter() {
            // Given
            final List<Item> items = List.of(new Item("test", 0.5));

            // When
            final Result result = AsciiRenderBarsAuto.renderBarsAuto(items, 100, 0);

            // Then
            assertThat(result.text()).contains("█"); // Block character
        }
    }

    @Nested
    @DisplayName("Result Record")
    class ResultRecord {

        @Test
        @DisplayName("Should return both text and summary")
        void shouldReturnBothTextAndSummary() {
            // Given
            final List<Item> items = List.of(new Item("test", 0.5));

            // When
            final Result result = AsciiRenderBarsAuto.renderBarsAuto(items, 100, 0);

            // Then
            assertThat(result.text()).isNotBlank();
            assertThat(result.summary()).isNotNull();
            assertThat(result.summary().avg()).isEqualTo(0.5);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle single value equal to min and max")
        void shouldHandleSingleValueEqualToMinAndMax() {
            // Given
            final List<Item> items = List.of(new Item("only", 0.75));

            // When
            final Result result = AsciiRenderBarsAuto.renderBarsAuto(items, 100, 0);
            final Summary summary = result.summary();

            // Then
            assertThat(summary.min()).isEqualTo(0.75);
            assertThat(summary.max()).isEqualTo(0.75);
            assertThat(summary.avg()).isEqualTo(0.75);
            assertThat(summary.minLabel()).isEqualTo("only");
            assertThat(summary.maxLabel()).isEqualTo("only");
        }

        @Test
        @DisplayName("Should handle very narrow width")
        void shouldHandleVeryNarrowWidth() {
            // Given
            final List<Item> items = List.of(new Item("a", 0.5));

            // When
            final Result result = AsciiRenderBarsAuto.renderBarsAuto(items, 30, 0);

            // Then
            assertThat(result.text()).isNotBlank();
        }
    }
}
