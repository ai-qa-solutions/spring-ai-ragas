package ai.qa.solutions.allure.model;

import static org.assertj.core.api.Assertions.assertThat;

import ai.qa.solutions.execution.ModelResult;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ModelExecutionData")
class ModelExecutionDataTest {

    @Nested
    @DisplayName("from (success)")
    class FromSuccess {

        @Test
        @DisplayName("should convert successful ModelResult")
        void shouldConvertSuccessfulResult() {
            final TestResponse response = new TestResponse("test value");
            final ModelResult<TestResponse> modelResult =
                    ModelResult.success("model-1", response, Duration.ofMillis(500), "test prompt");

            final ModelExecutionData data = ModelExecutionData.from(modelResult);

            assertThat(data.getModelId()).isEqualTo("model-1");
            assertThat(data.isSuccess()).isTrue();
            assertThat(data.getDuration()).isEqualTo(Duration.ofMillis(500));
            assertThat(data.getRequest()).isEqualTo("test prompt");
            assertThat(data.getResult()).isEqualTo(response);
            assertThat(data.getResultJson()).contains("test value");
            assertThat(data.getErrorMessage()).isNull();
            assertThat(data.getStackTrace()).isNull();
        }

        @Test
        @DisplayName("should handle null result gracefully")
        void shouldHandleNullResult() {
            final ModelResult<Object> modelResult =
                    ModelResult.success("model-1", null, Duration.ofMillis(100), "prompt");

            final ModelExecutionData data = ModelExecutionData.from(modelResult);

            assertThat(data.isSuccess()).isTrue();
            assertThat(data.getResultJson()).isEqualTo("null");
        }
    }

    @Nested
    @DisplayName("from (failure)")
    class FromFailure {

        @Test
        @DisplayName("should convert failed ModelResult with exception")
        void shouldConvertFailedResult() {
            final RuntimeException error = new RuntimeException("Connection timeout");
            final ModelResult<Object> modelResult =
                    ModelResult.failure("model-2", Duration.ofMillis(200), "test prompt", error);

            final ModelExecutionData data = ModelExecutionData.from(modelResult);

            assertThat(data.getModelId()).isEqualTo("model-2");
            assertThat(data.isSuccess()).isFalse();
            assertThat(data.getDuration()).isEqualTo(Duration.ofMillis(200));
            assertThat(data.getErrorMessage()).isEqualTo("Connection timeout");
            assertThat(data.getStackTrace()).contains("RuntimeException");
            assertThat(data.getStackTrace()).contains("Connection timeout");
            assertThat(data.getResult()).isNull();
            assertThat(data.getResultJson()).isNull();
        }

        @Test
        @DisplayName("should handle exception without message")
        void shouldHandleExceptionWithoutMessage() {
            final NullPointerException error = new NullPointerException();
            final ModelResult<Object> modelResult =
                    ModelResult.failure("model-3", Duration.ofMillis(50), "prompt", error);

            final ModelExecutionData data = ModelExecutionData.from(modelResult);

            assertThat(data.isSuccess()).isFalse();
            assertThat(data.getErrorMessage()).isEqualTo("NullPointerException");
        }

        @Test
        @DisplayName("should handle error with null message")
        void shouldHandleErrorWithNullMessage() {
            // Test that error with null message uses class name
            final RuntimeException error = new RuntimeException((String) null);
            final ModelResult<Object> modelResult =
                    ModelResult.failure("model-4", Duration.ofMillis(10), "prompt", error);

            final ModelExecutionData data = ModelExecutionData.from(modelResult);

            assertThat(data.isSuccess()).isFalse();
            assertThat(data.getErrorMessage()).isEqualTo("RuntimeException");
            assertThat(data.getStackTrace()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("getDurationMs")
    class GetDurationMs {

        @Test
        @DisplayName("should return duration in milliseconds")
        void shouldReturnDurationMs() {
            final ModelExecutionData data = ModelExecutionData.builder()
                    .modelId("model")
                    .duration(Duration.ofSeconds(2))
                    .build();

            assertThat(data.getDurationMs()).isEqualTo(2000);
        }

        @Test
        @DisplayName("should return 0 when duration is null")
        void shouldReturnZeroWhenDurationNull() {
            final ModelExecutionData data =
                    ModelExecutionData.builder().modelId("model").build();

            assertThat(data.getDurationMs()).isZero();
        }
    }

    record TestResponse(String value) {}
}
