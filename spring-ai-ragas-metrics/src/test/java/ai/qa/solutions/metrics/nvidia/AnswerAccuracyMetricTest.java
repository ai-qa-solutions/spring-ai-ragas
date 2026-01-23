package ai.qa.solutions.metrics.nvidia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import ai.qa.solutions.execution.StubMultiModelExecutor;
import ai.qa.solutions.metrics.nvidia.AnswerAccuracyMetric.AccuracyEvaluationResponse;
import ai.qa.solutions.metrics.nvidia.AnswerAccuracyMetric.AnswerAccuracyConfig;
import ai.qa.solutions.metrics.nvidia.AnswerAccuracyMetric.ConfirmationEvaluationResponse;
import ai.qa.solutions.sample.Sample;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AnswerAccuracyMetric")
class AnswerAccuracyMetricTest {

    private StubMultiModelExecutor executor;
    private AnswerAccuracyMetric metric;

    @BeforeEach
    void setUp() {
        executor = new StubMultiModelExecutor(List.of("model1"));
        metric = AnswerAccuracyMetric.builder().executor(executor).build();
    }

    @Nested
    @DisplayName("Accuracy Evaluation")
    class AccuracyEvaluation {

        @Test
        @DisplayName("Should return high score for fully correct response")
        void shouldReturnHighScoreForFullyCorrectResponse() {
            executor = executor.withResponseProvider(
                    AccuracyEvaluationResponse.class,
                    prompt -> new AccuracyEvaluationResponse(2, "Response is fully correct"));

            metric = AnswerAccuracyMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .response("Paris is the capital of France")
                    .reference("Paris is the capital of France")
                    .build();

            final AnswerAccuracyConfig config = AnswerAccuracyConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isCloseTo(1.0, within(0.01));
        }

        @Test
        @DisplayName("Should return moderate score for partially correct response")
        void shouldReturnModerateScoreForPartiallyCorrectResponse() {
            executor = executor.withResponseProvider(
                    AccuracyEvaluationResponse.class,
                    prompt -> new AccuracyEvaluationResponse(1, "Response is partially correct"));

            metric = AnswerAccuracyMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .response("Paris is a city in France")
                    .reference("Paris is the capital of France")
                    .build();

            final AnswerAccuracyConfig config = AnswerAccuracyConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isCloseTo(0.5, within(0.01));
        }

        @Test
        @DisplayName("Should return low score for incorrect response")
        void shouldReturnLowScoreForIncorrectResponse() {
            executor = executor.withResponseProvider(
                    AccuracyEvaluationResponse.class,
                    prompt -> new AccuracyEvaluationResponse(0, "Response is incorrect"));

            metric = AnswerAccuracyMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .response("London is the capital of France")
                    .reference("Paris is the capital of France")
                    .build();

            final AnswerAccuracyConfig config = AnswerAccuracyConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isCloseTo(0.0, within(0.01));
        }
    }

    @Nested
    @DisplayName("Dual Judge Mode")
    class DualJudgeMode {

        @Test
        @DisplayName("Should use dual judge when enabled")
        void shouldUseDualJudgeWhenEnabled() {
            executor = executor.withResponseProvider(
                            AccuracyEvaluationResponse.class,
                            prompt -> new AccuracyEvaluationResponse(1, "Initial assessment"))
                    .withResponseProvider(
                            ConfirmationEvaluationResponse.class,
                            prompt -> new ConfirmationEvaluationResponse(2, "Confirmed as fully correct", true));

            metric = AnswerAccuracyMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .response("Paris is the capital of France")
                    .reference("Paris is the capital of France")
                    .build();

            final AnswerAccuracyConfig config =
                    AnswerAccuracyConfig.builder().useDualJudge(true).build();

            final Double score = metric.singleTurnScore(config, sample);

            // Should use confirmation score (2/2 = 1.0)
            assertThat(score).isCloseTo(1.0, within(0.01));
        }

        @Test
        @DisplayName("Should skip dual judge when disabled")
        void shouldSkipDualJudgeWhenDisabled() {
            executor = executor.withResponseProvider(
                            AccuracyEvaluationResponse.class,
                            prompt -> new AccuracyEvaluationResponse(1, "Initial assessment"))
                    .withResponseProvider(ConfirmationEvaluationResponse.class, prompt -> {
                        throw new RuntimeException("Should not be called");
                    });

            metric = AnswerAccuracyMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .response("Response text")
                    .reference("Reference text")
                    .build();

            final AnswerAccuracyConfig config =
                    AnswerAccuracyConfig.builder().useDualJudge(false).build();

            // Should not throw because dual judge is disabled
            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isCloseTo(0.5, within(0.01));
        }

        @Test
        @DisplayName("Should fall back to initial score if confirmation fails")
        void shouldFallBackToInitialScoreIfConfirmationFails() {
            executor = executor.withResponseProvider(
                            AccuracyEvaluationResponse.class,
                            prompt -> new AccuracyEvaluationResponse(2, "Initial score"))
                    .withResponseProvider(
                            ConfirmationEvaluationResponse.class,
                            prompt -> new ConfirmationEvaluationResponse(null, null, null));

            metric = AnswerAccuracyMetric.builder().executor(executor).build();

            final Sample sample =
                    Sample.builder().response("Response").reference("Reference").build();

            final AnswerAccuracyConfig config =
                    AnswerAccuracyConfig.builder().useDualJudge(true).build();

            // Should fall back to initial score (2/2 = 1.0)
            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isCloseTo(1.0, within(0.01));
        }
    }

    @Nested
    @DisplayName("Score Normalization")
    class ScoreNormalization {

        @Test
        @DisplayName("Should normalize score 0 to 0.0")
        void shouldNormalizeScoreZero() {
            executor = executor.withResponseProvider(
                    AccuracyEvaluationResponse.class, prompt -> new AccuracyEvaluationResponse(0, "Incorrect"));

            metric = AnswerAccuracyMetric.builder().executor(executor).build();

            final Sample sample =
                    Sample.builder().response("Response").reference("Reference").build();

            final AnswerAccuracyConfig config = AnswerAccuracyConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should normalize score 1 to 0.5")
        void shouldNormalizeScoreOne() {
            executor = executor.withResponseProvider(
                    AccuracyEvaluationResponse.class, prompt -> new AccuracyEvaluationResponse(1, "Partially correct"));

            metric = AnswerAccuracyMetric.builder().executor(executor).build();

            final Sample sample =
                    Sample.builder().response("Response").reference("Reference").build();

            final AnswerAccuracyConfig config = AnswerAccuracyConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isCloseTo(0.5, within(0.01));
        }

        @Test
        @DisplayName("Should normalize score 2 to 1.0")
        void shouldNormalizeScoreTwo() {
            executor = executor.withResponseProvider(
                    AccuracyEvaluationResponse.class, prompt -> new AccuracyEvaluationResponse(2, "Fully correct"));

            metric = AnswerAccuracyMetric.builder().executor(executor).build();

            final Sample sample =
                    Sample.builder().response("Response").reference("Reference").build();

            final AnswerAccuracyConfig config = AnswerAccuracyConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isCloseTo(1.0, within(0.01));
        }

        @Test
        @DisplayName("Should clamp scores above 2 to 1.0")
        void shouldClampHighScores() {
            executor = executor.withResponseProvider(
                    AccuracyEvaluationResponse.class,
                    prompt -> new AccuracyEvaluationResponse(5, "Invalid high score"));

            metric = AnswerAccuracyMetric.builder().executor(executor).build();

            final Sample sample =
                    Sample.builder().response("Response").reference("Reference").build();

            final AnswerAccuracyConfig config = AnswerAccuracyConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should clamp negative scores to 0.0")
        void shouldClampNegativeScores() {
            executor = executor.withResponseProvider(
                    AccuracyEvaluationResponse.class,
                    prompt -> new AccuracyEvaluationResponse(-1, "Invalid negative score"));

            metric = AnswerAccuracyMetric.builder().executor(executor).build();

            final Sample sample =
                    Sample.builder().response("Response").reference("Reference").build();

            final AnswerAccuracyConfig config = AnswerAccuracyConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Multi-Model Scoring")
    class MultiModelScoring {

        @Test
        @DisplayName("Should average scores across models")
        void shouldAverageScoresAcrossModels() {
            executor = new StubMultiModelExecutor(List.of("model1", "model2"))
                    .withResponseProvider(
                            AccuracyEvaluationResponse.class, prompt -> new AccuracyEvaluationResponse(2, "Correct"));

            metric = AnswerAccuracyMetric.builder().executor(executor).build();

            final Sample sample =
                    Sample.builder().response("Response").reference("Reference").build();

            final AnswerAccuracyConfig config = AnswerAccuracyConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isCloseTo(1.0, within(0.01));
        }
    }

    @Nested
    @DisplayName("Input Validation")
    class InputValidation {

        @Test
        @DisplayName("Should return null for null response")
        void shouldReturnNullForNullResponse() {
            final Sample sample =
                    Sample.builder().response(null).reference("Reference").build();

            final AnswerAccuracyConfig config = AnswerAccuracyConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isNull();
        }

        @Test
        @DisplayName("Should return null for empty response")
        void shouldReturnNullForEmptyResponse() {
            final Sample sample =
                    Sample.builder().response("").reference("Reference").build();

            final AnswerAccuracyConfig config = AnswerAccuracyConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isNull();
        }

        @Test
        @DisplayName("Should return null for null reference")
        void shouldReturnNullForNullReference() {
            final Sample sample =
                    Sample.builder().response("Response").reference(null).build();

            final AnswerAccuracyConfig config = AnswerAccuracyConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isNull();
        }

        @Test
        @DisplayName("Should return null for empty reference")
        void shouldReturnNullForEmptyReference() {
            final Sample sample =
                    Sample.builder().response("Response").reference("").build();

            final AnswerAccuracyConfig config = AnswerAccuracyConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isNull();
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should throw when all models fail")
        void shouldThrowWhenAllModelsFail() {
            executor = executor.withResponseProvider(
                    AccuracyEvaluationResponse.class, prompt -> new AccuracyEvaluationResponse(null, null));

            metric = AnswerAccuracyMetric.builder().executor(executor).build();

            final Sample sample =
                    Sample.builder().response("Response").reference("Reference").build();

            final AnswerAccuracyConfig config = AnswerAccuracyConfig.builder().build();

            assertThatThrownBy(() -> metric.singleTurnScore(config, sample))
                    .hasRootCauseInstanceOf(IllegalStateException.class)
                    .hasRootCauseMessage("All models failed in metric: AnswerAccuracyMetric");
        }
    }

    @Nested
    @DisplayName("Async Execution")
    class AsyncExecution {

        @Test
        @DisplayName("Should execute asynchronously")
        void shouldExecuteAsynchronously() {
            executor = executor.withResponseProvider(
                    AccuracyEvaluationResponse.class, prompt -> new AccuracyEvaluationResponse(2, "Correct"));

            metric = AnswerAccuracyMetric.builder().executor(executor).build();

            final Sample sample =
                    Sample.builder().response("Response").reference("Reference").build();

            final AnswerAccuracyConfig config = AnswerAccuracyConfig.builder().build();

            final CompletableFuture<Double> future = metric.singleTurnScoreAsync(config, sample);

            assertThat(future).isNotNull();
            assertThat(future.join()).isCloseTo(1.0, within(0.01));
        }
    }

    @Nested
    @DisplayName("Configuration")
    class Configuration {

        @Test
        @DisplayName("Should use default config values")
        void shouldUseDefaultConfigValues() {
            final AnswerAccuracyConfig config = AnswerAccuracyConfig.builder().build();

            assertThat(config.isUseDualJudge()).isFalse();
            assertThat(config.getTemperature()).isEqualTo(0.1);
        }

        @Test
        @DisplayName("Should allow custom config values")
        void shouldAllowCustomConfigValues() {
            final AnswerAccuracyConfig config = AnswerAccuracyConfig.builder()
                    .useDualJudge(true)
                    .temperature(0.5)
                    .models(List.of("custom-model"))
                    .build();

            assertThat(config.isUseDualJudge()).isTrue();
            assertThat(config.getTemperature()).isEqualTo(0.5);
            assertThat(config.getModels()).containsExactly("custom-model");
        }
    }
}
