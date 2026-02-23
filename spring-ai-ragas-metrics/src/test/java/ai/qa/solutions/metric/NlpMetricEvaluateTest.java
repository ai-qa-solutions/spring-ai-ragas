package ai.qa.solutions.metric;

import static org.assertj.core.api.Assertions.assertThat;

import ai.qa.solutions.metric.explanation.BleuScoreExplanation;
import ai.qa.solutions.metrics.nlp.BleuScoreMetric;
import ai.qa.solutions.sample.Sample;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("NLP Metric Evaluate API Tests")
class NlpMetricEvaluateTest {

    private BleuScoreMetric metric;

    @BeforeEach
    void setUp() {
        metric = new BleuScoreMetric();
    }

    @Nested
    @DisplayName("BleuScoreMetric singleTurnEvaluate")
    class BleuScoreSingleTurnEvaluate {

        @Test
        @DisplayName("Should return result with score between 0 and 1")
        void shouldReturnResultWithScore() {
            final var config = BleuScoreMetric.BleuScoreConfig.builder().build();
            final var sample = Sample.builder()
                    .response("The cat sat on the mat")
                    .reference("The cat sat on the mat")
                    .build();

            final EvaluationResult result = metric.singleTurnEvaluate(config, sample);

            assertThat(result.getScore()).isNotNull();
            assertThat(result.getScore()).isBetween(0.0, 1.0);
        }

        @Test
        @DisplayName("Should return result with explanation")
        void shouldReturnResultWithExplanation() {
            final var config = BleuScoreMetric.BleuScoreConfig.builder().build();
            final var sample = Sample.builder()
                    .response("The cat sat on the mat")
                    .reference("The cat sat on the mat")
                    .build();

            final EvaluationResult result = metric.singleTurnEvaluate(config, sample);

            assertThat(result.getExplanation()).isNotNull();
            assertThat(result.getExplanation()).isInstanceOf(BleuScoreExplanation.class);
        }

        @Test
        @DisplayName("Should return valid result with Russian language")
        void shouldReturnValidResultWithRussianLanguage() {
            final var config =
                    BleuScoreMetric.BleuScoreConfig.builder().language("ru").build();
            final var sample = Sample.builder()
                    .response("The cat sat on the mat")
                    .reference("The cat sat on the mat")
                    .build();

            final EvaluationResult result = metric.singleTurnEvaluate(config, sample);

            assertThat(result).isNotNull();
            assertThat(result.getScore()).isNotNull();
            assertThat(result.getScore()).isBetween(0.0, 1.0);
        }

        @Test
        @DisplayName("Should return 1.0 for identical texts")
        void shouldReturnPerfectScoreForIdenticalTexts() {
            final var config = BleuScoreMetric.BleuScoreConfig.builder().build();
            final var sample = Sample.builder()
                    .response("The cat sat on the mat")
                    .reference("The cat sat on the mat")
                    .build();

            final EvaluationResult result = metric.singleTurnEvaluate(config, sample);

            assertThat(result.getScore()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should return metricName in result")
        void shouldReturnMetricNameInResult() {
            final var config = BleuScoreMetric.BleuScoreConfig.builder().build();
            final var sample = Sample.builder()
                    .response("The cat sat on the mat")
                    .reference("The cat sat on the mat")
                    .build();

            final EvaluationResult result = metric.singleTurnEvaluate(config, sample);

            assertThat(result.getMetricName()).isEqualTo("BleuScoreMetric");
        }

        @Test
        @DisplayName("Should return sample in result")
        void shouldReturnSampleInResult() {
            final var config = BleuScoreMetric.BleuScoreConfig.builder().build();
            final var sample = Sample.builder()
                    .response("The cat sat on the mat")
                    .reference("The cat sat on the mat")
                    .build();

            final EvaluationResult result = metric.singleTurnEvaluate(config, sample);

            assertThat(result.getSample()).isSameAs(sample);
        }

        @Test
        @DisplayName("Should return empty modelIds for NLP metric")
        void shouldReturnEmptyModelIdsForNlpMetric() {
            final var config = BleuScoreMetric.BleuScoreConfig.builder().build();
            final var sample = Sample.builder()
                    .response("The cat sat on the mat")
                    .reference("The cat sat on the mat")
                    .build();

            final EvaluationResult result = metric.singleTurnEvaluate(config, sample);

            assertThat(result.getModelIds()).isEmpty();
        }
    }
}
