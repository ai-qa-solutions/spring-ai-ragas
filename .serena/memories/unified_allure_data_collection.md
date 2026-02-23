# Unified Allure Data Collection Pattern

## Overview

As of February 2026, the project uses a **unified pattern** for Allure report data collection across ALL metrics (LLM-based and NLP).

## Architecture

### Single Listener Interface (2 callbacks)

```java
public interface MetricExecutionListener {
    default void beforeMetricEvaluation(MetricEvaluationContext context) {}
    default void afterMetricEvaluation(MetricEvaluationResult result) {}
    default int getOrder() { return 0; }
    default MetricExecutionListener forEvaluation() { return this; }
}
```

### Enriched MetricEvaluationResult (full snapshot)

`MetricEvaluationResult` contains ALL data needed for Allure reports:
- `Sample sample` — input data
- `Object config` — metric configuration
- `List<String> modelIds` — LLM model IDs used
- `List<String> embeddingModelIds` — embedding model IDs used
- `List<StepResults> steps` — step-by-step execution data
- `List<ModelExclusionEvent> exclusions` — model failures
- `MetricMetadata metadata` — **typed metadata record** implementing `MetricMetadata` marker interface (not Object)
- `Double aggregatedScore` — final score
- `Duration totalDuration` — total execution time (may be null for NLP metrics)

### Typed Metadata Records

Each metric creates a specific record (compile-time safe, no stringly-typed keys):

- **Retrieval**: `FaithfulnessMetadata`, `ContextPrecisionMetadata`, `ContextRecallMetadata`, `ContextEntityRecallMetadata`, `NoiseSensitivityMetadata`, `ResponseRelevancyMetadata`
- **General**: `AspectCriticMetadata`, `SimpleCriteriaMetadata`, `RubricsMetadata`
- **Agent**: `AgentGoalAccuracyMetadata`, `ToolCallAccuracyMetadata`, `TopicAdherenceMetadata`
- **Response**: `AnswerCorrectnessMetadata`, `FactualCorrectnessMetadata`, `SemanticSimilarityMetadata`, `HallucinationMetadata`
- **NVIDIA**: `AnswerAccuracyMetadata`, `ContextRelevanceMetadata`, `ResponseGroundednessMetadata`
- **NLP**: `BleuScoreMetadata`, `RougeScoreMetadata`, `ChrfScoreMetadata`, `StringSimilarityMetadata`

All located in `ai.qa.solutions.metric.metadata` package. All implement `MetricMetadata` marker interface from `ai.qa.solutions.execution.listener.dto`.

**NLP metadata deduplication**: NLP metadata records (`BleuScoreMetadata`, `RougeScoreMetadata`, `ChrfScoreMetadata`, `StringSimilarityMetadata`) do NOT contain `response`/`reference` fields — these are sourced from `MetricEvaluationResult.getSample()` instead.

### ScoreExplanationFactory

Replaces the old ~1000-line `ScoreExplanationExtractor` (JSON roundtrip parser).

Located at `spring-ai-ragas-allure/src/main/java/ai/qa/solutions/allure/explanation/ScoreExplanationFactory.java`.

Uses Java 17 `if-else instanceof` chain to dispatch typed metadata to appropriate explanation classes (~100 lines vs ~1000 lines).

**Signature**: `create(MetricEvaluationResult result, String language)` — factory receives the full result (score + metadata + sample), not individual parameters. This allows NLP explanations to get response/reference from `result.getSample()`.

### AllureMetricExecutionListener

Simplified to stateless — all data comes from the enriched `MetricEvaluationResult` in `afterMetricEvaluation()`. No mutable state accumulation during evaluation.

## Deleted Code

- `ScoreExplanationExtractor.java` — replaced by `ScoreExplanationFactory`
- `AllureNlpMetricHelper.java` — NLP metrics now use the listener pattern
- `StepContext.java` — no longer needed (steps accumulated in metrics)

## Class Hierarchy

```
AbstractMetric<T>  (listener management, EvaluationNotifier)
├── AbstractMultiModelMetric<T>  (adds MultiModelExecutor, aggregation)
│   └── AbstractMultiTurnMetric<T>  (adds multi-turn conversation support)
│       ├── AgentGoalAccuracyMetric
│       ├── ToolCallAccuracyMetric
│       └── TopicAdherenceMetric
├── BleuScoreMetric (NLP - extends AbstractMetric directly)
├── RougeScoreMetric
├── ChrfScoreMetric
└── StringSimilarityMetric
```

