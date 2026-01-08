package ai.qa.solutions.metrics.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
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

@DisplayName("ContextRecallMetric Tests")
class ContextRecallMetricTest {

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
            ContextRecallMetric metric =
                    ContextRecallMetric.builder().executor(executor).build();

            assertThat(metric).isNotNull();
            assertThat(metric.getName()).isEqualTo("ContextRecallMetric");
        }

        @Test
        @DisplayName("Should allow custom prompt template")
        void shouldAllowCustomPromptTemplate() {
            String customPrompt = "Custom: {question} {context} {reference_answer}";

            ContextRecallMetric metric = ContextRecallMetric.builder()
                    .executor(executor)
                    .contextRecallPrompt(customPrompt)
                    .build();

            assertThat(metric).isNotNull();
        }

        @Test
        @DisplayName("toBuilder should preserve settings")
        void toBuilderShouldPreserveSettings() {
            ContextRecallMetric original =
                    ContextRecallMetric.builder().executor(executor).build();

            ContextRecallMetric copy = original.toBuilder().build();

            assertThat(copy).isNotSameAs(original);
            assertThat(copy.getName()).isEqualTo(original.getName());
        }
    }

    @Nested
    @DisplayName("ContextRecallClassification Record")
    class ClassificationTests {

        @Test
        @DisplayName("Should store all fields")
        void shouldStoreAllFields() {
            ContextRecallMetric.ContextRecallClassification classification =
                    new ContextRecallMetric.ContextRecallClassification(
                            "The Eiffel Tower is in Paris.", "Found in context", 1);

            assertThat(classification.statement()).isEqualTo("The Eiffel Tower is in Paris.");
            assertThat(classification.reason()).isEqualTo("Found in context");
            assertThat(classification.attributed()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should handle not attributed statement")
        void shouldHandleNotAttributedStatement() {
            ContextRecallMetric.ContextRecallClassification classification =
                    new ContextRecallMetric.ContextRecallClassification("Some statement", "Not found in context", 0);

            assertThat(classification.attributed()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should handle null values")
        void shouldHandleNullValues() {
            ContextRecallMetric.ContextRecallClassification classification =
                    new ContextRecallMetric.ContextRecallClassification(null, null, null);

            assertThat(classification.statement()).isNull();
            assertThat(classification.reason()).isNull();
            assertThat(classification.attributed()).isNull();
        }
    }

    @Nested
    @DisplayName("ContextRecallClassifications Record")
    class ClassificationsTests {

        @Test
        @DisplayName("Should store classifications list")
        void shouldStoreClassificationsList() {
            List<ContextRecallMetric.ContextRecallClassification> list = List.of(
                    new ContextRecallMetric.ContextRecallClassification("stmt1", "reason1", 1),
                    new ContextRecallMetric.ContextRecallClassification("stmt2", "reason2", 0));

            ContextRecallMetric.ContextRecallClassifications classifications =
                    new ContextRecallMetric.ContextRecallClassifications(list);

            assertThat(classifications.classifications()).hasSize(2);
        }

        @Test
        @DisplayName("Should handle empty list")
        void shouldHandleEmptyList() {
            ContextRecallMetric.ContextRecallClassifications classifications =
                    new ContextRecallMetric.ContextRecallClassifications(List.of());

            assertThat(classifications.classifications()).isEmpty();
        }

        @Test
        @DisplayName("Should handle null list")
        void shouldHandleNullList() {
            ContextRecallMetric.ContextRecallClassifications classifications =
                    new ContextRecallMetric.ContextRecallClassifications(null);

            assertThat(classifications.classifications()).isNull();
        }
    }

    @Nested
    @DisplayName("ContextRecallConfig")
    class ConfigTests {

        @Test
        @DisplayName("Should allow empty config")
        void shouldAllowEmptyConfig() {
            ContextRecallMetric.ContextRecallConfig config =
                    ContextRecallMetric.ContextRecallConfig.builder().build();

            assertThat(config).isNotNull();
            assertThat(config.getModels()).isEmpty();
        }

        @Test
        @DisplayName("Should allow specifying models")
        void shouldAllowSpecifyingModels() {
            ContextRecallMetric.ContextRecallConfig config = ContextRecallMetric.ContextRecallConfig.builder()
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
        @DisplayName("Should contain required placeholders")
        void shouldContainRequiredPlaceholders() {
            assertThat(ContextRecallMetric.DEFAULT_CONTEXT_RECALL_PROMPT)
                    .contains("{question}")
                    .contains("{context}")
                    .contains("{reference_answer}");
        }

        @Test
        @DisplayName("Should contain classification instructions")
        void shouldContainClassificationInstructions() {
            assertThat(ContextRecallMetric.DEFAULT_CONTEXT_RECALL_PROMPT)
                    .contains("attributed")
                    .contains("1")
                    .contains("0")
                    .contains("statement")
                    .contains("reason");
        }
    }

    @Nested
    @DisplayName("Scoring")
    class ScoringTests {

        @Test
        @DisplayName("Should return 1.0 when all statements are attributed")
        void shouldReturn1WhenAllAttributed() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            ContextRecallMetric.ContextRecallClassifications.class,
                            new ContextRecallMetric.ContextRecallClassifications(List.of(
                                    new ContextRecallMetric.ContextRecallClassification("stmt1", "found", 1),
                                    new ContextRecallMetric.ContextRecallClassification("stmt2", "found", 1))));

            ContextRecallMetric metric =
                    ContextRecallMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("What is Java?")
                    .reference("Java is a programming language.")
                    .retrievedContexts(List.of("Java is a high-level programming language."))
                    .build();

            ContextRecallMetric.ContextRecallConfig config =
                    ContextRecallMetric.ContextRecallConfig.builder().build();

            Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should return 0.0 when no statements are attributed")
        void shouldReturn0WhenNoneAttributed() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            ContextRecallMetric.ContextRecallClassifications.class,
                            new ContextRecallMetric.ContextRecallClassifications(List.of(
                                    new ContextRecallMetric.ContextRecallClassification("stmt1", "not found", 0),
                                    new ContextRecallMetric.ContextRecallClassification("stmt2", "not found", 0))));

            ContextRecallMetric metric =
                    ContextRecallMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("What is Java?")
                    .reference("Java is a coffee brand.")
                    .retrievedContexts(List.of("Java is a programming language."))
                    .build();

            ContextRecallMetric.ContextRecallConfig config =
                    ContextRecallMetric.ContextRecallConfig.builder().build();

            Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 0.5 when half statements are attributed")
        void shouldReturn05WhenHalfAttributed() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            ContextRecallMetric.ContextRecallClassifications.class,
                            new ContextRecallMetric.ContextRecallClassifications(List.of(
                                    new ContextRecallMetric.ContextRecallClassification("stmt1", "found", 1),
                                    new ContextRecallMetric.ContextRecallClassification("stmt2", "not found", 0))));

            ContextRecallMetric metric =
                    ContextRecallMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("Question")
                    .reference("Reference")
                    .retrievedContexts(List.of("Context"))
                    .build();

            ContextRecallMetric.ContextRecallConfig config =
                    ContextRecallMetric.ContextRecallConfig.builder().build();

            Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isCloseTo(0.5, within(0.01));
        }

        @Test
        @DisplayName("Should return 0.0 when classifications list is empty")
        void shouldReturn0WhenEmptyClassifications() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            ContextRecallMetric.ContextRecallClassifications.class,
                            new ContextRecallMetric.ContextRecallClassifications(List.of()));

            ContextRecallMetric metric =
                    ContextRecallMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("Question")
                    .reference("Reference")
                    .retrievedContexts(List.of("Context"))
                    .build();

            ContextRecallMetric.ContextRecallConfig config =
                    ContextRecallMetric.ContextRecallConfig.builder().build();

            Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 0.0 when reference is missing")
        void shouldReturn0WhenNoReference() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"));

            ContextRecallMetric metric =
                    ContextRecallMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("Question")
                    .retrievedContexts(List.of("Context"))
                    .build();

            ContextRecallMetric.ContextRecallConfig config =
                    ContextRecallMetric.ContextRecallConfig.builder().build();

            Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 0.0 when context is missing")
        void shouldReturn0WhenNoContext() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"));

            ContextRecallMetric metric =
                    ContextRecallMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("Question")
                    .reference("Reference")
                    .build();

            ContextRecallMetric.ContextRecallConfig config =
                    ContextRecallMetric.ContextRecallConfig.builder().build();

            Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 0.0 when user input is missing")
        void shouldReturn0WhenNoUserInput() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"));

            ContextRecallMetric metric =
                    ContextRecallMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .reference("Reference")
                    .retrievedContexts(List.of("Context"))
                    .build();

            ContextRecallMetric.ContextRecallConfig config =
                    ContextRecallMetric.ContextRecallConfig.builder().build();

            Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should throw when all models fail")
        void shouldThrowWhenAllModelsFail() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withModelError("model-1", new RuntimeException("Model failed"));

            ContextRecallMetric metric =
                    ContextRecallMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("Question")
                    .reference("Reference")
                    .retrievedContexts(List.of("Context"))
                    .build();

            ContextRecallMetric.ContextRecallConfig config =
                    ContextRecallMetric.ContextRecallConfig.builder().build();

            assertThatThrownBy(() -> metric.singleTurnScore(config, sample))
                    .hasCauseInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("All models failed");
        }
    }
}
