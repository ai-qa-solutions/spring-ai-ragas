package ai.qa.solutions.metrics.general;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

@DisplayName("RubricsScoreMetric Tests")
class RubricsScoreMetricTest {

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
            RubricsScoreMetric metric =
                    RubricsScoreMetric.builder().executor(executor).build();

            assertThat(metric).isNotNull();
            assertThat(metric.getName()).isEqualTo("RubricsScoreMetric");
        }

        @Test
        @DisplayName("Should allow custom prompt template")
        void shouldAllowCustomPromptTemplate() {
            String customTemplate = "Custom: {user_input} {response} {rubrics}";

            RubricsScoreMetric metric = RubricsScoreMetric.builder()
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
        @DisplayName("Should return normalized score")
        void shouldReturnNormalizedScore() {
            RubricsScoreMetric.Response response =
                    new RubricsScoreMetric.Response(4, "score4_description", "reasoning");

            assertThat(response.getNormalizedScore()).isEqualTo(4.0);
        }

        @Test
        @DisplayName("Should return 0 for null score")
        void shouldReturn0ForNullScore() {
            RubricsScoreMetric.Response response =
                    new RubricsScoreMetric.Response(null, "score_description", "reasoning");

            assertThat(response.getNormalizedScore()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should store all fields")
        void shouldStoreAllFields() {
            RubricsScoreMetric.Response response =
                    new RubricsScoreMetric.Response(3, "score3_description", "detailed reasoning");

            assertThat(response.score()).isEqualTo(3);
            assertThat(response.rubric_level()).isEqualTo("score3_description");
            assertThat(response.reasoning()).isEqualTo("detailed reasoning");
        }
    }

    @Nested
    @DisplayName("RubricsConfig")
    class ConfigTests {

        @Test
        @DisplayName("Should create empty rubrics by default")
        void shouldCreateEmptyRubricsByDefault() {
            // @Singular creates an empty map, not null
            RubricsScoreMetric.RubricsConfig config =
                    RubricsScoreMetric.RubricsConfig.builder().build();

            assertThat(config.getRubrics()).isEmpty();
        }

        @Test
        @DisplayName("Should allow building with rubrics")
        void shouldAllowBuildingWithRubrics() {
            RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                    .rubric("score1_description", "Poor response")
                    .rubric("score2_description", "Below average")
                    .rubric("score3_description", "Average")
                    .rubric("score4_description", "Good")
                    .rubric("score5_description", "Excellent")
                    .build();

            assertThat(config.getRubrics()).hasSize(5);
        }

        @Test
        @DisplayName("validateRubrics should throw for empty rubrics")
        void validateRubricsShouldThrowForEmpty() {
            RubricsScoreMetric.RubricsConfig config =
                    RubricsScoreMetric.RubricsConfig.builder().rubrics(Map.of()).build();

            assertThatThrownBy(config::validateRubrics)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null or empty");
        }

        @Test
        @DisplayName("validateRubrics should throw for invalid keys")
        void validateRubricsShouldThrowForInvalidKeys() {
            RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                    .rubric("invalid_key", "Description")
                    .build();

            assertThatThrownBy(config::validateRubrics)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("scoreN_description");
        }

        @Test
        @DisplayName("validateRubrics should accept valid keys")
        void validateRubricsShouldAcceptValidKeys() {
            RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                    .rubric("score1_description", "Level 1")
                    .rubric("score2_description", "Level 2")
                    .build();

            // Should not throw
            config.validateRubrics();
        }

        @Test
        @DisplayName("Should allow specifying models")
        void shouldAllowSpecifyingModels() {
            RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                    .rubric("score1_description", "Level 1")
                    .model("gpt-4")
                    .build();

            assertThat(config.getModels()).containsExactly("gpt-4");
        }
    }

    @Nested
    @DisplayName("Default Prompt Template")
    class PromptTemplateTests {

        @Test
        @DisplayName("Should contain required placeholders")
        void shouldContainRequiredPlaceholders() {
            assertThat(RubricsScoreMetric.DEFAULT_PROMPT_TEMPLATE)
                    .contains("{user_input}")
                    .contains("{response}")
                    .contains("{reference}")
                    .contains("{rubrics}");
        }

        @Test
        @DisplayName("Should mention score and reasoning in instructions")
        void shouldMentionScoreAndReasoning() {
            assertThat(RubricsScoreMetric.DEFAULT_PROMPT_TEMPLATE)
                    .contains("score")
                    .contains("reasoning");
        }
    }

    @Nested
    @DisplayName("Scoring")
    class ScoringTests {

        @Test
        @DisplayName("Should return score from model response")
        void shouldReturnScoreFromModel() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            RubricsScoreMetric.Response.class,
                            new RubricsScoreMetric.Response(4, "score4_description", "good response"));

            RubricsScoreMetric metric =
                    RubricsScoreMetric.builder().executor(stubExecutor).build();

            Sample sample =
                    Sample.builder().userInput("Question").response("Answer").build();

            RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                    .rubric("score1_description", "Poor")
                    .rubric("score2_description", "Below average")
                    .rubric("score3_description", "Average")
                    .rubric("score4_description", "Good")
                    .rubric("score5_description", "Excellent")
                    .build();

            Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(4.0);
        }

        @Test
        @DisplayName("Should handle null score as 0.0")
        void shouldHandleNullScoreAs0() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            RubricsScoreMetric.Response.class,
                            new RubricsScoreMetric.Response(null, "score_description", "no score"));

            RubricsScoreMetric metric =
                    RubricsScoreMetric.builder().executor(stubExecutor).build();

            Sample sample =
                    Sample.builder().userInput("Question").response("Answer").build();

            RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                    .rubric("score1_description", "Poor")
                    .build();

            Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should aggregate scores from multiple models")
        void shouldAggregateScoresFromMultipleModels() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1", "model-2"))
                    .withResponse(
                            RubricsScoreMetric.Response.class,
                            new RubricsScoreMetric.Response(3, "score3_description", "average"));

            RubricsScoreMetric metric =
                    RubricsScoreMetric.builder().executor(stubExecutor).build();

            Sample sample =
                    Sample.builder().userInput("Question").response("Answer").build();

            RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                    .rubric("score1_description", "Poor")
                    .rubric("score3_description", "Average")
                    .build();

            Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(3.0);
        }

        @Test
        @DisplayName("Should throw when all models fail")
        void shouldThrowWhenAllModelsFail() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withModelError("model-1", new RuntimeException("Model failed"));

            RubricsScoreMetric metric =
                    RubricsScoreMetric.builder().executor(stubExecutor).build();

            Sample sample =
                    Sample.builder().userInput("Question").response("Answer").build();

            RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                    .rubric("score1_description", "Poor")
                    .build();

            assertThatThrownBy(() -> metric.singleTurnScore(config, sample))
                    .hasCauseInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("All models failed");
        }

        @Test
        @DisplayName("Should include reference in prompt when provided")
        void shouldIncludeReferenceInPrompt() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            RubricsScoreMetric.Response.class,
                            new RubricsScoreMetric.Response(5, "score5_description", "excellent"));

            RubricsScoreMetric metric =
                    RubricsScoreMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("What is Java?")
                    .response("Java is a programming language")
                    .reference("Java is a high-level programming language")
                    .build();

            RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                    .rubric("score5_description", "Excellent")
                    .build();

            Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(5.0);
        }
    }

    @Nested
    @DisplayName("Prompt Resolution")
    class PromptResolutionTests {

        @Test
        @DisplayName("Should use config promptTemplate when provided")
        void shouldUseConfigPromptTemplateWhenProvided() {
            final String customPrompt = "CUSTOM: {user_input} | {response} | {reference} | {rubrics}";
            final List<String> capturedPrompts = new ArrayList<>();

            final StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponseProvider(RubricsScoreMetric.Response.class, prompt -> {
                        capturedPrompts.add(prompt);
                        return new RubricsScoreMetric.Response(3, "score3_description", "average");
                    });

            final RubricsScoreMetric metric =
                    RubricsScoreMetric.builder().executor(stubExecutor).build();

            final Sample sample = Sample.builder()
                    .userInput("question")
                    .response("answer")
                    .reference("ref")
                    .build();

            final RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                    .rubric("score1_description", "Poor")
                    .rubric("score3_description", "Average")
                    .promptTemplate(customPrompt)
                    .build();

            final Double score = metric.singleTurnScore(config, sample);
            assertThat(score).isNotNull();

            assertThat(capturedPrompts).hasSize(1);
            assertThat(capturedPrompts.get(0)).startsWith("CUSTOM:");
            assertThat(capturedPrompts.get(0)).contains("question");
            assertThat(capturedPrompts.get(0)).contains("answer");
        }

        @Test
        @DisplayName("Should use constructor promptTemplate when no config override")
        void shouldUseConstructorPromptTemplateWhenNoConfigOverride() {
            final String constructorPrompt = "CONSTRUCTOR: {user_input} | {response} | {reference} | {rubrics}";
            final List<String> capturedPrompts = new ArrayList<>();

            final StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponseProvider(RubricsScoreMetric.Response.class, prompt -> {
                        capturedPrompts.add(prompt);
                        return new RubricsScoreMetric.Response(3, "score3_description", "average");
                    });

            final RubricsScoreMetric metric = RubricsScoreMetric.builder()
                    .executor(stubExecutor)
                    .promptTemplate(constructorPrompt)
                    .build();

            final Sample sample =
                    Sample.builder().userInput("question").response("answer").build();

            final RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                    .rubric("score1_description", "Poor")
                    .rubric("score3_description", "Average")
                    .build();

            final Double score = metric.singleTurnScore(config, sample);
            assertThat(score).isNotNull();

            assertThat(capturedPrompts).hasSize(1);
            assertThat(capturedPrompts.get(0)).startsWith("CONSTRUCTOR:");
            assertThat(capturedPrompts.get(0)).contains("question");
        }

        @Test
        @DisplayName("Should resolve Russian prompt when language is ru and no overrides")
        void shouldResolveRussianPromptWhenLanguageIsRu() {
            final List<String> capturedPrompts = new ArrayList<>();

            final StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponseProvider(RubricsScoreMetric.Response.class, prompt -> {
                        capturedPrompts.add(prompt);
                        return new RubricsScoreMetric.Response(3, "score3_description", "average");
                    });

            final RubricsScoreMetric metric =
                    RubricsScoreMetric.builder().executor(stubExecutor).build();

            final Sample sample =
                    Sample.builder().userInput("question").response("answer").build();

            final RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                    .rubric("score1_description", "Poor")
                    .rubric("score3_description", "Average")
                    .language("ru")
                    .build();

            final Double score = metric.singleTurnScore(config, sample);
            assertThat(score).isNotNull();

            assertThat(capturedPrompts).hasSize(1);
            // Russian prompt should NOT start with the English default text
            assertThat(capturedPrompts.get(0)).doesNotStartWith("Evaluate the AI response using");
            // Russian prompt should contain the substituted values
            assertThat(capturedPrompts.get(0)).contains("question");
        }
    }
}
