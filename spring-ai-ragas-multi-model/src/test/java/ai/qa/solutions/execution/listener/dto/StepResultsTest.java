package ai.qa.solutions.execution.listener.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import ai.qa.solutions.execution.ModelResult;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("StepResults Tests")
class StepResultsTest {

    private static final Duration DURATION_100MS = Duration.ofMillis(100);
    private static final Duration DURATION_200MS = Duration.ofMillis(200);
    private static final Duration DURATION_300MS = Duration.ofMillis(300);

    @Nested
    @DisplayName("Builder and Basic Fields")
    class BuilderAndBasicFields {

        @Test
        @DisplayName("Should build with all fields")
        void shouldBuildWithAllFields() {
            // Given
            final List<ModelResult<?>> results =
                    List.of(ModelResult.success("model-1", "result", DURATION_100MS, "request"));

            // When
            final StepResults stepResults = StepResults.builder()
                    .stepName("TestStep")
                    .stepIndex(0)
                    .totalSteps(3)
                    .results(results)
                    .stepType(StepType.LLM)
                    .request("test request")
                    .build();

            // Then
            assertThat(stepResults.getStepName()).isEqualTo("TestStep");
            assertThat(stepResults.getStepIndex()).isZero();
            assertThat(stepResults.getTotalSteps()).isEqualTo(3);
            assertThat(stepResults.getResults()).hasSize(1);
            assertThat(stepResults.getStepType()).isEqualTo(StepType.LLM);
            assertThat(stepResults.getRequest()).isEqualTo("test request");
        }

        @Test
        @DisplayName("Should use default empty list for results")
        void shouldUseDefaultEmptyListForResults() {
            // When
            final StepResults stepResults = StepResults.builder()
                    .stepName("EmptyStep")
                    .stepIndex(0)
                    .totalSteps(1)
                    .stepType(StepType.COMPUTE)
                    .build();

            // Then
            assertThat(stepResults.getResults()).isEmpty();
            assertThat(stepResults.getEmbeddingModelResults()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getSuccessful and getFailed")
    class SuccessfulAndFailed {

        @Test
        @DisplayName("Should return only successful results")
        void shouldReturnOnlySuccessfulResults() {
            // Given
            final StepResults stepResults = StepResults.builder()
                    .stepName("MixedStep")
                    .stepIndex(0)
                    .totalSteps(1)
                    .results(List.of(
                            ModelResult.success("model-1", "ok", DURATION_100MS, "req"),
                            ModelResult.failure("model-2", DURATION_100MS, "req", new RuntimeException("fail")),
                            ModelResult.success("model-3", "ok", DURATION_100MS, "req")))
                    .stepType(StepType.LLM)
                    .build();

            // When
            final List<ModelResult<?>> successful = stepResults.getSuccessful();

            // Then
            assertThat(successful).hasSize(2);
            assertThat(successful).allMatch(ModelResult::isSuccess);
            assertThat(successful).extracting(ModelResult::modelId).containsExactlyInAnyOrder("model-1", "model-3");
        }

        @Test
        @DisplayName("Should return only failed results")
        void shouldReturnOnlyFailedResults() {
            // Given
            final StepResults stepResults = StepResults.builder()
                    .stepName("MixedStep")
                    .stepIndex(0)
                    .totalSteps(1)
                    .results(List.of(
                            ModelResult.success("model-1", "ok", DURATION_100MS, "req"),
                            ModelResult.failure("model-2", DURATION_100MS, "req", new RuntimeException("fail")),
                            ModelResult.failure("model-3", DURATION_100MS, "req", new RuntimeException("fail2"))))
                    .stepType(StepType.LLM)
                    .build();

            // When
            final List<ModelResult<?>> failed = stepResults.getFailed();

            // Then
            assertThat(failed).hasSize(2);
            assertThat(failed).allMatch(ModelResult::isFailure);
            assertThat(failed).extracting(ModelResult::modelId).containsExactlyInAnyOrder("model-2", "model-3");
        }

        @Test
        @DisplayName("Should return empty list when no successful results")
        void shouldReturnEmptyListWhenNoSuccessfulResults() {
            // Given
            final StepResults stepResults = StepResults.builder()
                    .stepName("AllFailed")
                    .stepIndex(0)
                    .totalSteps(1)
                    .results(List.of(
                            ModelResult.failure("model-1", DURATION_100MS, "req", new RuntimeException("fail"))))
                    .stepType(StepType.LLM)
                    .build();

            // When/Then
            assertThat(stepResults.getSuccessful()).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list when no failed results")
        void shouldReturnEmptyListWhenNoFailedResults() {
            // Given
            final StepResults stepResults = StepResults.builder()
                    .stepName("AllSuccess")
                    .stepIndex(0)
                    .totalSteps(1)
                    .results(List.of(ModelResult.success("model-1", "ok", DURATION_100MS, "req")))
                    .stepType(StepType.LLM)
                    .build();

            // When/Then
            assertThat(stepResults.getFailed()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getSuccessCount and getFailCount")
    class SuccessAndFailCounts {

        @Test
        @DisplayName("Should count successful results")
        void shouldCountSuccessfulResults() {
            // Given
            final StepResults stepResults = StepResults.builder()
                    .stepName("Step")
                    .stepIndex(0)
                    .totalSteps(1)
                    .results(List.of(
                            ModelResult.success("m1", "ok", DURATION_100MS, "req"),
                            ModelResult.success("m2", "ok", DURATION_100MS, "req"),
                            ModelResult.failure("m3", DURATION_100MS, "req", new RuntimeException("err"))))
                    .stepType(StepType.LLM)
                    .build();

            // When/Then
            assertThat(stepResults.getSuccessCount()).isEqualTo(2);
            assertThat(stepResults.getFailCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return zero for empty results")
        void shouldReturnZeroForEmptyResults() {
            // Given
            final StepResults stepResults = StepResults.builder()
                    .stepName("Empty")
                    .stepIndex(0)
                    .totalSteps(1)
                    .build();

            // When/Then
            assertThat(stepResults.getSuccessCount()).isZero();
            assertThat(stepResults.getFailCount()).isZero();
        }
    }

    @Nested
    @DisplayName("getSuccessRate")
    class SuccessRate {

        @Test
        @DisplayName("Should calculate success rate correctly")
        void shouldCalculateSuccessRateCorrectly() {
            // Given - 3 success, 1 fail = 75%
            final StepResults stepResults = StepResults.builder()
                    .stepName("Step")
                    .stepIndex(0)
                    .totalSteps(1)
                    .results(List.of(
                            ModelResult.success("m1", "ok", DURATION_100MS, "req"),
                            ModelResult.success("m2", "ok", DURATION_100MS, "req"),
                            ModelResult.success("m3", "ok", DURATION_100MS, "req"),
                            ModelResult.failure("m4", DURATION_100MS, "req", new RuntimeException("err"))))
                    .stepType(StepType.LLM)
                    .build();

            // When/Then
            assertThat(stepResults.getSuccessRate()).isCloseTo(0.75, offset(0.001));
        }

        @Test
        @DisplayName("Should return 0.0 for empty results")
        void shouldReturnZeroForEmptyResults() {
            // Given
            final StepResults stepResults = StepResults.builder()
                    .stepName("Empty")
                    .stepIndex(0)
                    .totalSteps(1)
                    .build();

            // When/Then
            assertThat(stepResults.getSuccessRate()).isZero();
        }

        @Test
        @DisplayName("Should return 1.0 when all successful")
        void shouldReturnOneWhenAllSuccessful() {
            // Given
            final StepResults stepResults = StepResults.builder()
                    .stepName("Step")
                    .stepIndex(0)
                    .totalSteps(1)
                    .results(List.of(
                            ModelResult.success("m1", "ok", DURATION_100MS, "req"),
                            ModelResult.success("m2", "ok", DURATION_100MS, "req")))
                    .stepType(StepType.LLM)
                    .build();

            // When/Then
            assertThat(stepResults.getSuccessRate()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should return 0.0 when all failed")
        void shouldReturnZeroWhenAllFailed() {
            // Given
            final StepResults stepResults = StepResults.builder()
                    .stepName("Step")
                    .stepIndex(0)
                    .totalSteps(1)
                    .results(List.of(
                            ModelResult.failure("m1", DURATION_100MS, "req", new RuntimeException("err")),
                            ModelResult.failure("m2", DURATION_100MS, "req", new RuntimeException("err"))))
                    .stepType(StepType.LLM)
                    .build();

            // When/Then
            assertThat(stepResults.getSuccessRate()).isZero();
        }
    }

    @Nested
    @DisplayName("getTotalDuration")
    class TotalDuration {

        @Test
        @DisplayName("Should return max duration across all results")
        void shouldReturnMaxDuration() {
            // Given
            final StepResults stepResults = StepResults.builder()
                    .stepName("Step")
                    .stepIndex(0)
                    .totalSteps(1)
                    .results(List.of(
                            ModelResult.success("m1", "ok", DURATION_100MS, "req"),
                            ModelResult.success("m2", "ok", DURATION_300MS, "req"),
                            ModelResult.success("m3", "ok", DURATION_200MS, "req")))
                    .stepType(StepType.LLM)
                    .build();

            // When/Then
            assertThat(stepResults.getTotalDuration()).isEqualTo(DURATION_300MS);
        }

        @Test
        @DisplayName("Should return Duration.ZERO for empty results")
        void shouldReturnZeroForEmptyResults() {
            // Given
            final StepResults stepResults = StepResults.builder()
                    .stepName("Empty")
                    .stepIndex(0)
                    .totalSteps(1)
                    .build();

            // When/Then
            assertThat(stepResults.getTotalDuration()).isEqualTo(Duration.ZERO);
        }

        @Test
        @DisplayName("Should include failed results in duration calculation")
        void shouldIncludeFailedResultsInDuration() {
            // Given - failed result has max duration
            final StepResults stepResults = StepResults.builder()
                    .stepName("Step")
                    .stepIndex(0)
                    .totalSteps(1)
                    .results(List.of(
                            ModelResult.success("m1", "ok", DURATION_100MS, "req"),
                            ModelResult.failure("m2", DURATION_300MS, "req", new RuntimeException("err"))))
                    .stepType(StepType.LLM)
                    .build();

            // When/Then
            assertThat(stepResults.getTotalDuration()).isEqualTo(DURATION_300MS);
        }
    }

    @Nested
    @DisplayName("getResultsByModelId")
    class ResultsByModelId {

        @Test
        @DisplayName("Should return results indexed by model ID")
        void shouldReturnResultsIndexedByModelId() {
            // Given
            final ModelResult<?> result1 = ModelResult.success("model-alpha", "ok1", DURATION_100MS, "req");
            final ModelResult<?> result2 = ModelResult.success("model-beta", "ok2", DURATION_200MS, "req");

            final StepResults stepResults = StepResults.builder()
                    .stepName("Step")
                    .stepIndex(0)
                    .totalSteps(1)
                    .results(List.of(result1, result2))
                    .stepType(StepType.LLM)
                    .build();

            // When
            final Map<String, ModelResult<?>> byModelId = stepResults.getResultsByModelId();

            // Then
            assertThat(byModelId).hasSize(2);
            assertThat(byModelId.get("model-alpha")).isEqualTo(result1);
            assertThat(byModelId.get("model-beta")).isEqualTo(result2);
        }

        @Test
        @DisplayName("Should return empty map for empty results")
        void shouldReturnEmptyMapForEmptyResults() {
            // Given
            final StepResults stepResults = StepResults.builder()
                    .stepName("Empty")
                    .stepIndex(0)
                    .totalSteps(1)
                    .build();

            // When/Then
            assertThat(stepResults.getResultsByModelId()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Embedding Results")
    class EmbeddingResults {

        @Test
        @DisplayName("getEmbeddingDuration should return max embedding duration")
        void getEmbeddingDurationShouldReturnMax() {
            // Given
            final StepResults stepResults = StepResults.builder()
                    .stepName("EmbedStep")
                    .stepIndex(0)
                    .totalSteps(1)
                    .stepType(StepType.EMBEDDING)
                    .embeddingModelResults(List.of(
                            ModelResult.success("embed-1", new float[] {0.1f}, DURATION_100MS, "text"),
                            ModelResult.success("embed-2", new float[] {0.2f}, DURATION_300MS, "text"),
                            ModelResult.success("embed-3", new float[] {0.3f}, DURATION_200MS, "text")))
                    .build();

            // When/Then
            assertThat(stepResults.getEmbeddingDuration()).isEqualTo(DURATION_300MS);
        }

        @Test
        @DisplayName("getEmbeddingDuration should return ZERO for null embedding results")
        void getEmbeddingDurationShouldReturnZeroForNull() {
            // Given
            final StepResults stepResults = StepResults.builder()
                    .stepName("Step")
                    .stepIndex(0)
                    .totalSteps(1)
                    .stepType(StepType.LLM)
                    .embeddingModelResults(null)
                    .build();

            // When/Then
            assertThat(stepResults.getEmbeddingDuration()).isEqualTo(Duration.ZERO);
        }

        @Test
        @DisplayName("getEmbeddingDuration should return ZERO for empty embedding results")
        void getEmbeddingDurationShouldReturnZeroForEmpty() {
            // Given
            final StepResults stepResults = StepResults.builder()
                    .stepName("Step")
                    .stepIndex(0)
                    .totalSteps(1)
                    .stepType(StepType.LLM)
                    .embeddingModelResults(List.of())
                    .build();

            // When/Then
            assertThat(stepResults.getEmbeddingDuration()).isEqualTo(Duration.ZERO);
        }

        @Test
        @DisplayName("getEmbeddingSuccessCount should count successful embedding results")
        void getEmbeddingSuccessCountShouldWork() {
            // Given
            final StepResults stepResults = StepResults.builder()
                    .stepName("EmbedStep")
                    .stepIndex(0)
                    .totalSteps(1)
                    .stepType(StepType.EMBEDDING)
                    .embeddingModelResults(List.of(
                            ModelResult.success("embed-1", new float[] {0.1f}, DURATION_100MS, "text"),
                            ModelResult.failure("embed-2", DURATION_100MS, "text", new RuntimeException("err")),
                            ModelResult.success("embed-3", new float[] {0.3f}, DURATION_200MS, "text")))
                    .build();

            // When/Then
            assertThat(stepResults.getEmbeddingSuccessCount()).isEqualTo(2);
            assertThat(stepResults.getEmbeddingFailCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("getEmbeddingSuccessCount should return 0 for null embedding results")
        void getEmbeddingSuccessCountShouldReturnZeroForNull() {
            // Given
            final StepResults stepResults = StepResults.builder()
                    .stepName("Step")
                    .stepIndex(0)
                    .totalSteps(1)
                    .embeddingModelResults(null)
                    .build();

            // When/Then
            assertThat(stepResults.getEmbeddingSuccessCount()).isZero();
            assertThat(stepResults.getEmbeddingFailCount()).isZero();
        }
    }

    @Nested
    @DisplayName("Step Type Handling")
    class StepTypeHandling {

        @Test
        @DisplayName("Should handle LLM step type")
        void shouldHandleLlmStepType() {
            // Given
            final StepResults stepResults = StepResults.builder()
                    .stepName("LlmStep")
                    .stepIndex(0)
                    .totalSteps(1)
                    .stepType(StepType.LLM)
                    .request("What is the answer?")
                    .build();

            // Then
            assertThat(stepResults.getStepType()).isEqualTo(StepType.LLM);
            assertThat(stepResults.getRequest()).isEqualTo("What is the answer?");
        }

        @Test
        @DisplayName("Should handle EMBEDDING step type")
        void shouldHandleEmbeddingStepType() {
            // Given
            final StepResults stepResults = StepResults.builder()
                    .stepName("EmbedStep")
                    .stepIndex(0)
                    .totalSteps(1)
                    .stepType(StepType.EMBEDDING)
                    .request("Text to embed")
                    .build();

            // Then
            assertThat(stepResults.getStepType()).isEqualTo(StepType.EMBEDDING);
        }

        @Test
        @DisplayName("Should handle COMPUTE step type with no request")
        void shouldHandleComputeStepType() {
            // Given
            final StepResults stepResults = StepResults.builder()
                    .stepName("ComputeStep")
                    .stepIndex(0)
                    .totalSteps(1)
                    .stepType(StepType.COMPUTE)
                    .build();

            // Then
            assertThat(stepResults.getStepType()).isEqualTo(StepType.COMPUTE);
            assertThat(stepResults.getRequest()).isNull();
        }
    }
}
