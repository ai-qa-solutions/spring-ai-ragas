# Code Style and Conventions

## General Rules

- Use `final` for ALL variables, parameters, and fields
- Use Lombok `@Builder` for configuration classes (with `toBuilder()`)
- Use Java records for DTOs
- Keep metrics stateless - inject executor via constructor
- Return `CompletableFuture<Double>` for async methods
- Spotless with Palantir Java Format enforced on build

## No-Nest Style

Avoid deep nesting. Use early returns and guard clauses:

```java
// GOOD
if (request == null) { return; }
if (!request.isValid()) { return; }
// actual logic at top level
```

## Testing Conventions

- Unit tests: `*Test.java` naming, use `StubMultiModelExecutor`
- Integration tests: `*IT.java`, organized by language (`en/`, `ru/`)
- Use `@Nested` classes for grouping, `@DisplayName` for descriptions

## Metric Implementation Pattern

Class hierarchy:
- `AbstractMetric<T>` — base class with listener management, EvaluationNotifier
- `AbstractMultiModelMetric<T>` extends `AbstractMetric<T>` — adds MultiModelExecutor
- `AbstractMultiTurnMetric<T>` extends `AbstractMultiModelMetric<T>` — for agent metrics

LLM metrics extend `AbstractMultiModelMetric<Config>` and implement `singleTurnScoreAsync()`.
NLP metrics extend `AbstractMetric<Config>` directly (no LLM calls, no executor).
Agent metrics extend `AbstractMultiTurnMetric<Config>` and implement `multiTurnScoreAsync()`.

## Listener Pattern (Unified)

`MetricExecutionListener` has exactly 4 methods:
- `beforeMetricEvaluation(MetricEvaluationContext)` — called before evaluation
- `afterMetricEvaluation(MetricEvaluationResult)` — called after (full snapshot)
- `getOrder()` — listener priority
- `forEvaluation()` — creates evaluation-specific instance for thread safety

All data for Allure reports comes from the enriched `MetricEvaluationResult` (sample, config, steps, exclusions, typed metadata record).

## Typed Metadata Records

Each metric creates a specific Java record for Allure data (in `ai.qa.solutions.metric.metadata`).
Never use `Map<String, Object>` — use compile-time safe typed records.
`ScoreExplanationFactory` dispatches via instanceof to create explanations.

## Dual Executor Architecture

- `ragasMetricExecutor` (core=4, max=32) - metric-level async
- `ragasHttpExecutor` (core=8, max=64) - HTTP/LLM API calls
- Metrics MUST use `executor.runAsync()` instead of `CompletableFuture.supplyAsync()`

