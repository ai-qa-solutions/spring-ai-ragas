package ai.qa.solutions.metric;

import ai.qa.solutions.sample.Sample;
import java.util.concurrent.CompletableFuture;

/**
 * Base interface for all metrics.
 * <p>
 * Metrics support two evaluation modes:
 * <ul>
 *   <li><b>Single-turn</b>: For RAG and response quality evaluation (query → response)</li>
 *   <li><b>Multi-turn</b>: For agent evaluation (conversation history with tool calls)</li>
 * </ul>
 * <p>
 * By default, metrics only support single-turn evaluation. Multi-turn metrics
 * should extend {@link AbstractMultiTurnMetric} which overrides {@link #supportsMultiTurn()}
 * to return {@code true}.
 *
 * @param <T> the configuration type for this metric
 */
public interface Metric<T extends Metric.MetricConfiguration> {

    /**
     * Returns the name of this metric.
     *
     * @return the metric name (defaults to simple class name)
     */
    default String getName() {
        return getClass().getSimpleName();
    }

    // ==================== Single-turn evaluation ====================

    /**
     * Evaluates a single-turn sample synchronously.
     *
     * @param metricConfiguration the metric configuration
     * @param sample the sample to evaluate
     * @return the evaluation score (0.0 to 1.0), or null if evaluation failed
     */
    Double singleTurnScore(T metricConfiguration, Sample sample);

    /**
     * Evaluates a single-turn sample asynchronously.
     *
     * @param metricConfiguration the metric configuration
     * @param sample the sample to evaluate
     * @return a CompletableFuture containing the evaluation score
     */
    CompletableFuture<Double> singleTurnScoreAsync(T metricConfiguration, Sample sample);

    // ==================== Multi-turn evaluation ====================

    /**
     * Returns whether this metric supports multi-turn evaluation.
     * <p>
     * Multi-turn metrics can evaluate conversation history with multiple
     * user/assistant exchanges and tool calls.
     *
     * @return true if multi-turn evaluation is supported, false otherwise
     */
    default boolean supportsMultiTurn() {
        return false;
    }

    /**
     * Evaluates a multi-turn sample synchronously.
     * <p>
     * Multi-turn samples contain conversation history via {@link Sample#getUserInputMessages()}
     * with typed messages ({@link ai.qa.solutions.sample.message.HumanMessage},
     * {@link ai.qa.solutions.sample.message.AIMessage}, {@link ai.qa.solutions.sample.message.ToolMessage}).
     *
     * @param metricConfiguration the metric configuration
     * @param sample the sample to evaluate (must contain userInputMessages)
     * @return the evaluation score (0.0 to 1.0), or null if evaluation failed
     * @throws UnsupportedOperationException if this metric does not support multi-turn
     */
    default Double multiTurnScore(final T metricConfiguration, final Sample sample) {
        throw new UnsupportedOperationException(
                getName() + " does not support multi-turn evaluation. Use singleTurnScore() instead.");
    }

    /**
     * Evaluates a multi-turn sample asynchronously.
     *
     * @param metricConfiguration the metric configuration
     * @param sample the sample to evaluate (must contain userInputMessages)
     * @return a CompletableFuture containing the evaluation score
     * @throws UnsupportedOperationException if this metric does not support multi-turn
     */
    default CompletableFuture<Double> multiTurnScoreAsync(final T metricConfiguration, final Sample sample) {
        return CompletableFuture.supplyAsync(() -> multiTurnScore(metricConfiguration, sample));
    }

    /**
     * Marker interface for metric configuration.
     */
    interface MetricConfiguration {}
}
