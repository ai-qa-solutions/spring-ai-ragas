package ai.qa.solutions.metrics.general.ru;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.general.SimpleCriteriaScoreMetric;
import ai.qa.solutions.sample.Sample;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
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

    public static Stream<Arguments> models() {
        return Stream.of(
                Arguments.of("x-ai/grok-code-fast-1"),
                Arguments.of("x-ai/grok-4.1-fast"),
                Arguments.of("google/gemini-2.5-flash"),
                Arguments.of("google/gemini-2.5-pro"),
                Arguments.of("minimax/minimax-m2"),
                Arguments.of("anthropic/claude-sonnet-4.5"),
                Arguments.of("anthropic/claude-haiku-4.5"),
                Arguments.of("deepseek/deepseek-chat-v3-0324"),
                Arguments.of("deepseek/deepseek-chat-v3.1"),
                Arguments.of("qwen/qwen3-235b-a22b-2507"),
                Arguments.of("qwen/qwen3-coder-30b-a3b-instruct"),
                Arguments.of("z-ai/glm-4.6"),
                Arguments.of("openai/gpt-5-mini"),
                Arguments.of("openai/gpt-5.1"),
                Arguments.of("openai/gpt-4o-mini"),
                Arguments.of("openai/gpt-oss-120b"),
                Arguments.of("openai/gpt-oss-20b"),
                Arguments.of("GigaChat-2"),
                Arguments.of("GigaChat-2-Pro"),
                Arguments.of("GigaChat-2-Max"));
    }

    @Configuration
    public static class GeneralMetricsIntegrationTestConfiguration {}

    @Autowired
    private SimpleCriteriaScoreMetric simpleCriteriaScoreMetric;

    @Autowired(required = false)
    private OpenAiApi openAiApi;

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @ParameterizedTest
    @MethodSource("models")
    @DisplayName("SimpleCriteriaScore: Позитивный тест - высокое качество ответа")
    void testSimpleCriteriaScorePositive_HighQuality(String model) {
        if (openAiApi == null && model.contains("/")) return;
        if (openAiApi != null && !model.contains("/")) return;

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
                .build();

        SimpleCriteriaScoreMetric simpleCriteriaScoreMetricWithModel = simpleCriteriaScoreMetric.toBuilder()
                .chatClient(chatClientBuilder
                        .defaultAdvisors(new SimpleLoggerAdvisor())
                        .defaultOptions(ChatOptions.builder()
                                .model(model)
                                .temperature(0.0)
                                .build())
                        .build())
                .build();

        Double score = simpleCriteriaScoreMetricWithModel.singleTurnScore(config, sample);

        log.info("Модель: {}", model);
        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Ответ: {}", sample.getResponse());
        log.info("Эталон: {}", sample.getReference());
        log.info("Оценка качества: {} (1-5 шкала)", score);

        assertNotNull(score);
        assertTrue(score >= 1.0 && score <= 5.0);
        assertTrue(score >= 4.0, "Ожидается высокая оценка для качественного объяснения, получен: " + score);
    }

    @ParameterizedTest
    @MethodSource("models")
    @DisplayName("SimpleCriteriaScore: Негативный тест - низкое качество ответа")
    void testSimpleCriteriaScoreNegative_PoorQuality(String model) {
        if (openAiApi == null && model.contains("/")) return;
        if (openAiApi != null && !model.contains("/")) return;

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

        SimpleCriteriaScoreMetric simpleCriteriaScoreMetricWithModel = simpleCriteriaScoreMetric.toBuilder()
                .chatClient(chatClientBuilder
                        .defaultAdvisors(new SimpleLoggerAdvisor())
                        .defaultOptions(ChatOptions.builder()
                                .model(model)
                                .temperature(0.0)
                                .build())
                        .build())
                .build();

        Double score = simpleCriteriaScoreMetricWithModel.singleTurnScore(config, sample);

        log.info("Модель: {}", model);
        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Ответ: {}", sample.getResponse());
        log.info("Эталон: {}", sample.getReference());
        log.info("Оценка качества: {} (1-5 шкала)", score);

        assertNotNull(score);
        assertTrue(score >= 1.0 && score <= 5.0);
        assertTrue(score <= 2.5, "Ожидается низкая оценка для поверхностного ответа, получен: " + score);
    }

    @ParameterizedTest
    @MethodSource("models")
    @DisplayName("SimpleCriteriaScore: Тест математической точности")
    void testSimpleCriteriaScore_MathAccuracy(String model) {
        if (openAiApi == null && model.contains("/")) return;
        if (openAiApi != null && !model.contains("/")) return;

        log.info("=== SimpleCriteriaScore: Математическая точность ===");

        // Правильный ответ
        Sample correctSample = Sample.builder()
                .userInput("Сколько будет 15 умножить на 12?")
                .response("15 умножить на 12 равно 180.")
                .reference("180")
                .build();

        // Неправильный ответ
        Sample incorrectSample = Sample.builder()
                .userInput("Сколько будет 15 умножить на 12?")
                .response("15 умножить на 12 равно 170.")
                .reference("180")
                .build();

        SimpleCriteriaScoreMetric.SimpleCriteriaConfig config = SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                .definition("Оцените математическую точность от 0 до 5")
                .minScore(0.0)
                .maxScore(5.0)
                .build();

        SimpleCriteriaScoreMetric simpleCriteriaScoreMetricWithModel = simpleCriteriaScoreMetric.toBuilder()
                .chatClient(chatClientBuilder
                        .defaultAdvisors(new SimpleLoggerAdvisor())
                        .defaultOptions(ChatOptions.builder()
                                .model(model)
                                .temperature(0.0)
                                .build())
                        .build())
                .build();

        Double correctScore = simpleCriteriaScoreMetricWithModel.singleTurnScore(config, correctSample);
        log.info("Модель: {}", model);
        log.info("Вопрос: {}", correctSample.getUserInput());
        log.info("Ответ: {}", correctSample.getResponse());
        log.info("Эталон: {}", correctSample.getReference());
        log.info("Оценка качества: {} (1-5 шкала)", correctScore);

        Double incorrectScore = simpleCriteriaScoreMetricWithModel.singleTurnScore(config, incorrectSample);
        log.info("Модель: {}", model);
        log.info("Вопрос: {}", incorrectSample.getUserInput());
        log.info("Ответ: {}", incorrectSample.getResponse());
        log.info("Эталон: {}", incorrectSample.getReference());
        log.info("Оценка качества: {} (1-5 шкала)", incorrectScore);

        log.info("Правильный ответ - оценка: {}", correctScore);
        log.info("Неправильный ответ - оценка: {}", incorrectScore);

        assertTrue(correctScore >= 4.5, "Правильный ответ должен получить высокую оценку");
        assertTrue(incorrectScore <= 2.0, "Неправильный ответ должен получить низкую оценку");
        assertTrue(correctScore > incorrectScore, "Правильный ответ должен оцениваться выше неправильного");
    }
}
