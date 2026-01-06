package ai.qa.solutions.metrics.general.ru;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.general.SimpleCriteriaScoreMetric;
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
@SuppressWarnings("LoggingSimilarMessage")
@DisplayName("Интеграционные тесты общих метрик с русскоязычными примерами")
@SpringBootTest(classes = RuSimpleCriteriaMetricIntegrationTest.GeneralMetricsIntegrationTestConfiguration.class)
class RuSimpleCriteriaMetricIntegrationTest {

    @Configuration
    public static class GeneralMetricsIntegrationTestConfiguration {}

    @Autowired
    private SimpleCriteriaScoreMetric simpleCriteriaScoreMetric;

    @Test
    @DisplayName("SimpleCriteriaScore: Позитивный тест - высокое качество ответа")
    void testSimpleCriteriaScorePositive_HighQuality() {
        log.info("=== SimpleCriteriaScore: Позитивный тест ===");

        Sample sample = Sample.builder()
                .userInput("Объясните, что такое искусственный интеллект")
                .response("Искусственный интеллект (ИИ) — это область информатики, которая занимается "
                        + "созданием систем, способных выполнять задачи, обычно требующие человеческого "
                        + "интеллекта. Это включает обучение, рассуждение, восприятие и принятие решений. "
                        + "ИИ используется в различных областях: от медицины до автономных автомобилей.")
                .reference("Искусственный интеллект — это технология, имитирующая человеческое мышление "
                        + "для решения сложных задач.")
                .build();

        SimpleCriteriaScoreMetric.SimpleCriteriaConfig config = SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                .definition("Оцените качество объяснения от 1 до 5, " + "учитывая полноту, ясность и точность")
                .minScore(1.0)
                .maxScore(5.0)
                .strictness(2)
                .build();

        Double score = simpleCriteriaScoreMetric.singleTurnScore(config, sample);
        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Ответ: {}", sample.getResponse());
        log.info("Эталон: {}", sample.getReference());
        log.info("Нормализованная оценка: {} (0-1 шкала)", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0, "Оценка должна быть нормализована в диапазон [0, 1]");
        assertTrue(
                score >= 0.75,
                "Ожидается высокая нормализованная оценка для качественного объяснения, получен: " + score);
    }

    @Test
    @DisplayName("SimpleCriteriaScore: Негативный тест - низкое качество ответа")
    void testSimpleCriteriaScoreNegative_PoorQuality() {
        log.info("=== SimpleCriteriaScore: Негативный тест ===");

        Sample sample = Sample.builder()
                .userInput("Объясните принципы квантовой физики")
                .response("Квантовая физика это сложно. Там всякие частицы и волны. " + "Не знаю, что еще сказать.")
                .reference("Квантовая физика изучает поведение материи и энергии на атомном "
                        + "и субатомном уровне, где действуют принципы неопределенности и суперпозиции.")
                .build();

        SimpleCriteriaScoreMetric.SimpleCriteriaConfig config = SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                .definition("Оцените качество объяснения от 1 до 5, " + "учитывая полноту, ясность и научную точность")
                .minScore(1.0)
                .maxScore(5.0)
                .build();

        Double score = simpleCriteriaScoreMetric.singleTurnScore(config, sample);
        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Ответ: {}", sample.getResponse());
        log.info("Эталон: {}", sample.getReference());
        log.info("Нормализованная оценка: {} (0-1 шкала)", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0, "Оценка должна быть нормализована в диапазон [0, 1]");
        assertTrue(
                score <= 0.4, "Ожидается низкая нормализованная оценка для поверхностного ответа, получен: " + score);
    }

    @Test
    @DisplayName("SimpleCriteriaScore: Математическая точность - правильный ответ")
    void testSimpleCriteriaScore_MathAccuracy_CorrectAnswer() {
        log.info("=== SimpleCriteriaScore: Математическая точность - правильный ответ ===");

        Sample sample = Sample.builder()
                .userInput("Сколько будет 15 умножить на 12?")
                .response("15 умножить на 12 равно 180.")
                .reference("180")
                .build();

        SimpleCriteriaScoreMetric.SimpleCriteriaConfig config = SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                .definition("Оцените математическую точность от 0 до 5")
                .minScore(0.0)
                .maxScore(5.0)
                .build();

        Double score = simpleCriteriaScoreMetric.singleTurnScore(config, sample);
        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Ответ: {}", sample.getResponse());
        log.info("Эталон: {}", sample.getReference());
        log.info("Нормализованная оценка: {} (0-1 шкала)", score);

        assertTrue(score >= 0.9, "Правильный ответ должен получить высокую нормализованную оценку (>=0.9)");
    }

    @Test
    @DisplayName("SimpleCriteriaScore: Математическая точность - неправильный ответ")
    void testSimpleCriteriaScore_MathAccuracy_IncorrectAnswer() {
        log.info("=== SimpleCriteriaScore: Математическая точность - неправильный ответ ===");

        Sample sample = Sample.builder()
                .userInput("Сколько будет 15 умножить на 12?")
                .response("15 умножить на 12 равно 170.")
                .reference("180")
                .build();

        SimpleCriteriaScoreMetric.SimpleCriteriaConfig config = SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                .definition("Оцените математическую точность от 0 до 5")
                .minScore(0.0)
                .maxScore(5.0)
                .build();

        Double score = simpleCriteriaScoreMetric.singleTurnScore(config, sample);
        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Ответ: {}", sample.getResponse());
        log.info("Эталон: {}", sample.getReference());
        log.info("Нормализованная оценка: {} (0-1 шкала)", score);

        assertTrue(score <= 0.4, "Неправильный ответ должен получить низкую нормализованную оценку (<=0.4)");
    }
}
