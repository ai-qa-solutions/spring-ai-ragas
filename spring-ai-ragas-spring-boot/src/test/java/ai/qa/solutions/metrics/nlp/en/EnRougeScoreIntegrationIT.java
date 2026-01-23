package ai.qa.solutions.metrics.nlp.en;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.allure.nlp.AllureNlpMetricHelper;
import ai.qa.solutions.metrics.nlp.RougeScoreMetric;
import ai.qa.solutions.metrics.nlp.RougeScoreMetric.Mode;
import ai.qa.solutions.metrics.nlp.RougeScoreMetric.RougeScoreConfig;
import ai.qa.solutions.metrics.nlp.RougeScoreMetric.RougeType;
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
 * Integration tests for ROUGE Score Metric - English Language.
 * <p>
 * Tests ROUGE-1, ROUGE-2, and ROUGE-L score calculations.
 * This is a non-LLM metric that doesn't require API keys.
 */
@Slf4j
@EnableAutoConfiguration
@SuppressWarnings("LoggingSimilarMessage")
@DisplayName("ROUGE Score Metric - English Language Validation")
@SpringBootTest(classes = EnRougeScoreIntegrationIT.RougeScoreIntegrationTestConfiguration.class)
class EnRougeScoreIntegrationIT {

    @Configuration
    public static class RougeScoreIntegrationTestConfiguration {}

    @Autowired
    private RougeScoreMetric rougeScoreMetric;

    @Nested
    @DisplayName("ROUGE-L Tests")
    class RougeLTests {

        @Test
        @DisplayName("Identical texts - EXPECTED PERFECT SCORE")
        void testIdenticalTexts() {
            log.info("=== Identical Texts Test ===");

            final Sample sample = Sample.builder()
                    .response("The quick brown fox jumps over the lazy dog.")
                    .reference("The quick brown fox jumps over the lazy dog.")
                    .build();

            final RougeScoreConfig config = RougeScoreConfig.builder()
                    .rougeType(RougeType.ROUGE_L)
                    .mode(Mode.FMEASURE)
                    .build();

            final Double score = rougeScoreMetric.singleTurnScore(config, sample);

            log.info("ROUGE-L Score: {}", String.format("%.4f", score));

            AllureNlpMetricHelper.attachRougeScore(
                    score,
                    sample.getResponse(),
                    sample.getReference(),
                    config.getRougeType().name(),
                    config.getMode().name(),
                    "en");

            assertNotNull(score);
            assertTrue(score >= 0.99, "Identical texts should have perfect ROUGE-L. Received: " + score);
        }

        @Test
        @DisplayName("Similar texts - EXPECTED HIGH SCORE")
        void testSimilarTexts() {
            log.info("=== Similar Texts Test ===");

            final Sample sample = Sample.builder()
                    .response("The cat sat on the mat.")
                    .reference("The cat sat on the floor mat.")
                    .build();

            final RougeScoreConfig config =
                    RougeScoreConfig.builder().rougeType(RougeType.ROUGE_L).build();

            final Double score = rougeScoreMetric.singleTurnScore(config, sample);

            log.info("ROUGE-L Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            assertTrue(score >= 0.6, "Similar texts should have good ROUGE-L. Received: " + score);
        }
    }

    @Nested
    @DisplayName("ROUGE-1 and ROUGE-2 Tests")
    class RougeNTests {

        @Test
        @DisplayName("ROUGE-1 unigram overlap")
        void testRouge1() {
            log.info("=== ROUGE-1 Test ===");

            final Sample sample = Sample.builder()
                    .response("Machine learning is a subset of artificial intelligence.")
                    .reference("Artificial intelligence includes machine learning as a subset.")
                    .build();

            final RougeScoreConfig config = RougeScoreConfig.builder()
                    .rougeType(RougeType.ROUGE_1)
                    .mode(Mode.FMEASURE)
                    .build();

            final Double score = rougeScoreMetric.singleTurnScore(config, sample);

            log.info("ROUGE-1 Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            assertTrue(score >= 0.5, "ROUGE-1 should capture word overlap. Received: " + score);
        }

        @Test
        @DisplayName("ROUGE-2 bigram overlap")
        void testRouge2() {
            log.info("=== ROUGE-2 Test ===");

            final Sample sample = Sample.builder()
                    .response("The quick brown fox jumps.")
                    .reference("The quick brown fox runs.")
                    .build();

            final RougeScoreConfig config = RougeScoreConfig.builder()
                    .rougeType(RougeType.ROUGE_2)
                    .mode(Mode.FMEASURE)
                    .build();

            final Double score = rougeScoreMetric.singleTurnScore(config, sample);

            log.info("ROUGE-2 Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            assertTrue(score >= 0.4, "ROUGE-2 should capture bigram overlap. Received: " + score);
        }
    }

    @Nested
    @DisplayName("Scoring Mode Tests")
    class ScoringModeTests {

        @Test
        @DisplayName("Precision vs Recall vs F-measure")
        void testScoringModes() {
            log.info("=== Scoring Modes Test ===");

            final Sample sample = Sample.builder()
                    .response("The quick brown")
                    .reference("The quick brown fox jumps over the lazy dog")
                    .build();

            final Double precision = rougeScoreMetric.singleTurnScore(
                    RougeScoreConfig.builder().mode(Mode.PRECISION).build(), sample);
            final Double recall = rougeScoreMetric.singleTurnScore(
                    RougeScoreConfig.builder().mode(Mode.RECALL).build(), sample);
            final Double fmeasure = rougeScoreMetric.singleTurnScore(
                    RougeScoreConfig.builder().mode(Mode.FMEASURE).build(), sample);

            log.info("Precision: {}", String.format("%.4f", precision));
            log.info("Recall: {}", String.format("%.4f", recall));
            log.info("F-measure: {}", String.format("%.4f", fmeasure));

            assertNotNull(precision);
            assertNotNull(recall);
            assertNotNull(fmeasure);

            // Short response = high precision, low recall
            assertTrue(precision >= recall, "Short response should have higher precision than recall");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Async scoring")
        void testAsyncScoring() {
            log.info("=== Async Scoring Test ===");

            final Sample sample = Sample.builder()
                    .response("Hello world")
                    .reference("Hello world")
                    .build();

            final RougeScoreConfig config = RougeScoreConfig.builder().build();

            final Double score =
                    rougeScoreMetric.singleTurnScoreAsync(config, sample).join();

            log.info("Async ROUGE-L Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            assertTrue(score >= 0.99, "Async scoring should work correctly. Received: " + score);
        }
    }
}
