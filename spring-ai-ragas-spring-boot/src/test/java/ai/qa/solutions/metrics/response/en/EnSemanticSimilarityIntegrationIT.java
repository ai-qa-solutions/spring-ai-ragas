package ai.qa.solutions.metrics.response.en;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.response.SemanticSimilarityMetric;
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
 * Integration tests for Semantic Similarity Metric - English Language.
 * <p>
 * Tests the cosine similarity between response and reference embeddings.
 * This metric does NOT use LLM calls, only embedding models.
 * <p>
 * Key characteristics:
 * - Fast execution (embeddings only, no LLM)
 * - Deterministic given same embedding model
 * - Score range: typically 0.0-1.0 for text embeddings
 * - Higher score = more semantically similar
 */
@Slf4j
@EnableAutoConfiguration
@SuppressWarnings("LoggingSimilarMessage")
@DisplayName("Semantic Similarity Metric - English Language Validation")
@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".*")
@SpringBootTest(classes = EnSemanticSimilarityIntegrationIT.SemanticSimilarityIntegrationTestConfiguration.class)
class EnSemanticSimilarityIntegrationIT {

    @Configuration
    public static class SemanticSimilarityIntegrationTestConfiguration {}

    @Autowired
    private SemanticSimilarityMetric semanticSimilarityMetric;

    @Test
    @DisplayName("Identical texts - EXPECTED VERY HIGH SCORE")
    void testSemanticSimilarity_IdenticalTexts() {
        log.info("=== Identical Texts Test ===");

        Sample sample = Sample.builder()
                .response("Paris is the capital of France.")
                .reference("Paris is the capital of France.")
                .build();

        Double score = semanticSimilarityMetric.singleTurnScore(sample);

        log.info("Response: {}", sample.getResponse());
        log.info("Reference: {}", sample.getReference());
        log.info("Score: {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(score >= 0.99, "Identical texts should have nearly perfect similarity. Received: " + score);
    }

    @Test
    @DisplayName("Same meaning, different wording - EXPECTED HIGH SCORE")
    void testSemanticSimilarity_SameMeaningDifferentWording() {
        log.info("=== Same Meaning, Different Wording Test ===");

        Sample sample = Sample.builder()
                .response("The capital city of France is Paris.")
                .reference("Paris serves as the capital of France.")
                .build();

        Double score = semanticSimilarityMetric.singleTurnScore(sample);

        log.info("Response: {}", sample.getResponse());
        log.info("Reference: {}", sample.getReference());
        log.info("Score: {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(score >= 0.85, "Same meaning should have high similarity. Received: " + score);
    }

    @Test
    @DisplayName("Related but different information - EXPECTED MODERATE SCORE")
    void testSemanticSimilarity_RelatedButDifferent() {
        log.info("=== Related but Different Test ===");

        Sample sample = Sample.builder()
                .response("France is a country in Western Europe.")
                .reference("Paris is the capital of France, known for the Eiffel Tower.")
                .build();

        Double score = semanticSimilarityMetric.singleTurnScore(sample);

        log.info("Response: {}", sample.getResponse());
        log.info("Reference: {}", sample.getReference());
        log.info("Score: {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(
                score >= 0.50 && score <= 0.90, "Related topics should have moderate similarity. Received: " + score);
    }

    @Test
    @DisplayName("Completely unrelated texts - EXPECTED LOW SCORE")
    void testSemanticSimilarity_UnrelatedTexts() {
        log.info("=== Unrelated Texts Test ===");

        Sample sample = Sample.builder()
                .response("Machine learning is a subset of artificial intelligence.")
                .reference("The Great Wall of China is over 13,000 miles long.")
                .build();

        Double score = semanticSimilarityMetric.singleTurnScore(sample);

        log.info("Response: {}", sample.getResponse());
        log.info("Reference: {}", sample.getReference());
        log.info("Score: {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(score <= 0.60, "Unrelated texts should have low similarity. Received: " + score);
    }

    @Test
    @DisplayName("Contradictory statements - EXPECTED MODERATE TO HIGH SCORE")
    void testSemanticSimilarity_ContradictoryStatements() {
        log.info("=== Contradictory Statements Test ===");

        Sample sample = Sample.builder()
                .response("The Earth is flat.")
                .reference("The Earth is spherical.")
                .build();

        Double score = semanticSimilarityMetric.singleTurnScore(sample);

        log.info("Response: {}", sample.getResponse());
        log.info("Reference: {}", sample.getReference());
        log.info("Score: {}", String.format("%.4f", score));

        assertNotNull(score);
        // Note: Contradictory statements can still be semantically similar
        // because they discuss the same topic with similar vocabulary
        assertTrue(
                score >= 0.60,
                "Contradictory statements about same topic can be semantically similar. Received: " + score);
        log.warn("Note: Semantic similarity does NOT detect factual contradiction - "
                + "both sentences discuss 'Earth shape' so they embed similarly");
    }

    @Test
    @DisplayName("Long vs short response on same topic - EXPECTED MODERATE SCORE")
    void testSemanticSimilarity_LongVsShort() {
        log.info("=== Long vs Short Response Test ===");

        Sample sample = Sample.builder()
                .response("Paris is the capital of France, known for the Eiffel Tower, "
                        + "Louvre Museum, and its rich history. The city is a major European "
                        + "center for art, fashion, gastronomy, and culture.")
                .reference("Paris is the capital of France.")
                .build();

        Double score = semanticSimilarityMetric.singleTurnScore(sample);

        log.info("Response length: {} chars", sample.getResponse().length());
        log.info("Reference length: {} chars", sample.getReference().length());
        log.info("Score: {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(
                score >= 0.70, "Long response containing short reference info should be similar. Received: " + score);
    }

    @Test
    @DisplayName("Technical vs casual language - EXPECTED MODERATE TO HIGH SCORE")
    void testSemanticSimilarity_TechnicalVsCasual() {
        log.info("=== Technical vs Casual Language Test ===");

        Sample sample = Sample.builder()
                .response("The photosynthesis process converts solar radiation into chemical energy.")
                .reference("Plants use sunlight to make food.")
                .build();

        Double score = semanticSimilarityMetric.singleTurnScore(sample);

        log.info("Response: {}", sample.getResponse());
        log.info("Reference: {}", sample.getReference());
        log.info("Score: {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(
                score >= 0.50,
                "Technical and casual descriptions of same concept should be moderately similar. Received: " + score);
    }

    @Test
    @DisplayName("With threshold - binary pass/fail")
    void testSemanticSimilarity_WithThreshold() {
        log.info("=== Threshold Test ===");

        Sample sample = Sample.builder()
                .response("Paris is the capital of France.")
                .reference("The capital of France is Paris.")
                .build();

        SemanticSimilarityMetric.SemanticSimilarityConfig configHighThreshold =
                SemanticSimilarityMetric.SemanticSimilarityConfig.builder()
                        .threshold(0.95)
                        .build();

        Double scoreHighThreshold = semanticSimilarityMetric.singleTurnScore(configHighThreshold, sample);

        SemanticSimilarityMetric.SemanticSimilarityConfig configLowThreshold =
                SemanticSimilarityMetric.SemanticSimilarityConfig.builder()
                        .threshold(0.70)
                        .build();

        Double scoreLowThreshold = semanticSimilarityMetric.singleTurnScore(configLowThreshold, sample);

        log.info("Score with 0.95 threshold: {}", scoreHighThreshold);
        log.info("Score with 0.70 threshold: {}", scoreLowThreshold);

        assertNotNull(scoreHighThreshold);
        assertNotNull(scoreLowThreshold);

        // With threshold, individual model scores are binary (0.0 or 1.0)
        // However, aggregated scores may be intermediate when models disagree
        // e.g., if 2 models pass threshold (1.0) and 2 fail (0.0), average = 0.5
        assertTrue(
                scoreHighThreshold >= 0.0 && scoreHighThreshold <= 1.0,
                "Score with threshold should be in valid range. Received: " + scoreHighThreshold);
        assertTrue(
                scoreLowThreshold >= 0.0 && scoreLowThreshold <= 1.0,
                "Score with threshold should be in valid range. Received: " + scoreLowThreshold);

        // Low threshold should generally pass more easily (higher or equal score)
        assertTrue(scoreLowThreshold >= scoreHighThreshold, "Lower threshold should result in >= score");
    }

    @Test
    @DisplayName("Empty response - EXPECTED ZERO SCORE")
    void testSemanticSimilarity_EmptyResponse() {
        log.info("=== Empty Response Test ===");

        Sample sample = Sample.builder()
                .response("")
                .reference("Paris is the capital of France.")
                .build();

        Double score = semanticSimilarityMetric.singleTurnScore(sample);

        log.info("Score: {}", score);

        assertNotNull(score);
        assertTrue(score == 0.0, "Empty response should return 0.0. Received: " + score);
    }

    @Test
    @DisplayName("Empty reference - EXPECTED ZERO SCORE")
    void testSemanticSimilarity_EmptyReference() {
        log.info("=== Empty Reference Test ===");

        Sample sample = Sample.builder()
                .response("Paris is the capital of France.")
                .reference("")
                .build();

        Double score = semanticSimilarityMetric.singleTurnScore(sample);

        log.info("Score: {}", score);

        assertNotNull(score);
        assertTrue(score == 0.0, "Empty reference should return 0.0. Received: " + score);
    }

    @Test
    @DisplayName("Async scoring works correctly")
    void testSemanticSimilarity_AsyncScoring() {
        log.info("=== Async Scoring Test ===");

        Sample sample = Sample.builder()
                .response("Paris is the capital of France.")
                .reference("Paris is the capital of France.")
                .build();

        Double score = semanticSimilarityMetric.singleTurnScoreAsync(sample).join();

        log.info("Async Score: {}", String.format("%.4f", score));

        assertNotNull(score);
        assertTrue(score >= 0.99, "Async scoring should work identically to sync. Received: " + score);
    }
}
