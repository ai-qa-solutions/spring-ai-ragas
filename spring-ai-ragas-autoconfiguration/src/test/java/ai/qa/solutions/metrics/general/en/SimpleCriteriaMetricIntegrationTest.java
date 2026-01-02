package ai.qa.solutions.metrics.general.en;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.general.SimpleCriteriaScoreMetric;
import ai.qa.solutions.sample.Sample;
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
@SpringBootTest(classes = SimpleCriteriaMetricIntegrationTest.GeneralMetricsIntegrationTestConfiguration.class)
class SimpleCriteriaMetricIntegrationTest {

    @Configuration
    public static class GeneralMetricsIntegrationTestConfiguration {}

    @Autowired
    private SimpleCriteriaScoreMetric simpleCriteriaScoreMetric;

    @Autowired(required = false)
    private OpenAiApi openAiApi;

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @ParameterizedTest
    @MethodSource("ai.qa.solutions.metrics.ModelsProvider#models")
    @DisplayName("SimpleCriteriaScore: Positive test - high quality response")
    void testSimpleCriteriaScorePositive_HighQuality(String model) {
        if (openAiApi == null && model.contains("/")) return;
        if (openAiApi != null && !model.contains("/")) return;

        log.info("=== SimpleCriteriaScore: Positive test ===");

        Sample sample = Sample.builder()
                .userInput("Explain what artificial intelligence is")
                .response("Artificial Intelligence (AI) is a branch of computer science focused on creating "
                        + "systems capable of performing tasks that typically require human intelligence. "
                        + "This includes learning, reasoning, perception, and decision-making. "
                        + "AI is used across various fields, from medicine to autonomous vehicles.")
                .reference("Artificial intelligence is technology that simulates human thinking "
                        + "to solve complex problems.")
                .build();

        SimpleCriteriaScoreMetric.SimpleCriteriaConfig config = SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                .definition("Rate the quality of explanation from 1 to 5, "
                        + "considering completeness, clarity, and accuracy")
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

        log.info("Model: {}", model);
        log.info("Question: {}", sample.getUserInput());
        log.info("Response: {}", sample.getResponse());
        log.info("Reference: {}", sample.getReference());
        log.info("Quality score: {} (1-5 scale)", score);

        assertTrue(score >= 1.0 && score <= 5.0);
        assertTrue(score >= 4.0, "Expected high score for quality explanation, got: " + score);
    }

    @ParameterizedTest
    @MethodSource("ai.qa.solutions.metrics.ModelsProvider#models")
    @DisplayName("SimpleCriteriaScore: Negative test - low quality response")
    void testSimpleCriteriaScoreNegative_PoorQuality(String model) {
        if (openAiApi == null && model.contains("/")) return;
        if (openAiApi != null && !model.contains("/")) return;

        log.info("=== SimpleCriteriaScore: Negative test ===");

        Sample sample = Sample.builder()
                .userInput("Explain the principles of quantum physics")
                .response("Quantum physics is complicated. There are particles and waves. "
                        + "I don't know what else to say.")
                .reference("Quantum physics studies the behavior of matter and energy at atomic "
                        + "and subatomic levels, where principles of uncertainty and superposition apply.")
                .build();

        SimpleCriteriaScoreMetric.SimpleCriteriaConfig config = SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                .definition("Rate the quality of explanation from 1 to 5, "
                        + "considering completeness, clarity, and scientific accuracy")
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

        log.info("Model: {}", model);
        log.info("Question: {}", sample.getUserInput());
        log.info("Response: {}", sample.getResponse());
        log.info("Reference: {}", sample.getReference());
        log.info("Quality score: {} (1-5 scale)", score);

        assertTrue(score >= 1.0 && score <= 5.0);
        assertTrue(score <= 2.5, "Expected low score for superficial answer, got: " + score);
    }

    @ParameterizedTest
    @MethodSource("ai.qa.solutions.metrics.ModelsProvider#models")
    @DisplayName("SimpleCriteriaScore: Mathematical accuracy test")
    void testSimpleCriteriaScore_MathAccuracy(String model) {
        if (openAiApi == null && model.contains("/")) return;
        if (openAiApi != null && !model.contains("/")) return;

        log.info("=== SimpleCriteriaScore: Mathematical accuracy ===");

        // Correct answer
        Sample correctSample = Sample.builder()
                .userInput("What is 15 multiplied by 12?")
                .response("15 multiplied by 12 equals 180.")
                .reference("180")
                .build();

        // Incorrect answer
        Sample incorrectSample = Sample.builder()
                .userInput("What is 15 multiplied by 12?")
                .response("15 multiplied by 12 equals 170.")
                .reference("180")
                .build();

        SimpleCriteriaScoreMetric.SimpleCriteriaConfig config = SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                .definition("Rate the mathematical accuracy from 0 to 5")
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
        Double incorrectScore = simpleCriteriaScoreMetricWithModel.singleTurnScore(config, incorrectSample);

        log.info("Model: {}", model);
        log.info("Correct answer - score: {}", correctScore);
        log.info("Incorrect answer - score: {}", incorrectScore);

        assertTrue(correctScore >= 4.5, "Correct answer should receive high score");
        assertTrue(incorrectScore <= 2.0, "Incorrect answer should receive low score");
    }

    @ParameterizedTest
    @MethodSource("ai.qa.solutions.metrics.ModelsProvider#models")
    @DisplayName("SimpleCriteriaScore: Relevance evaluation")
    void testSimpleCriteriaScore_Relevance(String model) {
        if (openAiApi == null && model.contains("/")) return;
        if (openAiApi != null && !model.contains("/")) return;

        log.info("=== SimpleCriteriaScore: Relevance evaluation ===");

        // Relevant answer
        Sample relevantSample = Sample.builder()
                .userInput("How does photosynthesis work?")
                .response("Photosynthesis is the process by which plants convert light energy into chemical energy."
                        + "Using sunlight, water, and carbon dioxide, plants produce glucose and release oxygen.")
                .reference("Plants use light to make food from CO2 and water")
                .build();

        // Irrelevant answer
        Sample irrelevantSample = Sample.builder()
                .userInput("How does photosynthesis work?")
                .response("Plants are important for the environment. They provide oxygen and food for many animals.")
                .reference("Plants use light to make food from CO2 and water")
                .build();

        SimpleCriteriaScoreMetric.SimpleCriteriaConfig config = SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                .definition("Rate how relevant the response is to the question from 0 to 5")
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

        Double relevantScore = simpleCriteriaScoreMetricWithModel.singleTurnScore(config, relevantSample);
        Double irrelevantScore = simpleCriteriaScoreMetricWithModel.singleTurnScore(config, irrelevantSample);

        log.info("Model: {}", model);
        log.info("Relevant answer score: {}", relevantScore);
        log.info("Irrelevant answer score: {}", irrelevantScore);

        assertTrue(relevantScore >= 4.0, "Relevant answer should score high");
        assertTrue(irrelevantScore <= 2.5, "Irrelevant answer should score low");
    }
}
