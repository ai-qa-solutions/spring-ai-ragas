package ai.qa.solutions.execution.ratelimit;

import io.github.bucket4j.Bucket;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bucket4j-based implementation of {@link ProviderRateLimiterRegistry}.
 * <p>
 * Uses the token-bucket algorithm to enforce per-provider RPS limits. Each provider
 * gets a separate {@link Bucket} instance, and all models belonging to the same provider
 * share that bucket.
 * <p>
 * Buckets are created lazily on first access and stored in a thread-safe
 * {@link ConcurrentHashMap}.
 *
 * <h3>Important for non-starter users:</h3>
 * If you use {@code spring-ai-ragas-multi-model} directly (without the Spring Boot starter),
 * you must add the {@code bucket4j-core} dependency explicitly:
 * <pre>{@code
 * <dependency>
 *     <groupId>com.bucket4j</groupId>
 *     <artifactId>bucket4j-core</artifactId>
 *     <version>8.10.1</version>
 * </dependency>
 * }</pre>
 *
 * @author Artem Simeshin
 * @see ProviderRateLimiterRegistry
 * @see RateLimitConfig
 */
public class Bucket4jProviderRateLimiterRegistry implements ProviderRateLimiterRegistry {

    private final Map<String, String> modelToProvider;
    private final Map<String, RateLimitConfig> providerConfigs;
    private final ConcurrentHashMap<String, Bucket> providerBuckets;

    /**
     * Creates a new registry with the given model-to-provider mapping and provider configurations.
     *
     * @param modelToProvider maps model ID to provider name (e.g., "gpt-4o" to "openai")
     * @param providerConfigs maps provider name to rate limit configuration
     */
    public Bucket4jProviderRateLimiterRegistry(
            final Map<String, String> modelToProvider, final Map<String, RateLimitConfig> providerConfigs) {
        this.modelToProvider = Objects.requireNonNull(modelToProvider, "modelToProvider must not be null");
        this.providerConfigs = Objects.requireNonNull(providerConfigs, "providerConfigs must not be null");
        this.providerBuckets = new ConcurrentHashMap<>();
    }

    @Override
    public void acquire(final String modelId) throws RateLimitExceededException {
        final String providerName = modelToProvider.get(modelId);
        if (providerName == null) {
            return;
        }

        final RateLimitConfig config = providerConfigs.get(providerName);
        if (config == null) {
            return;
        }

        final Bucket bucket = providerBuckets.computeIfAbsent(providerName, k -> createBucket(config));

        switch (config.strategy()) {
            case WAIT -> acquireWithWait(bucket, config, modelId, providerName);
            case REJECT -> acquireWithReject(bucket, modelId, providerName);
        }
    }

    private void acquireWithWait(
            final Bucket bucket, final RateLimitConfig config, final String modelId, final String providerName) {
        try {
            if (config.timeout().isZero()) {
                bucket.asBlocking().consume(1);
            } else {
                final boolean consumed = bucket.asBlocking().tryConsume(1, config.timeout());
                if (!consumed) {
                    throw new RateLimitExceededException(
                            modelId, providerName, "Rate limit wait timeout exceeded for provider: " + providerName);
                }
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RateLimitExceededException(modelId, providerName, "Rate limit acquisition interrupted", e);
        }
    }

    private void acquireWithReject(final Bucket bucket, final String modelId, final String providerName) {
        final boolean consumed = bucket.tryConsume(1);
        if (!consumed) {
            throw new RateLimitExceededException(
                    modelId, providerName, "Rate limit exceeded for provider: " + providerName);
        }
    }

    private Bucket createBucket(final RateLimitConfig config) {
        return Bucket.builder()
                .addLimit(limit -> limit.capacity(config.rps()).refillGreedy(config.rps(), Duration.ofSeconds(1)))
                .build();
    }
}
