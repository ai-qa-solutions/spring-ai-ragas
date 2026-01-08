# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/claude-code) when working with code in this repository.

## Project Overview

Spring AI RAGAS is a Java library for evaluating LLM-based AI agents, inspired by the Python RAGAS framework. It provides objective, automated quality assessment for AI systems using Spring Boot and Spring AI SDK.

**Key features:**
- Multi-model evaluation with configurable aggregation (AVERAGE, MEDIAN, MIN, MAX, MAJORITY_VOTING)
- Asynchronous parallel execution via CompletableFuture
- Observer pattern for evaluation monitoring
- Multilingual support (English and Russian)

## Project Structure

```
spring-ai-ragas/
├── spring-ai-ragas-metrics/        # Core metrics library (9 metrics)
├── spring-ai-ragas-multi-model/    # Multi-model execution engine
├── spring-ai-ragas-spring-boot/    # Spring Boot autoconfiguration
├── spring-ai-ragas-spring-boot-starter/  # Convenience starter POM
└── docs/                           # Documentation (en/, ru/)
```

### Module Dependencies
```
spring-boot-starter → spring-boot → metrics → multi-model
```

## Build Commands

```bash
# Build project
mvn clean install

# Run unit tests only
mvn test

# Run integration tests (requires API keys)
mvn test -P integration-tests

# Verify code coverage (80% minimum)
mvn verify -P coverage

# Format code (Palantir Java Format)
mvn spotless:apply
```

## Key Technologies

- **Java 17+**
- **Spring Boot 3.5.9**
- **Spring AI 1.1.2**
- **Testing:** JUnit 5, AssertJ, Mockito
- **Coverage:** JaCoCo (80% line/branch minimum)
- **Formatting:** Spotless with Palantir Java Format

## Architecture Patterns

### Metric Implementation Pattern

All metrics extend `AbstractMultiModelMetric<Config>` and implement:

```java
@Override
protected CompletableFuture<Double> singleTurnScoreAsync(
        Config config,
        Sample sample,
        EvaluationNotifier notifier) {
    // 1. Call notifier.beforeStep() before LLM execution
    // 2. Execute LLM calls via executor.executeLlm()
    // 3. Call notifier.afterLlmStep() with results
    // 4. Track excluded models via notifier.onModelExcluded()
    // 5. Aggregate scores and return
}
```

### Score Aggregation

Scores from multiple models are combined using `ScoreAggregator`:
- `ScoreAggregator.AVERAGE` - arithmetic mean
- `ScoreAggregator.MEDIAN` - middle value
- `ScoreAggregator.consensus(tolerance)` - requires model agreement

### Listener Pattern

`MetricExecutionListener` provides hooks for evaluation monitoring:
- `beforeMetricEvaluation()` / `afterMetricEvaluation()`
- `beforeStep()` / `afterStep()` / `afterLlmStep()`
- `onModelExcluded()` - when a model fails

**Thread safety:** Listeners must implement `forEvaluation()` to return evaluation-specific instances.

## Available Metrics

### General Purpose
- `AspectCriticMetric` - Binary evaluation (pass/fail)
- `SimpleCriteriaScoreMetric` - Continuous scale (0-1)
- `RubricsScoreMetric` - Detailed rubric-based evaluation

### Retrieval (RAG)
- `FaithfulnessMetric` - Factual consistency with context
- `ContextPrecisionMetric` - Retrieval ranking quality
- `ContextRecallMetric` - Completeness of retrieved info
- `ContextEntityRecallMetric` - Entity coverage
- `NoiseSensitivityMetric` - Robustness to irrelevant contexts
- `ResponseRelevancyMetric` - Semantic relevance of response

## Code Style

### General Rules
- Use `final` for all variables, parameters, and fields (enforced by convention)
- Use Lombok `@Builder` for configuration classes
- Use Java records for DTOs
- All metric configs should support `toBuilder()` for copying
- Keep metrics stateless - inject executor via constructor
- Return `CompletableFuture<Double>` for async methods

### No-Nest Code Style
Avoid deep nesting. Use early returns and guard clauses:

```java
// BAD - nested code
public void process(final Request request) {
    if (request != null) {
        if (request.isValid()) {
            // actual logic deeply nested
        }
    }
}

// GOOD - flat code with early returns
public void process(final Request request) {
    if (request == null) {
        return;
    }
    if (!request.isValid()) {
        return;
    }
    // actual logic at top level
}
```

### Code Formatting (Spotless)

The project uses Spotless with Palantir Java Format. Formatting is enforced on build.

```bash
# Check formatting
mvn spotless:check

# Auto-fix formatting
mvn spotless:apply
```

Spotless runs automatically during `compile` phase. CI will fail if code is not formatted.

## Testing and Coverage

### Unit Tests
- Use `StubMultiModelExecutor` for isolation (no network calls)
- Located in `src/test/java` with `*Test.java` naming
- Use `@Nested` classes for logical grouping
- Use `@DisplayName` for descriptive test names

### Integration Tests
- Use `*IT.java` naming convention
- Organized by language: `en/` and `ru/` subdirectories
- Require actual LLM API credentials

### Code Coverage (JaCoCo)

Coverage is enforced via JaCoCo with **80% minimum** for both lines and branches.

```bash
# Run tests with coverage verification
mvn verify -P coverage

# Coverage reports generated at:
# target/site/jacoco/index.html
```

JaCoCo configuration excludes Mockito-generated classes. Coverage check fails the build if thresholds are not met.

### Example: Testing a Metric
```java
@Test
void shouldCalculateScore() {
    final var executor = new StubMultiModelExecutor()
        .withResponseProvider(YourResponseType.class, () -> mockResponse);

    final var metric = new YourMetric(executor, config);
    final var result = metric.singleTurnScore(config, sample);

    assertThat(result).isBetween(0.0, 1.0);
}
```

## Common Patterns

### Adding a New Metric

1. Create config class with `@Builder` in metrics module
2. Extend `AbstractMultiModelMetric<YourConfig>`
3. Implement `singleTurnScoreAsync()` with proper notifier calls
4. Add Spring bean in `RagasMetricsAutoconfiguration`
5. Write unit tests using `StubMultiModelExecutor`
6. Add integration tests for both `en/` and `ru/`

### Sample Data Object
```java
Sample sample = Sample.builder()
    .userInput("User question")
    .response("AI response")
    .retrievedContexts(List.of("context1", "context2"))
    .reference("Ground truth answer")
    .build();
```
