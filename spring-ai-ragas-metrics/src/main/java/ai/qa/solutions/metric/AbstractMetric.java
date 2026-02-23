package ai.qa.solutions.metric;

import ai.qa.solutions.execution.listener.MetricExecutionListener;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationContext;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationResult;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract base class for all metrics, providing listener management and evaluation notifications.
 * <p>
 * This class serves as the common base for both LLM-based metrics (via {@link AbstractMultiModelMetric})
 * and NLP metrics that compute scores algorithmically without LLM calls.
 * <p>
 * Provides:
 * <ul>
 *   <li>Thread-safe listener registration and management</li>
 *   <li>{@link EvaluationNotifier} for notifying listeners at evaluation start/end</li>
 *   <li>Evaluation-scoped listener instances via {@link MetricExecutionListener#forEvaluation()}</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * public class MyMetric extends AbstractMetric<MyConfig> {
 *
 *     @Override
 *     public Double singleTurnScore(MyConfig config, Sample sample) {
 *         EvaluationNotifier notifier = createEvaluationNotifier();
 *         notifier.beforeMetricEvaluation(MetricEvaluationContext.builder()
 *             .metricName(getName())
 *             .sample(sample)
 *             .config(config)
 *             .totalSteps(1)
 *             .build());
 *
 *         double score = computeScore(sample);
 *
 *         notifier.afterMetricEvaluation(MetricEvaluationResult.builder()
 *             .metricName(getName())
 *             .aggregatedScore(score)
 *             .sample(sample)
 *             .config(config)
 *             .build());
 *
 *         return score;
 *     }
 * }
 * }</pre>
 *
 * @param <T> the configuration type for this metric
 */
@Slf4j
public abstract class AbstractMetric<T extends Metric.MetricConfiguration> implements Metric<T> {

    /**
     * Registered metric execution listeners, maintained in priority order.
     */
    private final CopyOnWriteArrayList<MetricExecutionListener> listeners = new CopyOnWriteArrayList<>();

    // ============ Listener Management ============

    /**
     * Adds a listener for metric execution lifecycle events.
     *
     * @param listener the listener to add
     * @return this metric instance for method chaining
     */
    public AbstractMetric<T> addListener(final MetricExecutionListener listener) {
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
     * MyMetric metric = new MyMetric()
     *     .withListeners(listeners);
     * }</pre>
     *
     * @param listeners the listeners to add (null-safe, ignores null elements)
     * @param <M>       the concrete metric type for fluent return
     * @return this metric instance for method chaining
     */
    @SuppressWarnings("unchecked")
    public <M extends AbstractMetric<T>> M withListeners(final Collection<MetricExecutionListener> listeners) {
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
    public AbstractMetric<T> removeListener(final MetricExecutionListener listener) {
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
     * // ... evaluation logic ...
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
     * <p>
     * The notifier provides exactly two notification methods corresponding to the
     * {@link MetricExecutionListener} callbacks:
     * <ul>
     *   <li>{@link #beforeMetricEvaluation(MetricEvaluationContext)} - before evaluation starts</li>
     *   <li>{@link #afterMetricEvaluation(MetricEvaluationResult)} - after evaluation completes</li>
     * </ul>
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
                } catch (final Exception e) {
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
         * Notifies all listeners after metric evaluation completes.
         *
         * @param result the complete evaluation result with all execution metadata
         */
        public void afterMetricEvaluation(final MetricEvaluationResult result) {
            for (final MetricExecutionListener listener : evaluationListeners) {
                try {
                    listener.afterMetricEvaluation(result);
                } catch (final Exception e) {
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
}
