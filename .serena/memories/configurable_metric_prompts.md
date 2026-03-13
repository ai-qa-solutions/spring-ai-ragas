# Configurable Metric Prompts — Plan Summary

## Status

Planned (specs/configurable-metric-prompts.md). Not yet implemented.

## Scope (MVP)

3 General metrics only: AspectCriticMetric, SimpleCriteriaScoreMetric, RubricsScoreMetric.

## Architecture — 3-Level Prompt Resolution Chain

1. `config.promptTemplate` (per-evaluation override, highest priority)
2. Spring property `spring.ai.ragas.metrics.prompts.aspect-critic` (global override)
3. Classpath resource `ai/qa/solutions/prompts/{lang}/{metric-key}.txt` (bundled i18n)
4. Fallback: `DEFAULT_PROMPT_TEMPLATE` constant (hardcoded English)

## Key Decisions

- Config classes get nullable `promptTemplate` field (backward compatible)
- `PromptTemplateResolver` utility in `ai.qa.solutions.metric.prompt` package
- Resource path follows allure methodology convention: `ai/qa/solutions/prompts/{lang}/`
- Metric keys: `aspect-critic`, `simple-criteria-score`, `rubrics-score`
- Spring properties support `classpath:` prefix for external file references
- Russian prompts: JSON field names stay English, only instruction text translated
- Each metric's `renderPrompt()` calls `resolveTemplate(config)` which implements the chain

## Files

- New: `PromptTemplateResolver.java` in metrics module
- New: 6 resource files (3 en + 3 ru prompt templates)
- Modified: 3 metric classes (Config + renderPrompt), RagasMetricsProperties, RagasMetricsAutoconfiguration

## Future Extension

Same pattern applies to all remaining metrics (Retrieval: 6 metrics ~10 prompts, Agent: 3 metrics ~7 prompts, NVIDIA: 3 metrics ~5 prompts).
