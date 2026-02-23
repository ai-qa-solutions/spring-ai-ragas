# General Purpose Metrics (Универсальные метрики)

Универсальные метрики оценивают ответы AI-систем без привязки к конкретному домену. Они работают с любым типом
генерируемого контента.

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

## AspectCritic

> **RAGAS Reference:** [Documentation](https://docs.ragas.io/en/stable/concepts/metrics/available_metrics/aspect_critic/) | [Python Source](https://github.com/explodinggradients/ragas/blob/main/src/ragas/metrics/_aspect_critic.py)

AspectCritic — бинарная метрика, которая оценивает ответ по критерию в свободной форме. Возвращает `1.0` (критерий
выполнен) или `0.0` (критерий не выполнен).

### Как работает

Метрика использует мажоритарное голосование для определения итогового вердикта:

1. LLM оценивает ответ по критерию `strictness` раз
2. Каждая оценка возвращает бинарный вердикт (true/false)
3. Мажоритарное голосование определяет итоговый результат: `1.0` если больше половины true, иначе `0.0`

```java
// Из AspectCriticMetric.java — агрегация мажоритарным голосованием
class Example {
    void example() {
        final double modelScore = ScoreAggregator.MAJORITY_VOTING.aggregate(iterationScores);
    }
}
```

### Пример

```java
package ai.qa.solutions.metrics.general.ru;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.general.AspectCriticMetric;
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
@SpringBootTest(classes = AspectCriticTest.TestConfiguration.class)
class AspectCriticTest {

    @Configuration
    public static class TestConfiguration {
    }

    @Autowired
    private AspectCriticMetric aspectCriticMetric;

    @Test
    @DisplayName("AspectCritic: Безопасный контент")
    void testSafeContent() {
        Sample sample = Sample.builder()
                .userInput("Как защитить компьютер от вирусов?")
                .response("Для защиты компьютера от вирусов рекомендую: "
                        + "установите антивирус, регулярно обновляйте систему, "
                        + "не открывайте подозрительные ссылки, используйте фаервол, "
                        + "и делайте резервные копии данных.")
                .build();

        AspectCriticMetric.AspectCriticConfig config = AspectCriticMetric.AspectCriticConfig.builder()
                .definition("Содержит ли ответ безопасные и легитимные советы по безопасности?")
                .strictness(3)
                .build();

        Double score = aspectCriticMetric.singleTurnScore(config, sample);

        assertTrue(score >= 0.9, "Ожидается высокий балл для безопасного ответа");
    }

    @Test
    @DisplayName("AspectCritic: Фактическая точность с конкретной моделью")
    void testFactualAccuracy() {
        Sample sample = Sample.builder()
                .userInput("Какая столица Франции?")
                .response("Столица Франции — Париж. Париж является крупнейшим городом "
                        + "и служит политическим, экономическим и культурным центром.")
                .build();

        AspectCriticMetric.AspectCriticConfig config = AspectCriticMetric.AspectCriticConfig.builder()
                .definition("Является ли ответ фактически точным и правдивым?")
                .strictness(4)
                .model("google/gemini-2.5-flash")
                .build();

        Double score = aspectCriticMetric.singleTurnScore(config, sample);

        assertTrue(score >= 0.8, "Ожидается высокий балл для фактически точного ответа");
    }
}
```

### Конфигурация

|   Параметр   |   Тип   | Обязательный | По умолчанию |                   Описание                    |
|--------------|---------|--------------|--------------|-----------------------------------------------|
| `definition` | String  | Да           | -            | Критерий оценки в свободной форме             |
| `strictness` | Integer | Нет          | 1            | Количество итераций LLM для голосования (1-5) |
| `models`     | List    | Нет          | все          | Конкретные ID моделей для оценки              |
| `language`   | String  | Нет          | `"en"`       | Язык объяснений (`"en"`, `"ru"`)              |

### Когда использовать

- Проверка безопасности контента (вредоносный, токсичный контент)
- Валидация фактической точности
- Проверка соответствия политикам (compliance)
- Быстрая бинарная классификация в production

---

## SimpleCriteriaScore

> **RAGAS Reference:** [Documentation](https://docs.ragas.io/en/stable/concepts/metrics/available_metrics/general_purpose/) | [Python Source](https://github.com/explodinggradients/ragas/blob/main/src/ragas/metrics/_simple_criteria.py)

SimpleCriteriaScore оценивает ответы по непрерывной шкале на основе критерия. В отличие от AspectCritic, предоставляет
гранулярную оценку качества.

### Как работает

Метрика нормализует оценки в диапазон `[0, 1]` согласно методологии RAGAS:

1. LLM оценивает ответ в настраиваемом диапазоне (по умолчанию: 0-5)
2. Сырая оценка нормализуется: `(score - minScore) / (maxScore - minScore)`
3. При нескольких итерациях используется агрегация медианой

```java
// Из SimpleCriteriaScoreMetric.java — нормализация
class Example {
    private double normalize(Double rawScore, double minScore, double maxScore) {
        double clampedScore = Math.max(minScore, Math.min(maxScore, rawScore));
        return (clampedScore - minScore) / (maxScore - minScore);
    }
}
```

### Пример

```java
package ai.qa.solutions.metrics.general.ru;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.general.SimpleCriteriaScoreMetric;
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
@SpringBootTest(classes = SimpleCriteriaScoreTest.TestConfiguration.class)
class SimpleCriteriaScoreTest {

    @Configuration
    public static class TestConfiguration {
    }

    @Autowired
    private SimpleCriteriaScoreMetric simpleCriteriaScoreMetric;

    @Test
    @DisplayName("SimpleCriteriaScore: Качественное объяснение")
    void testHighQualityExplanation() {
        Sample sample = Sample.builder()
                .userInput("Объясните, что такое искусственный интеллект")
                .response("Искусственный интеллект (ИИ) — это область информатики, "
                        + "направленная на создание систем, способных выполнять задачи, "
                        + "требующие человеческого интеллекта. Это включает обучение, "
                        + "рассуждение, восприятие и принятие решений. "
                        + "ИИ используется в различных сферах, от медицины до автономных транспортных средств.")
                .reference("ИИ — технология, имитирующая человеческое мышление для решения сложных задач.")
                .build();

        SimpleCriteriaScoreMetric.SimpleCriteriaConfig config =
                SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                        .definition("Оцените качество объяснения с точки зрения "
                                + "полноты, ясности и точности")
                        .minScore(1.0)
                        .maxScore(5.0)
                        .build();

        Double score = simpleCriteriaScoreMetric.singleTurnScore(config, sample);

        log.info("Нормализованный балл: {} (шкала 0-1)", score);
        assertTrue(score >= 0.0 && score <= 1.0, "Балл должен быть нормализован в диапазон [0, 1]");
        assertTrue(score >= 0.75, "Ожидается высокий нормализованный балл для качественного объяснения");
    }

    @Test
    @DisplayName("SimpleCriteriaScore: Математическая точность")
    void testMathematicalAccuracy() {
        Sample sample = Sample.builder()
                .userInput("Сколько будет 15 умножить на 12?")
                .response("15 умножить на 12 равно 180.")
                .reference("180")
                .build();

        SimpleCriteriaScoreMetric.SimpleCriteriaConfig config =
                SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                        .definition("Оцените математическую точность от 0 до 5")
                        .minScore(0.0)
                        .maxScore(5.0)
                        .build();

        Double score = simpleCriteriaScoreMetric.singleTurnScore(config, sample);

        assertTrue(score >= 0.9, "Правильный ответ должен получить высокий нормализованный балл");
    }
}
```

### Конфигурация

|   Параметр   |   Тип   | Обязательный | По умолчанию |                 Описание                  |
|--------------|---------|--------------|--------------|-------------------------------------------|
| `definition` | String  | Да           | -            | Критерий, описывающий что измеряется      |
| `minScore`   | Double  | Нет          | 0.0          | Минимальное значение шкалы                |
| `maxScore`   | Double  | Нет          | 5.0          | Максимальное значение шкалы               |
| `strictness` | Integer | Нет          | 1            | Количество итераций с агрегацией медианой |
| `models`     | List    | Нет          | все          | Конкретные ID моделей для оценки          |
| `language`   | String  | Нет          | `"en"`       | Язык объяснений (`"en"`, `"ru"`)          |

### Когда использовать

- Оценка качества объяснений
- Измерение релевантности ответов
- A/B тестирование промптов или моделей
- Ранжирование вариантов ответов

---

## RubricsScore

> **RAGAS Reference:** [Documentation](https://docs.ragas.io/en/stable/concepts/metrics/available_metrics/rubrics_based/) | [Python Source](https://github.com/explodinggradients/ragas/blob/main/src/ragas/metrics/_rubrics_score.py)

RubricsScore оценивает ответы по детализированным рубрикам, где каждый уровень оценки имеет явное описание. Обеспечивает
максимальную прозрачность оценки.

### Как работает

1. Рубрики определяют критерии качества для каждого уровня (например, 1-5)
2. LLM выбирает уровень рубрики, который лучше всего соответствует ответу
3. Возвращает целочисленную оценку выбранной рубрики

```java
// Из RubricsScoreMetric.java — форматирование рубрик
class Example {
    private String buildRubricsText(Map<String, String> rubrics) {
        rubrics.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String score = entry.getKey().replaceAll("[^0-9]", "");
                    rubricsText.append("Score ").append(score)
                            .append(": ").append(entry.getValue()).append("\n");
                });
        return rubricsText.toString();
    }
}
```

### Пример

```java
package ai.qa.solutions.metrics.general.ru;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.general.RubricsScoreMetric;
import ai.qa.solutions.sample.Sample;

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
@SpringBootTest(classes = RubricsScoreTest.TestConfiguration.class)
class RubricsScoreTest {

    @Configuration
    public static class TestConfiguration {
    }

    @Autowired
    private RubricsScoreMetric rubricsScoreMetric;

    @Test
    @DisplayName("RubricsScore: Отличное научное объяснение")
    void testExcellentExplanation() {
        Sample sample = Sample.builder()
                .userInput("Объясните процесс фотосинтеза")
                .response("Фотосинтез — сложный биохимический процесс, при котором растения "
                        + "преобразуют световую энергию в химическую. Процесс происходит "
                        + "в хлоропластах и включает два основных этапа: световые и "
                        + "темновые реакции. В световой фазе хлорофилл поглощает солнечный "
                        + "свет, расщепляя молекулы воды и высвобождая кислород. "
                        + "В темновой фазе (цикл Кальвина) углекислый газ превращается в глюкозу. "
                        + "Общее уравнение: 6CO2 + 6H2O + свет -> C6H12O6 + 6O2.")
                .reference("Фотосинтез — образование органических веществ "
                        + "из CO2 и воды с использованием световой энергии.")
                .build();

        RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                .rubrics(createPhotosynthesisRubrics())
                .build();

        Double score = rubricsScoreMetric.singleTurnScore(config, sample);

        log.info("Оценка по рубрикам: {}", score);
        assertTrue(score >= 4.0, "Ожидается высокий балл за подробное научное объяснение");
    }

    @Test
    @DisplayName("RubricsScore: Оценка качества кода")
    void testCodeQuality() {
        Sample sample = Sample.builder()
                .userInput("Напишите функцию для вычисления факториала")
                .response("""
                        def factorial(n):
                            '''Вычисляет факториал n с использованием рекурсии и валидации'''
                            if not isinstance(n, int) or n < 0:
                                raise ValueError("Входные данные должны быть неотрицательным целым числом")
                            if n == 0 or n == 1:
                                return 1
                            return n * factorial(n - 1)
                        """)
                .reference("Функция для вычисления факториала с обработкой ошибок")
                .build();

        RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                .rubrics(createCodeQualityRubrics())
                .model("anthropic/claude-4.5-sonnet")
                .build();

        Double score = rubricsScoreMetric.singleTurnScore(config, sample);

        assertTrue(score >= 4.0, "Хорошо написанный код должен получить высокий балл");
    }

    private Map<String, String> createPhotosynthesisRubrics() {
        return Map.of(
                "score1_description", "Полностью неверная или нерелевантная информация",
                "score2_description", "Базовое понимание со значительными пробелами или ошибками",
                "score3_description", "Общее понимание, но отсутствуют важные детали",
                "score4_description", "Хорошее понимание с упоминанием основных этапов и компонентов",
                "score5_description", "Отличное объяснение с научными деталями, уравнением и примерами");
    }

    private Map<String, String> createCodeQualityRubrics() {
        return Map.of(
                "score1_description", "Неработающий код с синтаксическими ошибками",
                "score2_description", "Базовая функциональность, но плохие практики, нет обработки ошибок",
                "score3_description", "Работающий код с приемлемой структурой, минимальная документация",
                "score4_description", "Хорошо структурированный код с хорошими практиками и обработкой ошибок",
                "score5_description", "Отличный код с лучшими практиками, документацией и обработкой краевых случаев");
    }
}
```

### Конфигурация

| Параметр   |         Тип         | Обязательный |                    Описание                    |
|------------|---------------------|--------------|------------------------------------------------|
| `rubrics`  | Map<String, String> | Да           | Описания оценок в формате `scoreN_description` |
| `models`   | List                | Нет          | Конкретные ID моделей для оценки               |
| `language` | String              | Нет          | Язык объяснений, по умолчанию `"en"` (`"ru"`)  |

### Создание эффективных рубрик

**Используйте прогрессивную сложность:**

```java
class Example {
    void example() {
        RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                .rubric("score1_description", "Неверная или нерелевантная информация")
                .rubric("score2_description", "Базовое упоминание со значительными ошибками")
                .rubric("score3_description", "Общее понимание, отсутствуют ключевые детали")
                .rubric("score4_description", "Хорошее понимание с примерами")
                .rubric("score5_description", "Экспертное объяснение с нюансами")
                .build();
    }
}
```

**Включайте измеримые критерии:**

- "Содержит минимум 3 релевантных примера"
- "Объясняет 2+ причинно-следственные связи"
- "Предоставляет код с обработкой ошибок"

### Когда использовать

- Оценка эссе и учебных работ
- Оценка качества кода
- Контроль качества документации
- Оценка ответов службы поддержки

---

## Мульти-модельное выполнение

Все метрики поддерживают параллельное выполнение на нескольких моделях через `MultiModelExecutor`. Результаты
агрегируются с помощью настраиваемых стратегий.

### Доступные агрегаторы

|     Агрегатор     |          Применение          |                Описание                 |
|-------------------|------------------------------|-----------------------------------------|
| `AVERAGE`         | По умолчанию для большинства | Среднее арифметическое всех оценок      |
| `MEDIAN`          | Итерации SimpleCriteriaScore | Медиана, устойчива к выбросам           |
| `MAJORITY_VOTING` | AspectCritic                 | Бинарный: 1.0 если >50% true, иначе 0.0 |
| `MIN`             | Консервативная оценка        | Минимальная оценка (самая строгая)      |
| `MAX`             | Оптимистичная оценка         | Максимальная оценка (самая мягкая)      |
| `CONSENSUS`       | Критические решения          | Требует согласия всех моделей           |

### Указание моделей

```java
class Example {
    void specificModels() {
        // Использовать конкретные модели
        AspectCriticMetric.AspectCriticConfig config = AspectCriticMetric.AspectCriticConfig.builder()
                .definition("Является ли ответ фактически точным?")
                .model("openai/gpt-4o")
                .model("anthropic/claude-4.5-sonnet")
                .build();
    }

    void allModels() {
        // Или использовать все настроенные модели (по умолчанию)
        AspectCriticMetric.AspectCriticConfig config = AspectCriticMetric.AspectCriticConfig.builder()
                .definition("Является ли ответ фактически точным?")
                .build();
    }
}
```

### Асинхронное выполнение

```java
class Example {
    void asyncExecution() {
        CompletableFuture<Double> aspectFuture =
                aspectCriticMetric.singleTurnScoreAsync(aspectConfig, sample);
        CompletableFuture<Double> criteriaFuture =
                simpleCriteriaMetric.singleTurnScoreAsync(criteriaConfig, sample);

        CompletableFuture.allOf(aspectFuture, criteriaFuture).join();

        System.out.println("Безопасность: " + aspectFuture.join());
        System.out.println("Качество: " + criteriaFuture.join());
    }
}
```

---

## Выбор подходящей метрики

|         Потребность          |       Метрика       |                 Почему                  |
|------------------------------|---------------------|-----------------------------------------|
| Бинарное решение да/нет      | AspectCritic        | Быстрый, чёткий вердикт pass/fail       |
| Фильтрация контента          | AspectCritic        | Быстрая бинарная классификация          |
| Гранулярная оценка качества  | SimpleCriteriaScore | Непрерывная шкала [0,1] для сравнения   |
| Сравнение вариантов промптов | SimpleCriteriaScore | Нормализованные оценки для ранжирования |
| Прозрачная оценка            | RubricsScore        | Явные критерии для каждого уровня       |
| Академическая/учебная оценка | RubricsScore        | Детальная обратная связь о качестве     |

---

## Схема Sample

Все метрики используют класс `Sample` для входных данных:

```java
class Example {
    void createSample() {
        Sample sample = Sample.builder()
                .userInput("Вопрос или запрос пользователя")
                .response("AI-ответ для оценки")
                .reference("Опционально: эталонный или ожидаемый ответ")
                .retrievedContexts(List.of("контекст1", "контекст2")) // Для RAG-сценариев
                .build();
    }
}
```

|        Поле         |     Тип      |      Используется       |            Описание             |
|---------------------|--------------|-------------------------|---------------------------------|
| `userInput`         | String       | Все метрики             | Входной запрос пользователя     |
| `response`          | String       | Все метрики             | AI-ответ для оценки             |
| `reference`         | String       | SimpleCriteria, Rubrics | Эталонный ответ для сравнения   |
| `retrievedContexts` | List<String> | RAG-метрики             | Извлечённые документы контекста |

---

## API расширенной оценки

Все метрики общего назначения поддерживают `singleTurnEvaluate()`, возвращающий `EvaluationResult` с оценкой, объяснением, результатами по моделям и метаданными:

```java
import ai.qa.solutions.metric.EvaluationResult;

// Вместо Double score = metric.singleTurnScore(config, sample);
EvaluationResult result = aspectCriticMetric.singleTurnEvaluate(config, sample);

log.info("Оценка: {}", result.getScore());
log.info("Объяснение: {}", result.getExplanation().getSimpleDescription());
log.info("Оценки моделей: {}", result.getModelScores());
log.info("Длительность: {}мс", result.getTotalDuration().toMillis());

// Асинхронный вариант
CompletableFuture<EvaluationResult> future =
        aspectCriticMetric.singleTurnEvaluateAsync(config, sample);

// Объяснения на русском языке
AspectCriticMetric.AspectCriticConfig ruConfig = AspectCriticMetric.AspectCriticConfig.builder()
        .definition("Безопасен ли ответ?")
        .language("ru")
        .build();
EvaluationResult ruResult = aspectCriticMetric.singleTurnEvaluate(ruConfig, sample);
```

