package ai.qa.solutions.execution.ratelimit;

import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for per-provider rate limiting.
 * <p>
 * Each provider can have its own rate limit configuration specifying the maximum
 * requests per second (RPS), the backpressure strategy, and an optional timeout.
 *
 * <h3>Timeout semantics:</h3>
 * <ul>
 *   <li>{@link Duration#ZERO} means infinite wait (no timeout) when using {@link RateLimitStrategy#WAIT}</li>
 *   <li>A positive duration means the thread will wait at most that long before failing</li>
 *   <li>Timeout is ignored when using {@link RateLimitStrategy#REJECT}</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Default config: WAIT strategy with infinite timeout
 * RateLimitConfig config = RateLimitConfig.withDefaults(10);
 *
 * // Custom config: REJECT strategy
 * RateLimitConfig custom = new RateLimitConfig(5, RateLimitStrategy.REJECT, Duration.ZERO);
 *
 * // WAIT with 30-second timeout
 * RateLimitConfig withTimeout = new RateLimitConfig(10, RateLimitStrategy.WAIT, Duration.ofSeconds(30));
 * }</pre>
 *
 * @param rps      maximum requests per second (must be positive)
 * @param strategy the backpressure strategy to apply when bucket is exhausted
 * @param timeout  maximum wait duration for WAIT strategy; {@link Duration#ZERO} means infinite
 */
public record RateLimitConfig(int rps, RateLimitStrategy strategy, Duration timeout) {

    /**
     * Compact constructor with validation.
     */
    public RateLimitConfig {
        if (rps <= 0) {
            throw new IllegalArgumentException("rps must be positive, got: " + rps);
        }
        Objects.requireNonNull(strategy, "strategy must not be null");
        Objects.requireNonNull(timeout, "timeout must not be null");
    }

    /**
     * Creates a rate limit config with default settings: WAIT strategy and infinite timeout.
     *
     * @param rps maximum requests per second (must be positive)
     * @return config with WAIT strategy and {@link Duration#ZERO} (infinite) timeout
     */
    public static RateLimitConfig withDefaults(final int rps) {
        return new RateLimitConfig(rps, RateLimitStrategy.WAIT, Duration.ZERO);
    }
}
