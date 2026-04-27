# Spring AI RAGAS — Project Overview

Java port of Python RAGAS framework. LLM agent evaluation library on Spring Boot + Spring AI.

## Coordinates

- Group: `io.github.ai-qa-solutions`
- Artifact: `spring-ai-ragas` (parent pom, packaging=pom)
- Version: `0.3.3`
- Java: 17
- Spring Boot: 3.5.13
- Spring AI: 1.1.2
- License: MIT
- Maven Central: published via central-publishing-maven-plugin
- Repo: github.com/ai-qa-solutions/spring-ai-ragas

## Multi-module layout

- `spring-ai-ragas-metrics` — pure metric implementations (no Spring Boot deps). Packages: `ai.qa.solutions.metric` (core: EvaluationResult, explanations) + `ai.qa.solutions.metrics` (impls per category)
- `spring-ai-ragas-spring-boot` — autoconfig + properties (`config/`, `properties/`). Wires beans via `RagasMetricsAutoconfiguration`, `MultiProviderAutoConfiguration`, `MultiModelExecutorAutoconfiguration`
- `spring-ai-ragas-spring-boot-starter` — starter aggregator that transitively pulls in `spring-ai-ragas-spring-boot` (and via it: metrics, multi-model, bucket4j, autoconfigure)
- `spring-ai-ragas-multi-model` — multi-LLM execution. Packages: `chatclient/`, `embedding/`, `execution/`, `sample/`. Aggregation strategies, per-provider rate limiting (Bucket4j WAIT/REJECT)
- `spring-ai-ragas-allure` — Allure reporting integration (optional). Has `example/` subdir

## Metric categories (in spring-ai-ragas-metrics)

- `general/` — AspectCritic, SimpleCriteriaScore, RubricsScore
- `retrieval/` — ContextEntityRecall, ContextPrecision, ContextRecall, Faithfulness, NoiseSensitivity, ResponseRelevancy
- `agent/` — AgentGoalAccuracy, ToolCallAccuracy, TopicAdherence
- `response/` — AnswerCorrectness, FactualCorrectness, SemanticSimilarity, Hallucination + TextChunker helper
- `nvidia/` — AnswerAccuracy, ContextRelevance, ResponseGroundedness
- `nlp/` — BleuScore, RougeScore, ChrfScore, StringSimilarity (non-LLM, direct text similarity)

## Pipelines

1. **With references** — POC tests, synthetic monitoring (~8 LLM calls)
2. **Without references** — sampling/analyzing live prod traffic

## Recent work (git)

- branch: `main`
- Recent: release 0.3.3 — Allure listener builder API with step/attachment/section toggles (#14, closes #13); earlier 0.3.2: starter POM fix (#9, closes #7), README GigaChat best-practices rewrite (#8), Spring Boot bump 3.5.11 → 3.5.13 (#10), chunked embedding (#5), PMD, ThreadLocal Allure fix (#3)

