package ai.qa.solutions.metrics.response.ru;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.response.SemanticSimilarityMetric;
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
 * Интеграционные тесты для метрики Semantic Similarity - Русский язык.
 * <p>
 * Тестирует косинусное сходство между эмбеддингами ответа и референса.
 * Эта метрика НЕ использует LLM вызовы, только модели эмбеддингов.
 */
@Slf4j
@EnableAutoConfiguration
@SuppressWarnings("LoggingSimilarMessage")
@DisplayName("Semantic Similarity Metric - Russian Language Validation")
@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".*")
@SpringBootTest(classes = RuSemanticSimilarityIntegrationIT.SemanticSimilarityIntegrationTestConfiguration.class)
class RuSemanticSimilarityIntegrationIT {

    @Configuration
    public static class SemanticSimilarityIntegrationTestConfiguration {}

    @Autowired
    private SemanticSimilarityMetric semanticSimilarityMetric;

    @Test
    @DisplayName("Идентичные тексты - ОЖИДАЕТСЯ ОЧЕНЬ ВЫСОКИЙ БАЛЛ")
    void testSemanticSimilarity_IdenticalTexts() {
        log.info("=== Тест идентичных текстов ===");

        Sample sample = Sample.builder()
                .response("Москва является столицей России.")
                .reference("Москва является столицей России.")
                .build();

        Double score = semanticSimilarityMetric.singleTurnScore(sample);

        log.info("Ответ: {}", sample.getResponse());
        log.info("Референс: {}", sample.getReference());
        log.info("Балл: {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(score >= 0.99, "Идентичные тексты должны иметь почти идеальное сходство. Получено: " + score);
    }

    @Test
    @DisplayName("Одинаковый смысл, разные слова - ОЖИДАЕТСЯ ВЫСОКИЙ БАЛЛ")
    void testSemanticSimilarity_SameMeaningDifferentWording() {
        log.info("=== Тест одинакового смысла, разных слов ===");

        Sample sample = Sample.builder()
                .response("Столицей Российской Федерации является город Москва.")
                .reference("Москва - главный город России.")
                .build();

        Double score = semanticSimilarityMetric.singleTurnScore(sample);

        log.info("Ответ: {}", sample.getResponse());
        log.info("Референс: {}", sample.getReference());
        log.info("Балл: {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(score >= 0.75, "Одинаковый смысл должен иметь высокое сходство. Получено: " + score);
    }

    @Test
    @DisplayName("Связанные, но разные темы - ОЖИДАЕТСЯ СРЕДНИЙ БАЛЛ")
    void testSemanticSimilarity_RelatedButDifferent() {
        log.info("=== Тест связанных, но разных тем ===");

        Sample sample = Sample.builder()
                .response("Россия - самая большая страна в мире по площади.")
                .reference("Москва является столицей России и крупнейшим городом страны.")
                .build();

        Double score = semanticSimilarityMetric.singleTurnScore(sample);

        log.info("Ответ: {}", sample.getResponse());
        log.info("Референс: {}", sample.getReference());
        log.info("Балл: {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(score >= 0.40 && score <= 0.85, "Связанные темы должны иметь среднее сходство. Получено: " + score);
    }

    @Test
    @DisplayName("Несвязанные тексты - ОЖИДАЕТСЯ НИЗКИЙ БАЛЛ")
    void testSemanticSimilarity_UnrelatedTexts() {
        log.info("=== Тест несвязанных текстов ===");

        Sample sample = Sample.builder()
                .response("Искусственный интеллект - это область компьютерных наук.")
                .reference("Великая Китайская стена была построена в древние времена.")
                .build();

        Double score = semanticSimilarityMetric.singleTurnScore(sample);

        log.info("Ответ: {}", sample.getResponse());
        log.info("Референс: {}", sample.getReference());
        log.info("Балл: {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(score <= 0.60, "Несвязанные тексты должны иметь низкое сходство. Получено: " + score);
    }

    @Test
    @DisplayName("Противоречивые утверждения - ОЖИДАЕТСЯ СРЕДНИЙ/ВЫСОКИЙ БАЛЛ")
    void testSemanticSimilarity_ContradictoryStatements() {
        log.info("=== Тест противоречивых утверждений ===");

        Sample sample = Sample.builder()
                .response("Земля плоская.")
                .reference("Земля имеет форму шара.")
                .build();

        Double score = semanticSimilarityMetric.singleTurnScore(sample);

        log.info("Ответ: {}", sample.getResponse());
        log.info("Референс: {}", sample.getReference());
        log.info("Балл: {}", String.format("%.4f", score));

        assertNotNull(score);
        // Противоречивые утверждения могут быть семантически похожи,
        // так как обсуждают одну тему с похожей лексикой
        assertTrue(
                score >= 0.50,
                "Противоречивые утверждения на одну тему могут быть семантически похожи. Получено: " + score);
        log.warn("Примечание: Семантическое сходство НЕ обнаруживает фактические противоречия");
    }

    @Test
    @DisplayName("Длинный vs короткий ответ на одну тему - ОЖИДАЕТСЯ СРЕДНИЙ БАЛЛ")
    void testSemanticSimilarity_LongVsShort() {
        log.info("=== Тест длинного vs короткого ответа ===");

        Sample sample = Sample.builder()
                .response("Москва - столица Российской Федерации, крупнейший город страны, "
                        + "важный политический, экономический, культурный и научный центр. "
                        + "Город расположен на реке Москве и является домом для Кремля.")
                .reference("Москва - столица России.")
                .build();

        Double score = semanticSimilarityMetric.singleTurnScore(sample);

        log.info("Длина ответа: {} символов", sample.getResponse().length());
        log.info("Длина референса: {} символов", sample.getReference().length());
        log.info("Балл: {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(
                score >= 0.65,
                "Длинный ответ, содержащий информацию референса, должен быть похожим. Получено: " + score);
    }

    @Test
    @DisplayName("Научный vs разговорный стиль - ОЖИДАЕТСЯ СРЕДНИЙ/ВЫСОКИЙ БАЛЛ")
    void testSemanticSimilarity_ScientificVsCasual() {
        log.info("=== Тест научного vs разговорного стиля ===");

        Sample sample = Sample.builder()
                .response("Процесс фотосинтеза преобразует солнечную радиацию в химическую энергию.")
                .reference("Растения используют солнечный свет для производства пищи.")
                .build();

        Double score = semanticSimilarityMetric.singleTurnScore(sample);

        log.info("Ответ: {}", sample.getResponse());
        log.info("Референс: {}", sample.getReference());
        log.info("Балл: {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(
                score >= 0.45,
                "Научное и разговорное описание одного концепта должны быть умеренно похожи. Получено: " + score);
    }

    @Test
    @DisplayName("С порогом - бинарный результат")
    void testSemanticSimilarity_WithThreshold() {
        log.info("=== Тест с порогом ===");

        Sample sample = Sample.builder()
                .response("Москва является столицей России.")
                .reference("Столица России - Москва.")
                .build();

        SemanticSimilarityMetric.SemanticSimilarityConfig configHighThreshold =
                SemanticSimilarityMetric.SemanticSimilarityConfig.builder()
                        .threshold(0.95)
                        .build();

        Double scoreHighThreshold = semanticSimilarityMetric.singleTurnScore(configHighThreshold, sample);

        SemanticSimilarityMetric.SemanticSimilarityConfig configLowThreshold =
                SemanticSimilarityMetric.SemanticSimilarityConfig.builder()
                        .threshold(0.70)
                        .build();

        Double scoreLowThreshold = semanticSimilarityMetric.singleTurnScore(configLowThreshold, sample);

        log.info("Балл с порогом 0.95: {}", scoreHighThreshold);
        log.info("Балл с порогом 0.70: {}", scoreLowThreshold);

        assertNotNull(scoreHighThreshold);
        assertNotNull(scoreLowThreshold);

        // С порогом баллы должны быть бинарными (0.0 или 1.0)
        assertTrue(
                scoreHighThreshold == 0.0 || scoreHighThreshold == 1.0,
                "С порогом балл должен быть бинарным. Получено: " + scoreHighThreshold);
        assertTrue(
                scoreLowThreshold == 0.0 || scoreLowThreshold == 1.0,
                "С порогом балл должен быть бинарным. Получено: " + scoreLowThreshold);
    }

    @Test
    @DisplayName("Пустой ответ - ОЖИДАЕТСЯ НУЛЕВОЙ БАЛЛ")
    void testSemanticSimilarity_EmptyResponse() {
        log.info("=== Тест пустого ответа ===");

        Sample sample = Sample.builder()
                .response("")
                .reference("Москва является столицей России.")
                .build();

        Double score = semanticSimilarityMetric.singleTurnScore(sample);

        log.info("Балл: {}", score);

        assertNotNull(score);
        assertTrue(score == 0.0, "Пустой ответ должен возвращать 0.0. Получено: " + score);
    }

    @Test
    @DisplayName("Пустой референс - ОЖИДАЕТСЯ НУЛЕВОЙ БАЛЛ")
    void testSemanticSimilarity_EmptyReference() {
        log.info("=== Тест пустого референса ===");

        Sample sample = Sample.builder()
                .response("Москва является столицей России.")
                .reference("")
                .build();

        Double score = semanticSimilarityMetric.singleTurnScore(sample);

        log.info("Балл: {}", score);

        assertNotNull(score);
        assertTrue(score == 0.0, "Пустой референс должен возвращать 0.0. Получено: " + score);
    }

    @Test
    @DisplayName("Асинхронная оценка работает корректно")
    void testSemanticSimilarity_AsyncScoring() {
        log.info("=== Тест асинхронной оценки ===");

        Sample sample = Sample.builder()
                .response("Москва является столицей России.")
                .reference("Москва является столицей России.")
                .build();

        Double score = semanticSimilarityMetric.singleTurnScoreAsync(sample).join();

        log.info("Асинхронный балл: {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(score >= 0.99, "Асинхронная оценка должна работать идентично синхронной. Получено: " + score);
    }
}
