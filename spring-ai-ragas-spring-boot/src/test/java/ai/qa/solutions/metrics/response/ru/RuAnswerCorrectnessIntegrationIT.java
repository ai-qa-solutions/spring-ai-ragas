package ai.qa.solutions.metrics.response.ru;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.response.AnswerCorrectnessMetric;
import ai.qa.solutions.sample.Sample;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

/**
 * Интеграционные тесты для метрики Answer Correctness - Русский язык.
 * <p>
 * Тестирует комбинированную оценку фактической корректности (75%) и семантического сходства (25%).
 * <p>
 * Характеристики:
 * - Требует вызовы LLM для фактической корректности (декомпозиция claims + NLI)
 * - Требует вызовы embedding для семантического сходства
 * - Возвращает взвешенное среднее обоих компонентов
 */
@Slf4j
@EnableAutoConfiguration
@SuppressWarnings("LoggingSimilarMessage")
@DisplayName("Answer Correctness Metric - Russian Language Validation")
@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".*")
@SpringBootTest(classes = RuAnswerCorrectnessIntegrationIT.AnswerCorrectnessIntegrationTestConfiguration.class)
class RuAnswerCorrectnessIntegrationIT {

    @Configuration
    public static class AnswerCorrectnessIntegrationTestConfiguration {}

    @Autowired
    private AnswerCorrectnessMetric answerCorrectnessMetric;

    @Test
    @DisplayName("Идентичные тексты - ОЖИДАЕТСЯ ОЧЕНЬ ВЫСОКИЙ БАЛЛ")
    void testAnswerCorrectness_IdenticalTexts() {
        log.info("=== Тест идентичных текстов ===");

        Sample sample = Sample.builder()
                .response("Москва является столицей России. Кремль расположен в Москве.")
                .reference("Москва является столицей России. Кремль расположен в Москве.")
                .build();

        Double score = answerCorrectnessMetric.singleTurnScore(sample);

        log.info("Ответ: {}", sample.getResponse());
        log.info("Эталон: {}", sample.getReference());
        log.info("Балл: {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(
                score >= 0.8, "Идентичные тексты должны иметь очень высокую корректность ответа. Получено: " + score);
    }

    @Test
    @DisplayName("Корректное перефразирование - ОЖИДАЕТСЯ ВЫСОКИЙ БАЛЛ")
    void testAnswerCorrectness_RephrasedCorrect() {
        log.info("=== Тест корректного перефразирования ===");

        Sample sample = Sample.builder()
                .response("Столица Российской Федерации - город Москва, где находится знаменитый Кремль.")
                .reference("Москва является столицей России. Кремль расположен в Москве.")
                .build();

        Double score = answerCorrectnessMetric.singleTurnScore(sample);

        log.info("Ответ: {}", sample.getResponse());
        log.info("Эталон: {}", sample.getReference());
        log.info("Балл: {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(score >= 0.6, "Перефразированный корректный ответ должен иметь высокий балл. Получено: " + score);
    }

    @Test
    @DisplayName("Частичная информация - ОЖИДАЕТСЯ СРЕДНИЙ БАЛЛ")
    void testAnswerCorrectness_PartialInformation() {
        log.info("=== Тест частичной информации ===");

        Sample sample = Sample.builder()
                .response("Москва является столицей России.")
                .reference("Москва является столицей России. Кремль расположен в Москве. "
                        + "Москва также называется Белокаменной.")
                .build();

        Double score = answerCorrectnessMetric.singleTurnScore(sample);

        log.info("Ответ: {}", sample.getResponse());
        log.info("Эталон: {}", sample.getReference());
        log.info("Балл: {}", String.format("%.4f", score));

        assertNotNull(score);
        // Фактическая корректность (высокая) + семантическое сходство (среднее) = умеренно-высокий
        assertTrue(
                score >= 0.3 && score <= 0.85,
                "Частичная, но корректная информация должна иметь средний балл. Получено: " + score);
    }

    @Test
    @DisplayName("Фактически неверно - ОЖИДАЕТСЯ НИЗКИЙ БАЛЛ")
    void testAnswerCorrectness_FactuallyIncorrect() {
        log.info("=== Тест фактически неверного ответа ===");

        Sample sample = Sample.builder()
                .response("Санкт-Петербург является столицей России. Кремль находится в Казани.")
                .reference("Москва является столицей России. Кремль расположен в Москве.")
                .build();

        Double score = answerCorrectnessMetric.singleTurnScore(sample);

        log.info("Ответ: {}", sample.getResponse());
        log.info("Эталон: {}", sample.getReference());
        log.info("Балл: {}", String.format("%.4f", score));

        assertNotNull(score);
        // И фактическая, и семантическая корректность должны быть низкими
        assertTrue(score <= 0.5, "Фактически неверный ответ должен иметь низкий балл. Получено: " + score);
    }

    @Test
    @DisplayName("Смешанные факты - ОЖИДАЕТСЯ СРЕДНИЙ БАЛЛ")
    void testAnswerCorrectness_MixedCorrectIncorrect() {
        log.info("=== Тест смешанных фактов ===");

        Sample sample = Sample.builder()
                .response("Москва является столицей России. Кремль был построен в 1500 году.")
                .reference("Москва является столицей России. Кремль был построен в конце XV века.")
                .build();

        Double score = answerCorrectnessMetric.singleTurnScore(sample);

        log.info("Ответ: {}", sample.getResponse());
        log.info("Эталон: {}", sample.getReference());
        log.info("Балл: {}", String.format("%.4f", score));

        assertNotNull(score);
        // Одно корректное утверждение, одно некорректное, но семантически схоже
        assertTrue(score >= 0.2 && score <= 0.8, "Смешанные факты должны иметь промежуточный балл. Получено: " + score);
    }

    @Test
    @DisplayName("Настраиваемые веса - фокус на фактах")
    void testAnswerCorrectness_FactualFocused() {
        log.info("=== Тест с фокусом на фактах ===");

        Sample sample = Sample.builder()
                .response("Москва является столицей России. Кремль расположен в Москве.")
                .reference("Москва является столицей России. Кремль расположен в Москве.")
                .build();

        AnswerCorrectnessMetric.AnswerCorrectnessConfig config =
                AnswerCorrectnessMetric.AnswerCorrectnessConfig.factualFocused();

        Double score = answerCorrectnessMetric.singleTurnScore(config, sample);

        log.info("Ответ: {}", sample.getResponse());
        log.info("Эталон: {}", sample.getReference());
        log.info("Балл с фокусом на фактах (90/10): {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(score >= 0.5, "Корректный ответ с фокусом на фактах должен иметь хороший балл. Получено: " + score);
    }

    @Test
    @DisplayName("Настраиваемые веса - фокус на семантике")
    void testAnswerCorrectness_SemanticFocused() {
        log.info("=== Тест с фокусом на семантике ===");

        Sample sample = Sample.builder()
                .response("Столица Российской Федерации - Москва, в которой находится исторический Кремль.")
                .reference("Москва является столицей России. Кремль расположен в Москве.")
                .build();

        AnswerCorrectnessMetric.AnswerCorrectnessConfig config =
                AnswerCorrectnessMetric.AnswerCorrectnessConfig.semanticFocused();

        Double score = answerCorrectnessMetric.singleTurnScore(config, sample);

        log.info("Ответ: {}", sample.getResponse());
        log.info("Эталон: {}", sample.getReference());
        log.info("Балл с фокусом на семантике (10/90): {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(
                score >= 0.5,
                "Семантически схожий ответ с фокусом на семантике должен иметь хороший балл. Получено: " + score);
    }

    @Test
    @DisplayName("Сложное фактическое сравнение")
    void testAnswerCorrectness_ComplexFacts() {
        log.info("=== Тест сложного фактического сравнения ===");

        Sample sample = Sample.builder()
                .response("Александр Пушкин был великим русским поэтом. "
                        + "Он родился в 1799 году и написал роман в стихах «Евгений Онегин».")
                .reference("Александр Сергеевич Пушкин - величайший русский поэт. "
                        + "Родился в 1799 году. Автор романа в стихах «Евгений Онегин».")
                .build();

        Double score = answerCorrectnessMetric.singleTurnScore(sample);

        log.info("Ответ: {}", sample.getResponse());
        log.info("Эталон: {}", sample.getReference());
        log.info("Балл: {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(
                score >= 0.6, "Фактически согласованный сложный текст должен иметь хороший балл. Получено: " + score);
    }

    @Test
    @DisplayName("Асинхронная оценка работает корректно")
    void testAnswerCorrectness_AsyncScoring() {
        log.info("=== Тест асинхронной оценки ===");

        Sample sample = Sample.builder()
                .response("Москва является столицей России.")
                .reference("Москва является столицей России.")
                .build();

        Double score = answerCorrectnessMetric.singleTurnScoreAsync(sample).join();

        log.info("Асинхронный балл: {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(score >= 0.7, "Асинхронная оценка должна работать идентично синхронной. Получено: " + score);
    }
}
