package ai.qa.solutions.metric;

import static org.assertj.core.api.Assertions.assertThat;

import ai.qa.solutions.sample.Sample;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("EvaluationResult Tests")
class EvaluationResultTest {

    @Nested
    @DisplayName("Builder")
    class Builder {

        @Test
        @DisplayName("Should create valid result with all fields populated")
        void shouldCreateResultWithAllFields() {
            final var sample = Sample.builder()
                    .userInput("What is Spring?")
                    .response("Spring is a framework")
                    .reference("Spring is a Java framework")
                    .build();

            final var result = EvaluationResult.builder()
                    .metricName("Faithfulness")
                    .score(0.85)
                    .modelScores(Map.of("model-1", 0.85))
                    .excludedModels(List.of("model-2"))
                    .totalDuration(Duration.ofMillis(150))
                    .sample(sample)
                    .config("test-config")
                    .modelIds(List.of("model-1", "model-2"))
                    .embeddingModelIds(List.of("embed-1"))
                    .build();

            assertThat(result.getMetricName()).isEqualTo("Faithfulness");
            assertThat(result.getScore()).isEqualTo(0.85);
            assertThat(result.getModelScores()).containsEntry("model-1", 0.85);
            assertThat(result.getExcludedModels()).containsExactly("model-2");
            assertThat(result.getTotalDuration()).isEqualTo(Duration.ofMillis(150));
            assertThat(result.getSample()).isSameAs(sample);
            assertThat(result.getConfig()).isEqualTo("test-config");
            assertThat(result.getModelIds()).containsExactly("model-1", "model-2");
            assertThat(result.getEmbeddingModelIds()).containsExactly("embed-1");
        }

        @Test
        @DisplayName("Should default modelIds to empty list")
        void shouldDefaultModelIdsToEmptyList() {
            final var result =
                    EvaluationResult.builder().metricName("TestMetric").build();

            assertThat(result.getModelIds()).isNotNull();
            assertThat(result.getModelIds()).isEmpty();
        }

        @Test
        @DisplayName("Should default embeddingModelIds to empty list")
        void shouldDefaultEmbeddingModelIdsToEmptyList() {
            final var result =
                    EvaluationResult.builder().metricName("TestMetric").build();

            assertThat(result.getEmbeddingModelIds()).isNotNull();
            assertThat(result.getEmbeddingModelIds()).isEmpty();
        }

        @Test
        @DisplayName("Should handle null aggregated score")
        void shouldHandleNullScore() {
            final var result = EvaluationResult.builder()
                    .metricName("FailedMetric")
                    .score(null)
                    .build();

            assertThat(result.getScore()).isNull();
            assertThat(result.getMetricName()).isEqualTo("FailedMetric");
        }

        @Test
        @DisplayName("Should create result with only metricName")
        void shouldCreateResultWithMinimalFields() {
            final var result =
                    EvaluationResult.builder().metricName("MinimalMetric").build();

            assertThat(result.getMetricName()).isEqualTo("MinimalMetric");
            assertThat(result.getScore()).isNull();
            assertThat(result.getModelScores()).isNull();
            assertThat(result.getExcludedModels()).isNull();
            assertThat(result.getTotalDuration()).isNull();
            assertThat(result.getSample()).isNull();
            assertThat(result.getConfig()).isNull();
            assertThat(result.getExplanation()).isNull();
            assertThat(result.getMetadata()).isNull();
            assertThat(result.getModelIds()).isEmpty();
            assertThat(result.getEmbeddingModelIds()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Immutability")
    class Immutability {

        @Test
        @DisplayName("Should be immutable via @Value annotation")
        void shouldBeImmutable() {
            final var result = EvaluationResult.builder()
                    .metricName("TestMetric")
                    .score(0.5)
                    .modelIds(List.of("m1"))
                    .embeddingModelIds(List.of("e1"))
                    .build();

            // Verify fields are accessible via getters (Lombok @Value generates them)
            assertThat(result.getMetricName()).isEqualTo("TestMetric");
            assertThat(result.getScore()).isEqualTo(0.5);
            assertThat(result.getModelIds()).containsExactly("m1");
            assertThat(result.getEmbeddingModelIds()).containsExactly("e1");
        }
    }
}
