package ai.qa.solutions.metrics.general.en;

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
@DisplayName("Integration tests for general purpose metrics with English examples")
@SpringBootTest(classes = EnSimpleCriteriaMetricIntegrationTest.GeneralMetricsIntegrationTestConfiguration.class)
class EnSimpleCriteriaMetricIntegrationTest {

    @Configuration
    public static class GeneralMetricsIntegrationTestConfiguration {}

    @Autowired
    private SimpleCriteriaScoreMetric simpleCriteriaScoreMetric;

    @Test
    @DisplayName("SimpleCriteriaScore: Positive test - high quality response")
    void testSimpleCriteriaScorePositive_HighQuality() {
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

        Double score = simpleCriteriaScoreMetric.singleTurnScore(config, sample);
        log.info("Question: {}", sample.getUserInput());
        log.info("Response: {}", sample.getResponse());
        log.info("Reference: {}", sample.getReference());
        log.info("Normalized score: {} (0-1 scale)", score);

        assertTrue(score >= 0.0 && score <= 1.0, "Score must be normalized to [0, 1] range");
        assertTrue(score >= 0.75, "Expected high normalized score for quality explanation, got: " + score);
    }

    @Test
    @DisplayName("SimpleCriteriaScore: Negative test - low quality response")
    void testSimpleCriteriaScoreNegative_PoorQuality() {
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

        Double score = simpleCriteriaScoreMetric.singleTurnScore(config, sample);
        log.info("Question: {}", sample.getUserInput());
        log.info("Response: {}", sample.getResponse());
        log.info("Reference: {}", sample.getReference());
        log.info("Normalized score: {} (0-1 scale)", score);

        assertTrue(score >= 0.0 && score <= 1.0, "Score must be normalized to [0, 1] range");
        assertTrue(score <= 0.4, "Expected low normalized score for superficial answer, got: " + score);
    }

    @Test
    @DisplayName("SimpleCriteriaScore: Mathematical accuracy - correct answer")
    void testSimpleCriteriaScore_MathAccuracy_CorrectAnswer() {
        log.info("=== SimpleCriteriaScore: Mathematical accuracy - correct answer ===");

        Sample sample = Sample.builder()
                .userInput("What is 15 multiplied by 12?")
                .response("15 multiplied by 12 equals 180.")
                .reference("180")
                .build();

        SimpleCriteriaScoreMetric.SimpleCriteriaConfig config = SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                .definition("Rate the mathematical accuracy from 0 to 5")
                .minScore(0.0)
                .maxScore(5.0)
                .build();

        Double score = simpleCriteriaScoreMetric.singleTurnScore(config, sample);
        log.info("Correct answer - normalized score: {}", score);

        assertTrue(score >= 0.9, "Correct answer should receive high normalized score (>=0.9)");
    }

    @Test
    @DisplayName("SimpleCriteriaScore: Mathematical accuracy - incorrect answer")
    void testSimpleCriteriaScore_MathAccuracy_IncorrectAnswer() {
        log.info("=== SimpleCriteriaScore: Mathematical accuracy - incorrect answer ===");

        Sample sample = Sample.builder()
                .userInput("What is 15 multiplied by 12?")
                .response("15 multiplied by 12 equals 170.")
                .reference("180")
                .build();

        SimpleCriteriaScoreMetric.SimpleCriteriaConfig config = SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                .definition("Rate the mathematical accuracy from 0 to 5")
                .minScore(0.0)
                .maxScore(5.0)
                .build();

        Double score = simpleCriteriaScoreMetric.singleTurnScore(config, sample);
        log.info("Incorrect answer - normalized score: {}", score);

        assertTrue(score <= 0.4, "Incorrect answer should receive low normalized score (<=0.4)");
    }

    @Test
    @DisplayName("SimpleCriteriaScore: Relevance - relevant answer")
    void testSimpleCriteriaScore_Relevance_RelevantAnswer() {
        log.info("=== SimpleCriteriaScore: Relevance - relevant answer ===");

        Sample sample = Sample.builder()
                .userInput("How does photosynthesis work?")
                .response("Photosynthesis is the process by which plants convert light energy into chemical energy."
                        + "Using sunlight, water, and carbon dioxide, plants produce glucose and release oxygen.")
                .reference("Plants use light to make food from CO2 and water")
                .build();

        SimpleCriteriaScoreMetric.SimpleCriteriaConfig config = SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                .definition("Rate how relevant the response is to the question from 0 to 5")
                .minScore(0.0)
                .maxScore(5.0)
                .build();

        Double score = simpleCriteriaScoreMetric.singleTurnScore(config, sample);
        log.info("Relevant answer - normalized score: {}", score);

        assertTrue(score >= 0.8, "Relevant answer should have high normalized score (>=0.8)");
    }

    @Test
    @DisplayName("SimpleCriteriaScore: Relevance - irrelevant answer")
    void testSimpleCriteriaScore_Relevance_IrrelevantAnswer() {
        log.info("=== SimpleCriteriaScore: Relevance - irrelevant answer ===");

        Sample sample = Sample.builder()
                .userInput("How does photosynthesis work?")
                .response("Plants are important for the environment. They provide oxygen and food for many animals.")
                .reference("Plants use light to make food from CO2 and water")
                .build();

        SimpleCriteriaScoreMetric.SimpleCriteriaConfig config = SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                .definition("Rate how relevant the response is to the question from 0 to 5")
                .minScore(0.0)
                .maxScore(5.0)
                .build();

        Double score = simpleCriteriaScoreMetric.singleTurnScore(config, sample);
        log.info("Irrelevant answer - normalized score: {}", score);

        assertTrue(score <= 0.5, "Irrelevant answer should have low normalized score (<=0.5)");
    }
}
