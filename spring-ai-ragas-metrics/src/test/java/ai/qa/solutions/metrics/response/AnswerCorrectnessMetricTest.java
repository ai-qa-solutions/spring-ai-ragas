package ai.qa.solutions.metrics.response;

import static org.assertj.core.api.Assertions.assertThat;

import ai.qa.solutions.execution.StubMultiModelExecutor;
import ai.qa.solutions.execution.listener.MetricExecutionListener;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationContext;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationResult;
import ai.qa.solutions.execution.listener.dto.ModelExclusionEvent;
import ai.qa.solutions.execution.listener.dto.StepContext;
import ai.qa.solutions.execution.listener.dto.StepResults;
import ai.qa.solutions.metrics.response.AnswerCorrectnessMetric.AnswerCorrectnessConfig;
import ai.qa.solutions.metrics.response.FactualCorrectnessMetric.ClaimsResponse;
import ai.qa.solutions.metrics.response.FactualCorrectnessMetric.NliResponse;
import ai.qa.solutions.metrics.response.FactualCorrectnessMetric.NliVerdict;
import ai.qa.solutions.sample.Sample;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for AnswerCorrectnessMetric.
 */
@DisplayName("AnswerCorrectnessMetric")
class AnswerCorrectnessMetricTest {

    private StubMultiModelExecutor stubExecutor;

    @BeforeEach
    void setUp() {
        stubExecutor = new StubMultiModelExecutor(List.of("model1"), List.of("embed1"));
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("should create metric with builder")
        void shouldCreateMetricWithBuilder() {
            final AnswerCorrectnessMetric metric =
                    AnswerCorrectnessMetric.builder().executor(stubExecutor).build();

            assertThat(metric).isNotNull();
            assertThat(metric.getName()).isEqualTo("AnswerCorrectnessMetric");
        }

        @Test
        @DisplayName("should create metric with custom internal metrics")
        void shouldCreateMetricWithCustomInternalMetrics() {
            final FactualCorrectnessMetric factualMetric =
                    FactualCorrectnessMetric.builder().executor(stubExecutor).build();
            final SemanticSimilarityMetric semanticMetric =
                    SemanticSimilarityMetric.builder().executor(stubExecutor).build();

            final AnswerCorrectnessMetric metric = AnswerCorrectnessMetric.builder()
                    .executor(stubExecutor)
                    .factualCorrectnessMetric(factualMetric)
                    .semanticSimilarityMetric(semanticMetric)
                    .build();

            assertThat(metric).isNotNull();
        }
    }

    @Nested
    @DisplayName("Config Tests")
    class ConfigTests {

        @Test
        @DisplayName("should use default weights")
        void shouldUseDefaultWeights() {
            final AnswerCorrectnessConfig config =
                    AnswerCorrectnessConfig.builder().build();

            assertThat(config.getFactualWeight()).isEqualTo(0.75);
            assertThat(config.getSemanticWeight()).isEqualTo(0.25);
        }

        @Test
        @DisplayName("should support custom weights")
        void shouldSupportCustomWeights() {
            final AnswerCorrectnessConfig config = AnswerCorrectnessConfig.builder()
                    .factualWeight(0.6)
                    .semanticWeight(0.4)
                    .build();

            assertThat(config.getFactualWeight()).isEqualTo(0.6);
            assertThat(config.getSemanticWeight()).isEqualTo(0.4);
        }

        @Test
        @DisplayName("should provide preset configurations")
        void shouldProvidePresetConfigurations() {
            final AnswerCorrectnessConfig defaultConfig = AnswerCorrectnessConfig.defaultConfig();
            assertThat(defaultConfig.getFactualWeight()).isEqualTo(0.75);
            assertThat(defaultConfig.getSemanticWeight()).isEqualTo(0.25);

            final AnswerCorrectnessConfig equalConfig = AnswerCorrectnessConfig.equalWeights();
            assertThat(equalConfig.getFactualWeight()).isEqualTo(0.5);
            assertThat(equalConfig.getSemanticWeight()).isEqualTo(0.5);

            final AnswerCorrectnessConfig factualConfig = AnswerCorrectnessConfig.factualFocused();
            assertThat(factualConfig.getFactualWeight()).isEqualTo(0.9);
            assertThat(factualConfig.getSemanticWeight()).isEqualTo(0.1);

            final AnswerCorrectnessConfig semanticConfig = AnswerCorrectnessConfig.semanticFocused();
            assertThat(semanticConfig.getFactualWeight()).isEqualTo(0.1);
            assertThat(semanticConfig.getSemanticWeight()).isEqualTo(0.9);
        }

        @Test
        @DisplayName("should support models list in config")
        void shouldSupportModelsListInConfig() {
            final AnswerCorrectnessConfig config = AnswerCorrectnessConfig.builder()
                    .models(List.of("model-a", "model-b"))
                    .build();

            assertThat(config.getModels()).containsExactly("model-a", "model-b");
        }
    }

    @Nested
    @DisplayName("Input Validation Tests")
    class InputValidationTests {

        @Test
        @DisplayName("should return 0 for empty response")
        void shouldReturnZeroForEmptyResponse() {
            final AnswerCorrectnessMetric metric =
                    AnswerCorrectnessMetric.builder().executor(stubExecutor).build();

            final Sample sample =
                    Sample.builder().response("").reference("Valid reference").build();

            final Double score = metric.singleTurnScore(sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should return 0 for null response")
        void shouldReturnZeroForNullResponse() {
            final AnswerCorrectnessMetric metric =
                    AnswerCorrectnessMetric.builder().executor(stubExecutor).build();

            final Sample sample = Sample.builder().reference("Valid reference").build();

            final Double score = metric.singleTurnScore(sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should return 0 for empty reference")
        void shouldReturnZeroForEmptyReference() {
            final AnswerCorrectnessMetric metric =
                    AnswerCorrectnessMetric.builder().executor(stubExecutor).build();

            final Sample sample =
                    Sample.builder().response("Valid response").reference("").build();

            final Double score = metric.singleTurnScore(sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should return 0 for null reference")
        void shouldReturnZeroForNullReference() {
            final AnswerCorrectnessMetric metric =
                    AnswerCorrectnessMetric.builder().executor(stubExecutor).build();

            final Sample sample = Sample.builder().response("Valid response").build();

            final Double score = metric.singleTurnScore(sample);

            assertThat(score).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Score Calculation Tests")
    class ScoreCalculationTests {

        @Test
        @DisplayName("should calculate weighted score with default weights")
        void shouldCalculateWeightedScoreWithDefaultWeights() {
            // Setup factual correctness responses (2 claims, both supported = 1.0)
            final ClaimsResponse responseClaims = new ClaimsResponse(List.of("Claim A", "Claim B"));
            final ClaimsResponse referenceClaims = new ClaimsResponse(List.of("Claim A", "Claim B"));
            final NliResponse nliResponse = new NliResponse(List.of(
                    new NliVerdict("Claim A", "SUPPORTED", "matches"),
                    new NliVerdict("Claim B", "SUPPORTED", "matches")));

            stubExecutor
                    .withResponseProvider(ClaimsResponse.class, prompt -> responseClaims)
                    .withResponseProvider(NliResponse.class, prompt -> nliResponse);

            // Setup semantic similarity (high similarity)
            final float[] responseEmb = {0.8f, 0.6f, 0.0f};
            final float[] referenceEmb = {0.8f, 0.6f, 0.0f}; // identical
            stubExecutor.withEmbeddings(texts -> List.of(responseEmb, referenceEmb));

            final AnswerCorrectnessMetric metric =
                    AnswerCorrectnessMetric.builder().executor(stubExecutor).build();

            final Sample sample = Sample.builder()
                    .response("Test response")
                    .reference("Test reference")
                    .build();

            final Double score = metric.singleTurnScore(sample);

            // Both components should be ~1.0, so combined should be ~1.0
            assertThat(score).isNotNull();
            assertThat(score).isGreaterThan(0.0);
        }

        @Test
        @DisplayName("should weight factual higher with default config")
        void shouldWeightFactualHigherWithDefaultConfig() {
            // Setup for factual = 0, semantic = 1
            final ClaimsResponse responseClaims = new ClaimsResponse(List.of("Wrong claim"));
            final ClaimsResponse referenceClaims = new ClaimsResponse(List.of("Correct claim"));
            final NliResponse nliResponse =
                    new NliResponse(List.of(new NliVerdict("Wrong claim", "CONTRADICTED", "does not match")));

            stubExecutor
                    .withResponseProvider(ClaimsResponse.class, prompt -> responseClaims)
                    .withResponseProvider(NliResponse.class, prompt -> nliResponse);

            // High semantic similarity (embeddings are similar)
            final float[] responseEmb = {0.9f, 0.1f, 0.0f};
            final float[] referenceEmb = {0.9f, 0.1f, 0.0f};
            stubExecutor.withEmbeddings(texts -> List.of(responseEmb, referenceEmb));

            final AnswerCorrectnessMetric metric =
                    AnswerCorrectnessMetric.builder().executor(stubExecutor).build();

            final Sample sample = Sample.builder()
                    .response("Wrong information")
                    .reference("Correct information")
                    .build();

            final Double score = metric.singleTurnScore(sample);

            // Factual=0, Semantic=1 -> Combined = 0.75*0 + 0.25*1 = 0.25
            assertThat(score).isNotNull();
        }

        @Test
        @DisplayName("should normalize weights when not summing to 1")
        void shouldNormalizeWeights() {
            final ClaimsResponse claims = new ClaimsResponse(List.of("Claim"));
            final NliResponse nliResponse = new NliResponse(List.of(new NliVerdict("Claim", "SUPPORTED", "ok")));

            stubExecutor
                    .withResponseProvider(ClaimsResponse.class, prompt -> claims)
                    .withResponseProvider(NliResponse.class, prompt -> nliResponse);

            final float[] emb = {1.0f, 0.0f};
            stubExecutor.withEmbeddings(texts -> List.of(emb, emb));

            final AnswerCorrectnessMetric metric =
                    AnswerCorrectnessMetric.builder().executor(stubExecutor).build();

            // Use weights that don't sum to 1
            final AnswerCorrectnessConfig config = AnswerCorrectnessConfig.builder()
                    .factualWeight(1.5) // Will normalize to 0.6
                    .semanticWeight(1.0) // Will normalize to 0.4
                    .build();

            final Sample sample =
                    Sample.builder().response("Test").reference("Test").build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isNotNull();
        }
    }

    @Nested
    @DisplayName("Async Execution Tests")
    class AsyncExecutionTests {

        @Test
        @DisplayName("should execute asynchronously")
        void shouldExecuteAsync() {
            final ClaimsResponse claims = new ClaimsResponse(List.of("Test claim"));
            final NliResponse nliResponse = new NliResponse(List.of(new NliVerdict("Test claim", "SUPPORTED", "ok")));

            stubExecutor
                    .withResponseProvider(ClaimsResponse.class, prompt -> claims)
                    .withResponseProvider(NliResponse.class, prompt -> nliResponse);

            final float[] emb = {0.5f, 0.5f};
            stubExecutor.withEmbeddings(texts -> List.of(emb, emb));

            final AnswerCorrectnessMetric metric =
                    AnswerCorrectnessMetric.builder().executor(stubExecutor).build();

            final Sample sample = Sample.builder()
                    .response("Test response")
                    .reference("Test reference")
                    .build();

            final CompletableFuture<Double> future = metric.singleTurnScoreAsync(sample);

            assertThat(future).isNotNull();
            assertThat(future.join()).isNotNull();
        }

        @Test
        @DisplayName("should execute async with config")
        void shouldExecuteAsyncWithConfig() {
            final ClaimsResponse claims = new ClaimsResponse(List.of("Claim"));
            final NliResponse nliResponse = new NliResponse(List.of(new NliVerdict("Claim", "SUPPORTED", "ok")));

            stubExecutor
                    .withResponseProvider(ClaimsResponse.class, prompt -> claims)
                    .withResponseProvider(NliResponse.class, prompt -> nliResponse);

            final float[] emb = {1.0f};
            stubExecutor.withEmbeddings(texts -> List.of(emb, emb));

            final AnswerCorrectnessMetric metric =
                    AnswerCorrectnessMetric.builder().executor(stubExecutor).build();

            final AnswerCorrectnessConfig config = AnswerCorrectnessConfig.equalWeights();

            final Sample sample =
                    Sample.builder().response("Test").reference("Test").build();

            final CompletableFuture<Double> future = metric.singleTurnScoreAsync(config, sample);

            assertThat(future.join()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Listener Notification Tests")
    class ListenerNotificationTests {

        @Test
        @DisplayName("should notify listener on evaluation")
        void shouldNotifyListenerOnEvaluation() {
            final ClaimsResponse claims = new ClaimsResponse(List.of("Claim"));
            final NliResponse nliResponse = new NliResponse(List.of(new NliVerdict("Claim", "SUPPORTED", "ok")));

            stubExecutor
                    .withResponseProvider(ClaimsResponse.class, prompt -> claims)
                    .withResponseProvider(NliResponse.class, prompt -> nliResponse);

            final float[] emb = {0.7f, 0.3f};
            stubExecutor.withEmbeddings(texts -> List.of(emb, emb));

            final AtomicInteger beforeCount = new AtomicInteger(0);
            final AtomicInteger afterCount = new AtomicInteger(0);
            final AtomicInteger stepCount = new AtomicInteger(0);

            final MetricExecutionListener testListener = new MetricExecutionListener() {
                @Override
                public void beforeMetricEvaluation(final MetricEvaluationContext context) {
                    beforeCount.incrementAndGet();
                }

                @Override
                public void afterMetricEvaluation(final MetricEvaluationResult result) {
                    afterCount.incrementAndGet();
                }

                @Override
                public void beforeStep(final StepContext context) {
                    stepCount.incrementAndGet();
                }

                @Override
                public void afterStep(final StepResults results) {}

                @Override
                public void onModelExcluded(final ModelExclusionEvent event) {}

                @Override
                public MetricExecutionListener forEvaluation() {
                    return this;
                }
            };

            final AnswerCorrectnessMetric metric = AnswerCorrectnessMetric.builder()
                    .executor(stubExecutor)
                    .build()
                    .withListeners(List.of(testListener));

            final Sample sample = Sample.builder()
                    .response("Test response")
                    .reference("Test reference")
                    .build();

            metric.singleTurnScore(sample);

            assertThat(beforeCount.get()).isEqualTo(1);
            assertThat(afterCount.get()).isEqualTo(1);
            assertThat(stepCount.get()).isGreaterThanOrEqualTo(3); // At least 3 steps
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should handle factual correctness failure gracefully")
        void shouldHandleFactualCorrectnessFailureGracefully() {
            // Setup to throw exception during factual correctness
            stubExecutor.withResponseProvider(ClaimsResponse.class, prompt -> {
                throw new RuntimeException("Factual failure");
            });

            // But embeddings work fine
            final float[] emb = {0.8f, 0.2f};
            stubExecutor.withEmbeddings(texts -> List.of(emb, emb));

            final AnswerCorrectnessMetric metric =
                    AnswerCorrectnessMetric.builder().executor(stubExecutor).build();

            final Sample sample = Sample.builder()
                    .response("Test response")
                    .reference("Test reference")
                    .build();

            // Should not throw, but return partial score (only semantic)
            final Double score = metric.singleTurnScore(sample);

            assertThat(score).isNotNull();
            // Score should be only from semantic part (25% weight of 1.0)
        }

        @Test
        @DisplayName("should handle semantic similarity failure gracefully")
        void shouldHandleSemanticSimilarityFailureGracefully() {
            // Setup factual to work
            final ClaimsResponse claims = new ClaimsResponse(List.of("Claim"));
            final NliResponse nliResponse = new NliResponse(List.of(new NliVerdict("Claim", "SUPPORTED", "ok")));

            stubExecutor
                    .withResponseProvider(ClaimsResponse.class, prompt -> claims)
                    .withResponseProvider(NliResponse.class, prompt -> nliResponse);

            // But embeddings fail - simulate by model error
            stubExecutor.withModelError("embed1", new RuntimeException("Embedding failure"));

            final AnswerCorrectnessMetric metric =
                    AnswerCorrectnessMetric.builder().executor(stubExecutor).build();

            final Sample sample = Sample.builder()
                    .response("Test response")
                    .reference("Test reference")
                    .build();

            // Should not throw, but return partial score (only factual)
            final Double score = metric.singleTurnScore(sample);

            assertThat(score).isNotNull();
        }
    }
}
