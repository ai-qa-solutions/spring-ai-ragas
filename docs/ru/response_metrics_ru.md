# Метрики ответов

Метрики ответов оценивают качество ответов AI путём сравнения с эталонными ответами.
Эти метрики используют комбинацию семантического сходства на основе эмбеддингов и
фактической проверки на основе NLI.

## Обзор

|      Метрика       |                      Подход                       |
|--------------------|---------------------------------------------------|
| SemanticSimilarity | Косинусное сходство эмбеддингов (без вызовов LLM) |
| FactualCorrectness | Декомпозиция утверждений + верификация NLI        |
| AnswerCorrectness  | Комбинация фактической и семантической оценок     |

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
    # Модели для мультимодельной оценки
    chat-models:
      default-options:
        temperature: 0.0
        max-tokens: 1000
        top-p: 1.0
      list:
        - { id: anthropic/claude-4.5-sonnet }
        - { id: google/gemini-2.5-flash }
        - { id: openai/gpt-4o-mini }
    # Модели эмбеддингов для семантического сходства
    embedding-models:
      default-options:
        dimensions: 1024
      list:
        - id: openai/text-embedding-3-large
          options: { dimensions: 3072 }
        - id: qwen/qwen3-embedding-8b
  threads:
    virtual:
      enabled: true
```

---

## SemanticSimilarity

> **Ссылка:** [Sentence-BERT: Sentence Embeddings using Siamese BERT-Networks](https://arxiv.org/pdf/1908.10084.pdf)

SemanticSimilarity измеряет семантическую близость между ответом и эталоном с помощью векторных эмбеддингов.
Это быстрая и экономичная метрика, не требующая вызовов LLM.

### Принцип работы

1. **Вычисление эмбеддингов**: Генерация векторных эмбеддингов для ответа и эталона
2. **Косинусное сходство**: Вычисление косинуса угла между векторами
3. **Опциональный порог**: Преобразование в бинарный результат (прошёл/не прошёл) по порогу

```java
// Из SemanticSimilarityMetric.java - вычисление косинусного сходства
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

### Интерпретация оценки

| Диапазон оценки |                 Интерпретация                  |
|-----------------|------------------------------------------------|
| 0.9 - 1.0       | Семантически идентичны                         |
| 0.8 - 0.9       | Очень высокое сходство, почти одинаковый смысл |
| 0.5 - 0.8       | Умеренное сходство, связаны, но различаются    |
| 0.0 - 0.5       | Низкое сходство, разный смысл                  |

### Пример

```java
package ai.qa.solutions.metrics.response.ru;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.response.SemanticSimilarityMetric;
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
@SpringBootTest(classes = SemanticSimilarityTest.TestConfiguration.class)
class SemanticSimilarityTest {

    @Configuration
    public static class TestConfiguration {
    }

    @Autowired
    private SemanticSimilarityMetric semanticSimilarityMetric;

    @Test
    @DisplayName("SemanticSimilarity: Высокое сходство перефразировок")
    void testHighSimilarityParaphrases() {
        Sample sample = Sample.builder()
                .response("Машинное обучение - это раздел искусственного интеллекта.")
                .reference("ML - это ветвь ИИ, позволяющая системам учиться на данных.")
                .build();

        SemanticSimilarityMetric.SemanticSimilarityConfig config =
                SemanticSimilarityMetric.SemanticSimilarityConfig.defaultConfig();

        Double score = semanticSimilarityMetric.singleTurnScore(config, sample);

        log.info("Оценка семантического сходства: {}", score);
        assertTrue(score >= 0.7, "Ожидается высокое сходство для перефразировок");
    }

    @Test
    @DisplayName("SemanticSimilarity: Низкое сходство разных тем")
    void testLowSimilarityDifferentTopics() {
        Sample sample = Sample.builder()
                .response("Сегодня погода солнечная и тёплая.")
                .reference("Квантовые компьютеры используют кубиты для вычислений.")
                .build();

        SemanticSimilarityMetric.SemanticSimilarityConfig config =
                SemanticSimilarityMetric.SemanticSimilarityConfig.defaultConfig();

        Double score = semanticSimilarityMetric.singleTurnScore(config, sample);

        log.info("Оценка семантического сходства: {}", score);
        assertTrue(score <= 0.5, "Ожидается низкое сходство для разных тем");
    }

    @Test
    @DisplayName("SemanticSimilarity: Классификация по порогу")
    void testThresholdBasedClassification() {
        Sample sample = Sample.builder()
                .response("Python - язык программирования для веб-разработки.")
                .reference("Python - универсальный язык программирования для веб-приложений.")
                .build();

        SemanticSimilarityMetric.SemanticSimilarityConfig config =
                SemanticSimilarityMetric.SemanticSimilarityConfig.builder()
                        .threshold(0.8)  // Бинарно: 1.0 если >= 0.8, иначе 0.0
                        .build();

        Double score = semanticSimilarityMetric.singleTurnScore(config, sample);

        log.info("Оценка семантического сходства (порог=0.8): {}", score);
        assertTrue(score == 0.0 || score == 1.0, "Ожидается бинарная оценка с порогом");
    }
}
```

### Конфигурация

|  Параметр   |     Тип      | Обязательный | По умолчанию |                   Описание                   |
|-------------|--------------|--------------|--------------|----------------------------------------------|
| `threshold` | Double       | Нет          | null         | Если задан, возвращает 1.0 или 0.0 по порогу |
| `models`    | List<String> | Нет          | все          | ID моделей эмбеддингов                       |

### Когда использовать

- Быстрая оценка сходства в масштабе
- Обнаружение перефразировок
- Проверка эквивалентности ответов
- Экономичная оценка (без вызовов LLM)

---

## FactualCorrectness

> **Ссылка RAGAS:** [Документация](https://docs.ragas.io/en/stable/concepts/metrics/available_metrics/factual_correctness/) | [Исходный код Python](https://github.com/explodinggradients/ragas/blob/main/src/ragas/metrics/_factual_correctness.py)

FactualCorrectness оценивает фактическую точность путём декомпозиции текстов на атомарные утверждения
и их верификации с помощью Natural Language Inference (NLI).

### Принцип работы

1. **Декомпозиция утверждений**: Разбиение ответа и эталона на атомарные утверждения
2. **Верификация NLI (Precision)**: Проверка утверждений ответа относительно эталона
3. **Верификация NLI (Recall)**: Проверка утверждений эталона относительно ответа
4. **Вычисление оценки**: Расчёт precision, recall или F1

```java
// Из FactualCorrectnessMetric.java - процесс фактической корректности
class Example {
    double computeFactualCorrectness(String response, String reference) {
        // Шаг 1: Декомпозиция на утверждения
        List<String> responseClaims = llm.decomposeClaims(response);
        List<String> referenceClaims = llm.decomposeClaims(reference);

        // Шаг 2: Проверка утверждений ответа относительно эталона (precision)
        double precision = verifyClaimsNLI(responseClaims, reference);

        // Шаг 3: Проверка утверждений эталона относительно ответа (recall)
        double recall = verifyClaimsNLI(referenceClaims, response);

        // Шаг 4: Вычисление F1
        return 2 * precision * recall / (precision + recall);
    }
}
```

**Вердикты NLI:**

- **SUPPORTED**: Утверждение может быть выведено из контекста
- **CONTRADICTED**: Утверждение противоречит контексту
- **NEUTRAL**: Невозможно проверить (недостаточно информации)

### Пример

```java
package ai.qa.solutions.metrics.response.ru;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.response.FactualCorrectnessMetric;
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
@SpringBootTest(classes = FactualCorrectnessTest.TestConfiguration.class)
class FactualCorrectnessTest {

    @Configuration
    public static class TestConfiguration {
    }

    @Autowired
    private FactualCorrectnessMetric factualCorrectnessMetric;

    @Test
    @DisplayName("FactualCorrectness: Высокая фактическая точность")
    void testHighFactualAccuracy() {
        Sample sample = Sample.builder()
                .response("Альберт Эйнштейн был немецким физиком. "
                        + "Он разработал теорию относительности. "
                        + "Он получил Нобелевскую премию по физике в 1921 году.")
                .reference("Альберт Эйнштейн родился в Германии в 1879 году. "
                        + "Он известен разработкой теории относительности. "
                        + "Эйнштейн получил Нобелевскую премию по физике в 1921 году.")
                .build();

        FactualCorrectnessMetric.FactualCorrectnessConfig config =
                FactualCorrectnessMetric.FactualCorrectnessConfig.builder()
                        .mode(FactualCorrectnessMetric.Mode.F1)
                        .build();

        Double score = factualCorrectnessMetric.singleTurnScore(config, sample);

        log.info("Оценка фактической корректности: {}", score);
        assertTrue(score >= 0.8, "Ожидается высокая оценка для фактически точного ответа");
    }

    @Test
    @DisplayName("FactualCorrectness: Обнаружены фактические ошибки")
    void testFactualErrorsDetected() {
        Sample sample = Sample.builder()
                .response("Эйнштейн родился во Франции. "
                        + "Он изобрёл телефон. "
                        + "Он получил Нобелевскую премию по химии.")
                .reference("Альберт Эйнштейн родился в Германии. "
                        + "Он разработал теорию относительности. "
                        + "Он получил Нобелевскую премию по физике.")
                .build();

        FactualCorrectnessMetric.FactualCorrectnessConfig config =
                FactualCorrectnessMetric.FactualCorrectnessConfig.builder()
                        .mode(FactualCorrectnessMetric.Mode.F1)
                        .build();

        Double score = factualCorrectnessMetric.singleTurnScore(config, sample);

        log.info("Оценка фактической корректности: {}", score);
        assertTrue(score <= 0.3, "Ожидается низкая оценка для фактически неверного ответа");
    }

    @Test
    @DisplayName("FactualCorrectness: Режим Precision")
    void testPrecisionMode() {
        Sample sample = Sample.builder()
                .response("Париж - столица Франции.")  // Верно, но неполно
                .reference("Париж - столица Франции. Население составляет более 2 миллионов человек.")
                .build();

        FactualCorrectnessMetric.FactualCorrectnessConfig config =
                FactualCorrectnessMetric.FactualCorrectnessConfig.builder()
                        .mode(FactualCorrectnessMetric.Mode.PRECISION)
                        .build();

        Double score = factualCorrectnessMetric.singleTurnScore(config, sample);

        log.info("Precision фактической корректности: {}", score);
        assertTrue(score >= 0.9, "Ожидается высокая precision для верного, но неполного ответа");
    }

    @Test
    @DisplayName("FactualCorrectness: Режим Recall")
    void testRecallMode() {
        Sample sample = Sample.builder()
                .response("Париж - столица Франции.")  // Нет информации о населении
                .reference("Париж - столица Франции. Население составляет более 2 миллионов человек.")
                .build();

        FactualCorrectnessMetric.FactualCorrectnessConfig config =
                FactualCorrectnessMetric.FactualCorrectnessConfig.builder()
                        .mode(FactualCorrectnessMetric.Mode.RECALL)
                        .build();

        Double score = factualCorrectnessMetric.singleTurnScore(config, sample);

        log.info("Recall фактической корректности: {}", score);
        assertTrue(score >= 0.4 && score <= 0.7, "Ожидается умеренный recall для неполного ответа");
    }
}
```

### Конфигурация

| Параметр |     Тип      | Обязательный | По умолчанию |               Описание                |
|----------|--------------|--------------|--------------|---------------------------------------|
| `mode`   | Mode         | Нет          | F1           | Режим оценки F1, PRECISION или RECALL |
| `models` | List<String> | Нет          | все          | ID моделей для декомпозиции и NLI     |

**Режимы оценки:**

- **F1**: Гармоническое среднее precision и recall (сбалансированный)
- **PRECISION**: Фокус на корректности утверждений ответа
- **RECALL**: Фокус на покрытии утверждений эталона

### Когда использовать

- Детальная фактическая проверка
- Обнаружение конкретных фактических ошибок
- Понимание компромиссов precision vs. recall
- Высокоответственная оценка точности

---

## AnswerCorrectness

> **Ссылка RAGAS:** [Документация](https://docs.ragas.io/en/stable/concepts/metrics/available_metrics/answer_correctness/) | [Исходный код Python](https://github.com/explodinggradients/ragas/blob/main/src/ragas/metrics/_answer_correctness.py)

AnswerCorrectness объединяет FactualCorrectness и SemanticSimilarity для комплексной оценки ответов.

### Принцип работы

1. **Фактическая корректность**: Декомпозиция утверждений + верификация NLI
2. **Семантическое сходство**: Косинусное сходство эмбеддингов
3. **Взвешенная комбинация**: `factualWeight * factual + semanticWeight * semantic`

Веса по умолчанию: 75% фактическая, 25% семантическая

```java
// Из AnswerCorrectnessMetric.java - комбинированная оценка
class Example {
    double computeAnswerCorrectness(Sample sample, double factualWeight, double semanticWeight) {
        // Шаг 1: Фактическая корректность (на основе NLI)
        double factualScore = factualCorrectnessMetric.singleTurnScore(sample);

        // Шаг 2: Семантическое сходство (на основе эмбеддингов)
        double semanticScore = semanticSimilarityMetric.singleTurnScore(sample);

        // Шаг 3: Взвешенная комбинация
        return factualWeight * factualScore + semanticWeight * semanticScore;
    }
}
```

### Пример

```java
package ai.qa.solutions.metrics.response.ru;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.response.AnswerCorrectnessMetric;
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
@SpringBootTest(classes = AnswerCorrectnessTest.TestConfiguration.class)
class AnswerCorrectnessTest {

    @Configuration
    public static class TestConfiguration {
    }

    @Autowired
    private AnswerCorrectnessMetric answerCorrectnessMetric;

    @Test
    @DisplayName("AnswerCorrectness: Высокая корректность")
    void testHighCorrectness() {
        Sample sample = Sample.builder()
                .response("Великая Китайская стена имеет длину более 21 000 км "
                        + "и была построена для защиты от вторжений.")
                .reference("Великая Китайская стена протянулась более чем на 21 000 км. "
                        + "Она была возведена для обороны от захватчиков.")
                .build();

        AnswerCorrectnessMetric.AnswerCorrectnessConfig config =
                AnswerCorrectnessMetric.AnswerCorrectnessConfig.defaultConfig();

        Double score = answerCorrectnessMetric.singleTurnScore(config, sample);

        log.info("Оценка корректности ответа: {}", score);
        assertTrue(score >= 0.8, "Ожидается высокая оценка корректности");
    }

    @Test
    @DisplayName("AnswerCorrectness: Конфигурация с равными весами")
    void testEqualWeights() {
        Sample sample = Sample.builder()
                .response("Python - это язык программирования.")
                .reference("Python - высокоуровневый интерпретируемый язык программирования.")
                .build();

        AnswerCorrectnessMetric.AnswerCorrectnessConfig config =
                AnswerCorrectnessMetric.AnswerCorrectnessConfig.equalWeights();  // 50/50

        Double score = answerCorrectnessMetric.singleTurnScore(config, sample);

        log.info("Оценка корректности ответа (равные веса): {}", score);
        assertTrue(score >= 0.5 && score <= 0.9, "Ожидается умеренно-высокая оценка");
    }

    @Test
    @DisplayName("AnswerCorrectness: Конфигурация с фокусом на факты")
    void testFactualFocused() {
        Sample sample = Sample.builder()
                .response("Вода кипит при 100°C. Лёд тает при 0°C.")
                .reference("При стандартном давлении вода кипит при 100 градусах Цельсия "
                        + "и лёд тает при 0 градусах Цельсия.")
                .build();

        AnswerCorrectnessMetric.AnswerCorrectnessConfig config =
                AnswerCorrectnessMetric.AnswerCorrectnessConfig.factualFocused();  // 90/10

        Double score = answerCorrectnessMetric.singleTurnScore(config, sample);

        log.info("Оценка корректности ответа (фокус на факты): {}", score);
        assertTrue(score >= 0.8, "Ожидается высокая оценка с фокусом на факты");
    }

    @Test
    @DisplayName("AnswerCorrectness: Пользовательские веса")
    void testCustomWeights() {
        Sample sample = Sample.builder()
                .response("Машинное обучение позволяет компьютерам учиться на данных.")
                .reference("ML позволяет системам автоматически улучшаться на основе опыта.")
                .build();

        AnswerCorrectnessMetric.AnswerCorrectnessConfig config =
                AnswerCorrectnessMetric.AnswerCorrectnessConfig.builder()
                        .factualWeight(0.6)
                        .semanticWeight(0.4)
                        .build();

        Double score = answerCorrectnessMetric.singleTurnScore(config, sample);

        log.info("Оценка корректности ответа (веса 60/40): {}", score);
        assertTrue(score >= 0.0 && score <= 1.0, "Оценка должна быть нормализована");
    }
}
```

### Конфигурация

|     Параметр     |     Тип      | Обязательный | По умолчанию |                Описание                 |
|------------------|--------------|--------------|--------------|-----------------------------------------|
| `factualWeight`  | double       | Нет          | 0.75         | Вес компонента фактической корректности |
| `semanticWeight` | double       | Нет          | 0.25         | Вес компонента семантического сходства  |
| `models`         | List<String> | Нет          | все          | ID моделей для фактической корректности |

**Предустановленные конфигурации:**

```java
class Example {
    void configurations() {
        // По умолчанию: 75% факты, 25% семантика
        AnswerCorrectnessConfig.defaultConfig();

        // Равные: 50% факты, 50% семантика
        AnswerCorrectnessConfig.equalWeights();

        // Фокус на фактах: 90% факты, 10% семантика
        AnswerCorrectnessConfig.factualFocused();

        // Фокус на семантике: 10% факты, 90% семантика
        AnswerCorrectnessConfig.semanticFocused();
    }
}
```

### Когда использовать

- Комплексная оценка качества ответов
- Сбалансированная фактическая и семантическая оценка
- Универсальная оценка ответов
- Выбор по умолчанию, когда неясно какую метрику использовать

---

## Выбор метрики

|        Потребность        |      Метрика       |              Причина               |
|---------------------------|--------------------|------------------------------------|
| Быстрая проверка сходства | SemanticSimilarity | Без вызовов LLM, только эмбеддинги |
| Детальная проверка фактов | FactualCorrectness | Верификация утверждений через NLI  |
| Сбалансированная оценка   | AnswerCorrectness  | Объединяет оба подхода             |
| Большие объёмы оценки     | SemanticSimilarity | Наиболее экономичная               |
| Критическая точность      | FactualCorrectness | Выявляет конкретные ошибки         |
| Универсальная             | AnswerCorrectness  | Лучший выбор по умолчанию          |

---

## Схема Sample

Все метрики ответов требуют `response` и `reference`:

```java
class Example {
    void createSample() {
        Sample sample = Sample.builder()
                .response("Сгенерированный AI ответ для оценки")
                .reference("Эталонный ответ (ground truth)")
                .build();
    }
}
```

|    Поле     |  Тип   | Обязательный |     Описание     |
|-------------|--------|--------------|------------------|
| `response`  | String | Да           | Ответ для оценки |
| `reference` | String | Да           | Эталонный ответ  |

