package ai.qa.solutions.metrics.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.qa.solutions.execution.StubMultiModelExecutor;
import ai.qa.solutions.execution.listener.MetricExecutionListener;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationContext;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationResult;
import ai.qa.solutions.execution.listener.dto.ModelExclusionEvent;
import ai.qa.solutions.execution.listener.dto.StepContext;
import ai.qa.solutions.execution.listener.dto.StepResults;
import ai.qa.solutions.sample.Sample;
import ai.qa.solutions.sample.message.AIMessage;
import ai.qa.solutions.sample.message.HumanMessage;
import ai.qa.solutions.sample.message.ToolCall;
import ai.qa.solutions.sample.message.ToolMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AgentGoalAccuracyMetric Tests")
class AgentGoalAccuracyMetricTest {

    private StubMultiModelExecutor executor;
    private AgentGoalAccuracyMetric metric;

    @BeforeEach
    void setUp() {
        executor = new StubMultiModelExecutor(List.of("model1"));
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should build with defaults")
        void shouldBuildWithDefaults() {
            metric = AgentGoalAccuracyMetric.builder().executor(executor).build();

            assertThat(metric).isNotNull();
            assertThat(metric.getName()).isEqualTo("AgentGoalAccuracyMetric");
        }

        @Test
        @DisplayName("Should allow custom prompts")
        void shouldAllowCustomPrompts() {
            final String customPrompt = "Custom prompt {conversation}";
            metric = AgentGoalAccuracyMetric.builder()
                    .executor(executor)
                    .inferGoalPrompt(customPrompt)
                    .build();

            assertThat(metric).isNotNull();
        }
    }

    @Nested
    @DisplayName("Config Tests")
    class ConfigTests {

        @Test
        @DisplayName("Should use default mode WITH_REFERENCE")
        void shouldUseDefaultMode() {
            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder().build();

            assertThat(config.getMode()).isEqualTo(AgentGoalAccuracyMetric.Mode.WITH_REFERENCE);
        }

        @Test
        @DisplayName("Should allow configuring mode")
        void shouldAllowConfiguringMode() {
            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITHOUT_REFERENCE)
                            .build();

            assertThat(config.getMode()).isEqualTo(AgentGoalAccuracyMetric.Mode.WITHOUT_REFERENCE);
        }

        @Test
        @DisplayName("Should allow configuring models")
        void shouldAllowConfiguringModels() {
            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .models(List.of("model1", "model2"))
                            .build();

            assertThat(config.getModels()).containsExactly("model1", "model2");
        }
    }

    @Nested
    @DisplayName("Input Validation Tests")
    class InputValidationTests {

        @Test
        @DisplayName("Should return null for missing messages")
        void shouldReturnNullForMissingMessages() {
            executor = executor.withResponseProvider(
                    AgentGoalAccuracyMetric.GoalComparisonResponse.class,
                    prompt -> new AgentGoalAccuracyMetric.GoalComparisonResponse(true, "Goal achieved"));

            metric = AgentGoalAccuracyMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder().reference("Expected goal").build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isNull();
        }

        @Test
        @DisplayName("Should return null for empty messages")
        void shouldReturnNullForEmptyMessages() {
            executor = executor.withResponseProvider(
                    AgentGoalAccuracyMetric.GoalComparisonResponse.class,
                    prompt -> new AgentGoalAccuracyMetric.GoalComparisonResponse(true, "Goal achieved"));

            metric = AgentGoalAccuracyMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of())
                    .reference("Expected goal")
                    .build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isNull();
        }
    }

    @Nested
    @DisplayName("WITH_REFERENCE Mode Tests")
    class WithReferenceModeTests {

        @Test
        @DisplayName("Should return 1.0 when goal is achieved")
        void shouldReturn1WhenGoalAchieved() {
            executor = executor.withResponseProvider(
                    AgentGoalAccuracyMetric.GoalComparisonResponse.class,
                    prompt -> new AgentGoalAccuracyMetric.GoalComparisonResponse(true, "Goal achieved"));

            metric = AgentGoalAccuracyMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("Book a flight to Paris"),
                            new AIMessage("I've booked your flight to Paris for tomorrow.")))
                    .reference("Book a flight to Paris")
                    .build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITH_REFERENCE)
                            .build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should return 0.0 when goal is not achieved")
        void shouldReturn0WhenGoalNotAchieved() {
            executor = executor.withResponseProvider(
                    AgentGoalAccuracyMetric.GoalComparisonResponse.class,
                    prompt -> new AgentGoalAccuracyMetric.GoalComparisonResponse(false, "Goal not achieved"));

            metric = AgentGoalAccuracyMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("Book a flight to Paris"),
                            new AIMessage("I'm sorry, I cannot book flights.")))
                    .reference("Book a flight to Paris")
                    .build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITH_REFERENCE)
                            .build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should fall back to WITHOUT_REFERENCE when no reference provided")
        void shouldFallBackWithoutReference() {
            final AtomicInteger callCount = new AtomicInteger(0);
            executor = executor.withResponseProvider(AgentGoalAccuracyMetric.InferredGoalResponse.class, prompt -> {
                        callCount.incrementAndGet();
                        return new AgentGoalAccuracyMetric.InferredGoalResponse("Book a flight", "User asked to book");
                    })
                    .withResponseProvider(AgentGoalAccuracyMetric.GoalComparisonResponse.class, prompt -> {
                        callCount.incrementAndGet();
                        return new AgentGoalAccuracyMetric.GoalComparisonResponse(true, "Goal achieved");
                    });

            metric = AgentGoalAccuracyMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("Book a flight to Paris"), new AIMessage("I've booked your flight.")))
                    .build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITH_REFERENCE)
                            .build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(1.0);
            assertThat(callCount.get()).isEqualTo(2); // InferGoal + EvaluateOutcome
        }
    }

    @Nested
    @DisplayName("WITHOUT_REFERENCE Mode Tests")
    class WithoutReferenceModeTests {

        @Test
        @DisplayName("Should infer goal and evaluate")
        void shouldInferGoalAndEvaluate() {
            executor = executor.withResponseProvider(
                            AgentGoalAccuracyMetric.InferredGoalResponse.class,
                            prompt -> new AgentGoalAccuracyMetric.InferredGoalResponse(
                                    "Book a flight to Paris", "User explicitly asked to book a flight"))
                    .withResponseProvider(
                            AgentGoalAccuracyMetric.GoalComparisonResponse.class,
                            prompt -> new AgentGoalAccuracyMetric.GoalComparisonResponse(
                                    true, "Flight was successfully booked"));

            metric = AgentGoalAccuracyMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("I need to book a flight to Paris for next week"),
                            new AIMessage("I've booked you a flight to Paris departing Monday.")))
                    .build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITHOUT_REFERENCE)
                            .build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should return 0.0 when goal not achieved")
        void shouldReturn0WhenGoalNotAchievedWithoutReference() {
            executor = executor.withResponseProvider(
                            AgentGoalAccuracyMetric.InferredGoalResponse.class,
                            prompt -> new AgentGoalAccuracyMetric.InferredGoalResponse(
                                    "Book a flight to Paris", "User asked to book a flight"))
                    .withResponseProvider(
                            AgentGoalAccuracyMetric.GoalComparisonResponse.class,
                            prompt -> new AgentGoalAccuracyMetric.GoalComparisonResponse(
                                    false, "Agent failed to book the flight"));

            metric = AgentGoalAccuracyMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("Book a flight to Paris"),
                            new AIMessage("I'm unable to access the booking system right now.")))
                    .build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITHOUT_REFERENCE)
                            .build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Async Execution Tests")
    class AsyncExecutionTests {

        @Test
        @DisplayName("Should execute async correctly")
        void shouldExecuteAsyncCorrectly() {
            executor = executor.withResponseProvider(
                    AgentGoalAccuracyMetric.GoalComparisonResponse.class,
                    prompt -> new AgentGoalAccuracyMetric.GoalComparisonResponse(true, "Goal achieved"));

            metric = AgentGoalAccuracyMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(new HumanMessage("Help me"), new AIMessage("Done")))
                    .reference("Help the user")
                    .build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder().build();

            final CompletableFuture<Double> future = metric.singleTurnScoreAsync(config, sample);

            assertThat(future).isNotNull();
            assertThat(future.join()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("Multi-Model Tests")
    class MultiModelTests {

        @Test
        @DisplayName("Should aggregate scores from multiple models")
        void shouldAggregateScoresFromMultipleModels() {
            final AtomicInteger callCount = new AtomicInteger(0);
            executor = new StubMultiModelExecutor(List.of("model1", "model2", "model3"))
                    .withResponseProvider(AgentGoalAccuracyMetric.GoalComparisonResponse.class, prompt -> {
                        final int call = callCount.incrementAndGet();
                        // model1: achieved, model2: not achieved, model3: achieved
                        final boolean achieved = call != 2;
                        return new AgentGoalAccuracyMetric.GoalComparisonResponse(achieved, "reason");
                    });

            metric = AgentGoalAccuracyMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(new HumanMessage("Task"), new AIMessage("Done")))
                    .reference("Complete task")
                    .build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            // Average of 1.0, 0.0, 1.0 = 0.666...
            assertThat(score).isBetween(0.6, 0.7);
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should throw when all models fail on compare")
        void shouldThrowWhenAllModelsFailOnCompare() {
            executor = executor.withModelError("model1", new RuntimeException("Model failed"));

            metric = AgentGoalAccuracyMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(new HumanMessage("Task"), new AIMessage("Done")))
                    .reference("Complete task")
                    .build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder().build();

            assertThatThrownBy(() -> metric.singleTurnScore(config, sample))
                    .hasCauseInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("All models failed");
        }

        @Test
        @DisplayName("Should throw when goal inference fails")
        void shouldThrowWhenGoalInferenceFails() {
            executor = executor.withModelError("model1", new RuntimeException("Inference failed"));

            metric = AgentGoalAccuracyMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(new HumanMessage("Task"), new AIMessage("Done")))
                    .build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITHOUT_REFERENCE)
                            .build();

            assertThatThrownBy(() -> metric.singleTurnScore(config, sample))
                    .hasCauseInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Failed to infer goal");
        }
    }

    @Nested
    @DisplayName("Listener Notification Tests")
    class ListenerNotificationTests {

        @Test
        @DisplayName("Should notify listeners during evaluation")
        void shouldNotifyListenersDuringEvaluation() {
            executor = executor.withResponseProvider(
                    AgentGoalAccuracyMetric.GoalComparisonResponse.class,
                    prompt -> new AgentGoalAccuracyMetric.GoalComparisonResponse(true, "Goal achieved"));

            final List<String> events = new ArrayList<>();
            final MetricExecutionListener listener = new MetricExecutionListener() {
                @Override
                public void beforeMetricEvaluation(final MetricEvaluationContext context) {
                    events.add("beforeMetric:" + context.getMetricName());
                }

                @Override
                public void afterMetricEvaluation(final MetricEvaluationResult result) {
                    events.add("afterMetric:" + result.getAggregatedScore());
                }

                @Override
                public void beforeStep(final StepContext context) {
                    events.add("beforeStep:" + context.getStepName());
                }

                @Override
                public void afterStep(final StepResults results) {
                    events.add("afterStep:" + results.getStepName());
                }

                @Override
                public void onModelExcluded(final ModelExclusionEvent event) {
                    events.add("excluded:" + event.getModelId());
                }

                @Override
                public MetricExecutionListener forEvaluation() {
                    return this;
                }
            };

            metric =
                    AgentGoalAccuracyMetric.builder().executor(executor).build().withListeners(List.of(listener));

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(new HumanMessage("Task"), new AIMessage("Done")))
                    .reference("Complete task")
                    .build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder().build();

            metric.singleTurnScore(config, sample);

            assertThat(events).contains("beforeMetric:AgentGoalAccuracyMetric");
            assertThat(events).contains("beforeStep:CompareOutcome");
            assertThat(events).contains("afterStep:CompareOutcome");
            assertThat(events).contains("afterMetric:1.0");
        }
    }

    @Nested
    @DisplayName("Conversation Formatting Tests")
    class ConversationFormattingTests {

        @Test
        @DisplayName("Should format multi-turn conversation correctly")
        void shouldFormatMultiTurnConversation() {
            executor = executor.withResponseProvider(
                    AgentGoalAccuracyMetric.GoalComparisonResponse.class,
                    prompt -> new AgentGoalAccuracyMetric.GoalComparisonResponse(true, "Goal achieved"));

            metric = AgentGoalAccuracyMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("You are a helpful assistant"),
                            new HumanMessage("Hello"),
                            new AIMessage("Hi, how can I help?"),
                            new HumanMessage("Book a flight"),
                            new AIMessage("Flight booked!")))
                    .reference("Book a flight")
                    .build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("Typed Messages Tests")
    class TypedMessagesTests {

        @Test
        @DisplayName("Should accept HumanMessage and AIMessage")
        void shouldAcceptHumanAndAIMessages() {
            executor = executor.withResponseProvider(
                    AgentGoalAccuracyMetric.GoalComparisonResponse.class,
                    prompt -> new AgentGoalAccuracyMetric.GoalComparisonResponse(true, "Goal achieved"));

            metric = AgentGoalAccuracyMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("Book a flight to Paris"),
                            new AIMessage("I've booked your flight to Paris.")))
                    .reference("Book a flight to Paris")
                    .build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder().build();

            final Double score = metric.multiTurnScore(config, sample);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should extract tool calls from AIMessage")
        void shouldExtractToolCallsFromAIMessages() {
            executor = executor.withResponseProvider(
                    AgentGoalAccuracyMetric.GoalComparisonResponse.class,
                    prompt -> new AgentGoalAccuracyMetric.GoalComparisonResponse(true, "Goal achieved"));

            metric = AgentGoalAccuracyMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("What's the weather in Paris?"),
                            new AIMessage(
                                    "Let me check the weather for you.",
                                    List.of(new ToolCall("get_weather", Map.of("location", "Paris")))),
                            new ToolMessage("Weather: Sunny, 22°C"),
                            new AIMessage("The weather in Paris is sunny with 22°C.")))
                    .reference("Provide weather information")
                    .build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder().build();

            final Double score = metric.multiTurnScore(config, sample);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("multiTurnScore should delegate correctly")
        void multiTurnScoreShouldDelegateCorrectly() {
            executor = executor.withResponseProvider(
                    AgentGoalAccuracyMetric.GoalComparisonResponse.class,
                    prompt -> new AgentGoalAccuracyMetric.GoalComparisonResponse(true, "Goal achieved"));

            metric = AgentGoalAccuracyMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(new HumanMessage("Help me"), new AIMessage("Done")))
                    .reference("Help the user")
                    .build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder().build();

            final Double multiTurnScore = metric.multiTurnScore(config, sample);

            assertThat(multiTurnScore).isEqualTo(1.0);
        }

        @Test
        @DisplayName("singleTurnScore should still work for backward compatibility")
        void singleTurnScoreShouldStillWork() {
            executor = executor.withResponseProvider(
                    AgentGoalAccuracyMetric.GoalComparisonResponse.class,
                    prompt -> new AgentGoalAccuracyMetric.GoalComparisonResponse(true, "Goal achieved"));

            metric = AgentGoalAccuracyMetric.builder().executor(executor).build();

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(new HumanMessage("Book a flight"), new AIMessage("Flight booked!")))
                    .reference("Book a flight")
                    .build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder().build();

            // Legacy API should still work
            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(1.0);
        }
    }
}
