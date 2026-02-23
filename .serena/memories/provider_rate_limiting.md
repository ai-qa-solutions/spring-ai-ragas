# Provider Rate Limiting

## Overview

Per-provider RPS rate limiting using Bucket4j token bucket algorithm. Implemented in Feb 2026.
Disabled by default — only activated when `rps` is configured.

## Package

`ai.qa.solutions.execution.ratelimit` (in `spring-ai-ragas-multi-model` module)

## Classes

- `ProviderRateLimiterRegistry` — interface: `void acquire(String modelId)`
- `Bucket4jProviderRateLimiterRegistry` — Bucket4j implementation, lazy bucket creation via `ConcurrentHashMap.computeIfAbsent`
- `RateLimitConfig` — Java record: `(int rps, RateLimitStrategy strategy, Duration timeout)`, validates rps > 0
- `RateLimitStrategy` — enum: `WAIT` (block) / `REJECT` (fail-fast)
- `RateLimitExceededException` — RuntimeException with `modelId` and `providerName` fields

## Key Design Decisions

1. **Per-provider scope**: All models from the same provider share one bucket
2. **Duration.ZERO = infinite wait**: Deliberate convention for no-timeout WAIT
3. **Optional dependency**: `bucket4j-core:8.10.1` is `<optional>` in multi-model, regular in spring-boot
4. **Conditional bean**: `@ConditionalOnClass(name = "io.github.bucket4j.Bucket")` guards autoconfiguration
5. **Timing exclusion**: `acquireRateLimit()` called BEFORE `Instant.now()` — wait time excluded from ModelResult duration
6. **Separate try-catch**: Rate limit exceptions caught in their own try block, return `ModelResult.failure(modelId, Duration.ZERO, prompt, e)`
7. **InterruptedException**: Catch → restore interrupt flag → wrap in `RateLimitExceededException` for graceful shutdown
8. **Null-safe**: `MultiModelExecutor` 5-arg constructor accepts nullable registry; null = no rate limiting
9. **Constructor chain**: 5-arg is PRIMARY, 4-arg delegates with null, 3-arg→4-arg→5-arg

## Configuration Properties

```yaml
spring.ai.ragas.providers:
  rate-limit:                    # Global defaults (RateLimitDefaults class)
    default-rps: null            # null = disabled
    default-strategy: WAIT
    default-timeout: 0           # Duration.ZERO = infinite
  openai-compatible:
    - name: provider-name
      rate-limit:                # Per-provider (ProviderRateLimitConfig class)
        rps: 5
        strategy: WAIT
        timeout: 30s
  default-provider:
    rate-limit:
      rps: 10
  external-starters:
    gigachat:
      rate-limit:
        rps: 3
        strategy: REJECT
```

## Integration Points

- `MultiModelExecutor.executeLlmOnModelAsync()` — calls `acquireRateLimit(modelId)` before HTTP call
- `MultiModelExecutor.executeEmbeddingOnModelAsync()` — same pattern
- `MultiModelExecutor.executeEmbeddingsOnModelAsync()` — same pattern
- `MultiProviderAutoConfiguration.providerRateLimiterRegistry()` — builds model-to-provider mapping from all 3 provider layers
- `MultiModelExecutorAutoconfiguration` — injects optional `ProviderRateLimiterRegistry` via `@Autowired(required = false)`

## Test Coverage (18 tests)

- `Bucket4jProviderRateLimiterRegistryTest` — 9 tests: wait strategy (4), reject strategy (2), provider isolation (2), edge cases (1)
- `MultiModelExecutorRateLimitTest` — 5 tests: null registry, acquire ordering, embedding acquire, failure propagation, no NPE
- `RateLimitAutoconfigurationTest` — 4 tests: Spring Boot context with provider config, global defaults, no bean, executor injection

## Bucket Configuration

```java
Bucket.builder()
    .addLimit(limit -> limit
        .capacity(config.rps())
        .refillGreedy(config.rps(), Duration.ofSeconds(1)))
    .build();
```

Token bucket with greedy refill: capacity = RPS, refill = RPS tokens per second.
