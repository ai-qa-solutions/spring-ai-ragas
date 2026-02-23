# Spring AI RAGAS - Project Overview

## Purpose

Java library for evaluating LLM-based AI agents, inspired by the Python RAGAS framework.
Provides objective, automated quality assessment for AI systems using Spring Boot and Spring AI SDK.

## Tech Stack

- **Java 17+**
- **Spring Boot 3.5.9**
- **Spring AI 1.1.2**
- **Build:** Maven multi-module
- **Testing:** JUnit 5, AssertJ, Mockito
- **Coverage:** JaCoCo (80% min line/branch)
- **Formatting:** Spotless with Palantir Java Format
- **Reporting:** Allure

## Module Structure

```
spring-ai-ragas/
├── spring-ai-ragas-multi-model/     # Core execution engine (MultiModelExecutor, listeners, Sample, messages)
├── spring-ai-ragas-metrics/         # 20+ metrics (extends AbstractMultiModelMetric)
├── spring-ai-ragas-allure/          # Allure reporting (listener, explanations, templates)
├── spring-ai-ragas-spring-boot/     # Spring Boot autoconfiguration
└── spring-ai-ragas-spring-boot-starter/  # Convenience starter POM
```

### Module Dependencies

```
spring-boot-starter → spring-boot → metrics → multi-model
                                  ↘ allure
```

## Key Packages

- `ai.qa.solutions.execution` - MultiModelExecutor, ModelResult, ScoreAggregator
- `ai.qa.solutions.execution.listener` - MetricExecutionListener, DTOs
- `ai.qa.solutions.execution.listener.dto` - MetricEvaluationResult, MetricEvaluationContext, MetricMetadata (marker interface)
- `ai.qa.solutions.metric.metadata` - 23 typed metadata records (all implement MetricMetadata)
- `ai.qa.solutions.metric` - Metric interface, AbstractMultiModelMetric, AbstractMultiTurnMetric
- `ai.qa.solutions.metrics.*` - Concrete metrics (general, retrieval, agent, response, nvidia, nlp)
- `ai.qa.solutions.sample` - Sample, BaseMessage hierarchy
- `ai.qa.solutions.metric.explanation` - Score explanation classes (moved from allure module)
- `ai.qa.solutions.metric.i18n` - Explanation i18n messages (moved from allure module)
- `ai.qa.solutions.allure.*` - Allure reporting (listener, methodology, template, model)
- `ai.qa.solutions.config` - Spring Boot autoconfiguration

