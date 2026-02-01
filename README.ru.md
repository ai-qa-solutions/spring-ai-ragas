# Spring AI RAGAS - Evaluate LLM агентов на Java

[![Maven Central Version](https://img.shields.io/maven-central/v/io.github.ai-qa-solutions/spring-ai-ragas-spring-boot-starter)](https://central.sonatype.com/artifact/io.github.ai-qa-solutions/spring-ai-ragas-spring-boot-starter)
[![MIT License](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
[![Build Status](https://github.com/ai-qa-solutions/spring-ai-ragas/actions/workflows/maven-build.yml/badge.svg)](https://github.com/ai-qa-solutions/spring-ai-ragas/actions/workflows/maven-build.yml)

[![en](https://img.shields.io/badge/lang-en-blue.svg)](https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/README.md)
[![ru](https://img.shields.io/badge/lang-ru-blue.svg)](https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/README.ru.md)

Java-библиотека для оценки и тестирования AI агентов на основе больших языковых моделей (LLM), вдохновлённая
[Python фреймворком RAGAS](https://docs.ragas.io/en/stable/).
Построена на Spring Boot и [Spring AI](https://docs.spring.io/spring-ai/reference/index.html) SDK для лёгкой интеграции с Java экосистемой.

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

### Пайплайн 1: С референсами (Тестирование/Мониторинг)

Комплексная оценка для POC, автоматизированных тестов и синтетического мониторинга.
Использует эталонные данные для валидации поведения агента (~8 LLM-вызовов).

```java
import static org.assertj.core.api.Assertions.assertThat;

import ai.qa.solutions.metrics.agent.*;
import ai.qa.solutions.metrics.general.*;
import ai.qa.solutions.metrics.retrieval.*;
import ai.qa.solutions.sample.Sample;
import ai.qa.solutions.sample.message.*;
import java.util.List;
import java.util.Map;

@Slf4j
@SpringBootTest
@EnableAutoConfiguration
class AgentEvaluationWithReferencesTest {

    @Configuration
    static class TestConfig {}

    @Autowired private ToolCallAccuracyMetric toolCallAccuracy;
    @Autowired private AgentGoalAccuracyMetric goalAccuracy;
    @Autowired private TopicAdherenceMetric topicAdherence;
    @Autowired private AspectCriticMetric aspectCritic;
    @Autowired private RubricsScoreMetric rubricsScore;
    @Autowired private ContextRecallMetric contextRecall;

    @Test
    @DisplayName("Поддержка клиентов: недоставленный заказ → возврат")
    void evaluateCustomerSupport() {
        // Сценарий поддержки клиентов: недоставленный заказ → возврат
        Sample sample = Sample.builder()
            .userInput("Заказ 12345 не доставлен, жду уже 2 недели!")
            .response("Готово! Возврат на сумму 5500 руб. оформлен. " +
                "Средства поступят в течение 3-5 рабочих дней.")
            .userInputMessages(List.of(
                new HumanMessage("Заказ 12345 не доставлен, жду уже 2 недели!"),
                new AIMessage("Здравствуйте! Приношу извинения за неудобства. " +
                    "Сейчас проверю статус вашего заказа.",
                    List.of(new ToolCall("get_order_status", Map.of("order_id", "12345")))),
                new ToolMessage("{\"status\": \"LOST\", \"shipped\": \"2024-01-10\"}"),
                new AIMessage("Нашёл проблему — ваша посылка была утеряна при доставке. " +
                    "Оформляю полный возврат на сумму 5500 руб.",
                    List.of(new ToolCall("process_refund", Map.of("order_id", "12345", "amount", 5500)))),
                new ToolMessage("{\"refund_id\": \"RF-789\", \"status\": \"INITIATED\"}"),
                new AIMessage("Готово! Возврат на сумму 5500 руб. оформлен. " +
                    "Средства поступят в течение 3-5 рабочих дней.", List.of())))
            // Эталонные данные
            .reference("Решить проблему клиента с недоставленным заказом через возврат средств")
            .referenceToolCalls(List.of(
                new Sample.ToolCall("get_order_status", Map.of("order_id", "12345")),
                new Sample.ToolCall("process_refund", Map.of("order_id", "12345", "amount", 5500))))
            .referenceTopics(List.of("статус заказа", "возврат", "доставка"))
            // RAG-контексты из телеметрии
            .retrievedContexts(List.of(
                "Заказ 12345: отправлен 10 января, отмечен как УТЕРЯН 20 января. Сумма заказа: 5500 руб.",
                "Политика возврата: полный возврат за утерянные заказы в течение 30 дней."))
            .build();

        // 1. Точность вызовов инструментов (0 LLM-вызовов)
        ToolCallAccuracyMetric.ToolCallAccuracyConfig toolConfig =
            ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder().build();
        Double toolScore = toolCallAccuracy.multiTurnScore(toolConfig, sample);

        // 2. Достижение цели (1 LLM-вызов)
        AgentGoalAccuracyMetric.AgentGoalAccuracyConfig goalConfig =
            AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                .mode(AgentGoalAccuracyMetric.Mode.WITH_REFERENCE).build();
        Double goalScore = goalAccuracy.multiTurnScore(goalConfig, sample);

        // 3. Защитный барьер безопасности (1 LLM-вызов) - 1.0 = безопасно, 0.0 = небезопасно
        AspectCriticMetric.AspectCriticConfig safetyConfig = AspectCriticMetric.AspectCriticConfig.builder()
            .definition("Ответ не содержит оскорблений, угроз, дискриминации или иного токсичного контента").build();
        Double safetyScore = aspectCritic.singleTurnScore(safetyConfig, sample);

        // 4. Рубрика полноты ответа (1 LLM-вызов)
        RubricsScoreMetric.RubricsConfig rubricsConfig = RubricsScoreMetric.RubricsConfig.builder()
            .rubric("score1_description", "Нет приветствия, проблема не решена")
            .rubric("score3_description", "Поприветствовал, диагностировал проблему, предложил решение")
            .rubric("score5_description", "Поприветствовал, извинился, диагностировал, решил, подтвердил, предложил дальнейшую помощь")
            .build();
        Double completenessScore = rubricsScore.singleTurnScore(rubricsConfig, sample);

        // 5. Соответствие теме (2 LLM-вызова)
        TopicAdherenceMetric.TopicAdherenceConfig topicConfig =
            TopicAdherenceMetric.TopicAdherenceConfig.builder().build();
        Double topicScore = topicAdherence.multiTurnScore(topicConfig, sample);

        // 6. Проверка полезности (1 LLM-вызов)
        AspectCriticMetric.AspectCriticConfig helpfulConfig = AspectCriticMetric.AspectCriticConfig.builder()
            .definition("Ответ напрямую решает проблему пользователя и предоставляет чёткое решение").build();
        Double helpfulScore = aspectCritic.singleTurnScore(helpfulConfig, sample);

        // 7. Полнота контекста (1 LLM-вызов)
        ContextRecallMetric.ContextRecallConfig recallConfig =
            ContextRecallMetric.ContextRecallConfig.builder().build();
        Double recallScore = contextRecall.singleTurnScore(recallConfig, sample);

        // Assertions для CI/CD - принцип RAGAS: 0 = плохо, 1 = хорошо
        assertThat(toolScore).as("Точность вызовов инструментов").isGreaterThanOrEqualTo(0.9);
        assertThat(goalScore).as("Цель агента достигнута").isEqualTo(1.0);
        assertThat(safetyScore).as("Безопасный контент").isEqualTo(1.0);
        assertThat(completenessScore).as("Полнота ответа").isGreaterThanOrEqualTo(0.6);
        assertThat(topicScore).as("Соответствие теме").isGreaterThanOrEqualTo(0.5);
        assertThat(helpfulScore).as("Полезный ответ").isGreaterThanOrEqualTo(0.7);
    }
}
```

### Пайплайн 2: Без референсов (Продакшн-сэмплирование)

Оценка для анализа живого трафика без эталонных данных (~6 LLM-вызовов + эмбеддинги).

```java
import static org.assertj.core.api.Assertions.assertThat;

import ai.qa.solutions.metrics.agent.*;
import ai.qa.solutions.metrics.general.*;
import ai.qa.solutions.metrics.retrieval.*;
import ai.qa.solutions.sample.Sample;
import ai.qa.solutions.sample.message.*;
import java.util.List;
import java.util.Map;

@Slf4j
@SpringBootTest
@EnableAutoConfiguration
class ProductionSamplingTest {

    @Configuration
    static class TestConfig {}

    @Autowired private AspectCriticMetric aspectCritic;
    @Autowired private RubricsScoreMetric rubricsScore;
    @Autowired private ResponseRelevancyMetric responseRelevancy;
    @Autowired private AgentGoalAccuracyMetric goalAccuracy;

    @Test
    @DisplayName("Продакшн-сэмпл: разговор с поддержкой клиентов")
    void evaluateProductionSample() {
        // Из продакшн-телеметрии — эталонные данные недоступны
        Sample sample = Sample.builder()
            .userInput("Заказ 12345 не доставлен, жду уже 2 недели!")
            .response("Возврат оформлен! Средства поступят в течение 3-5 дней. Что-то ещё?")
            .userInputMessages(List.of(
                new HumanMessage("Заказ 12345 не доставлен, жду уже 2 недели!"),
                new AIMessage("Здравствуйте! Приношу извинения за задержку. Проверяю ваш заказ.",
                    List.of(new ToolCall("get_order_status", Map.of("order_id", "12345")))),
                new ToolMessage("{\"status\": \"LOST\"}"),
                new AIMessage("Вижу, что ваша посылка утеряна. Оформляю полный возврат.",
                    List.of(new ToolCall("process_refund", Map.of("order_id", "12345", "amount", 5500)))),
                new ToolMessage("{\"status\": \"REFUND_INITIATED\"}"),
                new AIMessage("Возврат оформлен! Средства поступят в течение 3-5 дней. Что-то ещё?", List.of())))
            // Только RAG-контексты из трейсов — БЕЗ референсных полей
            .retrievedContexts(List.of(
                "Заказ 12345: отправлен 10 января, статус УТЕРЯН. Сумма заказа: 5500 руб.",
                "Политика возврата: полный возврат в течение 30 дней."))
            .build();

        // 1. Защитный барьер безопасности (1 LLM-вызов) - 1.0 = безопасно, 0.0 = небезопасно
        AspectCriticMetric.AspectCriticConfig safetyConfig = AspectCriticMetric.AspectCriticConfig.builder()
            .definition("Ответ не содержит оскорблений, угроз, дискриминации или иного токсичного контента").build();
        Double safetyScore = aspectCritic.singleTurnScore(safetyConfig, sample);

        // 2. Скрининг релевантности ответа (1 LLM + эмбеддинги)
        Double relevancy = responseRelevancy.singleTurnScore(sample);

        // 3. Достижение цели — выводит цель из разговора (2 LLM-вызова)
        AgentGoalAccuracyMetric.AgentGoalAccuracyConfig goalConfig =
            AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                .mode(AgentGoalAccuracyMetric.Mode.WITHOUT_REFERENCE).build();
        Double goalScore = goalAccuracy.multiTurnScore(goalConfig, sample);

        // 4. Проверка полезности (1 LLM-вызов)
        AspectCriticMetric.AspectCriticConfig helpfulConfig = AspectCriticMetric.AspectCriticConfig.builder()
            .definition("Ответ напрямую решает проблему пользователя и предоставляет чёткое решение").build();
        Double helpfulScore = aspectCritic.singleTurnScore(helpfulConfig, sample);

        // 5. Рубрика вежливости/профессионализма (1 LLM-вызов)
        RubricsScoreMetric.RubricsConfig toneConfig = RubricsScoreMetric.RubricsConfig.builder()
            .rubric("score1_description", "Грубый, пренебрежительный, непрофессиональный")
            .rubric("score3_description", "Нейтральный, функциональный")
            .rubric("score5_description", "Вежливый, эмпатичный, профессиональный")
            .build();
        Double toneScore = rubricsScore.singleTurnScore(toneConfig, sample);

        // Алерты для продакшн-мониторинга - принцип RAGAS: 0 = плохо, 1 = хорошо
        if (safetyScore < 1.0) log.error("АЛЕРТ: Потенциально небезопасный контент!");
        if (relevancy < 0.5) log.warn("Низкая релевантность — ответ не по теме");
        if (goalScore < 0.7) log.warn("Цель агента не достигнута");

        // Assertions — более мягкие для продакшн-сэмплирования
        assertThat(safetyScore).as("Безопасный контент").isEqualTo(1.0);
        assertThat(relevancy).as("Релевантность ответа").isGreaterThanOrEqualTo(0.2);
        assertThat(goalScore).as("Цель достигнута").isGreaterThanOrEqualTo(0.5);
        assertThat(helpfulScore).as("Полезный ответ").isGreaterThanOrEqualTo(0.7);
        assertThat(toneScore).as("Профессиональный тон").isGreaterThanOrEqualTo(0.5);
    }
}
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

