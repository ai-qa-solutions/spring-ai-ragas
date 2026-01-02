package ai.qa.solutions.execution.listeners;

import ai.qa.solutions.execution.AggregatedExecutionResult;
import ai.qa.solutions.execution.ModelExecutionContext;
import ai.qa.solutions.execution.ModelExecutionListener;
import ai.qa.solutions.execution.ModelExecutionResult;
import ai.qa.solutions.execution.visual.AsciiRenderBarsAuto;
import lombok.extern.slf4j.Slf4j;

/**
 * Default execution listener that logs execution events and renders visual charts.
 * <p>
 * This listener provides comprehensive logging for the multi-model execution lifecycle:
 * <ul>
 *   <li>Logs each model execution start at DEBUG level</li>
 *   <li>Logs individual model completion with timing and scores at DEBUG level</li>
 *   <li>Logs aggregated results with statistics at INFO level</li>
 *   <li>Renders ASCII bar charts showing score distributions across models</li>
 * </ul>
 * <p>
 * The listener runs with the highest priority ({@link Integer#MIN_VALUE}) to ensure
 * that logging occurs before other listeners process the events.
 */
@Slf4j
public class LoggingExecutionListener implements ModelExecutionListener {

    /**
     * Logs the start of a model execution at DEBUG level.
     * <p>
     * The log message includes the metric name, model ID, and execution ID for tracing.
     *
     * @param context the execution context
     */
    @Override
    public void beforeExecution(final ModelExecutionContext context) {
        log.debug(
                "[{}] Starting execution on model {} (executionId={})",
                context.getMetricName(),
                context.getModelId(),
                context.getExecutionId());
    }

    /**
     * Logs the completion of a single model execution at DEBUG or WARN level.
     * <p>
     * For successful executions, logs at DEBUG level with the score and duration.
     * For failed executions, logs at WARN level with the error message and duration.
     *
     * @param result the execution result (success or failure)
     */
    @Override
    public void afterExecution(final ModelExecutionResult result) {
        final var ctx = result.getContext();
        if (result.isSuccess()) {
            log.debug(
                    "[{}] Model {} completed in {}ms with score {}",
                    ctx.getMetricName(),
                    ctx.getModelId(),
                    result.getDuration().toMillis(),
                    result.getScore().orElse(null));
        } else {
            log.warn(
                    "[{}] Model {} failed after {}ms: {}",
                    ctx.getMetricName(),
                    ctx.getModelId(),
                    result.getDuration().toMillis(),
                    result.getError().getMessage());
        }
    }

    /**
     * Logs the final aggregated results at INFO level with a visual chart.
     * <p>
     * This method:
     * <ul>
     *   <li>Logs summary statistics (aggregated score, strategy, success rate, duration)</li>
     *   <li>Renders an ASCII bar chart showing score distribution across models</li>
     *   <li>Includes min/max/average scores in both the summary and chart header</li>
     * </ul>
     * <p>
     * If no successful scores are available, only logs a DEBUG message without rendering a chart.
     *
     * @param result the aggregated result with all execution statistics
     */
    @Override
    public void afterAggregation(final AggregatedExecutionResult result) {
        final var statsOpt = result.getScoreStatistics();

        log.info(
                "[{}] Aggregation complete: score={}, strategy={}, success={}/{}, duration={}ms{}",
                result.getMetricName(),
                result.getAggregatedScore(),
                result.getAggregationStrategy(),
                result.getSuccessfulResults().size(),
                result.getResults().size(),
                result.getTotalDuration().toMillis(),
                statsOpt.map(s -> String.format(
                                " (min=%.2f, max=%.2f, avg=%.2f)", s.getMin(), s.getMax(), s.getAverage()))
                        .orElse(""));

        final var items = result.getResults().stream()
                .filter(r -> r.getScore().isPresent())
                .map(r -> new AsciiRenderBarsAuto.Item(
                        r.getContext().getModelId() + " ", r.getScore().orElse(0.0)))
                .toList();

        if (!items.isEmpty()) {
            final int width = 100;
            final int height = 0;

            final var rendered = AsciiRenderBarsAuto.renderBarsAuto(items, width, height);
            final var s = rendered.summary();

            final String header = String.format(
                    "[%s] Scores by model (n=%d, min=%.2f [%s], max=%.2f [%s], avg=%.2f, aggregated=%s, strategy=%s)",
                    result.getMetricName(),
                    items.size(),
                    s.min(),
                    s.minLabel(),
                    s.max(),
                    s.maxLabel(),
                    s.avg(),
                    result.getAggregatedScore(),
                    result.getAggregationStrategy());

            log.info("\n{}\n{}", header, rendered.text());
        } else {
            log.debug("[{}] No successful scores to render chart.", result.getMetricName());
        }
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
