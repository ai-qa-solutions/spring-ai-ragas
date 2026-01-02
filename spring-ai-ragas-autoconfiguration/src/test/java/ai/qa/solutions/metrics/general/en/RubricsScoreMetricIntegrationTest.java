package ai.qa.solutions.metrics.general.en;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.general.RubricsScoreMetric;
import ai.qa.solutions.sample.Sample;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
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
@DisplayName("Integration tests for general purpose metrics with English examples")
@SpringBootTest(classes = RubricsScoreMetricIntegrationTest.GeneralMetricsIntegrationTestConfiguration.class)
class RubricsScoreMetricIntegrationTest {

    @Configuration
    public static class GeneralMetricsIntegrationTestConfiguration {}

    @Autowired
    private RubricsScoreMetric rubricsScoreMetric;

    @Autowired(required = false)
    private OpenAiApi openAiApi;

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @ParameterizedTest
    @MethodSource("ai.qa.solutions.metrics.ModelsProvider#models")
    @DisplayName("RubricsScore: Positive test - excellent explanation")
    void testRubricsScorePositive_ExcellentExplanation(String model) {
        log.info("=== RubricsScore: Positive test ===");

        Sample sample = Sample.builder()
                .userInput("Explain the process of photosynthesis")
                .response("Photosynthesis is a complex biochemical process by which plants convert light energy "
                        + "into chemical energy. The process occurs in chloroplasts and includes two main stages:"
                        + "light-dependent and light-independent reactions. In the light-dependent phase, "
                        + "chlorophyll absorbs sunlight, splitting water molecules and releasing oxygen. "
                        + "In the light-independent phase (Calvin cycle), carbon dioxide from the atmosphere "
                        + "is converted into glucose. The overall equation: 6CO₂ + 6H₂O + light energy → C₆H₁₂O₆+6 O₂.")
                .reference("Photosynthesis is the process of forming organic substances from CO₂ and water "
                        + "using light energy.")
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

        log.info("Question: {}", sample.getUserInput());
        log.info("Response: {}", sample.getResponse());
        log.info("Rubrics score: {}", score);

        assertTrue(score >= 1.0 && score <= 5.0);
        assertTrue(score >= 4.0, "Expected high score for detailed scientific explanation, got: " + score);
    }

    @ParameterizedTest
    @MethodSource("models")
    @DisplayName("RubricsScore: Negative test - superficial explanation")
    void testRubricsScoreNegative_SuperficialExplanation(String model) {
        log.info("=== RubricsScore: Negative test ===");

        Sample sample = Sample.builder()
                .userInput("Explain the process of photosynthesis")
                .response(
                        "Photosynthesis is when plants do something with light. " + "They somehow use the sun to grow.")
                .reference("Photosynthesis is the process of forming organic substances from CO₂ and water "
                        + "using light energy.")
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

        log.info("Question: {}", sample.getUserInput());
        log.info("Response: {}", sample.getResponse());
        log.info("Rubrics score: {}", score);

        assertTrue(score >= 1.0 && score <= 5.0);
        assertTrue(score <= 2.0, "Expected low score for superficial explanation, got: " + score);
    }

    @ParameterizedTest
    @MethodSource("ai.qa.solutions.metrics.ModelsProvider#models")
    @DisplayName("RubricsScore: Essay evaluation test")
    void testRubricsScore_EssayEvaluation(String model) {
        log.info("=== RubricsScore: Essay evaluation ===");

        // Good essay
        Sample goodEssay = Sample.builder()
                .userInput("Write an essay about the impact of technology on society")
                .response("Technological progress has fundamentally transformed modern society. "
                        + "On one hand, digital technologies have provided unprecedented opportunities "
                        + "for communication, education, and access to information. The Internet has connected "
                        + "the world, allowing people to instantly exchange ideas regardless of geographical boundaries."
                        + "On the other hand, new challenges have emerged: digital inequality, technology dependence, "
                        + "and privacy concerns. A balance between innovation and social responsibility is necessary"
                        + "for sustainable development.")
                .reference("Essay about the impact of technology on society with examples and arguments")
                .build();

        // Weak essay
        Sample weakEssay = Sample.builder()
                .userInput("Write an essay about the impact of technology on society")
                .response("Technology is good. There are phones and computers. "
                        + "People use the internet. It's convenient.")
                .reference("Essay about the impact of technology on society with examples and arguments")
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

        log.info("Good essay score: {}", goodScore);
        log.info("Weak essay score: {}", weakScore);

        assertTrue(goodScore >= 4.0, "Good essay should receive high score");
        assertTrue(weakScore <= 2.0, "Weak essay should receive low score");
    }

    @ParameterizedTest
    @MethodSource("ai.qa.solutions.metrics.ModelsProvider#models")
    @DisplayName("RubricsScore: Code quality evaluation")
    void testRubricsScore_CodeQuality(String model) {
        log.info("=== RubricsScore: Code quality evaluation ===");

        // Well-written code
        Sample goodCode = Sample.builder()
                .userInput("Write a function to calculate factorial")
                .response(
                        """
                                def factorial(n):
                                    '''Calculate factorial of n using recursion with validation'''
                                    if not isinstance(n, int) or n < 0:
                                        raise ValueError("Input must be a non-negative integer")
                                    if n == 0 or n == 1:
                                        return 1
                                    return n * factorial(n - 1)
                                """)
                .reference("Function to calculate factorial with proper error handling")
                .build();

        // Poor code
        Sample poorCode = Sample.builder()
                .userInput("Write a function to calculate factorial")
                .response(
                        """
                                def f(n):
                                    if n == 0:
                                        return 1
                                    return n * f(n-1)
                                """)
                .reference("Function to calculate factorial with proper error handling")
                .build();

        RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                .rubrics(createCodeQualityRubrics())
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

        Double goodScore = rubricsScoreMetricWithModel.singleTurnScore(config, goodCode);
        Double poorScore = rubricsScoreMetricWithModel.singleTurnScore(config, poorCode);

        log.info("Good code score: {}", goodScore);
        log.info("Poor code score: {}", poorScore);

        assertTrue(goodScore >= 4.0, "Well-written code should score high");
        assertTrue(poorScore <= 3.0, "Poor code should score lower");
    }

    // ==================== HELPER METHODS ====================

    private Map<String, String> createPhotosynthesisRubrics() {
        return Map.of(
                "score1_description", "Completely incorrect or irrelevant information about photosynthesis",
                "score2_description", "Basic understanding with significant gaps or errors",
                "score3_description", "General understanding of the process, but missing important details",
                "score4_description", "Good understanding mentioning main stages and components",
                "score5_description", "Excellent explanation with scientific details, equation, and examples");
    }

    private Map<String, String> createEssayRubrics() {
        return Map.of(
                "score1_description", "No structure, no arguments, multiple errors",
                "score2_description", "Weak structure, superficial arguments, some errors",
                "score3_description", "Basic structure, some arguments, generally clear",
                "score4_description", "Good structure, convincing arguments, quality presentation",
                "score5_description", "Excellent structure, deep analysis, examples, flawless presentation");
    }

    private Map<String, String> createCodeQualityRubrics() {
        return Map.of(
                "score1_description",
                "Non-functional code with syntax errors, no structure",
                "score2_description",
                "Basic functionality but poor practices, no error handling",
                "score3_description",
                "Working code with acceptable structure, minimal documentation",
                "score4_description",
                "Well-structured code with good practices, error handling, and some documentation",
                "score5_description",
                "Excellent code with best practices, comprehensive error handling, documentation, and efficiency");
    }
}
