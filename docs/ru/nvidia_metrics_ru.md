# Метрики в стиле NVIDIA

Метрики в стиле NVIDIA обеспечивают упрощённую оценку RAG-систем с использованием шкалы 0-2,
которая нормализуется в диапазон 0-1. Эти метрики разработаны для практического использования
в продакшене с понятными и интерпретируемыми рубриками оценки.

## Обзор

|       Метрика        |                  Оценивает                  |
|----------------------|---------------------------------------------|
| ContextRelevance     | Релевантны ли извлечённые контексты вопросу |
| ResponseGroundedness | Основан ли ответ на извлечённых контекстах  |
| AnswerAccuracy       | Соответствует ли ответ эталонному ответу    |

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
    # Модели для мультимодельной оценки
    chat-models:
      default-options:
        temperature: 0.1
        max-tokens: 1000
        top-p: 1.0
      list:
        - { id: anthropic/claude-4.5-sonnet }
        - { id: google/gemini-2.5-flash }
        - { id: openai/gpt-4o-mini }
  threads:
    virtual:
      enabled: true
```

---

## ContextRelevance

ContextRelevance оценивает, релевантны ли извлечённые контексты вопросу пользователя.
Каждый фрагмент контекста оценивается отдельно, затем оценки усредняются.

### Принцип работы

**Шкала оценки (0-2, нормализуется в 0-1):**

| Оценка |        Уровень        |                       Описание                        |
|--------|-----------------------|-------------------------------------------------------|
| 0      | Нерелевантный         | Контекст не содержит информации для ответа на вопрос  |
| 1      | Частично релевантный  | Контекст содержит некоторую релевантную информацию    |
| 2      | Полностью релевантный | Контекст содержит исчерпывающую информацию для ответа |

```java
// Из ContextRelevanceMetric.java - оценка релевантности
class Example {
    void evaluateRelevance(String userInput, List<String> contexts) {
        List<Double> contextScores = new ArrayList<>();

        for (String context : contexts) {
            // LLM оценивает каждый контекст относительно вопроса
            // Оценка: 0, 1 или 2 -> нормализуется в 0.0, 0.5 или 1.0
            double normalizedScore = llmScore / 2.0;
            contextScores.add(normalizedScore);
        }

        // Усреднение по всем контекстам
        return contextScores.stream().average().orElse(0.0);
    }
}
```

### Пример

```java
package ai.qa.solutions.metrics.nvidia.ru;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.nvidia.ContextRelevanceMetric;
import ai.qa.solutions.sample.Sample;
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
@SpringBootTest(classes = ContextRelevanceTest.TestConfiguration.class)
class ContextRelevanceTest {

    @Configuration
    public static class TestConfiguration {
    }

    @Autowired
    private ContextRelevanceMetric contextRelevanceMetric;

    @Test
    @DisplayName("ContextRelevance: Высоко релевантные контексты")
    void testHighlyRelevantContexts() {
        Sample sample = Sample.builder()
                .userInput("Что такое фотосинтез?")
                .retrievedContexts(List.of(
                        "Фотосинтез - это процесс, при котором растения преобразуют световую энергию в химическую.",
                        "В процессе фотосинтеза растения используют солнечный свет, углекислый газ и воду для производства глюкозы и кислорода.",
                        "Хлоропласт - это органелла, в которой происходит фотосинтез в растительных клетках."
                ))
                .build();

        ContextRelevanceMetric.ContextRelevanceConfig config =
                ContextRelevanceMetric.ContextRelevanceConfig.builder()
                        .build();

        Double score = contextRelevanceMetric.singleTurnScore(config, sample);

        log.info("Оценка релевантности контекста: {}", score);
        assertTrue(score >= 0.8, "Ожидается высокая оценка для релевантных контекстов");
    }

    @Test
    @DisplayName("ContextRelevance: Смешанная релевантность контекстов")
    void testMixedRelevanceContexts() {
        Sample sample = Sample.builder()
                .userInput("Какая столица Франции?")
                .retrievedContexts(List.of(
                        "Париж - столица и крупнейший город Франции.",
                        "Эйфелева башня находится в Париже и является знаменитой достопримечательностью.",
                        "Погода в Антарктиде экстремально холодная, температура опускается ниже -60°C."
                ))
                .build();

        ContextRelevanceMetric.ContextRelevanceConfig config =
                ContextRelevanceMetric.ContextRelevanceConfig.builder()
                        .build();

        Double score = contextRelevanceMetric.singleTurnScore(config, sample);

        log.info("Оценка релевантности контекста: {}", score);
        assertTrue(score >= 0.4 && score <= 0.8, "Ожидается умеренная оценка для смешанной релевантности");
    }
}
```

### Конфигурация

|   Параметр    |     Тип      | Обязательный | По умолчанию |             Описание             |
|---------------|--------------|--------------|--------------|----------------------------------|
| `models`      | List<String> | Нет          | все          | Конкретные ID моделей для оценки |
| `temperature` | double       | Нет          | 0.1          | Температура LLM для детерминизма |

### Когда использовать

- Оценка качества поиска в RAG
- Оценка релевантности поиска
- Оптимизация извлечения документов
- Оценка качества фильтрации контекста

---

## ResponseGroundedness

ResponseGroundedness оценивает, основан ли (подтверждён ли) ответ AI на извлечённых контекстах.
Это помогает обнаружить галлюцинации, когда модель генерирует информацию, отсутствующую в контексте.

### Принцип работы

**Шкала оценки (0-2, нормализуется в 0-1):**

| Оценка |       Уровень       |                        Описание                         |
|--------|---------------------|---------------------------------------------------------|
| 0      | Не обоснован        | Ответ содержит значительную информацию не из контекстов |
| 1      | Частично обоснован  | Ответ частично подтверждён контекстами                  |
| 2      | Полностью обоснован | Ответ полностью подтверждён контекстами                 |

**Эвристические сокращения (при включении):**

- Пустой ответ → оценка 0.0
- Ответ точно совпадает с контекстом → оценка 1.0

```java
// Из ResponseGroundednessMetric.java - оценка обоснованности
class Example {
    Double evaluateGroundedness(String response, List<String> contexts, boolean useHeuristics) {
        // Эвристические сокращения
        if (useHeuristics) {
            if (response.isEmpty()) return 0.0;
            if (contextContainsExactResponse(contexts, response)) return 1.0;
        }

        // Оценка LLM
        String combinedContext = String.join("\n\n", contexts);
        // LLM оценивает обоснованность: 0, 1 или 2
        return llmScore / 2.0;
    }
}
```

### Пример

```java
package ai.qa.solutions.metrics.nvidia.ru;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.nvidia.ResponseGroundednessMetric;
import ai.qa.solutions.sample.Sample;
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
@SpringBootTest(classes = ResponseGroundednessTest.TestConfiguration.class)
class ResponseGroundednessTest {

    @Configuration
    public static class TestConfiguration {
    }

    @Autowired
    private ResponseGroundednessMetric responseGroundednessMetric;

    @Test
    @DisplayName("ResponseGroundedness: Полностью обоснованный ответ")
    void testFullyGroundedResponse() {
        Sample sample = Sample.builder()
                .response("Эйфелева башня находится в Париже и была завершена в 1889 году.")
                .retrievedContexts(List.of(
                        "Эйфелева башня - это решётчатая башня из кованого железа на Марсовом поле в Париже, Франция.",
                        "Строительство Эйфелевой башни было завершено 31 марта 1889 года."
                ))
                .build();

        ResponseGroundednessMetric.ResponseGroundednessConfig config =
                ResponseGroundednessMetric.ResponseGroundednessConfig.builder()
                        .useHeuristicShortcuts(true)
                        .build();

        Double score = responseGroundednessMetric.singleTurnScore(config, sample);

        log.info("Оценка обоснованности: {}", score);
        assertTrue(score >= 0.9, "Ожидается высокая оценка для обоснованного ответа");
    }

    @Test
    @DisplayName("ResponseGroundedness: Галлюцинированный ответ")
    void testHallucinatedResponse() {
        Sample sample = Sample.builder()
                .response("Эйфелева башня - самое высокое здание в мире высотой 1000 метров, построенное Леонардо да Винчи.")
                .retrievedContexts(List.of(
                        "Эйфелева башня - это решётчатая башня из кованого железа в Париже, Франция.",
                        "Высота башни составляет 330 метров, она была спроектирована Гюставом Эйфелем."
                ))
                .build();

        ResponseGroundednessMetric.ResponseGroundednessConfig config =
                ResponseGroundednessMetric.ResponseGroundednessConfig.builder()
                        .useHeuristicShortcuts(true)
                        .build();

        Double score = responseGroundednessMetric.singleTurnScore(config, sample);

        log.info("Оценка обоснованности: {}", score);
        assertTrue(score <= 0.5, "Ожидается низкая оценка для галлюцинированного контента");
    }

    @Test
    @DisplayName("ResponseGroundedness: Частично обоснованный ответ")
    void testPartiallyGroundedResponse() {
        Sample sample = Sample.builder()
                .response("Париж - столица Франции. В нём проживает более 12 миллионов человек.")
                .retrievedContexts(List.of(
                        "Париж - столица и самый населённый город Франции."
                        // Примечание: конкретная численность населения в контексте отсутствует
                ))
                .build();

        ResponseGroundednessMetric.ResponseGroundednessConfig config =
                ResponseGroundednessMetric.ResponseGroundednessConfig.builder()
                        .build();

        Double score = responseGroundednessMetric.singleTurnScore(config, sample);

        log.info("Оценка обоснованности: {}", score);
        assertTrue(score >= 0.3 && score <= 0.7, "Ожидается умеренная оценка для частичного обоснования");
    }
}
```

### Конфигурация

|        Параметр         |     Тип      | Обязательный | По умолчанию |                   Описание                    |
|-------------------------|--------------|--------------|--------------|-----------------------------------------------|
| `models`                | List<String> | Нет          | все          | Конкретные ID моделей для оценки              |
| `useHeuristicShortcuts` | boolean      | Нет          | true         | Включить быстрый путь для тривиальных случаев |
| `temperature`           | double       | Нет          | 0.1          | Температура LLM для детерминизма              |

### Когда использовать

- Обнаружение галлюцинаций в RAG-системах
- Контроль качества ответов
- Проверка фактов в сгенерированном контенте
- Мониторинг RAG в продакшене

---

## AnswerAccuracy

AnswerAccuracy оценивает, точно ли ответ AI соответствует эталонному ответу.
Поддерживает опциональный режим двойного судьи для повышения надёжности.

### Принцип работы

**Шкала оценки (0-2, нормализуется в 0-1):**

| Оценка |     Уровень      |                       Описание                       |
|--------|------------------|------------------------------------------------------|
| 0      | Неверный         | Ответ фактически неверен или противоречит эталону    |
| 1      | Частично верный  | Ответ частично верен, но неполон или содержит ошибки |
| 2      | Полностью верный | Ответ точно соответствует эталонному ответу          |

**Режим двойного судьи (опционально):**

1. Первичная оценка выставляет балл ответу
2. Подтверждающая оценка проверяет и может скорректировать балл

```java
// Из AnswerAccuracyMetric.java - оценка с двойным судьёй
class Example {
    Double evaluateAccuracy(String response, String reference, boolean useDualJudge) {
        // Первичная оценка
        int initialScore = llm.evaluate(response, reference);

        if (useDualJudge) {
            // Подтверждающая оценка проверяет первичную
            int confirmedScore = llm.confirm(response, reference, initialScore);
            return confirmedScore / 2.0;
        }

        return initialScore / 2.0;
    }
}
```

### Пример

```java
package ai.qa.solutions.metrics.nvidia.ru;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.nvidia.AnswerAccuracyMetric;
import ai.qa.solutions.sample.Sample;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

@Slf4j
@EnableAutoConfiguration
@SpringBootTest(classes = AnswerAccuracyTest.TestConfiguration.class)
class AnswerAccuracyTest {

    @Configuration
    public static class TestConfiguration {
    }

    @Autowired
    private AnswerAccuracyMetric answerAccuracyMetric;

    @Test
    @DisplayName("AnswerAccuracy: Правильный ответ")
    void testCorrectAnswer() {
        Sample sample = Sample.builder()
                .response("Столица Франции - Париж.")
                .reference("Париж является столицей Франции.")
                .build();

        AnswerAccuracyMetric.AnswerAccuracyConfig config =
                AnswerAccuracyMetric.AnswerAccuracyConfig.builder()
                        .useDualJudge(false)
                        .build();

        Double score = answerAccuracyMetric.singleTurnScore(config, sample);

        log.info("Оценка точности ответа: {}", score);
        assertTrue(score >= 0.9, "Ожидается высокая оценка для правильного ответа");
    }

    @Test
    @DisplayName("AnswerAccuracy: Неправильный ответ")
    void testIncorrectAnswer() {
        Sample sample = Sample.builder()
                .response("Столица Франции - Лондон.")
                .reference("Париж является столицей Франции.")
                .build();

        AnswerAccuracyMetric.AnswerAccuracyConfig config =
                AnswerAccuracyMetric.AnswerAccuracyConfig.builder()
                        .useDualJudge(false)
                        .build();

        Double score = answerAccuracyMetric.singleTurnScore(config, sample);

        log.info("Оценка точности ответа: {}", score);
        assertTrue(score <= 0.2, "Ожидается низкая оценка для неправильного ответа");
    }

    @Test
    @DisplayName("AnswerAccuracy: Режим двойного судьи для повышения надёжности")
    void testDualJudgeMode() {
        Sample sample = Sample.builder()
                .response("Вода закипает при 100 градусах Цельсия при стандартном атмосферном давлении.")
                .reference("При стандартном атмосферном давлении (1 атм) вода закипает при 100°C или 212°F.")
                .build();

        AnswerAccuracyMetric.AnswerAccuracyConfig config =
                AnswerAccuracyMetric.AnswerAccuracyConfig.builder()
                        .useDualJudge(true)  // Включить подтверждающую оценку
                        .build();

        Double score = answerAccuracyMetric.singleTurnScore(config, sample);

        log.info("Оценка точности ответа (двойной судья): {}", score);
        assertTrue(score >= 0.8, "Ожидается высокая оценка для фактически верного ответа");
    }

    @Test
    @DisplayName("AnswerAccuracy: Частично правильный ответ")
    void testPartiallyCorrectAnswer() {
        Sample sample = Sample.builder()
                .response("Эйнштейн разработал теорию относительности.")
                .reference("Альберт Эйнштейн разработал общую теорию относительности в 1915 году и специальную теорию относительности в 1905 году.")
                .build();

        AnswerAccuracyMetric.AnswerAccuracyConfig config =
                AnswerAccuracyMetric.AnswerAccuracyConfig.builder()
                        .build();

        Double score = answerAccuracyMetric.singleTurnScore(config, sample);

        log.info("Оценка точности ответа: {}", score);
        assertTrue(score >= 0.4 && score <= 0.8, "Ожидается умеренная оценка для частичного ответа");
    }
}
```

### Конфигурация

|    Параметр    |     Тип      | Обязательный | По умолчанию |             Описание             |
|----------------|--------------|--------------|--------------|----------------------------------|
| `models`       | List<String> | Нет          | все          | Конкретные ID моделей для оценки |
| `useDualJudge` | boolean      | Нет          | false        | Включить подтверждающую оценку   |
| `temperature`  | double       | Нет          | 0.1          | Температура LLM для детерминизма |

### Когда использовать

- Оценка систем вопрос-ответ
- Проверка фактической точности
- Тестирование извлечения знаний
- Высокоответственная валидация ответов (использовать dual-judge)

---

## Оценка RAG-пайплайна

Эти три метрики вместе обеспечивают комплексную оценку RAG-системы:

```
Вопрос пользователя → [Поиск] → Извлечённые контексты → [Генерация] → Ответ
                         ↓                                    ↓
                  ContextRelevance          ResponseGroundedness + AnswerAccuracy
```

### Полный пример оценки RAG

```java
class Example {
    void evaluateRAGPipeline(Sample sample) {
        // 1. Оценка качества поиска
        Double contextRelevance = contextRelevanceMetric.singleTurnScore(sample);

        // 2. Оценка обоснованности ответа
        Double groundedness = responseGroundednessMetric.singleTurnScore(sample);

        // 3. Оценка точности ответа
        Double accuracy = answerAccuracyMetric.singleTurnScore(sample);

        // Комбинированная оценка (пример весов)
        Double ragScore = 0.3 * contextRelevance + 0.3 * groundedness + 0.4 * accuracy;

        log.info("Оценка RAG-пайплайна: {} (релевантность={}, обоснованность={}, точность={})",
                ragScore, contextRelevance, groundedness, accuracy);
    }
}
```

---

## Выбор метрики

|       Цель оценки        |       Метрика        |
|--------------------------|----------------------|
| Качество поиска          | ContextRelevance     |
| Обнаружение галлюцинаций | ResponseGroundedness |
| Корректность ответа      | AnswerAccuracy       |
| Сквозное качество RAG    | Все три вместе       |

---

## Схема Sample

|        Поле         |     Тип      |         Требуется для          |
|---------------------|--------------|--------------------------------|
| `userInput`         | String       | ContextRelevance               |
| `retrievedContexts` | List<String> | ContextRelevance, Groundedness |
| `response`          | String       | Groundedness, AnswerAccuracy   |
| `reference`         | String       | AnswerAccuracy                 |

```java
class Example {
    void createSample() {
        Sample sample = Sample.builder()
                .userInput("Вопрос пользователя")
                .retrievedContexts(List.of("контекст1", "контекст2"))
                .response("Сгенерированный AI ответ")
                .reference("Эталонный ответ")
                .build();
    }
}
```

