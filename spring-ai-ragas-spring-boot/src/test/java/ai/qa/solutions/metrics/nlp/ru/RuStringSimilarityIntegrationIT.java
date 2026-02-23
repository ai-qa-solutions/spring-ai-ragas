package ai.qa.solutions.metrics.nlp.ru;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.nlp.StringSimilarityMetric;
import ai.qa.solutions.metrics.nlp.StringSimilarityMetric.DistanceMeasure;
import ai.qa.solutions.metrics.nlp.StringSimilarityMetric.StringSimilarityConfig;
import ai.qa.solutions.sample.Sample;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

/**
 * Интеграционные тесты для String Similarity метрики - Русский язык.
 */
@Slf4j
@EnableAutoConfiguration
@SuppressWarnings("LoggingSimilarMessage")
@DisplayName("String Similarity Metric - Русский язык")
@SpringBootTest(classes = RuStringSimilarityIntegrationIT.StringSimilarityIntegrationTestConfiguration.class)
class RuStringSimilarityIntegrationIT {

    @Configuration
    public static class StringSimilarityIntegrationTestConfiguration {}

    @Autowired
    private StringSimilarityMetric stringSimilarityMetric;

    @Nested
    @DisplayName("Jaro-Winkler Тесты")
    class JaroWinklerTests {

        @Test
        @DisplayName("Идентичные строки")
        void testIdenticalStrings() {
            log.info("=== Тест идентичных строк ===");

            final Sample sample = Sample.builder()
                    .response("Привет мир")
                    .reference("Привет мир")
                    .build();

            final StringSimilarityConfig config =
                    StringSimilarityConfig.builder().build();

            final Double score = stringSimilarityMetric.singleTurnScore(config, sample);

            log.info("Jaro-Winkler Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            assertTrue(score >= 0.99, "Идентичные строки должны иметь идеальный балл. Получено: " + score);
        }

        @Test
        @DisplayName("Похожие строки с общим префиксом")
        void testSimilarStringsWithPrefix() {
            log.info("=== Тест строк с общим префиксом ===");

            final Sample sample = Sample.builder()
                    .response("программирование")
                    .reference("программист")
                    .build();

            final StringSimilarityConfig config = StringSimilarityConfig.builder()
                    .distanceMeasure(DistanceMeasure.JARO_WINKLER)
                    .build();

            final Double score = stringSimilarityMetric.singleTurnScore(config, sample);

            log.info("Jaro-Winkler Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            // Общий корень "програм" даёт бонус
            assertTrue(score >= 0.7, "Общий корень должен давать хороший балл. Получено: " + score);
        }
    }

    @Nested
    @DisplayName("Тесты Левенштейна")
    class LevenshteinTests {

        @Test
        @DisplayName("Одна буква различия")
        void testSingleCharacterDifference() {
            log.info("=== Тест одной буквы различия ===");

            final Sample sample =
                    Sample.builder().response("мама").reference("папа").build();

            final StringSimilarityConfig config = StringSimilarityConfig.builder()
                    .distanceMeasure(DistanceMeasure.LEVENSHTEIN)
                    .build();

            final Double score = stringSimilarityMetric.singleTurnScore(config, sample);

            log.info("Levenshtein Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            // 2 замены из 4 символов = 0.5 сходство
            assertTrue(score >= 0.4, "Небольшие различия должны давать умеренный балл. Получено: " + score);
        }
    }

    @Nested
    @DisplayName("Тесты кириллицы")
    class CyrillicTests {

        @Test
        @DisplayName("Кириллические символы")
        void testCyrillicCharacters() {
            log.info("=== Тест кириллицы ===");

            final Sample sample = Sample.builder()
                    .response("Москва")
                    .reference("Москвa") // Последняя 'а' - латинская
                    .build();

            final StringSimilarityConfig config = StringSimilarityConfig.builder()
                    .distanceMeasure(DistanceMeasure.LEVENSHTEIN)
                    .build();

            final Double score = stringSimilarityMetric.singleTurnScore(config, sample);

            log.info("Levenshtein Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            // Латинская и кириллическая 'а' - разные символы
            assertTrue(score >= 0.8, "Визуально похожие символы. Получено: " + score);
        }

        @Test
        @DisplayName("Буквы ё и е")
        void testYoAndYe() {
            log.info("=== Тест ё и е ===");

            final Sample sample =
                    Sample.builder().response("ёлка").reference("елка").build();

            final StringSimilarityConfig config = StringSimilarityConfig.builder()
                    .distanceMeasure(DistanceMeasure.JARO_WINKLER)
                    .build();

            final Double score = stringSimilarityMetric.singleTurnScore(config, sample);

            log.info("Jaro-Winkler Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            // ё и е - разные символы, но строки очень похожи
            assertTrue(score >= 0.8, "ё и е должны давать высокий балл. Получено: " + score);
        }
    }

    @Nested
    @DisplayName("Граничные случаи")
    class EdgeCasesTests {

        @Test
        @DisplayName("Асинхронный подсчёт")
        void testAsyncScoring() {
            log.info("=== Тест асинхронного подсчёта ===");

            final Sample sample =
                    Sample.builder().response("привет").reference("привет").build();

            final StringSimilarityConfig config =
                    StringSimilarityConfig.builder().build();

            final Double score =
                    stringSimilarityMetric.singleTurnScoreAsync(config, sample).join();

            log.info("Async Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            assertTrue(score >= 0.99, "Асинхронный подсчёт должен работать корректно. Получено: " + score);
        }
    }
}
