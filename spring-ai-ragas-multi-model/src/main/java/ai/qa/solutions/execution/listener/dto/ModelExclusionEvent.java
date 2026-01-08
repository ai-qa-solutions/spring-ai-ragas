package ai.qa.solutions.execution.listener.dto;

import ai.qa.solutions.execution.listener.MetricExecutionListener;
import lombok.Builder;
import lombok.Value;

/**
 * Event fired when a model is excluded from further execution due to a failure.
 * <p>
 * In multi-step metric evaluation, if a model fails at any step, it is excluded
 * from all subsequent steps. This event captures which model was excluded, at which
 * step it failed, and the error that caused the exclusion.
 * <p>
 * Passed to {@link MetricExecutionListener#onModelExcluded(ModelExclusionEvent)}
 * when a model is removed from the active execution set.
 *
 * <h3>Exclusion Strategy:</h3>
 * <pre>{@code
 * Step 1: GenerateStatements
 *   model-1 ✓ success
 *   model-2 ✓ success
 *   model-3 ✗ FAILED → onModelExcluded(model-3, "GenerateStatements", error)
 *
 * Step 2: EvaluateFaithfulness
 *   model-1 ✓ success    // Only successful models continue
 *   model-2 ✓ success
 *   // model-3 skipped
 * }</pre>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * public void onModelExcluded(ModelExclusionEvent event) {
 *     log.warn("Model {} excluded after failing step {} (step {}): {}",
 *         event.getModelId(),
 *         event.getFailedStepName(),
 *         event.getFailedStepIndex() + 1,
 *         event.getCause().getMessage());
 *
 *     // Update metrics dashboard
 *     modelFailureCounter.increment(event.getModelId());
 * }
 * }</pre>
 */
@Value
@Builder
public class ModelExclusionEvent {

    /**
     * The ID of the model that was excluded.
     * <p>
     * Examples: "anthropic-claude-3-5-sonnet", "openai-gpt-4o"
     */
    String modelId;

    /**
     * The name of the step where the model failed.
     * <p>
     * Examples: "GenerateStatements", "EvaluateFaithfulness"
     */
    String failedStepName;

    /**
     * The zero-based index of the step where failure occurred.
     * <p>
     * First step is 0, second is 1, etc.
     */
    int failedStepIndex;

    /**
     * The error that caused the exclusion.
     * <p>
     * Contains the exception thrown during step execution.
     * Common causes:
     * <ul>
     *   <li>Network timeouts</li>
     *   <li>Rate limit errors</li>
     *   <li>Malformed JSON responses</li>
     *   <li>Model API errors</li>
     * </ul>
     */
    Throwable cause;
}
