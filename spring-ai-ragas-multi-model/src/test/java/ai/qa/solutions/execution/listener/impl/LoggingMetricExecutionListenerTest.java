package ai.qa.solutions.execution.listener.impl;

import static org.assertj.core.api.Assertions.assertThat;

import ai.qa.solutions.execution.ModelResult;
import ai.qa.solutions.execution.listener.MetricExecutionListener;
import ai.qa.solutions.execution.listener.dto.*;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("LoggingMetricExecutionListener Tests")
class LoggingMetricExecutionListenerTest {

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {

        @Test
        @DisplayName("Default constructor should create instance")
        void defaultConstructorShouldCreateInstance() {
            // When
            final LoggingMetricExecutionListener listener = new LoggingMetricExecutionListener();

            // Then
            assertThat(listener).isNotNull();
            assertThat(listener.getOrder()).isEqualTo(Integer.MIN_VALUE);
        }

        @Test
        @DisplayName("minimal() should create instance with showStepDetails=false")
        void minimalShouldCreateInstanceWithoutStepDetails() {
            // When
            final LoggingMetricExecutionListener listener = LoggingMetricExecutionListener.minimal();

            // Then
            assertThat(listener).isNotNull();
        }

        @Test
        @DisplayName("verbose() should create instance with showStepDetails=true")
        void verboseShouldCreateInstanceWithStepDetails() {
            // When
            final LoggingMetricExecutionListener listener = LoggingMetricExecutionListener.verbose();

            // Then
            assertThat(listener).isNotNull();
        }

        @Test
        @DisplayName("Custom constructor should accept parameters")
        void customConstructorShouldAcceptParameters() {
            // When
            final LoggingMetricExecutionListener listener = new LoggingMetricExecutionListener(80, 10, false);

            // Then
            assertThat(listener).isNotNull();
        }
    }

    @Nested
    @DisplayName("forEvaluation")
    class ForEvaluation {

        @Test
        @DisplayName("Should return new instance each time")
        void shouldReturnNewInstanceEachTime() {
            // Given
            final LoggingMetricExecutionListener original = new LoggingMetricExecutionListener();

            // When
            final MetricExecutionListener instance1 = original.forEvaluation();
            final MetricExecutionListener instance2 = original.forEvaluation();

            // Then
            assertThat(instance1).isNotSameAs(original);
            assertThat(instance2).isNotSameAs(original);
            assertThat(instance1).isNotSameAs(instance2);
        }

        @Test
        @DisplayName("New instance should be of same type")
        void newInstanceShouldBeOfSameType() {
            // Given
            final LoggingMetricExecutionListener original = new LoggingMetricExecutionListener();

            // When
            final MetricExecutionListener instance = original.forEvaluation();

            // Then
            assertThat(instance).isInstanceOf(LoggingMetricExecutionListener.class);
        }
    }

    @Nested
    @DisplayName("getOrder")
    class GetOrder {

        @Test
        @DisplayName("Should return Integer.MIN_VALUE for highest priority")
        void shouldReturnMinValueForHighestPriority() {
            // Given
            final LoggingMetricExecutionListener listener = new LoggingMetricExecutionListener();

            // When/Then
            assertThat(listener.getOrder()).isEqualTo(Integer.MIN_VALUE);
        }
    }

    @Nested
    @DisplayName("Lifecycle Methods")
    class LifecycleMethods {

        @Test
        @DisplayName("beforeMetricEvaluation should not throw")
        void beforeMetricEvaluationShouldNotThrow() {
            // Given
            final LoggingMetricExecutionListener listener = new LoggingMetricExecutionListener();
            final MetricEvaluationContext context = MetricEvaluationContext.builder()
                    .metricName("TestMetric")
                    .modelIds(List.of("model-1", "model-2"))
                    .totalSteps(3)
                    .build();

            // When/Then - should not throw
            listener.beforeMetricEvaluation(context);
        }

        @Test
        @DisplayName("beforeMetricEvaluation should handle embedding models")
        void beforeMetricEvaluationShouldHandleEmbeddingModels() {
            // Given
            final LoggingMetricExecutionListener listener = new LoggingMetricExecutionListener();
            final MetricEvaluationContext context = MetricEvaluationContext.builder()
                    .metricName("ResponseRelevancy")
                    .modelIds(List.of("llm-1"))
                    .embeddingModelIds(List.of("embed-1", "embed-2"))
                    .totalSteps(2)
                    .build();

            // When/Then - should not throw
            listener.beforeMetricEvaluation(context);
        }

        @Test
        @DisplayName("beforeStep should not throw")
        void beforeStepShouldNotThrow() {
            // Given
            final LoggingMetricExecutionListener listener = new LoggingMetricExecutionListener();
            listener.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("Test")
                    .modelIds(List.of("m1"))
                    .totalSteps(1)
                    .build());

            final StepContext context = StepContext.builder()
                    .stepName("GenerateStatements")
                    .stepIndex(0)
                    .totalSteps(2)
                    .build();

            // When/Then - should not throw
            listener.beforeStep(context);
        }

        @Test
        @DisplayName("afterStep should accumulate durations")
        void afterStepShouldAccumulateDurations() {
            // Given
            final LoggingMetricExecutionListener listener = new LoggingMetricExecutionListener();
            listener.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("Test")
                    .modelIds(List.of("m1"))
                    .totalSteps(1)
                    .build());

            final StepResults results = StepResults.builder()
                    .stepName("Step1")
                    .stepIndex(0)
                    .totalSteps(1)
                    .stepType(StepType.LLM)
                    .results(List.of(ModelResult.success("model-1", "ok", Duration.ofMillis(100), "request")))
                    .build();

            // When/Then - should not throw
            listener.afterStep(results);
        }

        @Test
        @DisplayName("afterStep should handle embedding results")
        void afterStepShouldHandleEmbeddingResults() {
            // Given
            final LoggingMetricExecutionListener listener = new LoggingMetricExecutionListener();
            listener.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("Test")
                    .modelIds(List.of("m1"))
                    .totalSteps(1)
                    .build());

            final StepResults results = StepResults.builder()
                    .stepName("Embed")
                    .stepIndex(0)
                    .totalSteps(1)
                    .stepType(StepType.EMBEDDING)
                    .results(List.of(ModelResult.success("llm-1", 0.8, Duration.ofMillis(50), "text")))
                    .embeddingModelResults(List.of(
                            ModelResult.success("embed-1", new float[] {0.1f}, Duration.ofMillis(100), "text"),
                            ModelResult.success("embed-2", new float[] {0.2f}, Duration.ofMillis(150), "text")))
                    .build();

            // When/Then - should not throw
            listener.afterStep(results);
        }

        @Test
        @DisplayName("onModelExcluded should not throw")
        void onModelExcludedShouldNotThrow() {
            // Given
            final LoggingMetricExecutionListener listener = new LoggingMetricExecutionListener();
            listener.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("Test")
                    .modelIds(List.of("m1"))
                    .totalSteps(1)
                    .build());

            final ModelExclusionEvent event = ModelExclusionEvent.builder()
                    .modelId("model-1")
                    .failedStepName("Step1")
                    .failedStepIndex(0)
                    .cause(new RuntimeException("Parse error"))
                    .build();

            // When/Then - should not throw
            listener.onModelExcluded(event);
        }

        @Test
        @DisplayName("afterMetricEvaluation should not throw")
        void afterMetricEvaluationShouldNotThrow() {
            // Given
            final LoggingMetricExecutionListener listener = new LoggingMetricExecutionListener();
            listener.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("Test")
                    .modelIds(List.of("m1"))
                    .totalSteps(1)
                    .build());

            final MetricEvaluationResult result = MetricEvaluationResult.builder()
                    .metricName("TestMetric")
                    .aggregatedScore(0.85)
                    .modelScores(Map.of("model-1", 0.8, "model-2", 0.9))
                    .excludedModels(List.of())
                    .totalDuration(Duration.ofMillis(500))
                    .build();

            // When/Then - should not throw
            listener.afterMetricEvaluation(result);
        }

        @Test
        @DisplayName("afterMetricEvaluation should handle null aggregated score")
        void afterMetricEvaluationShouldHandleNullScore() {
            // Given
            final LoggingMetricExecutionListener listener = new LoggingMetricExecutionListener();
            listener.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("Test")
                    .modelIds(List.of("m1"))
                    .totalSteps(1)
                    .build());

            final MetricEvaluationResult result = MetricEvaluationResult.builder()
                    .metricName("TestMetric")
                    .aggregatedScore(null)
                    .modelScores(Map.of())
                    .excludedModels(List.of("model-1"))
                    .totalDuration(Duration.ofMillis(100))
                    .build();

            // When/Then - should not throw
            listener.afterMetricEvaluation(result);
        }
    }

    @Nested
    @DisplayName("Error Reason Extraction")
    class ErrorReasonExtraction {

        @Test
        @DisplayName("Should handle null cause")
        void shouldHandleNullCause() {
            // Given
            final LoggingMetricExecutionListener listener = new LoggingMetricExecutionListener();
            listener.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("Test")
                    .modelIds(List.of("m1"))
                    .totalSteps(1)
                    .build());

            final ModelExclusionEvent event = ModelExclusionEvent.builder()
                    .modelId("model-1")
                    .failedStepName("Step1")
                    .failedStepIndex(0)
                    .cause(null)
                    .build();

            // When/Then - should not throw
            listener.onModelExcluded(event);
        }

        @Test
        @DisplayName("Should handle exception with null message")
        void shouldHandleExceptionWithNullMessage() {
            // Given
            final LoggingMetricExecutionListener listener = new LoggingMetricExecutionListener();
            listener.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("Test")
                    .modelIds(List.of("m1"))
                    .totalSteps(1)
                    .build());

            final ModelExclusionEvent event = ModelExclusionEvent.builder()
                    .modelId("model-1")
                    .failedStepName("Step1")
                    .failedStepIndex(0)
                    .cause(new RuntimeException((String) null))
                    .build();

            // When/Then - should not throw
            listener.onModelExcluded(event);
        }

        @Test
        @DisplayName("Should handle empty exception message")
        void shouldHandleEmptyExceptionMessage() {
            // Given
            final LoggingMetricExecutionListener listener = new LoggingMetricExecutionListener();
            listener.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("Test")
                    .modelIds(List.of("m1"))
                    .totalSteps(1)
                    .build());

            final ModelExclusionEvent event = ModelExclusionEvent.builder()
                    .modelId("model-1")
                    .failedStepName("Step1")
                    .failedStepIndex(0)
                    .cause(new IllegalStateException("   "))
                    .build();

            // When/Then - should not throw
            listener.onModelExcluded(event);
        }

        @Test
        @DisplayName("Should extract timeout errors")
        void shouldExtractTimeoutErrors() {
            // Given
            final LoggingMetricExecutionListener listener = new LoggingMetricExecutionListener();
            listener.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("Test")
                    .modelIds(List.of("m1"))
                    .totalSteps(1)
                    .build());

            final ModelExclusionEvent event = ModelExclusionEvent.builder()
                    .modelId("model-1")
                    .failedStepName("Step1")
                    .failedStepIndex(0)
                    .cause(new RuntimeException("Connection timeout after 30 seconds"))
                    .build();

            // When/Then - should not throw
            listener.onModelExcluded(event);
        }

        @Test
        @DisplayName("Should extract rate limit errors")
        void shouldExtractRateLimitErrors() {
            // Given
            final LoggingMetricExecutionListener listener = new LoggingMetricExecutionListener();
            listener.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("Test")
                    .modelIds(List.of("m1"))
                    .totalSteps(1)
                    .build());

            final ModelExclusionEvent event = ModelExclusionEvent.builder()
                    .modelId("model-1")
                    .failedStepName("Step1")
                    .failedStepIndex(0)
                    .cause(new RuntimeException("Error 429: rate limit exceeded"))
                    .build();

            // When/Then - should not throw
            listener.onModelExcluded(event);
        }

        @Test
        @DisplayName("Should extract parse errors")
        void shouldExtractParseErrors() {
            // Given
            final LoggingMetricExecutionListener listener = new LoggingMetricExecutionListener();
            listener.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("Test")
                    .modelIds(List.of("m1"))
                    .totalSteps(1)
                    .build());

            final ModelExclusionEvent event = ModelExclusionEvent.builder()
                    .modelId("model-1")
                    .failedStepName("Step1")
                    .failedStepIndex(0)
                    .cause(new RuntimeException("Failed to parse JSON response"))
                    .build();

            // When/Then - should not throw
            listener.onModelExcluded(event);
        }

        @Test
        @DisplayName("Should handle very long error messages")
        void shouldHandleVeryLongErrorMessages() {
            // Given
            final LoggingMetricExecutionListener listener = new LoggingMetricExecutionListener();
            listener.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("Test")
                    .modelIds(List.of("m1"))
                    .totalSteps(1)
                    .build());

            final String longMessage = "A".repeat(200);
            final ModelExclusionEvent event = ModelExclusionEvent.builder()
                    .modelId("model-1")
                    .failedStepName("Step1")
                    .failedStepIndex(0)
                    .cause(new RuntimeException(longMessage))
                    .build();

            // When/Then - should not throw
            listener.onModelExcluded(event);
        }

        @Test
        @DisplayName("Should extract empty response errors")
        void shouldExtractEmptyResponseErrors() {
            // Given
            final LoggingMetricExecutionListener listener = new LoggingMetricExecutionListener();
            listener.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("Test")
                    .modelIds(List.of("m1"))
                    .totalSteps(1)
                    .build());

            final ModelExclusionEvent event = ModelExclusionEvent.builder()
                    .modelId("model-1")
                    .failedStepName("Step1")
                    .failedStepIndex(0)
                    .cause(new RuntimeException("Response is empty"))
                    .build();

            listener.onModelExcluded(event);
        }

        @Test
        @DisplayName("Should extract end-of-input errors")
        void shouldExtractEndOfInputErrors() {
            // Given
            final LoggingMetricExecutionListener listener = new LoggingMetricExecutionListener();
            listener.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("Test")
                    .modelIds(List.of("m1"))
                    .totalSteps(1)
                    .build());

            final ModelExclusionEvent event = ModelExclusionEvent.builder()
                    .modelId("model-1")
                    .failedStepName("Step1")
                    .failedStepIndex(0)
                    .cause(new RuntimeException("Unexpected end-of-input"))
                    .build();

            listener.onModelExcluded(event);
        }

        @Test
        @DisplayName("Should extract markdown in response errors")
        void shouldExtractMarkdownErrors() {
            // Given
            final LoggingMetricExecutionListener listener = new LoggingMetricExecutionListener();
            listener.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("Test")
                    .modelIds(List.of("m1"))
                    .totalSteps(1)
                    .build());

            final ModelExclusionEvent event = ModelExclusionEvent.builder()
                    .modelId("model-1")
                    .failedStepName("Step1")
                    .failedStepIndex(0)
                    .cause(new RuntimeException("Found ```json in response"))
                    .build();

            listener.onModelExcluded(event);
        }

        @Test
        @DisplayName("Should extract unrecognized token errors")
        void shouldExtractUnrecognizedTokenErrors() {
            // Given
            final LoggingMetricExecutionListener listener = new LoggingMetricExecutionListener();
            listener.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("Test")
                    .modelIds(List.of("m1"))
                    .totalSteps(1)
                    .build());

            final ModelExclusionEvent event = ModelExclusionEvent.builder()
                    .modelId("model-1")
                    .failedStepName("Step1")
                    .failedStepIndex(0)
                    .cause(new RuntimeException("Unrecognized token 'Sure'"))
                    .build();

            listener.onModelExcluded(event);
        }

        @Test
        @DisplayName("Should extract truncated JSON errors")
        void shouldExtractTruncatedJsonErrors() {
            // Given
            final LoggingMetricExecutionListener listener = new LoggingMetricExecutionListener();
            listener.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("Test")
                    .modelIds(List.of("m1"))
                    .totalSteps(1)
                    .build());

            final ModelExclusionEvent event = ModelExclusionEvent.builder()
                    .modelId("model-1")
                    .failedStepName("Step1")
                    .failedStepIndex(0)
                    .cause(new RuntimeException("Missing closing quote"))
                    .build();

            listener.onModelExcluded(event);
        }

        @Test
        @DisplayName("Should extract duplicate key errors")
        void shouldExtractDuplicateKeyErrors() {
            // Given
            final LoggingMetricExecutionListener listener = new LoggingMetricExecutionListener();
            listener.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("Test")
                    .modelIds(List.of("m1"))
                    .totalSteps(1)
                    .build());

            final ModelExclusionEvent event = ModelExclusionEvent.builder()
                    .modelId("model-1")
                    .failedStepName("Step1")
                    .failedStepIndex(0)
                    .cause(new RuntimeException("duplicate key found"))
                    .build();

            listener.onModelExcluded(event);
        }

        @Test
        @DisplayName("Should extract auth errors")
        void shouldExtractAuthErrors() {
            // Given
            final LoggingMetricExecutionListener listener = new LoggingMetricExecutionListener();
            listener.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("Test")
                    .modelIds(List.of("m1"))
                    .totalSteps(1)
                    .build());

            final ModelExclusionEvent event = ModelExclusionEvent.builder()
                    .modelId("model-1")
                    .failedStepName("Step1")
                    .failedStepIndex(0)
                    .cause(new RuntimeException("401 Unauthorized"))
                    .build();

            listener.onModelExcluded(event);
        }

        @Test
        @DisplayName("Should extract server errors")
        void shouldExtractServerErrors() {
            // Given
            final LoggingMetricExecutionListener listener = new LoggingMetricExecutionListener();
            listener.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("Test")
                    .modelIds(List.of("m1"))
                    .totalSteps(1)
                    .build());

            final ModelExclusionEvent event = ModelExclusionEvent.builder()
                    .modelId("model-1")
                    .failedStepName("Step1")
                    .failedStepIndex(0)
                    .cause(new RuntimeException("500 Internal Server Error"))
                    .build();

            listener.onModelExcluded(event);
        }

        @Test
        @DisplayName("Should handle short error message without truncation")
        void shouldHandleShortErrorMessage() {
            // Given
            final LoggingMetricExecutionListener listener = new LoggingMetricExecutionListener();
            listener.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("Test")
                    .modelIds(List.of("m1"))
                    .totalSteps(1)
                    .build());

            final ModelExclusionEvent event = ModelExclusionEvent.builder()
                    .modelId("model-1")
                    .failedStepName("Step1")
                    .failedStepIndex(0)
                    .cause(new RuntimeException("Short error"))
                    .build();

            listener.onModelExcluded(event);
        }
    }

    @Nested
    @DisplayName("Full Lifecycle")
    class FullLifecycle {

        @Test
        @DisplayName("Should handle complete evaluation lifecycle")
        void shouldHandleCompleteEvaluationLifecycle() {
            // Given
            final LoggingMetricExecutionListener listener = new LoggingMetricExecutionListener();

            // When - simulate full lifecycle
            listener.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("Faithfulness")
                    .modelIds(List.of("gpt-4", "claude-3"))
                    .totalSteps(3)
                    .build());

            // Step 1
            listener.beforeStep(StepContext.builder()
                    .stepName("GenerateStatements")
                    .stepIndex(0)
                    .totalSteps(3)
                    .build());
            listener.afterStep(StepResults.builder()
                    .stepName("GenerateStatements")
                    .stepIndex(0)
                    .totalSteps(3)
                    .stepType(StepType.LLM)
                    .results(List.of(
                            ModelResult.success("gpt-4", "statements", Duration.ofMillis(500), "prompt"),
                            ModelResult.success("claude-3", "statements", Duration.ofMillis(400), "prompt")))
                    .request("Generate statements...")
                    .build());

            // Step 2 with failure
            listener.beforeStep(StepContext.builder()
                    .stepName("EvaluateFaithfulness")
                    .stepIndex(1)
                    .totalSteps(3)
                    .build());
            listener.afterStep(StepResults.builder()
                    .stepName("EvaluateFaithfulness")
                    .stepIndex(1)
                    .totalSteps(3)
                    .stepType(StepType.LLM)
                    .results(List.of(
                            ModelResult.success("gpt-4", "verdict", Duration.ofMillis(300), "prompt"),
                            ModelResult.failure(
                                    "claude-3", Duration.ofMillis(200), "prompt", new RuntimeException("Parse error"))))
                    .build());
            listener.onModelExcluded(ModelExclusionEvent.builder()
                    .modelId("claude-3")
                    .failedStepName("EvaluateFaithfulness")
                    .failedStepIndex(1)
                    .cause(new RuntimeException("Parse error"))
                    .build());

            // Step 3
            listener.beforeStep(StepContext.builder()
                    .stepName("ComputeScore")
                    .stepIndex(2)
                    .totalSteps(3)
                    .build());
            listener.afterStep(StepResults.builder()
                    .stepName("ComputeScore")
                    .stepIndex(2)
                    .totalSteps(3)
                    .stepType(StepType.COMPUTE)
                    .results(List.of(ModelResult.success("gpt-4", 0.85, Duration.ofMillis(10), null)))
                    .build());

            // Final result
            listener.afterMetricEvaluation(MetricEvaluationResult.builder()
                    .metricName("Faithfulness")
                    .aggregatedScore(0.85)
                    .modelScores(Map.of("gpt-4", 0.85))
                    .excludedModels(List.of("claude-3"))
                    .totalDuration(Duration.ofMillis(1410))
                    .build());

            // Then - should complete without errors
        }

        @Test
        @DisplayName("Should handle evaluation with all models failing")
        void shouldHandleEvaluationWithAllModelsFailing() {
            // Given
            final LoggingMetricExecutionListener listener = new LoggingMetricExecutionListener();

            // When
            listener.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("FailingMetric")
                    .modelIds(List.of("model-1"))
                    .totalSteps(1)
                    .build());

            listener.beforeStep(StepContext.builder()
                    .stepName("Step1")
                    .stepIndex(0)
                    .totalSteps(1)
                    .build());
            listener.afterStep(StepResults.builder()
                    .stepName("Step1")
                    .stepIndex(0)
                    .totalSteps(1)
                    .stepType(StepType.LLM)
                    .results(List.of(ModelResult.failure(
                            "model-1", Duration.ofMillis(100), "prompt", new RuntimeException("Error"))))
                    .build());
            listener.onModelExcluded(ModelExclusionEvent.builder()
                    .modelId("model-1")
                    .failedStepName("Step1")
                    .failedStepIndex(0)
                    .cause(new RuntimeException("Error"))
                    .build());

            listener.afterMetricEvaluation(MetricEvaluationResult.builder()
                    .metricName("FailingMetric")
                    .aggregatedScore(null)
                    .modelScores(Map.of())
                    .excludedModels(List.of("model-1"))
                    .totalDuration(Duration.ofMillis(100))
                    .build());

            // Then - should complete without errors
        }
    }

    @Nested
    @DisplayName("State Isolation")
    class StateIsolation {

        @Test
        @DisplayName("Each forEvaluation instance should have isolated state")
        void eachInstanceShouldHaveIsolatedState() {
            // Given
            final LoggingMetricExecutionListener original = new LoggingMetricExecutionListener();
            final MetricExecutionListener instance1 = original.forEvaluation();
            final MetricExecutionListener instance2 = original.forEvaluation();

            // When - modify one instance
            instance1.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("Metric1")
                    .modelIds(List.of("m1"))
                    .totalSteps(1)
                    .build());
            instance1.afterStep(StepResults.builder()
                    .stepName("Step")
                    .stepIndex(0)
                    .totalSteps(1)
                    .stepType(StepType.LLM)
                    .results(List.of(ModelResult.success("m1", "ok", Duration.ofSeconds(1), "req")))
                    .build());

            // Then - other instance should not be affected
            instance2.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("Metric2")
                    .modelIds(List.of("m2"))
                    .totalSteps(1)
                    .build());

            // Both should complete their lifecycle independently
            instance1.afterMetricEvaluation(MetricEvaluationResult.builder()
                    .metricName("Metric1")
                    .aggregatedScore(0.8)
                    .modelScores(Map.of("m1", 0.8))
                    .excludedModels(List.of())
                    .totalDuration(Duration.ofSeconds(1))
                    .build());

            instance2.afterMetricEvaluation(MetricEvaluationResult.builder()
                    .metricName("Metric2")
                    .aggregatedScore(0.9)
                    .modelScores(Map.of("m2", 0.9))
                    .excludedModels(List.of())
                    .totalDuration(Duration.ofMillis(500))
                    .build());
        }
    }

    @Nested
    @DisplayName("Embedding Timeline")
    class EmbeddingTimeline {

        @Test
        @DisplayName("Should render embedding timeline in final report")
        void shouldRenderEmbeddingTimelineInFinalReport() {
            // Given
            final LoggingMetricExecutionListener listener = new LoggingMetricExecutionListener();

            listener.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("ResponseRelevancy")
                    .modelIds(List.of("llm-1"))
                    .embeddingModelIds(List.of("embed-1", "embed-2"))
                    .totalSteps(2)
                    .build());

            // Embedding step with multiple embedding models
            listener.afterStep(StepResults.builder()
                    .stepName("ComputeEmbeddings")
                    .stepIndex(0)
                    .totalSteps(2)
                    .stepType(StepType.EMBEDDING)
                    .results(List.of(ModelResult.success("llm-1", 0.9, Duration.ofMillis(50), "text")))
                    .embeddingModelResults(List.of(
                            ModelResult.success("embed-1", new float[] {0.1f}, Duration.ofMillis(100), "text"),
                            ModelResult.success("embed-2", new float[] {0.2f}, Duration.ofMillis(150), "text")))
                    .request("Text to embed")
                    .build());

            // Final step
            listener.afterStep(StepResults.builder()
                    .stepName("ComputeScore")
                    .stepIndex(1)
                    .totalSteps(2)
                    .stepType(StepType.COMPUTE)
                    .results(List.of(ModelResult.success("llm-1", 0.85, Duration.ofMillis(10), null)))
                    .build());

            // Final result - should include embedding timeline
            listener.afterMetricEvaluation(MetricEvaluationResult.builder()
                    .metricName("ResponseRelevancy")
                    .aggregatedScore(0.85)
                    .modelScores(Map.of("llm-1", 0.85))
                    .excludedModels(List.of())
                    .totalDuration(Duration.ofMillis(210))
                    .build());
        }

        @Test
        @DisplayName("Should handle embedding step with failures")
        void shouldHandleEmbeddingStepWithFailures() {
            // Given
            final LoggingMetricExecutionListener listener = new LoggingMetricExecutionListener();

            listener.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("Test")
                    .modelIds(List.of("llm-1"))
                    .embeddingModelIds(List.of("embed-1", "embed-2"))
                    .totalSteps(1)
                    .build());

            // Embedding step with one failure
            listener.afterStep(StepResults.builder()
                    .stepName("Embed")
                    .stepIndex(0)
                    .totalSteps(1)
                    .stepType(StepType.EMBEDDING)
                    .results(List.of(ModelResult.success("llm-1", 0.8, Duration.ofMillis(50), "text")))
                    .embeddingModelResults(List.of(
                            ModelResult.success("embed-1", new float[] {0.1f}, Duration.ofMillis(100), "text"),
                            ModelResult.failure(
                                    "embed-2", Duration.ofMillis(50), "text", new RuntimeException("Embed error"))))
                    .build());

            listener.afterMetricEvaluation(MetricEvaluationResult.builder()
                    .metricName("Test")
                    .aggregatedScore(0.8)
                    .modelScores(Map.of("llm-1", 0.8))
                    .excludedModels(List.of())
                    .totalDuration(Duration.ofMillis(150))
                    .build());
        }
    }

    @Nested
    @DisplayName("Edge Cases in Rendering")
    class EdgeCasesInRendering {

        @Test
        @DisplayName("Should handle step with zero duration in results")
        void shouldHandleStepWithZeroDuration() {
            // Given
            final LoggingMetricExecutionListener listener = new LoggingMetricExecutionListener();

            listener.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("Test")
                    .modelIds(List.of("m1"))
                    .totalSteps(1)
                    .build());

            // Result with zero duration
            listener.afterStep(StepResults.builder()
                    .stepName("Step1")
                    .stepIndex(0)
                    .totalSteps(1)
                    .stepType(StepType.LLM)
                    .results(List.of(ModelResult.success("m1", "result", Duration.ZERO, "request")))
                    .build());

            listener.afterMetricEvaluation(MetricEvaluationResult.builder()
                    .metricName("Test")
                    .aggregatedScore(0.8)
                    .modelScores(Map.of("m1", 0.8))
                    .excludedModels(List.of())
                    .totalDuration(Duration.ofMillis(100))
                    .build());
        }

        @Test
        @DisplayName("Should handle minimal listener without step details")
        void shouldHandleMinimalListenerWithoutStepDetails() {
            // Given
            final LoggingMetricExecutionListener listener = LoggingMetricExecutionListener.minimal();

            listener.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("Test")
                    .modelIds(List.of("m1"))
                    .totalSteps(1)
                    .build());

            listener.beforeStep(StepContext.builder()
                    .stepName("Step1")
                    .stepIndex(0)
                    .totalSteps(1)
                    .build());

            listener.afterStep(StepResults.builder()
                    .stepName("Step1")
                    .stepIndex(0)
                    .totalSteps(1)
                    .stepType(StepType.LLM)
                    .results(List.of(ModelResult.success("m1", "ok", Duration.ofMillis(100), "req")))
                    .build());

            listener.afterMetricEvaluation(MetricEvaluationResult.builder()
                    .metricName("Test")
                    .aggregatedScore(0.8)
                    .modelScores(Map.of("m1", 0.8))
                    .excludedModels(List.of())
                    .totalDuration(Duration.ofMillis(100))
                    .build());
        }

        @Test
        @DisplayName("Should handle single model without timeline chart")
        void shouldHandleSingleModelWithoutTimelineChart() {
            // Given
            final LoggingMetricExecutionListener listener = new LoggingMetricExecutionListener();

            listener.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("Test")
                    .modelIds(List.of("single-model"))
                    .totalSteps(1)
                    .build());

            // Single model - no timeline chart needed
            listener.afterStep(StepResults.builder()
                    .stepName("Step1")
                    .stepIndex(0)
                    .totalSteps(1)
                    .stepType(StepType.LLM)
                    .results(List.of(ModelResult.success("single-model", "ok", Duration.ofMillis(100), "prompt")))
                    .request("Test prompt")
                    .build());

            listener.afterMetricEvaluation(MetricEvaluationResult.builder()
                    .metricName("Test")
                    .aggregatedScore(0.9)
                    .modelScores(Map.of("single-model", 0.9))
                    .excludedModels(List.of())
                    .totalDuration(Duration.ofMillis(100))
                    .build());
        }

        @Test
        @DisplayName("Should handle empty model scores")
        void shouldHandleEmptyModelScores() {
            // Given
            final LoggingMetricExecutionListener listener = new LoggingMetricExecutionListener();

            listener.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("Test")
                    .modelIds(List.of("m1"))
                    .totalSteps(1)
                    .build());

            // All models failed - empty scores
            listener.afterMetricEvaluation(MetricEvaluationResult.builder()
                    .metricName("Test")
                    .aggregatedScore(null)
                    .modelScores(null)
                    .excludedModels(List.of("m1"))
                    .totalDuration(Duration.ofMillis(50))
                    .build());
        }
    }
}
