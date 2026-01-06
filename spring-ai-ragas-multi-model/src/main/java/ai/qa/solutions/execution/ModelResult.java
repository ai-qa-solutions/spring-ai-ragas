package ai.qa.solutions.execution;

import java.time.Duration;
import java.util.function.Function;

/**
 * Result of a single model execution (LLM or embedding).
 * <p>
 * This record encapsulates all information about a model call:
 * <ul>
 *   <li>Which model was called</li>
 *   <li>The result (if successful)</li>
 *   <li>How long it took</li>
 *   <li>What request was sent</li>
 *   <li>Any error that occurred</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * List<ModelResult<Response>> results = executor.executeLlm(prompt, Response.class);
 *
 * for (ModelResult<Response> result : results) {
 *     if (result.isSuccess()) {
 *         double score = result.result().getScore();
 *         log.info("Model {} scored {}", result.modelId(), score);
 *     } else {
 *         log.warn("Model {} failed: {}", result.modelId(), result.error().getMessage());
 *     }
 * }
 * }</pre>
 *
 * @param <R> the result type
 * @param modelId  the ID of the model that was called
 * @param result   the result value, null if error occurred
 * @param duration how long the call took
 * @param request  the request that was sent (prompt for LLM, text for embedding)
 * @param error    the error that occurred, null if successful
 */
public record ModelResult<R>(String modelId, R result, Duration duration, String request, Throwable error) {

    /**
     * Checks if this execution was successful.
     *
     * @return true if no error occurred and result is available
     */
    public boolean isSuccess() {
        return error == null;
    }

    /**
     * Checks if this execution failed.
     *
     * @return true if an error occurred
     */
    public boolean isFailure() {
        return error != null;
    }

    /**
     * Gets the result or throws if the execution failed.
     *
     * @return the result
     * @throws RuntimeException if the execution failed
     */
    public R getResultOrThrow() {
        if (error != null) {
            throw new RuntimeException("Model " + modelId + " failed", error);
        }
        return result;
    }

    /**
     * Maps a successful result to a new type.
     * <p>
     * If this result is a failure, returns a new failure with the same error.
     *
     * @param mapper the mapping function
     * @param <U>    the new result type
     * @return a new ModelResult with the mapped value or the original error
     */
    public <U> ModelResult<U> map(final Function<R, U> mapper) {
        if (isSuccess()) {
            return new ModelResult<>(modelId, mapper.apply(result), duration, request, null);
        }
        return new ModelResult<>(modelId, null, duration, request, error);
    }

    /**
     * Creates a successful result.
     *
     * @param modelId  the model ID
     * @param result   the result value
     * @param duration the execution duration
     * @param request  the request that was sent
     * @param <R>      the result type
     * @return a successful ModelResult
     */
    public static <R> ModelResult<R> success(
            final String modelId, final R result, final Duration duration, final String request) {
        return new ModelResult<>(modelId, result, duration, request, null);
    }

    /**
     * Creates a failed result.
     *
     * @param modelId  the model ID
     * @param duration the execution duration
     * @param request  the request that was sent
     * @param error    the error that occurred
     * @param <R>      the result type
     * @return a failed ModelResult
     */
    public static <R> ModelResult<R> failure(
            final String modelId, final Duration duration, final String request, final Throwable error) {
        return new ModelResult<>(modelId, null, duration, request, error);
    }
}
