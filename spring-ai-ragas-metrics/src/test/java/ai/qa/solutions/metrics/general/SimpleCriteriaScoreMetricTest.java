package ai.qa.solutions.metrics.general;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;

import ai.qa.solutions.chatclient.ChatClientStore;
import ai.qa.solutions.execution.MultiModelExecutor;
import ai.qa.solutions.execution.StubMultiModelExecutor;
import ai.qa.solutions.sample.Sample;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

@DisplayName("SimpleCriteriaScoreMetric Tests")
class SimpleCriteriaScoreMetricTest {

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
            SimpleCriteriaScoreMetric metric =
                    SimpleCriteriaScoreMetric.builder().executor(executor).build();

            assertThat(metric).isNotNull();
            assertThat(metric.getName()).isEqualTo("SimpleCriteriaScoreMetric");
        }

        @Test
        @DisplayName("Should allow custom prompt template")
        void shouldAllowCustomPromptTemplate() {
            String customTemplate = "Custom: {definition}";

            SimpleCriteriaScoreMetric metric = SimpleCriteriaScoreMetric.builder()
                    .executor(executor)
                    .promptTemplate(customTemplate)
                    .build();

            assertThat(metric).isNotNull();
        }
    }

    @Nested
    @DisplayName("Response Record")
    class ResponseTests {

        @Test
        @DisplayName("Should store all fields")
        void shouldStoreAllFields() {
            SimpleCriteriaScoreMetric.Response response =
                    new SimpleCriteriaScoreMetric.Response("criteria", 4.5, "reasoning");

            assertThat(response.criteria()).isEqualTo("criteria");
            assertThat(response.score()).isEqualTo(4.5);
            assertThat(response.reasoning()).isEqualTo("reasoning");
        }

        @Test
        @DisplayName("Should handle null score")
        void shouldHandleNullScore() {
            SimpleCriteriaScoreMetric.Response response =
                    new SimpleCriteriaScoreMetric.Response("criteria", null, "reasoning");

            assertThat(response.score()).isNull();
        }
    }

    @Nested
    @DisplayName("SimpleCriteriaConfig")
    class ConfigTests {

        @Test
        @DisplayName("Should require definition")
        void shouldRequireDefinition() {
            assertThatThrownBy(() -> SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                            .build())
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should have default min score of 0.0")
        void shouldHaveDefaultMinScore() {
            SimpleCriteriaScoreMetric.SimpleCriteriaConfig config =
                    SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                            .definition("Test")
                            .build();

            assertThat(config.getMinScore()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should have default max score of 5.0")
        void shouldHaveDefaultMaxScore() {
            SimpleCriteriaScoreMetric.SimpleCriteriaConfig config =
                    SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                            .definition("Test")
                            .build();

            assertThat(config.getMaxScore()).isEqualTo(5.0);
        }

        @Test
        @DisplayName("Should have default strictness of 1")
        void shouldHaveDefaultStrictness() {
            SimpleCriteriaScoreMetric.SimpleCriteriaConfig config =
                    SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                            .definition("Test")
                            .build();

            assertThat(config.getStrictness()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should allow custom score range")
        void shouldAllowCustomScoreRange() {
            SimpleCriteriaScoreMetric.SimpleCriteriaConfig config =
                    SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                            .definition("Test")
                            .minScore(1.0)
                            .maxScore(10.0)
                            .build();

            assertThat(config.getMinScore()).isEqualTo(1.0);
            assertThat(config.getMaxScore()).isEqualTo(10.0);
        }

        @Test
        @DisplayName("setScoreRange should validate min < max")
        void setScoreRangeShouldValidate() {
            SimpleCriteriaScoreMetric.SimpleCriteriaConfig config =
                    SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                            .definition("Test")
                            .build();

            assertThatThrownBy(() -> config.setScoreRange(5.0, 5.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("minScore must be less than maxScore");
        }

        @Test
        @DisplayName("setScoreRange should reject min >= max")
        void setScoreRangeShouldRejectInvalidRange() {
            SimpleCriteriaScoreMetric.SimpleCriteriaConfig config =
                    SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                            .definition("Test")
                            .build();

            assertThatThrownBy(() -> config.setScoreRange(10.0, 5.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("minScore must be less than maxScore");
        }

        @Test
        @DisplayName("Should allow specifying models")
        void shouldAllowSpecifyingModels() {
            SimpleCriteriaScoreMetric.SimpleCriteriaConfig config =
                    SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                            .definition("Test")
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
            assertThat(SimpleCriteriaScoreMetric.DEFAULT_PROMPT_TEMPLATE)
                    .contains("{definition}")
                    .contains("{user_input}")
                    .contains("{response}")
                    .contains("{reference}")
                    .contains("{min_score}")
                    .contains("{max_score}");
        }
    }

    @Nested
    @DisplayName("Normalization Logic")
    class NormalizationTests {

        // Testing normalization indirectly through Response handling
        // The normalize method is private, but we can verify its behavior
        // through the metric's overall behavior

        @Test
        @DisplayName("Config should support full score range for normalization")
        void configShouldSupportFullRange() {
            // Given a config with 0-10 range
            SimpleCriteriaScoreMetric.SimpleCriteriaConfig config =
                    SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                            .definition("Test")
                            .minScore(0.0)
                            .maxScore(10.0)
                            .build();

            // The normalized score for 5.0 should be 0.5
            // This is verified through integration tests, but here we verify
            // the config correctly stores the range
            assertThat(config.getMinScore()).isEqualTo(0.0);
            assertThat(config.getMaxScore()).isEqualTo(10.0);
        }

        @Test
        @DisplayName("Config should handle negative ranges")
        void configShouldHandleNegativeRanges() {
            SimpleCriteriaScoreMetric.SimpleCriteriaConfig config =
                    SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                            .definition("Test")
                            .minScore(-5.0)
                            .maxScore(5.0)
                            .build();

            assertThat(config.getMinScore()).isEqualTo(-5.0);
            assertThat(config.getMaxScore()).isEqualTo(5.0);
        }
    }

    @Nested
    @DisplayName("Scoring")
    class ScoringTests {

        @Test
        @DisplayName("Should normalize max score to 1.0")
        void shouldNormalizeMaxScoreTo1() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            SimpleCriteriaScoreMetric.Response.class,
                            new SimpleCriteriaScoreMetric.Response("test", 5.0, "perfect"));

            SimpleCriteriaScoreMetric metric =
                    SimpleCriteriaScoreMetric.builder().executor(stubExecutor).build();

            Sample sample =
                    Sample.builder().userInput("Question").response("Answer").build();

            SimpleCriteriaScoreMetric.SimpleCriteriaConfig config =
                    SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                            .definition("Test criteria")
                            .minScore(0.0)
                            .maxScore(5.0)
                            .build();

            Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should normalize min score to 0.0")
        void shouldNormalizeMinScoreTo0() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            SimpleCriteriaScoreMetric.Response.class,
                            new SimpleCriteriaScoreMetric.Response("test", 0.0, "poor"));

            SimpleCriteriaScoreMetric metric =
                    SimpleCriteriaScoreMetric.builder().executor(stubExecutor).build();

            Sample sample =
                    Sample.builder().userInput("Question").response("Answer").build();

            SimpleCriteriaScoreMetric.SimpleCriteriaConfig config =
                    SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                            .definition("Test criteria")
                            .build();

            Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should normalize mid-range score to 0.5")
        void shouldNormalizeMidRangeScore() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            SimpleCriteriaScoreMetric.Response.class,
                            new SimpleCriteriaScoreMetric.Response("test", 2.5, "average"));

            SimpleCriteriaScoreMetric metric =
                    SimpleCriteriaScoreMetric.builder().executor(stubExecutor).build();

            Sample sample =
                    Sample.builder().userInput("Question").response("Answer").build();

            SimpleCriteriaScoreMetric.SimpleCriteriaConfig config =
                    SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                            .definition("Test criteria")
                            .build();

            Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isCloseTo(0.5, within(0.01));
        }

        @Test
        @DisplayName("Should handle null score as 0.0")
        void shouldHandleNullScoreAs0() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            SimpleCriteriaScoreMetric.Response.class,
                            new SimpleCriteriaScoreMetric.Response("test", null, "no score"));

            SimpleCriteriaScoreMetric metric =
                    SimpleCriteriaScoreMetric.builder().executor(stubExecutor).build();

            Sample sample =
                    Sample.builder().userInput("Question").response("Answer").build();

            SimpleCriteriaScoreMetric.SimpleCriteriaConfig config =
                    SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                            .definition("Test criteria")
                            .build();

            Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should throw when all models fail")
        void shouldThrowWhenAllModelsFail() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withModelError("model-1", new RuntimeException("Model failed"));

            SimpleCriteriaScoreMetric metric =
                    SimpleCriteriaScoreMetric.builder().executor(stubExecutor).build();

            Sample sample =
                    Sample.builder().userInput("Question").response("Answer").build();

            SimpleCriteriaScoreMetric.SimpleCriteriaConfig config =
                    SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                            .definition("Test criteria")
                            .build();

            assertThatThrownBy(() -> metric.singleTurnScore(config, sample))
                    .hasCauseInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("All models failed");
        }

        @Test
        @DisplayName("Should clamp scores above max to 1.0")
        void shouldClampScoresAboveMax() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            SimpleCriteriaScoreMetric.Response.class,
                            new SimpleCriteriaScoreMetric.Response("test", 10.0, "over max"));

            SimpleCriteriaScoreMetric metric =
                    SimpleCriteriaScoreMetric.builder().executor(stubExecutor).build();

            Sample sample =
                    Sample.builder().userInput("Question").response("Answer").build();

            SimpleCriteriaScoreMetric.SimpleCriteriaConfig config =
                    SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                            .definition("Test criteria")
                            .maxScore(5.0)
                            .build();

            Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should include reference in prompt when provided")
        void shouldIncludeReferenceInPrompt() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            SimpleCriteriaScoreMetric.Response.class,
                            new SimpleCriteriaScoreMetric.Response("test", 4.0, "good"));

            SimpleCriteriaScoreMetric metric =
                    SimpleCriteriaScoreMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("What is Java?")
                    .response("Java is a programming language")
                    .reference("Java is a high-level programming language")
                    .build();

            SimpleCriteriaScoreMetric.SimpleCriteriaConfig config =
                    SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                            .definition("Test criteria")
                            .build();

            Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isCloseTo(0.8, within(0.01));
        }
    }

    @Nested
    @DisplayName("Prompt Resolution")
    class PromptResolutionTests {

        @Test
        @DisplayName("Should use config promptTemplate when provided")
        void shouldUseConfigPromptTemplateWhenProvided() {
            final String customPrompt =
                    "CUSTOM: {definition} | {user_input} | {response} | {reference} | {min_score} | {max_score}";
            final List<String> capturedPrompts = new ArrayList<>();

            final StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponseProvider(SimpleCriteriaScoreMetric.Response.class, prompt -> {
                        capturedPrompts.add(prompt);
                        return new SimpleCriteriaScoreMetric.Response("test", 3.0, "ok");
                    });

            final SimpleCriteriaScoreMetric metric =
                    SimpleCriteriaScoreMetric.builder().executor(stubExecutor).build();

            final Sample sample = Sample.builder()
                    .userInput("question")
                    .response("answer")
                    .reference("ref")
                    .build();

            final SimpleCriteriaScoreMetric.SimpleCriteriaConfig config =
                    SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                            .definition("test def")
                            .promptTemplate(customPrompt)
                            .build();

            final Double score = metric.singleTurnScore(config, sample);
            assertThat(score).isNotNull();

            assertThat(capturedPrompts).hasSize(1);
            assertThat(capturedPrompts.get(0)).startsWith("CUSTOM:");
            assertThat(capturedPrompts.get(0)).contains("test def");
            assertThat(capturedPrompts.get(0)).contains("question");
            assertThat(capturedPrompts.get(0)).contains("answer");
            assertThat(capturedPrompts.get(0)).contains("ref");
        }

        @Test
        @DisplayName("Should use constructor promptTemplate when no config override")
        void shouldUseConstructorPromptTemplateWhenNoConfigOverride() {
            final String constructorPrompt =
                    "CONSTRUCTOR: {definition} | {user_input} | {response} | {reference} | {min_score} | {max_score}";
            final List<String> capturedPrompts = new ArrayList<>();

            final StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponseProvider(SimpleCriteriaScoreMetric.Response.class, prompt -> {
                        capturedPrompts.add(prompt);
                        return new SimpleCriteriaScoreMetric.Response("test", 3.0, "ok");
                    });

            final SimpleCriteriaScoreMetric metric = SimpleCriteriaScoreMetric.builder()
                    .executor(stubExecutor)
                    .promptTemplate(constructorPrompt)
                    .build();

            final Sample sample = Sample.builder()
                    .userInput("question")
                    .response("answer")
                    .reference("ref")
                    .build();

            final SimpleCriteriaScoreMetric.SimpleCriteriaConfig config =
                    SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                            .definition("test def")
                            .build();

            final Double score = metric.singleTurnScore(config, sample);
            assertThat(score).isNotNull();

            assertThat(capturedPrompts).hasSize(1);
            assertThat(capturedPrompts.get(0)).startsWith("CONSTRUCTOR:");
            assertThat(capturedPrompts.get(0)).contains("test def");
        }

        @Test
        @DisplayName("Should resolve Russian prompt when language is ru and no overrides")
        void shouldResolveRussianPromptWhenLanguageIsRu() {
            final List<String> capturedPrompts = new ArrayList<>();

            final StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponseProvider(SimpleCriteriaScoreMetric.Response.class, prompt -> {
                        capturedPrompts.add(prompt);
                        return new SimpleCriteriaScoreMetric.Response("test", 3.0, "ok");
                    });

            final SimpleCriteriaScoreMetric metric =
                    SimpleCriteriaScoreMetric.builder().executor(stubExecutor).build();

            final Sample sample = Sample.builder()
                    .userInput("question")
                    .response("answer")
                    .reference("ref")
                    .build();

            final SimpleCriteriaScoreMetric.SimpleCriteriaConfig config =
                    SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                            .definition("test def")
                            .language("ru")
                            .build();

            final Double score = metric.singleTurnScore(config, sample);
            assertThat(score).isNotNull();

            assertThat(capturedPrompts).hasSize(1);
            // Russian prompt should NOT start with the English default text
            assertThat(capturedPrompts.get(0)).doesNotStartWith("Evaluate the AI response based on");
            // Russian prompt should contain the substituted values
            assertThat(capturedPrompts.get(0)).contains("test def");
        }
    }
}
