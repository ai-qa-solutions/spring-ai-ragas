package ai.qa.solutions.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ScoreAggregator Tests")
class ScoreAggregatorTest {

    @Nested
    @DisplayName("AVERAGE Aggregator")
    class AverageAggregator {

        @Test
        @DisplayName("Should calculate average of scores")
        void shouldCalculateAverageOfScores() {
            // Given
            final List<Double> scores = List.of(0.5, 0.7, 0.9);

            // When
            final double result = ScoreAggregator.AVERAGE.aggregate(scores);

            // Then
            assertThat(result).isCloseTo(0.7, offset(0.001));
        }

        @Test
        @DisplayName("Should return 0 for empty list")
        void shouldReturn0ForEmptyList() {
            // Given
            final List<Double> scores = List.of();

            // When
            final double result = ScoreAggregator.AVERAGE.aggregate(scores);

            // Then
            assertThat(result).isZero();
        }

        @Test
        @DisplayName("Should return single value for one score")
        void shouldReturnSingleValueForOneScore() {
            // Given
            final List<Double> scores = List.of(0.8);

            // When
            final double result = ScoreAggregator.AVERAGE.aggregate(scores);

            // Then
            assertThat(result).isEqualTo(0.8);
        }

        @Test
        @DisplayName("Should have correct name")
        void shouldHaveCorrectName() {
            assertThat(ScoreAggregator.AVERAGE.getName()).isEqualTo("AVERAGE");
        }
    }

    @Nested
    @DisplayName("MEDIAN Aggregator")
    class MedianAggregator {

        @Test
        @DisplayName("Should calculate median for odd number of scores")
        void shouldCalculateMedianForOddNumberOfScores() {
            // Given
            final List<Double> scores = List.of(0.1, 0.5, 0.9);

            // When
            final double result = ScoreAggregator.MEDIAN.aggregate(scores);

            // Then
            assertThat(result).isEqualTo(0.5);
        }

        @Test
        @DisplayName("Should calculate median for even number of scores")
        void shouldCalculateMedianForEvenNumberOfScores() {
            // Given
            final List<Double> scores = List.of(0.2, 0.4, 0.6, 0.8);

            // When
            final double result = ScoreAggregator.MEDIAN.aggregate(scores);

            // Then
            assertThat(result).isCloseTo(0.5, offset(0.001)); // (0.4 + 0.6) / 2
        }

        @Test
        @DisplayName("Should handle unsorted scores")
        void shouldHandleUnsortedScores() {
            // Given
            final List<Double> scores = List.of(0.9, 0.1, 0.5, 0.3, 0.7);

            // When
            final double result = ScoreAggregator.MEDIAN.aggregate(scores);

            // Then
            assertThat(result).isEqualTo(0.5);
        }

        @Test
        @DisplayName("Should return single value for one score")
        void shouldReturnSingleValueForOneScore() {
            // Given
            final List<Double> scores = List.of(0.6);

            // When
            final double result = ScoreAggregator.MEDIAN.aggregate(scores);

            // Then
            assertThat(result).isEqualTo(0.6);
        }

        @Test
        @DisplayName("Should have correct name")
        void shouldHaveCorrectName() {
            assertThat(ScoreAggregator.MEDIAN.getName()).isEqualTo("MEDIAN");
        }
    }

    @Nested
    @DisplayName("MIN Aggregator")
    class MinAggregator {

        @Test
        @DisplayName("Should return minimum score")
        void shouldReturnMinimumScore() {
            // Given
            final List<Double> scores = List.of(0.5, 0.2, 0.9, 0.3);

            // When
            final double result = ScoreAggregator.MIN.aggregate(scores);

            // Then
            assertThat(result).isEqualTo(0.2);
        }

        @Test
        @DisplayName("Should return 0 for empty list")
        void shouldReturn0ForEmptyList() {
            // Given
            final List<Double> scores = List.of();

            // When
            final double result = ScoreAggregator.MIN.aggregate(scores);

            // Then
            assertThat(result).isZero();
        }

        @Test
        @DisplayName("Should return single value for one score")
        void shouldReturnSingleValueForOneScore() {
            // Given
            final List<Double> scores = List.of(0.7);

            // When
            final double result = ScoreAggregator.MIN.aggregate(scores);

            // Then
            assertThat(result).isEqualTo(0.7);
        }

        @Test
        @DisplayName("Should have correct name")
        void shouldHaveCorrectName() {
            assertThat(ScoreAggregator.MIN.getName()).isEqualTo("MIN");
        }
    }

    @Nested
    @DisplayName("MAX Aggregator")
    class MaxAggregator {

        @Test
        @DisplayName("Should return maximum score")
        void shouldReturnMaximumScore() {
            // Given
            final List<Double> scores = List.of(0.5, 0.9, 0.2, 0.7);

            // When
            final double result = ScoreAggregator.MAX.aggregate(scores);

            // Then
            assertThat(result).isEqualTo(0.9);
        }

        @Test
        @DisplayName("Should return 0 for empty list")
        void shouldReturn0ForEmptyList() {
            // Given
            final List<Double> scores = List.of();

            // When
            final double result = ScoreAggregator.MAX.aggregate(scores);

            // Then
            assertThat(result).isZero();
        }

        @Test
        @DisplayName("Should return single value for one score")
        void shouldReturnSingleValueForOneScore() {
            // Given
            final List<Double> scores = List.of(0.6);

            // When
            final double result = ScoreAggregator.MAX.aggregate(scores);

            // Then
            assertThat(result).isEqualTo(0.6);
        }

        @Test
        @DisplayName("Should have correct name")
        void shouldHaveCorrectName() {
            assertThat(ScoreAggregator.MAX.getName()).isEqualTo("MAX");
        }
    }

    @Nested
    @DisplayName("CONSENSUS Aggregator")
    class ConsensusAggregator {

        @Test
        @DisplayName("Should return average when within tolerance")
        void shouldReturnAverageWhenWithinTolerance() {
            // Given
            final List<Double> scores = List.of(0.79, 0.80, 0.81);
            final ScoreAggregator aggregator = ScoreAggregator.consensus(0.1);

            // When
            final double result = aggregator.aggregate(scores);

            // Then
            assertThat(result).isCloseTo(0.8, offset(0.001));
        }

        @Test
        @DisplayName("Should throw when scores differ beyond tolerance")
        void shouldThrowWhenScoresDifferBeyondTolerance() {
            // Given
            final List<Double> scores = List.of(0.1, 0.9);
            final ScoreAggregator aggregator = ScoreAggregator.consensus(0.1);

            // When/Then
            assertThatThrownBy(() -> aggregator.aggregate(scores))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No consensus")
                    .hasMessageContaining("0.1")
                    .hasMessageContaining("0.9")
                    .hasMessageContaining("tolerance: 0.1");
        }

        @Test
        @DisplayName("Should handle exact agreement")
        void shouldHandleExactAgreement() {
            // Given
            final List<Double> scores = List.of(0.5, 0.5, 0.5);
            final ScoreAggregator aggregator = ScoreAggregator.consensus(0.01);

            // When
            final double result = aggregator.aggregate(scores);

            // Then
            assertThat(result).isEqualTo(0.5);
        }

        @Test
        @DisplayName("Should work with different tolerance values")
        void shouldWorkWithDifferentToleranceValues() {
            // Given
            final List<Double> scores = List.of(0.5, 0.55);

            // When/Then - tolerance too small
            final ScoreAggregator strictAggregator = ScoreAggregator.consensus(0.01);
            assertThatThrownBy(() -> strictAggregator.aggregate(scores)).isInstanceOf(IllegalStateException.class);

            // When/Then - tolerance large enough
            final ScoreAggregator lenientAggregator = ScoreAggregator.consensus(0.1);
            final double result = lenientAggregator.aggregate(scores);
            assertThat(result).isCloseTo(0.525, offset(0.001));
        }

        @Test
        @DisplayName("Should have correct name with tolerance")
        void shouldHaveCorrectNameWithTolerance() {
            // Given
            final ScoreAggregator aggregator = ScoreAggregator.consensus(0.15);

            // When/Then
            assertThat(aggregator.getName()).isEqualTo("CONSENSUS(tolerance=0.15)");
        }

        @Test
        @DisplayName("Should handle single score")
        void shouldHandleSingleScore() {
            // Given
            final List<Double> scores = List.of(0.7);
            final ScoreAggregator aggregator = ScoreAggregator.consensus(0.1);

            // When
            final double result = aggregator.aggregate(scores);

            // Then
            assertThat(result).isEqualTo(0.7);
        }
    }

    @Nested
    @DisplayName("Custom Aggregator")
    class CustomAggregator {

        @Test
        @DisplayName("Should support custom aggregation logic")
        void shouldSupportCustomAggregationLogic() {
            // Given
            final ScoreAggregator customAggregator = new ScoreAggregator() {
                @Override
                public double aggregate(final List<Double> scores) {
                    return scores.stream().mapToDouble(Double::doubleValue).sum();
                }

                @Override
                public String getName() {
                    return "SUM";
                }
            };

            final List<Double> scores = List.of(0.2, 0.3, 0.5);

            // When
            final double result = customAggregator.aggregate(scores);

            // Then
            assertThat(result).isCloseTo(1.0, offset(0.001));
            assertThat(customAggregator.getName()).isEqualTo("SUM");
        }

        @Test
        @DisplayName("Should have default getName implementation")
        void shouldHaveDefaultGetNameImplementation() {
            // Given
            final ScoreAggregator aggregator = scores -> 0.5;

            // When
            final String name = aggregator.getName();

            // Then
            assertThat(name).contains("ScoreAggregatorTest");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle very small scores")
        void shouldHandleVerySmallScores() {
            // Given
            final List<Double> scores = List.of(0.001, 0.002, 0.003);

            // When
            final double avg = ScoreAggregator.AVERAGE.aggregate(scores);
            final double median = ScoreAggregator.MEDIAN.aggregate(scores);
            final double min = ScoreAggregator.MIN.aggregate(scores);
            final double max = ScoreAggregator.MAX.aggregate(scores);

            // Then
            assertThat(avg).isCloseTo(0.002, offset(0.0001));
            assertThat(median).isEqualTo(0.002);
            assertThat(min).isEqualTo(0.001);
            assertThat(max).isEqualTo(0.003);
        }

        @Test
        @DisplayName("Should handle scores close to 1.0")
        void shouldHandleScoresCloseTo1() {
            // Given
            final List<Double> scores = List.of(0.99, 0.999, 1.0);

            // When
            final double avg = ScoreAggregator.AVERAGE.aggregate(scores);
            final double median = ScoreAggregator.MEDIAN.aggregate(scores);
            final double min = ScoreAggregator.MIN.aggregate(scores);
            final double max = ScoreAggregator.MAX.aggregate(scores);

            // Then
            assertThat(avg).isCloseTo(0.996, offset(0.001));
            assertThat(median).isEqualTo(0.999);
            assertThat(min).isEqualTo(0.99);
            assertThat(max).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should handle identical scores")
        void shouldHandleIdenticalScores() {
            // Given
            final List<Double> scores = List.of(0.7, 0.7, 0.7, 0.7);

            // When
            final double avg = ScoreAggregator.AVERAGE.aggregate(scores);
            final double median = ScoreAggregator.MEDIAN.aggregate(scores);
            final double min = ScoreAggregator.MIN.aggregate(scores);
            final double max = ScoreAggregator.MAX.aggregate(scores);

            // Then
            assertThat(avg).isEqualTo(0.7);
            assertThat(median).isEqualTo(0.7);
            assertThat(min).isEqualTo(0.7);
            assertThat(max).isEqualTo(0.7);
        }
    }
}
