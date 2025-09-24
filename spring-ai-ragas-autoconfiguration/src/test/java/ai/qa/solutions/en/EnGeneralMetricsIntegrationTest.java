package ai.qa.solutions.en;

import static org.junit.jupiter.api.Assertions.*;

import ai.qa.solutions.metrics.general.AspectCriticMetric;
import ai.qa.solutions.metrics.general.RubricsScoreMetric;
import ai.qa.solutions.metrics.general.SimpleCriteriaScoreMetric;
import ai.qa.solutions.sample.SingleTurnSample;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

@EnableAutoConfiguration
@SpringBootTest(classes = EnGeneralMetricsIntegrationTest.GeneralMetricsIntegrationTestConfiguration.class)
class EnGeneralMetricsIntegrationTest {

    @Configuration
    public static class GeneralMetricsIntegrationTestConfiguration {}

    @Autowired
    private AspectCriticMetric aspectCriticMetric;

    @Autowired
    private SimpleCriteriaScoreMetric simpleCriteriaScoreMetric;

    @Autowired
    private RubricsScoreMetric rubricsScoreMetric;

    @Test
    void testAspectCriticIntegration() {
        SingleTurnSample sample = SingleTurnSample.builder()
                .userInput("What is the capital of France?")
                .response("The capital of France is Paris.")
                .build();

        aspectCriticMetric.setDefinition("Is the response factually accurate?");

        Double score = aspectCriticMetric.singleTurnScore(sample);
        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
    }

    @Test
    void testSimpleCriteriaScoreIntegration() {
        SingleTurnSample sample = SingleTurnSample.builder()
                .userInput("What is 2 + 2?")
                .response("2 + 2 equals 4.")
                .reference("The answer is 4.")
                .build();

        simpleCriteriaScoreMetric.setDefinition("Rate the accuracy of the mathematical response");
        simpleCriteriaScoreMetric.setScoreRange(1.0, 5.0);

        Double score = simpleCriteriaScoreMetric.singleTurnScore(sample);
        assertNotNull(score);
        assertTrue(score >= 1.0 && score <= 5.0);
    }

    @Test
    void testRubricsScoreIntegration() {
        SingleTurnSample sample = SingleTurnSample.builder()
                .userInput("Explain photosynthesis")
                .response(
                        "Photosynthesis is the process by which plants use sunlight, carbon dioxide, and water to produce glucose and oxygen.")
                .reference("Photosynthesis is how plants make food using light, CO2, and water.")
                .build();

        Map<String, String> rubrics = Map.of(
                "score1_description", "Completely incorrect or irrelevant response",
                "score2_description", "Partially correct but with major errors",
                "score3_description", "Mostly correct with minor inaccuracies",
                "score4_description", "Accurate and clear explanation",
                "score5_description", "Excellent, comprehensive, and perfectly accurate");

        rubricsScoreMetric.setRubrics(rubrics);

        Double score = rubricsScoreMetric.singleTurnScore(sample);
        assertNotNull(score);
        assertTrue(score >= 1.0 && score <= 5.0);
    }

    @Test
    void testAsyncEvaluation() throws Exception {
        SingleTurnSample sample = SingleTurnSample.builder()
                .userInput("What is machine learning?")
                .response(
                        "Machine learning is a subset of artificial intelligence that enables computers to learn and improve from experience without being explicitly programmed.")
                .build();

        aspectCriticMetric.setDefinition("Is this a clear and accurate definition?");

        CompletableFuture<Double> asyncScore = aspectCriticMetric.singleTurnScoreAsync(sample);
        Double score = asyncScore.get();

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
    }
}
