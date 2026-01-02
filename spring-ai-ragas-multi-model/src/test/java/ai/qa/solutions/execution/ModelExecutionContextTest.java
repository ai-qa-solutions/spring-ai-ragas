package ai.qa.solutions.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ModelExecutionContext Tests")
class ModelExecutionContextTest {

    @Nested
    @DisplayName("Builder and Properties")
    class BuilderAndProperties {

        @Test
        @DisplayName("Should build with all properties")
        void shouldBuildWithAllProperties() {
            // Given/When
            final ModelExecutionContext context = ModelExecutionContext.builder()
                    .executionId("test-id")
                    .modelId("gpt-4")
                    .metricName("AspectCritic")
                    .prompt("Test prompt")
                    .metadata(Map.of("key", "value"))
                    .build();

            // Then
            assertThat(context.getExecutionId()).isEqualTo("test-id");
            assertThat(context.getModelId()).isEqualTo("gpt-4");
            assertThat(context.getMetricName()).isEqualTo("AspectCritic");
            assertThat(context.getPrompt()).isEqualTo("Test prompt");
            assertThat(context.getMetadata()).containsEntry("key", "value");
            assertThat(context.getStartedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should generate unique execution ID by default")
        void shouldGenerateUniqueExecutionIdByDefault() {
            // Given/When
            final ModelExecutionContext context1 = ModelExecutionContext.builder()
                    .modelId("model-1")
                    .metricName("TestMetric")
                    .prompt("prompt")
                    .build();

            final ModelExecutionContext context2 = ModelExecutionContext.builder()
                    .modelId("model-2")
                    .metricName("TestMetric")
                    .prompt("prompt")
                    .build();

            // Then
            assertThat(context1.getExecutionId()).isNotBlank();
            assertThat(context2.getExecutionId()).isNotBlank();
            assertThat(context1.getExecutionId()).isNotEqualTo(context2.getExecutionId());
        }

        @Test
        @DisplayName("Should have default startedAt timestamp")
        void shouldHaveDefaultStartedAtTimestamp() {
            // Given
            final Instant before = Instant.now();

            // When
            final ModelExecutionContext context = ModelExecutionContext.builder()
                    .modelId("model-1")
                    .metricName("TestMetric")
                    .prompt("prompt")
                    .build();

            // Then
            final Instant after = Instant.now();
            assertThat(context.getStartedAt()).isBetween(before, after);
        }

        @Test
        @DisplayName("Should have empty metadata by default")
        void shouldHaveEmptyMetadataByDefault() {
            // Given/When
            final ModelExecutionContext context = ModelExecutionContext.builder()
                    .modelId("model-1")
                    .metricName("TestMetric")
                    .prompt("prompt")
                    .build();

            // Then
            assertThat(context.getMetadata()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Metadata Management")
    class MetadataManagement {

        @Test
        @DisplayName("Should add metadata to existing context")
        void shouldAddMetadataToExistingContext() {
            // Given
            final ModelExecutionContext original = ModelExecutionContext.builder()
                    .modelId("model-1")
                    .metricName("TestMetric")
                    .prompt("prompt")
                    .metadata(Map.of("existing", "value"))
                    .build();

            // When
            final ModelExecutionContext updated = original.withMetadata("new", "data");

            // Then
            assertThat(updated.getMetadata()).hasSize(2);
            assertThat(updated.getMetadata()).containsEntry("existing", "value");
            assertThat(updated.getMetadata()).containsEntry("new", "data");
        }

        @Test
        @DisplayName("Should not modify original context when adding metadata")
        void shouldNotModifyOriginalContextWhenAddingMetadata() {
            // Given
            final ModelExecutionContext original = ModelExecutionContext.builder()
                    .modelId("model-1")
                    .metricName("TestMetric")
                    .prompt("prompt")
                    .metadata(Map.of("key", "value"))
                    .build();

            // When
            final ModelExecutionContext updated = original.withMetadata("new", "data");

            // Then
            assertThat(original.getMetadata()).hasSize(1);
            assertThat(original.getMetadata()).containsOnlyKeys("key");
            assertThat(updated.getMetadata()).hasSize(2);
        }

        @Test
        @DisplayName("Should preserve other properties when adding metadata")
        void shouldPreserveOtherPropertiesWhenAddingMetadata() {
            // Given
            final ModelExecutionContext original = ModelExecutionContext.builder()
                    .executionId("test-id")
                    .modelId("model-1")
                    .metricName("TestMetric")
                    .prompt("prompt")
                    .build();

            // When
            final ModelExecutionContext updated = original.withMetadata("key", "value");

            // Then
            assertThat(updated.getExecutionId()).isEqualTo(original.getExecutionId());
            assertThat(updated.getModelId()).isEqualTo(original.getModelId());
            assertThat(updated.getMetricName()).isEqualTo(original.getMetricName());
            assertThat(updated.getPrompt()).isEqualTo(original.getPrompt());
            assertThat(updated.getStartedAt()).isEqualTo(original.getStartedAt());
        }

        @Test
        @DisplayName("Should create immutable metadata map")
        void shouldCreateImmutableMetadataMap() {
            // Given
            final ModelExecutionContext context = ModelExecutionContext.builder()
                    .modelId("model-1")
                    .metricName("TestMetric")
                    .prompt("prompt")
                    .build();

            // When
            final ModelExecutionContext updated = context.withMetadata("key", "value");

            // Then
            assertThat(updated.getMetadata()).isInstanceOf(Map.class);
            assertThatThrownBy(() -> updated.getMetadata().put("new", "value"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("Should handle various value types in metadata")
        void shouldHandleVariousValueTypesInMetadata() {
            // Given
            final ModelExecutionContext context = ModelExecutionContext.builder()
                    .modelId("model-1")
                    .metricName("TestMetric")
                    .prompt("prompt")
                    .build();

            // When
            final ModelExecutionContext updated = context.withMetadata("string", "value")
                    .withMetadata("number", 42)
                    .withMetadata("boolean", true)
                    .withMetadata("list", java.util.List.of("a", "b"));

            // Then
            assertThat(updated.getMetadata()).hasSize(4);
            assertThat(updated.getMetadata().get("string")).isEqualTo("value");
            assertThat(updated.getMetadata().get("number")).isEqualTo(42);
            assertThat(updated.getMetadata().get("boolean")).isEqualTo(true);
            assertThat(updated.getMetadata().get("list")).isEqualTo(java.util.List.of("a", "b"));
        }
    }

    @Nested
    @DisplayName("Immutability")
    class Immutability {

        @Test
        @DisplayName("Should be immutable value object")
        void shouldBeImmutableValueObject() {
            // Given
            final ModelExecutionContext context1 = ModelExecutionContext.builder()
                    .executionId("id-1")
                    .modelId("model-1")
                    .metricName("TestMetric")
                    .prompt("prompt")
                    .build();

            final ModelExecutionContext context2 = ModelExecutionContext.builder()
                    .executionId("id-1")
                    .modelId("model-1")
                    .metricName("TestMetric")
                    .prompt("prompt")
                    .startedAt(context1.getStartedAt())
                    .build();

            // Then
            assertThat(context1).isEqualTo(context2);
            assertThat(context1.hashCode()).isEqualTo(context2.hashCode());
        }
    }
}
