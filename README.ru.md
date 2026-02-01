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

## Сценарии использования

Комплексный анализ качества агентских систем в двух сценариях:

- **С референсами**: Оценка POC, автотесты, синтетический мониторинг
- **Без референсов**: Семплирование и анализ живого продакшен-трафика

## Совместимость версий

| Spring AI RAGAS | RAGAS (Python) | Spring Boot | Spring AI |
|-----------------|----------------|-------------|-----------|
| 0.3.0           | 0.3.x          | 3.5.x       | 1.1.x     |

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

### Метрики агентов (Agent Metrics)

|                              Метрика                               |                 Описание                  |
|--------------------------------------------------------------------|-------------------------------------------|
| [AgentGoalAccuracy](docs/ru/agent_metrics_ru.md#agentgoalaccuracy) | Достиг ли агент поставленной цели         |
| [ToolCallAccuracy](docs/ru/agent_metrics_ru.md#toolcallaccuracy)   | Корректность вызовов инструментов/функций |
| [TopicAdherence](docs/ru/agent_metrics_ru.md#topicadherence)       | Следование теме в ходе разговора          |

Полная документация: [Руководство по метрикам агентов](docs/ru/agent_metrics_ru.md)

### Метрики ответов (Response Metrics)

|                                 Метрика                                 |                    Описание                     |
|-------------------------------------------------------------------------|-------------------------------------------------|
| [AnswerCorrectness](docs/ru/response_metrics_ru.md#answercorrectness)   | Общая корректность ответа                       |
| [FactualCorrectness](docs/ru/response_metrics_ru.md#factualcorrectness) | Фактическая точность утверждений                |
| [SemanticSimilarity](docs/ru/response_metrics_ru.md#semanticsimilarity) | Семантическое сходство (требует EmbeddingModel) |

Полная документация: [Руководство по метрикам ответов](docs/ru/response_metrics_ru.md)

### Метрики NVIDIA (NVIDIA Metrics)

|                                  Метрика                                  |             Описание              |
|---------------------------------------------------------------------------|-----------------------------------|
| [AnswerAccuracy](docs/ru/nvidia_metrics_ru.md#answeraccuracy)             | Точность ответа в стиле NVIDIA    |
| [ContextRelevance](docs/ru/nvidia_metrics_ru.md#contextrelevance)         | Оценка релевантности контекста    |
| [ResponseGroundedness](docs/ru/nvidia_metrics_ru.md#responsegroundedness) | Обоснованность ответа в контексте |

Полная документация: [Руководство по метрикам NVIDIA](docs/ru/nvidia_metrics_ru.md)

### NLP метрики (без LLM)

Эти метрики вычисляют сходство текстов напрямую, без вызовов LLM:

|                            Метрика                             |                       Описание                       |
|----------------------------------------------------------------|------------------------------------------------------|
| [BleuScore](docs/ru/nlp_metrics_ru.md#bleuscore)               | BLEU-оценка качества перевода                        |
| [RougeScore](docs/ru/nlp_metrics_ru.md#rougescore)             | ROUGE-оценка (ROUGE-1, ROUGE-2, ROUGE-L)             |
| [ChrfScore](docs/ru/nlp_metrics_ru.md#chrfscore)               | Символьная F-мера n-грамм (chrF/chrF++)              |
| [StringSimilarity](docs/ru/nlp_metrics_ru.md#stringsimilarity) | Метрики редакционного расстояния (Левенштейн, Джаро) |

Полная документация: [Руководство по NLP метрикам](docs/ru/nlp_metrics_ru.md)

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
    <version>0.3.0</version>
    <scope>test</scope>
</dependency>
        <!-- И любые нужные вам стартеры из экосистемы spring-ai -->
<dependency>
<groupId>org.springframework.ai</groupId>
<artifactId>spring-ai-starter-model-openai</artifactId>
<version>1.1.2</version>
<scope>test</scope>
</dependency>
```

#### Gradle

```groovy
testImplementation 'io.github.ai-qa-solutions:spring-ai-ragas-spring-boot-starter:0.3.0'
testImplementation 'org.springframework.ai:spring-ai-starter-model-openai:1.1.2'
```

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
    ragas:
      # Конфигурация мультимодельной оценки
      providers:
        auto-detect-beans: false
        openai-compatible:
          - name: openrouter
            base-url: https://openrouter.ai/api
            api-key: ${OPENROUTER_API_KEY}
            chat-models:
              - { id: anthropic/claude-3.5-sonnet }
              - { id: google/gemini-2.5-flash }
              - { id: openai/gpt-4o-mini }
        default-provider:
          enabled: false
        default-options:
          temperature: 0.0
          max-tokens: 1000
```

### Несколько провайдеров (OpenRouter + cloud.ru)

Пример конфигурации с двумя провайдерами — OpenRouter для глобальных моделей и cloud.ru Evolution для моделей, размещённых в России:

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
          model: google/gemini-2.0-flash-001
          temperature: 0.0

    ragas:
      providers:
        auto-detect-beans: false
        openai-compatible:
          # Провайдер 1: OpenRouter — глобальные модели
          - name: openrouter
            base-url: https://openrouter.ai/api
            api-key: ${OPENROUTER_API_KEY}
            chat-models:
              - { id: anthropic/claude-3.5-sonnet }
              - { id: openai/gpt-4o }
              - { id: google/gemini-2.0-flash-exp }
          # Провайдер 2: cloud.ru Evolution — модели в России
          - name: cloudru
            base-url: https://foundation-models.api.cloud.ru
            api-key: ${CLOUD_RU_API_KEY}
            chat-models:
              - { id: Qwen/Qwen3-235B-A22B-Instruct-2507 }
              - { id: openai/gpt-oss-120b }
              - { id: t-tech/T-pro-it-2.0 }
        default-provider:
          enabled: false
        default-options:
          temperature: 0.0
          max-tokens: 1000
```

#### Поддерживаемые OpenAI-совместимые провайдеры

|     Провайдер      |                 Base URL                 |         Примечание         |
|--------------------|------------------------------------------|----------------------------|
| OpenRouter         | `https://openrouter.ai/api`              | Доступ к 200+ моделям      |
| cloud.ru Evolution | `https://foundation-models.api.cloud.ru` | Российский хостинг         |
| Groq               | `https://api.groq.com/openai`            | Быстрый инференс           |
| Together AI        | `https://api.together.xyz`               | Open-source модели         |
| Fireworks AI       | `https://api.fireworks.ai/inference`     | Быстрые open-source модели |
| Azure OpenAI       | `https://{resource}.openai.azure.com`    | Enterprise Azure           |
| Ollama             | `http://localhost:11434`                 | Локальные модели           |

### Внешние Spring AI стартеры (GigaChat, Anthropic и др.)

Библиотека автоматически обнаруживает бины ChatModel и EmbeddingModel из внешних Spring AI стартеров. Дополнительная конфигурация не требуется — просто добавьте зависимость стартера и настройте его.

#### Пример: Интеграция с GigaChat

**1. Добавьте зависимость GigaChat стартера:**

```xml
<dependency>
    <groupId>chat.giga</groupId>
    <artifactId>spring-ai-starter-model-gigachat</artifactId>
    <version>1.1.1</version>
</dependency>
```

**2. Настройте GigaChat в application.yml:**

```yaml
spring:
  ai:
    # Конфигурация GigaChat
    gigachat:
      auth:
        bearer:
          api-key: ${GIGACHAT_API_KEY}
      chat:
        options:
          model: GigaChat
          temperature: 0.5
      embedding:
        options:
          model: Embeddings

    # RAGAS автоматически обнаружит модели GigaChat
    ragas:
      providers:
        auto-detect-beans: true  # Включено по умолчанию
```

**3. Используйте модели GigaChat:**

```java
@Service
public class GigaChatService {

    private final ChatClientStore chatClientStore;

    public String chat(String message) {
        // GigaChat автоматически обнаружен и доступен по ID модели
        ChatClient client = chatClientStore.get("GigaChat");
        return client.prompt().user(message).call().content();
    }
}
```

#### Поддерживаемые внешние стартеры

|  Стартер  |                   Артефакт                   | ChatModel | EmbeddingModel |
|-----------|----------------------------------------------|-----------|----------------|
| GigaChat  | `chat.giga:spring-ai-starter-model-gigachat` | GigaChat  | Embeddings     |
| Anthropic | `spring-ai-starter-model-anthropic`          | Claude    | -              |
| Ollama    | `spring-ai-starter-model-ollama`             | *         | *              |
| Mistral   | `spring-ai-starter-model-mistral-ai`         | *         | *              |
| Vertex AI | `spring-ai-starter-model-vertex-ai`          | *         | *              |

\* ID модели зависит от конфигурации

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

### Оценка агентов (Многоэтапные диалоги)

Метрики агентов поддерживают типизированные классы сообщений для многоэтапных диалогов:

```java
import ai.qa.solutions.sample.message.*;

Sample sample = Sample.builder()
        .userInputMessages(List.of(
                new HumanMessage("Забронируй рейс в Нью-Йорк"),
                new AIMessage("Ищу рейсы...", List.of(
                        new ToolCall("search_flights", Map.of("destination", "NYC"))
                )),
                new ToolMessage("Найдено 5 рейсов"),
                new AIMessage("Я нашёл 5 вариантов. Рейс UA123 вылетает в 9:00.")
        ))
        .referenceToolCalls(List.of(
                new ToolCall("search_flights", Map.of("destination", "NYC"))
        ))
        .reference("Рейс в Нью-Йорк забронирован")
        .build();

Double score = agentGoalAccuracy.multiTurnScore(config, sample);
```

## Allure отчёты (Опционально)

Для генерации HTML-отчётов с объяснениями оценок и временной шкалой выполнения добавьте модуль интеграции с Allure.
Требуется предварительно настроенный AspectJ и Allure в вашем проекте.
Пример полной настройки см. в [pom.xml](pom.xml).

```xml
<dependency>
    <groupId>io.github.ai-qa-solutions</groupId>
    <artifactId>spring-ai-ragas-allure</artifactId>
    <version>0.3.0</version>
    <scope>test</scope>
</dependency>
```

Примеры отчётов:

**Универсальные:**
[AspectCritic](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/AspectCriticMetric_ru.html) |
[SimpleCriteriaScore](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/SimpleCriteriaScoreMetric_ru.html) |
[RubricsScore](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/RubricsScoreMetric_ru.html)

**Извлечение (RAG):**
[Faithfulness](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/FaithfulnessMetric_ru.html) |
[ContextPrecision](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/ContextPrecisionMetric_ru.html) |
[ContextRecall](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/ContextRecallMetric_en.html) |
[ContextEntityRecall](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/ContextEntityRecallMetric_en.html) |
[NoiseSensitivity](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/NoiseSensitivityMetric_en.html) |
[ResponseRelevancy](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/ResponseRelevancyMetric_en.html)

**Агенты:**
[AgentGoalAccuracy](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/AgentGoalAccuracyMetric_en.html) |
[ToolCallAccuracy](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/ToolCallAccuracyMetric_en.html) |
[TopicAdherence](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/TopicAdherenceMetric_en.html)

**Ответы:**
[AnswerCorrectness](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/AnswerCorrectnessMetric_en.html) |
[FactualCorrectness](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/FactualCorrectnessMetric_en.html) |
[SemanticSimilarity](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/SemanticSimilarityMetric_en.html)

**NVIDIA:**
[AnswerAccuracy](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/AnswerAccuracyMetric_en.html) |
[ContextRelevance](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/ContextRelevanceMetric_en.html) |
[ResponseGroundedness](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/ResponseGroundednessMetric_en.html)

**NLP:**
[BleuScore](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/BleuScoreMetric_en.html) |
[RougeScore](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/RougeScoreMetric_en.html) |
[ChrfScore](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/ChrfScoreMetric_en.html) |
[StringSimilarity](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/StringSimilarityMetric_en.html)

Полная документация: [Руководство по интеграции с Allure](spring-ai-ragas-allure/README.md)

## Архитектура

```
spring-ai-ragas/
├── spring-ai-ragas-metrics/              # Основная библиотека метрик (20+ метрик)
│   └── ai.qa.solutions/
│       ├── metric/                       # Базовые классы метрик
│       │   ├── Metric                    # Интерфейс метрики
│       │   └── AbstractMultiModelMetric  # Базовый класс для мультимодельных метрик
│       └── metrics/
│           ├── general/                  # Общие метрики
│           ├── retrieval/                # Метрики для RAG
│           ├── agent/                    # Метрики оценки агентов
│           ├── response/                 # Метрики качества ответов
│           ├── nvidia/                   # Метрики в стиле NVIDIA
│           └── nlp/                      # NLP метрики (без LLM)
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
├── spring-ai-ragas-allure/               # Интеграция с Allure отчётами
│   └── ai.qa.solutions.allure/
│       ├── listener/                     # AllureMetricExecutionListener
│       ├── nlp/                          # AllureNlpMetricHelper
│       ├── explanation/                  # Классы объяснения оценок
│       ├── methodology/                  # Документация методологии (en/ru)
│       └── template/                     # Freemarker шаблоны отчётов
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

