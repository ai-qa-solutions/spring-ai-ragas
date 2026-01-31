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
├── spring-ai-ragas-metrics/        # Core metrics library (20+ metrics)
├── spring-ai-ragas-multi-model/    # Multi-model execution engine
├── spring-ai-ragas-allure/         # Allure reporting integration
├── spring-ai-ragas-spring-boot/    # Spring Boot autoconfiguration
├── spring-ai-ragas-spring-boot-starter/  # Convenience starter POM
└── docs/                           # Documentation (en/, ru/)
```

### Module Dependencies

```
spring-boot-starter → spring-boot → metrics → multi-model
                                  ↘ allure
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

### Dual Executor Architecture

To prevent deadlocks during parallel execution, the system uses two separate thread pools:

- `ragasMetricExecutor` (core=4, max=32) - for metric-level async operations (`runAsync()`)
- `ragasHttpExecutor` (core=8, max=64) - for HTTP/LLM API calls

**Important:** Metrics must use `executor.runAsync()` instead of `CompletableFuture.supplyAsync()`.

### NLP Metric Pattern

NLP metrics (BLEU, ROUGE, chrF, StringSimilarity) do NOT use MultiModelExecutor:

```java
public class BleuScoreMetric implements Metric<BleuScoreConfig> {
    @Override
    public Double singleTurnScore(BleuScoreConfig config, Sample sample) {
        // Direct computation, no LLM calls
        return computeBleuScore(sample.getResponse(), sample.getReference(), config);
    }
}
```

For Allure reporting, use `AllureNlpMetricHelper`:

```java
AllureNlpMetricHelper.attachBleuScore(score, response, reference, maxNgram, smoothing, "en");
```

## Available Metrics

### General Purpose (LLM-based)

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

### Agent Metrics

- `AgentGoalAccuracyMetric` - Whether agent achieved its goal
- `ToolCallAccuracyMetric` - Correctness of tool/function calls
- `TopicAdherenceMetric` - Staying on topic during conversation

### Multi-turn Message Types

Agent metrics use a sealed interface hierarchy for type-safe multi-turn conversations:

```java
public sealed interface BaseMessage permits HumanMessage, AIMessage, ToolMessage {
    String content();
}

// Usage with pattern matching
String formatted = switch (message) {
    case HumanMessage h -> "[USER]: " + h.content();
    case AIMessage a -> "[ASSISTANT]: " + a.content() + formatToolCalls(a.toolCalls());
    case ToolMessage t -> "[TOOL]: " + t.content();
};
```

**Message types:**
- `HumanMessage(String content)` - User message
- `AIMessage(String content, List<ToolCall> toolCalls)` - Assistant response with optional tool calls
- `ToolMessage(String content)` - Tool execution result
- `ToolCall(String name, Map<String, Object> arguments)` - Tool invocation

**Multi-turn API:**

```java
Sample sample = Sample.builder()
    .userInputMessages(List.of(
        new HumanMessage("Book a flight"),
        new AIMessage("Searching...", List.of(new ToolCall("search", Map.of()))),
        new ToolMessage("Found 5 flights"),
        new AIMessage("I found 5 options.")
    ))
    .build();

// Use multiTurnScore for agent metrics
Double score = metric.multiTurnScore(config, sample);
```

**Message types:** `HumanMessage`, `AIMessage`, `ToolMessage` from `ai.qa.solutions.sample.message` package

### Response Metrics

- `AnswerCorrectnessMetric` - Overall answer correctness
- `FactualCorrectnessMetric` - Factual accuracy of statements
- `SemanticSimilarityMetric` - Embedding-based similarity (requires EmbeddingModel)

### NVIDIA Metrics

- `AnswerAccuracyMetric` - NVIDIA-style answer accuracy
- `ContextRelevanceMetric` - Context relevance scoring
- `ResponseGroundednessMetric` - Response grounding in context

### NLP Metrics (Non-LLM)

These metrics do NOT require LLM calls - they compute text similarity directly:

- `BleuScoreMetric` - BLEU score for translation quality
- `RougeScoreMetric` - ROUGE score (ROUGE-1, ROUGE-2, ROUGE-L)
- `ChrfScoreMetric` - Character n-gram F-score (chrF/chrF++)
- `StringSimilarityMetric` - Edit distance metrics (Levenshtein, Jaro, Jaro-Winkler)

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

## Allure Reporting Module

The `spring-ai-ragas-allure` module provides rich HTML reports for metric evaluations.

### Structure

```
spring-ai-ragas-allure/
├── explanation/     # Score explanation classes per metric
├── i18n/            # Internationalization (en/ru)
├── listener/        # AllureMetricExecutionListener
├── methodology/     # Markdown methodology files
├── model/           # Report data models
├── nlp/             # AllureNlpMetricHelper for NLP metrics
└── template/        # Freemarker HTML templates
```

### Report Sections

Each Allure attachment includes:
1. **Summary** - Input data (response, reference, contexts)
2. **Score Explanation** - Step-by-step calculation with formula
3. **Scale** - Visual interpretation (Excellent/Good/Moderate/Poor)
4. **Methodology** - Detailed metric documentation
5. **Execution Log** - Debug information

### Usage in Tests

For LLM metrics, use `AllureMetricExecutionListener` (auto-registered).

For NLP metrics, manually attach reports:

```java
@Test
void testBleuScore() {
    Double score = bleuScoreMetric.singleTurnScore(config, sample);

    AllureNlpMetricHelper.attachBleuScore(
        score, sample.getResponse(), sample.getReference(),
        config.getMaxNgram(), config.isSmoothing(), "en");
}
```

### Adding New Metric Reports

1. Create `YourMetricExplanation` class in `explanation/`
2. Add methodology markdown in `methodology/en/` and `methodology/ru/`
3. Add i18n messages in `ExplanationMessages.java`
4. Update `ScoreExplanationExtractor` to handle new metric type

## Post-Change Validation (MANDATORY)

**After ANY changes to metrics or Allure reporting, you MUST validate the Allure reports.**

Allure reports are what users see after running evaluations. Changes to metrics can break report generation or display incorrect data.

### Required Validation Steps

After modifying metric code:

1. **Run unit tests** - verify metric logic works
2. **Run integration test** - verify with real LLM
3. **Validate Allure report** - verify user sees correct data

### Using /validate-allure-report Skill

```bash
# After fixing a metric, validate its Allure report:
/validate-allure-report EnToolCallAccuracyIntegrationIT
/validate-allure-report RuAgentGoalAccuracyIntegrationIT
/validate-allure-report *FaithfulnessIntegrationIT
```

The skill will:
1. Execute the integration test
2. Find generated Allure attachments
3. Validate report structure (4 questions per step)
4. Check CSS colors, conversation blocks, model results

### What to Validate (4 Questions Pattern)

Each step in Score Explanation must answer:

| #  |        Question        |      What User Sees      |
|----|------------------------|--------------------------|
| Q1 | What is being checked? | Actual text/conversation |
| Q2 | Why / what criteria?   | Step purpose             |
| Q3 | By which models?       | List of model IDs        |
| Q4 | What did they answer?  | Each model's response    |

### When Validation is Required

- After fixing metric bugs (like ToolCallAccuracy typed messages)
- After adding new metrics
- After modifying Allure templates
- After changes to ScoreExplanationExtractor
- After changes to explanation classes

