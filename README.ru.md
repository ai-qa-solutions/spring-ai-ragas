# Spring AI RAGAS - Evaluate LLM агентов на Java

[![Maven Central Version](https://img.shields.io/maven-central/v/io.github.ai-qa-solutions/spring-ai-ragas-spring-boot-starter?color=green)](https://central.sonatype.com/artifact/io.github.ai-qa-solutions/spring-ai-ragas-spring-boot-starter)
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
- **Rate Limiting**: Контроль RPS по провайдерам с [Bucket4j](https://github.com/bucket4j/bucket4j) (стратегии WAIT/REJECT)
- **Мультиязычность**: Поддержка русского и английского языков
- **Расширяемость**: Легко создавать собственные метрики

## Сценарии использования

Комплексный анализ качества агентских систем в двух сценариях:

- **С референсами**: Оценка POC, автотесты, синтетический мониторинг
- **Без референсов**: Семплирование и анализ живого продакшен-трафика

## Совместимость версий

| Spring AI RAGAS | RAGAS (Python) | Spring Boot | Spring AI |
|-----------------|----------------|-------------|-----------|
| 0.3.3           | 0.3.x          | 3.5.x       | 1.1.x     |

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
    <version>0.3.3</version>
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
testImplementation 'io.github.ai-qa-solutions:spring-ai-ragas-spring-boot-starter:0.3.3'
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
    <version>1.1.2</version>
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
        # По умолчанию GIGACHAT_API_PERS. Для корпоративных ключей укажите GIGACHAT_API_CORP или GIGACHAT_API_B2B.
        scope: ${GIGACHAT_API_SCOPE:GIGACHAT_API_PERS}
        # dev-only: GigaChat использует сертификат от Минцифры.
        # Для прод-окружений настройте ssl-bundle с доверенным CA вместо unsafe-ssl.
        unsafe-ssl: true
      chat:
        options:
          # Для evaluation рекомендуется max-версия: lite-модель (`GigaChat`) периодически
          # обрывает JSON-ответы в сложных метриках (например ResponseRelevancyMetric).
          model: GigaChat-2-Max
          # Детерминизм LLM-судьи — температура строго 0.0.
          temperature: 0.0
          # ResponseRelevancyMetric генерирует несколько вопросов в JSON-массиве,
          # при значении 1000 GigaChat может обрезать вывод. Рекомендуется >= 1500.
          max-tokens: 2000
      embedding:
        options:
          # Актуальная модель эмбеддингов — EmbeddingsGigaR.
          model: EmbeddingsGigaR

    # RAGAS автоматически обнаружит модели GigaChat
    ragas:
      providers:
        auto-detect-beans: true  # Включено по умолчанию
        default-options:
          temperature: 0.0
          max-tokens: 2000
```

**3. Используйте модели GigaChat:**

```java
@Service
public class GigaChatService {

    private final ChatClientStore chatClientStore;

    public String chat(String message) {
        // ID модели должен совпадать со значением spring.ai.gigachat.chat.options.model.
        // В конфиге выше указано GigaChat-2-Max — именно этот ID и используем здесь.
        ChatClient client = chatClientStore.get("GigaChat-2-Max");
        return client.prompt().user(message).call().content();
    }
}
```

#### Поддерживаемые внешние стартеры

|  Стартер  |                   Артефакт                   |                              ChatModel                               |       EmbeddingModel        |
|-----------|----------------------------------------------|----------------------------------------------------------------------|-----------------------------|
| GigaChat  | `chat.giga:spring-ai-starter-model-gigachat` | GigaChat, GigaChat-Pro, GigaChat-Max, GigaChat-2-Pro, GigaChat-2-Max | Embeddings, EmbeddingsGigaR |
| Anthropic | `spring-ai-starter-model-anthropic`          | Claude                                                               | -                           |
| Ollama    | `spring-ai-starter-model-ollama`             | *                                                                    | *                           |
| Mistral   | `spring-ai-starter-model-mistral-ai`         | *                                                                    | *                           |
| Vertex AI | `spring-ai-starter-model-vertex-ai`          | *                                                                    | *                           |

\* ID модели зависит от конфигурации

> **Заметка про модели GigaChat.** Для LLM-as-judge рекомендуется `GigaChat-2-Max` (или `GigaChat-Max` / `GigaChat-2-Pro`): lite-модель `GigaChat` периодически отдаёт невалидный JSON в метриках, которые генерируют массивы (`ResponseRelevancyMetric`, `ContextPrecisionMetric` и др.). Для эмбеддингов используйте `EmbeddingsGigaR` — он точнее и быстрее legacy-модели `Embeddings`.

## Как писать критерии: best practices

LLM-as-judge даёт нестабильные оценки, если критерий расплывчатый. Следующие правила
применяются ко всем примерам ниже:

- **AspectCritic — только атомарные бинарные факты.** Один тезис = один проверяемый
  признак. Не объединяйте несколько условий через «и». Не используйте слова «примерно»,
  «более-менее», «профессионально», «качественно».
  - ✓ `"AI Response содержит точную сумму '5500' с указанием валюты ('руб.' или '₽')?"`
  - ✗ `"Ответ напрямую решает проблему пользователя и предоставляет чёткое решение"`
  - ✗ `"Агент ответил профессионально?"`
- **SimpleCriteriaScore — одна измеряемая шкала, один аспект.** Каждый уровень шкалы
  описан явным наблюдаемым признаком.
  - ✓ `"Оцени от 1 до 5, насколько конкретно агент указал сумму возврата"`
  - ✗ `"Оцени от 1 до 5, насколько ответ был полезным, вежливым и точным"`
- **AgentGoalAccuracy reference — конкретный оцифрованный результат.** С номерами,
  суммами, датами, названиями инструментов.
  - ✓ `"Оформить возврат 5500 руб. за заказ 12345 и сообщить срок поступления средств в днях"`
  - ✗ `"Решить проблему клиента через возврат средств"`
- **RubricsScore — 5 уровней, каждый добавляет РОВНО 1 наблюдаемый признак одного flow.**
  Все уровни — про один и тот же аспект работы агента.
  - ✓ `1: не идентифицировал → 2: +идентифицировал → 3: +предложил решение → 4: +выполнил действие → 5: +сообщил срок`
  - ✗ `"Плохой / ниже среднего / средний / хороший / отличный"`
  - ✗ Разные аспекты в разных уровнях: «уровень 1 — про даты, уровень 3 — про вежливость»

### Диапазоны значений метрик и пороги в assertions

Метрики возвращают значения в разных диапазонах — пороги надо подбирать с учётом этого:

|                                 Метрика                                  |                 Диапазон                 |            Рекомендуемый порог             |
|--------------------------------------------------------------------------|------------------------------------------|--------------------------------------------|
| `AspectCriticMetric`, `ToolCallAccuracyMetric`                           | бинарный 0 / 1                           | `isEqualTo(1.0)`                           |
| `SimpleCriteriaScoreMetric`                                              | [0..1] (нормализуется из raw)            | `>= 0.75` (raw 4/5) или `== 1.0` (raw 5/5) |
| `RubricsScoreMetric`                                                     | raw integer [1..5], **без нормализации** | `>= 4.0` или `== 5.0`                      |
| `ResponseRelevancyMetric`, `TopicAdherenceMetric`, `ContextRecallMetric` | [0..1]                                   | `>= 0.5` / `>= 0.75` по задаче             |

Порог `>= 0.6` для `RubricsScoreMetric` бессмыслен — любой валидный результат (1..5) его проходит.

## Пример использования

### Пайплайн 1: С референсами (Тестирование/Мониторинг)

Оценка для сценариев, где ожидаемое поведение агента известно заранее:
- **Бизнес-регрессия**: Валидация ответов агента против заранее определённых ожиданий
- **Синтетический мониторинг**: Сервис-наблюдатель по крону отправляет запросы с известными правильными исходами

Референсы = эталонные данные для ожидаемых ответов и вызовов инструментов (~10–15 LLM-вызовов в зависимости от детализации критериев).

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
    @Autowired private SimpleCriteriaScoreMetric simpleCriteriaScore;
    @Autowired private ContextRecallMetric contextRecall;

    @Test
    @DisplayName("Поддержка клиентов: недоставленный заказ → возврат")
    void evaluateCustomerSupport() {
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
            // Reference — КОНКРЕТНЫЙ оцифрованный результат с номером заказа, суммой и явным действием
            .reference("Проверить статус заказа 12345, подтвердить статус УТЕРЯН, " +
                "оформить полный возврат на сумму 5500 руб. и сообщить клиенту " +
                "срок поступления средств в днях")
            .referenceToolCalls(List.of(
                new Sample.ToolCall("get_order_status", Map.of("order_id", "12345")),
                new Sample.ToolCall("process_refund", Map.of("order_id", "12345", "amount", 5500))))
            .referenceTopics(List.of("статус заказа", "возврат средств", "сроки поступления"))
            .retrievedContexts(List.of(
                "Заказ 12345: отправлен 10 января, отмечен как УТЕРЯН 20 января. Сумма заказа: 5500 руб.",
                "Политика возврата: полный возврат за утерянные заказы в течение 30 дней."))
            .build();

        // 1. Точность вызовов инструментов (0 LLM-вызовов)
        Double toolScore = toolCallAccuracy.multiTurnScore(
            ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder().build(), sample);

        // 2. Достижение конкретной оцифрованной цели (1 LLM-вызов)
        Double goalScore = goalAccuracy.multiTurnScore(
            AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                .mode(AgentGoalAccuracyMetric.Mode.WITH_REFERENCE).build(), sample);

        // 3. AspectCritic — атомарные факты, по одному на тезис.
        //    НЕ объединяйте критерии. НЕ используйте расплывчатые слова.
        Double noProfanityScore = aspectCritic.singleTurnScore(
            AspectCriticMetric.AspectCriticConfig.builder()
                .definition("AI Response не содержит мата, оскорблений или обвинений в адрес пользователя?")
                .build(), sample);

        Double refundAmountScore = aspectCritic.singleTurnScore(
            AspectCriticMetric.AspectCriticConfig.builder()
                .definition("AI Response содержит точную сумму возврата '5500' с указанием валюты " +
                    "('руб.', '₽' или эквивалент)?")
                .build(), sample);

        Double timelineStatedScore = aspectCritic.singleTurnScore(
            AspectCriticMetric.AspectCriticConfig.builder()
                .definition("AI Response содержит конкретный срок поступления средств в днях " +
                    "(например '3-5 рабочих дней')?")
                .build(), sample);

        // 4. SimpleCriteriaScore — одна измеряемая шкала, один аспект.
        //    Каждый уровень описан явным наблюдаемым признаком.
        Double amountDetailScore = simpleCriteriaScore.singleTurnScore(
            SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                .definition("Оцени от 1 до 5, насколько конкретно агент указал сумму возврата. " +
                    "1 = сумма не названа; " +
                    "2 = сумма упомянута общими словами ('полный возврат'); " +
                    "3 = назван порядок суммы без точного значения; " +
                    "4 = названа точная сумма без валюты ('5500'); " +
                    "5 = названа точная сумма с валютой ('5500 руб.' или '5500 ₽').")
                .minScore(1.0).maxScore(5.0)
                .build(), sample);

        // 5. RubricsScore — 5 уровней одного flow "цикл обработки обращения".
        //    Каждый следующий уровень добавляет РОВНО 1 наблюдаемый признак.
        Double supportFlowScore = rubricsScore.singleTurnScore(
            RubricsScoreMetric.RubricsConfig.builder()
                .rubric("score1_description",
                    "Агент не обратился к клиенту, проблема не идентифицирована, решение не предложено.")
                .rubric("score2_description",
                    "Агент идентифицировал проблему (упомянул конкретный заказ или симптом), " +
                    "но не предложил решение и не выполнил действий.")
                .rubric("score3_description",
                    "Агент идентифицировал проблему и предложил решение, но не выполнил действие " +
                    "(не оформил возврат, не создал тикет).")
                .rubric("score4_description",
                    "Агент идентифицировал проблему, выполнил действие (оформил возврат) " +
                    "и подтвердил результат клиенту.")
                .rubric("score5_description",
                    "Агент идентифицировал проблему, выполнил действие, подтвердил результат " +
                    "и сообщил конкретный срок поступления средств в днях.")
                .build(), sample);

        // 6. Соответствие заранее заданным темам (2 LLM-вызова)
        Double topicScore = topicAdherence.multiTurnScore(
            TopicAdherenceMetric.TopicAdherenceConfig.builder().build(), sample);

        // 7. Полнота покрытия reference контекстами (1 LLM-вызов)
        Double recallScore = contextRecall.singleTurnScore(
            ContextRecallMetric.ContextRecallConfig.builder().build(), sample);

        // Assertions — пороги с явной семантикой в терминах диапазонов метрик.
        // Не используем пороги "для галочки" типа >= 0.5 для рубрики, которая вернёт 1..5.
        assertThat(toolScore).as("Точность вызовов инструментов").isEqualTo(1.0);
        assertThat(goalScore).as("Цель агента достигнута").isEqualTo(1.0);
        assertThat(noProfanityScore).as("Нет оскорблений").isEqualTo(1.0);
        assertThat(refundAmountScore).as("Сумма возврата с валютой").isEqualTo(1.0);
        assertThat(timelineStatedScore).as("Срок в днях указан").isEqualTo(1.0);
        assertThat(amountDetailScore).as("Детализация суммы (raw 5/5)").isEqualTo(1.0);
        assertThat(supportFlowScore).as("Flow цикла обработки (все 5 признаков)").isEqualTo(5.0);
        assertThat(topicScore).as("Соответствие темам").isGreaterThanOrEqualTo(0.75);
        assertThat(recallScore).as("Полнота контекста").isEqualTo(1.0);
    }
}
```

### Пайплайн 2: Без референсов (Продакшн-сэмплирование)

Оценка для анализа живого трафика без эталонных данных (~8–12 LLM-вызовов + эмбеддинги).

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
    @Autowired private SimpleCriteriaScoreMetric simpleCriteriaScore;
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
                new AIMessage("Возврат оформлен! Средства поступят в течение 3-5 дней. Что-то ещё?",
                    List.of())))
            // Только RAG-контексты из трейсов — БЕЗ референсных полей
            .retrievedContexts(List.of(
                "Заказ 12345: отправлен 10 января, статус УТЕРЯН. Сумма заказа: 5500 руб.",
                "Политика возврата: полный возврат в течение 30 дней."))
            .build();

        // 1. AspectCritic — атомарные факты. Безопасность как один атомарный тезис.
        Double noProfanityScore = aspectCritic.singleTurnScore(
            AspectCriticMetric.AspectCriticConfig.builder()
                .definition("AI Response не содержит мата, оскорблений или обвинений в адрес пользователя?")
                .build(), sample);

        // 2. Проверка конкретного наблюдаемого факта: фраза подтверждения возврата
        Double refundConfirmedScore = aspectCritic.singleTurnScore(
            AspectCriticMetric.AspectCriticConfig.builder()
                .definition("AI Response содержит явную фразу подтверждения оформления возврата " +
                    "(например 'возврат оформлен', 'возврат инициирован', 'refund initiated')?")
                .build(), sample);

        // 3. Проверка конкретного наблюдаемого факта: срок в днях
        Double timelineStatedScore = aspectCritic.singleTurnScore(
            AspectCriticMetric.AspectCriticConfig.builder()
                .definition("AI Response содержит конкретный срок поступления средств в днях " +
                    "(например '3-5 дней', '5 банковских дней')?")
                .build(), sample);

        // 4. SimpleCriteriaScore — одна измеряемая шкала: детализация срока поступления
        Double timelineDetailScore = simpleCriteriaScore.singleTurnScore(
            SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                .definition("Оцени от 1 до 5, насколько конкретно агент указал срок поступления средств. " +
                    "1 = срок не указан; " +
                    "2 = срок указан размыто ('скоро', 'в ближайшее время'); " +
                    "3 = указан общий период без дней ('на этой неделе'); " +
                    "4 = указан диапазон в днях без уточнений ('3-5 дней'); " +
                    "5 = указан диапазон в рабочих/банковских днях с явным типом дней.")
                .minScore(1.0).maxScore(5.0)
                .build(), sample);

        // 5. Скрининг релевантности ответа (1 LLM + эмбеддинги)
        Double relevancy = responseRelevancy.singleTurnScore(sample);

        // 6. Достижение цели — выводит цель из разговора (2 LLM-вызова)
        Double goalScore = goalAccuracy.multiTurnScore(
            AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                .mode(AgentGoalAccuracyMetric.Mode.WITHOUT_REFERENCE).build(), sample);

        // 7. RubricsScore — тот же 5-уровневый flow, что и в Пайплайне 1.
        //    Каждый следующий уровень добавляет РОВНО 1 наблюдаемый признак.
        Double supportFlowScore = rubricsScore.singleTurnScore(
            RubricsScoreMetric.RubricsConfig.builder()
                .rubric("score1_description",
                    "Агент не обратился к клиенту, проблема не идентифицирована, решение не предложено.")
                .rubric("score2_description",
                    "Агент идентифицировал проблему, но не предложил решение и не выполнил действий.")
                .rubric("score3_description",
                    "Агент идентифицировал проблему и предложил решение, но не выполнил действие.")
                .rubric("score4_description",
                    "Агент идентифицировал проблему, выполнил действие и подтвердил результат клиенту.")
                .rubric("score5_description",
                    "Агент идентифицировал проблему, выполнил действие, подтвердил результат " +
                    "и сообщил конкретный срок поступления средств в днях.")
                .build(), sample);

        // Алерты для продакшн-мониторинга — 0 = плохо, 1 = хорошо
        if (noProfanityScore < 1.0) log.error("АЛЕРТ: Потенциально небезопасный контент!");
        if (relevancy < 0.5) log.warn("Низкая релевантность — ответ не по теме");
        if (goalScore < 1.0) log.warn("Цель агента не достигнута");

        // Assertions — без расплывчатых порогов
        assertThat(noProfanityScore).as("Нет оскорблений").isEqualTo(1.0);
        assertThat(refundConfirmedScore).as("Подтверждение возврата").isEqualTo(1.0);
        assertThat(timelineStatedScore).as("Срок в днях").isEqualTo(1.0);
        assertThat(timelineDetailScore).as("Детализация срока (raw >=4/5)").isGreaterThanOrEqualTo(0.75);
        assertThat(relevancy).as("Релевантность").isGreaterThanOrEqualTo(0.5);
        assertThat(goalScore).as("Цель достигнута").isEqualTo(1.0);
        assertThat(supportFlowScore).as("Flow цикла обработки (>=4)").isGreaterThanOrEqualTo(4.0);
    }
}
```

> **Примеры взяты из рабочего демо-проекта.** Оба пайплайна регулярно прогоняются на `GigaChat-2-Max` и проходят все assertions. Критерии построены по принципу «один атомарный наблюдаемый факт на тезис» — это главное условие стабильной оценки LLM-судьёй.

## Allure отчёты (Опционально)

Для генерации HTML-отчётов с объяснениями оценок и временной шкалой выполнения добавьте модуль интеграции с Allure.
Требуется предварительно настроенный AspectJ и Allure в вашем проекте.
Пример полной настройки см. в [pom.xml](pom.xml).

```xml
<dependency>
    <groupId>io.github.ai-qa-solutions</groupId>
    <artifactId>spring-ai-ragas-allure</artifactId>
    <version>0.3.3</version>
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
[ContextRecall](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/ContextRecallMetric_ru.html) |
[ContextEntityRecall](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/ContextEntityRecallMetric_ru.html) |
[NoiseSensitivity](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/NoiseSensitivityMetric_ru.html) |
[ResponseRelevancy](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/ResponseRelevancyMetric_ru.html)

**Агенты:**
[AgentGoalAccuracy](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/AgentGoalAccuracyMetric_ru.html) |
[ToolCallAccuracy](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/ToolCallAccuracyMetric_ru.html) |
[TopicAdherence](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/TopicAdherenceMetric_ru.html)

**Ответы:**
[AnswerCorrectness](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/AnswerCorrectnessMetric_ru.html) |
[FactualCorrectness](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/FactualCorrectnessMetric_ru.html) |
[SemanticSimilarity](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/SemanticSimilarityMetric_ru.html)

**NVIDIA:**
[AnswerAccuracy](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/AnswerAccuracyMetric_ru.html) |
[ContextRelevance](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/ContextRelevanceMetric_ru.html) |
[ResponseGroundedness](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/ResponseGroundednessMetric_ru.html)

**NLP:**
[BleuScore](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/BleuScoreMetric_ru.html) |
[RougeScore](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/RougeScoreMetric_ru.html) |
[ChrfScore](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/ChrfScoreMetric_ru.html) |
[StringSimilarity](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/StringSimilarityMetric_ru.html)

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

