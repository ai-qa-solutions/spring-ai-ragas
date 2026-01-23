package ai.qa.solutions.metrics.nlp.ru;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.allure.nlp.AllureNlpMetricHelper;
import ai.qa.solutions.metrics.nlp.BleuScoreMetric;
import ai.qa.solutions.metrics.nlp.BleuScoreMetric.BleuScoreConfig;
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
 * Интеграционные тесты для BLEU Score метрики - Русский язык.
 * <p>
 * Тестирует расчет BLEU score для сравнения текстов.
 * Это не-LLM метрика, не требующая API ключей.
 */
@Slf4j
@EnableAutoConfiguration
@SuppressWarnings("LoggingSimilarMessage")
@DisplayName("BLEU Score Metric - Русский язык")
@SpringBootTest(classes = RuBleuScoreIntegrationIT.BleuScoreIntegrationTestConfiguration.class)
class RuBleuScoreIntegrationIT {

    @Configuration
    public static class BleuScoreIntegrationTestConfiguration {}

    @Autowired
    private BleuScoreMetric bleuScoreMetric;

    @Nested
    @DisplayName("Базовые тесты оценки")
    class BasicScoringTests {

        @Test
        @DisplayName("Идентичные тексты - ОЖИДАЕТСЯ ВЫСОКИЙ БАЛЛ")
        void testIdenticalTexts() {
            log.info("=== Тест идентичных текстов ===");

            final Sample sample = Sample.builder()
                    .response("Москва является столицей Российской Федерации.")
                    .reference("Москва является столицей Российской Федерации.")
                    .build();

            final BleuScoreConfig config = BleuScoreConfig.builder().build();

            final Double score = bleuScoreMetric.singleTurnScore(config, sample);

            log.info("Ответ: {}", sample.getResponse());
            log.info("Эталон: {}", sample.getReference());
            log.info("BLEU Score: {}", String.format("%.4f", score));

            AllureNlpMetricHelper.attachBleuScore(
                    score,
                    sample.getResponse(),
                    sample.getReference(),
                    config.getMaxNgram(),
                    config.isSmoothing(),
                    "ru");

            assertNotNull(score);
            assertTrue(score >= 0.95, "Идентичные тексты должны иметь почти идеальный BLEU. Получено: " + score);
        }

        @Test
        @DisplayName("Похожие тексты - ОЖИДАЕТСЯ ВЫСОКИЙ БАЛЛ")
        void testSimilarTexts() {
            log.info("=== Тест похожих текстов ===");

            final Sample sample = Sample.builder()
                    .response("Москва является столицей России.")
                    .reference("Москва - столица Российской Федерации.")
                    .build();

            final BleuScoreConfig config = BleuScoreConfig.builder().build();

            final Double score = bleuScoreMetric.singleTurnScore(config, sample);

            log.info("BLEU Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            assertTrue(score >= 0.3, "Похожие тексты должны иметь приличный BLEU. Получено: " + score);
        }

        @Test
        @DisplayName("Разные тексты - ОЖИДАЕТСЯ НИЗКИЙ БАЛЛ")
        void testDifferentTexts() {
            log.info("=== Тест разных текстов ===");

            final Sample sample = Sample.builder()
                    .response("Машинное обучение требует больших наборов данных.")
                    .reference("Сегодня хорошая погода в Париже.")
                    .build();

            final BleuScoreConfig config = BleuScoreConfig.builder().build();

            final Double score = bleuScoreMetric.singleTurnScore(config, sample);

            log.info("BLEU Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            assertTrue(score <= 0.3, "Разные тексты должны иметь низкий BLEU. Получено: " + score);
        }
    }

    @Nested
    @DisplayName("Тесты морфологии")
    class MorphologyTests {

        @Test
        @DisplayName("Падежные изменения")
        void testCaseVariations() {
            log.info("=== Тест падежных изменений ===");

            final Sample sample = Sample.builder()
                    .response("Я вижу красивый дом.")
                    .reference("Я вижу красивого дома.")
                    .build();

            final BleuScoreConfig config = BleuScoreConfig.builder().build();

            final Double score = bleuScoreMetric.singleTurnScore(config, sample);

            log.info("BLEU Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            // Русский язык имеет богатую морфологию, изменения падежей влияют на BLEU
            assertTrue(score >= 0.3, "Падежные вариации должны давать умеренный балл. Получено: " + score);
        }

        @Test
        @DisplayName("Глагольные формы")
        void testVerbForms() {
            log.info("=== Тест глагольных форм ===");

            final Sample sample = Sample.builder()
                    .response("Он читает книгу.")
                    .reference("Он читал книгу.")
                    .build();

            final BleuScoreConfig config = BleuScoreConfig.builder().build();

            final Double score = bleuScoreMetric.singleTurnScore(config, sample);

            log.info("BLEU Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            assertTrue(score >= 0.4, "Разные глагольные формы должны давать хороший балл. Получено: " + score);
        }
    }

    @Nested
    @DisplayName("Граничные случаи")
    class EdgeCasesTests {

        @Test
        @DisplayName("Короткий ответ с длинным эталоном")
        void testBrevityPenalty() {
            log.info("=== Тест штрафа за краткость ===");

            final Sample sample = Sample.builder()
                    .response("Привет")
                    .reference("Привет мир как дела сегодня у тебя")
                    .build();

            final BleuScoreConfig config = BleuScoreConfig.builder().build();

            final Double score = bleuScoreMetric.singleTurnScore(config, sample);

            log.info("BLEU Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            assertTrue(score < 0.8, "Короткий ответ должен штрафоваться. Получено: " + score);
        }

        @Test
        @DisplayName("Асинхронный подсчёт")
        void testAsyncScoring() {
            log.info("=== Тест асинхронного подсчёта ===");

            final Sample sample = Sample.builder()
                    .response("Добрый день всем")
                    .reference("Добрый день всем")
                    .build();

            final BleuScoreConfig config = BleuScoreConfig.builder().build();

            final Double score =
                    bleuScoreMetric.singleTurnScoreAsync(config, sample).join();

            log.info("Async BLEU Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            // BLEU для коротких текстов (3 слова) может быть ниже из-за brevity penalty
            assertTrue(score >= 0.7, "Асинхронный подсчёт должен работать корректно. Получено: " + score);
        }
    }
}
