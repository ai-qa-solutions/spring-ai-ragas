package ai.qa.solutions.metrics.response.ru;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.response.FactualCorrectnessMetric;
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
 * Интеграционные тесты для метрики Factual Correctness - Русский язык.
 * <p>
 * Тестирует фактическую точность ответов по сравнению с эталонными ответами.
 * Использует декомпозицию утверждений и NLI верификацию.
 */
@Slf4j
@EnableAutoConfiguration
@SuppressWarnings("LoggingSimilarMessage")
@DisplayName("Factual Correctness Metric - Russian Language Validation")
@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".*")
@SpringBootTest(classes = RuFactualCorrectnessIntegrationIT.FactualCorrectnessIntegrationTestConfiguration.class)
class RuFactualCorrectnessIntegrationIT {

    @Configuration
    public static class FactualCorrectnessIntegrationTestConfiguration {}

    @Autowired
    private FactualCorrectnessMetric factualCorrectnessMetric;

    @Test
    @DisplayName("Идентичные тексты - ОЖИДАЕТСЯ ОЧЕНЬ ВЫСОКИЙ БАЛЛ")
    void testFactualCorrectness_IdenticalTexts() {
        log.info("=== Тест идентичных текстов ===");

        Sample sample = Sample.builder()
                .response("Москва является столицей России. Кремль расположен в Москве.")
                .reference("Москва является столицей России. Кремль расположен в Москве.")
                .build();

        Double score = factualCorrectnessMetric.singleTurnScore(sample);

        log.info("Ответ: {}", sample.getResponse());
        log.info("Эталон: {}", sample.getReference());
        log.info("Балл: {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(
                score >= 0.8, "Идентичные тексты должны иметь очень высокую фактическую точность. Получено: " + score);
    }

    @Test
    @DisplayName("Корректное перефразирование - ОЖИДАЕТСЯ ВЫСОКИЙ БАЛЛ")
    void testFactualCorrectness_RephrasedCorrect() {
        log.info("=== Тест корректного перефразирования ===");

        Sample sample = Sample.builder()
                .response("Столица Российской Федерации - город Москва, где находится знаменитый Кремль.")
                .reference("Москва является столицей России. Кремль расположен в Москве.")
                .build();

        Double score = factualCorrectnessMetric.singleTurnScore(sample);

        log.info("Ответ: {}", sample.getResponse());
        log.info("Эталон: {}", sample.getReference());
        log.info("Балл: {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(score >= 0.6, "Перефразированный корректный ответ должен иметь высокий балл. Получено: " + score);
    }

    @Test
    @DisplayName("Частичная информация - ОЖИДАЕТСЯ СРЕДНИЙ БАЛЛ")
    void testFactualCorrectness_PartialInformation() {
        log.info("=== Тест частичной информации ===");

        Sample sample = Sample.builder()
                .response("Москва является столицей России.")
                .reference("Москва является столицей России. Кремль расположен в Москве. "
                        + "Москва также называется Белокаменной.")
                .build();

        Double score = factualCorrectnessMetric.singleTurnScore(sample);

        log.info("Ответ: {}", sample.getResponse());
        log.info("Эталон: {}", sample.getReference());
        log.info("Балл: {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(
                score >= 0.3 && score <= 0.8,
                "Частичная, но корректная информация должна иметь средний балл. Получено: " + score);
    }

    @Test
    @DisplayName("Фактически неверно - ОЖИДАЕТСЯ НИЗКИЙ БАЛЛ")
    void testFactualCorrectness_FactuallyIncorrect() {
        log.info("=== Тест фактически неверного ответа ===");

        Sample sample = Sample.builder()
                .response("Санкт-Петербург является столицей России. Кремль находится в Казани.")
                .reference("Москва является столицей России. Кремль расположен в Москве.")
                .build();

        Double score = factualCorrectnessMetric.singleTurnScore(sample);

        log.info("Ответ: {}", sample.getResponse());
        log.info("Эталон: {}", sample.getReference());
        log.info("Балл: {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(score <= 0.4, "Фактически неверный ответ должен иметь низкий балл. Получено: " + score);
    }

    @Test
    @DisplayName("Смешанные факты - ОЖИДАЕТСЯ СРЕДНИЙ БАЛЛ")
    void testFactualCorrectness_MixedCorrectIncorrect() {
        log.info("=== Тест смешанных фактов ===");

        Sample sample = Sample.builder()
                .response("Москва является столицей России. Кремль был построен в 1500 году.")
                .reference("Москва является столицей России. Кремль был построен в конце XV века.")
                .build();

        Double score = factualCorrectnessMetric.singleTurnScore(sample);

        log.info("Ответ: {}", sample.getResponse());
        log.info("Эталон: {}", sample.getReference());
        log.info("Балл: {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(score >= 0.2 && score <= 0.8, "Смешанные факты должны иметь промежуточный балл. Получено: " + score);
    }

    @Test
    @DisplayName("Режим PRECISION - точность")
    void testFactualCorrectness_PrecisionMode() {
        log.info("=== Тест режима Precision ===");

        Sample sample = Sample.builder()
                .response("Москва является столицей России. Кремль расположен в Москве. "
                        + "Население Москвы превышает 12 миллионов человек.")
                .reference("Москва является столицей России. Кремль расположен в Москве.")
                .build();

        FactualCorrectnessMetric.FactualCorrectnessConfig precisionConfig =
                FactualCorrectnessMetric.FactualCorrectnessConfig.builder()
                        .mode(FactualCorrectnessMetric.Mode.PRECISION)
                        .build();

        Double precisionScore = factualCorrectnessMetric.singleTurnScore(precisionConfig, sample);

        log.info("Ответ: {}", sample.getResponse());
        log.info("Эталон: {}", sample.getReference());
        log.info("Precision балл: {}", String.format("%.4f", precisionScore));

        assertNotNull(precisionScore);
        assertTrue(
                precisionScore >= 0.5,
                "Дополнительная корректная информация не должна снижать precision. Получено: " + precisionScore);
    }

    @Test
    @DisplayName("Режим RECALL - полнота")
    void testFactualCorrectness_RecallMode() {
        log.info("=== Тест режима Recall ===");

        Sample sample = Sample.builder()
                .response("Москва - столица.")
                .reference("Москва является столицей России. Кремль расположен в Москве.")
                .build();

        FactualCorrectnessMetric.FactualCorrectnessConfig recallConfig =
                FactualCorrectnessMetric.FactualCorrectnessConfig.builder()
                        .mode(FactualCorrectnessMetric.Mode.RECALL)
                        .build();

        Double recallScore = factualCorrectnessMetric.singleTurnScore(recallConfig, sample);

        log.info("Ответ: {}", sample.getResponse());
        log.info("Эталон: {}", sample.getReference());
        log.info("Recall балл: {}", String.format("%.4f", recallScore));

        assertNotNull(recallScore);
        assertTrue(recallScore <= 0.7, "Пропущенная информация должна снижать recall. Получено: " + recallScore);
    }

    @Test
    @DisplayName("Сложное фактическое сравнение")
    void testFactualCorrectness_ComplexFacts() {
        log.info("=== Тест сложного фактического сравнения ===");

        Sample sample = Sample.builder()
                .response("Александр Пушкин был великим русским поэтом. "
                        + "Он родился в 1799 году и написал роман в стихах «Евгений Онегин».")
                .reference("Александр Сергеевич Пушкин - величайший русский поэт. "
                        + "Родился в 1799 году. Автор романа в стихах «Евгений Онегин».")
                .build();

        Double score = factualCorrectnessMetric.singleTurnScore(sample);

        log.info("Ответ: {}", sample.getResponse());
        log.info("Эталон: {}", sample.getReference());
        log.info("Балл: {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(
                score >= 0.6, "Фактически согласованный сложный текст должен иметь хороший балл. Получено: " + score);
    }

    @Test
    @DisplayName("Асинхронная оценка работает корректно")
    void testFactualCorrectness_AsyncScoring() {
        log.info("=== Тест асинхронной оценки ===");

        Sample sample = Sample.builder()
                .response("Москва является столицей России.")
                .reference("Москва является столицей России.")
                .build();

        Double score = factualCorrectnessMetric.singleTurnScoreAsync(sample).join();

        log.info("Асинхронный балл: {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(score >= 0.7, "Асинхронная оценка должна работать идентично синхронной. Получено: " + score);
    }
}
