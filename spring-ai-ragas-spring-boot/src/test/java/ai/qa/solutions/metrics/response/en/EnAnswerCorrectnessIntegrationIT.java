package ai.qa.solutions.metrics.response.en;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.response.AnswerCorrectnessMetric;
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
 * Integration tests for Answer Correctness Metric - English Language.
 * <p>
 * Tests the combined evaluation of factual correctness (75%) and semantic similarity (25%).
 * <p>
 * Key characteristics:
 * - Requires LLM calls for factual correctness (claims decomposition + NLI)
 * - Requires embedding calls for semantic similarity
 * - Returns weighted average of both components
 */
@Slf4j
@EnableAutoConfiguration
@SuppressWarnings("LoggingSimilarMessage")
@DisplayName("Answer Correctness Metric - English Language Validation")
@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".*")
@SpringBootTest(classes = EnAnswerCorrectnessIntegrationIT.AnswerCorrectnessIntegrationTestConfiguration.class)
class EnAnswerCorrectnessIntegrationIT {

    @Configuration
    public static class AnswerCorrectnessIntegrationTestConfiguration {}

    @Autowired
    private AnswerCorrectnessMetric answerCorrectnessMetric;

    @Test
    @DisplayName("Identical texts - EXPECTED VERY HIGH SCORE")
    void testAnswerCorrectness_IdenticalTexts() {
        log.info("=== Identical Texts Test ===");

        Sample sample = Sample.builder()
                .response("Paris is the capital of France. The Eiffel Tower is located in Paris.")
                .reference("Paris is the capital of France. The Eiffel Tower is located in Paris.")
                .build();

        Double score = answerCorrectnessMetric.singleTurnScore(sample);

        log.info("Response: {}", sample.getResponse());
        log.info("Reference: {}", sample.getReference());
        log.info("Score: {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(score >= 0.8, "Identical texts should have very high answer correctness. Received: " + score);
    }

    @Test
    @DisplayName("Correct but rephrased - EXPECTED HIGH SCORE")
    void testAnswerCorrectness_RephrasedCorrect() {
        log.info("=== Correct but Rephrased Test ===");

        Sample sample = Sample.builder()
                .response("France's capital city is Paris, where you can find the famous Eiffel Tower.")
                .reference("Paris is the capital of France. The Eiffel Tower is located in Paris.")
                .build();

        Double score = answerCorrectnessMetric.singleTurnScore(sample);

        log.info("Response: {}", sample.getResponse());
        log.info("Reference: {}", sample.getReference());
        log.info("Score: {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(score >= 0.6, "Rephrased but correct response should have high score. Received: " + score);
    }

    @Test
    @DisplayName("Partial information - EXPECTED MODERATE SCORE")
    void testAnswerCorrectness_PartialInformation() {
        log.info("=== Partial Information Test ===");

        Sample sample = Sample.builder()
                .response("Paris is the capital of France.")
                .reference("Paris is the capital of France. The Eiffel Tower is located in Paris. "
                        + "Paris is also known as the City of Light.")
                .build();

        Double score = answerCorrectnessMetric.singleTurnScore(sample);

        log.info("Response: {}", sample.getResponse());
        log.info("Reference: {}", sample.getReference());
        log.info("Score: {}", String.format("%.4f", score));

        assertNotNull(score);
        // Factual correctness (high) + semantic similarity (moderate) = moderate-high
        assertTrue(
                score >= 0.3 && score <= 0.85,
                "Partial but correct information should have moderate score. Received: " + score);
    }

    @Test
    @DisplayName("Factually incorrect - EXPECTED LOW SCORE")
    void testAnswerCorrectness_FactuallyIncorrect() {
        log.info("=== Factually Incorrect Test ===");

        Sample sample = Sample.builder()
                .response("London is the capital of France. The Eiffel Tower is in Berlin.")
                .reference("Paris is the capital of France. The Eiffel Tower is located in Paris.")
                .build();

        Double score = answerCorrectnessMetric.singleTurnScore(sample);

        log.info("Response: {}", sample.getResponse());
        log.info("Reference: {}", sample.getReference());
        log.info("Score: {}", String.format("%.4f", score));

        assertNotNull(score);
        // Both factual and semantic similarity should be low
        assertTrue(score <= 0.5, "Factually incorrect response should have low score. Received: " + score);
    }

    @Test
    @DisplayName("Mixed correct and incorrect - EXPECTED MODERATE SCORE")
    void testAnswerCorrectness_MixedCorrectIncorrect() {
        log.info("=== Mixed Correct and Incorrect Test ===");

        Sample sample = Sample.builder()
                .response("Paris is the capital of France. The Eiffel Tower was built in 1500.")
                .reference("Paris is the capital of France. The Eiffel Tower was completed in 1889.")
                .build();

        Double score = answerCorrectnessMetric.singleTurnScore(sample);

        log.info("Response: {}", sample.getResponse());
        log.info("Reference: {}", sample.getReference());
        log.info("Score: {}", String.format("%.4f", score));

        assertNotNull(score);
        // One correct claim, one incorrect, but semantically similar
        assertTrue(score >= 0.2 && score <= 0.8, "Mixed facts should have intermediate score. Received: " + score);
    }

    @Test
    @DisplayName("Custom weights - factual focused")
    void testAnswerCorrectness_FactualFocused() {
        log.info("=== Factual Focused Test ===");

        Sample sample = Sample.builder()
                .response("Paris is the capital of France. The Eiffel Tower is in Paris.")
                .reference("Paris is the capital of France. The Eiffel Tower is located in Paris.")
                .build();

        AnswerCorrectnessMetric.AnswerCorrectnessConfig config =
                AnswerCorrectnessMetric.AnswerCorrectnessConfig.factualFocused();

        Double score = answerCorrectnessMetric.singleTurnScore(config, sample);

        log.info("Response: {}", sample.getResponse());
        log.info("Reference: {}", sample.getReference());
        log.info("Factual-focused Score (90/10): {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(score >= 0.5, "Correct answer with factual focus should score well. Received: " + score);
    }

    @Test
    @DisplayName("Custom weights - semantic focused")
    void testAnswerCorrectness_SemanticFocused() {
        log.info("=== Semantic Focused Test ===");

        Sample sample = Sample.builder()
                .response("The capital city of France is Paris, home to the iconic Eiffel Tower.")
                .reference("Paris is the capital of France. The Eiffel Tower is located in Paris.")
                .build();

        AnswerCorrectnessMetric.AnswerCorrectnessConfig config =
                AnswerCorrectnessMetric.AnswerCorrectnessConfig.semanticFocused();

        Double score = answerCorrectnessMetric.singleTurnScore(config, sample);

        log.info("Response: {}", sample.getResponse());
        log.info("Reference: {}", sample.getReference());
        log.info("Semantic-focused Score (10/90): {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(
                score >= 0.5, "Semantically similar answer with semantic focus should score well. Received: " + score);
    }

    @Test
    @DisplayName("Complex factual comparison")
    void testAnswerCorrectness_ComplexFacts() {
        log.info("=== Complex Factual Comparison Test ===");

        Sample sample = Sample.builder()
                .response("Albert Einstein was a German-born physicist who developed the theory of relativity. "
                        + "He received the Nobel Prize in Physics in 1921.")
                .reference("Albert Einstein was born in Germany and became one of the most famous physicists. "
                        + "He developed the theory of relativity and won the Nobel Prize in Physics in 1921.")
                .build();

        Double score = answerCorrectnessMetric.singleTurnScore(sample);

        log.info("Response: {}", sample.getResponse());
        log.info("Reference: {}", sample.getReference());
        log.info("Score: {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(score >= 0.6, "Factually aligned complex text should have good score. Received: " + score);
    }

    @Test
    @DisplayName("Async scoring works correctly")
    void testAnswerCorrectness_AsyncScoring() {
        log.info("=== Async Scoring Test ===");

        Sample sample = Sample.builder()
                .response("Paris is the capital of France.")
                .reference("Paris is the capital of France.")
                .build();

        Double score = answerCorrectnessMetric.singleTurnScoreAsync(sample).join();

        log.info("Async Score: {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(score >= 0.7, "Async scoring should work identically to sync. Received: " + score);
    }
}
