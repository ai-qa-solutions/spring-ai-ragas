package ai.qa.solutions.allure.model;

import static org.assertj.core.api.Assertions.assertThat;

import ai.qa.solutions.execution.listener.dto.ModelExclusionEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ModelExclusionData")
class ModelExclusionDataTest {

    @Nested
    @DisplayName("from")
    class From {

        @Test
        @DisplayName("should convert exclusion event with exception")
        void shouldConvertWithException() {
            final RuntimeException cause = new RuntimeException("API timeout");
            final ModelExclusionEvent event = ModelExclusionEvent.builder()
                    .modelId("model-1")
                    .failedStepName("GenerateStatements")
                    .failedStepIndex(0)
                    .cause(cause)
                    .build();

            final ModelExclusionData data = ModelExclusionData.from(event);

            assertThat(data.getModelId()).isEqualTo("model-1");
            assertThat(data.getFailedStepName()).isEqualTo("GenerateStatements");
            assertThat(data.getFailedStepIndex()).isZero();
            assertThat(data.getErrorMessage()).isEqualTo("API timeout");
            assertThat(data.getStackTrace()).contains("RuntimeException");
            assertThat(data.getStackTrace()).contains("API timeout");
        }

        @Test
        @DisplayName("should handle exception without message")
        void shouldHandleExceptionWithoutMessage() {
            final NullPointerException cause = new NullPointerException();
            final ModelExclusionEvent event = ModelExclusionEvent.builder()
                    .modelId("model-2")
                    .failedStepName("Step1")
                    .failedStepIndex(1)
                    .cause(cause)
                    .build();

            final ModelExclusionData data = ModelExclusionData.from(event);

            assertThat(data.getErrorMessage()).isEqualTo("NullPointerException");
        }

        @Test
        @DisplayName("should handle exception with blank message")
        void shouldHandleExceptionWithBlankMessage() {
            final RuntimeException cause = new RuntimeException("   ");
            final ModelExclusionEvent event = ModelExclusionEvent.builder()
                    .modelId("model-3")
                    .failedStepName("Step")
                    .failedStepIndex(0)
                    .cause(cause)
                    .build();

            final ModelExclusionData data = ModelExclusionData.from(event);

            // Blank message should fallback to nested cause or class name
            assertThat(data.getErrorMessage()).isEqualTo("RuntimeException");
        }

        @Test
        @DisplayName("should extract message from nested cause")
        void shouldExtractMessageFromNestedCause() {
            final RuntimeException rootCause = new RuntimeException("root error");
            final RuntimeException cause = new RuntimeException((String) null, rootCause);
            final ModelExclusionEvent event = ModelExclusionEvent.builder()
                    .modelId("model-4")
                    .failedStepName("Step")
                    .failedStepIndex(0)
                    .cause(cause)
                    .build();

            final ModelExclusionData data = ModelExclusionData.from(event);

            assertThat(data.getErrorMessage()).isEqualTo("root error");
        }

        @Test
        @DisplayName("should handle null cause")
        void shouldHandleNullCause() {
            final ModelExclusionEvent event = ModelExclusionEvent.builder()
                    .modelId("model-5")
                    .failedStepName("Step")
                    .failedStepIndex(2)
                    .cause(null)
                    .build();

            final ModelExclusionData data = ModelExclusionData.from(event);

            assertThat(data.getErrorMessage()).isEqualTo("Unknown error");
            assertThat(data.getStackTrace()).isEmpty();
        }

        @Test
        @DisplayName("should handle exception with null message and null nested cause")
        void shouldHandleNoMessageAndNoNestedCause() {
            final IllegalStateException cause = new IllegalStateException((String) null);
            final ModelExclusionEvent event = ModelExclusionEvent.builder()
                    .modelId("model-6")
                    .failedStepName("Step")
                    .failedStepIndex(0)
                    .cause(cause)
                    .build();

            final ModelExclusionData data = ModelExclusionData.from(event);

            assertThat(data.getErrorMessage()).isEqualTo("IllegalStateException");
        }
    }
}
