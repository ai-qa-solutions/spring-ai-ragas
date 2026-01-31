package ai.qa.solutions.metrics.agent;

import static org.assertj.core.api.Assertions.assertThat;

import ai.qa.solutions.execution.StubMultiModelExecutor;
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

@DisplayName("ToolCallAccuracyMetric")
class ToolCallAccuracyMetricTest {

    private StubMultiModelExecutor executor;
    private ToolCallAccuracyMetric metric;

    @BeforeEach
    void setUp() {
        executor = new StubMultiModelExecutor(List.of("model1"));
        metric = ToolCallAccuracyMetric.builder().executor(executor).build();
    }

    @Nested
    @DisplayName("Builder and Config")
    class BuilderAndConfigTests {

        @Test
        @DisplayName("Should create metric with builder")
        void shouldCreateMetricWithBuilder() {
            final ToolCallAccuracyMetric m =
                    ToolCallAccuracyMetric.builder().executor(executor).build();

            assertThat(m).isNotNull();
            assertThat(m.getName()).isEqualTo("ToolCallAccuracyMetric");
        }

        @Test
        @DisplayName("Should create config with default values")
        void shouldCreateConfigWithDefaults() {
            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder().build();

            assertThat(config.getMode()).isEqualTo(ToolCallAccuracyMetric.Mode.STRICT);
            assertThat(config.getArgumentMatchThreshold()).isEqualTo(0.8);
        }

        @Test
        @DisplayName("Should allow config customization")
        void shouldAllowConfigCustomization() {
            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.FLEXIBLE)
                            .argumentMatchThreshold(0.7)
                            .model("custom-model")
                            .build();

            assertThat(config.getMode()).isEqualTo(ToolCallAccuracyMetric.Mode.FLEXIBLE);
            assertThat(config.getArgumentMatchThreshold()).isEqualTo(0.7);
            assertThat(config.getModels()).containsExactly("custom-model");
        }
    }

    @Nested
    @DisplayName("Input Validation")
    class InputValidationTests {

        @Test
        @DisplayName("Should return null when no tool calls provided")
        void shouldReturnNullWhenNoToolCalls() {
            final Sample sample = Sample.builder()
                    .referenceToolCalls(List.of(new Sample.ToolCall("search", Map.of("query", "test"))))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isNull();
        }

        @Test
        @DisplayName("Should return null when no reference tool calls provided")
        void shouldReturnNullWhenNoReferenceToolCalls() {
            final Sample sample = Sample.builder()
                    .toolCalls(List.of(new Sample.ToolCall("search", Map.of("query", "test"))))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isNull();
        }

        @Test
        @DisplayName("Should return null when tool calls list is empty")
        void shouldReturnNullWhenToolCallsEmpty() {
            final Sample sample = Sample.builder()
                    .toolCalls(List.of())
                    .referenceToolCalls(List.of(new Sample.ToolCall("search", Map.of("query", "test"))))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isNull();
        }
    }

    @Nested
    @DisplayName("STRICT Mode Evaluation")
    class StrictModeTests {

        @Test
        @DisplayName("Should return 1.0 for perfect match")
        void shouldReturnOneForPerfectMatch() {
            final Sample sample = Sample.builder()
                    .toolCalls(List.of(
                            new Sample.ToolCall("search", Map.of("query", "weather Paris")),
                            new Sample.ToolCall("get_time", Map.of("timezone", "UTC"))))
                    .referenceToolCalls(List.of(
                            new Sample.ToolCall("search", Map.of("query", "weather Paris")),
                            new Sample.ToolCall("get_time", Map.of("timezone", "UTC"))))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.STRICT)
                            .build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should return 0.0 for completely different tools")
        void shouldReturnZeroForDifferentTools() {
            final Sample sample = Sample.builder()
                    .toolCalls(List.of(new Sample.ToolCall("search", Map.of("query", "weather"))))
                    .referenceToolCalls(List.of(new Sample.ToolCall("get_time", Map.of("timezone", "UTC"))))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.STRICT)
                            .build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 0.0 when tool names match but arguments differ")
        void shouldReturnZeroWhenArgumentsDiffer() {
            final Sample sample = Sample.builder()
                    .toolCalls(List.of(new Sample.ToolCall("search", Map.of("query", "weather Paris"))))
                    .referenceToolCalls(List.of(new Sample.ToolCall("search", Map.of("query", "weather London"))))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.STRICT)
                            .build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should calculate F1 score for partial match")
        void shouldCalculateF1ForPartialMatch() {
            final Sample sample = Sample.builder()
                    .toolCalls(List.of(
                            new Sample.ToolCall("search", Map.of("query", "weather")),
                            new Sample.ToolCall("wrong_tool", Map.of())))
                    .referenceToolCalls(List.of(
                            new Sample.ToolCall("search", Map.of("query", "weather")),
                            new Sample.ToolCall("get_time", Map.of())))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.STRICT)
                            .build();

            final Double score = metric.singleTurnScore(config, sample);

            // 1 TP, 1 FP, 1 FN
            // Precision = 1/2 = 0.5, Recall = 1/2 = 0.5
            // F1 = 2 * 0.5 * 0.5 / (0.5 + 0.5) = 0.5
            assertThat(score).isCloseTo(0.5, org.assertj.core.api.Assertions.within(0.01));
        }

        @Test
        @DisplayName("Should match tools regardless of order")
        void shouldMatchToolsRegardlessOfOrder() {
            final Sample sample = Sample.builder()
                    .toolCalls(List.of(
                            new Sample.ToolCall("get_time", Map.of("timezone", "UTC")),
                            new Sample.ToolCall("search", Map.of("query", "weather"))))
                    .referenceToolCalls(List.of(
                            new Sample.ToolCall("search", Map.of("query", "weather")),
                            new Sample.ToolCall("get_time", Map.of("timezone", "UTC"))))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.STRICT)
                            .build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("FLEXIBLE Mode Evaluation")
    class FlexibleModeTests {

        @Test
        @DisplayName("Should return 1.0 for perfect match in flexible mode")
        void shouldReturnOneForPerfectMatchInFlexibleMode() {
            final Sample sample = Sample.builder()
                    .toolCalls(List.of(new Sample.ToolCall("search", Map.of("query", "weather", "limit", 10))))
                    .referenceToolCalls(List.of(new Sample.ToolCall("search", Map.of("query", "weather", "limit", 10))))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.FLEXIBLE)
                            .argumentMatchThreshold(0.8)
                            .build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should match with partial arguments above threshold")
        void shouldMatchWithPartialArgumentsAboveThreshold() {
            final Sample sample = Sample.builder()
                    .toolCalls(List.of(new Sample.ToolCall("search", Map.of("query", "weather", "limit", 10))))
                    .referenceToolCalls(List.of(new Sample.ToolCall("search", Map.of("query", "weather", "limit", 20))))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.FLEXIBLE)
                            .argumentMatchThreshold(0.5) // 50% match required
                            .build();

            final Double score = metric.singleTurnScore(config, sample);

            // query matches (1/2 = 0.5), which meets threshold
            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should not match with partial arguments below threshold")
        void shouldNotMatchWithPartialArgumentsBelowThreshold() {
            final Sample sample = Sample.builder()
                    .toolCalls(
                            List.of(new Sample.ToolCall("search", Map.of("query", "wrong", "limit", 999, "offset", 0))))
                    .referenceToolCalls(List.of(
                            new Sample.ToolCall("search", Map.of("query", "weather", "limit", 10, "offset", 5))))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.FLEXIBLE)
                            .argumentMatchThreshold(0.9) // 90% match required
                            .build();

            final Double score = metric.singleTurnScore(config, sample);

            // 0/3 arguments match = 0%, below 90% threshold
            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should handle tools with no arguments")
        void shouldHandleToolsWithNoArguments() {
            final Sample sample = Sample.builder()
                    .toolCalls(List.of(new Sample.ToolCall("get_status", Map.of())))
                    .referenceToolCalls(List.of(new Sample.ToolCall("get_status", Map.of())))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.FLEXIBLE)
                            .build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should handle null arguments")
        void shouldHandleNullArguments() {
            final Sample sample = Sample.builder()
                    .toolCalls(List.of(new Sample.ToolCall("get_status", null)))
                    .referenceToolCalls(List.of(new Sample.ToolCall("get_status", null)))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.FLEXIBLE)
                            .build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("Async Execution")
    class AsyncExecutionTests {

        @Test
        @DisplayName("Should execute asynchronously")
        void shouldExecuteAsynchronously() {
            final Sample sample = Sample.builder()
                    .toolCalls(List.of(new Sample.ToolCall("search", Map.of("query", "test"))))
                    .referenceToolCalls(List.of(new Sample.ToolCall("search", Map.of("query", "test"))))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder().build();

            final CompletableFuture<Double> future = metric.singleTurnScoreAsync(config, sample);

            assertThat(future).isNotNull();
            assertThat(future.join()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle more actual calls than reference")
        void shouldHandleMoreActualCallsThanReference() {
            final Sample sample = Sample.builder()
                    .toolCalls(List.of(
                            new Sample.ToolCall("search", Map.of("query", "weather")),
                            new Sample.ToolCall("get_time", Map.of()),
                            new Sample.ToolCall("extra_tool", Map.of())))
                    .referenceToolCalls(List.of(new Sample.ToolCall("search", Map.of("query", "weather"))))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.STRICT)
                            .build();

            final Double score = metric.singleTurnScore(config, sample);

            // 1 TP, 2 FP, 0 FN
            // Precision = 1/3, Recall = 1/1 = 1.0
            // F1 = 2 * (1/3) * 1 / (1/3 + 1) = 0.5
            assertThat(score).isCloseTo(0.5, org.assertj.core.api.Assertions.within(0.01));
        }

        @Test
        @DisplayName("Should handle more reference calls than actual")
        void shouldHandleMoreReferenceCallsThanActual() {
            final Sample sample = Sample.builder()
                    .toolCalls(List.of(new Sample.ToolCall("search", Map.of("query", "weather"))))
                    .referenceToolCalls(List.of(
                            new Sample.ToolCall("search", Map.of("query", "weather")),
                            new Sample.ToolCall("get_time", Map.of()),
                            new Sample.ToolCall("get_date", Map.of())))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.STRICT)
                            .build();

            final Double score = metric.singleTurnScore(config, sample);

            // 1 TP, 0 FP, 2 FN
            // Precision = 1/1 = 1.0, Recall = 1/3
            // F1 = 2 * 1 * (1/3) / (1 + 1/3) = 0.5
            assertThat(score).isCloseTo(0.5, org.assertj.core.api.Assertions.within(0.01));
        }

        @Test
        @DisplayName("Should handle duplicate tool calls")
        void shouldHandleDuplicateToolCalls() {
            final Sample sample = Sample.builder()
                    .toolCalls(List.of(
                            new Sample.ToolCall("search", Map.of("query", "weather")),
                            new Sample.ToolCall("search", Map.of("query", "weather"))))
                    .referenceToolCalls(List.of(
                            new Sample.ToolCall("search", Map.of("query", "weather")),
                            new Sample.ToolCall("search", Map.of("query", "weather"))))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.STRICT)
                            .build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("Typed Messages Tests")
    class TypedMessagesTests {

        @Test
        @DisplayName("Should extract tool calls from AIMessage")
        void shouldExtractToolCallsFromAIMessages() {
            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("Search for flights to NYC"),
                            new AIMessage(
                                    "Searching flights...",
                                    List.of(new ToolCall("search_flights", Map.of("destination", "NYC")))),
                            new ToolMessage("Found 5 flights"),
                            new AIMessage("I found 5 available flights.")))
                    .toolCalls(List.of(new Sample.ToolCall("search_flights", Map.of("destination", "NYC"))))
                    .referenceToolCalls(List.of(new Sample.ToolCall("search_flights", Map.of("destination", "NYC"))))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.STRICT)
                            .build();

            final Double score = metric.multiTurnScore(config, sample);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should compare with reference tool calls")
        void shouldCompareWithReferenceToolCalls() {
            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("Get weather and time"),
                            new AIMessage(
                                    "Let me check both for you.",
                                    List.of(
                                            new ToolCall("get_weather", Map.of("location", "Paris")),
                                            new ToolCall("get_time", Map.of()))),
                            new ToolMessage("Weather: Sunny, 22°C"),
                            new ToolMessage("Time: 14:30"),
                            new AIMessage("It's sunny at 22°C and the time is 14:30.")))
                    .toolCalls(List.of(
                            new Sample.ToolCall("get_weather", Map.of("location", "Paris")),
                            new Sample.ToolCall("get_time", Map.of())))
                    .referenceToolCalls(List.of(
                            new Sample.ToolCall("get_weather", Map.of("location", "Paris")),
                            new Sample.ToolCall("get_time", Map.of())))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.STRICT)
                            .build();

            final Double score = metric.multiTurnScore(config, sample);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("multiTurnScore should work correctly")
        void multiTurnScoreShouldWork() {
            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("Search for weather"),
                            new AIMessage("Searching...", List.of(new ToolCall("search", Map.of("query", "weather")))),
                            new ToolMessage("Weather: Sunny"),
                            new AIMessage("It's sunny today.")))
                    .toolCalls(List.of(new Sample.ToolCall("search", Map.of("query", "weather"))))
                    .referenceToolCalls(List.of(new Sample.ToolCall("search", Map.of("query", "weather"))))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder().build();

            final Double score = metric.multiTurnScore(config, sample);

            assertThat(score).isEqualTo(1.0);
        }
    }
}
