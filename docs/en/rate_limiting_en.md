# Provider Rate Limiting

Provider rate limiting allows you to control the number of requests per second (RPS) to each LLM API provider.
All models from the same provider share a single rate limiter bucket, ensuring the combined request rate
does not exceed the provider's limit.

Rate limiting uses the [Bucket4j](https://github.com/bucket4j/bucket4j) token bucket algorithm and is
**disabled by default** — it only activates when you configure `rps` for a provider.

## Configuration

### application.yaml

```yaml
spring:
  ai:
    ragas:
      providers:
        # Global rate limit defaults (applied to all providers without explicit config)
        rate-limit:
          default-rps: 10            # requests per second (null = disabled)
          default-strategy: WAIT     # WAIT or REJECT
          default-timeout: 0         # 0 = infinite wait (no timeout)

        openai-compatible:
          - name: openrouter
            base-url: https://openrouter.ai/api
            api-key: ${OPENROUTER_API_KEY}
            # Per-provider rate limit (overrides global defaults)
            rate-limit:
              rps: 5
              strategy: WAIT
              timeout: 30s
            chat-models:
              - { id: anthropic/claude-3.5-sonnet }
              - { id: google/gemini-2.5-flash }
              - { id: openai/gpt-4o-mini }

          - name: groq
            base-url: https://api.groq.com/openai
            api-key: ${GROQ_API_KEY}
            rate-limit:
              rps: 30
              strategy: REJECT
            chat-models:
              - { id: llama3-70b-8192 }

        default-provider:
          enabled: true
          rate-limit:
            rps: 10
            strategy: WAIT
          models:
            - { id: gpt-4o-mini }
```

---

## Strategies

### WAIT

Blocks the calling thread until a token becomes available in the provider's bucket. If a timeout is configured,
the thread will wait at most that long before throwing `RateLimitExceededException`.

| Parameter |     Value     |             Behavior             |
|-----------|---------------|----------------------------------|
| timeout   | `0` (default) | Wait indefinitely until token    |
| timeout   | `30s`         | Wait up to 30 seconds, then fail |
| timeout   | `500ms`       | Wait up to 500ms, then fail      |

### REJECT

Fails immediately with `RateLimitExceededException` if no token is available. The timeout parameter is ignored
for this strategy.

---

## How It Works

1. **Token Bucket Algorithm**: Each provider gets a bucket with capacity equal to the configured RPS.
   Tokens are refilled greedily at RPS tokens per second.

2. **Per-Provider Scope**: All models from the same provider share one bucket. For example,
   if `openrouter` is configured with `rps: 5`, then `claude-3.5-sonnet`, `gemini-2.5-flash`,
   and `gpt-4o-mini` together cannot exceed 5 requests per second.

3. **Graceful Failure**: When a model is rate-limited, it returns `ModelResult.failure()` instead of
   propagating an unhandled exception. Other models continue evaluation normally.

4. **Timing Exclusion**: Rate limit wait time is **not** counted towards the model's response duration.
   The timer starts only after the rate limit token is acquired.

---

## Programmatic Usage

If you use `spring-ai-ragas-multi-model` without the Spring Boot starter, you can configure rate limiting
programmatically:

```java
import ai.qa.solutions.execution.ratelimit.*;
import java.time.Duration;
import java.util.Map;

class Example {
    void example() {
        // Create model-to-provider mapping
        final Map<String, String> modelToProvider = Map.of(
                "claude-3.5-sonnet", "openrouter",
                "gpt-4o-mini", "openrouter",
                "llama3-70b", "groq"
        );

        // Create per-provider rate limit configs
        final Map<String, RateLimitConfig> providerConfigs = Map.of(
                "openrouter", new RateLimitConfig(5, RateLimitStrategy.WAIT, Duration.ZERO),
                "groq", new RateLimitConfig(30, RateLimitStrategy.REJECT, Duration.ZERO)
        );

        // Create registry
        final ProviderRateLimiterRegistry registry =
                new Bucket4jProviderRateLimiterRegistry(modelToProvider, providerConfigs);

        // Pass to MultiModelExecutor
        final MultiModelExecutor executor = new MultiModelExecutor(
                chatClientStore, embeddingModelStore, metricExecutor, httpExecutor, registry);
    }
}
```

> **Note:** When using the library without the starter, add the `bucket4j-core` dependency explicitly:
>
> ```xml
> <dependency>
>     <groupId>com.bucket4j</groupId>
>     <artifactId>bucket4j-core</artifactId>
>     <version>8.10.1</version>
> </dependency>
> ```

---

## Configuration Reference

### Global Defaults

|                        Property                         |       Type        |      Default      |            Description            |
|---------------------------------------------------------|-------------------|-------------------|-----------------------------------|
| `spring.ai.ragas.providers.rate-limit.default-rps`      | `Integer`         | `null` (disabled) | Default RPS for all providers     |
| `spring.ai.ragas.providers.rate-limit.default-strategy` | `WAIT` / `REJECT` | `WAIT`            | Default backpressure strategy     |
| `spring.ai.ragas.providers.rate-limit.default-timeout`  | `Duration`        | `0` (infinite)    | Default timeout for WAIT strategy |

### Per-Provider

|       Property        |       Type        |    Default     |         Description         |
|-----------------------|-------------------|----------------|-----------------------------|
| `rate-limit.rps`      | `Integer`         | global default | RPS limit for this provider |
| `rate-limit.strategy` | `WAIT` / `REJECT` | global default | Backpressure strategy       |
| `rate-limit.timeout`  | `Duration`        | global default | Timeout for WAIT strategy   |

Per-provider properties are available on `openai-compatible[*]`, `default-provider`, and `external-starters.*`.

---

## Error Handling

When rate limiting triggers, the behavior depends on the strategy:

- **WAIT with timeout**: After timeout expires, `RateLimitExceededException` is thrown and the model is
  recorded as failed in `ModelResult.failure()`.
- **REJECT**: Immediately returns `ModelResult.failure()` for the rate-limited model.
- **Thread interruption**: If a thread waiting for a token is interrupted (e.g., during shutdown),
  the interrupt flag is restored and `RateLimitExceededException` is thrown.

In all cases, evaluation continues with the remaining models. The rate-limited model appears in the
`excludedModels` list of the evaluation result.
