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

@DisplayName("ResponseRelevancyMetric Tests")
class ResponseRelevancyMetricTest {

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
            ResponseRelevancyMetric metric =
                    ResponseRelevancyMetric.builder().executor(executor).build();

            assertThat(metric).isNotNull();
            assertThat(metric.getName()).isEqualTo("ResponseRelevancyMetric");
        }

        @Test
        @DisplayName("Should allow custom question generation prompt")
        void shouldAllowCustomQuestionGenerationPrompt() {
            String customPrompt = "Custom: {userInput} {response} {numberOfQuestions}";

            ResponseRelevancyMetric metric = ResponseRelevancyMetric.builder()
                    .executor(executor)
                    .questionGenerationPrompt(customPrompt)
                    .build();

            assertThat(metric).isNotNull();
        }

        @Test
        @DisplayName("toBuilder should preserve settings")
        void toBuilderShouldPreserveSettings() {
            ResponseRelevancyMetric original =
                    ResponseRelevancyMetric.builder().executor(executor).build();

            ResponseRelevancyMetric copy = original.toBuilder().build();

            assertThat(copy).isNotSameAs(original);
            assertThat(copy.getName()).isEqualTo(original.getName());
        }
    }

    @Nested
    @DisplayName("GeneratedQuestion Record")
    class GeneratedQuestionTests {

        @Test
        @DisplayName("Should store all fields with committal question")
        void shouldStoreAllFieldsWithCommittalQuestion() {
            ResponseRelevancyMetric.GeneratedQuestion question =
                    new ResponseRelevancyMetric.GeneratedQuestion("Where was Einstein born?", 0);

            assertThat(question.question()).isEqualTo("Where was Einstein born?");
            assertThat(question.noncommittal()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should handle noncommittal question")
        void shouldHandleNoncommittalQuestion() {
            ResponseRelevancyMetric.GeneratedQuestion question =
                    new ResponseRelevancyMetric.GeneratedQuestion("What do you not know?", 1);

            assertThat(question.noncommittal()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should handle null values")
        void shouldHandleNullValues() {
            ResponseRelevancyMetric.GeneratedQuestion question =
                    new ResponseRelevancyMetric.GeneratedQuestion(null, null);

            assertThat(question.question()).isNull();
            assertThat(question.noncommittal()).isNull();
        }
    }

    @Nested
    @DisplayName("GeneratedQuestionsResponse Record")
    class GeneratedQuestionsResponseTests {

        @Test
        @DisplayName("Should store questions list")
        void shouldStoreQuestionsList() {
            List<ResponseRelevancyMetric.GeneratedQuestion> questions = List.of(
                    new ResponseRelevancyMetric.GeneratedQuestion("Question 1?", 0),
                    new ResponseRelevancyMetric.GeneratedQuestion("Question 2?", 0),
                    new ResponseRelevancyMetric.GeneratedQuestion("Question 3?", 0));

            ResponseRelevancyMetric.GeneratedQuestionsResponse response =
                    new ResponseRelevancyMetric.GeneratedQuestionsResponse(questions);

            assertThat(response.questions()).hasSize(3);
        }

        @Test
        @DisplayName("Should handle empty questions list")
        void shouldHandleEmptyQuestionsList() {
            ResponseRelevancyMetric.GeneratedQuestionsResponse response =
                    new ResponseRelevancyMetric.GeneratedQuestionsResponse(List.of());

            assertThat(response.questions()).isEmpty();
        }

        @Test
        @DisplayName("Should handle null questions")
        void shouldHandleNullQuestions() {
            ResponseRelevancyMetric.GeneratedQuestionsResponse response =
                    new ResponseRelevancyMetric.GeneratedQuestionsResponse(null);

            assertThat(response.questions()).isNull();
        }
    }

    @Nested
    @DisplayName("EmbeddingsResult Record")
    class EmbeddingsResultTests {

        @Test
        @DisplayName("Should store embeddings")
        void shouldStoreEmbeddings() {
            double[] userInputEmbedding = {0.1, 0.2, 0.3};
            List<double[]> questionEmbeddings = List.of(new double[] {0.1, 0.2, 0.3}, new double[] {0.4, 0.5, 0.6});

            ResponseRelevancyMetric.EmbeddingsResult result =
                    new ResponseRelevancyMetric.EmbeddingsResult(userInputEmbedding, questionEmbeddings);

            assertThat(result.userInputEmbedding()).isEqualTo(userInputEmbedding);
            assertThat(result.questionEmbeddings()).hasSize(2);
        }

        @Test
        @DisplayName("Should handle empty question embeddings")
        void shouldHandleEmptyQuestionEmbeddings() {
            double[] userInputEmbedding = {0.1, 0.2, 0.3};

            ResponseRelevancyMetric.EmbeddingsResult result =
                    new ResponseRelevancyMetric.EmbeddingsResult(userInputEmbedding, List.of());

            assertThat(result.questionEmbeddings()).isEmpty();
        }

        @Test
        @DisplayName("Should handle null embeddings")
        void shouldHandleNullEmbeddings() {
            ResponseRelevancyMetric.EmbeddingsResult result = new ResponseRelevancyMetric.EmbeddingsResult(null, null);

            assertThat(result.userInputEmbedding()).isNull();
            assertThat(result.questionEmbeddings()).isNull();
        }
    }

    @Nested
    @DisplayName("ResponseRelevancyConfig")
    class ConfigTests {

        @Test
        @DisplayName("Should have default numberOfQuestions of 3")
        void shouldHaveDefaultNumberOfQuestions() {
            ResponseRelevancyMetric.ResponseRelevancyConfig config =
                    ResponseRelevancyMetric.ResponseRelevancyConfig.builder().build();

            assertThat(config.getNumberOfQuestions()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should allow custom number of questions")
        void shouldAllowCustomNumberOfQuestions() {
            ResponseRelevancyMetric.ResponseRelevancyConfig config =
                    ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                            .numberOfQuestions(5)
                            .build();

            assertThat(config.getNumberOfQuestions()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should start with empty models list")
        void shouldStartWithEmptyModelsList() {
            ResponseRelevancyMetric.ResponseRelevancyConfig config =
                    ResponseRelevancyMetric.ResponseRelevancyConfig.builder().build();

            assertThat(config.getModels()).isEmpty();
        }

        @Test
        @DisplayName("Should allow specifying models")
        void shouldAllowSpecifyingModels() {
            ResponseRelevancyMetric.ResponseRelevancyConfig config =
                    ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                            .model("gpt-4")
                            .model("claude-3")
                            .build();

            assertThat(config.getModels()).containsExactly("gpt-4", "claude-3");
        }

        @Test
        @DisplayName("defaultConfig should create valid default config")
        void defaultConfigShouldCreateValidConfig() {
            ResponseRelevancyMetric.ResponseRelevancyConfig config =
                    ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

            assertThat(config).isNotNull();
            assertThat(config.getNumberOfQuestions()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Default Prompt Template")
    class PromptTemplateTests {

        @Test
        @DisplayName("Should contain required placeholders")
        void shouldContainRequiredPlaceholders() {
            assertThat(ResponseRelevancyMetric.DEFAULT_QUESTION_GENERATION_PROMPT)
                    .contains("{userInput}")
                    .contains("{response}")
                    .contains("{numberOfQuestions}");
        }

        @Test
        @DisplayName("Should contain noncommittal instructions")
        void shouldContainNoncommittalInstructions() {
            assertThat(ResponseRelevancyMetric.DEFAULT_QUESTION_GENERATION_PROMPT)
                    .contains("noncommittal")
                    .contains("I don't know")
                    .contains("I'm not sure");
        }

        @Test
        @DisplayName("Should contain examples")
        void shouldContainExamples() {
            assertThat(ResponseRelevancyMetric.DEFAULT_QUESTION_GENERATION_PROMPT)
                    .contains("Albert Einstein")
                    .contains("Germany")
                    .contains("Example");
        }

        @Test
        @DisplayName("Should contain Russian language examples")
        void shouldContainRussianExamples() {
            assertThat(ResponseRelevancyMetric.DEFAULT_QUESTION_GENERATION_PROMPT)
                    .contains("Какая столица Франции")
                    .contains("Италия");
        }
    }

    @Nested
    @DisplayName("Scoring")
    class ScoringTests {

        @Test
        @DisplayName("Should return 0.0 when user input is missing")
        void shouldReturn0WhenNoUserInput() {
            StubMultiModelExecutor stubExecutor =
                    new StubMultiModelExecutor(List.of("model-1"), List.of("embedding-1"));

            ResponseRelevancyMetric metric =
                    ResponseRelevancyMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder().response("Response").build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 0.0 when response is missing")
        void shouldReturn0WhenNoResponse() {
            StubMultiModelExecutor stubExecutor =
                    new StubMultiModelExecutor(List.of("model-1"), List.of("embedding-1"));

            ResponseRelevancyMetric metric =
                    ResponseRelevancyMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder().userInput("Question").build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 0.0 when all questions are noncommittal")
        void shouldReturn0WhenAllNoncommittal() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"), List.of("embedding-1"))
                    .withResponse(
                            ResponseRelevancyMetric.GeneratedQuestionsResponse.class,
                            new ResponseRelevancyMetric.GeneratedQuestionsResponse(List.of(
                                    new ResponseRelevancyMetric.GeneratedQuestion("What don't you know?", 1),
                                    new ResponseRelevancyMetric.GeneratedQuestion("What are you unsure about?", 1))));

            ResponseRelevancyMetric metric =
                    ResponseRelevancyMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("What is Java?")
                    .response("I don't know.")
                    .build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should throw when questions list is empty")
        void shouldThrowWhenEmptyQuestions() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"), List.of("embedding-1"))
                    .withResponse(
                            ResponseRelevancyMetric.GeneratedQuestionsResponse.class,
                            new ResponseRelevancyMetric.GeneratedQuestionsResponse(List.of()));

            ResponseRelevancyMetric metric =
                    ResponseRelevancyMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("What is Java?")
                    .response("Response.")
                    .build();

            // Empty questions result in no similarity computation which throws
            assertThatThrownBy(() -> metric.singleTurnScore(sample))
                    .hasCauseInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("All models failed");
        }

        @Test
        @DisplayName("Should throw when all models fail")
        void shouldThrowWhenAllModelsFail() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"), List.of("embedding-1"))
                    .withModelError("model-1", new RuntimeException("Model failed"));

            ResponseRelevancyMetric metric =
                    ResponseRelevancyMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("What is Java?")
                    .response("Java is a programming language.")
                    .build();

            assertThatThrownBy(() -> metric.singleTurnScore(sample))
                    .hasCauseInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("All models failed");
        }

        @Test
        @DisplayName("Should compute score when all steps succeed")
        void shouldComputeScoreWhenAllStepsSucceed() {
            // Create identical embeddings to get score of 1.0
            float[] embedding = {0.1f, 0.2f, 0.3f, 0.4f, 0.5f};

            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"), List.of("embedding-1"))
                    .withResponse(
                            ResponseRelevancyMetric.GeneratedQuestionsResponse.class,
                            new ResponseRelevancyMetric.GeneratedQuestionsResponse(
                                    List.of(new ResponseRelevancyMetric.GeneratedQuestion("What is Java?", 0))))
                    // embeddings returns list: [userInputEmbedding, questionEmbedding1, ...]
                    .withEmbeddings(texts -> {
                        // Return same embedding for all texts
                        return texts.stream().map(t -> embedding).toList();
                    });

            ResponseRelevancyMetric metric =
                    ResponseRelevancyMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("What is Java?")
                    .response("Java is a programming language.")
                    .build();

            Double score = metric.singleTurnScore(sample);

            // Same embeddings for question and user input should give high similarity
            assertThat(score).isCloseTo(1.0, within(0.01));
        }

        @Test
        @DisplayName("Should work with async scoring")
        void shouldWorkWithAsyncScoring() {
            float[] embedding = {0.1f, 0.2f, 0.3f, 0.4f, 0.5f};

            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"), List.of("embedding-1"))
                    .withResponse(
                            ResponseRelevancyMetric.GeneratedQuestionsResponse.class,
                            new ResponseRelevancyMetric.GeneratedQuestionsResponse(
                                    List.of(new ResponseRelevancyMetric.GeneratedQuestion("What is Java?", 0))))
                    .withEmbeddings(texts -> texts.stream().map(t -> embedding).toList());

            ResponseRelevancyMetric metric =
                    ResponseRelevancyMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("What is Java?")
                    .response("Java is a programming language.")
                    .build();

            Double score = metric.singleTurnScoreAsync(sample).join();

            assertThat(score).isCloseTo(1.0, within(0.01));
        }

        @Test
        @DisplayName("Should work with config and specified models")
        void shouldWorkWithConfigAndModels() {
            float[] embedding = {0.1f, 0.2f, 0.3f};

            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(
                            List.of("model-1", "model-2"), List.of("embedding-1"))
                    .withResponse(
                            ResponseRelevancyMetric.GeneratedQuestionsResponse.class,
                            new ResponseRelevancyMetric.GeneratedQuestionsResponse(
                                    List.of(new ResponseRelevancyMetric.GeneratedQuestion("Generated?", 0))))
                    .withEmbeddings(texts -> texts.stream().map(t -> embedding).toList());

            ResponseRelevancyMetric metric =
                    ResponseRelevancyMetric.builder().executor(stubExecutor).build();

            Sample sample =
                    Sample.builder().userInput("Question").response("Response").build();

            ResponseRelevancyMetric.ResponseRelevancyConfig config =
                    ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                            .numberOfQuestions(2)
                            .model("model-1")
                            .build();

            Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isGreaterThanOrEqualTo(0.0).isLessThanOrEqualTo(1.0);
        }
    }
}
