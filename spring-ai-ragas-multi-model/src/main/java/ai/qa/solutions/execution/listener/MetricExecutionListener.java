package ai.qa.solutions.execution.listener;

import ai.qa.solutions.execution.listener.dto.*;

/**
 * Listener interface for observing metric execution lifecycle events.
 * <p>
 * This listener provides visibility into metric evaluation start and completion.
 * All intermediate step data (step results, model exclusions) is accumulated by the metric
 * and delivered in the enriched {@link MetricEvaluationResult}.
 *
 * <h3>Lifecycle Events:</h3>
 * <pre>{@code
 * beforeMetricEvaluation()  // Once before evaluation starts
 *   ↓
 * [Metric executes all steps internally, accumulating results]
 *   ↓
 * afterMetricEvaluation()   // Once after all steps complete (with enriched result)
 * }</pre>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * FaithfulnessMetric metric = ...;
 * metric.addListener(new MetricExecutionListener() {
 *     @Override
 *     public void afterMetricEvaluation(MetricEvaluationResult result) {
 *         log.info("=== {} Evaluation Complete ===", result.getMetricName());
 *         log.info("Final Score: {}", result.getAggregatedScore());
 *         log.info("Steps: {}", result.getSteps().size());
 *         log.info("Exclusions: {}", result.getExclusions().size());
 *     }
 * });
 * }</pre>
 *
 * @see MetricEvaluationContext
 * @see MetricEvaluationResult
 */
public interface MetricExecutionListener {

    /**
     * Called once before metric evaluation begins.
     * <p>
     * Provides context about the metric, sample, and models that will be used.
     *
     * @param context the evaluation context containing metric metadata
     */
    default void beforeMetricEvaluation(MetricEvaluationContext context) {}

    /**
     * Called once after metric evaluation completes.
     * <p>
     * Provides the final aggregated result along with per-model scores,
     * all step results, model exclusion events, and total duration.
     *
     * @param result the complete evaluation result with all execution metadata
     */
    default void afterMetricEvaluation(MetricEvaluationResult result) {}

    /**
     * Determines the execution order of multiple listeners.
     * <p>
     * Listeners with lower order values execute first. Default is 0.
     *
     * @return the order value for this listener
     */
    default int getOrder() {
        return 0;
    }

    /**
     * Creates a listener instance for a single metric evaluation.
     * <p>
     * Stateless listeners can return {@code this}. Stateful listeners that accumulate
     * data between {@link #beforeMetricEvaluation} and {@link #afterMetricEvaluation}
     * should return a new instance to ensure thread-safety in parallel execution.
     * <p>
     * This method is called by the metric before each evaluation begins.
     *
     * @return a listener instance for the evaluation (may be {@code this} or a new instance)
     */
    default MetricExecutionListener forEvaluation() {
        return this;
    }
}
