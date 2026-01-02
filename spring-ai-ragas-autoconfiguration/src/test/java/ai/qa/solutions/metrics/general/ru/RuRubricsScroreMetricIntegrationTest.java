package ai.qa.solutions.metrics.general.ru;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.general.RubricsScoreMetric;
import ai.qa.solutions.sample.Sample;
import java.util.Map;
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
@SpringBootTest(classes = RuRubricsScroreMetricIntegrationTest.GeneralMetricsIntegrationTestConfiguration.class)
class RuRubricsScroreMetricIntegrationTest {

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
    private RubricsScoreMetric rubricsScoreMetric;

    @Autowired(required = false)
    private OpenAiApi openAiApi;

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @ParameterizedTest
    @MethodSource("models")
    @DisplayName("RubricsScore: Позитивный тест - отличное объяснение")
    void testRubricsScorePositive_ExcellentExplanation(String model) {
        if (openAiApi == null && model.contains("/")) return;
        if (openAiApi != null && !model.contains("/")) return;

        log.info("=== RubricsScore: Позитивный тест ===");

        Sample sample = Sample.builder()
                .userInput("Объясните процесс фотосинтеза")
                .response("Фотосинтез — это сложный биохимический процесс, в ходе которого растения "
                        + "преобразуют световую энергию в химическую. Процесс происходит в хлоропластах "
                        + "и включает две основные стадии: световую и темновую фазы. В световой фазе "
                        + "хлорофилл поглощает солнечный свет, расщепляя молекулы воды и выделяя кислород. "
                        + "В темновой фазе (цикл Кальвина) углекислый газ из атмосферы превращается в глюкозу. "
                        + "Общее уравнение: 6CO₂ + 6H₂O + световая энергия → C₆H₁₂O₆ + 6O₂.")
                .reference("Фотосинтез - процесс образования органических веществ из CO₂ и воды "
                        + "с использованием световой энергии.")
                .build();

        RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                .rubrics(createPhotosynthesisRubrics())
                .build();

        RubricsScoreMetric rubricsScoreMetricWithModel = rubricsScoreMetric.toBuilder()
                .chatClient(chatClientBuilder
                        .defaultAdvisors(new SimpleLoggerAdvisor())
                        .defaultOptions(ChatOptions.builder()
                                .model(model)
                                .temperature(0.0)
                                .build())
                        .build())
                .build();

        Double score = rubricsScoreMetricWithModel.singleTurnScore(config, sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Ответ: {}", sample.getResponse());
        log.info("Оценка по рубрикам: {}", score);

        assertNotNull(score);
        assertTrue(score >= 1.0 && score <= 5.0);
        assertTrue(score >= 4.0, "Ожидается высокая оценка для подробного научного объяснения, получен: " + score);
    }

    @ParameterizedTest
    @MethodSource("models")
    @DisplayName("RubricsScore: Негативный тест - поверхностное объяснение")
    void testRubricsScoreNegative_SuperficialExplanation(String model) {
        if (openAiApi == null && model.contains("/")) return;
        if (openAiApi != null && !model.contains("/")) return;

        log.info("=== RubricsScore: Негативный тест ===");

        Sample sample = Sample.builder()
                .userInput("Объясните процесс фотосинтеза")
                .response("Фотосинтез это когда растения что-то делают со светом. "
                        + "Они как-то используют солнце для роста.")
                .reference("Фотосинтез - процесс образования органических веществ из CO₂ и воды "
                        + "с использованием световой энергии.")
                .build();

        RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                .rubrics(createPhotosynthesisRubrics())
                .build();

        RubricsScoreMetric rubricsScoreMetricWithModel = rubricsScoreMetric.toBuilder()
                .chatClient(chatClientBuilder
                        .defaultAdvisors(new SimpleLoggerAdvisor())
                        .defaultOptions(ChatOptions.builder()
                                .model(model)
                                .temperature(0.0)
                                .build())
                        .build())
                .build();

        Double score = rubricsScoreMetricWithModel.singleTurnScore(config, sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Ответ: {}", sample.getResponse());
        log.info("Оценка по рубрикам: {}", score);

        assertNotNull(score);
        assertTrue(score >= 1.0 && score <= 5.0);
        assertTrue(score <= 2.0, "Ожидается низкая оценка для поверхностного объяснения, получен: " + score);
    }

    @ParameterizedTest
    @MethodSource("models")
    @DisplayName("RubricsScore: Тест оценки эссе")
    void testRubricsScore_EssayEvaluation(String model) {
        if (openAiApi == null && model.contains("/")) return;
        if (openAiApi != null && !model.contains("/")) return;

        log.info("=== RubricsScore: Оценка эссе ===");

        // Хорошее эссе
        Sample goodEssay = Sample.builder()
                .userInput("Напишите эссе о влиянии технологий на общество")
                .response("Технологический прогресс кардинально изменил современное общество. "
                        + "С одной стороны, цифровые технологии обеспечили беспрецедентные возможности "
                        + "для коммуникации, образования и доступа к информации. Интернет объединил мир, "
                        + "позволив людям мгновенно обмениваться идеями независимо от географических границ. "
                        + "С другой стороны, возникли новые вызовы: цифровое неравенство, зависимость от "
                        + "технологий и вопросы приватности. Необходим баланс между инновациями и "
                        + "социальной ответственностью для устойчивого развития.")
                .reference("Эссе о влиянии технологий на общество с примерами и аргументацией")
                .build();

        // Слабое эссе
        Sample weakEssay = Sample.builder()
                .userInput("Напишите эссе о влиянии технологий на общество")
                .response("Технологии хорошие. Есть телефоны и компьютеры. " + "Люди используют интернет. Это удобно.")
                .reference("Эссе о влиянии технологий на общество с примерами и аргументацией")
                .build();

        RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                .rubrics(createEssayRubrics())
                .build();

        RubricsScoreMetric rubricsScoreMetricWithModel = rubricsScoreMetric.toBuilder()
                .chatClient(chatClientBuilder
                        .defaultAdvisors(new SimpleLoggerAdvisor())
                        .defaultOptions(ChatOptions.builder()
                                .model(model)
                                .temperature(0.0)
                                .build())
                        .build())
                .build();

        Double goodScore = rubricsScoreMetricWithModel.singleTurnScore(config, goodEssay);
        Double weakScore = rubricsScoreMetricWithModel.singleTurnScore(config, weakEssay);

        log.info("Хорошее эссе - оценка: {}", goodScore);
        log.info("Слабое эссе - оценка: {}", weakScore);

        assertTrue(goodScore >= 2.0, "Хорошее эссе должно получить высокую оценку");
        assertTrue(weakScore <= 2.0, "Слабое эссе должно получить низкую оценку");
        assertTrue(goodScore > weakScore, "Хорошее эссе должно оцениваться выше слабого");
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================
    private Map<String, String> createPhotosynthesisRubrics() {
        return Map.of(
                "score1_description", "Полностью неверная или нерелевантная информация о фотосинтезе",
                "score2_description", "Базовое понимание с существенными пробелами или ошибками",
                "score3_description", "Общее понимание процесса, но отсутствуют важные детали",
                "score4_description", "Хорошее понимание с упоминанием основных этапов и компонентов",
                "score5_description", "Отличное объяснение с научными деталями, уравнением и примерами");
    }

    private Map<String, String> createEssayRubrics() {
        return Map.of(
                "score1_description", "Отсутствует структура, нет аргументов, множество ошибок",
                "score2_description", "Слабая структура, поверхностные аргументы, есть ошибки",
                "score3_description", "Базовая структура, некоторые аргументы, в целом понятно",
                "score4_description", "Хорошая структура, убедительные аргументы, качественное изложение",
                "score5_description", "Отличная структура, глубокий анализ, примеры, безупречное изложение");
    }
}
