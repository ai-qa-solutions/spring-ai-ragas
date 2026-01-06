package ai.qa.solutions.metric;

import ai.qa.solutions.execution.ModelResult;
import ai.qa.solutions.execution.MultiModelExecutor;
import ai.qa.solutions.execution.ScoreAggregator;
import ai.qa.solutions.execution.listener.MetricEvaluationContext;
import ai.qa.solutions.execution.listener.MetricEvaluationResult;
import ai.qa.solutions.execution.listener.MetricExecutionListener;
import ai.qa.solutions.execution.listener.ModelExclusionEvent;
import ai.qa.solutions.execution.listener.StepContext;
import ai.qa.solutions.execution.listener.StepResults;
import ai.qa.solutions.execution.listener.StepType;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract base class for metrics with explicit flow control.
 * <p>
 * Metrics now:
 * <ul>
 *   <li>Own their execution flow explicitly</li>
 *   <li>Call executor methods directly for each step</li>
 *   <li>Notify listeners at each step</li>
 *   <li>Track excluded models themselves</li>
 *   <li>Aggregate scores themselves</li>
 * </ul>
 * <p>
 * This class provides:
 * <ul>
 *   <li>Metric-level listener registration and management</li>
 *   <li>Notification methods for all lifecycle events</li>
 *   <li>Aggregation helper methods</li>
 *   <li>Access to the {@link MultiModelExecutor}</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * public class FaithfulnessMetric extends AbstractMultiModelMetric<FaithfulnessConfig> {
 *
 *     @Override
 *     public CompletableFuture<Double> singleTurnScoreAsync(
 *             FaithfulnessConfig config, Sample sample) {
 *
 *         // Create thread-safe notifier for this evaluation
 *         EvaluationNotifier notifier = createEvaluationNotifier();
 *
 *         notifier.beforeMetricEvaluation(MetricEvaluationContext.builder()
 *             .metricName(getName())
 *             .sample(sample)
 *             .totalSteps(3)
 *             .build());
 *
 *         // Step 1: Generate statements
 *         notifier.beforeStep("GenerateStatements", 0, 3);
 *         List<ModelResult<StatementsResponse>> results =
 *             executor.executeLlm(modelIds, prompt, StatementsResponse.class);
 *         notifier.afterLlmStep("GenerateStatements", 0, 3, prompt, results);
 *
 *         // ... more steps ...
 *
 *         // Aggregate
 *         double score = aggregate(modelScores);
 *
 *         notifier.afterMetricEvaluation(MetricEvaluationResult.builder()
 *             .metricName(getName())
 *             .aggregatedScore(score)
 *             .build());
 *
 *         return CompletableFuture.completedFuture(score);
 *     }
 * }
 * }</pre>
 *
 * @param <T> the configuration type for this metric
 */
@Slf4j
public abstract class AbstractMultiModelMetric<T extends Metric.MetricConfiguration> implements Metric<T> {

    /**
     * The executor for multi-model parallel execution.
     * <p>
     * Provides methods for:
     * <ul>
     *   <li>{@link MultiModelExecutor#executeLlm} - execute LLM calls</li>
     *   <li>{@link MultiModelExecutor#executeEmbedding} - execute embeddings</li>
     * </ul>
     */
    protected final MultiModelExecutor executor;

    /**
     * Default aggregation strategy for combining scores from multiple models.
     */
    protected final ScoreAggregator defaultAggregator;

    /**
     * Registered metric execution listeners, maintained in priority order.
     */
    private final List<MetricExecutionListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Creates a new metric with the specified executor and AVERAGE aggregation.
     *
     * @param executor the multi-model executor for parallel evaluation
     */
    protected AbstractMultiModelMetric(final MultiModelExecutor executor) {
        this(executor, ScoreAggregator.AVERAGE);
    }

    /**
     * Creates a new metric with the specified executor and aggregation strategy.
     *
     * @param executor          the multi-model executor for parallel evaluation
     * @param defaultAggregator the default aggregation strategy
     */
    protected AbstractMultiModelMetric(final MultiModelExecutor executor, final ScoreAggregator defaultAggregator) {
        this.executor = Objects.requireNonNull(executor, "executor");
        this.defaultAggregator = Objects.requireNonNull(defaultAggregator, "defaultAggregator");
    }

    // ============ Listener Management ============

    /**
     * Adds a listener for metric execution lifecycle events.
     *
     * @param listener the listener to add
     * @return this metric instance for method chaining
     */
    public AbstractMultiModelMetric<T> addListener(final MetricExecutionListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
        listeners.sort(Comparator.comparingInt(MetricExecutionListener::getOrder));
        log.debug(
                "Added listener {} with order {} to {}",
                listener.getClass().getSimpleName(),
                listener.getOrder(),
                getName());
        return this;
    }

    /**
     * Adds multiple listeners for metric execution lifecycle events.
     * <p>
     * This is a convenience method for fluent configuration, typically used
     * after building the metric:
     * <pre>{@code
     * AspectCriticMetric metric = AspectCriticMetric.builder()
     *     .executor(executor)
     *     .build()
     *     .withListeners(listeners);
     * }</pre>
     *
     * @param listeners the listeners to add (null-safe, ignores null elements)
     * @param <M>       the concrete metric type for fluent return
     * @return this metric instance for method chaining
     */
    @SuppressWarnings("unchecked")
    public <M extends AbstractMultiModelMetric<T>> M withListeners(
            final Collection<MetricExecutionListener> listeners) {
        if (listeners != null) {
            listeners.stream().filter(Objects::nonNull).forEach(this::addListener);
        }
        return (M) this;
    }

    /**
     * Removes a previously registered listener.
     *
     * @param listener the listener to remove
     * @return this metric instance for method chaining
     */
    public AbstractMultiModelMetric<T> removeListener(final MetricExecutionListener listener) {
        listeners.remove(listener);
        log.debug("Removed listener {} from {}", listener.getClass().getSimpleName(), getName());
        return this;
    }

    /**
     * Gets an unmodifiable view of all registered listeners.
     *
     * @return list of all listeners in priority order
     */
    public List<MetricExecutionListener> getListeners() {
        return List.copyOf(listeners);
    }

    // ============ Evaluation Session ============

    /**
     * Creates a new evaluation notifier for a single metric evaluation.
     * <p>
     * This method creates evaluation-specific listener instances by calling
     * {@link MetricExecutionListener#forEvaluation()} on each registered listener.
     * This ensures thread-safety when the same metric is evaluated concurrently.
     * <p>
     * Usage in metric implementations:
     * <pre>{@code
     * EvaluationNotifier notifier = createEvaluationNotifier();
     * notifier.beforeMetricEvaluation(context);
     * // ... evaluation steps ...
     * notifier.afterMetricEvaluation(result);
     * }</pre>
     *
     * @return a new notifier with evaluation-specific listener instances
     */
    protected EvaluationNotifier createEvaluationNotifier() {
        final List<MetricExecutionListener> evaluationListeners = listeners.stream()
                .map(MetricExecutionListener::forEvaluation)
                .sorted(Comparator.comparingInt(MetricExecutionListener::getOrder))
                .toList();
        return new EvaluationNotifier(evaluationListeners);
    }

    /**
     * Helper class for notifying listeners during a single metric evaluation.
     * <p>
     * Each instance holds its own list of evaluation-specific listeners,
     * ensuring thread-safety when the same metric is evaluated concurrently.
     */
    protected class EvaluationNotifier {
        private final List<MetricExecutionListener> evaluationListeners;

        private EvaluationNotifier(final List<MetricExecutionListener> evaluationListeners) {
            this.evaluationListeners = evaluationListeners;
        }

        /**
         * Notifies all listeners before metric evaluation begins.
         *
         * @param context the evaluation context with metric metadata
         */
        public void beforeMetricEvaluation(final MetricEvaluationContext context) {
            for (final MetricExecutionListener listener : evaluationListeners) {
                try {
                    listener.beforeMetricEvaluation(context);
                } catch (Exception e) {
                    log.error(
                            "Listener {} failed in beforeMetricEvaluation for {}: {}",
                            listener.getClass().getSimpleName(),
                            getName(),
                            e.getMessage(),
                            e);
                }
            }
        }

        /**
         * Notifies all listeners before a step executes on all models.
         *
         * @param stepName   the name of the step
         * @param stepIndex  zero-based step index
         * @param totalSteps total number of steps
         */
        public void beforeStep(final String stepName, final int stepIndex, final int totalSteps) {
            final StepContext context = StepContext.builder()
                    .stepName(stepName)
                    .stepIndex(stepIndex)
                    .totalSteps(totalSteps)
                    .build();

            for (final MetricExecutionListener listener : evaluationListeners) {
                try {
                    listener.beforeStep(context);
                } catch (Exception e) {
                    log.error(
                            "Listener {} failed in beforeStep for {} (step {}): {}",
                            listener.getClass().getSimpleName(),
                            getName(),
                            stepName,
                            e.getMessage(),
                            e);
                }
            }
        }

        /**
         * Notifies all listeners after a step completes on all models.
         *
         * @param stepName   the name of the step
         * @param stepIndex  zero-based step index
         * @param totalSteps total number of steps
         * @param stepType   the type of step (LLM, EMBEDDING, COMPUTE)
         * @param request    the request/prompt sent (null for COMPUTE steps)
         * @param results    results from all models
         */
        public void afterStep(
                final String stepName,
                final int stepIndex,
                final int totalSteps,
                final StepType stepType,
                final String request,
                final Collection<? extends ModelResult<?>> results) {
            final List<ModelResult<?>> resultList = new java.util.ArrayList<>(results);
            final StepResults stepResults = StepResults.builder()
                    .stepName(stepName)
                    .stepIndex(stepIndex)
                    .totalSteps(totalSteps)
                    .stepType(stepType)
                    .request(request)
                    .results(resultList)
                    .build();

            for (final MetricExecutionListener listener : evaluationListeners) {
                try {
                    listener.afterStep(stepResults);
                } catch (Exception e) {
                    log.error(
                            "Listener {} failed in afterStep for {} (step {}): {}",
                            listener.getClass().getSimpleName(),
                            getName(),
                            stepName,
                            e.getMessage(),
                            e);
                }
            }
        }

        /**
         * Notifies all listeners after an LLM step completes on all models.
         *
         * @param stepName   the name of the step
         * @param stepIndex  zero-based step index
         * @param totalSteps total number of steps
         * @param prompt     the prompt sent to the LLM
         * @param results    results from all models
         */
        public void afterLlmStep(
                final String stepName,
                final int stepIndex,
                final int totalSteps,
                final String prompt,
                final Collection<? extends ModelResult<?>> results) {
            afterStep(stepName, stepIndex, totalSteps, StepType.LLM, prompt, results);
        }

        /**
         * Notifies all listeners after an embedding step completes on all models.
         *
         * @param stepName   the name of the step
         * @param stepIndex  zero-based step index
         * @param totalSteps total number of steps
         * @param text       the text being embedded
         * @param results    results from all models
         */
        public void afterEmbeddingStep(
                final String stepName,
                final int stepIndex,
                final int totalSteps,
                final String text,
                final Collection<? extends ModelResult<?>> results) {
            afterEmbeddingStep(stepName, stepIndex, totalSteps, text, results, List.of());
        }

        /**
         * Notifies all listeners after an embedding step completes on all models.
         *
         * @param stepName              the name of the step
         * @param stepIndex             zero-based step index
         * @param totalSteps            total number of steps
         * @param text                  the text being embedded
         * @param results               results indexed by LLM model ID
         * @param embeddingModelResults raw results from embedding models for timeline tracking
         */
        public void afterEmbeddingStep(
                final String stepName,
                final int stepIndex,
                final int totalSteps,
                final String text,
                final Collection<? extends ModelResult<?>> results,
                final Collection<? extends ModelResult<?>> embeddingModelResults) {
            final List<ModelResult<?>> resultList = new java.util.ArrayList<>(results);
            final List<ModelResult<?>> embeddingList = new java.util.ArrayList<>(embeddingModelResults);

            final StepResults stepResults = StepResults.builder()
                    .stepName(stepName)
                    .stepIndex(stepIndex)
                    .totalSteps(totalSteps)
                    .stepType(StepType.EMBEDDING)
                    .request(text)
                    .results(resultList)
                    .embeddingModelResults(embeddingList)
                    .build();

            for (final MetricExecutionListener listener : evaluationListeners) {
                try {
                    listener.afterStep(stepResults);
                } catch (Exception e) {
                    log.error(
                            "Listener {} failed in afterStep for {} (step {}): {}",
                            listener.getClass().getSimpleName(),
                            getName(),
                            stepName,
                            e.getMessage(),
                            e);
                }
            }
        }

        /**
         * Notifies all listeners after a compute step completes.
         *
         * @param stepName   the name of the step
         * @param stepIndex  zero-based step index
         * @param totalSteps total number of steps
         * @param results    results from all models
         */
        public void afterComputeStep(
                final String stepName,
                final int stepIndex,
                final int totalSteps,
                final Collection<? extends ModelResult<?>> results) {
            afterStep(stepName, stepIndex, totalSteps, StepType.COMPUTE, null, results);
        }

        /**
         * Notifies all listeners when a model is excluded from further execution.
         *
         * @param event the exclusion event with model ID, failed step, and cause
         */
        public void onModelExcluded(final ModelExclusionEvent event) {
            for (final MetricExecutionListener listener : evaluationListeners) {
                try {
                    listener.onModelExcluded(event);
                } catch (Exception e) {
                    log.error(
                            "Listener {} failed in onModelExcluded for {} (model {}): {}",
                            listener.getClass().getSimpleName(),
                            getName(),
                            event.getModelId(),
                            e.getMessage(),
                            e);
                }
            }
        }

        /**
         * Notifies all listeners after metric evaluation completes.
         *
         * @param result the complete evaluation result with all execution metadata
         */
        public void afterMetricEvaluation(final MetricEvaluationResult result) {
            for (final MetricExecutionListener listener : evaluationListeners) {
                try {
                    listener.afterMetricEvaluation(result);
                } catch (Exception e) {
                    log.error(
                            "Listener {} failed in afterMetricEvaluation for {}: {}",
                            listener.getClass().getSimpleName(),
                            getName(),
                            e.getMessage(),
                            e);
                }
            }
        }
    }

    // ============ Aggregation Helpers ============

    /**
     * Aggregates scores from successful model results using the default aggregator.
     *
     * @param modelScores map of model ID to score
     * @return aggregated score
     * @throws IllegalStateException if no successful scores to aggregate
     */
    protected double aggregate(final Map<String, Double> modelScores) {
        return aggregate(modelScores, defaultAggregator);
    }

    /**
     * Aggregates scores from successful model results using a custom aggregator.
     *
     * @param modelScores map of model ID to score
     * @param aggregator  the aggregation strategy to use
     * @return aggregated score
     * @throws IllegalStateException if no successful scores to aggregate
     */
    protected double aggregate(final Map<String, Double> modelScores, final ScoreAggregator aggregator) {
        if (modelScores.isEmpty()) {
            throw new IllegalStateException("No successful model scores to aggregate for metric: " + getName());
        }
        return aggregator.aggregate(List.copyOf(modelScores.values()));
    }
}
