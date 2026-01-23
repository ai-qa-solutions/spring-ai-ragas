package ai.qa.solutions.metrics.response.en;

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
 * Integration tests for Factual Correctness Metric - English Language.
 * <p>
 * Tests the factual accuracy of responses compared to reference answers.
 * Uses claims decomposition and NLI verification.
 * <p>
 * Key characteristics:
 * - Requires LLM calls for claim extraction and NLI
 * - Measures precision, recall, and F1 of factual claims
 * - Higher score = more factually correct
 */
@Slf4j
@EnableAutoConfiguration
@SuppressWarnings("LoggingSimilarMessage")
@DisplayName("Factual Correctness Metric - English Language Validation")
@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".*")
@SpringBootTest(classes = EnFactualCorrectnessIntegrationIT.FactualCorrectnessIntegrationTestConfiguration.class)
class EnFactualCorrectnessIntegrationIT {

    @Configuration
    public static class FactualCorrectnessIntegrationTestConfiguration {}

    @Autowired
    private FactualCorrectnessMetric factualCorrectnessMetric;

    @Test
    @DisplayName("Identical texts - EXPECTED VERY HIGH SCORE")
    void testFactualCorrectness_IdenticalTexts() {
        log.info("=== Identical Texts Test ===");

        Sample sample = Sample.builder()
                .response("Paris is the capital of France. The Eiffel Tower is located in Paris.")
                .reference("Paris is the capital of France. The Eiffel Tower is located in Paris.")
                .build();

        Double score = factualCorrectnessMetric.singleTurnScore(sample);

        log.info("Response: {}", sample.getResponse());
        log.info("Reference: {}", sample.getReference());
        log.info("Score: {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(score >= 0.8, "Identical texts should have very high factual correctness. Received: " + score);
    }

    @Test
    @DisplayName("Correct but rephrased - EXPECTED HIGH SCORE")
    void testFactualCorrectness_RephrasedCorrect() {
        log.info("=== Correct but Rephrased Test ===");

        Sample sample = Sample.builder()
                .response("France's capital city is Paris, where you can find the famous Eiffel Tower.")
                .reference("Paris is the capital of France. The Eiffel Tower is located in Paris.")
                .build();

        Double score = factualCorrectnessMetric.singleTurnScore(sample);

        log.info("Response: {}", sample.getResponse());
        log.info("Reference: {}", sample.getReference());
        log.info("Score: {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(score >= 0.6, "Rephrased but correct response should have high score. Received: " + score);
    }

    @Test
    @DisplayName("Partial information - EXPECTED MODERATE SCORE")
    void testFactualCorrectness_PartialInformation() {
        log.info("=== Partial Information Test ===");

        Sample sample = Sample.builder()
                .response("Paris is the capital of France.")
                .reference("Paris is the capital of France. The Eiffel Tower is located in Paris. "
                        + "Paris is also known as the City of Light.")
                .build();

        Double score = factualCorrectnessMetric.singleTurnScore(sample);

        log.info("Response: {}", sample.getResponse());
        log.info("Reference: {}", sample.getReference());
        log.info("Score: {}", String.format("%.4f", score));

        assertNotNull(score);
        // Response is correct but incomplete - moderate recall
        assertTrue(
                score >= 0.3 && score <= 0.8,
                "Partial but correct information should have moderate score. Received: " + score);
    }

    @Test
    @DisplayName("Factually incorrect - EXPECTED LOW SCORE")
    void testFactualCorrectness_FactuallyIncorrect() {
        log.info("=== Factually Incorrect Test ===");

        Sample sample = Sample.builder()
                .response("London is the capital of France. The Eiffel Tower is in Berlin.")
                .reference("Paris is the capital of France. The Eiffel Tower is located in Paris.")
                .build();

        Double score = factualCorrectnessMetric.singleTurnScore(sample);

        log.info("Response: {}", sample.getResponse());
        log.info("Reference: {}", sample.getReference());
        log.info("Score: {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(score <= 0.4, "Factually incorrect response should have low score. Received: " + score);
    }

    @Test
    @DisplayName("Mixed correct and incorrect - EXPECTED MODERATE SCORE")
    void testFactualCorrectness_MixedCorrectIncorrect() {
        log.info("=== Mixed Correct and Incorrect Test ===");

        Sample sample = Sample.builder()
                .response("Paris is the capital of France. The Eiffel Tower was built in 1500.")
                .reference("Paris is the capital of France. The Eiffel Tower was completed in 1889.")
                .build();

        Double score = factualCorrectnessMetric.singleTurnScore(sample);

        log.info("Response: {}", sample.getResponse());
        log.info("Reference: {}", sample.getReference());
        log.info("Score: {}", String.format("%.4f", score));

        assertNotNull(score);
        // One correct claim, one incorrect
        assertTrue(score >= 0.2 && score <= 0.7, "Mixed facts should have intermediate score. Received: " + score);
    }

    @Test
    @DisplayName("Extra correct information - EXPECTED HIGH PRECISION")
    void testFactualCorrectness_ExtraInformation() {
        log.info("=== Extra Information Test ===");

        Sample sample = Sample.builder()
                .response("Paris is the capital of France. The Eiffel Tower is located in Paris. "
                        + "Paris has a population of over 2 million people.")
                .reference("Paris is the capital of France. The Eiffel Tower is located in Paris.")
                .build();

        FactualCorrectnessMetric.FactualCorrectnessConfig precisionConfig =
                FactualCorrectnessMetric.FactualCorrectnessConfig.builder()
                        .mode(FactualCorrectnessMetric.Mode.PRECISION)
                        .build();

        Double precisionScore = factualCorrectnessMetric.singleTurnScore(precisionConfig, sample);

        log.info("Response: {}", sample.getResponse());
        log.info("Reference: {}", sample.getReference());
        log.info("Precision Score: {}", String.format("%.4f", precisionScore));

        assertNotNull(precisionScore);
        // Extra true info doesn't hurt precision (claims are still verifiable)
        assertTrue(
                precisionScore >= 0.5,
                "Extra correct information should maintain good precision. Received: " + precisionScore);
    }

    @Test
    @DisplayName("RECALL mode - test missing information")
    void testFactualCorrectness_RecallMode() {
        log.info("=== Recall Mode Test ===");

        Sample sample = Sample.builder()
                .response("Paris is the capital.")
                .reference("Paris is the capital of France. The Eiffel Tower is in Paris.")
                .build();

        FactualCorrectnessMetric.FactualCorrectnessConfig recallConfig =
                FactualCorrectnessMetric.FactualCorrectnessConfig.builder()
                        .mode(FactualCorrectnessMetric.Mode.RECALL)
                        .build();

        Double recallScore = factualCorrectnessMetric.singleTurnScore(recallConfig, sample);

        log.info("Response: {}", sample.getResponse());
        log.info("Reference: {}", sample.getReference());
        log.info("Recall Score: {}", String.format("%.4f", recallScore));

        assertNotNull(recallScore);
        // Low recall - missing most reference claims
        assertTrue(recallScore <= 0.7, "Missing information should have lower recall. Received: " + recallScore);
    }

    @Test
    @DisplayName("Complex factual comparison")
    void testFactualCorrectness_ComplexFacts() {
        log.info("=== Complex Factual Comparison Test ===");

        Sample sample = Sample.builder()
                .response("Albert Einstein was a German-born physicist who developed the theory of relativity. "
                        + "He received the Nobel Prize in Physics in 1921.")
                .reference("Albert Einstein was born in Germany and became one of the most famous physicists. "
                        + "He developed the theory of relativity and won the Nobel Prize in Physics in 1921.")
                .build();

        Double score = factualCorrectnessMetric.singleTurnScore(sample);

        log.info("Response: {}", sample.getResponse());
        log.info("Reference: {}", sample.getReference());
        log.info("Score: {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(score >= 0.6, "Factually aligned complex text should have good score. Received: " + score);
    }

    @Test
    @DisplayName("Async scoring works correctly")
    void testFactualCorrectness_AsyncScoring() {
        log.info("=== Async Scoring Test ===");

        Sample sample = Sample.builder()
                .response("Paris is the capital of France.")
                .reference("Paris is the capital of France.")
                .build();

        Double score = factualCorrectnessMetric.singleTurnScoreAsync(sample).join();

        log.info("Async Score: {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(score >= 0.7, "Async scoring should work identically to sync. Received: " + score);
    }
}
