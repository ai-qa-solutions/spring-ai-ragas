package ai.qa.solutions.execution;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import lombok.Builder;
import lombok.Value;

/**
 * Immutable result of a single model execution.
 * <p>
 * This class captures the outcome of executing a metric on a single AI model,
 * including both successful results and failures. It provides a complete view
 * of the execution for comprehensive reporting, debugging, and analysis.
 * <p>
 * The class supports two factory methods for creating results:
 * <ul>
 *   <li>{@link #success(ModelExecutionContext, Double, Object)} for successful executions</li>
 *   <li>{@link #failure(ModelExecutionContext, Throwable)} for failed executions</li>
 * </ul>
 */
@Value
@Builder
public class ModelExecutionResult {

    /**
     * The execution context containing information about the request.
     * <p>
     * This includes the model ID, metric name, prompt, and other contextual data
     * that was used to initiate this execution.
     */
    ModelExecutionContext context;

    /**
     * The extracted score from the model's response.
     * <p>
     * This optional will be empty if the execution failed or if the model's response
     * could not be parsed into a valid score.
     */
    @Builder.Default
    Optional<Double> score = Optional.empty();

    /**
     * The raw response object from the model.
     * <p>
     * This is the unparsed or structured response returned by the model, useful for
     * debugging, logging, or extracting additional information beyond the score.
     * The type of this object depends on the response type specified in the execution request.
     */
    Object rawResponse;

    /**
     * The exception that caused the execution to fail, if any.
     * <p>
     * This will be {@code null} for successful executions. For failed executions,
     * it contains the throwable that prevented the execution from completing successfully.
     */
    Throwable error;

    /**
     * Timestamp when the execution completed.
     * <p>
     * Defaults to the current time when the result object is created. Used in conjunction
     * with the start time from the context to calculate execution duration.
     */
    @Builder.Default
    Instant completedAt = Instant.now();

    /**
     * Checks if the execution completed successfully.
     * <p>
     * An execution is considered successful if it produced a score and encountered no errors.
     *
     * @return {@code true} if execution completed successfully with a score, {@code false} otherwise
     */
    public boolean isSuccess() {
        return score.isPresent() && error == null;
    }

    /**
     * Calculates the duration of this execution.
     * <p>
     * The duration is computed as the time between when the execution started
     * (from the context) and when it completed.
     *
     * @return the execution duration
     */
    public Duration getDuration() {
        return Duration.between(context.getStartedAt(), completedAt);
    }

    /**
     * Factory method to create a successful execution result.
     * <p>
     * Use this method when the model execution completed successfully and produced a score.
     *
     * @param context     the execution context
     * @param score       the extracted score (may be {@code null})
     * @param rawResponse the raw response from the model
     * @return a new successful result instance
     */
    public static ModelExecutionResult success(
            final ModelExecutionContext context, final Double score, final Object rawResponse) {
        return ModelExecutionResult.builder()
                .context(context)
                .score(Optional.ofNullable(score))
                .rawResponse(rawResponse)
                .build();
    }

    /**
     * Factory method to create a failed execution result.
     * <p>
     * Use this method when the model execution failed with an error.
     *
     * @param context the execution context
     * @param error   the error that caused the failure
     * @return a new failed result instance
     */
    public static ModelExecutionResult failure(final ModelExecutionContext context, final Throwable error) {
        return ModelExecutionResult.builder().context(context).error(error).build();
    }
}
