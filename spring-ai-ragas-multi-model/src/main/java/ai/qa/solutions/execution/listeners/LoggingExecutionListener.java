package ai.qa.solutions.execution.listeners;

import ai.qa.solutions.execution.AggregatedExecutionResult;
import ai.qa.solutions.execution.BatchExecutionContext;
import ai.qa.solutions.execution.ModelExecutionContext;
import ai.qa.solutions.execution.ModelExecutionListener;
import ai.qa.solutions.execution.ModelExecutionResult;
import ai.qa.solutions.execution.visual.AsciiGanttChart;
import ai.qa.solutions.execution.visual.AsciiRenderBarsAuto;
import java.util.Comparator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Default execution listener that logs execution events and renders visual charts.
 * <p>
 * This listener provides comprehensive logging for the multi-model execution lifecycle:
 * <ul>
 *   <li>Logs execution start with metric info and LLM request at INFO level</li>
 *   <li>Logs individual model failures at WARN level (success is silent)</li>
 *   <li>Logs aggregated results with statistics at INFO level</li>
 *   <li>Renders ASCII bar charts showing score distributions across models</li>
 * </ul>
 * <p>
 * The listener runs with the highest priority ({@link Integer#MIN_VALUE}) to ensure
 * that logging occurs before other listeners process the events.
 */
@Slf4j
public class LoggingExecutionListener implements ModelExecutionListener {

    private static final String SEPARATOR = "═".repeat(100);

    /**
     * Logs the start of batch execution with metric details and LLM request.
     * <p>
     * This method provides a clean, readable log output showing:
     * <ul>
     *   <li>Metric name and number of models</li>
     *   <li>The complete LLM prompt</li>
     *   <li>Visual separators for better readability</li>
     * </ul>
     *
     * @param context the batch execution context
     */
    @Override
    public void beforeAllExecutions(final BatchExecutionContext context) {
        final String promptPreview = formatPrompt(context.getPrompt());

        log.info(
                "\n{}\n[{}] Executing on {} models\n{}\n\nLLM Request:\n{}\n\n{}",
                SEPARATOR,
                context.getMetricName(),
                context.getModelIds().size(),
                SEPARATOR,
                promptPreview,
                SEPARATOR);
    }

    /**
     * No-op implementation. Individual model start is not logged to reduce noise.
     * Use {@link #beforeAllExecutions(BatchExecutionContext)} to see execution start.
     *
     * @param context the execution context
     */
    @Override
    public void beforeExecution(final ModelExecutionContext context) {
        // Silent - batch start is logged in beforeAllExecutions
    }

    /**
     * Formats the prompt for logging.
     * If the prompt is longer than PROMPT_PREVIEW_LENGTH, it will be truncated with ellipsis.
     *
     * @param prompt the prompt to format
     * @return formatted prompt string
     */
    private String formatPrompt(final String prompt) {
        return StringUtils.isBlank(prompt) ? "(empty)" : prompt;
    }

    /**
     * No-op implementation for individual execution completion.
     * <p>
     * Both successful and failed executions are silent during execution.
     * All results (including failures) will be shown in the final aggregation
     * report via {@link #afterAggregation(AggregatedExecutionResult)}.
     *
     * @param result the execution result (success or failure)
     */
    @Override
    public void afterExecution(final ModelExecutionResult result) {
        // Silent - all results will be shown in afterAggregation
    }

    /**
     * Simplifies error messages by removing verbose technical details.
     * <p>
     * This method extracts the root cause from common error patterns:
     * <ul>
     *   <li>JSON parsing errors (BeanOutputConverter)</li>
     *   <li>Nested RuntimeException messages</li>
     * </ul>
     *
     * @param message the original error message
     * @return simplified error message
     */
    private String simplifyErrorMessage(final String message) {
        if (message == null) {
            return "Unknown error";
        }

        // Extract root cause from RuntimeException wrapper
        if (message.contains("com.fasterxml.jackson")) {
            if (message.contains("MismatchedInputException: No content to map")) {
                return "Empty response from model";
            }
            if (message.contains("Unexpected character ('`'")) {
                return "Invalid JSON format (markdown code blocks detected)";
            }
            if (message.contains("Unexpected end-of-input")) {
                return "Incomplete JSON response";
            }
            return "JSON parsing error";
        }

        // Return first line only for other errors
        final int newlineIndex = message.indexOf('\n');
        return newlineIndex > 0 ? message.substring(0, newlineIndex) : message;
    }

    /**
     * Logs the final aggregated results at INFO level with a visual chart.
     * <p>
     * This method:
     * <ul>
     *   <li>Logs summary statistics with aggregated score and success rate</li>
     *   <li>Renders an ASCII bar chart showing score distribution across models</li>
     *   <li>Includes visual separators for better readability</li>
     * </ul>
     * <p>
     * If no successful scores are available, only logs a WARN message without rendering a chart.
     *
     * @param result the aggregated result with all execution statistics
     */
    @Override
    public void afterAggregation(final AggregatedExecutionResult result) {
        final var items = result.getResults().stream()
                .filter(r -> r.getScore().isPresent())
                .sorted(Comparator.comparing(r -> r.getContext().getModelId()))
                .map(r -> new AsciiRenderBarsAuto.Item(
                        r.getContext().getModelId() + " ", r.getScore().orElse(0.0)))
                .toList();

        if (items.isEmpty()) {
            log.warn(
                    "\n{}\n[{}] ⚠️  All {} models failed - no results to aggregate\n{}",
                    SEPARATOR,
                    result.getMetricName(),
                    result.getResults().size(),
                    SEPARATOR);
            return;
        }

        final var statsOpt = result.getScoreStatistics();
        final int width = 100;
        final int height = 0;

        final var rendered = AsciiRenderBarsAuto.renderBarsAuto(items, width, height);
        final var s = rendered.summary();

        // Build summary line
        final String summary = String.format(
                "[%s] ✓ Aggregated Score: %.2f (%s) | Success: %d/%d models | Duration: %.1fs%s",
                result.getMetricName(),
                result.getAggregatedScore(),
                result.getAggregationStrategy(),
                items.size(),
                result.getResults().size(),
                result.getTotalDuration().toMillis() / 1000.0,
                statsOpt.map(st -> String.format(
                                " | Range: %.2f-%.2f (avg %.2f)", st.getMin(), st.getMax(), st.getAverage()))
                        .orElse(""));

        // Build chart header
        final String chartHeader = String.format(
                "Score Distribution (min=%.2f [%s], max=%.2f [%s])",
                s.min(), s.minLabel().trim(), s.max(), s.maxLabel().trim());

        // Build failed models section if there are any failures
        final var failedResults =
                result.getResults().stream().filter(r -> !r.isSuccess()).toList();

        final StringBuilder output = new StringBuilder();
        output.append("\n").append(SEPARATOR).append("\n");
        output.append(summary).append("\n\n");
        output.append(chartHeader).append("\n");
        output.append(rendered.text());

        // Add execution timeline (Gantt chart)
        output.append("\n");
        output.append(AsciiGanttChart.render(result.getResults()));

        // Add failed models section if there are failures
        if (!failedResults.isEmpty()) {
            output.append("\n");
            output.append("Failed Models (")
                    .append(failedResults.size())
                    .append("/")
                    .append(result.getResults().size())
                    .append("):\n");

            for (final var failedResult : failedResults) {
                final String errorMsg =
                        simplifyErrorMessage(failedResult.getError().getMessage());
                output.append(String.format(
                        "  • %s - %s (%.1fs)\n",
                        failedResult.getContext().getModelId(),
                        errorMsg,
                        failedResult.getDuration().toMillis() / 1000.0));
            }
        }

        output.append("\n").append(SEPARATOR);
        log.info(output.toString());
    }

    /**
     * Returns the listener priority order.
     * <p>
     * This listener uses {@link Integer#MIN_VALUE} to ensure it runs first,
     * so that logging occurs before any other listener processing.
     *
     * @return {@link Integer#MIN_VALUE} (highest priority)
     */
    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }
}
