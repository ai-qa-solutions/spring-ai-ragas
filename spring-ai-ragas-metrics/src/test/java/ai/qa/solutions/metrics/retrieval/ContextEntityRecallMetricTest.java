package ai.qa.solutions.metrics.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import ai.qa.solutions.chatclient.ChatClientStore;
import ai.qa.solutions.execution.MultiModelExecutor;
import ai.qa.solutions.execution.StubMultiModelExecutor;
import ai.qa.solutions.sample.Sample;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

@DisplayName("ContextEntityRecallMetric Tests")
class ContextEntityRecallMetricTest {

    private MultiModelExecutor executor;

    @BeforeEach
    void setUp() {
        ChatClient mockClient = mock(ChatClient.class);
        ChatClientStore store = new ChatClientStore(Map.of("model-1", mockClient), mockClient);
        executor = new MultiModelExecutor(store, null, new SimpleAsyncTaskExecutor());
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("Should create metric with executor")
        void shouldCreateWithExecutor() {
            ContextEntityRecallMetric metric =
                    ContextEntityRecallMetric.builder().executor(executor).build();

            assertThat(metric).isNotNull();
            assertThat(metric.getName()).isEqualTo("ContextEntityRecallMetric");
        }

        @Test
        @DisplayName("Should allow custom entity extraction prompt")
        void shouldAllowCustomEntityExtractionPrompt() {
            String customPrompt = "Custom extraction: {text}";

            ContextEntityRecallMetric metric = ContextEntityRecallMetric.builder()
                    .executor(executor)
                    .entityExtractionPrompt(customPrompt)
                    .build();

            assertThat(metric).isNotNull();
        }

        @Test
        @DisplayName("toBuilder should preserve settings")
        void toBuilderShouldPreserveSettings() {
            ContextEntityRecallMetric original =
                    ContextEntityRecallMetric.builder().executor(executor).build();

            ContextEntityRecallMetric copy = original.toBuilder().build();

            assertThat(copy).isNotSameAs(original);
            assertThat(copy.getName()).isEqualTo(original.getName());
        }
    }

    @Nested
    @DisplayName("EntitiesResponse Record")
    class EntitiesResponseTests {

        @Test
        @DisplayName("Should store entities list")
        void shouldStoreEntitiesList() {
            List<String> entities = List.of("Paris", "France", "1889");
            ContextEntityRecallMetric.EntitiesResponse response =
                    new ContextEntityRecallMetric.EntitiesResponse(entities);

            assertThat(response.entities()).containsExactly("Paris", "France", "1889");
        }

        @Test
        @DisplayName("Should handle empty entities")
        void shouldHandleEmptyEntities() {
            ContextEntityRecallMetric.EntitiesResponse response =
                    new ContextEntityRecallMetric.EntitiesResponse(List.of());

            assertThat(response.entities()).isEmpty();
        }

        @Test
        @DisplayName("Should handle null entities")
        void shouldHandleNullEntities() {
            ContextEntityRecallMetric.EntitiesResponse response = new ContextEntityRecallMetric.EntitiesResponse(null);

            assertThat(response.entities()).isNull();
        }
    }

    @Nested
    @DisplayName("ContextEntityRecallConfig")
    class ConfigTests {

        @Test
        @DisplayName("Should allow empty config")
        void shouldAllowEmptyConfig() {
            ContextEntityRecallMetric.ContextEntityRecallConfig config =
                    ContextEntityRecallMetric.ContextEntityRecallConfig.builder()
                            .build();

            assertThat(config).isNotNull();
            assertThat(config.getModels()).isEmpty();
        }

        @Test
        @DisplayName("Should allow specifying models")
        void shouldAllowSpecifyingModels() {
            ContextEntityRecallMetric.ContextEntityRecallConfig config =
                    ContextEntityRecallMetric.ContextEntityRecallConfig.builder()
                            .model("gpt-4")
                            .model("claude-3")
                            .build();

            assertThat(config.getModels()).containsExactly("gpt-4", "claude-3");
        }
    }

    @Nested
    @DisplayName("Default Prompt Template")
    class PromptTemplateTests {

        @Test
        @DisplayName("Should contain required placeholder")
        void shouldContainRequiredPlaceholder() {
            assertThat(ContextEntityRecallMetric.DEFAULT_ENTITY_EXTRACTION_PROMPT)
                    .contains("{text}");
        }

        @Test
        @DisplayName("Should contain entity types instructions")
        void shouldContainEntityTypesInstructions() {
            assertThat(ContextEntityRecallMetric.DEFAULT_ENTITY_EXTRACTION_PROMPT)
                    .contains("Person names")
                    .contains("Place names")
                    .contains("Organizations")
                    .contains("Dates");
        }

        @Test
        @DisplayName("Should contain examples")
        void shouldContainExamples() {
            assertThat(ContextEntityRecallMetric.DEFAULT_ENTITY_EXTRACTION_PROMPT)
                    .contains("Eiffel Tower")
                    .contains("Paris")
                    .contains("Neil Armstrong");
        }
    }

    @Nested
    @DisplayName("Scoring")
    class ScoringTests {

        @Test
        @DisplayName("Should return 1.0 when all entities match")
        void shouldReturn1WhenAllEntitiesMatch() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            ContextEntityRecallMetric.EntitiesResponse.class,
                            new ContextEntityRecallMetric.EntitiesResponse(List.of("Paris", "France", "1889")));

            ContextEntityRecallMetric metric =
                    ContextEntityRecallMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .reference("The Eiffel Tower is in Paris, France, built in 1889.")
                    .retrievedContexts(List.of("Paris, France - home of the Eiffel Tower since 1889."))
                    .build();

            Double score = metric.singleTurnScore(sample);

            // Same entities returned for both reference and context, so 100% recall
            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should return 0.0 when reference is missing")
        void shouldReturn0WhenNoReference() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"));

            ContextEntityRecallMetric metric =
                    ContextEntityRecallMetric.builder().executor(stubExecutor).build();

            Sample sample =
                    Sample.builder().retrievedContexts(List.of("Some context")).build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 0.0 when context is missing")
        void shouldReturn0WhenNoContext() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"));

            ContextEntityRecallMetric metric =
                    ContextEntityRecallMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder().reference("Some reference").build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 0.0 when extracted entities are empty")
        void shouldReturn0WhenEmptyEntities() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            ContextEntityRecallMetric.EntitiesResponse.class,
                            new ContextEntityRecallMetric.EntitiesResponse(List.of()));

            ContextEntityRecallMetric metric =
                    ContextEntityRecallMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .reference("Some reference")
                    .retrievedContexts(List.of("Some context"))
                    .build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 0.0 when extracted entities are null")
        void shouldReturn0WhenNullEntities() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            ContextEntityRecallMetric.EntitiesResponse.class,
                            new ContextEntityRecallMetric.EntitiesResponse(null));

            ContextEntityRecallMetric metric =
                    ContextEntityRecallMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .reference("Some reference")
                    .retrievedContexts(List.of("Some context"))
                    .build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should throw when all models fail")
        void shouldThrowWhenAllModelsFail() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withModelError("model-1", new RuntimeException("Model failed"));

            ContextEntityRecallMetric metric =
                    ContextEntityRecallMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .reference("Some reference")
                    .retrievedContexts(List.of("Some context"))
                    .build();

            assertThatThrownBy(() -> metric.singleTurnScore(sample))
                    .hasCauseInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("All models failed");
        }

        @Test
        @DisplayName("Should work with async scoring")
        void shouldWorkWithAsyncScoring() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            ContextEntityRecallMetric.EntitiesResponse.class,
                            new ContextEntityRecallMetric.EntitiesResponse(List.of("Paris", "France")));

            ContextEntityRecallMetric metric =
                    ContextEntityRecallMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .reference("Paris, France")
                    .retrievedContexts(List.of("Paris, France"))
                    .build();

            Double score = metric.singleTurnScoreAsync(sample).join();

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should work with config and specified models")
        void shouldWorkWithConfigAndModels() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1", "model-2"))
                    .withResponse(
                            ContextEntityRecallMetric.EntitiesResponse.class,
                            new ContextEntityRecallMetric.EntitiesResponse(List.of("Entity1", "Entity2")));

            ContextEntityRecallMetric metric =
                    ContextEntityRecallMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .reference("Reference text")
                    .retrievedContexts(List.of("Context text"))
                    .build();

            ContextEntityRecallMetric.ContextEntityRecallConfig config =
                    ContextEntityRecallMetric.ContextEntityRecallConfig.builder()
                            .model("model-1")
                            .build();

            Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should handle case-insensitive entity matching")
        void shouldHandleCaseInsensitiveMatching() {
            // Both extractions return same entities (just different case representations)
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            ContextEntityRecallMetric.EntitiesResponse.class,
                            new ContextEntityRecallMetric.EntitiesResponse(List.of("PARIS", "france")));

            ContextEntityRecallMetric metric =
                    ContextEntityRecallMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .reference("Reference")
                    .retrievedContexts(List.of("Context"))
                    .build();

            Double score = metric.singleTurnScore(sample);

            // Since same entities are returned for both, recall should be 1.0
            assertThat(score).isEqualTo(1.0);
        }
    }
}
