# MetricMetadata Interface Pattern

## Overview

As of February 2026, all metric metadata records implement a common `MetricMetadata` marker interface. This enables type-safe handling in listeners, report generators, and the ScoreExplanationFactory.

## Interface Location

**CRITICAL**: `MetricMetadata` lives in `spring-ai-ragas-multi-model` (NOT in `spring-ai-ragas-metrics`) to avoid circular module dependency. The dependency direction is `metrics -> multi-model`.

```
Package: ai.qa.solutions.execution.listener.dto
File: spring-ai-ragas-multi-model/src/main/java/ai/qa/solutions/execution/listener/dto/MetricMetadata.java
```

```java
public interface MetricMetadata {}
```

## 23 Implementing Records

All in `ai.qa.solutions.metric.metadata` package (`spring-ai-ragas-metrics` module):

| Category  |                                                                         Records                                                                         |
|-----------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| Retrieval | FaithfulnessMetadata, ContextPrecisionMetadata, ContextRecallMetadata, ContextEntityRecallMetadata, NoiseSensitivityMetadata, ResponseRelevancyMetadata |
| General   | AspectCriticMetadata, SimpleCriteriaMetadata, RubricsMetadata                                                                                           |
| Agent     | AgentGoalAccuracyMetadata, ToolCallAccuracyMetadata, TopicAdherenceMetadata                                                                             |
| Response  | AnswerCorrectnessMetadata, FactualCorrectnessMetadata, SemanticSimilarityMetadata, HallucinationMetadata                                                |
| NVIDIA    | AnswerAccuracyMetadata, ContextRelevanceMetadata, ResponseGroundednessMetadata                                                                          |
| NLP       | BleuScoreMetadata, RougeScoreMetadata, ChrfScoreMetadata, StringSimilarityMetadata                                                                      |

## Key Design Decisions

### Why NOT sealed

- 23 permits would be too long
- Users of the library can create custom metrics with custom metadata
- `instanceof` pattern matching works equally well with non-sealed interfaces

### NLP Metadata Deduplication

NLP metadata records do NOT contain `response`/`reference` fields. These are sourced from `MetricEvaluationResult.getSample()` in `ScoreExplanationFactory`. This eliminates data duplication between metadata and Sample.

### MetricEvaluationResult.metadata Field

Typed as `MetricMetadata` (not `Object`). Provides compile-time safety.

### MetricEvaluationContext — No metadata Field

The deprecated `Map<String, Object> metadata` field was removed from `MetricEvaluationContext`. All metric-specific data flows through `MetricEvaluationResult.metadata` instead.

### ScoreExplanationFactory Signature

```java
// New signature (receives full result):
public Optional<ScoreExplanation> create(MetricEvaluationResult result, String language)

// Old signature (removed):
// public Optional<ScoreExplanation> create(Double score, Object metadata, String language)
```

The factory receives the full `MetricEvaluationResult` so it can access both metadata AND sample data (needed for NLP explanations that show response/reference).

## Adding a New Metric with Metadata

1. Create `YourMetricMetadata` record in `ai.qa.solutions.metric.metadata`:

   ```java
   public record YourMetricMetadata(/* config-only fields */) implements MetricMetadata {}
   ```
2. Set `.metadata(new YourMetricMetadata(...))` in `MetricEvaluationResult.builder()`
3. Add `instanceof` branch in `ScoreExplanationFactory.createExplanation()`
4. Create corresponding `YourMetricExplanation` class in allure module

