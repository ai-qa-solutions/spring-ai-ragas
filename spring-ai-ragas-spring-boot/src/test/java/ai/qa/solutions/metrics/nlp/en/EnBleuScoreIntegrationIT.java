package ai.qa.solutions.metrics.nlp.en;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.nlp.BleuScoreMetric;
import ai.qa.solutions.metrics.nlp.BleuScoreMetric.BleuScoreConfig;
import ai.qa.solutions.sample.Sample;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

/**
 * Integration tests for BLEU Score Metric - English Language.
 * <p>
 * Tests the BLEU score calculation for text similarity.
 * This is a non-LLM metric that doesn't require API keys.
 */
@Slf4j
@EnableAutoConfiguration
@SuppressWarnings("LoggingSimilarMessage")
@DisplayName("BLEU Score Metric - English Language Validation")
@SpringBootTest(classes = EnBleuScoreIntegrationIT.BleuScoreIntegrationTestConfiguration.class)
class EnBleuScoreIntegrationIT {

    @Configuration
    public static class BleuScoreIntegrationTestConfiguration {}

    @Autowired
    private BleuScoreMetric bleuScoreMetric;

    @Nested
    @DisplayName("Basic Scoring Tests")
    class BasicScoringTests {

        @Test
        @DisplayName("Identical texts - EXPECTED HIGH SCORE")
        void testIdenticalTexts() {
            log.info("=== Identical Texts Test ===");

            final Sample sample = Sample.builder()
                    .response("The quick brown fox jumps over the lazy dog.")
                    .reference("The quick brown fox jumps over the lazy dog.")
                    .build();

            final BleuScoreConfig config = BleuScoreConfig.builder().build();

            final Double score = bleuScoreMetric.singleTurnScore(config, sample);

            log.info("Response: {}", sample.getResponse());
            log.info("Reference: {}", sample.getReference());
            log.info("BLEU Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            assertTrue(score >= 0.95, "Identical texts should have near-perfect BLEU score. Received: " + score);
        }

        @Test
        @DisplayName("Similar texts - EXPECTED HIGH SCORE")
        void testSimilarTexts() {
            log.info("=== Similar Texts Test ===");

            final Sample sample = Sample.builder()
                    .response("The quick brown fox jumped over the lazy dog.")
                    .reference("The quick brown fox jumps over the lazy dog.")
                    .build();

            final BleuScoreConfig config = BleuScoreConfig.builder().build();

            final Double score = bleuScoreMetric.singleTurnScore(config, sample);

            log.info("BLEU Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            assertTrue(score >= 0.5, "Similar texts should have decent BLEU score. Received: " + score);
        }

        @Test
        @DisplayName("Different texts - EXPECTED LOW SCORE")
        void testDifferentTexts() {
            log.info("=== Different Texts Test ===");

            final Sample sample = Sample.builder()
                    .response("Machine learning models require large datasets.")
                    .reference("The weather is nice today in Paris.")
                    .build();

            final BleuScoreConfig config = BleuScoreConfig.builder().build();

            final Double score = bleuScoreMetric.singleTurnScore(config, sample);

            log.info("BLEU Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            assertTrue(score <= 0.3, "Different texts should have low BLEU score. Received: " + score);
        }
    }

    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {

        @Test
        @DisplayName("Different n-gram orders")
        void testDifferentNgramOrders() {
            log.info("=== N-gram Order Test ===");

            final Sample sample = Sample.builder()
                    .response("The cat sat on the mat.")
                    .reference("The cat sat on the floor.")
                    .build();

            final BleuScoreConfig config1 =
                    BleuScoreConfig.builder().maxNgram(1).build();
            final BleuScoreConfig config4 =
                    BleuScoreConfig.builder().maxNgram(4).build();

            final Double score1 = bleuScoreMetric.singleTurnScore(config1, sample);
            final Double score4 = bleuScoreMetric.singleTurnScore(config4, sample);

            log.info("BLEU-1 Score: {}", String.format("%.4f", score1));
            log.info("BLEU-4 Score: {}", String.format("%.4f", score4));

            assertNotNull(score1);
            assertNotNull(score4);
            // Higher n-gram order is usually more strict
            assertTrue(score1 >= score4, "BLEU-1 should be >= BLEU-4");
        }

        @Test
        @DisplayName("Smoothing effect")
        void testSmoothingEffect() {
            log.info("=== Smoothing Test ===");

            final Sample sample = Sample.builder()
                    .response("Hello world")
                    .reference("Goodbye world")
                    .build();

            final BleuScoreConfig smoothed =
                    BleuScoreConfig.builder().smoothing(true).build();
            final BleuScoreConfig unsmoothed =
                    BleuScoreConfig.builder().smoothing(false).build();

            final Double scoreSmoothed = bleuScoreMetric.singleTurnScore(smoothed, sample);
            final Double scoreUnsmoothed = bleuScoreMetric.singleTurnScore(unsmoothed, sample);

            log.info("Smoothed Score: {}", String.format("%.4f", scoreSmoothed));
            log.info("Unsmoothed Score: {}", String.format("%.4f", scoreUnsmoothed));

            assertNotNull(scoreSmoothed);
            assertNotNull(scoreUnsmoothed);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Short response with longer reference")
        void testBrevityPenalty() {
            log.info("=== Brevity Penalty Test ===");

            final Sample sample = Sample.builder()
                    .response("Hello")
                    .reference("Hello world how are you doing today")
                    .build();

            final BleuScoreConfig config = BleuScoreConfig.builder().build();

            final Double score = bleuScoreMetric.singleTurnScore(config, sample);

            log.info("BLEU Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            // Brevity penalty should reduce the score
            assertTrue(score < 0.8, "Short response should be penalized. Received: " + score);
        }

        @Test
        @DisplayName("Async scoring works correctly")
        void testAsyncScoring() {
            log.info("=== Async Scoring Test ===");

            final Sample sample = Sample.builder()
                    .response("The quick brown fox")
                    .reference("The quick brown fox")
                    .build();

            final BleuScoreConfig config = BleuScoreConfig.builder().build();

            final Double score =
                    bleuScoreMetric.singleTurnScoreAsync(config, sample).join();

            log.info("Async BLEU Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            assertTrue(score >= 0.95, "Async scoring should work correctly. Received: " + score);
        }
    }
}
