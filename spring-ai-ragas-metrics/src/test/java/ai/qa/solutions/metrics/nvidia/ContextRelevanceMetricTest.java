package ai.qa.solutions.metrics.nvidia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.qa.solutions.execution.StubMultiModelExecutor;
import ai.qa.solutions.metrics.nvidia.ContextRelevanceMetric.ContextRelevanceConfig;
import ai.qa.solutions.metrics.nvidia.ContextRelevanceMetric.RelevanceEvaluationResponse;
import ai.qa.solutions.sample.Sample;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ContextRelevanceMetric")
class ContextRelevanceMetricTest {

    private StubMultiModelExecutor executor;
    private ContextRelevanceMetric metric;

    @BeforeEach
    void setUp() {
        executor = new StubMultiModelExecutor(List.of("model1"));
    }

    @Nested
    @DisplayName("Full Relevance Evaluation")
    class FullRelevanceTests {

        @Test
        @DisplayName("Should return 1.0 for fully relevant context (score 2)")
        void shouldReturnOneForFullyRelevantContext() {
            executor = executor.withResponseProvider(
                    RelevanceEvaluationResponse.class,
                    prompt -> new RelevanceEvaluationResponse(2, "Context fully addresses the question"));

            metric = ContextRelevanceMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInput("What is machine learning?")
                    .retrievedContexts(List.of("Machine learning is a subset of AI that enables systems to learn."))
                    .build();

            final Double score =
                    metric.singleTurnScore(ContextRelevanceConfig.builder().build(), sample);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should return 0.5 for partially relevant context (score 1)")
        void shouldReturnHalfForPartiallyRelevantContext() {
            executor = executor.withResponseProvider(
                    RelevanceEvaluationResponse.class,
                    prompt -> new RelevanceEvaluationResponse(1, "Context contains some relevant information"));

            metric = ContextRelevanceMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInput("What is machine learning?")
                    .retrievedContexts(List.of("AI is used in many applications."))
                    .build();

            final Double score =
                    metric.singleTurnScore(ContextRelevanceConfig.builder().build(), sample);

            assertThat(score).isEqualTo(0.5);
        }

        @Test
        @DisplayName("Should return 0.0 for not relevant context (score 0)")
        void shouldReturnZeroForNotRelevantContext() {
            executor = executor.withResponseProvider(
                    RelevanceEvaluationResponse.class,
                    prompt -> new RelevanceEvaluationResponse(0, "Context does not address the question"));

            metric = ContextRelevanceMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInput("What is machine learning?")
                    .retrievedContexts(List.of("The weather is nice today."))
                    .build();

            final Double score =
                    metric.singleTurnScore(ContextRelevanceConfig.builder().build(), sample);

            assertThat(score).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Multiple Contexts Evaluation")
    class MultipleContextsTests {

        @Test
        @DisplayName("Should average scores across multiple contexts")
        void shouldAverageScoresAcrossMultipleContexts() {
            // Return different scores for different contexts based on prompt content
            executor = executor.withResponseProvider(RelevanceEvaluationResponse.class, prompt -> {
                if (prompt.contains("context 1")) {
                    return new RelevanceEvaluationResponse(2, "Fully relevant");
                } else if (prompt.contains("context 2")) {
                    return new RelevanceEvaluationResponse(1, "Partially relevant");
                } else {
                    return new RelevanceEvaluationResponse(0, "Not relevant");
                }
            });

            metric = ContextRelevanceMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInput("What is AI?")
                    .retrievedContexts(
                            List.of("context 1: AI definition", "context 2: some info", "context 3: unrelated"))
                    .build();

            final Double score =
                    metric.singleTurnScore(ContextRelevanceConfig.builder().build(), sample);

            // (1.0 + 0.5 + 0.0) / 3 = 0.5
            assertThat(score).isEqualTo(0.5);
        }

        @Test
        @DisplayName("Should handle all contexts being fully relevant")
        void shouldHandleAllContextsFullyRelevant() {
            executor = executor.withResponseProvider(
                    RelevanceEvaluationResponse.class, prompt -> new RelevanceEvaluationResponse(2, "Fully relevant"));

            metric = ContextRelevanceMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInput("What is AI?")
                    .retrievedContexts(List.of("context 1", "context 2", "context 3"))
                    .build();

            final Double score =
                    metric.singleTurnScore(ContextRelevanceConfig.builder().build(), sample);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should handle all contexts being not relevant")
        void shouldHandleAllContextsNotRelevant() {
            executor = executor.withResponseProvider(
                    RelevanceEvaluationResponse.class, prompt -> new RelevanceEvaluationResponse(0, "Not relevant"));

            metric = ContextRelevanceMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInput("What is AI?")
                    .retrievedContexts(List.of("context 1", "context 2"))
                    .build();

            final Double score =
                    metric.singleTurnScore(ContextRelevanceConfig.builder().build(), sample);

            assertThat(score).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Score Normalization")
    class ScoreNormalizationTests {

        @Test
        @DisplayName("Should clamp scores above maximum to 1.0")
        void shouldClampScoresAboveMaximum() {
            executor = executor.withResponseProvider(
                    RelevanceEvaluationResponse.class,
                    prompt -> new RelevanceEvaluationResponse(3, "Score above range"));

            metric = ContextRelevanceMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInput("Question")
                    .retrievedContexts(List.of("Context"))
                    .build();

            final Double score =
                    metric.singleTurnScore(ContextRelevanceConfig.builder().build(), sample);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should clamp negative scores to 0.0")
        void shouldClampNegativeScores() {
            executor = executor.withResponseProvider(
                    RelevanceEvaluationResponse.class, prompt -> new RelevanceEvaluationResponse(-1, "Negative score"));

            metric = ContextRelevanceMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInput("Question")
                    .retrievedContexts(List.of("Context"))
                    .build();

            final Double score =
                    metric.singleTurnScore(ContextRelevanceConfig.builder().build(), sample);

            assertThat(score).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Input Validation")
    class InputValidationTests {

        @Test
        @DisplayName("Should return null for missing user input")
        void shouldReturnNullForMissingUserInput() {
            executor = executor.withResponseProvider(
                    RelevanceEvaluationResponse.class, prompt -> new RelevanceEvaluationResponse(2, "Relevant"));

            metric = ContextRelevanceMetric.builder().executor(executor).build();

            final Sample sample =
                    Sample.builder().retrievedContexts(List.of("Some context")).build();

            final Double score =
                    metric.singleTurnScore(ContextRelevanceConfig.builder().build(), sample);

            assertThat(score).isNull();
        }

        @Test
        @DisplayName("Should return null for empty user input")
        void shouldReturnNullForEmptyUserInput() {
            executor = executor.withResponseProvider(
                    RelevanceEvaluationResponse.class, prompt -> new RelevanceEvaluationResponse(2, "Relevant"));

            metric = ContextRelevanceMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInput("")
                    .retrievedContexts(List.of("Some context"))
                    .build();

            final Double score =
                    metric.singleTurnScore(ContextRelevanceConfig.builder().build(), sample);

            assertThat(score).isNull();
        }

        @Test
        @DisplayName("Should return null for missing retrieved contexts")
        void shouldReturnNullForMissingContexts() {
            executor = executor.withResponseProvider(
                    RelevanceEvaluationResponse.class, prompt -> new RelevanceEvaluationResponse(2, "Relevant"));

            metric = ContextRelevanceMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder().userInput("What is AI?").build();

            final Double score =
                    metric.singleTurnScore(ContextRelevanceConfig.builder().build(), sample);

            assertThat(score).isNull();
        }

        @Test
        @DisplayName("Should return null for empty retrieved contexts")
        void shouldReturnNullForEmptyContexts() {
            executor = executor.withResponseProvider(
                    RelevanceEvaluationResponse.class, prompt -> new RelevanceEvaluationResponse(2, "Relevant"));

            metric = ContextRelevanceMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInput("What is AI?")
                    .retrievedContexts(List.of())
                    .build();

            final Double score =
                    metric.singleTurnScore(ContextRelevanceConfig.builder().build(), sample);

            assertThat(score).isNull();
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should throw when all models fail")
        void shouldThrowWhenAllModelsFail() {
            executor = executor.withResponseProvider(
                    RelevanceEvaluationResponse.class, prompt -> new RelevanceEvaluationResponse(null, null));

            metric = ContextRelevanceMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInput("What is AI?")
                    .retrievedContexts(List.of("Context"))
                    .build();

            assertThatThrownBy(() -> metric.singleTurnScore(
                            ContextRelevanceConfig.builder().build(), sample))
                    .hasCauseInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("All models failed");
        }
    }

    @Nested
    @DisplayName("Builder and Config")
    class BuilderTests {

        @Test
        @DisplayName("Should create metric with custom prompt")
        void shouldCreateWithCustomPrompt() {
            final String customPrompt = "Custom relevance prompt: {userInput} - {context}";

            executor = executor.withResponseProvider(
                    RelevanceEvaluationResponse.class, prompt -> new RelevanceEvaluationResponse(2, "Relevant"));

            metric = ContextRelevanceMetric.builder()
                    .executor(executor)
                    .evaluateRelevancePrompt(customPrompt)
                    .build();

            final Sample sample = Sample.builder()
                    .userInput("Test question")
                    .retrievedContexts(List.of("Test context"))
                    .build();

            final Double score =
                    metric.singleTurnScore(ContextRelevanceConfig.builder().build(), sample);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should use default temperature in config")
        void shouldUseDefaultTemperature() {
            final ContextRelevanceConfig config =
                    ContextRelevanceConfig.builder().build();

            assertThat(config.getTemperature()).isEqualTo(0.1);
        }

        @Test
        @DisplayName("Should allow custom temperature in config")
        void shouldAllowCustomTemperature() {
            final ContextRelevanceConfig config =
                    ContextRelevanceConfig.builder().temperature(0.5).build();

            assertThat(config.getTemperature()).isEqualTo(0.5);
        }

        @Test
        @DisplayName("Should allow specifying models in config")
        void shouldAllowSpecifyingModels() {
            final ContextRelevanceConfig config = ContextRelevanceConfig.builder()
                    .model("gpt-4")
                    .model("claude-3")
                    .build();

            assertThat(config.getModels()).containsExactly("gpt-4", "claude-3");
        }
    }

    @Nested
    @DisplayName("Async Execution")
    class AsyncTests {

        @Test
        @DisplayName("Should work correctly with async execution")
        void shouldWorkWithAsyncExecution() {
            executor = executor.withResponseProvider(
                    RelevanceEvaluationResponse.class, prompt -> new RelevanceEvaluationResponse(2, "Fully relevant"));

            metric = ContextRelevanceMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInput("What is AI?")
                    .retrievedContexts(List.of("AI is artificial intelligence"))
                    .build();

            final Double score = metric.singleTurnScoreAsync(
                            ContextRelevanceConfig.builder().build(), sample)
                    .join();

            assertThat(score).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("Multi-Model Aggregation")
    class MultiModelTests {

        @Test
        @DisplayName("Should average scores across multiple models for single context")
        void shouldAverageAcrossModelsForSingleContext() {
            executor = new StubMultiModelExecutor(List.of("model1", "model2"));
            // Both models will return the same response in this test
            executor = executor.withResponseProvider(
                    RelevanceEvaluationResponse.class, prompt -> new RelevanceEvaluationResponse(2, "Relevant"));

            metric = ContextRelevanceMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInput("What is AI?")
                    .retrievedContexts(List.of("AI context"))
                    .build();

            final Double score =
                    metric.singleTurnScore(ContextRelevanceConfig.builder().build(), sample);

            // Both models return 2, normalized to 1.0, averaged = 1.0
            assertThat(score).isEqualTo(1.0);
        }
    }
}
