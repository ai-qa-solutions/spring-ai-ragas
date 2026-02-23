package ai.qa.solutions.execution.ratelimit;

/**
 * Registry that manages per-provider rate limiters for LLM and embedding API calls.
 * <p>
 * Each provider (e.g., OpenAI, OpenRouter, GigaChat) has its own rate limiter.
 * All models belonging to the same provider share a single rate limiter bucket,
 * ensuring the combined request rate does not exceed the provider's limit.
 * <p>
 * Models that are not registered in the registry are not rate-limited.
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Acquire a token before making an API call
 * registry.acquire("gpt-4o");
 * chatClient.prompt(prompt).call();
 * }</pre>
 *
 * @see Bucket4jProviderRateLimiterRegistry
 * @see RateLimitConfig
 */
public interface ProviderRateLimiterRegistry {

    /**
     * Acquires a rate limit token for the given model.
     * <p>
     * If the model's provider has an available token, it is consumed immediately.
     * If no token is available, the behavior depends on the provider's
     * {@link RateLimitStrategy}:
     * <ul>
     *   <li>{@link RateLimitStrategy#WAIT} - blocks until a token becomes available
     *       (or until timeout, if configured)</li>
     *   <li>{@link RateLimitStrategy#REJECT} - throws immediately</li>
     * </ul>
     * <p>
     * If the model is not registered in this registry, this method returns immediately
     * without any rate limiting.
     *
     * @param modelId the model ID to acquire a token for
     * @throws RateLimitExceededException if the rate limit is exceeded and cannot be satisfied
     */
    void acquire(String modelId) throws RateLimitExceededException;
}
