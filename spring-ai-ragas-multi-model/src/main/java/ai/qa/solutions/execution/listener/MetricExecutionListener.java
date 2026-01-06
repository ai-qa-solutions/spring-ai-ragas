package ai.qa.solutions.execution.listener;

/**
 * Listener interface for observing metric execution lifecycle events.
 * <p>
 * This listener provides full visibility into metric evaluation:
 * <ul>
 *   <li>All execution steps in multi-step metrics</li>
 *   <li>Results from all models for each step</li>
 *   <li>Model exclusion events when failures occur</li>
 *   <li>Complete metadata and error information</li>
 * </ul>
 * <p>
 * Listeners are registered directly on metric instances, allowing for metric-specific
 * observation patterns and custom logging/monitoring strategies.
 *
 * <h3>Lifecycle Events:</h3>
 * <pre>{@code
 * beforeMetricEvaluation()  // Once before evaluation starts
 *   ↓
 * [For each step]
 *   beforeStep()            // Before step executes on all models
 *   afterStep()             // After step completes on all models
 *   onModelExcluded()       // For each model that failed (if any)
 *   ↓
 * afterMetricEvaluation()   // Once after all steps complete
 * }</pre>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * FaithfulnessMetric metric = ...;
 * metric.addListener(new MetricExecutionListener() {
 *     @Override
 *     public void beforeStep(StepContext context) {
 *         log.info("Starting step {}/{}: {}",
 *             context.getStepIndex() + 1,
 *             context.getTotalSteps(),
 *             context.getStepName());
 *     }
 *
 *     @Override
 *     public void afterStep(StepResults results) {
 *         log.info("Step {} completed: {} successful, {} failed in {}ms",
 *             results.getStepName(),
 *             results.getSuccessCount(),
 *             results.getFailCount(),
 *             results.getTotalDuration().toMillis());
 *     }
 *
 *     @Override
 *     public void onModelExcluded(ModelExclusionEvent event) {
 *         log.warn("Model {} excluded after step {}: {}",
 *             event.getModelId(),
 *             event.getFailedStepName(),
 *             event.getCause().getMessage());
 *     }
 * });
 * }</pre>
 *
 * @see MetricEvaluationContext
 * @see StepContext
 * @see StepResults
 * @see ModelExclusionEvent
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
     * Called before a step executes on all models.
     * <p>
     * Provides step metadata including name, index, and total steps.
     *
     * @param context the step context containing step metadata
     */
    default void beforeStep(StepContext context) {}

    /**
     * Called after a step completes on all models.
     * <p>
     * Provides results from all models that executed this step,
     * including both successful and failed results.
     *
     * @param results the step results containing all model results
     */
    default void afterStep(StepResults results) {}

    /**
     * Called when a model is excluded from further steps due to a failure.
     * <p>
     * Once a model fails a step, it is excluded from all subsequent steps.
     * This method provides visibility into which models were excluded and why.
     *
     * @param event the exclusion event with model ID, failed step, and cause
     */
    default void onModelExcluded(ModelExclusionEvent event) {}

    /**
     * Called once after metric evaluation completes.
     * <p>
     * Provides the final aggregated result along with per-model scores
     * and total duration.
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
