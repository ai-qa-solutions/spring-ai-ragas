# Rich Evaluation Result API — IMPLEMENTED

## Status: Completed (Feb 2026, commit 41cd798)

## What Was Done

Added `singleTurnEvaluate()` / `multiTurnEvaluate()` methods to `Metric` interface returning `EvaluationResult` with score + explanation + metadata. Old `singleTurnScore()` / `multiTurnScore()` methods unchanged (backward compatible).

## Key Changes

- **New class:** `ai.qa.solutions.metric.EvaluationResult` — rich return type (score, explanation, metadata, modelScores, excludedModels, totalDuration, sample, config, modelIds, embeddingModelIds)
- **New methods:** `singleTurnEvaluate`, `singleTurnEvaluateAsync`, `multiTurnEvaluate`, `multiTurnEvaluateAsync`
- **Moved 29 classes:** `ai.qa.solutions.allure.explanation.*` → `ai.qa.solutions.metric.explanation.*`
- **Moved i18n:** `ai.qa.solutions.allure.i18n.ExplanationMessages` → `ai.qa.solutions.metric.i18n.ExplanationMessages`
- **Language config:** All 23 config classes got `@Builder.Default private String language = "en"`, `MetricConfiguration.getLanguage()` default method added
- **MetricEvaluationResult:** gained `Object explanation` field

## Implementation Pattern

`AbstractMetric.singleTurnEvaluate()` uses a `ResultCapturingListener` (anonymous `MetricExecutionListener` with `getOrder() = Integer.MIN_VALUE`) to intercept the `MetricEvaluationResult` that the metric builds internally during `singleTurnScore()`. Then calls `ScoreExplanationFactory.create(result, config.getLanguage())` to build the explanation eagerly.
