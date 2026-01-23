package ai.qa.solutions.metrics.nlp.en;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.allure.nlp.AllureNlpMetricHelper;
import ai.qa.solutions.metrics.nlp.ChrfScoreMetric;
import ai.qa.solutions.metrics.nlp.ChrfScoreMetric.ChrfScoreConfig;
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
 * Integration tests for chrF Score Metric - English Language.
 * <p>
 * Tests character-level n-gram F-score calculation.
 * This is a non-LLM metric that doesn't require API keys.
 */
@Slf4j
@EnableAutoConfiguration
@SuppressWarnings("LoggingSimilarMessage")
@DisplayName("chrF Score Metric - English Language Validation")
@SpringBootTest(classes = EnChrfScoreIntegrationIT.ChrfScoreIntegrationTestConfiguration.class)
class EnChrfScoreIntegrationIT {

    @Configuration
    public static class ChrfScoreIntegrationTestConfiguration {}

    @Autowired
    private ChrfScoreMetric chrfScoreMetric;

    @Nested
    @DisplayName("Basic Scoring Tests")
    class BasicScoringTests {

        @Test
        @DisplayName("Identical texts - EXPECTED PERFECT SCORE")
        void testIdenticalTexts() {
            log.info("=== Identical Texts Test ===");

            final Sample sample = Sample.builder()
                    .response("The quick brown fox jumps over the lazy dog.")
                    .reference("The quick brown fox jumps over the lazy dog.")
                    .build();

            final ChrfScoreConfig config = ChrfScoreConfig.builder().build();

            final Double score = chrfScoreMetric.singleTurnScore(config, sample);

            log.info("chrF Score: {}", String.format("%.4f", score));

            AllureNlpMetricHelper.attachChrfScore(
                    score,
                    sample.getResponse(),
                    sample.getReference(),
                    config.getCharNgramOrder(),
                    config.getWordNgramOrder(),
                    config.getBeta(),
                    "en");

            assertNotNull(score);
            assertTrue(score >= 0.99, "Identical texts should have perfect chrF score. Received: " + score);
        }

        @Test
        @DisplayName("Minor spelling difference - EXPECTED HIGH SCORE")
        void testMinorSpellingDifference() {
            log.info("=== Minor Spelling Difference Test ===");

            final Sample sample = Sample.builder()
                    .response("colour behaviour favour")
                    .reference("color behavior favor")
                    .build();

            final ChrfScoreConfig config = ChrfScoreConfig.builder().build();

            final Double score = chrfScoreMetric.singleTurnScore(config, sample);

            log.info("chrF Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            // chrF is robust to minor spelling variations
            assertTrue(score >= 0.5, "Minor spelling differences should still score well. Received: " + score);
        }

        @Test
        @DisplayName("Different texts - EXPECTED LOW SCORE")
        void testDifferentTexts() {
            log.info("=== Different Texts Test ===");

            final Sample sample = Sample.builder()
                    .response("Machine learning models")
                    .reference("The weather is sunny")
                    .build();

            final ChrfScoreConfig config = ChrfScoreConfig.builder().build();

            final Double score = chrfScoreMetric.singleTurnScore(config, sample);

            log.info("chrF Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            assertTrue(score <= 0.4, "Different texts should have low chrF score. Received: " + score);
        }
    }

    @Nested
    @DisplayName("chrF++ Mode Tests")
    class ChrfPlusPlusTests {

        @Test
        @DisplayName("chrF++ with word n-grams")
        void testChrfPlusPlus() {
            log.info("=== chrF++ Test ===");

            final Sample sample = Sample.builder()
                    .response("The quick brown fox")
                    .reference("The quick brown fox")
                    .build();

            // chrF (character only)
            final ChrfScoreConfig chrfConfig =
                    ChrfScoreConfig.builder().wordNgramOrder(0).build();
            // chrF++ (character + word)
            final ChrfScoreConfig chrfPPConfig =
                    ChrfScoreConfig.builder().wordNgramOrder(2).build();

            final Double chrfScore = chrfScoreMetric.singleTurnScore(chrfConfig, sample);
            final Double chrfPPScore = chrfScoreMetric.singleTurnScore(chrfPPConfig, sample);

            log.info("chrF Score: {}", String.format("%.4f", chrfScore));
            log.info("chrF++ Score: {}", String.format("%.4f", chrfPPScore));

            assertNotNull(chrfScore);
            assertNotNull(chrfPPScore);
            // Both should be high for identical texts
            assertTrue(chrfScore >= 0.9, "chrF should be high for identical texts");
            assertTrue(chrfPPScore >= 0.9, "chrF++ should be high for identical texts");
        }
    }

    @Nested
    @DisplayName("Typo Robustness Tests")
    class TypoRobustnessTests {

        @Test
        @DisplayName("Common typos")
        void testCommonTypos() {
            log.info("=== Common Typos Test ===");

            final Sample sample = Sample.builder()
                    .response("recieve occassion accomodate")
                    .reference("receive occasion accommodate")
                    .build();

            final ChrfScoreConfig config = ChrfScoreConfig.builder().build();

            final Double score = chrfScoreMetric.singleTurnScore(config, sample);

            log.info("chrF Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            // chrF is character-based, so typos with similar characters score well
            assertTrue(score >= 0.6, "Common typos should still score reasonably well. Received: " + score);
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

            final ChrfScoreConfig config = ChrfScoreConfig.builder().build();

            final Double score =
                    chrfScoreMetric.singleTurnScoreAsync(config, sample).join();

            log.info("Async chrF Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            assertTrue(score >= 0.99, "Async scoring should work correctly. Received: " + score);
        }
    }
}
