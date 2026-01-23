# NLP-метрики

NLP-метрики оценивают сходство текстов с использованием традиционных алгоритмов обработки естественного языка.
Это не-LLM метрики, которые вычисляют оценки алгоритмически, что делает их быстрыми, детерминированными
и экономически эффективными для масштабных оценок.

## Обзор

NLP-метрики сравнивают сгенерированный текст с эталонным с помощью различных мер сходства:

|      Метрика      |    Тип     |                      Фокус                       |
|-------------------|------------|--------------------------------------------------|
| BLEU Score        | N-граммы   | Точность на основе совпадения n-грамм            |
| ROUGE Score       | N-граммы   | Полнота на основе совпадения n-грамм             |
| chrF Score        | Символьный | Сходство по символьным n-граммам                 |
| String Similarity | Расстояние | Редакционное расстояние и сопоставление символов |

---

## BleuScore

> **Ссылка:** [BLEU: a Method for Automatic Evaluation of Machine Translation](https://aclanthology.org/P02-1040.pdf)

BLEU (Bilingual Evaluation Understudy) оценивает качество текста путём сравнения точности n-грамм с эталоном.
Изначально разработан для машинного перевода, широко используется для оценки генерации текста.

### Принцип работы

1. **Извлечение n-грамм**: Извлекает n-граммы (n=1 до maxNgram) из ответа и эталона
2. **Модифицированная точность**: Обрезает счётчики n-грамм для избежания повторного учёта
3. **Штраф за краткость**: Штрафует ответы короче эталона
4. **Среднее геометрическое**: Объединяет точности по размерам n-грамм

```java
// Из BleuScoreMetric.java - вычисление BLEU
class Example {
    double computeBleuScore(List<String> response, List<String> reference, int maxNgram) {
        // Вычисление модифицированной точности для каждого n
        List<Double> precisions = new ArrayList<>();
        for (int n = 1; n <= maxNgram; n++) {
            precisions.add(computeModifiedPrecision(response, reference, n));
        }

        // Штраф за краткость
        double brevityPenalty = computeBrevityPenalty(response.size(), reference.size());

        // Среднее геометрическое точностей × штраф за краткость
        double geometricMean = Math.exp(logSum / maxNgram);
        return brevityPenalty * geometricMean;
    }
}
```

### Пример

```java
package ai.qa.solutions.metrics.nlp.ru;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.nlp.BleuScoreMetric;
import ai.qa.solutions.sample.Sample;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Slf4j
class BleuScoreTest {

    private final BleuScoreMetric bleuScoreMetric = new BleuScoreMetric();

    @Test
    @DisplayName("BleuScore: Высокое сходство перевода")
    void testHighSimilarity() {
        Sample sample = Sample.builder()
                .response("Кошка сидела на коврике.")
                .reference("Кошка сидит на коврике.")
                .build();

        BleuScoreMetric.BleuScoreConfig config = BleuScoreMetric.BleuScoreConfig.builder()
                .maxNgram(4)
                .smoothing(true)
                .build();

        Double score = bleuScoreMetric.singleTurnScore(config, sample);

        log.info("BLEU Score: {}", score);
        assertTrue(score >= 0.5, "Ожидается умеренно-высокая оценка для похожих предложений");
    }

    @Test
    @DisplayName("BleuScore: Точное совпадение")
    void testExactMatch() {
        Sample sample = Sample.builder()
                .response("Париж - столица Франции.")
                .reference("Париж - столица Франции.")
                .build();

        BleuScoreMetric.BleuScoreConfig config = BleuScoreMetric.BleuScoreConfig.builder()
                .maxNgram(4)
                .build();

        Double score = bleuScoreMetric.singleTurnScore(config, sample);

        log.info("BLEU Score: {}", score);
        assertTrue(score >= 0.99, "Ожидается идеальная оценка для точного совпадения");
    }

    @Test
    @DisplayName("BleuScore: Нет совпадений")
    void testNoOverlap() {
        Sample sample = Sample.builder()
                .response("Привет мир")
                .reference("До свидания луна")
                .build();

        BleuScoreMetric.BleuScoreConfig config = BleuScoreMetric.BleuScoreConfig.builder()
                .maxNgram(4)
                .smoothing(false)
                .build();

        Double score = bleuScoreMetric.singleTurnScore(config, sample);

        log.info("BLEU Score: {}", score);
        assertTrue(score <= 0.1, "Ожидается низкая оценка при отсутствии совпадений");
    }
}
```

### Конфигурация

|  Параметр   |   Тип   | Обязательный | По умолчанию |                     Описание                     |
|-------------|---------|--------------|--------------|--------------------------------------------------|
| `maxNgram`  | int     | Нет          | 4            | Максимальный размер n-грамм (стандарт BLEU = 4)  |
| `smoothing` | boolean | Нет          | true         | Сглаживание для коротких текстов с нулём n-грамм |

### Интерпретация оценки

- **0.6-1.0**: Высокое качество, очень близко к эталону
- **0.4-0.6**: Хорошее качество, передаёт основное содержание
- **0.2-0.4**: Среднее качество, частичное совпадение
- **0.0-0.2**: Низкое качество, мало сходства

### Когда использовать

- Оценка машинного перевода
- Качество суммаризации текста
- Оценка перефразирования
- Любое сравнение текста с фокусом на точность

---

## RougeScore

> **Ссылка:** [ROUGE: A Package for Automatic Evaluation of Summaries](https://aclanthology.org/W04-1013.pdf)

ROUGE (Recall-Oriented Understudy for Gisting Evaluation) измеряет сходство текста на основе полноты совпадения n-грамм.
Это стандартная метрика для оценки суммаризации текста.

### Принцип работы

Поддерживает три варианта:

1. **ROUGE-1**: Совпадение униграмм (отдельные слова)
2. **ROUGE-2**: Совпадение биграмм (пары слов)
3. **ROUGE-L**: Наибольшая общая подпоследовательность

```java
// Из RougeScoreMetric.java - вычисление ROUGE-L
class Example {
    double computeRougeL(List<String> response, List<String> reference, Mode mode) {
        int lcsLength = computeLCS(response, reference);

        double recall = (double) lcsLength / reference.size();
        double precision = (double) lcsLength / response.size();

        return switch (mode) {
            case RECALL -> recall;
            case PRECISION -> precision;
            case FMEASURE -> 2 * precision * recall / (precision + recall);
        };
    }
}
```

### Пример

```java
package ai.qa.solutions.metrics.nlp.ru;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.nlp.RougeScoreMetric;
import ai.qa.solutions.sample.Sample;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Slf4j
class RougeScoreTest {

    private final RougeScoreMetric rougeScoreMetric = new RougeScoreMetric();

    @Test
    @DisplayName("RougeScore: ROUGE-L для суммаризации")
    void testRougeLSummarization() {
        Sample sample = Sample.builder()
                .response("ИИ помогает врачам быстрее диагностировать болезни.")
                .reference("Искусственный интеллект помогает медицинским специалистам диагностировать заболевания быстрее и точнее.")
                .build();

        RougeScoreMetric.RougeScoreConfig config = RougeScoreMetric.RougeScoreConfig.builder()
                .rougeType(RougeScoreMetric.RougeType.ROUGE_L)
                .mode(RougeScoreMetric.Mode.FMEASURE)
                .build();

        Double score = rougeScoreMetric.singleTurnScore(config, sample);

        log.info("ROUGE-L F1 Score: {}", score);
        assertTrue(score >= 0.3, "Ожидается умеренная оценка для суммаризации");
    }

    @Test
    @DisplayName("RougeScore: ROUGE-1 совпадение униграмм")
    void testRouge1Unigram() {
        Sample sample = Sample.builder()
                .response("Быстрая коричневая лиса прыгает через ленивую собаку")
                .reference("Быстрая бурая лиса перепрыгивает через ленивую собаку")
                .build();

        RougeScoreMetric.RougeScoreConfig config = RougeScoreMetric.RougeScoreConfig.builder()
                .rougeType(RougeScoreMetric.RougeType.ROUGE_1)
                .mode(RougeScoreMetric.Mode.RECALL)
                .build();

        Double score = rougeScoreMetric.singleTurnScore(config, sample);

        log.info("ROUGE-1 Recall Score: {}", score);
        assertTrue(score >= 0.6, "Ожидается высокая полнота для похожих предложений");
    }

    @Test
    @DisplayName("RougeScore: ROUGE-2 совпадение биграмм")
    void testRouge2Bigram() {
        Sample sample = Sample.builder()
                .response("алгоритмы машинного обучения")
                .reference("алгоритмы машинного обучения и модели глубокого обучения")
                .build();

        RougeScoreMetric.RougeScoreConfig config = RougeScoreMetric.RougeScoreConfig.builder()
                .rougeType(RougeScoreMetric.RougeType.ROUGE_2)
                .mode(RougeScoreMetric.Mode.PRECISION)
                .build();

        Double score = rougeScoreMetric.singleTurnScore(config, sample);

        log.info("ROUGE-2 Precision Score: {}", score);
        assertTrue(score >= 0.5, "Ожидается высокая точность для подмножества биграмм");
    }
}
```

### Конфигурация

|  Параметр   |    Тип    | Обязательный | По умолчанию |                  Описание                   |
|-------------|-----------|--------------|--------------|---------------------------------------------|
| `rougeType` | RougeType | Нет          | ROUGE_L      | Вариант ROUGE_1, ROUGE_2 или ROUGE_L        |
| `mode`      | Mode      | Нет          | FMEASURE     | Режим оценки RECALL, PRECISION или FMEASURE |

### Когда использовать

- Оценка суммаризации текста
- Оценка сходства документов
- Измерение покрытия содержания
- Любое сравнение текста с фокусом на полноту

---

## ChrfScore

> **Ссылка:** [chrF: character n-gram F-score for automatic MT evaluation](https://aclanthology.org/W15-3049.pdf)

chrF (Character n-gram F-score) оценивает текст с использованием символьных n-грамм, что делает метрику
более устойчивой к морфологическим вариациям, опечаткам и агглютинативным языкам по сравнению
с метриками на основе слов.

### Принцип работы

1. **Символьные n-граммы**: Извлекает символьные n-граммы (по умолчанию n=1 до 6)
2. **Словесные n-граммы (опционально)**: Режим chrF++ добавляет словесные n-граммы
3. **F-beta оценка**: Вычисляет взвешенную F-оценку с настраиваемым beta

```java
// Из ChrfScoreMetric.java - вычисление chrF
class Example {
    double computeChrfScore(String response, String reference, int charNgramOrder, int wordNgramOrder, double beta) {
        // F-оценка по символьным n-граммам
        double charFScore = 0.0;
        for (int n = 1; n <= charNgramOrder; n++) {
            charFScore += computeCharNgramFScore(response, reference, n, beta);
        }
        charFScore /= charNgramOrder;

        // Опциональные словесные n-граммы для режима chrF++
        if (wordNgramOrder > 0) {
            double wordFScore = computeWordNgramFScore(responseTokens, referenceTokens, wordNgramOrder, beta);
            return (charFScore + wordFScore) / 2.0;
        }

        return charFScore;
    }
}
```

### Пример

```java
package ai.qa.solutions.metrics.nlp.ru;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.nlp.ChrfScoreMetric;
import ai.qa.solutions.sample.Sample;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Slf4j
class ChrfScoreTest {

    private final ChrfScoreMetric chrfScoreMetric = new ChrfScoreMetric();

    @Test
    @DisplayName("ChrfScore: Устойчивость к опечаткам")
    void testRobustToTypos() {
        Sample sample = Sample.builder()
                .response("Быстрая кричневая лиса прыгает через ленивую собаку")  // опечатка: кричневая
                .reference("Быстрая коричневая лиса прыгает через ленивую собаку")
                .build();

        ChrfScoreMetric.ChrfScoreConfig config = ChrfScoreMetric.ChrfScoreConfig.builder()
                .charNgramOrder(6)
                .beta(2.0)
                .build();

        Double score = chrfScoreMetric.singleTurnScore(config, sample);

        log.info("chrF Score: {}", score);
        assertTrue(score >= 0.9, "Ожидается высокая оценка несмотря на небольшую опечатку");
    }

    @Test
    @DisplayName("ChrfScore: Режим chrF++ со словесными n-граммами")
    void testChrfPlusPlus() {
        Sample sample = Sample.builder()
                .response("Обработка естественного языка увлекательна")
                .reference("Обработка естественного языка интересна и увлекательна")
                .build();

        ChrfScoreMetric.ChrfScoreConfig config = ChrfScoreMetric.ChrfScoreConfig.builder()
                .charNgramOrder(6)
                .wordNgramOrder(2)  // Включить режим chrF++
                .beta(2.0)
                .build();

        Double score = chrfScoreMetric.singleTurnScore(config, sample);

        log.info("chrF++ Score: {}", score);
        assertTrue(score >= 0.7, "Ожидается хорошая оценка для похожего текста");
    }

    @Test
    @DisplayName("ChrfScore: Морфологические вариации")
    void testMorphologicalVariations() {
        Sample sample = Sample.builder()
                .response("бегущий быстро")
                .reference("бежит быстро")
                .build();

        ChrfScoreMetric.ChrfScoreConfig config = ChrfScoreMetric.ChrfScoreConfig.builder()
                .charNgramOrder(6)
                .build();

        Double score = chrfScoreMetric.singleTurnScore(config, sample);

        log.info("chrF Score: {}", score);
        assertTrue(score >= 0.5, "Ожидается умеренная оценка для морфологических вариаций");
    }
}
```

### Конфигурация

|     Параметр     |  Тип   | Обязательный | По умолчанию |                       Описание                       |
|------------------|--------|--------------|--------------|------------------------------------------------------|
| `charNgramOrder` | int    | Нет          | 6            | Максимальный порядок символьных n-грамм (стандарт 6) |
| `wordNgramOrder` | int    | Нет          | 0            | Порядок словесных n-грамм (0 = chrF, >0 = chrF++)    |
| `beta`           | double | Нет          | 2.0          | Вес F-beta (выше = больший вес полноты)              |

### Когда использовать

- Морфологически богатые языки (русский, финский)
- Тексты с возможными опечатками или ошибками OCR
- Оценка агглютинативных языков
- Сравнение текста на уровне символов

---

## StringSimilarity

StringSimilarity предоставляет множество классических алгоритмов расстояния между строками для сравнения текста.

### Доступные алгоритмы

|   Алгоритм   |                      Описание                       |
|--------------|-----------------------------------------------------|
| LEVENSHTEIN  | Редакционное расстояние (вставки, удаления, замены) |
| HAMMING      | Расстояние только по заменам (строки равной длины)  |
| JARO         | Сопоставление символов с учётом транспозиций        |
| JARO_WINKLER | Jaro с бонусом за совпадающий префикс               |

### Принцип работы

```java
// Из StringSimilarityMetric.java - сходство Левенштейна
class Example {
    double computeLevenshteinSimilarity(String s1, String s2) {
        int distance = levenshteinDistance.apply(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());
        return 1.0 - ((double) distance / maxLength);
    }
}
```

### Пример

```java
package ai.qa.solutions.metrics.nlp.ru;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.nlp.StringSimilarityMetric;
import ai.qa.solutions.sample.Sample;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Slf4j
class StringSimilarityTest {

    private final StringSimilarityMetric stringSimilarityMetric = new StringSimilarityMetric();

    @Test
    @DisplayName("StringSimilarity: Расстояние Левенштейна")
    void testLevenshtein() {
        Sample sample = Sample.builder()
                .response("котёнок")
                .reference("котенок")
                .build();

        StringSimilarityMetric.StringSimilarityConfig config =
                StringSimilarityMetric.StringSimilarityConfig.builder()
                        .distanceMeasure(StringSimilarityMetric.DistanceMeasure.LEVENSHTEIN)
                        .caseSensitive(false)
                        .build();

        Double score = stringSimilarityMetric.singleTurnScore(config, sample);

        log.info("Сходство по Левенштейну: {}", score);
        assertTrue(score >= 0.8, "Ожидается высокое сходство");
    }

    @Test
    @DisplayName("StringSimilarity: Jaro-Winkler для имён")
    void testJaroWinkler() {
        Sample sample = Sample.builder()
                .response("МАРТА")
                .reference("МАРФА")
                .build();

        StringSimilarityMetric.StringSimilarityConfig config =
                StringSimilarityMetric.StringSimilarityConfig.builder()
                        .distanceMeasure(StringSimilarityMetric.DistanceMeasure.JARO_WINKLER)
                        .caseSensitive(false)
                        .build();

        Double score = stringSimilarityMetric.singleTurnScore(config, sample);

        log.info("Сходство Jaro-Winkler: {}", score);
        assertTrue(score >= 0.8, "Ожидается высокое сходство для транспонированных символов");
    }

    @Test
    @DisplayName("StringSimilarity: Чувствительность к регистру")
    void testCaseSensitivity() {
        Sample sample = Sample.builder()
                .response("Привет Мир")
                .reference("привет мир")
                .build();

        // Без учёта регистра
        StringSimilarityMetric.StringSimilarityConfig insensitiveConfig =
                StringSimilarityMetric.StringSimilarityConfig.builder()
                        .distanceMeasure(StringSimilarityMetric.DistanceMeasure.LEVENSHTEIN)
                        .caseSensitive(false)
                        .build();

        Double insensitiveScore = stringSimilarityMetric.singleTurnScore(insensitiveConfig, sample);

        // С учётом регистра
        StringSimilarityMetric.StringSimilarityConfig sensitiveConfig =
                StringSimilarityMetric.StringSimilarityConfig.builder()
                        .distanceMeasure(StringSimilarityMetric.DistanceMeasure.LEVENSHTEIN)
                        .caseSensitive(true)
                        .build();

        Double sensitiveScore = stringSimilarityMetric.singleTurnScore(sensitiveConfig, sample);

        log.info("Без учёта регистра: {}, С учётом: {}", insensitiveScore, sensitiveScore);
        assertTrue(insensitiveScore > sensitiveScore, "Без учёта регистра должна быть выше");
    }
}
```

### Конфигурация

|     Параметр      |       Тип       | Обязательный | По умолчанию |              Описание              |
|-------------------|-----------------|--------------|--------------|------------------------------------|
| `distanceMeasure` | DistanceMeasure | Нет          | JARO_WINKLER | Алгоритм для сравнения             |
| `caseSensitive`   | boolean         | Нет          | false        | Учитывать ли регистр при сравнении |

### Руководство по выбору алгоритма

|        Сценарий        |   Алгоритм   |                   Причина                   |
|------------------------|--------------|---------------------------------------------|
| Общее сравнение текста | LEVENSHTEIN  | Наиболее универсальный                      |
| Сопоставление имён     | JARO_WINKLER | Бонус за префикс помогает с вариантами имён |
| ДНК-последовательности | HAMMING      | Только замены, равная длина                 |
| Короткие строки        | JARO         | Хорош для коротких строк без префикса       |

---

## Выбор метрики

|           Сценарий           |      Метрика      |                Причина                 |
|------------------------------|-------------------|----------------------------------------|
| Машинный перевод             | BLEU              | Стандартная метрика оценки MT          |
| Суммаризация текста          | ROUGE             | Фокус на полноте эталонного содержания |
| Многоязычность/морфология    | chrF              | Устойчивость на уровне символов        |
| Точное сопоставление строк   | StringSimilarity  | Прямое измерение расстояния            |
| Обнаружение опечаток         | chrF, Levenshtein | Оба обрабатывают символьные ошибки     |
| Сопоставление имён/сущностей | Jaro-Winkler      | Оптимизирован для вариаций имён        |

---

## Схема Sample

Все NLP-метрики используют поля `response` и `reference`:

```java
class Example {
    void createSample() {
        Sample sample = Sample.builder()
                .response("Сгенерированный текст для оценки")
                .reference("Эталонный текст (ground truth)")
                .build();
    }
}
```

|    Поле     |  Тип   | Обязательный |       Описание        |
|-------------|--------|--------------|-----------------------|
| `response`  | String | Да           | Сгенерированный текст |
| `reference` | String | Да           | Эталонный текст       |

