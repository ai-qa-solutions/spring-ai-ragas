package ai.qa.solutions.execution.listeners;

import static org.assertj.core.api.Assertions.assertThat;

import ai.qa.solutions.execution.AggregatedExecutionResult;
import ai.qa.solutions.execution.ModelExecutionContext;
import ai.qa.solutions.execution.ModelExecutionResult;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

@DisplayName("LoggingExecutionListener Tests")
class LoggingExecutionListenerTest {

    private LoggingExecutionListener listener;
    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        listener = new LoggingExecutionListener();
        logger = (Logger) LoggerFactory.getLogger(LoggingExecutionListener.class);
        logger.setLevel(Level.DEBUG); // Enable DEBUG logging for tests

        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
    }

    @Nested
    @DisplayName("Before Execution")
    class BeforeExecution {

        @Test
        @DisplayName("Should be silent (no-op)")
        void shouldBeSilent() {
            // Given
            final ModelExecutionContext context = ModelExecutionContext.builder()
                    .executionId("test-id-123")
                    .modelId("gpt-4")
                    .metricName("AspectCritic")
                    .prompt("test prompt")
                    .build();

            // When
            listener.beforeExecution(context);

            // Then - should not log anything
            final List<ILoggingEvent> logEvents = listAppender.list;
            assertThat(logEvents).isEmpty();
        }
    }

    @Nested
    @DisplayName("After Execution")
    class AfterExecution {

        @Test
        @DisplayName("Should be silent for successful execution")
        void shouldBeSilentForSuccessfulExecution() {
            // Given
            final ModelExecutionContext context = ModelExecutionContext.builder()
                    .modelId("claude-3")
                    .metricName("Faithfulness")
                    .prompt("test")
                    .build();

            final ModelExecutionResult result = ModelExecutionResult.success(context, 0.85, "response");

            // When
            listener.afterExecution(result);

            // Then - should not log anything (silent during execution)
            final List<ILoggingEvent> logEvents = listAppender.list;
            assertThat(logEvents).isEmpty();
        }

        @Test
        @DisplayName("Should be silent for failed execution")
        void shouldBeSilentForFailedExecution() {
            // Given
            final ModelExecutionContext context = ModelExecutionContext.builder()
                    .modelId("model-1")
                    .metricName("TestMetric")
                    .prompt("test")
                    .build();

            final RuntimeException error = new RuntimeException("Connection timeout");
            final ModelExecutionResult result = ModelExecutionResult.failure(context, error);

            // When
            listener.afterExecution(result);

            // Then - should not log anything (failures shown in final report)
            final List<ILoggingEvent> logEvents = listAppender.list;
            assertThat(logEvents).isEmpty();
        }
    }

    @Nested
    @DisplayName("After Aggregation")
    class AfterAggregation {

        @Test
        @DisplayName("Should log aggregation results at INFO level")
        void shouldLogAggregationResultsAtInfoLevel() {
            // Given
            final ModelExecutionContext context = createContextWithTime("model-1");

            final ModelExecutionResult result = ModelExecutionResult.success(context, 0.75, "response");

            final AggregatedExecutionResult aggregated = AggregatedExecutionResult.builder()
                    .metricName("TestMetric")
                    .result(result)
                    .aggregatedScore(0.75)
                    .aggregationStrategy("AVERAGE")
                    .build();

            // When
            listener.afterAggregation(aggregated);

            // Then
            final List<ILoggingEvent> logEvents = listAppender.list;
            assertThat(logEvents).hasSizeGreaterThanOrEqualTo(1);

            final ILoggingEvent event = logEvents.get(0);
            assertThat(event.getLevel()).isEqualTo(Level.INFO);
            assertThat(event.getFormattedMessage())
                    .contains("TestMetric")
                    .contains("0.75")
                    .contains("AVERAGE");
        }

        @Test
        @DisplayName("Should include success rate in log")
        void shouldIncludeSuccessRateInLog() {
            // Given
            final ModelExecutionContext context1 = createContextWithTime("model-1");
            final ModelExecutionContext context2 = createContextWithTime("model-2");

            final ModelExecutionResult success = ModelExecutionResult.success(context1, 0.8, "response");
            final ModelExecutionResult failure = ModelExecutionResult.failure(context2, new RuntimeException());

            final AggregatedExecutionResult aggregated = AggregatedExecutionResult.builder()
                    .metricName("TestMetric")
                    .results(List.of(success, failure))
                    .aggregatedScore(0.8)
                    .aggregationStrategy("AVERAGE")
                    .build();

            // When
            listener.afterAggregation(aggregated);

            // Then
            final ILoggingEvent event = listAppender.list.get(0);
            assertThat(event.getFormattedMessage()).contains("Success: 1/2");
        }

        @Test
        @DisplayName("Should log WARN when no successful scores")
        void shouldLogWarnWhenNoSuccessfulScores() {
            // Given
            final ModelExecutionContext context = ModelExecutionContext.builder()
                    .modelId("model-1")
                    .metricName("TestMetric")
                    .prompt("test")
                    .build();

            final ModelExecutionResult failure = ModelExecutionResult.failure(context, new RuntimeException());

            final AggregatedExecutionResult aggregated = AggregatedExecutionResult.builder()
                    .metricName("TestMetric")
                    .results(List.of(failure))
                    .aggregatedScore(0.0)
                    .aggregationStrategy("AVERAGE")
                    .build();

            // When
            listener.afterAggregation(aggregated);

            // Then
            final List<ILoggingEvent> allEvents = listAppender.list;
            assertThat(allEvents).isNotEmpty();

            final boolean hasWarnMessage = allEvents.stream()
                    .anyMatch(e -> e.getLevel() == Level.WARN
                            && e.getFormattedMessage().contains("All")
                            && e.getFormattedMessage().contains("models failed"));

            assertThat(hasWarnMessage).isTrue();
        }
    }

    @Nested
    @DisplayName("Listener Order")
    class ListenerOrder {

        @Test
        @DisplayName("Should have highest priority (MIN_VALUE)")
        void shouldHaveHighestPriority() {
            // When
            final int order = listener.getOrder();

            // Then
            assertThat(order).isEqualTo(Integer.MIN_VALUE);
        }
    }

    @Nested
    @DisplayName("Chart Rendering")
    class ChartRendering {

        @Test
        @DisplayName("Should render ASCII chart for successful results")
        void shouldRenderAsciiChartForSuccessfulResults() {
            // Given
            final ModelExecutionContext context1 = createContextWithTime("model-1");
            final ModelExecutionContext context2 = createContextWithTime("model-2");

            final ModelExecutionResult result1 = ModelExecutionResult.success(context1, 0.7, "resp1");
            final ModelExecutionResult result2 = ModelExecutionResult.success(context2, 0.9, "resp2");

            final AggregatedExecutionResult aggregated = AggregatedExecutionResult.builder()
                    .metricName("TestMetric")
                    .results(List.of(result1, result2))
                    .aggregatedScore(0.8)
                    .aggregationStrategy("AVERAGE")
                    .build();

            // When
            listener.afterAggregation(aggregated);

            // Then
            final List<ILoggingEvent> infoEvents = listAppender.list.stream()
                    .filter(e -> e.getLevel() == Level.INFO)
                    .toList();

            assertThat(infoEvents).hasSizeGreaterThanOrEqualTo(1);

            // Check that the log contains TestMetric
            final boolean hasTestMetric =
                    infoEvents.stream().anyMatch(e -> e.getFormattedMessage().contains("TestMetric"));

            assertThat(hasTestMetric).isTrue();
        }
    }

    // ========== Helper Methods ==========

    private ModelExecutionContext createContext(final String modelId) {
        return ModelExecutionContext.builder()
                .modelId(modelId)
                .metricName("TestMetric")
                .prompt("test prompt")
                .build();
    }

    private ModelExecutionContext createContextWithTime(final String modelId) {
        return ModelExecutionContext.builder()
                .modelId(modelId)
                .metricName("TestMetric")
                .prompt("test prompt")
                .build();
    }
}
