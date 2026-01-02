package ai.qa.solutions.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ModelExecutionResult Tests")
class ModelExecutionResultTest {

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {

        @Test
        @DisplayName("Should create successful result with score")
        void shouldCreateSuccessfulResultWithScore() {
            // Given
            final ModelExecutionContext context = createContext();
            final Double score = 0.85;
            final String response = "test response";

            // When
            final ModelExecutionResult result = ModelExecutionResult.success(context, score, response);

            // Then
            assertThat(result.getContext()).isEqualTo(context);
            assertThat(result.getScore()).contains(score);
            assertThat(result.getRawResponse()).isEqualTo(response);
            assertThat(result.getError()).isNull();
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("Should create successful result with null score")
        void shouldCreateSuccessfulResultWithNullScore() {
            // Given
            final ModelExecutionContext context = createContext();

            // When
            final ModelExecutionResult result = ModelExecutionResult.success(context, null, "response");

            // Then
            assertThat(result.getScore()).isEmpty();
            assertThat(result.isSuccess()).isFalse(); // No score means not successful
        }

        @Test
        @DisplayName("Should create failed result")
        void shouldCreateFailedResult() {
            // Given
            final ModelExecutionContext context = createContext();
            final RuntimeException error = new RuntimeException("Test error");

            // When
            final ModelExecutionResult result = ModelExecutionResult.failure(context, error);

            // Then
            assertThat(result.getContext()).isEqualTo(context);
            assertThat(result.getScore()).isEmpty();
            assertThat(result.getRawResponse()).isNull();
            assertThat(result.getError()).isEqualTo(error);
            assertThat(result.isSuccess()).isFalse();
        }
    }

    @Nested
    @DisplayName("Success Detection")
    class SuccessDetection {

        @Test
        @DisplayName("Should detect success when score present and no error")
        void shouldDetectSuccessWhenScorePresentAndNoError() {
            // Given
            final ModelExecutionContext context = createContext();

            // When
            final ModelExecutionResult result = ModelExecutionResult.success(context, 0.5, "response");

            // Then
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("Should detect failure when score absent")
        void shouldDetectFailureWhenScoreAbsent() {
            // Given
            final ModelExecutionContext context = createContext();

            // When
            final ModelExecutionResult result = ModelExecutionResult.success(context, null, "response");

            // Then
            assertThat(result.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("Should detect failure when error present")
        void shouldDetectFailureWhenErrorPresent() {
            // Given
            final ModelExecutionContext context = createContext();

            // When
            final ModelExecutionResult result = ModelExecutionResult.failure(context, new RuntimeException());

            // Then
            assertThat(result.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("Should detect failure when both score and error present")
        void shouldDetectFailureWhenBothScoreAndErrorPresent() {
            // Given
            final ModelExecutionContext context = createContext();
            final RuntimeException error = new RuntimeException("error");

            // When
            final ModelExecutionResult result = ModelExecutionResult.builder()
                    .context(context)
                    .score(java.util.Optional.of(0.5))
                    .error(error)
                    .build();

            // Then
            assertThat(result.isSuccess()).isFalse(); // Error takes precedence
        }
    }

    @Nested
    @DisplayName("Duration Calculation")
    class DurationCalculation {

        @Test
        @DisplayName("Should calculate duration correctly")
        void shouldCalculateDurationCorrectly() throws InterruptedException {
            // Given
            final ModelExecutionContext context = createContext();
            Thread.sleep(50); // Sleep for 50ms

            // When
            final ModelExecutionResult result = ModelExecutionResult.success(context, 0.5, "response");
            final Duration duration = result.getDuration();

            // Then
            assertThat(duration).isNotNull();
            assertThat(duration.toMillis()).isGreaterThanOrEqualTo(50);
            assertThat(duration.toMillis()).isLessThan(1000); // Reasonable upper bound
        }

        @Test
        @DisplayName("Should have positive duration")
        void shouldHavePositiveDuration() {
            // Given
            final ModelExecutionContext context = createContext();

            // When
            final ModelExecutionResult result = ModelExecutionResult.success(context, 0.5, "response");

            // Then
            assertThat(result.getDuration()).isPositive();
        }
    }

    @Nested
    @DisplayName("Timestamps")
    class Timestamps {

        @Test
        @DisplayName("Should have default completedAt timestamp")
        void shouldHaveDefaultCompletedAtTimestamp() {
            // Given
            final ModelExecutionContext context = createContext();
            final Instant before = Instant.now();

            // When
            final ModelExecutionResult result = ModelExecutionResult.success(context, 0.5, "response");

            // Then
            final Instant after = Instant.now();
            assertThat(result.getCompletedAt()).isBetween(before, after);
        }

        @Test
        @DisplayName("Should complete after context started")
        void shouldCompleteAfterContextStarted() {
            // Given
            final ModelExecutionContext context = createContext();

            // When
            final ModelExecutionResult result = ModelExecutionResult.success(context, 0.5, "response");

            // Then
            assertThat(result.getCompletedAt()).isAfterOrEqualTo(context.getStartedAt());
        }
    }

    @Nested
    @DisplayName("Raw Response Storage")
    class RawResponseStorage {

        @Test
        @DisplayName("Should store various response types")
        void shouldStoreVariousResponseTypes() {
            // Given
            final ModelExecutionContext context = createContext();

            // When/Then - String response
            final ModelExecutionResult stringResult = ModelExecutionResult.success(context, 0.5, "string response");
            assertThat(stringResult.getRawResponse()).isEqualTo("string response");

            // When/Then - Object response
            final TestResponse objectResponse = new TestResponse(0.5, "details");
            final ModelExecutionResult objectResult = ModelExecutionResult.success(context, 0.5, objectResponse);
            assertThat(objectResult.getRawResponse()).isEqualTo(objectResponse);

            // When/Then - Null response
            final ModelExecutionResult nullResult = ModelExecutionResult.success(context, 0.5, null);
            assertThat(nullResult.getRawResponse()).isNull();
        }
    }

    @Nested
    @DisplayName("Builder Pattern")
    class BuilderPattern {

        @Test
        @DisplayName("Should build with all properties")
        void shouldBuildWithAllProperties() {
            // Given
            final ModelExecutionContext context = createContext();
            final Double score = 0.75;
            final String response = "response";
            final RuntimeException error = new RuntimeException("error");
            final Instant completedAt = Instant.now();

            // When
            final ModelExecutionResult result = ModelExecutionResult.builder()
                    .context(context)
                    .score(java.util.Optional.of(score))
                    .rawResponse(response)
                    .error(error)
                    .completedAt(completedAt)
                    .build();

            // Then
            assertThat(result.getContext()).isEqualTo(context);
            assertThat(result.getScore()).contains(score);
            assertThat(result.getRawResponse()).isEqualTo(response);
            assertThat(result.getError()).isEqualTo(error);
            assertThat(result.getCompletedAt()).isEqualTo(completedAt);
        }
    }

    // ========== Helper Methods ==========

    private ModelExecutionContext createContext() {
        return ModelExecutionContext.builder()
                .modelId("test-model")
                .metricName("TestMetric")
                .prompt("test prompt")
                .build();
    }

    private record TestResponse(double score, String details) {}
}
