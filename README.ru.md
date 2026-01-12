# Spring AI RAGAS - Evaluate LLM агентов на Java

[![en](https://img.shields.io/badge/lang-en-blue.svg)](https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/README.md)
[![ru](https://img.shields.io/badge/lang-ru-blue.svg)](https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/README.ru.md)

Java-библиотека для оценки и тестирования AI агентов на основе больших языковых моделей (LLM), вдохновлённая
Python фреймворком [RAGAS](https://docs.ragas.io/en/stable/concepts/metrics/available_metrics/general_purpose/).
Построена на Spring Boot и Spring AI SDK для лёгкой интеграции с Java экосистемой.

## Зачем нужна эта библиотека?

Современные AI агенты требуют объективной и автоматизированной оценки качества.
Ручное тестирование отнимает время и очень субъективно. Spring AI RAGAS решает эти проблемы:

- **Объективная оценка**: LLM-based метрики для автоматического тестирования
- **Spring-native**: Нативная интеграция с Spring Boot экосистемой
- **Асинхронность**: CompletableFuture для параллельных оценок
- **Мультимодельность**: Оценка на нескольких LLM с агрегацией результатов
- **Мультиязычность**: Поддержка русского и английского языков
- **Расширяемость**: Легко создавать собственные метрики

## Поддерживаемые метрики

### General Purpose Metrics (Общие метрики)

|                                     Метрика                                      |                  Описание                  |
|----------------------------------------------------------------------------------|--------------------------------------------|
| [AspectCritic](docs/ru/general_purpose_metrics_ru.md#aspectcritic)               | Бинарная оценка по заданному критерию      |
| [SimpleCriteriaScore](docs/ru/general_purpose_metrics_ru.md#simplecriteriascore) | Количественная оценка на непрерывной шкале |
| [RubricsScore](docs/ru/general_purpose_metrics_ru.md#rubricsscore)               | Детальная оценка на основе рубрик          |

Полная документация: [Руководство по общим метрикам](docs/ru/general_purpose_metrics_ru.md)

### Метрики извлечения (Retrieval Metrics)

|                                  Метрика                                   |                  Описание                  |
|----------------------------------------------------------------------------|--------------------------------------------|
| [ContextEntityRecall](docs/ru/retrieval_metrics_ru.md#contextentityrecall) | Покрытие сущностей в найденных контекстах  |
| [ContextPrecision](docs/ru/retrieval_metrics_ru.md#contextprecision)       | Точность ранжирования найденных контекстов |
| [ContextRecall](docs/ru/retrieval_metrics_ru.md#contextrecall)             | Полнота найденной информации               |
| [Faithfulness](docs/ru/retrieval_metrics_ru.md#faithfulness)               | Фактическая согласованность с контекстами  |
| [NoiseSensitivity](docs/ru/retrieval_metrics_ru.md#noisesensitivity)       | Устойчивость к нерелевантным контекстам    |
| [ResponseRelevancy](docs/ru/retrieval_metrics_ru.md#responserelevancy)     | Семантическая релевантность ответа         |

Полная документация: [Руководство по метрикам извлечения](docs/ru/retrieval_metrics_ru.md)

## Быстрый старт

### Предварительные требования

- Java 17+
- Spring Boot 3.x
- Доступ к LLM (OpenAI, Azure OpenAI, Anthropic, и др. через Spring AI)

### Установка

#### Maven

```xml

<dependency>
    <groupId>io.github.ai-qa-solutions</groupId>
    <artifactId>spring-ai-ragas-spring-boot-starter</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
        <!-- И любые нужные вам стартеры из экосистемы spring-ai -->
<dependency>
<groupId>org.springframework.ai</groupId>
<artifactId>spring-ai-starter-model-openai</artifactId>
<version>1.1.0-M2</version>
<scope>test</scope>
</dependency>
```

#### Gradle

```groovy
testImplementation 'io.github.ai-qa-solutions:spring-ai-ragas-spring-boot-starter:1.0.0'
testImplementation 'org.springframework.ai:spring-ai-starter-model-openai:1.1.0-M2'
```

#### Allure отчёты (Опционально)

Для генерации HTML-отчётов с объяснениями оценок и временной шкалой выполнения добавьте модуль интеграции с Allure.
Требуется предварительно настроенный AspectJ и Allure в вашем проекте.
Пример полной настройки см. в [pom.xml](pom.xml).

```xml
<dependency>
    <groupId>io.github.ai-qa-solutions</groupId>
    <artifactId>spring-ai-ragas-allure</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
```

Полная документация: [Руководство по интеграции с Allure](spring-ai-ragas-allure/README.md)

### Конфигурация

application.yaml

```yaml
spring:
  ai:
    retry:
      on-http-codes: [ 429 ]
      on-client-errors: true
      backoff:
        initial-interval: 2000ms
        max-interval: 30000ms
        multiplier: 2
    openai:
      base-url: https://openrouter.ai/api
      api-key: ${OPENROUTER_API_KEY}
      chat:
        options:
          model: google/gemini-2.5-flash
          temperature: 0.0
    # Конфигурация мультимодельной оценки
    chat-models:
      default-options:
        temperature: 0.0
        max-tokens: 1000
      list:
        - { id: anthropic/claude-4.5-sonnet }
        - { id: google/gemini-2.5-flash }
        - { id: openai/gpt-4o-mini }
```

## Пример использования

```java

@SpringBootTest
class MetricsTest {

    @Autowired
    private AspectCriticMetric aspectCritic;

    @Autowired
    private SimpleCriteriaScoreMetric simpleCriteria;

    @Autowired
    private RubricsScoreMetric rubrics;

    @Test
    void evaluateResponse() {
        Sample sample = Sample.builder()
                .userInput("Что такое искусственный интеллект?")
                .response("ИИ — это область информатики, которая создаёт системы, "
                        + "способные выполнять задачи, требующие человеческого интеллекта.")
                .build();

        // Бинарная оценка (AspectCritic)
        var aspectConfig = AspectCriticMetric.AspectCriticConfig.builder()
                .definition("Содержит ли ответ точное объяснение ИИ?")
                .build();
        Double aspectScore = aspectCritic.singleTurnScore(aspectConfig, sample);
        // Результат: 1.0 (да) или 0.0 (нет)

        // Оценка на непрерывной шкале (SimpleCriteriaScore)
        var criteriaConfig = SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                .definition("Оцените качество объяснения от 1 до 5")
                .build();
        Double criteriaScore = simpleCriteria.singleTurnScore(criteriaConfig, sample);
        // Результат: 0.0-1.0 (нормализованная оценка)

        // Оценка по рубрикам (RubricsScore)
        var rubricsConfig = RubricsScoreMetric.RubricsConfig.builder()
                .rubric("score1_description", "Нет релевантной информации")
                .rubric("score3_description", "Базовое определение предоставлено")
                .rubric("score5_description", "Полное объяснение с примерами")
                .build();
        Double rubricsScore = rubrics.singleTurnScore(rubricsConfig, sample);
        // Результат: 0.0-1.0 (нормализованная оценка)
    }
}
```

### Фильтрация контента по безопасности

```java
var config = AspectCriticMetric.AspectCriticConfig.builder()
        .definition("Содержит ли ответ вредную информацию?")
        .strictness(5)
        .build();
Double score = aspectCritic.singleTurnScore(config, sample);
// score == 0.0: безопасный контент, score == 1.0: вредный контент
```

### Оценка RAG-системы

```java

@Autowired
private FaithfulnessMetric faithfulness;

@Autowired
private ContextPrecisionMetric contextPrecision;

@Test
void evaluateRAG() {
    Sample sample = Sample.builder()
            .userInput("Когда был первый Суперкубок?")
            .response("Первый Суперкубок состоялся 15 января 1967 года.")
            .retrievedContexts(List.of(
                    "Первый Суперкубок состоялся 15 января 1967 года.",
                    "Игра проходила в Лос-Анджелесском мемориальном колизее."))
            .build();

    Double faithfulnessScore = faithfulness.singleTurnScore(sample);
    // Измеряет, обоснован ли ответ в извлечённых контекстах

    var precisionConfig = ContextPrecisionMetric.ContextPrecisionConfig.builder()
            .evaluationStrategy(ContextPrecisionMetric.EvaluationStrategy.RESPONSE_BASED)
            .build();
    Double precisionScore = contextPrecision.singleTurnScore(precisionConfig, sample);
    // Измеряет качество ранжирования извлечения
}
```

## Архитектура

```
spring-ai-ragas/
├── spring-ai-ragas-metrics/              # Основная библиотека метрик
│   └── ai.qa.solutions/
│       ├── metric/                       # Базовые классы метрик
│       │   ├── Metric                    # Интерфейс метрики
│       │   └── AbstractMultiModelMetric  # Базовый класс для мультимодельных метрик
│       └── metrics/
│           ├── general/                  # Общие метрики
│           │   ├── AspectCriticMetric
│           │   ├── SimpleCriteriaScoreMetric
│           │   └── RubricsScoreMetric
│           └── retrieval/                # Метрики для RAG
│               ├── ContextEntityRecallMetric
│               ├── ContextPrecisionMetric
│               ├── ContextRecallMetric
│               ├── FaithfulnessMetric
│               ├── NoiseSensitivityMetric
│               └── ResponseRelevancyMetric
│
├── spring-ai-ragas-multi-model/          # Мультимодельное выполнение
│   └── ai.qa.solutions/
│       ├── chatclient/                   # Фабрика ChatClient
│       ├── embedding/                    # Фабрика EmbeddingModel
│       ├── execution/                    # Движок мультимодельного выполнения
│       │   ├── MultiModelExecutor        # Параллельное выполнение на моделях
│       │   └── ScoreAggregator           # Стратегии агрегации оценок
│       └── sample/                       # DTO классы Sample
│
├── spring-ai-ragas-spring-boot/          # Spring Boot автоконфигурация
│   └── config/                           # Классы автоконфигурации
│
└── spring-ai-ragas-spring-boot-starter/  # Spring Boot стартер
```

## Лицензия

Этот проект лицензирован под лицензией MIT License — см. [LICENSE](LICENSE) для подробностей.

## Благодарности

- [RAGAS](https://github.com/explodinggradients/ragas) — Оригинальный Python фреймворк
- [Spring AI](https://spring.io/projects/spring-ai) — Основа для интеграции LLM

