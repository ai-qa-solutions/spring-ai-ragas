package ai.qa.solutions.metrics.nlp.ru;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.nlp.RougeScoreMetric;
import ai.qa.solutions.metrics.nlp.RougeScoreMetric.Mode;
import ai.qa.solutions.metrics.nlp.RougeScoreMetric.RougeScoreConfig;
import ai.qa.solutions.metrics.nlp.RougeScoreMetric.RougeType;
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
 * Интеграционные тесты для ROUGE Score метрики - Русский язык.
 */
@Slf4j
@EnableAutoConfiguration
@SuppressWarnings("LoggingSimilarMessage")
@DisplayName("ROUGE Score Metric - Русский язык")
@SpringBootTest(classes = RuRougeScoreIntegrationIT.RougeScoreIntegrationTestConfiguration.class)
class RuRougeScoreIntegrationIT {

    @Configuration
    public static class RougeScoreIntegrationTestConfiguration {}

    @Autowired
    private RougeScoreMetric rougeScoreMetric;

    @Nested
    @DisplayName("ROUGE-L Тесты")
    class RougeLTests {

        @Test
        @DisplayName("Идентичные тексты")
        void testIdenticalTexts() {
            log.info("=== Тест идентичных текстов ===");

            final Sample sample = Sample.builder()
                    .response("Москва является столицей России.")
                    .reference("Москва является столицей России.")
                    .build();

            final RougeScoreConfig config = RougeScoreConfig.builder()
                    .rougeType(RougeType.ROUGE_L)
                    .mode(Mode.FMEASURE)
                    .build();

            final Double score = rougeScoreMetric.singleTurnScore(config, sample);

            log.info("ROUGE-L Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            assertTrue(score >= 0.99, "Идентичные тексты должны иметь идеальный ROUGE-L. Получено: " + score);
        }

        @Test
        @DisplayName("Похожие тексты")
        void testSimilarTexts() {
            log.info("=== Тест похожих текстов ===");

            final Sample sample = Sample.builder()
                    .response("Кот сидит на коврике.")
                    .reference("Кот лежит на мягком коврике.")
                    .build();

            final RougeScoreConfig config =
                    RougeScoreConfig.builder().rougeType(RougeType.ROUGE_L).build();

            final Double score = rougeScoreMetric.singleTurnScore(config, sample);

            log.info("ROUGE-L Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            assertTrue(score >= 0.4, "Похожие тексты должны иметь хороший ROUGE-L. Получено: " + score);
        }
    }

    @Nested
    @DisplayName("Тесты морфологии")
    class MorphologyTests {

        @Test
        @DisplayName("ROUGE-1 для морфологических вариаций")
        void testRouge1Morphology() {
            log.info("=== Тест морфологических вариаций ===");

            final Sample sample = Sample.builder()
                    .response("Красивый дом стоит на холме.")
                    .reference("Красивые дома стоят на холмах.")
                    .build();

            final RougeScoreConfig config = RougeScoreConfig.builder()
                    .rougeType(RougeType.ROUGE_1)
                    .mode(Mode.FMEASURE)
                    .build();

            final Double score = rougeScoreMetric.singleTurnScore(config, sample);

            log.info("ROUGE-1 Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            // ROUGE работает на уровне слов, морфология влияет на совпадения
            assertTrue(score >= 0.2, "Морфологические вариации должны давать некоторое совпадение. Получено: " + score);
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

            final RougeScoreConfig config = RougeScoreConfig.builder().build();

            final Double score =
                    rougeScoreMetric.singleTurnScoreAsync(config, sample).join();

            log.info("Async ROUGE-L Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            assertTrue(score >= 0.99, "Асинхронный подсчёт должен работать корректно. Получено: " + score);
        }
    }
}
