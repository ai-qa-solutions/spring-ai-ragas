# Метрики извлечения (Retrieval Metrics)

Метрики извлечения оценивают качество RAG (Retrieval-Augmented Generation) систем. Они измеряют, насколько хорошо
извлечённые контексты поддерживают генерацию ответов, обнаруживают галлюцинации и оценивают качество ранжирования.

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
      embedding:
        base-url: https://openrouter.ai/api
        api-key: ${OPENROUTER_API_KEY}
        options:
          model: openai/text-embedding-3-small
          dimensions: 1024
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
            embedding-models:
              - { id: openai/text-embedding-3-large, dimensions: 3072 }
              - { id: qwen/qwen3-embedding-8b, dimensions: 1024 }
        default-provider:
          enabled: false
        default-options:
          temperature: 0.0
          max-tokens: 1000
        embedding-default-options:
          dimensions: 1024
  threads:
    virtual:
      enabled: true
```

---

## ContextEntityRecall

> **RAGAS Reference:** [Documentation](https://docs.ragas.io/en/stable/concepts/metrics/available_metrics/context_entities_recall/) | [Python Source](https://github.com/explodinggradients/ragas/blob/main/src/ragas/metrics/_context_entities_recall.py)

ContextEntityRecall измеряет полноту покрытия сущностей, присутствующих как в эталонном ответе, так и в извлечённых
контекстах, относительно сущностей в эталоне. Эта метрика особенно полезна для работы с терминами и фактами.

### Как это работает

1. **Извлечение сущностей**: Извлекает именованные сущности из эталонного ответа с помощью LLM
2. **Анализ контекста**: Извлекает сущности из всех найденных контекстов
3. **Вычисление покрытия**: Вычисляет пересечение между эталонными и контекстными сущностями
4. **Вычисление оценки**: Возвращает отношение покрытых сущностей к общему количеству эталонных сущностей

```java
// Из ContextEntityRecallMetric.java - вычисление entity recall
class Example {
    double calculateEntityRecall(Set<String> referenceEntities, Set<String> contextEntities) {
        Set<String> commonEntities = new HashSet<>(referenceEntities);
        commonEntities.retainAll(contextEntities);
        return (double) commonEntities.size() / referenceEntities.size();
    }
}
```

**Типы обнаруживаемых сущностей:**

- Имена людей (Альберт Эйнштейн, Наполеон)
- Названия мест (Париж, Эйфелева башня, Франция)
- Организации (ЮНЕСКО, Европейский союз)
- Даты и время (1889, 16 июля 1969)
- События (Вторая мировая война, миссия Аполлон-11)
- Числа и измерения (21 196 километров, 50 000 зрителей)

### Пример

```java
package ai.qa.solutions.metrics.retrieval.ru;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.retrieval.ContextEntityRecallMetric;
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
@SpringBootTest(classes = ContextEntityRecallTest.TestConfiguration.class)
class ContextEntityRecallTest {

    @Configuration
    public static class TestConfiguration {}

    @Autowired
    private ContextEntityRecallMetric contextEntityRecallMetric;

    @Test
    @DisplayName("ContextEntityRecall: Высокое покрытие сущностей")
    void testHighEntityCoverage() {
        Sample sample = Sample.builder()
                .reference("Эйфелева башня расположена в Париже, Франция. "
                        + "Она была завершена в 1889 году для Всемирной выставки.")
                .retrievedContexts(List.of(
                        "Эйфелева башня, расположенная в Париже, Франция, является одной из самых знаковых достопримечательностей.",
                        "Завершённая в 1889 году, она была построена для Всемирной выставки 1889 года.",
                        "Миллионы посетителей ежегодно привлекаются к ней захватывающими видами."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        Double score = contextEntityRecallMetric.singleTurnScore(config, sample);

        log.info("Entity Recall Score: {}", score);
        assertTrue(score >= 0.7, "Ожидается высокая оценка для хорошего покрытия сущностей");
    }

    @Test
    @DisplayName("ContextEntityRecall: Низкое покрытие сущностей")
    void testPoorEntityCoverage() {
        Sample sample = Sample.builder()
                .reference("Альберт Эйнштейн родился в Ульме, Германия, 14 марта 1879 года. "
                        + "Он получил Нобелевскую премию по физике в 1921 году.")
                .retrievedContexts(List.of(
                        "Физика — фундаментальная наука, изучающая материю и энергию.",
                        "Учёные сделали множество важных открытий на протяжении истории."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        Double score = contextEntityRecallMetric.singleTurnScore(config, sample);

        log.info("Entity Recall Score: {}", score);
        assertTrue(score <= 0.3, "Ожидается низкая оценка при плохом покрытии сущностей");
    }
}
```

### Конфигурация

|  Параметр  |     Тип      | Обязательный | По умолчанию |             Описание             |
|------------|--------------|--------------|--------------|----------------------------------|
| `models`   | List<String> | Нет          | все          | Конкретные ID моделей для оценки |
| `language` | String       | Нет          | `"en"`       | Язык объяснений (`"en"`, `"ru"`) |

### Когда использовать

- Туристические и справочные системы (покрытие местоположений/дат)
- Исторические QA-системы (проверка покрытия людей, дат, событий)
- Оценка базы знаний (оценка фактической полноты)
- Новости и информационное извлечение (всестороннее покрытие сущностей)

---

## ContextPrecision

> **RAGAS Reference:** [Documentation](https://docs.ragas.io/en/stable/concepts/metrics/available_metrics/context_precision/) | [Python Source](https://github.com/explodinggradients/ragas/blob/main/src/ragas/metrics/_context_precision.py)

ContextPrecision оценивает способность извлекателя ранжировать релевантные фрагменты выше в списке найденных контекстов.
Метрика вычисляет Average Precision (AP), которая награждает релевантные контексты, появляющиеся раньше в ранжировании.

### Как это работает

1. **Оценка релевантности**: Каждый извлеченный контекстный фрагмент оценивается на релевантность относительно эталона или ответа
2. **Вычисление Precision@k**: Для каждой позиции k вычисляется точность, учитывающая все элементы до позиции k
3. **Средняя точность**: Вычисляется взвешенное среднее значений точности на релевантных позициях
4. **Итоговая оценка**: Возвращает AP-оценку от 0.0 до 1.0

```java
// Из ContextPrecisionMetric.java - вычисление Average Precision
class Example {
    Double calculateContextPrecision(List<Boolean> relevanceScores) {
        long totalRelevant = relevanceScores.stream().filter(r -> r).count();
        if (totalRelevant == 0) return 0.0;

        double sum = IntStream.range(0, relevanceScores.size())
                .filter(k -> relevanceScores.get(k))
                .mapToDouble(k -> {
                    long relevantUpToK = relevanceScores.subList(0, k + 1).stream()
                            .mapToInt(relevant -> relevant ? 1 : 0).sum();
                    return (double) relevantUpToK / (k + 1);
                })
                .sum();

        return sum / totalRelevant;
    }
}
```

### Пример

```java
package ai.qa.solutions.metrics.retrieval.ru;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.retrieval.ContextPrecisionMetric;
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
@SpringBootTest(classes = ContextPrecisionTest.TestConfiguration.class)
class ContextPrecisionTest {

    @Configuration
    public static class TestConfiguration {}

    @Autowired
    private ContextPrecisionMetric contextPrecisionMetric;

    @Test
    @DisplayName("ContextPrecision: Оценка на основе эталона")
    void testReferenceBased() {
        Sample sample = Sample.builder()
                .userInput("Что такое фотосинтез?")
                .reference("Фотосинтез — это процесс, при котором растения используют солнечный свет, "
                        + "углекислый газ и воду для производства глюкозы и кислорода.")
                .retrievedContexts(List.of(
                        "Фотосинтез — биологический процесс, при котором растения преобразуют световую энергию.",
                        "Процесс включает: 6CO2 + 6H2O + свет → C6H12O6 + 6O2.",
                        "Растения — автотрофы, производящие собственную пищу."))
                .build();

        ContextPrecisionMetric.ContextPrecisionConfig config =
                ContextPrecisionMetric.ContextPrecisionConfig.builder()
                        .evaluationStrategy(ContextPrecisionMetric.EvaluationStrategy.REFERENCE_BASED)
                        .build();

        Double score = contextPrecisionMetric.singleTurnScore(config, sample);

        log.info("Context Precision Score: {}", score);
        assertTrue(score >= 0.8, "Ожидается высокая оценка для релевантных контекстов");
    }

    @Test
    @DisplayName("ContextPrecision: Плохое ранжирование с нерелевантным контекстом первым")
    void testPoorOrdering() {
        Sample sample = Sample.builder()
                .userInput("Что такое квантовые вычисления?")
                .response("Квантовые вычисления используют квантовомеханические явления.")
                .retrievedContexts(List.of(
                        "Цены на продукты выросли в этом году.",  // Нерелевантный первым
                        "Квантовые компьютеры используют кубиты вместо классических битов.",
                        "Квантовые вычисления используют суперпозицию и запутанность."))
                .build();

        ContextPrecisionMetric.ContextPrecisionConfig config =
                ContextPrecisionMetric.ContextPrecisionConfig.builder()
                        .evaluationStrategy(ContextPrecisionMetric.EvaluationStrategy.RESPONSE_BASED)
                        .build();

        Double score = contextPrecisionMetric.singleTurnScore(config, sample);

        log.info("Context Precision Score: {}", score);
        assertTrue(score <= 0.7, "Ожидается более низкая оценка, когда нерелевантный контекст первый");
    }
}
```

### Конфигурация

|       Параметр       |        Тип         | Обязательный | По умолчанию |              Описание              |
|----------------------|--------------------|--------------|--------------|------------------------------------|
| `evaluationStrategy` | EvaluationStrategy | Нет          | Авто-выбор   | REFERENCE_BASED или RESPONSE_BASED |
| `models`             | List<String>       | Нет          | все          | Конкретные ID моделей для оценки   |
| `language`           | String             | Нет          | `"en"`       | Язык объяснений (`"en"`, `"ru"`)   |

**Стратегии оценки:**

- **REFERENCE_BASED**: Использует эталонный ответ как золотой стандарт (предпочтительно при наличии)
- **RESPONSE_BASED**: Использует AI-ответ для оценки релевантности
- **Авто-выбор**: Выбирает REFERENCE_BASED, если эталон доступен, иначе RESPONSE_BASED

### Когда использовать

- Оптимизация системы извлечения (измерение качества ранжирования)
- Оценка релевантности поиска (оценка приоритизации документов)
- Настройка RAG-системы (оптимизация параметров извлечения)
- Сравнение различных стратегий извлечения

---

## ContextRecall

> **RAGAS Reference:** [Documentation](https://docs.ragas.io/en/stable/concepts/metrics/available_metrics/context_recall/) | [Python Source](https://github.com/explodinggradients/ragas/blob/main/src/ragas/metrics/_context_recall.py)

ContextRecall измеряет, сколько утверждений в эталонном ответе можно отнести к извлечённым контекстам. Эта метрика
оценивает полноту извлеченной информации.

### Как это работает

1. **Декомпозиция утверждений**: Разбивает эталонный ответ на отдельные предложения
2. **Анализ атрибуции**: Оценивает каждое утверждение относительно извлечённых контекстов
3. **Классификация поддержки**: Определяет, можно ли каждое утверждение атрибутировать (1) или нет (0)
4. **Вычисление отзыва**: Возвращает отношение атрибутируемых утверждений к общему количеству

```java
// Из ContextRecallMetric.java - вычисление recall
class Example {
    Double calculateContextRecall(List<ContextRecallClassification> classifications) {
        long attributedStatements = classifications.stream()
                .mapToInt(ContextRecallClassification::attributed)
                .sum();
        return (double) attributedStatements / classifications.size();
    }
}
```

### Пример

```java
package ai.qa.solutions.metrics.retrieval.ru;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.retrieval.ContextRecallMetric;
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
@SpringBootTest(classes = ContextRecallTest.TestConfiguration.class)
class ContextRecallTest {

    @Configuration
    public static class TestConfiguration {}

    @Autowired
    private ContextRecallMetric contextRecallMetric;

    @Test
    @DisplayName("ContextRecall: Высокий отзыв — все утверждения поддержаны")
    void testHighRecall() {
        Sample sample = Sample.builder()
                .userInput("Расскажите о фотосинтезе")
                .reference("Фотосинтез преобразует световую энергию в химическую. "
                        + "Он происходит в хлоропластах. Процесс требует CO₂, воды и солнечного света. "
                        + "Кислород выделяется как побочный продукт.")
                .retrievedContexts(List.of(
                        "Фотосинтез — процесс, при котором растения преобразуют солнечный свет в химическую энергию.",
                        "Хлоропласты — органеллы в растительных клетках, где происходит фотосинтез.",
                        "Во время фотосинтеза кислород производится как побочный продукт.",
                        "Процесс требует углекислого газа, воды и световой энергии."))
                .build();

        ContextRecallMetric.ContextRecallConfig config =
                ContextRecallMetric.ContextRecallConfig.builder().build();

        Double score = contextRecallMetric.singleTurnScore(config, sample);

        log.info("Context Recall Score: {}", score);
        assertTrue(score >= 0.9, "Ожидается высокая оценка, когда все утверждения поддержаны");
    }
}
```

### Конфигурация

|  Параметр  |     Тип      | Обязательный | По умолчанию |             Описание             |
|------------|--------------|--------------|--------------|----------------------------------|
| `models`   | List<String> | Нет          | все          | Конкретные ID моделей для оценки |
| `language` | String       | Нет          | `"en"`       | Язык объяснений (`"en"`, `"ru"`) |

### Когда использовать

- Комплексное извлечение информации (обеспечение доступности всей нужной информации)
- Проверка полноты базы знаний
- Оценка RAG-системы (измерение качества покрытия информации)
- Анализ пробелов в извлечении (выявление недостающей информации)

---

## Faithfulness

> **RAGAS Reference:** [Documentation](https://docs.ragas.io/en/stable/concepts/metrics/available_metrics/faithfulness/) | [Python Source](https://github.com/explodinggradients/ragas/blob/main/src/ragas/metrics/_faithfulness.py)

Faithfulness измеряет фактическую согласованность между сгенерированным ответом и извлечёнными контекстами.
Метрика выявляет галлюцинации и обеспечивает обоснованность ответов в предоставленной информации.

### Как это работает

1. **Генерация утверждений**: Декомпозирует ответ на атомарные утверждения без местоимений
2. **Оценка верности**: Проверяет каждое утверждение относительно извлечённых контекстов
3. **Назначение вердикта**: Для каждого утверждения вердикт 1 (можно вывести) или 0 (нельзя вывести)
4. **Вычисление оценки**: Возвращает отношение верных утверждений к общему количеству

```java
// Из FaithfulnessMetric.java - вычисление faithfulness
class Example {
    Double calculateFaithfulness(VerdictsResponse verdicts) {
        long faithfulStatements = verdicts.verdicts().stream()
                .filter(v -> v.verdict() != null && v.verdict() == 1)
                .count();
        return (double) faithfulStatements / verdicts.verdicts().size();
    }
}
```

### Пример

```java
package ai.qa.solutions.metrics.retrieval.ru;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.retrieval.FaithfulnessMetric;
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
@SpringBootTest(classes = FaithfulnessTest.TestConfiguration.class)
class FaithfulnessTest {

    @Configuration
    public static class TestConfiguration {}

    @Autowired
    private FaithfulnessMetric faithfulnessMetric;

    @Test
    @DisplayName("Faithfulness: Идеальная верность — все утверждения поддержаны")
    void testPerfectFaithfulness() {
        Sample sample = Sample.builder()
                .userInput("Когда был первый Суперкубок?")
                .response("Первый Суперкубок состоялся 15 января 1967 года.")
                .retrievedContexts(List.of(
                        "Первый Суперкубок состоялся 15 января 1967 года "
                                + "в Лос-Анджелесском мемориальном колизее."))
                .build();

        Double score = faithfulnessMetric.singleTurnScore(sample);

        log.info("Faithfulness Score: {}", score);
        assertTrue(score >= 0.9, "Ожидается высокая оценка для полностью поддержанного ответа");
    }

    @Test
    @DisplayName("Faithfulness: Низкая верность — галлюцинированная информация")
    void testHallucination() {
        Sample sample = Sample.builder()
                .userInput("Какие курсы посещает Иван?")
                .response("Иван посещает курсы по Структурам данных, Алгоритмам и Искусственному интеллекту. "
                        + "Он также работает на полставки в университетской библиотеке.")
                .retrievedContexts(List.of(
                        "Иван — студент университета XYZ, обучающийся по специальности Информатика. "
                                + "Он записан на курсы Структуры данных, Алгоритмы и Управление базами данных. "
                                + "Иван часто задерживается в библиотеке, работая над своими проектами."))
                .build();

        Double score = faithfulnessMetric.singleTurnScore(sample);

        log.info("Faithfulness Score: {}", score);
        assertTrue(score <= 0.7, "Ожидается низкая оценка из-за галлюцинированного курса ИИ и работы");
    }
}
```

### Конфигурация

|  Параметр  |     Тип      | Обязательный | По умолчанию |             Описание             |
|------------|--------------|--------------|--------------|----------------------------------|
| `models`   | List<String> | Нет          | все          | Конкретные ID моделей для оценки |
| `language` | String       | Нет          | `"en"`       | Язык объяснений (`"en"`, `"ru"`) |

### Когда использовать

- Обнаружение галлюцинаций (выявление неподдерживаемых утверждений)
- Валидация RAG-системы (обеспечение обоснованности ответов)
- Контроль качества (поддержание фактической точности)
- Медицинские/юридические приложения (критические домены, требующие точности)

---

## NoiseSensitivity

> **RAGAS Reference:** [Documentation](https://docs.ragas.io/en/stable/concepts/metrics/available_metrics/noise_sensitivity/) | [Python Source](https://github.com/explodinggradients/ragas/blob/main/src/ragas/metrics/_noise_sensitivity.py)

NoiseSensitivity измеряет, как часто система совершает ошибки, предоставляя неверные ответы при использовании релевантных
или нерелевантных извлечённых документов. **Более низкие оценки означают лучшую производительность.**

### Как это работает

1. **Декомпозиция утверждений**: Разбивает эталон и ответ на атомарные утверждения
2. **Оценка истинности**: Оценивает утверждения ответа относительно эталона
3. **Классификация релевантности контекста**: Определяет, какие контексты релевантны на основе эталона
4. **Атрибуция ошибок**: Выявляет неверные утверждения, атрибутируемые релевантным или нерелевантным контекстам
5. **Вычисление чувствительности**: Вычисляет долю ошибок, связанных с контекстом

```java
// Из NoiseSensitivityMetric.java - вычисление чувствительности для режима RELEVANT
class Example {
    Double calculateNoiseSensitivity(boolean[] incorrect, boolean[] relevantFaithful, int numStatements) {
        int count = 0;
        for (int i = 0; i < numStatements; i++) {
            if (relevantFaithful[i] && incorrect[i]) {
                count++;
            }
        }
        return (double) count / numStatements;
    }
}
```

### Пример

```java
package ai.qa.solutions.metrics.retrieval.ru;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.retrieval.NoiseSensitivityMetric;
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
@SpringBootTest(classes = NoiseSensitivityTest.TestConfiguration.class)
class NoiseSensitivityTest {

    @Configuration
    public static class TestConfiguration {}

    @Autowired
    private NoiseSensitivityMetric noiseSensitivityMetric;

    @Test
    @DisplayName("NoiseSensitivity: Низкая чувствительность — хорошая устойчивость")
    void testLowSensitivity() {
        Sample sample = Sample.builder()
                .userInput("Что вызывает землетрясения?")
                .response("Землетрясения вызываются движением тектонических плит.")
                .reference("Землетрясения вызываются движением тектонических плит. "
                        + "Плиты внезапно сдвигаются и высвобождают энергию.")
                .retrievedContexts(List.of(
                        "Тектонические плиты — большие участки земной коры, которые медленно движутся.",
                        "Когда тектонические плиты сталкиваются, они могут вызывать землетрясения.",
                        "Сейсмические волны — это энергия, высвобождающаяся во время землетрясений.",
                        "Лучшее время для посещения Японии — сезон цветения сакуры."))  // Нерелевантный
                .build();

        NoiseSensitivityMetric.NoiseSensitivityConfig config =
                NoiseSensitivityMetric.NoiseSensitivityConfig.builder()
                        .mode(NoiseSensitivityMetric.NoiseSensitivityMode.RELEVANT)
                        .build();

        Double score = noiseSensitivityMetric.singleTurnScore(config, sample);

        log.info("Noise Sensitivity Score: {}", score);
        assertTrue(score <= 0.3, "Ожидается низкая оценка чувствительности (хорошая устойчивость)");
    }

    @Test
    @DisplayName("NoiseSensitivity: Режим IRRELEVANT — измерение влияния нерелевантных контекстов")
    void testIrrelevantMode() {
        Sample sample = Sample.builder()
                .userInput("Какая столица Франции?")
                .response("Париж — столица Франции. Там прекрасная погода круглый год.")
                .reference("Париж — столица Франции.")
                .retrievedContexts(List.of(
                        "Париж — столица и крупнейший город Франции.",  // Релевантный
                        "Погода в Париже меняется в зависимости от сезона.",  // Нерелевантный
                        "Французская кухня славится круассанами и вином."))  // Нерелевантный
                .build();

        NoiseSensitivityMetric.NoiseSensitivityConfig config =
                NoiseSensitivityMetric.NoiseSensitivityConfig.builder()
                        .mode(NoiseSensitivityMetric.NoiseSensitivityMode.IRRELEVANT)
                        .build();

        Double score = noiseSensitivityMetric.singleTurnScore(config, sample);

        log.info("Noise Sensitivity (IRRELEVANT) Score: {}", score);
        // Оценка измеряет ошибки от нерелевантных контекстов — меньше лучше
        assertTrue(score >= 0.0 && score <= 1.0, "Оценка должна быть в допустимом диапазоне");
    }
}
```

### Конфигурация

|  Параметр  |         Тип          | Обязательный | По умолчанию |               Описание               |
|------------|----------------------|--------------|--------------|--------------------------------------|
| `mode`     | NoiseSensitivityMode | Нет          | RELEVANT     | Режим оценки RELEVANT или IRRELEVANT |
| `models`   | List<String>         | Нет          | все          | Конкретные ID моделей для оценки     |
| `language` | String               | Нет          | `"en"`       | Язык объяснений (`"en"`, `"ru"`)     |

**Режимы оценки:**

- **RELEVANT**: Измеряет ошибки, атрибутируемые релевантным извлечённым контекстам
- **IRRELEVANT**: Измеряет ошибки, атрибутируемые нерелевантным извлечённым контекстам

### Интерпретация результатов

**Более низкие оценки лучше:**

- **0.0-0.1**: Отличная устойчивость
- **0.1-0.3**: Хорошая устойчивость
- **0.3-0.5**: Умеренная устойчивость
- **0.5-1.0**: Плохая устойчивость, высокая чувствительность к шуму

### Когда использовать

- Тестирование устойчивости (оценка поведения системы с шумным извлечением)
- Анализ ошибок (понимание влияния нерелевантных контекстов на качество)
- Оптимизация системы (улучшение генерации ответов несмотря на шумные входы)
- Мониторинг в продакшене (обнаружение деградации из-за проблем извлечения)

---

## ResponseRelevancy

> **RAGAS Reference:** [Documentation](https://docs.ragas.io/en/stable/concepts/metrics/available_metrics/answer_relevance/) | [Python Source](https://github.com/explodinggradients/ragas/blob/main/src/ragas/metrics/_answer_relevance.py)

ResponseRelevancy измеряет, насколько релевантен ответ пользовательскому вводу. Метрика обнаруживает неполные ответы,
ответы не по теме и уклончивые (evasive) формулировки.

### Как это работает

1. **Генерация вопросов**: LLM генерирует N искусственных вопросов, на которые мог бы отвечать ответ
2. **Обнаружение уклончивости**: Каждый сгенерированный вопрос включает флаг для уклончивых ответов
3. **Проверка уклончивости**: Если все вопросы указывают на уклончивость, немедленно возвращается 0.0
4. **Вычисление эмбеддингов**: Получает векторные представления оригинального вопроса и сгенерированных вопросов
5. **Вычисление сходства**: Вычисляет косинусное сходство между оригиналом и каждым сгенерированным вопросом
6. **Агрегация**: Возвращает среднее всех оценок сходства

```java
// Из ResponseRelevancyMetric.java - вычисление косинусного сходства
class Example {
    double cosineSimilarity(double[] vectorA, double[] vectorB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
```

**Ключевая идея:**

- Если ответ релевантен, сгенерированные вопросы будут семантически похожи на оригинальный вопрос
- Если ответ не по теме или неполный, сгенерированные вопросы будут отличаться от оригинала
- Уклончивые ответы ("Я не знаю") автоматически получают оценку 0.0

### Пример

```java
package ai.qa.solutions.metrics.retrieval.ru;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.retrieval.ResponseRelevancyMetric;
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
@SpringBootTest(classes = ResponseRelevancyTest.TestConfiguration.class)
class ResponseRelevancyTest {

    @Configuration
    public static class TestConfiguration {}

    @Autowired
    private ResponseRelevancyMetric responseRelevancyMetric;

    @Test
    @DisplayName("ResponseRelevancy: Полный релевантный ответ")
    void testCompleteAnswer() {
        Sample sample = Sample.builder()
                .userInput("Где находится Франция и какая её столица?")
                .response("Франция находится в Западной Европе, и её столица — Париж.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Response Relevancy Score: {}", score);
        assertTrue(score >= 0.85, "Ожидается высокая оценка для полного ответа");
    }

    @Test
    @DisplayName("ResponseRelevancy: Неполный ответ")
    void testIncompleteAnswer() {
        Sample sample = Sample.builder()
                .userInput("Где находится Франция и какая её столица?")
                .response("Франция находится в Западной Европе.")  // Нет информации о столице
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Response Relevancy Score: {}", score);
        assertTrue(score >= 0.5 && score <= 0.8, "Ожидается умеренная оценка для неполного ответа");
    }

    @Test
    @DisplayName("ResponseRelevancy: Уклончивый ответ")
    void testNoncommittalAnswer() {
        Sample sample = Sample.builder()
                .userInput("Какая столица Франции?")
                .response("Я не знаю, какая столица Франции.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Response Relevancy Score: {}", score);
        assertTrue(score < 0.1, "Ожидается нулевая оценка для уклончивого ответа");
    }

    @Test
    @DisplayName("ResponseRelevancy: Настройка numberOfQuestions для более надёжной оценки")
    void testCustomNumberOfQuestions() {
        Sample sample = Sample.builder()
                .userInput("Объясните квантовую запутанность и её применения")
                .response("Квантовая запутанность — это явление, при котором частицы становятся " +
                        "взаимосвязанными. Она применяется в квантовых вычислениях и криптографии.")
                .build();

        // Больше вопросов для сложных тем обеспечивает более надёжные оценки сходства
        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(5)  // Больше вопросов для лучшего покрытия
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Response Relevancy Score (5 вопросов): {}", score);
        assertTrue(score >= 0.7, "Ожидается хорошая оценка для релевантного ответа");
    }
}
```

### Конфигурация

|      Параметр       |     Тип      | Обязательный | По умолчанию |                  Описание                   |
|---------------------|--------------|--------------|--------------|---------------------------------------------|
| `numberOfQuestions` | int          | Нет          | 3            | Количество вопросов для генерации из ответа |
| `models`            | List<String> | Нет          | все          | Конкретные ID моделей для оценки            |
| `language`          | String       | Нет          | `"en"`       | Язык объяснений (`"en"`, `"ru"`)            |

### Когда использовать

- Оценка качества чат-ботов (обеспечение ответов на вопросы пользователей)
- Вопросно-ответные системы (измерение релевантности ответов)
- Диалоговые системы (оценка качества многооборотных диалогов)
- Виртуальные ассистенты (контроль качества ответов)

---

## Выбор подходящей метрики

|          Случай использования           | Рекомендуемая метрика |                   Почему                   |
|-----------------------------------------|-----------------------|--------------------------------------------|
| Приложения, ориентированные на сущности | ContextEntityRecall   | Измеряет покрытие фактических сущностей    |
| Оптимизация ранжирования извлечения     | ContextPrecision      | Оценивает качество ранжирования контекстов |
| Полнота информации                      | ContextRecall         | Измеряет поддержку эталонных утверждений   |
| Обнаружение галлюцинаций                | Faithfulness          | Выявляет неподдерживаемые утверждения      |
| Тестирование устойчивости системы       | NoiseSensitivity      | Измеряет чувствительность к шумным входам  |
| Оценка релевантности ответов            | ResponseRelevancy     | Измеряет соответствие ответа вопросу       |

---

## Схема Sample

Все метрики извлечения используют класс `Sample` для входных данных:

```java
class Example {
    void createSample() {
        Sample sample = Sample.builder()
                .userInput("Вопрос пользователя")                     // Требуется для большинства метрик
                .response("AI-сгенерированный ответ")                 // Требуется для Faithfulness, NoiseSensitivity
                .reference("Эталонный ответ")                         // Требуется для ContextRecall, NoiseSensitivity
                .retrievedContexts(List.of("контекст1", "контекст2")) // Требуется для всех retrieval метрик
                .build();
    }
}
```

|        Поле         |     Тип      |                    Требуется для                     |
|---------------------|--------------|------------------------------------------------------|
| `userInput`         | String       | ContextPrecision, ContextRecall, ResponseRelevancy   |
| `response`          | String       | Faithfulness, NoiseSensitivity, ResponseRelevancy    |
| `reference`         | String       | ContextEntityRecall, ContextRecall, NoiseSensitivity |
| `retrievedContexts` | List<String> | Все метрики извлечения                               |

---

## API расширенной оценки

Все метрики поиска поддерживают `singleTurnEvaluate()`, возвращающий `EvaluationResult` с оценкой, объяснением, результатами по моделям и метаданными:

```java
import ai.qa.solutions.metric.EvaluationResult;

// Вместо Double score = metric.singleTurnScore(config, sample);
EvaluationResult result = faithfulnessMetric.singleTurnEvaluate(config, sample);

log.info("Оценка: {}", result.getScore());
log.info("Объяснение: {}", result.getExplanation().getSimpleDescription());
log.info("Оценки моделей: {}", result.getModelScores());
log.info("Длительность: {}мс", result.getTotalDuration().toMillis());

// Асинхронный вариант
CompletableFuture<EvaluationResult> future =
        faithfulnessMetric.singleTurnEvaluateAsync(config, sample);

// Объяснения на русском языке
FaithfulnessMetric.FaithfulnessConfig ruConfig = FaithfulnessMetric.FaithfulnessConfig.builder()
        .language("ru")
        .build();
EvaluationResult ruResult = faithfulnessMetric.singleTurnEvaluate(ruConfig, sample);
```

