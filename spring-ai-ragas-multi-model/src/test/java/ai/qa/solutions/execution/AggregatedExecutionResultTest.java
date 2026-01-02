package ai.qa.solutions.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AggregatedExecutionResult Tests")
class AggregatedExecutionResultTest {

    @Nested
    @DisplayName("Basic Properties")
    class BasicProperties {

        @Test
        @DisplayName("Should build with all properties")
        void shouldBuildWithAllProperties() {
            // Given
            final ModelExecutionContext context = createContext("model-1");
            final ModelExecutionResult result1 = ModelExecutionResult.success(context, 0.8, "response1");
            final ModelExecutionResult result2 = ModelExecutionResult.failure(context, new RuntimeException("error"));

            // When
            final AggregatedExecutionResult aggregated = AggregatedExecutionResult.builder()
                    .metricName("TestMetric")
                    .result(result1)
                    .result(result2)
                    .aggregatedScore(0.8)
                    .aggregationStrategy("AVERAGE")
                    .build();

            // Then
            assertThat(aggregated.getMetricName()).isEqualTo("TestMetric");
            assertThat(aggregated.getResults()).hasSize(2);
            assertThat(aggregated.getAggregatedScore()).isEqualTo(0.8);
            assertThat(aggregated.getAggregationStrategy()).isEqualTo("AVERAGE");
            assertThat(aggregated.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should have default completedAt timestamp")
        void shouldHaveDefaultCompletedAtTimestamp() {
            // Given
            final Instant before = Instant.now();

            // When
            final AggregatedExecutionResult result = AggregatedExecutionResult.builder()
                    .metricName("TestMetric")
                    .aggregatedScore(0.5)
                    .aggregationStrategy("AVERAGE")
                    .build();

            // Then
            final Instant after = Instant.now();
            assertThat(result.getCompletedAt()).isBetween(before, after);
        }
    }

    @Nested
    @DisplayName("Successful Results Filtering")
    class SuccessfulResultsFiltering {

        @Test
        @DisplayName("Should filter successful results")
        void shouldFilterSuccessfulResults() {
            // Given
            final ModelExecutionContext context1 = createContext("model-1");
            final ModelExecutionContext context2 = createContext("model-2");
            final ModelExecutionContext context3 = createContext("model-3");

            final ModelExecutionResult success1 = ModelExecutionResult.success(context1, 0.8, "resp1");
            final ModelExecutionResult failure = ModelExecutionResult.failure(context2, new RuntimeException());
            final ModelExecutionResult success2 = ModelExecutionResult.success(context3, 0.9, "resp3");

            final AggregatedExecutionResult aggregated = AggregatedExecutionResult.builder()
                    .metricName("TestMetric")
                    .results(List.of(success1, failure, success2))
                    .aggregatedScore(0.85)
                    .aggregationStrategy("AVERAGE")
                    .build();

            // When
            final List<ModelExecutionResult> successful = aggregated.getSuccessfulResults();

            // Then
            assertThat(successful).hasSize(2);
            assertThat(successful).containsExactly(success1, success2);
        }

        @Test
        @DisplayName("Should return empty list when all failed")
        void shouldReturnEmptyListWhenAllFailed() {
            // Given
            final ModelExecutionContext context = createContext("model-1");
            final ModelExecutionResult failure = ModelExecutionResult.failure(context, new RuntimeException());

            final AggregatedExecutionResult aggregated = AggregatedExecutionResult.builder()
                    .metricName("TestMetric")
                    .results(List.of(failure))
                    .aggregatedScore(0.0)
                    .aggregationStrategy("AVERAGE")
                    .build();

            // When
            final List<ModelExecutionResult> successful = aggregated.getSuccessfulResults();

            // Then
            assertThat(successful).isEmpty();
        }
    }

    @Nested
    @DisplayName("Failed Results Filtering")
    class FailedResultsFiltering {

        @Test
        @DisplayName("Should filter failed results")
        void shouldFilterFailedResults() {
            // Given
            final ModelExecutionContext context1 = createContext("model-1");
            final ModelExecutionContext context2 = createContext("model-2");

            final ModelExecutionResult success = ModelExecutionResult.success(context1, 0.8, "resp");
            final ModelExecutionResult failure = ModelExecutionResult.failure(context2, new RuntimeException());

            final AggregatedExecutionResult aggregated = AggregatedExecutionResult.builder()
                    .metricName("TestMetric")
                    .results(List.of(success, failure))
                    .aggregatedScore(0.8)
                    .aggregationStrategy("AVERAGE")
                    .build();

            // When
            final List<ModelExecutionResult> failed = aggregated.getFailedResults();

            // Then
            assertThat(failed).hasSize(1);
            assertThat(failed.get(0)).isEqualTo(failure);
        }

        @Test
        @DisplayName("Should return empty list when all succeeded")
        void shouldReturnEmptyListWhenAllSucceeded() {
            // Given
            final ModelExecutionContext context = createContext("model-1");
            final ModelExecutionResult success = ModelExecutionResult.success(context, 0.8, "resp");

            final AggregatedExecutionResult aggregated = AggregatedExecutionResult.builder()
                    .metricName("TestMetric")
                    .results(List.of(success))
                    .aggregatedScore(0.8)
                    .aggregationStrategy("AVERAGE")
                    .build();

            // When
            final List<ModelExecutionResult> failed = aggregated.getFailedResults();

            // Then
            assertThat(failed).isEmpty();
        }
    }

    @Nested
    @DisplayName("Success Rate Calculation")
    class SuccessRateCalculation {

        @Test
        @DisplayName("Should calculate 100% success rate")
        void shouldCalculate100PercentSuccessRate() {
            // Given
            final AggregatedExecutionResult aggregated = createAggregatedResult(2, 0);

            // When
            final double successRate = aggregated.getSuccessRate();

            // Then
            assertThat(successRate).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should calculate 50% success rate")
        void shouldCalculate50PercentSuccessRate() {
            // Given
            final AggregatedExecutionResult aggregated = createAggregatedResult(1, 1);

            // When
            final double successRate = aggregated.getSuccessRate();

            // Then
            assertThat(successRate).isEqualTo(0.5);
        }

        @Test
        @DisplayName("Should calculate 0% success rate")
        void shouldCalculate0PercentSuccessRate() {
            // Given
            final AggregatedExecutionResult aggregated = createAggregatedResult(0, 2);

            // When
            final double successRate = aggregated.getSuccessRate();

            // Then
            assertThat(successRate).isZero();
        }

        @Test
        @DisplayName("Should return 0 for empty results")
        void shouldReturn0ForEmptyResults() {
            // Given
            final AggregatedExecutionResult aggregated = AggregatedExecutionResult.builder()
                    .metricName("TestMetric")
                    .aggregatedScore(0.0)
                    .aggregationStrategy("AVERAGE")
                    .build();

            // When
            final double successRate = aggregated.getSuccessRate();

            // Then
            assertThat(successRate).isZero();
        }
    }

    @Nested
    @DisplayName("Score Statistics")
    class ScoreStatistics {

        @Test
        @DisplayName("Should calculate statistics for successful scores")
        void shouldCalculateStatisticsForSuccessfulScores() {
            // Given
            final AggregatedExecutionResult aggregated = createAggregatedResultWithScores(0.5, 0.7, 0.9);

            // When
            final var stats = aggregated.getScoreStatistics();

            // Then
            assertThat(stats).isPresent();
            assertThat(stats.get().getMin()).isEqualTo(0.5);
            assertThat(stats.get().getMax()).isEqualTo(0.9);
            assertThat(stats.get().getAverage()).isCloseTo(0.7, org.assertj.core.data.Offset.offset(0.001));
            assertThat(stats.get().getCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should return empty when no successful scores")
        void shouldReturnEmptyWhenNoSuccessfulScores() {
            // Given
            final ModelExecutionContext context = createContext("model-1");
            final ModelExecutionResult failure = ModelExecutionResult.failure(context, new RuntimeException());

            final AggregatedExecutionResult aggregated = AggregatedExecutionResult.builder()
                    .metricName("TestMetric")
                    .results(List.of(failure))
                    .aggregatedScore(0.0)
                    .aggregationStrategy("AVERAGE")
                    .build();

            // When
            final Optional<java.util.DoubleSummaryStatistics> stats = aggregated.getScoreStatistics();

            // Then
            assertThat(stats).isEmpty();
        }
    }

    @Nested
    @DisplayName("Total Duration Calculation")
    class TotalDurationCalculation {

        @Test
        @DisplayName("Should return max duration")
        void shouldReturnMaxDuration() throws InterruptedException {
            // Given
            final ModelExecutionContext context1 = createContext("model-1");
            Thread.sleep(10);
            final ModelExecutionContext context2 = createContext("model-2");
            Thread.sleep(10);
            final ModelExecutionContext context3 = createContext("model-3");

            final ModelExecutionResult result1 = ModelExecutionResult.success(context1, 0.8, "resp1");
            final ModelExecutionResult result2 = ModelExecutionResult.success(context2, 0.9, "resp2");
            final ModelExecutionResult result3 = ModelExecutionResult.success(context3, 0.7, "resp3");

            final AggregatedExecutionResult aggregated = AggregatedExecutionResult.builder()
                    .metricName("TestMetric")
                    .results(List.of(result1, result2, result3))
                    .aggregatedScore(0.8)
                    .aggregationStrategy("AVERAGE")
                    .build();

            // When
            final Duration totalDuration = aggregated.getTotalDuration();

            // Then
            assertThat(totalDuration).isNotNull();
            assertThat(totalDuration).isGreaterThan(Duration.ZERO);
        }

        @Test
        @DisplayName("Should return zero for empty results")
        void shouldReturnZeroForEmptyResults() {
            // Given
            final AggregatedExecutionResult aggregated = AggregatedExecutionResult.builder()
                    .metricName("TestMetric")
                    .aggregatedScore(0.0)
                    .aggregationStrategy("AVERAGE")
                    .build();

            // When
            final Duration totalDuration = aggregated.getTotalDuration();

            // Then
            assertThat(totalDuration).isEqualTo(Duration.ZERO);
        }
    }

    // ========== Helper Methods ==========

    private ModelExecutionContext createContext(final String modelId) {
        return ModelExecutionContext.builder()
                .modelId(modelId)
                .metricName("TestMetric")
                .prompt("test prompt")
                .build();
    }

    private AggregatedExecutionResult createAggregatedResult(final int successCount, final int failureCount) {
        final List<ModelExecutionResult> results = new java.util.ArrayList<>();

        for (int i = 0; i < successCount; i++) {
            final ModelExecutionContext context = createContext("success-" + i);
            results.add(ModelExecutionResult.success(context, 0.8, "response"));
        }

        for (int i = 0; i < failureCount; i++) {
            final ModelExecutionContext context = createContext("failure-" + i);
            results.add(ModelExecutionResult.failure(context, new RuntimeException()));
        }

        return AggregatedExecutionResult.builder()
                .metricName("TestMetric")
                .results(results)
                .aggregatedScore(0.8)
                .aggregationStrategy("AVERAGE")
                .build();
    }

    private AggregatedExecutionResult createAggregatedResultWithScores(final Double... scores) {
        final List<ModelExecutionResult> results = new java.util.ArrayList<>();

        for (int i = 0; i < scores.length; i++) {
            final ModelExecutionContext context = createContext("model-" + i);
            results.add(ModelExecutionResult.success(context, scores[i], "response"));
        }

        return AggregatedExecutionResult.builder()
                .metricName("TestMetric")
                .results(results)
                .aggregatedScore(0.7)
                .aggregationStrategy("AVERAGE")
                .build();
    }
}
