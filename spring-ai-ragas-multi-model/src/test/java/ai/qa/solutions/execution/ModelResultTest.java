package ai.qa.solutions.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ModelResult Tests")
class ModelResultTest {

    private static final String MODEL_ID = "test-model";
    private static final Duration DURATION = Duration.ofMillis(100);
    private static final String REQUEST = "test request";

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {

        @Test
        @DisplayName("success() should create successful result with all fields")
        void successShouldCreateSuccessfulResult() {
            // Given
            final String result = "test result";

            // When
            final ModelResult<String> modelResult = ModelResult.success(MODEL_ID, result, DURATION, REQUEST);

            // Then
            assertThat(modelResult.modelId()).isEqualTo(MODEL_ID);
            assertThat(modelResult.result()).isEqualTo(result);
            assertThat(modelResult.duration()).isEqualTo(DURATION);
            assertThat(modelResult.request()).isEqualTo(REQUEST);
            assertThat(modelResult.error()).isNull();
        }

        @Test
        @DisplayName("failure() should create failed result with error")
        void failureShouldCreateFailedResult() {
            // Given
            final Exception error = new RuntimeException("test error");

            // When
            final ModelResult<String> modelResult = ModelResult.failure(MODEL_ID, DURATION, REQUEST, error);

            // Then
            assertThat(modelResult.modelId()).isEqualTo(MODEL_ID);
            assertThat(modelResult.result()).isNull();
            assertThat(modelResult.duration()).isEqualTo(DURATION);
            assertThat(modelResult.request()).isEqualTo(REQUEST);
            assertThat(modelResult.error()).isEqualTo(error);
        }

        @Test
        @DisplayName("success() should handle null result value")
        void successShouldHandleNullResult() {
            // When
            final ModelResult<String> modelResult = ModelResult.success(MODEL_ID, null, DURATION, REQUEST);

            // Then
            assertThat(modelResult.isSuccess()).isTrue();
            assertThat(modelResult.result()).isNull();
        }
    }

    @Nested
    @DisplayName("Status Methods")
    class StatusMethods {

        @Test
        @DisplayName("isSuccess() should return true when no error")
        void isSuccessShouldReturnTrueWhenNoError() {
            // Given
            final ModelResult<String> modelResult = ModelResult.success(MODEL_ID, "result", DURATION, REQUEST);

            // When/Then
            assertThat(modelResult.isSuccess()).isTrue();
            assertThat(modelResult.isFailure()).isFalse();
        }

        @Test
        @DisplayName("isFailure() should return true when error present")
        void isFailureShouldReturnTrueWhenErrorPresent() {
            // Given
            final ModelResult<String> modelResult =
                    ModelResult.failure(MODEL_ID, DURATION, REQUEST, new RuntimeException("error"));

            // When/Then
            assertThat(modelResult.isFailure()).isTrue();
            assertThat(modelResult.isSuccess()).isFalse();
        }
    }

    @Nested
    @DisplayName("getResultOrThrow")
    class GetResultOrThrow {

        @Test
        @DisplayName("Should return result when successful")
        void shouldReturnResultWhenSuccessful() {
            // Given
            final String expectedResult = "expected value";
            final ModelResult<String> modelResult = ModelResult.success(MODEL_ID, expectedResult, DURATION, REQUEST);

            // When
            final String actualResult = modelResult.getResultOrThrow();

            // Then
            assertThat(actualResult).isEqualTo(expectedResult);
        }

        @Test
        @DisplayName("Should throw RuntimeException when failed")
        void shouldThrowRuntimeExceptionWhenFailed() {
            // Given
            final Exception originalError = new IllegalArgumentException("original error");
            final ModelResult<String> modelResult = ModelResult.failure(MODEL_ID, DURATION, REQUEST, originalError);

            // When/Then
            assertThatThrownBy(modelResult::getResultOrThrow)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining(MODEL_ID)
                    .hasMessageContaining("failed")
                    .hasCause(originalError);
        }

        @Test
        @DisplayName("Should return null when successful result is null")
        void shouldReturnNullWhenSuccessfulResultIsNull() {
            // Given
            final ModelResult<String> modelResult = ModelResult.success(MODEL_ID, null, DURATION, REQUEST);

            // When
            final String result = modelResult.getResultOrThrow();

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("map")
    class MapMethod {

        @Test
        @DisplayName("Should map successful result to new type")
        void shouldMapSuccessfulResultToNewType() {
            // Given
            final ModelResult<Integer> original = ModelResult.success(MODEL_ID, 42, DURATION, REQUEST);

            // When
            final ModelResult<String> mapped = original.map(String::valueOf);

            // Then
            assertThat(mapped.isSuccess()).isTrue();
            assertThat(mapped.result()).isEqualTo("42");
            assertThat(mapped.modelId()).isEqualTo(MODEL_ID);
            assertThat(mapped.duration()).isEqualTo(DURATION);
            assertThat(mapped.request()).isEqualTo(REQUEST);
            assertThat(mapped.error()).isNull();
        }

        @Test
        @DisplayName("Should preserve error on failed result")
        void shouldPreserveErrorOnFailedResult() {
            // Given
            final Exception originalError = new RuntimeException("original");
            final ModelResult<Integer> original = ModelResult.failure(MODEL_ID, DURATION, REQUEST, originalError);

            // When
            final ModelResult<String> mapped = original.map(String::valueOf);

            // Then
            assertThat(mapped.isFailure()).isTrue();
            assertThat(mapped.result()).isNull();
            assertThat(mapped.error()).isEqualTo(originalError);
            assertThat(mapped.modelId()).isEqualTo(MODEL_ID);
            assertThat(mapped.duration()).isEqualTo(DURATION);
            assertThat(mapped.request()).isEqualTo(REQUEST);
        }

        @Test
        @DisplayName("Should apply complex mapping function")
        void shouldApplyComplexMappingFunction() {
            // Given
            record Input(int value, String name) {}
            record Output(String description) {}

            final ModelResult<Input> original = ModelResult.success(MODEL_ID, new Input(42, "test"), DURATION, REQUEST);

            // When
            final ModelResult<Output> mapped = original.map(input -> new Output(input.name() + ": " + input.value()));

            // Then
            assertThat(mapped.isSuccess()).isTrue();
            assertThat(mapped.result().description()).isEqualTo("test: 42");
        }

        @Test
        @DisplayName("Should handle mapping to null")
        void shouldHandleMappingToNull() {
            // Given
            final ModelResult<String> original = ModelResult.success(MODEL_ID, "value", DURATION, REQUEST);

            // When
            final ModelResult<String> mapped = original.map(s -> null);

            // Then
            assertThat(mapped.isSuccess()).isTrue();
            assertThat(mapped.result()).isNull();
        }
    }

    @Nested
    @DisplayName("Record Equality and HashCode")
    class RecordEquality {

        @Test
        @DisplayName("Equal results should be equal")
        void equalResultsShouldBeEqual() {
            // Given
            final ModelResult<String> result1 = ModelResult.success(MODEL_ID, "value", DURATION, REQUEST);
            final ModelResult<String> result2 = ModelResult.success(MODEL_ID, "value", DURATION, REQUEST);

            // Then
            assertThat(result1).isEqualTo(result2);
            assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
        }

        @Test
        @DisplayName("Different results should not be equal")
        void differentResultsShouldNotBeEqual() {
            // Given
            final ModelResult<String> result1 = ModelResult.success(MODEL_ID, "value1", DURATION, REQUEST);
            final ModelResult<String> result2 = ModelResult.success(MODEL_ID, "value2", DURATION, REQUEST);

            // Then
            assertThat(result1).isNotEqualTo(result2);
        }

        @Test
        @DisplayName("Different model IDs should not be equal")
        void differentModelIdsShouldNotBeEqual() {
            // Given
            final ModelResult<String> result1 = ModelResult.success("model-1", "value", DURATION, REQUEST);
            final ModelResult<String> result2 = ModelResult.success("model-2", "value", DURATION, REQUEST);

            // Then
            assertThat(result1).isNotEqualTo(result2);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle zero duration")
        void shouldHandleZeroDuration() {
            // Given
            final Duration zeroDuration = Duration.ZERO;

            // When
            final ModelResult<String> result = ModelResult.success(MODEL_ID, "value", zeroDuration, REQUEST);

            // Then
            assertThat(result.duration()).isEqualTo(Duration.ZERO);
            assertThat(result.duration().toMillis()).isZero();
        }

        @Test
        @DisplayName("Should handle very long duration")
        void shouldHandleVeryLongDuration() {
            // Given
            final Duration longDuration = Duration.ofHours(1);

            // When
            final ModelResult<String> result = ModelResult.success(MODEL_ID, "value", longDuration, REQUEST);

            // Then
            assertThat(result.duration()).isEqualTo(longDuration);
        }

        @Test
        @DisplayName("Should handle empty model ID")
        void shouldHandleEmptyModelId() {
            // When
            final ModelResult<String> result = ModelResult.success("", "value", DURATION, REQUEST);

            // Then
            assertThat(result.modelId()).isEmpty();
        }

        @Test
        @DisplayName("Should handle empty request")
        void shouldHandleEmptyRequest() {
            // When
            final ModelResult<String> result = ModelResult.success(MODEL_ID, "value", DURATION, "");

            // Then
            assertThat(result.request()).isEmpty();
        }

        @Test
        @DisplayName("Should preserve error message and stack trace")
        void shouldPreserveErrorMessageAndStackTrace() {
            // Given
            final RuntimeException cause = new RuntimeException("cause");
            final IllegalStateException error = new IllegalStateException("error message", cause);

            // When
            final ModelResult<String> result = ModelResult.failure(MODEL_ID, DURATION, REQUEST, error);

            // Then
            assertThat(result.error()).isInstanceOf(IllegalStateException.class);
            assertThat(result.error().getMessage()).isEqualTo("error message");
            assertThat(result.error().getCause()).isEqualTo(cause);
        }

        @Test
        @DisplayName("Should handle result with complex generic type")
        void shouldHandleResultWithComplexGenericType() {
            // Given
            record ComplexType(String name, int[] values) {}
            final ComplexType complexValue = new ComplexType("test", new int[] {1, 2, 3});

            // When
            final ModelResult<ComplexType> result = ModelResult.success(MODEL_ID, complexValue, DURATION, REQUEST);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result().name()).isEqualTo("test");
            assertThat(result.result().values()).containsExactly(1, 2, 3);
        }
    }
}
