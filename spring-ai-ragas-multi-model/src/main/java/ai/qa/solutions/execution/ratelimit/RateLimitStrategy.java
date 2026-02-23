package ai.qa.solutions.execution.ratelimit;

/**
 * Strategy for handling rate limit exhaustion.
 * <p>
 * Determines what happens when a model's provider bucket has no available tokens.
 *
 * @see RateLimitConfig
 * @see ProviderRateLimiterRegistry
 */
public enum RateLimitStrategy {

    /**
     * Block the calling thread until a token becomes available.
     * <p>
     * If a timeout is configured, the thread will wait up to that duration
     * before failing with {@link RateLimitExceededException}.
     * A timeout of {@link java.time.Duration#ZERO} means infinite wait.
     */
    WAIT,

    /**
     * Fail immediately with {@link RateLimitExceededException} if no token is available.
     */
    REJECT
}
