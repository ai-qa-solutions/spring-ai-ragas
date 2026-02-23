package ai.qa.solutions.execution.ratelimit;

/**
 * Thrown when a rate limit is exceeded for a provider.
 * <p>
 * This exception is thrown in two scenarios:
 * <ul>
 *   <li>When using {@link RateLimitStrategy#REJECT} and no token is available</li>
 *   <li>When using {@link RateLimitStrategy#WAIT} and the configured timeout expires
 *       or the waiting thread is interrupted</li>
 * </ul>
 *
 * @see ProviderRateLimiterRegistry
 * @see RateLimitStrategy
 */
public class RateLimitExceededException extends RuntimeException {

    private final String modelId;
    private final String providerName;

    /**
     * Creates a new rate limit exceeded exception.
     *
     * @param modelId      the model ID that was rate-limited
     * @param providerName the provider whose rate limit was exceeded
     * @param message      the detail message
     */
    public RateLimitExceededException(final String modelId, final String providerName, final String message) {
        super(message);
        this.modelId = modelId;
        this.providerName = providerName;
    }

    /**
     * Creates a new rate limit exceeded exception with a cause.
     *
     * @param modelId      the model ID that was rate-limited
     * @param providerName the provider whose rate limit was exceeded
     * @param message      the detail message
     * @param cause        the underlying cause
     */
    public RateLimitExceededException(
            final String modelId, final String providerName, final String message, final Throwable cause) {
        super(message, cause);
        this.modelId = modelId;
        this.providerName = providerName;
    }

    /**
     * Gets the model ID that was rate-limited.
     *
     * @return the model ID
     */
    public String getModelId() {
        return modelId;
    }

    /**
     * Gets the provider name whose rate limit was exceeded.
     *
     * @return the provider name
     */
    public String getProviderName() {
        return providerName;
    }
}
