package ai.qa.solutions.en;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.general.AspectCriticMetric;
import ai.qa.solutions.metrics.general.RubricsScoreMetric;
import ai.qa.solutions.metrics.general.SimpleCriteriaScoreMetric;
import ai.qa.solutions.sample.Sample;
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
        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("The capital of France is Paris.")
                .build();

        AspectCriticMetric.AspectCriticConfig config = AspectCriticMetric.AspectCriticConfig.builder()
                .definition("Is the response factually accurate?")
                .build();

        Double score = aspectCriticMetric.singleTurnScore(config, sample);
        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
    }

    @Test
    void testSimpleCriteriaScoreIntegration() {
        Sample sample = Sample.builder()
                .userInput("What is 2 + 2?")
                .response("2 + 2 equals 4.")
                .reference("The answer is 4.")
                .build();

        SimpleCriteriaScoreMetric.SimpleCriteriaConfig config = SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                .definition("Rate the accuracy of the mathematical response")
                .minScore(1.0)
                .maxScore(5.0)
                .build();

        Double score = simpleCriteriaScoreMetric.singleTurnScore(config, sample);
        assertNotNull(score);
        assertTrue(score >= 1.0 && score <= 5.0);
    }

    @Test
    void testRubricsScoreIntegration() {
        Sample sample = Sample.builder()
                .userInput("Explain photosynthesis")
                .response(
                        "Photosynthesis is the process by which plants use sunlight, carbon dioxide, and water to produce glucose and oxygen.")
                .reference("Photosynthesis is how plants make food using light, CO2, and water.")
                .build();

        RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                .rubric("score1_description", "Completely incorrect or irrelevant response")
                .rubric("score2_description", "Partially correct but with major errors")
                .rubric("score3_description", "Mostly correct with minor inaccuracies")
                .rubric("score4_description", "Accurate and clear explanation")
                .rubric("score5_description", "Excellent, comprehensive, and perfectly accurate")
                .build();

        Double score = rubricsScoreMetric.singleTurnScore(config, sample);
        assertNotNull(score);
        assertTrue(score >= 1.0 && score <= 5.0);
    }

    @Test
    void testAsyncEvaluation() throws Exception {
        Sample sample = Sample.builder()
                .userInput("What is machine learning?")
                .response("Machine learning is a subset of artificial intelligence that enables "
                        + "computers to learn and improve from experience without being explicitly programmed.")
                .build();

        AspectCriticMetric.AspectCriticConfig config = AspectCriticMetric.AspectCriticConfig.builder()
                .definition("Is this a clear and accurate definition?")
                .build();

        CompletableFuture<Double> asyncScore = aspectCriticMetric.singleTurnScoreAsync(config, sample);
        Double score = asyncScore.get();

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
    }

    @Test
    void testParallelEvaluationWithConfigs() {
        Sample sample = Sample.builder()
                .userInput("Explain climate change")
                .response(
                        "Climate change refers to long-term shifts in global temperatures and weather patterns due to human activities.")
                .reference("Climate change is global warming caused by human greenhouse gas emissions.")
                .build();

        // Create configs
        AspectCriticMetric.AspectCriticConfig aspectConfig = AspectCriticMetric.AspectCriticConfig.builder()
                .definition("Is the response scientifically accurate?")
                .build();

        SimpleCriteriaScoreMetric.SimpleCriteriaConfig criteriaConfig =
                SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                        .definition("Rate explanation clarity and completeness")
                        .minScore(1.0)
                        .maxScore(5.0)
                        .build();

        RubricsScoreMetric.RubricsConfig rubricsConfig = RubricsScoreMetric.RubricsConfig.builder()
                .rubric("score1_description", "Incorrect or misleading information")
                .rubric("score2_description", "Basic understanding with gaps")
                .rubric("score3_description", "Good understanding of main concepts")
                .rubric("score4_description", "Comprehensive and accurate explanation")
                .rubric("score5_description", "Expert-level explanation with scientific details")
                .build();

        // Parallel execution - each with its own immutable config
        CompletableFuture<Double> aspectFuture = aspectCriticMetric.singleTurnScoreAsync(aspectConfig, sample);
        CompletableFuture<Double> criteriaFuture =
                simpleCriteriaScoreMetric.singleTurnScoreAsync(criteriaConfig, sample);
        CompletableFuture<Double> rubricsFuture = rubricsScoreMetric.singleTurnScoreAsync(rubricsConfig, sample);

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(aspectFuture, criteriaFuture, rubricsFuture);
        allFutures.join();

        Double aspectScore = aspectFuture.join();
        Double criteriaScore = criteriaFuture.join();
        Double rubricsScore = rubricsFuture.join();

        assertNotNull(aspectScore);
        assertNotNull(criteriaScore);
        assertNotNull(rubricsScore);

        assertTrue(aspectScore >= 0.0 && aspectScore <= 1.0);
        assertTrue(criteriaScore >= 1.0 && criteriaScore <= 5.0);
        assertTrue(rubricsScore >= 1.0 && rubricsScore <= 5.0);
    }
}
