package ai.qa.solutions.metrics.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.qa.solutions.execution.StubMultiModelExecutor;
import ai.qa.solutions.metrics.agent.TopicAdherenceMetric.ExtractedTopicsResponse;
import ai.qa.solutions.metrics.agent.TopicAdherenceMetric.Mode;
import ai.qa.solutions.metrics.agent.TopicAdherenceMetric.TopicAdherenceConfig;
import ai.qa.solutions.metrics.agent.TopicAdherenceMetric.TopicClassification;
import ai.qa.solutions.metrics.agent.TopicAdherenceMetric.TopicClassificationResponse;
import ai.qa.solutions.sample.Sample;
import ai.qa.solutions.sample.message.AIMessage;
import ai.qa.solutions.sample.message.HumanMessage;
import ai.qa.solutions.sample.message.ToolCall;
import ai.qa.solutions.sample.message.ToolMessage;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TopicAdherenceMetric")
class TopicAdherenceMetricTest {

    private StubMultiModelExecutor executor;
    private TopicAdherenceMetric metric;

    @BeforeEach
    void setUp() {
        executor = new StubMultiModelExecutor(List.of("model1"));
        metric = TopicAdherenceMetric.builder().executor(executor).build();
    }

    @Nested
    @DisplayName("Builder and Config")
    class BuilderAndConfigTests {

        @Test
        @DisplayName("Should create metric with default executor")
        void shouldCreateMetricWithDefaultExecutor() {
            final TopicAdherenceMetric m =
                    TopicAdherenceMetric.builder().executor(executor).build();
            assertThat(m).isNotNull();
            assertThat(m.getName()).isEqualTo("TopicAdherenceMetric");
        }

        @Test
        @DisplayName("Should create config with default values")
        void shouldCreateConfigWithDefaultValues() {
            final TopicAdherenceConfig config = TopicAdherenceConfig.builder().build();
            assertThat(config.getMode()).isEqualTo(Mode.F1);
            assertThat(config.getModels()).isEmpty();
        }

        @Test
        @DisplayName("Should create config with custom values")
        void shouldCreateConfigWithCustomValues() {
            final TopicAdherenceConfig config = TopicAdherenceConfig.builder()
                    .mode(Mode.PRECISION)
                    .model("gpt-4")
                    .model("claude-3")
                    .build();

            assertThat(config.getMode()).isEqualTo(Mode.PRECISION);
            assertThat(config.getModels()).containsExactly("gpt-4", "claude-3");
        }
    }

    @Nested
    @DisplayName("Input Validation")
    class InputValidationTests {

        @Test
        @DisplayName("Should return null when messages are null")
        void shouldReturnNullWhenMessagesAreNull() {
            final Sample sample =
                    Sample.builder().referenceTopics(List.of("topic1")).build();

            final TopicAdherenceConfig config = TopicAdherenceConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isNull();
        }

        @Test
        @DisplayName("Should return null when messages are empty")
        void shouldReturnNullWhenMessagesAreEmpty() {
            final Sample sample = Sample.builder()
                    .userInputMessages(List.of())
                    .referenceTopics(List.of("topic1"))
                    .build();

            final TopicAdherenceConfig config = TopicAdherenceConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isNull();
        }

        @Test
        @DisplayName("Should return null when reference topics are null")
        void shouldReturnNullWhenReferenceTopicsAreNull() {
            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(new HumanMessage("Hello")))
                    .build();

            final TopicAdherenceConfig config = TopicAdherenceConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isNull();
        }

        @Test
        @DisplayName("Should return null when reference topics are empty")
        void shouldReturnNullWhenReferenceTopicsAreEmpty() {
            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(new HumanMessage("Hello")))
                    .referenceTopics(List.of())
                    .build();

            final TopicAdherenceConfig config = TopicAdherenceConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isNull();
        }
    }

    @Nested
    @DisplayName("F1 Mode Evaluation")
    class F1ModeEvaluationTests {

        @Test
        @DisplayName("Should return perfect score when all topics match")
        void shouldReturnPerfectScoreWhenAllTopicsMatch() {
            executor = executor.withResponseProvider(
                            ExtractedTopicsResponse.class,
                            prompt -> new ExtractedTopicsResponse(List.of("flight booking", "travel planning")))
                    .withResponseProvider(
                            TopicClassificationResponse.class,
                            prompt -> new TopicClassificationResponse(List.of(
                                    new TopicClassification("flight booking", true, "flight booking", "Direct match"),
                                    new TopicClassification(
                                            "travel planning", true, "travel planning", "Direct match"))));

            metric = TopicAdherenceMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("I need to book a flight"),
                            new AIMessage("I'll help you with flight booking")))
                    .referenceTopics(List.of("flight booking", "travel planning"))
                    .build();

            final TopicAdherenceConfig config =
                    TopicAdherenceConfig.builder().mode(Mode.F1).build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should return zero when no topics match")
        void shouldReturnZeroWhenNoTopicsMatch() {
            executor = executor.withResponseProvider(
                            ExtractedTopicsResponse.class,
                            prompt -> new ExtractedTopicsResponse(List.of("weather", "sports")))
                    .withResponseProvider(
                            TopicClassificationResponse.class,
                            prompt -> new TopicClassificationResponse(List.of(
                                    new TopicClassification("weather", false, null, "Off topic"),
                                    new TopicClassification("sports", false, null, "Off topic"))));

            metric = TopicAdherenceMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(new HumanMessage("How's the weather?")))
                    .referenceTopics(List.of("flight booking", "travel planning"))
                    .build();

            final TopicAdherenceConfig config =
                    TopicAdherenceConfig.builder().mode(Mode.F1).build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return moderate score for partial match")
        void shouldReturnModerateScoreForPartialMatch() {
            executor = executor.withResponseProvider(
                            ExtractedTopicsResponse.class,
                            prompt -> new ExtractedTopicsResponse(List.of("flight booking", "weather")))
                    .withResponseProvider(
                            TopicClassificationResponse.class,
                            prompt -> new TopicClassificationResponse(List.of(
                                    new TopicClassification("flight booking", true, "flight booking", "Direct match"),
                                    new TopicClassification("weather", false, null, "Off topic"))));

            metric = TopicAdherenceMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("I need to book a flight"),
                            new HumanMessage("Also what's the weather like?")))
                    .referenceTopics(List.of("flight booking", "travel planning"))
                    .build();

            final TopicAdherenceConfig config =
                    TopicAdherenceConfig.builder().mode(Mode.F1).build();

            final Double score = metric.singleTurnScore(config, sample);

            // Precision: 1/2 = 0.5, Recall: 1/2 = 0.5, F1 = 0.5
            assertThat(score).isBetween(0.4, 0.6);
        }
    }

    @Nested
    @DisplayName("Precision Mode Evaluation")
    class PrecisionModeEvaluationTests {

        @Test
        @DisplayName("Should return high precision when few off-topic discussions")
        void shouldReturnHighPrecisionWhenFewOffTopicDiscussions() {
            executor = executor.withResponseProvider(
                            ExtractedTopicsResponse.class,
                            prompt -> new ExtractedTopicsResponse(List.of("flight booking", "weather")))
                    .withResponseProvider(
                            TopicClassificationResponse.class,
                            prompt -> new TopicClassificationResponse(List.of(
                                    new TopicClassification("flight booking", true, "flight booking", "Direct match"),
                                    new TopicClassification("weather", false, null, "Off topic"))));

            metric = TopicAdherenceMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(new HumanMessage("Flight and weather talk")))
                    .referenceTopics(List.of("flight booking"))
                    .build();

            final TopicAdherenceConfig config =
                    TopicAdherenceConfig.builder().mode(Mode.PRECISION).build();

            final Double score = metric.singleTurnScore(config, sample);

            // Precision: 1/2 = 0.5
            assertThat(score).isEqualTo(0.5);
        }
    }

    @Nested
    @DisplayName("Recall Mode Evaluation")
    class RecallModeEvaluationTests {

        @Test
        @DisplayName("Should return perfect recall when all reference topics covered")
        void shouldReturnPerfectRecallWhenAllReferenceTopicsCovered() {
            executor = executor.withResponseProvider(
                            ExtractedTopicsResponse.class,
                            prompt -> new ExtractedTopicsResponse(
                                    List.of("flight booking", "hotel reservation", "weather")))
                    .withResponseProvider(
                            TopicClassificationResponse.class,
                            prompt -> new TopicClassificationResponse(List.of(
                                    new TopicClassification("flight booking", true, "flights", "Related to flights"),
                                    new TopicClassification("hotel reservation", true, "hotels", "Related to hotels"),
                                    new TopicClassification("weather", false, null, "Off topic"))));

            metric = TopicAdherenceMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(new HumanMessage("I need flights and hotels")))
                    .referenceTopics(List.of("flights", "hotels"))
                    .build();

            final TopicAdherenceConfig config =
                    TopicAdherenceConfig.builder().mode(Mode.RECALL).build();

            final Double score = metric.singleTurnScore(config, sample);

            // Recall: 2/2 = 1.0
            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should return low recall when few reference topics covered")
        void shouldReturnLowRecallWhenFewReferenceTopicsCovered() {
            executor = executor.withResponseProvider(
                            ExtractedTopicsResponse.class,
                            prompt -> new ExtractedTopicsResponse(List.of("flight booking")))
                    .withResponseProvider(
                            TopicClassificationResponse.class,
                            prompt -> new TopicClassificationResponse(List.of(
                                    new TopicClassification("flight booking", true, "flights", "Related to flights"))));

            metric = TopicAdherenceMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(new HumanMessage("I need a flight")))
                    .referenceTopics(List.of("flights", "hotels", "car rentals"))
                    .build();

            final TopicAdherenceConfig config =
                    TopicAdherenceConfig.builder().mode(Mode.RECALL).build();

            final Double score = metric.singleTurnScore(config, sample);

            // Recall: 1/3 = 0.333...
            assertThat(score).isBetween(0.3, 0.4);
        }
    }

    @Nested
    @DisplayName("Async Execution")
    class AsyncExecutionTests {

        @Test
        @DisplayName("Should complete async evaluation successfully")
        void shouldCompleteAsyncEvaluationSuccessfully() {
            executor = executor.withResponseProvider(
                            ExtractedTopicsResponse.class,
                            prompt -> new ExtractedTopicsResponse(List.of("flight booking")))
                    .withResponseProvider(
                            TopicClassificationResponse.class,
                            prompt -> new TopicClassificationResponse(
                                    List.of(new TopicClassification("flight booking", true, "travel", "On topic"))));

            metric = TopicAdherenceMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(new HumanMessage("I need a flight")))
                    .referenceTopics(List.of("travel"))
                    .build();

            final TopicAdherenceConfig config = TopicAdherenceConfig.builder().build();

            final CompletableFuture<Double> future = metric.singleTurnScoreAsync(config, sample);

            final Double score = future.join();

            assertThat(score).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should return 0.0 when no topics extracted")
        void shouldReturnZeroWhenNoTopicsExtracted() {
            executor = executor.withResponseProvider(
                    ExtractedTopicsResponse.class, prompt -> new ExtractedTopicsResponse(List.of()));

            metric = TopicAdherenceMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(new HumanMessage("...")))
                    .referenceTopics(List.of("travel"))
                    .build();

            final TopicAdherenceConfig config = TopicAdherenceConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should throw when all models fail on classification")
        void shouldThrowWhenAllModelsFailOnClassification() {
            // Extraction succeeds but classification returns null classifications
            // which leaves modelScores empty, triggering IllegalStateException
            executor = executor.withResponseProvider(
                            ExtractedTopicsResponse.class, prompt -> new ExtractedTopicsResponse(List.of("topic")))
                    .withResponseProvider(
                            TopicClassificationResponse.class, prompt -> new TopicClassificationResponse(null));

            metric = TopicAdherenceMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(new HumanMessage("Hello")))
                    .referenceTopics(List.of("travel"))
                    .build();

            final TopicAdherenceConfig config = TopicAdherenceConfig.builder().build();

            assertThatThrownBy(() -> metric.singleTurnScore(config, sample))
                    .hasCauseInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("All models failed");
        }

        @Test
        @DisplayName("Should use default mode when not specified")
        void shouldUseDefaultModeWhenNotSpecified() {
            executor = executor.withResponseProvider(
                            ExtractedTopicsResponse.class, prompt -> new ExtractedTopicsResponse(List.of("topic")))
                    .withResponseProvider(
                            TopicClassificationResponse.class,
                            prompt -> new TopicClassificationResponse(
                                    List.of(new TopicClassification("topic", true, "ref", "Match"))));

            metric = TopicAdherenceMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(new HumanMessage("Hello")))
                    .referenceTopics(List.of("ref"))
                    .build();

            final TopicAdherenceConfig config = TopicAdherenceConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            // Default is F1 mode
            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should handle single topic single reference")
        void shouldHandleSingleTopicSingleReference() {
            executor = executor.withResponseProvider(
                            ExtractedTopicsResponse.class,
                            prompt -> new ExtractedTopicsResponse(List.of("customer support")))
                    .withResponseProvider(
                            TopicClassificationResponse.class,
                            prompt -> new TopicClassificationResponse(List.of(new TopicClassification(
                                    "customer support", true, "customer service", "Related topic"))));

            metric = TopicAdherenceMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("I need help with my order"), new AIMessage("I can help with that")))
                    .referenceTopics(List.of("customer service"))
                    .build();

            final TopicAdherenceConfig config = TopicAdherenceConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should handle duplicate matched reference topics")
        void shouldHandleDuplicateMatchedReferenceTopics() {
            executor = executor.withResponseProvider(
                            ExtractedTopicsResponse.class,
                            prompt -> new ExtractedTopicsResponse(List.of("booking flights", "airplane tickets")))
                    .withResponseProvider(
                            TopicClassificationResponse.class,
                            prompt -> new TopicClassificationResponse(List.of(
                                    new TopicClassification("booking flights", true, "travel", "On topic"),
                                    // Both match the same reference topic
                                    new TopicClassification("airplane tickets", true, "travel", "On topic"))));

            metric = TopicAdherenceMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(new HumanMessage("I need tickets")))
                    .referenceTopics(List.of("travel", "hotels"))
                    .build();

            final TopicAdherenceConfig config =
                    TopicAdherenceConfig.builder().mode(Mode.RECALL).build();

            final Double score = metric.singleTurnScore(config, sample);

            // Only 1 unique reference topic covered out of 2
            // Recall: 1/2 = 0.5
            assertThat(score).isEqualTo(0.5);
        }
    }

    @Nested
    @DisplayName("Custom Prompts")
    class CustomPromptsTests {

        @Test
        @DisplayName("Should use custom extract topics prompt")
        void shouldUseCustomExtractTopicsPrompt() {
            executor = executor.withResponseProvider(
                            ExtractedTopicsResponse.class, prompt -> new ExtractedTopicsResponse(List.of("topic")))
                    .withResponseProvider(
                            TopicClassificationResponse.class,
                            prompt -> new TopicClassificationResponse(
                                    List.of(new TopicClassification("topic", true, "ref", "Match"))));

            metric = TopicAdherenceMetric.builder()
                    .executor(executor)
                    .extractTopicsPrompt("Custom extract prompt for: {conversation}")
                    .build();

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(new HumanMessage("Test")))
                    .referenceTopics(List.of("ref"))
                    .build();

            final TopicAdherenceConfig config = TopicAdherenceConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isNotNull();
        }

        @Test
        @DisplayName("Should use custom classify topics prompt")
        void shouldUseCustomClassifyTopicsPrompt() {
            executor = executor.withResponseProvider(
                            ExtractedTopicsResponse.class, prompt -> new ExtractedTopicsResponse(List.of("topic")))
                    .withResponseProvider(
                            TopicClassificationResponse.class,
                            prompt -> new TopicClassificationResponse(
                                    List.of(new TopicClassification("topic", true, "ref", "Match"))));

            metric = TopicAdherenceMetric.builder()
                    .executor(executor)
                    .classifyTopicsPrompt("Classify: {extractedTopics} against {referenceTopics}")
                    .build();

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(new HumanMessage("Test")))
                    .referenceTopics(List.of("ref"))
                    .build();

            final TopicAdherenceConfig config = TopicAdherenceConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isNotNull();
        }
    }

    @Nested
    @DisplayName("Typed Messages Tests")
    class TypedMessagesTests {

        @Test
        @DisplayName("Should extract topics from typed messages")
        void shouldExtractTopicsFromTypedMessages() {
            executor = executor.withResponseProvider(
                            ExtractedTopicsResponse.class,
                            prompt -> new ExtractedTopicsResponse(List.of("flight booking", "travel planning")))
                    .withResponseProvider(
                            TopicClassificationResponse.class,
                            prompt -> new TopicClassificationResponse(List.of(
                                    new TopicClassification("flight booking", true, "travel", "On topic"),
                                    new TopicClassification("travel planning", true, "travel", "On topic"))));

            metric = TopicAdherenceMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("I need to book a flight to Paris"),
                            new AIMessage(
                                    "I'll help you book a flight to Paris.",
                                    List.of(new ToolCall("search_flights", Map.of("destination", "Paris")))),
                            new ToolMessage("Found 10 flights"),
                            new AIMessage("I found 10 available flights to Paris.")))
                    .referenceTopics(List.of("travel"))
                    .build();

            final TopicAdherenceConfig config = TopicAdherenceConfig.builder().build();

            final Double score = metric.multiTurnScore(config, sample);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should handle conversation with ToolMessage")
        void shouldHandleConversationWithToolMessages() {
            executor = executor.withResponseProvider(
                            ExtractedTopicsResponse.class,
                            prompt -> new ExtractedTopicsResponse(List.of("weather", "customer support")))
                    .withResponseProvider(
                            TopicClassificationResponse.class,
                            prompt -> new TopicClassificationResponse(List.of(
                                    new TopicClassification("weather", true, "weather info", "On topic"),
                                    new TopicClassification("customer support", false, null, "Off topic"))));

            metric = TopicAdherenceMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("What's the weather?"),
                            new AIMessage(
                                    "Let me check.",
                                    List.of(new ToolCall("get_weather", Map.of("location", "London")))),
                            new ToolMessage("Weather: Rainy, 15°C"),
                            new AIMessage("It's rainy with 15°C in London."),
                            new HumanMessage("Thanks for the help!"),
                            new AIMessage("You're welcome!")))
                    .referenceTopics(List.of("weather info"))
                    .build();

            final TopicAdherenceConfig config =
                    TopicAdherenceConfig.builder().mode(Mode.F1).build();

            final Double score = metric.multiTurnScore(config, sample);

            // Precision: 1/2 = 0.5, Recall: 1/1 = 1.0
            // F1 = 2 * 0.5 * 1.0 / (0.5 + 1.0) = 0.666...
            assertThat(score).isBetween(0.6, 0.7);
        }

        @Test
        @DisplayName("multiTurnScore should work correctly")
        void multiTurnScoreShouldWork() {
            executor = executor.withResponseProvider(
                            ExtractedTopicsResponse.class, prompt -> new ExtractedTopicsResponse(List.of("booking")))
                    .withResponseProvider(
                            TopicClassificationResponse.class,
                            prompt -> new TopicClassificationResponse(
                                    List.of(new TopicClassification("booking", true, "reservations", "On topic"))));

            metric = TopicAdherenceMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(new HumanMessage("Book a hotel"), new AIMessage("Hotel booked!")))
                    .referenceTopics(List.of("reservations"))
                    .build();

            final TopicAdherenceConfig config = TopicAdherenceConfig.builder().build();

            final Double score = metric.multiTurnScore(config, sample);

            assertThat(score).isEqualTo(1.0);
        }
    }
}
