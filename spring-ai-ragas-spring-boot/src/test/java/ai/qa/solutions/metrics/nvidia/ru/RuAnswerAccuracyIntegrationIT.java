package ai.qa.solutions.metrics.nvidia.ru;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.nvidia.AnswerAccuracyMetric;
import ai.qa.solutions.sample.Sample;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

/**
 * Интеграционные тесты метрики Answer Accuracy (NVIDIA-стиль) - Русский язык.
 * <p>
 * Тестируют оценку точности ответа относительно эталонного ответа.
 * <p>
 * Ключевые характеристики:
 * - Шкала оценки 0-2, нормализованная к 0-1
 * - 0: Неверно, 1: Частично верно, 2: Полностью верно
 * - Поддержка режима двойного судьи для повышения надёжности
 */
@Slf4j
@EnableAutoConfiguration
@SuppressWarnings("LoggingSimilarMessage")
@DisplayName("Метрика Answer Accuracy - Валидация на русском языке")
@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".*")
@SpringBootTest(classes = RuAnswerAccuracyIntegrationIT.AnswerAccuracyIntegrationTestConfiguration.class)
class RuAnswerAccuracyIntegrationIT {

    @Configuration
    public static class AnswerAccuracyIntegrationTestConfiguration {}

    @Autowired
    private AnswerAccuracyMetric answerAccuracyMetric;

    @Nested
    @DisplayName("Тесты оценки точности")
    class AccuracyEvaluationTests {

        @Test
        @DisplayName("Полностью правильный ответ - ОЖИДАЕТСЯ ВЫСОКИЙ БАЛЛ")
        void testFullyCorrectResponse() {
            log.info("=== Тест: полностью правильный ответ ===");

            final Sample sample = Sample.builder()
                    .response("Москва — столица России.")
                    .reference("Москва — столица России.")
                    .build();

            final AnswerAccuracyMetric.AnswerAccuracyConfig config = AnswerAccuracyMetric.AnswerAccuracyConfig.builder()
                    .useDualJudge(false)
                    .build();

            final Double score = answerAccuracyMetric.singleTurnScore(config, sample);

            log.info("Ответ: Москва — столица России.");
            log.info("Эталон: Москва — столица России.");
            log.info("Оценка: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.7, "Полностью правильный ответ должен дать высокий балл. Получено: " + score);
        }

        @Test
        @DisplayName("Частично правильный ответ - ОЖИДАЕТСЯ УМЕРЕННЫЙ БАЛЛ")
        void testPartiallyCorrectResponse() {
            log.info("=== Тест: частично правильный ответ ===");

            final Sample sample = Sample.builder()
                    .response("Москва — крупный город в России, известный Кремлём.")
                    .reference("Москва — столица и крупнейший город России с населением более 12 миллионов человек.")
                    .build();

            final AnswerAccuracyMetric.AnswerAccuracyConfig config = AnswerAccuracyMetric.AnswerAccuracyConfig.builder()
                    .useDualJudge(false)
                    .build();

            final Double score = answerAccuracyMetric.singleTurnScore(config, sample);

            log.info("Ответ: Москва — крупный город в России...");
            log.info("Эталон: Москва — столица и крупнейший город России...");
            log.info("Оценка: {}", score);

            assertNotNull(score);
            assertTrue(
                    score >= 0.2 && score <= 0.9,
                    "Частично правильный ответ должен дать умеренный балл. Получено: " + score);
        }

        @Test
        @DisplayName("Неправильный ответ - ОЖИДАЕТСЯ НИЗКИЙ БАЛЛ")
        void testIncorrectResponse() {
            log.info("=== Тест: неправильный ответ ===");

            final Sample sample = Sample.builder()
                    .response("Санкт-Петербург — столица России.")
                    .reference("Москва — столица России.")
                    .build();

            final AnswerAccuracyMetric.AnswerAccuracyConfig config = AnswerAccuracyMetric.AnswerAccuracyConfig.builder()
                    .useDualJudge(false)
                    .build();

            final Double score = answerAccuracyMetric.singleTurnScore(config, sample);

            log.info("Ответ: Санкт-Петербург — столица России.");
            log.info("Эталон: Москва — столица России.");
            log.info("Оценка: {}", score);

            assertNotNull(score);
            assertTrue(score <= 0.4, "Неправильный ответ должен дать низкий балл. Получено: " + score);
        }
    }

    @Nested
    @DisplayName("Тесты режима двойного судьи")
    class DualJudgeModeTests {

        @Test
        @DisplayName("Оценка в режиме двойного судьи")
        void testDualJudgeMode() {
            log.info("=== Тест: режим двойного судьи ===");

            final Sample sample = Sample.builder()
                    .response("Кремль — знаменитая достопримечательность в Москве.")
                    .reference("Московский Кремль — исторический укреплённый комплекс в центре Москвы.")
                    .build();

            final AnswerAccuracyMetric.AnswerAccuracyConfig config = AnswerAccuracyMetric.AnswerAccuracyConfig.builder()
                    .useDualJudge(true)
                    .build();

            final Double score = answerAccuracyMetric.singleTurnScore(config, sample);

            log.info("Режим двойного судьи включён");
            log.info("Оценка: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.3, "Двойной судья должен корректно оценить. Получено: " + score);
        }
    }

    @Nested
    @DisplayName("Крайние случаи")
    class EdgeCasesTests {

        @Test
        @DisplayName("Технический ответ с техническим эталоном")
        void testTechnicalContent() {
            log.info("=== Тест: технический контент ===");

            final Sample sample = Sample.builder()
                    .response("TCP использует трёхстороннее рукопожатие: SYN, SYN-ACK, ACK.")
                    .reference(
                            "Трёхстороннее рукопожатие TCP состоит из пакетов SYN, SYN-ACK и ACK для установления соединения.")
                    .build();

            final AnswerAccuracyMetric.AnswerAccuracyConfig config = AnswerAccuracyMetric.AnswerAccuracyConfig.builder()
                    .useDualJudge(false)
                    .build();

            final Double score = answerAccuracyMetric.singleTurnScore(config, sample);

            log.info("Ответ: TCP использует трёхстороннее рукопожатие...");
            log.info("Оценка: {}", score);

            assertNotNull(score);
            assertTrue(
                    score >= 0.6,
                    "Семантически эквивалентный технический ответ должен дать хороший балл. Получено: " + score);
        }

        @Test
        @DisplayName("Асинхронная оценка работает корректно")
        void testAsyncScoring() {
            log.info("=== Тест: асинхронная оценка ===");

            final Sample sample = Sample.builder()
                    .response("Вода кипит при 100 градусах Цельсия.")
                    .reference("Температура кипения воды составляет 100°C на уровне моря.")
                    .build();

            final AnswerAccuracyMetric.AnswerAccuracyConfig config = AnswerAccuracyMetric.AnswerAccuracyConfig.builder()
                    .useDualJudge(false)
                    .build();

            final Double score =
                    answerAccuracyMetric.singleTurnScoreAsync(config, sample).join();

            log.info("Асинхронная оценка: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.6, "Асинхронная оценка должна работать корректно. Получено: " + score);
        }
    }
}
