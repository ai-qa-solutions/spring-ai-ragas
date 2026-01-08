package ai.qa.solutions.metrics.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
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

@DisplayName("ContextPrecisionMetric Tests")
class ContextPrecisionMetricTest {

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
            ContextPrecisionMetric metric =
                    ContextPrecisionMetric.builder().executor(executor).build();

            assertThat(metric).isNotNull();
            assertThat(metric.getName()).isEqualTo("ContextPrecisionMetric");
        }

        @Test
        @DisplayName("Should allow custom reference prompt")
        void shouldAllowCustomReferencePrompt() {
            String customPrompt = "Custom: {user_input} {reference} {context_chunk}";

            ContextPrecisionMetric metric = ContextPrecisionMetric.builder()
                    .executor(executor)
                    .withReferencePrompt(customPrompt)
                    .build();

            assertThat(metric).isNotNull();
        }

        @Test
        @DisplayName("Should allow custom non-reference prompt")
        void shouldAllowCustomNonReferencePrompt() {
            String customPrompt = "Custom: {user_input} {response} {context_chunk}";

            ContextPrecisionMetric metric = ContextPrecisionMetric.builder()
                    .executor(executor)
                    .withoutReferencePrompt(customPrompt)
                    .build();

            assertThat(metric).isNotNull();
        }

        @Test
        @DisplayName("toBuilder should preserve settings")
        void toBuilderShouldPreserveSettings() {
            ContextPrecisionMetric original =
                    ContextPrecisionMetric.builder().executor(executor).build();

            ContextPrecisionMetric copy = original.toBuilder().build();

            assertThat(copy).isNotSameAs(original);
            assertThat(copy.getName()).isEqualTo(original.getName());
        }
    }

    @Nested
    @DisplayName("RelevanceResponse Record")
    class RelevanceResponseTests {

        @Test
        @DisplayName("Should store all fields")
        void shouldStoreAllFields() {
            ContextPrecisionMetric.RelevanceResponse response =
                    new ContextPrecisionMetric.RelevanceResponse(true, "The context is relevant");

            assertThat(response.relevant()).isTrue();
            assertThat(response.reasoning()).isEqualTo("The context is relevant");
        }

        @Test
        @DisplayName("Should handle false relevance")
        void shouldHandleFalseRelevance() {
            ContextPrecisionMetric.RelevanceResponse response =
                    new ContextPrecisionMetric.RelevanceResponse(false, "Not relevant");

            assertThat(response.relevant()).isFalse();
        }

        @Test
        @DisplayName("Should handle null values")
        void shouldHandleNullValues() {
            ContextPrecisionMetric.RelevanceResponse response =
                    new ContextPrecisionMetric.RelevanceResponse(null, null);

            assertThat(response.relevant()).isNull();
            assertThat(response.reasoning()).isNull();
        }
    }

    @Nested
    @DisplayName("EvaluationStrategy Enum")
    class EvaluationStrategyTests {

        @Test
        @DisplayName("Should have REFERENCE_BASED strategy")
        void shouldHaveReferenceBasedStrategy() {
            assertThat(ContextPrecisionMetric.EvaluationStrategy.REFERENCE_BASED)
                    .isNotNull();
        }

        @Test
        @DisplayName("Should have RESPONSE_BASED strategy")
        void shouldHaveResponseBasedStrategy() {
            assertThat(ContextPrecisionMetric.EvaluationStrategy.RESPONSE_BASED).isNotNull();
        }

        @Test
        @DisplayName("Should have exactly two strategies")
        void shouldHaveExactlyTwoStrategies() {
            assertThat(ContextPrecisionMetric.EvaluationStrategy.values()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("ContextPrecisionConfig")
    class ConfigTests {

        @Test
        @DisplayName("Should allow empty config")
        void shouldAllowEmptyConfig() {
            ContextPrecisionMetric.ContextPrecisionConfig config =
                    ContextPrecisionMetric.ContextPrecisionConfig.builder().build();

            assertThat(config).isNotNull();
            assertThat(config.getEvaluationStrategy()).isNull();
            assertThat(config.getModels()).isEmpty();
        }

        @Test
        @DisplayName("Should allow specifying evaluation strategy")
        void shouldAllowSpecifyingEvaluationStrategy() {
            ContextPrecisionMetric.ContextPrecisionConfig config =
                    ContextPrecisionMetric.ContextPrecisionConfig.builder()
                            .evaluationStrategy(ContextPrecisionMetric.EvaluationStrategy.REFERENCE_BASED)
                            .build();

            assertThat(config.getEvaluationStrategy())
                    .isEqualTo(ContextPrecisionMetric.EvaluationStrategy.REFERENCE_BASED);
        }

        @Test
        @DisplayName("Should allow specifying models")
        void shouldAllowSpecifyingModels() {
            ContextPrecisionMetric.ContextPrecisionConfig config =
                    ContextPrecisionMetric.ContextPrecisionConfig.builder()
                            .model("gpt-4")
                            .model("claude-3")
                            .build();

            assertThat(config.getModels()).containsExactly("gpt-4", "claude-3");
        }
    }

    @Nested
    @DisplayName("Default Prompt Templates")
    class PromptTemplateTests {

        @Test
        @DisplayName("Reference prompt should contain required placeholders")
        void referencePromptShouldContainPlaceholders() {
            assertThat(ContextPrecisionMetric.DEFAULT_WITH_REFERENCE_PROMPT)
                    .contains("{user_input}")
                    .contains("{reference}")
                    .contains("{context_chunk}");
        }

        @Test
        @DisplayName("Non-reference prompt should contain required placeholders")
        void nonReferencePromptShouldContainPlaceholders() {
            assertThat(ContextPrecisionMetric.DEFAULT_WITHOUT_REFERENCE_PROMPT)
                    .contains("{user_input}")
                    .contains("{response}")
                    .contains("{context_chunk}");
        }

        @Test
        @DisplayName("Both prompts should contain relevance instructions")
        void bothPromptsShouldContainRelevanceInstructions() {
            assertThat(ContextPrecisionMetric.DEFAULT_WITH_REFERENCE_PROMPT)
                    .contains("relevant")
                    .contains("reasoning");

            assertThat(ContextPrecisionMetric.DEFAULT_WITHOUT_REFERENCE_PROMPT)
                    .contains("relevant")
                    .contains("reasoning");
        }
    }

    @Nested
    @DisplayName("Scoring")
    class ScoringTests {

        @Test
        @DisplayName("Should return 1.0 when all contexts are relevant")
        void shouldReturn1WhenAllRelevant() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            ContextPrecisionMetric.RelevanceResponse.class,
                            new ContextPrecisionMetric.RelevanceResponse(true, "relevant"));

            ContextPrecisionMetric metric =
                    ContextPrecisionMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("What is Java?")
                    .reference("Java is a programming language.")
                    .retrievedContexts(
                            List.of("Java is a high-level language.", "Java was created by Sun Microsystems."))
                    .build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should return 0.0 when no contexts are relevant")
        void shouldReturn0WhenNoneRelevant() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            ContextPrecisionMetric.RelevanceResponse.class,
                            new ContextPrecisionMetric.RelevanceResponse(false, "not relevant"));

            ContextPrecisionMetric metric =
                    ContextPrecisionMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("What is Java?")
                    .reference("Java is a programming language.")
                    .retrievedContexts(List.of("Python is a language.", "C++ is popular."))
                    .build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 0.0 when no contexts provided")
        void shouldReturn0WhenNoContexts() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"));

            ContextPrecisionMetric metric =
                    ContextPrecisionMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("What is Java?")
                    .reference("Java is a programming language.")
                    .build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should use response-based strategy when no reference")
        void shouldUseResponseBasedWhenNoReference() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            ContextPrecisionMetric.RelevanceResponse.class,
                            new ContextPrecisionMetric.RelevanceResponse(true, "relevant"));

            ContextPrecisionMetric metric =
                    ContextPrecisionMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("What is Java?")
                    .response("Java is a programming language.")
                    .retrievedContexts(List.of("Java is a language."))
                    .build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should handle null relevance as not relevant")
        void shouldHandleNullRelevanceAsNotRelevant() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            ContextPrecisionMetric.RelevanceResponse.class,
                            new ContextPrecisionMetric.RelevanceResponse(null, "unclear"));

            ContextPrecisionMetric metric =
                    ContextPrecisionMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("What is Java?")
                    .reference("Java is a programming language.")
                    .retrievedContexts(List.of("Context."))
                    .build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should work with reference-based strategy config")
        void shouldWorkWithReferenceBasedConfig() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            ContextPrecisionMetric.RelevanceResponse.class,
                            new ContextPrecisionMetric.RelevanceResponse(true, "relevant"));

            ContextPrecisionMetric metric =
                    ContextPrecisionMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("What is Java?")
                    .reference("Java is a programming language.")
                    .response("Java is great.")
                    .retrievedContexts(List.of("Context."))
                    .build();

            ContextPrecisionMetric.ContextPrecisionConfig config =
                    ContextPrecisionMetric.ContextPrecisionConfig.builder()
                            .evaluationStrategy(ContextPrecisionMetric.EvaluationStrategy.REFERENCE_BASED)
                            .build();

            Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should fall back to response-based when reference strategy but no reference")
        void shouldFallbackWhenNoReference() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            ContextPrecisionMetric.RelevanceResponse.class,
                            new ContextPrecisionMetric.RelevanceResponse(true, "relevant"));

            ContextPrecisionMetric metric =
                    ContextPrecisionMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("What is Java?")
                    .response("Java is great.")
                    .retrievedContexts(List.of("Context."))
                    .build();

            ContextPrecisionMetric.ContextPrecisionConfig config =
                    ContextPrecisionMetric.ContextPrecisionConfig.builder()
                            .evaluationStrategy(ContextPrecisionMetric.EvaluationStrategy.REFERENCE_BASED)
                            .build();

            Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should return 0.0 when model fails for context")
        void shouldReturn0WhenModelFailsForContext() {
            // When a model fails evaluating a context, that context is treated as "not relevant"
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withModelError("model-1", new RuntimeException("Model failed"));

            ContextPrecisionMetric metric =
                    ContextPrecisionMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("What is Java?")
                    .reference("Java is a programming language.")
                    .retrievedContexts(List.of("Context."))
                    .build();

            // Failed context evaluations are treated as not relevant, resulting in 0.0 score
            Double score = metric.singleTurnScore(sample);
            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should work with async scoring")
        void shouldWorkWithAsyncScoring() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            ContextPrecisionMetric.RelevanceResponse.class,
                            new ContextPrecisionMetric.RelevanceResponse(true, "relevant"));

            ContextPrecisionMetric metric =
                    ContextPrecisionMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("What is Java?")
                    .reference("Java is a programming language.")
                    .retrievedContexts(List.of("Context."))
                    .build();

            Double score = metric.singleTurnScoreAsync(sample).join();

            assertThat(score).isEqualTo(1.0);
        }
    }
}
