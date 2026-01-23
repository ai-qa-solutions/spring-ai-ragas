package ai.qa.solutions.metrics.nvidia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import ai.qa.solutions.execution.StubMultiModelExecutor;
import ai.qa.solutions.metrics.nvidia.ResponseGroundednessMetric.GroundednessEvaluationResponse;
import ai.qa.solutions.metrics.nvidia.ResponseGroundednessMetric.ResponseGroundednessConfig;
import ai.qa.solutions.sample.Sample;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ResponseGroundednessMetric")
class ResponseGroundednessMetricTest {

    private StubMultiModelExecutor executor;
    private ResponseGroundednessMetric metric;

    @BeforeEach
    void setUp() {
        executor = new StubMultiModelExecutor(List.of("model1"));
        metric = ResponseGroundednessMetric.builder().executor(executor).build();
    }

    @Nested
    @DisplayName("Groundedness Evaluation")
    class GroundednessEvaluation {

        @Test
        @DisplayName("Should return high score for fully grounded response")
        void shouldReturnHighScoreForFullyGroundedResponse() {
            executor = executor.withResponseProvider(
                    GroundednessEvaluationResponse.class,
                    prompt -> new GroundednessEvaluationResponse(2, "Response is fully supported by context"));

            metric = ResponseGroundednessMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .response("Paris is the capital of France")
                    .retrievedContexts(List.of("France is a country in Europe. Paris is the capital of France."))
                    .build();

            final ResponseGroundednessConfig config = ResponseGroundednessConfig.builder()
                    .useHeuristicShortcuts(false)
                    .build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isCloseTo(1.0, within(0.01));
        }

        @Test
        @DisplayName("Should return moderate score for partially grounded response")
        void shouldReturnModerateScoreForPartiallyGroundedResponse() {
            executor = executor.withResponseProvider(
                    GroundednessEvaluationResponse.class,
                    prompt -> new GroundednessEvaluationResponse(1, "Response is partially supported"));

            metric = ResponseGroundednessMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .response("Paris is the beautiful capital of France with amazing food")
                    .retrievedContexts(List.of("Paris is the capital of France."))
                    .build();

            final ResponseGroundednessConfig config = ResponseGroundednessConfig.builder()
                    .useHeuristicShortcuts(false)
                    .build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isCloseTo(0.5, within(0.01));
        }

        @Test
        @DisplayName("Should return low score for ungrounded response")
        void shouldReturnLowScoreForUngroundedResponse() {
            executor = executor.withResponseProvider(
                    GroundednessEvaluationResponse.class,
                    prompt -> new GroundednessEvaluationResponse(0, "Response not supported by context"));

            metric = ResponseGroundednessMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .response("Berlin is the capital of Germany")
                    .retrievedContexts(List.of("Paris is the capital of France."))
                    .build();

            final ResponseGroundednessConfig config = ResponseGroundednessConfig.builder()
                    .useHeuristicShortcuts(false)
                    .build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isCloseTo(0.0, within(0.01));
        }
    }

    @Nested
    @DisplayName("Heuristic Shortcuts")
    class HeuristicShortcuts {

        @Test
        @DisplayName("Should return 1.0 when response exactly matches context")
        void shouldReturnOneForExactMatch() {
            final Sample sample = Sample.builder()
                    .response("Paris is the capital of France")
                    .retrievedContexts(List.of("Paris is the capital of France"))
                    .build();

            final ResponseGroundednessConfig config = ResponseGroundednessConfig.builder()
                    .useHeuristicShortcuts(true)
                    .build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should return 1.0 when response is contained in context")
        void shouldReturnOneWhenResponseContainedInContext() {
            final Sample sample = Sample.builder()
                    .response("capital")
                    .retrievedContexts(List.of("Paris is the capital of France"))
                    .build();

            final ResponseGroundednessConfig config = ResponseGroundednessConfig.builder()
                    .useHeuristicShortcuts(true)
                    .build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should return 0.0 for empty response when heuristics enabled")
        void shouldReturnZeroForEmptyResponse() {
            final Sample sample = Sample.builder()
                    .response("")
                    .retrievedContexts(List.of("Some context"))
                    .build();

            final ResponseGroundednessConfig config = ResponseGroundednessConfig.builder()
                    .useHeuristicShortcuts(true)
                    .build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should skip LLM when heuristic shortcut applies")
        void shouldSkipLlmWhenHeuristicApplies() {
            // Set up executor to fail if called - heuristic should prevent LLM call
            executor = executor.withResponseProvider(GroundednessEvaluationResponse.class, prompt -> {
                throw new RuntimeException("LLM should not be called");
            });

            metric = ResponseGroundednessMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .response("Exact match text")
                    .retrievedContexts(List.of("Exact match text"))
                    .build();

            final ResponseGroundednessConfig config = ResponseGroundednessConfig.builder()
                    .useHeuristicShortcuts(true)
                    .build();

            // Should not throw because heuristic shortcuts to 1.0
            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should use LLM when heuristics disabled")
        void shouldUseLlmWhenHeuristicsDisabled() {
            executor = executor.withResponseProvider(
                    GroundednessEvaluationResponse.class,
                    prompt -> new GroundednessEvaluationResponse(2, "Fully grounded"));

            metric = ResponseGroundednessMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .response("Exact match text")
                    .retrievedContexts(List.of("Exact match text"))
                    .build();

            final ResponseGroundednessConfig config = ResponseGroundednessConfig.builder()
                    .useHeuristicShortcuts(false)
                    .build();

            final Double score = metric.singleTurnScore(config, sample);

            // LLM was called and returned score 2 → normalized to 1.0
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
                    GroundednessEvaluationResponse.class,
                    prompt -> new GroundednessEvaluationResponse(0, "Not grounded"));

            metric = ResponseGroundednessMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .response("Some response")
                    .retrievedContexts(List.of("Different context"))
                    .build();

            final ResponseGroundednessConfig config = ResponseGroundednessConfig.builder()
                    .useHeuristicShortcuts(false)
                    .build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should normalize score 1 to 0.5")
        void shouldNormalizeScoreOne() {
            executor = executor.withResponseProvider(
                    GroundednessEvaluationResponse.class,
                    prompt -> new GroundednessEvaluationResponse(1, "Partially grounded"));

            metric = ResponseGroundednessMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .response("Some response")
                    .retrievedContexts(List.of("Some context"))
                    .build();

            final ResponseGroundednessConfig config = ResponseGroundednessConfig.builder()
                    .useHeuristicShortcuts(false)
                    .build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isCloseTo(0.5, within(0.01));
        }

        @Test
        @DisplayName("Should normalize score 2 to 1.0")
        void shouldNormalizeScoreTwo() {
            executor = executor.withResponseProvider(
                    GroundednessEvaluationResponse.class,
                    prompt -> new GroundednessEvaluationResponse(2, "Fully grounded"));

            metric = ResponseGroundednessMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .response("Some response")
                    .retrievedContexts(List.of("Some context"))
                    .build();

            final ResponseGroundednessConfig config = ResponseGroundednessConfig.builder()
                    .useHeuristicShortcuts(false)
                    .build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isCloseTo(1.0, within(0.01));
        }

        @Test
        @DisplayName("Should clamp scores above 2 to 1.0")
        void shouldClampHighScores() {
            executor = executor.withResponseProvider(
                    GroundednessEvaluationResponse.class,
                    prompt -> new GroundednessEvaluationResponse(5, "Invalid high score"));

            metric = ResponseGroundednessMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .response("Response")
                    .retrievedContexts(List.of("Context"))
                    .build();

            final ResponseGroundednessConfig config = ResponseGroundednessConfig.builder()
                    .useHeuristicShortcuts(false)
                    .build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should clamp negative scores to 0.0")
        void shouldClampNegativeScores() {
            executor = executor.withResponseProvider(
                    GroundednessEvaluationResponse.class,
                    prompt -> new GroundednessEvaluationResponse(-1, "Invalid negative score"));

            metric = ResponseGroundednessMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .response("Response")
                    .retrievedContexts(List.of("Context"))
                    .build();

            final ResponseGroundednessConfig config = ResponseGroundednessConfig.builder()
                    .useHeuristicShortcuts(false)
                    .build();

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
                    .withResponseProvider(GroundednessEvaluationResponse.class, prompt -> {
                        // Return different scores based on model call order
                        return new GroundednessEvaluationResponse(2, "Grounded");
                    });

            metric = ResponseGroundednessMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .response("Response text")
                    .retrievedContexts(List.of("Context text"))
                    .build();

            final ResponseGroundednessConfig config = ResponseGroundednessConfig.builder()
                    .useHeuristicShortcuts(false)
                    .build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isCloseTo(1.0, within(0.01));
        }
    }

    @Nested
    @DisplayName("Input Validation")
    class InputValidation {

        @Test
        @DisplayName("Should return 0.0 for null response with heuristics")
        void shouldReturnZeroForNullResponseWithHeuristics() {
            final Sample sample = Sample.builder()
                    .response(null)
                    .retrievedContexts(List.of("Context"))
                    .build();

            final ResponseGroundednessConfig config = ResponseGroundednessConfig.builder()
                    .useHeuristicShortcuts(true)
                    .build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return null for null response without heuristics")
        void shouldReturnNullForNullResponseWithoutHeuristics() {
            final Sample sample = Sample.builder()
                    .response(null)
                    .retrievedContexts(List.of("Context"))
                    .build();

            final ResponseGroundednessConfig config = ResponseGroundednessConfig.builder()
                    .useHeuristicShortcuts(false)
                    .build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isNull();
        }

        @Test
        @DisplayName("Should return null for empty contexts")
        void shouldReturnNullForEmptyContexts() {
            final Sample sample = Sample.builder()
                    .response("Response")
                    .retrievedContexts(List.of())
                    .build();

            final ResponseGroundednessConfig config =
                    ResponseGroundednessConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isNull();
        }

        @Test
        @DisplayName("Should return null for null contexts")
        void shouldReturnNullForNullContexts() {
            final Sample sample = Sample.builder()
                    .response("Response")
                    .retrievedContexts(null)
                    .build();

            final ResponseGroundednessConfig config =
                    ResponseGroundednessConfig.builder().build();

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
                    GroundednessEvaluationResponse.class, prompt -> new GroundednessEvaluationResponse(null, null));

            metric = ResponseGroundednessMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .response("Response")
                    .retrievedContexts(List.of("Context"))
                    .build();

            final ResponseGroundednessConfig config = ResponseGroundednessConfig.builder()
                    .useHeuristicShortcuts(false)
                    .build();

            assertThatThrownBy(() -> metric.singleTurnScore(config, sample))
                    .hasRootCauseInstanceOf(IllegalStateException.class)
                    .hasRootCauseMessage("All models failed in metric: ResponseGroundednessMetric");
        }
    }

    @Nested
    @DisplayName("Async Execution")
    class AsyncExecution {

        @Test
        @DisplayName("Should execute asynchronously")
        void shouldExecuteAsynchronously() {
            executor = executor.withResponseProvider(
                    GroundednessEvaluationResponse.class, prompt -> new GroundednessEvaluationResponse(2, "Grounded"));

            metric = ResponseGroundednessMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .response("Response")
                    .retrievedContexts(List.of("Context"))
                    .build();

            final ResponseGroundednessConfig config = ResponseGroundednessConfig.builder()
                    .useHeuristicShortcuts(false)
                    .build();

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
            final ResponseGroundednessConfig config =
                    ResponseGroundednessConfig.builder().build();

            assertThat(config.isUseHeuristicShortcuts()).isTrue();
            assertThat(config.getTemperature()).isEqualTo(0.1);
        }

        @Test
        @DisplayName("Should allow custom config values")
        void shouldAllowCustomConfigValues() {
            final ResponseGroundednessConfig config = ResponseGroundednessConfig.builder()
                    .useHeuristicShortcuts(false)
                    .temperature(0.5)
                    .models(List.of("custom-model"))
                    .build();

            assertThat(config.isUseHeuristicShortcuts()).isFalse();
            assertThat(config.getTemperature()).isEqualTo(0.5);
            assertThat(config.getModels()).containsExactly("custom-model");
        }
    }
}
