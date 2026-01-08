package ai.qa.solutions.metrics.general;

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

@DisplayName("AspectCriticMetric Tests")
class AspectCriticMetricTest {

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
            AspectCriticMetric metric =
                    AspectCriticMetric.builder().executor(executor).build();

            assertThat(metric).isNotNull();
            assertThat(metric.getName()).isEqualTo("AspectCriticMetric");
        }

        @Test
        @DisplayName("Should use default prompt template when not specified")
        void shouldUseDefaultPromptTemplate() {
            AspectCriticMetric metric =
                    AspectCriticMetric.builder().executor(executor).build();

            assertThat(metric).isNotNull();
        }

        @Test
        @DisplayName("Should allow custom prompt template")
        void shouldAllowCustomPromptTemplate() {
            String customTemplate = "Custom template: {definition} {user_input} {response}";

            AspectCriticMetric metric = AspectCriticMetric.builder()
                    .executor(executor)
                    .promptTemplate(customTemplate)
                    .build();

            assertThat(metric).isNotNull();
        }

        @Test
        @DisplayName("toBuilder should preserve settings")
        void toBuilderShouldPreserveSettings() {
            AspectCriticMetric original =
                    AspectCriticMetric.builder().executor(executor).build();

            AspectCriticMetric copy = original.toBuilder().build();

            assertThat(copy).isNotSameAs(original);
            assertThat(copy.getName()).isEqualTo(original.getName());
        }
    }

    @Nested
    @DisplayName("Response Record")
    class ResponseTests {

        @Test
        @DisplayName("Should return 1.0 for true verdict")
        void shouldReturn1ForTrueVerdict() {
            AspectCriticMetric.Response response = new AspectCriticMetric.Response("criteria", true, "reasoning");

            assertThat(response.getScore()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should return 0.0 for false verdict")
        void shouldReturn0ForFalseVerdict() {
            AspectCriticMetric.Response response = new AspectCriticMetric.Response("criteria", false, "reasoning");

            assertThat(response.getScore()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 0.0 for null verdict")
        void shouldReturn0ForNullVerdict() {
            AspectCriticMetric.Response response = new AspectCriticMetric.Response("criteria", null, "reasoning");

            assertThat(response.getScore()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should store all fields correctly")
        void shouldStoreAllFields() {
            AspectCriticMetric.Response response =
                    new AspectCriticMetric.Response("test criteria", true, "test reasoning");

            assertThat(response.criteria()).isEqualTo("test criteria");
            assertThat(response.verdict()).isTrue();
            assertThat(response.reasoning()).isEqualTo("test reasoning");
        }
    }

    @Nested
    @DisplayName("AspectCriticConfig")
    class ConfigTests {

        @Test
        @DisplayName("Should require definition")
        void shouldRequireDefinition() {
            assertThatThrownBy(() ->
                            AspectCriticMetric.AspectCriticConfig.builder().build())
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should have default strictness of 1")
        void shouldHaveDefaultStrictness() {
            AspectCriticMetric.AspectCriticConfig config = AspectCriticMetric.AspectCriticConfig.builder()
                    .definition("Test definition")
                    .build();

            assertThat(config.getStrictness()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should allow setting strictness")
        void shouldAllowSettingStrictness() {
            AspectCriticMetric.AspectCriticConfig config = AspectCriticMetric.AspectCriticConfig.builder()
                    .definition("Test definition")
                    .strictness(3)
                    .build();

            assertThat(config.getStrictness()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should reject strictness below 1")
        void shouldRejectStrictnessBelow1() {
            AspectCriticMetric.AspectCriticConfig config = AspectCriticMetric.AspectCriticConfig.builder()
                    .definition("Test")
                    .build();

            assertThatThrownBy(() -> config.setStrictness(0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("between 1 and 5");
        }

        @Test
        @DisplayName("Should reject strictness above 5")
        void shouldRejectStrictnessAbove5() {
            AspectCriticMetric.AspectCriticConfig config = AspectCriticMetric.AspectCriticConfig.builder()
                    .definition("Test")
                    .build();

            assertThatThrownBy(() -> config.setStrictness(6))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("between 1 and 5");
        }

        @Test
        @DisplayName("Should accept strictness at boundaries")
        void shouldAcceptStrictnessAtBoundaries() {
            AspectCriticMetric.AspectCriticConfig config = AspectCriticMetric.AspectCriticConfig.builder()
                    .definition("Test")
                    .build();

            config.setStrictness(1);
            assertThat(config.getStrictness()).isEqualTo(1);

            config.setStrictness(5);
            assertThat(config.getStrictness()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should allow specifying models")
        void shouldAllowSpecifyingModels() {
            AspectCriticMetric.AspectCriticConfig config = AspectCriticMetric.AspectCriticConfig.builder()
                    .definition("Test definition")
                    .model("gpt-4")
                    .model("claude-3")
                    .build();

            assertThat(config.getModels()).containsExactly("gpt-4", "claude-3");
        }

        @Test
        @DisplayName("Should start with empty models list")
        void shouldStartWithEmptyModelsList() {
            AspectCriticMetric.AspectCriticConfig config = AspectCriticMetric.AspectCriticConfig.builder()
                    .definition("Test definition")
                    .build();

            assertThat(config.getModels()).isEmpty();
        }

        @Test
        @DisplayName("Should store definition")
        void shouldStoreDefinition() {
            String definition = "The response should be helpful and accurate";

            AspectCriticMetric.AspectCriticConfig config = AspectCriticMetric.AspectCriticConfig.builder()
                    .definition(definition)
                    .build();

            assertThat(config.getDefinition()).isEqualTo(definition);
        }
    }

    @Nested
    @DisplayName("Default Prompt Template")
    class PromptTemplateTests {

        @Test
        @DisplayName("Should contain required placeholders")
        void shouldContainRequiredPlaceholders() {
            assertThat(AspectCriticMetric.DEFAULT_PROMPT_TEMPLATE)
                    .contains("{definition}")
                    .contains("{user_input}")
                    .contains("{response}");
        }

        @Test
        @DisplayName("Should contain evaluation instructions")
        void shouldContainEvaluationInstructions() {
            assertThat(AspectCriticMetric.DEFAULT_PROMPT_TEMPLATE)
                    .contains("criteria")
                    .contains("verdict")
                    .contains("reasoning");
        }
    }

    @Nested
    @DisplayName("Scoring")
    class ScoringTests {

        @Test
        @DisplayName("Should return 1.0 when model returns true verdict")
        void shouldReturn1WhenTrueVerdict() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            AspectCriticMetric.Response.class,
                            new AspectCriticMetric.Response("test", true, "good response"));

            AspectCriticMetric metric =
                    AspectCriticMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("What is Java?")
                    .response("Java is a programming language.")
                    .build();

            AspectCriticMetric.AspectCriticConfig config = AspectCriticMetric.AspectCriticConfig.builder()
                    .definition("Response should be accurate")
                    .build();

            Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should return 0.0 when model returns false verdict")
        void shouldReturn0WhenFalseVerdict() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            AspectCriticMetric.Response.class,
                            new AspectCriticMetric.Response("test", false, "bad response"));

            AspectCriticMetric metric =
                    AspectCriticMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("What is Java?")
                    .response("Java is a coffee.")
                    .build();

            AspectCriticMetric.AspectCriticConfig config = AspectCriticMetric.AspectCriticConfig.builder()
                    .definition("Response should be accurate")
                    .build();

            Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should aggregate scores from multiple models")
        void shouldAggregateScoresFromMultipleModels() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1", "model-2"))
                    .withResponse(
                            AspectCriticMetric.Response.class, new AspectCriticMetric.Response("test", true, "good"));

            AspectCriticMetric metric =
                    AspectCriticMetric.builder().executor(stubExecutor).build();

            Sample sample =
                    Sample.builder().userInput("Question").response("Answer").build();

            AspectCriticMetric.AspectCriticConfig config = AspectCriticMetric.AspectCriticConfig.builder()
                    .definition("Test definition")
                    .build();

            Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should throw when all models fail")
        void shouldThrowWhenAllModelsFail() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withModelError("model-1", new RuntimeException("Model failed"));

            AspectCriticMetric metric =
                    AspectCriticMetric.builder().executor(stubExecutor).build();

            Sample sample =
                    Sample.builder().userInput("Question").response("Answer").build();

            AspectCriticMetric.AspectCriticConfig config = AspectCriticMetric.AspectCriticConfig.builder()
                    .definition("Test definition")
                    .build();

            assertThatThrownBy(() -> metric.singleTurnScore(config, sample))
                    .hasCauseInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("All models failed");
        }

        @Test
        @DisplayName("Should work with async scoring")
        void shouldWorkWithAsyncScoring() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            AspectCriticMetric.Response.class, new AspectCriticMetric.Response("test", true, "good"));

            AspectCriticMetric metric =
                    AspectCriticMetric.builder().executor(stubExecutor).build();

            Sample sample =
                    Sample.builder().userInput("Question").response("Answer").build();

            AspectCriticMetric.AspectCriticConfig config = AspectCriticMetric.AspectCriticConfig.builder()
                    .definition("Test definition")
                    .build();

            Double score = metric.singleTurnScoreAsync(config, sample).join();

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should handle null verdict as false")
        void shouldHandleNullVerdictAsFalse() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            AspectCriticMetric.Response.class,
                            new AspectCriticMetric.Response("test", null, "unclear"));

            AspectCriticMetric metric =
                    AspectCriticMetric.builder().executor(stubExecutor).build();

            Sample sample =
                    Sample.builder().userInput("Question").response("Answer").build();

            AspectCriticMetric.AspectCriticConfig config = AspectCriticMetric.AspectCriticConfig.builder()
                    .definition("Test definition")
                    .build();

            Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(0.0);
        }
    }
}
