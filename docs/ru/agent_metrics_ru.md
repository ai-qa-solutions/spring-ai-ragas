# Метрики агентов

Метрики агентов оценивают качество AI-агентов в многоэтапных диалогах, включая достижение целей,
точность использования инструментов и соблюдение темы. Эти метрики предназначены для оценки
агентных AI-систем, которые могут использовать инструменты и вести диалоги.

## Конфигурация

### application.yaml

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
              - { id: deepseek/deepseek-v3.2 }
        default-provider:
          enabled: false
        default-options:
          temperature: 0.0
          max-tokens: 1000
  threads:
    virtual:
      enabled: true
```

---

## AgentGoalAccuracy

AgentGoalAccuracy оценивает, успешно ли AI-агент достиг поставленной цели в ходе диалога.
Поддерживает два режима оценки: с эталонной целью и без неё.

### Принцип работы

Метрика поддерживает два режима:

1. **WITH_REFERENCE**: Сравнивает результат диалога с указанной эталонной целью
2. **WITHOUT_REFERENCE**: Выводит цель из диалога и оценивает её достижение

```java
// Из AgentGoalAccuracyMetric.java - процесс оценки
class Example {
    // Режим 1: Сравнение с эталоном
    void withReference(String conversation, String reference) {
        // LLM сравнивает фактический результат с ожидаемым
        // Возвращает: goalAchieved (true/false) + reasoning
    }

    // Режим 2: Вывод цели и оценка
    void withoutReference(String conversation) {
        // Шаг 1: LLM выводит цель пользователя из диалога
        // Шаг 2: LLM оценивает, была ли цель достигнута
        // Возвращает: goalAchieved (true/false) + reasoning
    }
}
```

### Пример

```java
package ai.qa.solutions.metrics.agent.ru;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.agent.AgentGoalAccuracyMetric;
import ai.qa.solutions.sample.Sample;
import ai.qa.solutions.sample.message.*;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

@Slf4j
@EnableAutoConfiguration
@SpringBootTest(classes = AgentGoalAccuracyTest.TestConfiguration.class)
class AgentGoalAccuracyTest {

    @Configuration
    public static class TestConfiguration {
    }

    @Autowired
    private AgentGoalAccuracyMetric agentGoalAccuracyMetric;

    @Test
    @DisplayName("AgentGoalAccuracy: Цель достигнута с эталоном")
    void testGoalAchievedWithReference() {
        Sample sample = Sample.builder()
                .userInputMessages(List.of(
                        new HumanMessage("Мне нужно забронировать авиабилет в Париж на понедельник"),
                        new AIMessage("Помогу вам забронировать авиабилет в Париж.", List.of(
                                new ToolCall("search_flights", Map.of("destination", "Париж", "date", "понедельник"))
                        )),
                        new ToolMessage("Найдено: Air France в 10:00 за 450$"),
                        new AIMessage("Нашёл несколько рейсов. Лучший вариант - Air France в 10:00 за 450$. Забронировать?"),
                        new HumanMessage("Да, пожалуйста, забронируйте"),
                        new AIMessage("Готово! Я забронировал ваш рейс Air France в Париж на понедельник в 10:00. Номер подтверждения: AF12345.")
                ))
                .reference("Успешно забронировать авиабилет в Париж для пользователя")
                .build();

        AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                        .mode(AgentGoalAccuracyMetric.Mode.WITH_REFERENCE)
                        .build();

        Double score = agentGoalAccuracyMetric.multiTurnScore(config, sample);

        log.info("Оценка достижения цели: {}", score);
        assertTrue(score >= 0.9, "Ожидается высокая оценка для достигнутой цели");
    }

    @Test
    @DisplayName("AgentGoalAccuracy: Цель выводится без эталона")
    void testGoalInferredWithoutReference() {
        Sample sample = Sample.builder()
                .userInputMessages(List.of(
                        new HumanMessage("Какая погода в Токио?"),
                        new AIMessage("Проверяю погоду в Токио для вас.", List.of(
                                new ToolCall("get_weather", Map.of("city", "Токио"))
                        )),
                        new ToolMessage("Токио: 22°C, переменная облачность"),
                        new AIMessage("Текущая погода в Токио: 22°C, переменная облачность. Прогноз на остаток дня - ясное небо.")
                ))
                .build();

        AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                        .mode(AgentGoalAccuracyMetric.Mode.WITHOUT_REFERENCE)
                        .build();

        Double score = agentGoalAccuracyMetric.multiTurnScore(config, sample);

        log.info("Оценка достижения цели: {}", score);
        assertTrue(score >= 0.8, "Ожидается высокая оценка при выводе и достижении цели");
    }
}
```

### Конфигурация

| Параметр |     Тип      | Обязательный |  По умолчанию  |               Описание               |
|----------|--------------|--------------|----------------|--------------------------------------|
| `mode`   | Mode         | Нет          | WITH_REFERENCE | WITH_REFERENCE или WITHOUT_REFERENCE |
| `models` | List<String> | Нет          | все            | Конкретные ID моделей для оценки     |

### Когда использовать

- Оценка диалоговых AI-агентов
- Измерение процента выполнения задач
- Оценка целеориентированных диалоговых систем
- Контроль качества чат-ботов службы поддержки

---

## ToolCallAccuracy

ToolCallAccuracy оценивает точность вызовов инструментов агентом, сравнивая фактические вызовы
с эталонными. Это не-LLM метрика, которая алгоритмически вычисляет precision, recall и F1.

### Принцип работы

Метрика вычисляет precision, recall и F1:

1. **Сопоставление**: Сопоставляет фактические вызовы с эталонными
2. **Precision**: Правильные вызовы / Всего фактических вызовов
3. **Recall**: Правильные вызовы / Всего эталонных вызовов
4. **F1 Score**: Гармоническое среднее precision и recall

```java
// Из ToolCallAccuracyMetric.java - вычисление оценки
class Example {
    void computeScore() {
        int truePositives = countMatchedCalls();
        int falsePositives = actualCalls.size() - truePositives;
        int falseNegatives = referenceCalls.size() - truePositives;

        double precision = (double) truePositives / actualCalls.size();
        double recall = (double) truePositives / referenceCalls.size();
        double f1 = 2 * precision * recall / (precision + recall);
    }
}
```

**Режимы сопоставления:**

- **STRICT**: Точное совпадение имён инструментов и всех аргументов
- **FLEXIBLE**: Допускает частичное совпадение аргументов по порогу

### Пример

```java
package ai.qa.solutions.metrics.agent.ru;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.agent.ToolCallAccuracyMetric;
import ai.qa.solutions.sample.Sample;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

@Slf4j
@EnableAutoConfiguration
@SpringBootTest(classes = ToolCallAccuracyTest.TestConfiguration.class)
class ToolCallAccuracyTest {

    @Configuration
    public static class TestConfiguration {
    }

    @Autowired
    private ToolCallAccuracyMetric toolCallAccuracyMetric;

    @Test
    @DisplayName("ToolCallAccuracy: Идеальное использование инструментов")
    void testPerfectToolUsage() {
        Sample sample = Sample.builder()
                .toolCalls(List.of(
                        new Sample.ToolCall("search_flights",
                                Map.of("destination", "Париж", "date", "2024-01-15")),
                        new Sample.ToolCall("book_flight",
                                Map.of("flight_id", "AF123", "passenger", "Иван Иванов"))
                ))
                .referenceToolCalls(List.of(
                        new Sample.ToolCall("search_flights",
                                Map.of("destination", "Париж", "date", "2024-01-15")),
                        new Sample.ToolCall("book_flight",
                                Map.of("flight_id", "AF123", "passenger", "Иван Иванов"))
                ))
                .build();

        ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                        .mode(ToolCallAccuracyMetric.Mode.STRICT)
                        .build();

        Double score = toolCallAccuracyMetric.singleTurnScore(config, sample);

        log.info("Оценка точности вызовов инструментов: {}", score);
        assertTrue(score >= 0.99, "Ожидается идеальная оценка для точного совпадения");
    }

    @Test
    @DisplayName("ToolCallAccuracy: Гибкий режим с частичным совпадением")
    void testFlexibleMode() {
        Sample sample = Sample.builder()
                .toolCalls(List.of(
                        new Sample.ToolCall("get_weather",
                                Map.of("city", "Токио", "units", "celsius"))
                ))
                .referenceToolCalls(List.of(
                        new Sample.ToolCall("get_weather",
                                Map.of("city", "Токио", "units", "fahrenheit"))
                ))
                .build();

        ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                        .mode(ToolCallAccuracyMetric.Mode.FLEXIBLE)
                        .argumentMatchThreshold(0.5)
                        .build();

        Double score = toolCallAccuracyMetric.singleTurnScore(config, sample);

        log.info("Оценка точности вызовов инструментов: {}", score);
        assertTrue(score >= 0.5, "Ожидается частичная оценка для гибкого сопоставления");
    }
}
```

### Конфигурация

|         Параметр         |     Тип      | Обязательный | По умолчанию |                   Описание                    |
|--------------------------|--------------|--------------|--------------|-----------------------------------------------|
| `mode`                   | Mode         | Нет          | STRICT       | STRICT или FLEXIBLE режим сопоставления       |
| `argumentMatchThreshold` | Double       | Нет          | 0.8          | Порог совпадения аргументов в FLEXIBLE режиме |
| `models`                 | List<String> | Нет          | все          | ID моделей (не используется - не-LLM метрика) |

### Когда использовать

- Оценка AI-агентов с function calling
- Тестирование точности выбора инструментов
- Валидация корректности передачи аргументов
- Бенчмаркинг возможностей агентов

---

## TopicAdherence

TopicAdherence оценивает, остаются ли темы диалога в рамках ожидаемых эталонных тем.
Извлекает темы из диалога и классифицирует их относительно разрешённых тем.

### Принцип работы

1. **Извлечение тем**: LLM извлекает обсуждаемые темы из диалога
2. **Классификация тем**: Каждая тема классифицируется как релевантная или нерелевантная
3. **Вычисление оценки**: Вычисляет precision, recall или F1 по конфигурации

```java
// Из TopicAdherenceMetric.java - вычисление оценки
class Example {
    double computeScore(List<TopicClassification> classifications, List<String> referenceTopics, Mode mode) {
        // Precision: какая доля извлечённых тем релевантна
        double precision = (double) onTopicCount / classifications.size();

        // Recall: какая доля эталонных тем покрыта
        double recall = (double) coveredReferenceCount / referenceTopics.size();

        return switch (mode) {
            case PRECISION -> precision;
            case RECALL -> recall;
            case F1 -> 2 * precision * recall / (precision + recall);
        };
    }
}
```

### Пример

```java
package ai.qa.solutions.metrics.agent.ru;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.agent.TopicAdherenceMetric;
import ai.qa.solutions.sample.Sample;
import ai.qa.solutions.sample.message.*;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

@Slf4j
@EnableAutoConfiguration
@SpringBootTest(classes = TopicAdherenceTest.TestConfiguration.class)
class TopicAdherenceTest {

    @Configuration
    public static class TestConfiguration {
    }

    @Autowired
    private TopicAdherenceMetric topicAdherenceMetric;

    @Test
    @DisplayName("TopicAdherence: Диалог остаётся в теме")
    void testOnTopicConversation() {
        Sample sample = Sample.builder()
                .userInputMessages(List.of(
                        new HumanMessage("Хочу узнать о машинном обучении"),
                        new AIMessage("Машинное обучение - это раздел ИИ, позволяющий системам учиться на данных."),
                        new HumanMessage("Какие основные типы?"),
                        new AIMessage("Основные типы: обучение с учителем, без учителя и обучение с подкреплением.")
                ))
                .referenceTopics(List.of(
                        "машинное обучение",
                        "искусственный интеллект",
                        "обучение с учителем",
                        "обучение без учителя"
                ))
                .build();

        TopicAdherenceMetric.TopicAdherenceConfig config =
                TopicAdherenceMetric.TopicAdherenceConfig.builder()
                        .mode(TopicAdherenceMetric.Mode.F1)
                        .build();

        Double score = topicAdherenceMetric.multiTurnScore(config, sample);

        log.info("Оценка соблюдения темы: {}", score);
        assertTrue(score >= 0.7, "Ожидается высокая оценка для релевантного диалога");
    }

    @Test
    @DisplayName("TopicAdherence: Диалог уходит от темы")
    void testOffTopicConversation() {
        Sample sample = Sample.builder()
                .userInputMessages(List.of(
                        new HumanMessage("Расскажите о программировании на Python"),
                        new AIMessage("Python - это язык программирования. Кстати, вы знаете о последнем футбольном матче?"),
                        new HumanMessage("Что там с футболом?"),
                        new AIMessage("Финал Чемпионата мира был потрясающим! Счёт 3-2.")
                ))
                .referenceTopics(List.of(
                        "программирование на Python",
                        "программирование",
                        "разработка ПО"
                ))
                .build();

        TopicAdherenceMetric.TopicAdherenceConfig config =
                TopicAdherenceMetric.TopicAdherenceConfig.builder()
                        .mode(TopicAdherenceMetric.Mode.PRECISION)
                        .build();

        Double score = topicAdherenceMetric.multiTurnScore(config, sample);

        log.info("Оценка соблюдения темы: {}", score);
        assertTrue(score <= 0.6, "Ожидается низкая оценка при уходе от темы");
    }
}
```

### Конфигурация

| Параметр |     Тип      | Обязательный | По умолчанию |             Описание             |
|----------|--------------|--------------|--------------|----------------------------------|
| `mode`   | Mode         | Нет          | F1           | F1, PRECISION или RECALL режим   |
| `models` | List<String> | Нет          | все          | Конкретные ID моделей для оценки |

**Режимы оценки:**

- **F1**: Гармоническое среднее precision и recall (сбалансированный)
- **PRECISION**: Фокус на избежании нерелевантных обсуждений
- **RECALL**: Фокус на покрытии всех эталонных тем

### Когда использовать

- Чат-боты службы поддержки (обеспечение релевантных ответов)
- Образовательные чат-боты (соблюдение учебной программы)
- Модерируемые диалоги (соблюдение правил по темам)
- Системы модерации контента

---

## Выбор метрики

|           Сценарий           |      Метрика      |                      Причина                      |
|------------------------------|-------------------|---------------------------------------------------|
| Оценка выполнения задач      | AgentGoalAccuracy | Измеряет достижение цели пользователя             |
| Валидация function calling   | ToolCallAccuracy  | Проверяет корректность использования инструментов |
| Релевантность диалога        | TopicAdherence    | Гарантирует соблюдение разрешённых тем            |
| Сквозное тестирование агента | Все три           | Комплексная оценка качества агента                |

---

## Схема Sample

Метрики агентов используют класс `Sample` с типизированными классами сообщений:

### Типы сообщений

|      Тип       |             Описание             |          Поля          |
|----------------|----------------------------------|------------------------|
| `HumanMessage` | Сообщение пользователя           | `content`              |
| `AIMessage`    | Ответ ассистента                 | `content`, `toolCalls` |
| `ToolMessage`  | Результат выполнения инструмента | `content`              |
| `ToolCall`     | Вызов инструмента                | `name`, `arguments`    |

### Пример

```java
import ai.qa.solutions.sample.message.*;

Sample sample = Sample.builder()
        // Многоэтапный диалог с типизированными сообщениями
        .userInputMessages(List.of(
                new HumanMessage("Забронируй рейс в Нью-Йорк"),
                new AIMessage("Ищу рейсы...", List.of(
                        new ToolCall("search_flights", Map.of("destination", "NYC"))
                )),
                new ToolMessage("Найдено 5 рейсов"),
                new AIMessage("Я нашёл 5 вариантов. Рейс UA123 вылетает в 9:00.")
        ))
        // Для AgentGoalAccuracy (режим WITH_REFERENCE)
        .reference("Рейс в Нью-Йорк забронирован")
        // Для TopicAdherence
        .referenceTopics(List.of("бронирование рейсов", "путешествия"))
        // Для ToolCallAccuracy
        .toolCalls(List.of(
                new Sample.ToolCall("search_flights", Map.of("destination", "NYC"))
        ))
        .referenceToolCalls(List.of(
                new Sample.ToolCall("search_flights", Map.of("destination", "NYC"))
        ))
        .build();

// Использование multi-turn API
Double score = agentGoalAccuracy.multiTurnScore(config, sample);
```

### Поля Sample

|         Поле         |        Тип        |           Требуется для            |
|----------------------|-------------------|------------------------------------|
| `userInputMessages`  | List<BaseMessage> | AgentGoalAccuracy, TopicAdherence  |
| `reference`          | String            | AgentGoalAccuracy (WITH_REFERENCE) |
| `referenceTopics`    | List<String>      | TopicAdherence                     |
| `toolCalls`          | List<ToolCall>    | ToolCallAccuracy                   |
| `referenceToolCalls` | List<ToolCall>    | ToolCallAccuracy                   |

