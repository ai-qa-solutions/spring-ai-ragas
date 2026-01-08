package ai.qa.solutions.allure.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.qa.solutions.execution.ModelResult;
import ai.qa.solutions.execution.listener.dto.StepResults;
import ai.qa.solutions.execution.listener.dto.StepType;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("StepExecutionData")
class StepExecutionDataTest {

    @Nested
    @DisplayName("from")
    class From {

        @Test
        @DisplayName("should convert StepResults with LLM results")
        void shouldConvertWithLlmResults() {
            final StepResults results = StepResults.builder()
                    .stepName("GenerateStatements")
                    .stepIndex(0)
                    .totalSteps(3)
                    .stepType(StepType.LLM)
                    .request("test prompt")
                    .results(List.of(
                            ModelResult.success("model-1", "result1", Duration.ofMillis(300), "prompt"),
                            ModelResult.success("model-2", "result2", Duration.ofMillis(400), "prompt")))
                    .build();

            final StepExecutionData data = StepExecutionData.from(results);

            assertThat(data.getStepName()).isEqualTo("GenerateStatements");
            assertThat(data.getStepIndex()).isZero();
            assertThat(data.getTotalSteps()).isEqualTo(3);
            assertThat(data.getStepType()).isEqualTo(StepType.LLM);
            assertThat(data.getRequest()).isEqualTo("test prompt");
            assertThat(data.getDuration()).isEqualTo(Duration.ofMillis(400)); // max of 300 and 400
            assertThat(data.getModelResults()).hasSize(2);
            assertThat(data.getEmbeddingResults()).isEmpty();
        }

        @Test
        @DisplayName("should convert StepResults with embedding results")
        void shouldConvertWithEmbeddingResults() {
            final StepResults results = StepResults.builder()
                    .stepName("ComputeSimilarity")
                    .stepIndex(1)
                    .totalSteps(2)
                    .stepType(StepType.EMBEDDING)
                    .request("embedding text")
                    .results(List.of())
                    .embeddingModelResults(List.of(
                            ModelResult.success("embedding-1", new float[] {0.1f}, Duration.ofMillis(100), "text")))
                    .build();

            final StepExecutionData data = StepExecutionData.from(results);

            assertThat(data.getStepType()).isEqualTo(StepType.EMBEDDING);
            assertThat(data.getEmbeddingResults()).hasSize(1);
        }

        @Test
        @DisplayName("should handle null embedding results")
        void shouldHandleNullEmbeddingResults() {
            final StepResults results = mock(StepResults.class);
            when(results.getStepName()).thenReturn("Step");
            when(results.getStepIndex()).thenReturn(0);
            when(results.getTotalSteps()).thenReturn(1);
            when(results.getStepType()).thenReturn(StepType.COMPUTE);
            when(results.getResults()).thenReturn(List.of());
            when(results.getEmbeddingModelResults()).thenReturn(null);

            final StepExecutionData data = StepExecutionData.from(results);

            assertThat(data.getEmbeddingResults()).isEmpty();
        }

        @Test
        @DisplayName("should copy success and failure counts")
        void shouldCopySuccessAndFailureCounts() {
            final StepResults results = StepResults.builder()
                    .stepName("Step")
                    .stepIndex(0)
                    .totalSteps(1)
                    .stepType(StepType.LLM)
                    .results(List.of(
                            ModelResult.success("m1", "r", Duration.ZERO, "p"),
                            ModelResult.failure("m2", Duration.ZERO, "p", new RuntimeException("err"))))
                    .build();

            final StepExecutionData data = StepExecutionData.from(results);

            assertThat(data.getSuccessCount()).isEqualTo(1);
            assertThat(data.getFailureCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("getDurationMs")
    class GetDurationMs {

        @Test
        @DisplayName("should return duration in milliseconds")
        void shouldReturnDurationMs() {
            final StepExecutionData data = StepExecutionData.builder()
                    .stepName("Step")
                    .duration(Duration.ofSeconds(2))
                    .build();

            assertThat(data.getDurationMs()).isEqualTo(2000);
        }

        @Test
        @DisplayName("should return 0 when duration is null")
        void shouldReturnZeroWhenNull() {
            final StepExecutionData data =
                    StepExecutionData.builder().stepName("Step").build();

            assertThat(data.getDurationMs()).isZero();
        }
    }

    @Nested
    @DisplayName("step type checks")
    class StepTypeChecks {

        @Test
        @DisplayName("should identify LLM step")
        void shouldIdentifyLlmStep() {
            final StepExecutionData data = StepExecutionData.builder()
                    .stepName("Step")
                    .stepType(StepType.LLM)
                    .build();

            assertThat(data.isLlmStep()).isTrue();
            assertThat(data.isEmbeddingStep()).isFalse();
            assertThat(data.isComputeStep()).isFalse();
        }

        @Test
        @DisplayName("should identify embedding step")
        void shouldIdentifyEmbeddingStep() {
            final StepExecutionData data = StepExecutionData.builder()
                    .stepName("Step")
                    .stepType(StepType.EMBEDDING)
                    .build();

            assertThat(data.isLlmStep()).isFalse();
            assertThat(data.isEmbeddingStep()).isTrue();
            assertThat(data.isComputeStep()).isFalse();
        }

        @Test
        @DisplayName("should identify compute step")
        void shouldIdentifyComputeStep() {
            final StepExecutionData data = StepExecutionData.builder()
                    .stepName("Step")
                    .stepType(StepType.COMPUTE)
                    .build();

            assertThat(data.isLlmStep()).isFalse();
            assertThat(data.isEmbeddingStep()).isFalse();
            assertThat(data.isComputeStep()).isTrue();
        }
    }

    @Nested
    @DisplayName("getStepNumber")
    class GetStepNumber {

        @Test
        @DisplayName("should return 1-based step number")
        void shouldReturn1BasedNumber() {
            final StepExecutionData data =
                    StepExecutionData.builder().stepName("Step").stepIndex(0).build();

            assertThat(data.getStepNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("should convert any index to 1-based")
        void shouldConvertAnyIndex() {
            final StepExecutionData data =
                    StepExecutionData.builder().stepName("Step").stepIndex(4).build();

            assertThat(data.getStepNumber()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("hasFailures")
    class HasFailures {

        @Test
        @DisplayName("should return true when failure count > 0")
        void shouldReturnTrueWhenFailures() {
            final StepExecutionData data =
                    StepExecutionData.builder().stepName("Step").failureCount(2).build();

            assertThat(data.hasFailures()).isTrue();
        }

        @Test
        @DisplayName("should return false when failure count is 0")
        void shouldReturnFalseWhenNoFailures() {
            final StepExecutionData data =
                    StepExecutionData.builder().stepName("Step").failureCount(0).build();

            assertThat(data.hasFailures()).isFalse();
        }
    }

    @Nested
    @DisplayName("hasEmbeddingResults")
    class HasEmbeddingResults {

        @Test
        @DisplayName("should return true when embedding results present")
        void shouldReturnTrueWhenPresent() {
            final StepExecutionData data = StepExecutionData.builder()
                    .stepName("Step")
                    .embeddingResults(List.of(ModelExecutionData.builder()
                            .modelId("emb-1")
                            .success(true)
                            .build()))
                    .build();

            assertThat(data.hasEmbeddingResults()).isTrue();
        }

        @Test
        @DisplayName("should return false when no embedding results")
        void shouldReturnFalseWhenEmpty() {
            final StepExecutionData data =
                    StepExecutionData.builder().stepName("Step").build();

            assertThat(data.hasEmbeddingResults()).isFalse();
        }
    }
}
