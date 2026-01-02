package ai.qa.solutions.execution;

/**
 * Listener interface for model execution lifecycle events.
 * <p>
 * Implementations can be used for:
 * <ul>
 *   <li>Logging and monitoring</li>
 *   <li>Metrics collection (latency, success rates)</li>
 *   <li>Publishing results to external services</li>
 *   <li>Caching responses</li>
 *   <li>Alerting on failures</li>
 * </ul>
 * <p>
 * All methods have default no-op implementations, allowing listeners
 * to override only the events they care about.
 * <p>
 * <b>Thread Safety:</b> Listener methods may be called concurrently
 * from multiple threads. Implementations must be thread-safe.
 */
public interface ModelExecutionListener {

    /**
     * Called before a model execution starts.
     *
     * @param context execution context with request details
     */
    default void beforeExecution(final ModelExecutionContext context) {
        // No-op by default
    }

    /**
     * Called after a single model execution completes (success or failure).
     *
     * @param result execution result with score or error
     */
    default void afterExecution(final ModelExecutionResult result) {
        // No-op by default
    }

    /**
     * Called after all model executions complete and results are aggregated.
     *
     * @param result aggregated result with statistics
     */
    default void afterAggregation(final AggregatedExecutionResult result) {
        // No-op by default
    }

    /**
     * @return listener priority (lower values execute first). Default is 0.
     */
    default int getOrder() {
        return 0;
    }
}
