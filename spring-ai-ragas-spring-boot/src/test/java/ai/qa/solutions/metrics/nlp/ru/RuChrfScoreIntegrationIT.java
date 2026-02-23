package ai.qa.solutions.metrics.nlp.ru;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.nlp.ChrfScoreMetric;
import ai.qa.solutions.metrics.nlp.ChrfScoreMetric.ChrfScoreConfig;
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
 * Интеграционные тесты для chrF Score метрики - Русский язык.
 * <p>
 * chrF особенно полезен для морфологически богатых языков как русский.
 */
@Slf4j
@EnableAutoConfiguration
@SuppressWarnings("LoggingSimilarMessage")
@DisplayName("chrF Score Metric - Русский язык")
@SpringBootTest(classes = RuChrfScoreIntegrationIT.ChrfScoreIntegrationTestConfiguration.class)
class RuChrfScoreIntegrationIT {

    @Configuration
    public static class ChrfScoreIntegrationTestConfiguration {}

    @Autowired
    private ChrfScoreMetric chrfScoreMetric;

    @Nested
    @DisplayName("Базовые тесты")
    class BasicScoringTests {

        @Test
        @DisplayName("Идентичные тексты")
        void testIdenticalTexts() {
            log.info("=== Тест идентичных текстов ===");

            final Sample sample = Sample.builder()
                    .response("Москва является столицей России.")
                    .reference("Москва является столицей России.")
                    .build();

            final ChrfScoreConfig config = ChrfScoreConfig.builder().build();

            final Double score = chrfScoreMetric.singleTurnScore(config, sample);

            log.info("chrF Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            assertTrue(score >= 0.99, "Идентичные тексты должны иметь идеальный chrF. Получено: " + score);
        }
    }

    @Nested
    @DisplayName("Тесты морфологии")
    class MorphologyTests {

        @Test
        @DisplayName("Морфологические вариации")
        void testMorphologicalVariations() {
            log.info("=== Тест морфологических вариаций ===");

            final Sample sample = Sample.builder()
                    .response("красивый дом красивого дома")
                    .reference("красивые дома красивых домов")
                    .build();

            final ChrfScoreConfig config = ChrfScoreConfig.builder().build();

            final Double score = chrfScoreMetric.singleTurnScore(config, sample);

            log.info("chrF Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            // chrF хорошо работает с морфологическими вариациями
            assertTrue(score >= 0.5, "Морфологические вариации должны давать хороший chrF. Получено: " + score);
        }

        @Test
        @DisplayName("Падежные окончания")
        void testCaseEndings() {
            log.info("=== Тест падежных окончаний ===");

            final Sample sample = Sample.builder()
                    .response("книга книги книгу книгой книге")
                    .reference("книга книги книгу книгой книге")
                    .build();

            final ChrfScoreConfig config = ChrfScoreConfig.builder().build();

            final Double score = chrfScoreMetric.singleTurnScore(config, sample);

            log.info("chrF Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            assertTrue(score >= 0.99, "Идентичные падежные формы должны совпадать. Получено: " + score);
        }
    }

    @Nested
    @DisplayName("Тесты опечаток")
    class TypoTests {

        @Test
        @DisplayName("Распространённые опечатки")
        void testCommonTypos() {
            log.info("=== Тест распространённых опечаток ===");

            final Sample sample = Sample.builder()
                    .response("прывет как дила")
                    .reference("привет как дела")
                    .build();

            final ChrfScoreConfig config = ChrfScoreConfig.builder().build();

            final Double score = chrfScoreMetric.singleTurnScore(config, sample);

            log.info("chrF Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            // chrF устойчив к опечаткам
            assertTrue(score >= 0.5, "Опечатки должны давать умеренный chrF. Получено: " + score);
        }
    }

    @Nested
    @DisplayName("Граничные случаи")
    class EdgeCasesTests {

        @Test
        @DisplayName("Асинхронный подсчёт")
        void testAsyncScoring() {
            log.info("=== Тест асинхронного подсчёта ===");

            final Sample sample = Sample.builder()
                    .response("Привет мир")
                    .reference("Привет мир")
                    .build();

            final ChrfScoreConfig config = ChrfScoreConfig.builder().build();

            final Double score =
                    chrfScoreMetric.singleTurnScoreAsync(config, sample).join();

            log.info("Async chrF Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            assertTrue(score >= 0.99, "Асинхронный подсчёт должен работать корректно. Получено: " + score);
        }
    }
}
