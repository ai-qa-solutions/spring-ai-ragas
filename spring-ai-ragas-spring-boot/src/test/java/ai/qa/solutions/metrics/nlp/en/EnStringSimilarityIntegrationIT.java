package ai.qa.solutions.metrics.nlp.en;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.nlp.StringSimilarityMetric;
import ai.qa.solutions.metrics.nlp.StringSimilarityMetric.DistanceMeasure;
import ai.qa.solutions.metrics.nlp.StringSimilarityMetric.StringSimilarityConfig;
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
 * Integration tests for String Similarity Metric - English Language.
 * <p>
 * Tests various string distance algorithms (Levenshtein, Hamming, Jaro, Jaro-Winkler).
 * This is a non-LLM metric that doesn't require API keys.
 */
@Slf4j
@EnableAutoConfiguration
@SuppressWarnings("LoggingSimilarMessage")
@DisplayName("String Similarity Metric - English Language Validation")
@SpringBootTest(classes = EnStringSimilarityIntegrationIT.StringSimilarityIntegrationTestConfiguration.class)
class EnStringSimilarityIntegrationIT {

    @Configuration
    public static class StringSimilarityIntegrationTestConfiguration {}

    @Autowired
    private StringSimilarityMetric stringSimilarityMetric;

    @Nested
    @DisplayName("Jaro-Winkler Tests (Default)")
    class JaroWinklerTests {

        @Test
        @DisplayName("Identical strings - EXPECTED PERFECT SCORE")
        void testIdenticalStrings() {
            log.info("=== Identical Strings Test ===");

            final Sample sample = Sample.builder()
                    .response("Hello World")
                    .reference("Hello World")
                    .build();

            final StringSimilarityConfig config =
                    StringSimilarityConfig.builder().build();

            final Double score = stringSimilarityMetric.singleTurnScore(config, sample);

            log.info("Jaro-Winkler Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            assertTrue(score >= 0.99, "Identical strings should have perfect score. Received: " + score);
        }

        @Test
        @DisplayName("Similar strings with common prefix")
        void testSimilarStringsWithPrefix() {
            log.info("=== Similar Strings with Prefix Test ===");

            final Sample sample = Sample.builder()
                    .response("prefix_abc")
                    .reference("prefix_xyz")
                    .build();

            final StringSimilarityConfig config = StringSimilarityConfig.builder()
                    .distanceMeasure(DistanceMeasure.JARO_WINKLER)
                    .build();

            final Double score = stringSimilarityMetric.singleTurnScore(config, sample);

            log.info("Jaro-Winkler Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            // Jaro-Winkler gives bonus for common prefix
            assertTrue(score >= 0.7, "Common prefix should boost score. Received: " + score);
        }
    }

    @Nested
    @DisplayName("Levenshtein Tests")
    class LevenshteinTests {

        @Test
        @DisplayName("Single character difference")
        void testSingleCharacterDifference() {
            log.info("=== Single Character Difference Test ===");

            final Sample sample =
                    Sample.builder().response("hello").reference("hallo").build();

            final StringSimilarityConfig config = StringSimilarityConfig.builder()
                    .distanceMeasure(DistanceMeasure.LEVENSHTEIN)
                    .build();

            final Double score = stringSimilarityMetric.singleTurnScore(config, sample);

            log.info("Levenshtein Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            // 1 edit out of 5 characters = 0.8 similarity
            assertTrue(score >= 0.75, "Single character difference should have high score. Received: " + score);
        }

        @Test
        @DisplayName("Completely different strings")
        void testCompletelyDifferentStrings() {
            log.info("=== Completely Different Strings Test ===");

            final Sample sample =
                    Sample.builder().response("abc").reference("xyz").build();

            final StringSimilarityConfig config = StringSimilarityConfig.builder()
                    .distanceMeasure(DistanceMeasure.LEVENSHTEIN)
                    .build();

            final Double score = stringSimilarityMetric.singleTurnScore(config, sample);

            log.info("Levenshtein Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            assertTrue(score <= 0.1, "Completely different strings should have very low score. Received: " + score);
        }
    }

    @Nested
    @DisplayName("Algorithm Comparison Tests")
    class AlgorithmComparisonTests {

        @Test
        @DisplayName("Compare all distance measures")
        void testCompareAlgorithms() {
            log.info("=== Algorithm Comparison Test ===");

            final Sample sample =
                    Sample.builder().response("receive").reference("recieve").build();

            final Double levenshtein = stringSimilarityMetric.singleTurnScore(
                    StringSimilarityConfig.builder()
                            .distanceMeasure(DistanceMeasure.LEVENSHTEIN)
                            .build(),
                    sample);

            final Double jaro = stringSimilarityMetric.singleTurnScore(
                    StringSimilarityConfig.builder()
                            .distanceMeasure(DistanceMeasure.JARO)
                            .build(),
                    sample);

            final Double jaroWinkler = stringSimilarityMetric.singleTurnScore(
                    StringSimilarityConfig.builder()
                            .distanceMeasure(DistanceMeasure.JARO_WINKLER)
                            .build(),
                    sample);

            log.info("Levenshtein: {}", String.format("%.4f", levenshtein));
            log.info("Jaro: {}", String.format("%.4f", jaro));
            log.info("Jaro-Winkler: {}", String.format("%.4f", jaroWinkler));

            assertNotNull(levenshtein);
            assertNotNull(jaro);
            assertNotNull(jaroWinkler);

            // All should detect the similarity
            assertTrue(levenshtein >= 0.7, "Levenshtein should detect similarity");
            assertTrue(jaro >= 0.8, "Jaro should detect similarity");
            assertTrue(jaroWinkler >= 0.8, "Jaro-Winkler should detect similarity");
        }
    }

    @Nested
    @DisplayName("Case Sensitivity Tests")
    class CaseSensitivityTests {

        @Test
        @DisplayName("Case insensitive by default")
        void testCaseInsensitiveDefault() {
            log.info("=== Case Insensitive Test ===");

            final Sample sample = Sample.builder()
                    .response("HELLO WORLD")
                    .reference("hello world")
                    .build();

            final StringSimilarityConfig config =
                    StringSimilarityConfig.builder().build();

            final Double score = stringSimilarityMetric.singleTurnScore(config, sample);

            log.info("Score (case insensitive): {}", String.format("%.4f", score));

            assertNotNull(score);
            assertTrue(score >= 0.99, "Case insensitive should match. Received: " + score);
        }

        @Test
        @DisplayName("Case sensitive when configured")
        void testCaseSensitive() {
            log.info("=== Case Sensitive Test ===");

            final Sample sample =
                    Sample.builder().response("HELLO").reference("hello").build();

            final StringSimilarityConfig config = StringSimilarityConfig.builder()
                    .caseSensitive(true)
                    .distanceMeasure(DistanceMeasure.LEVENSHTEIN)
                    .build();

            final Double score = stringSimilarityMetric.singleTurnScore(config, sample);

            log.info("Score (case sensitive): {}", String.format("%.4f", score));

            assertNotNull(score);
            // All 5 characters are different when case sensitive
            assertTrue(score <= 0.1, "Case sensitive should not match. Received: " + score);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Async scoring")
        void testAsyncScoring() {
            log.info("=== Async Scoring Test ===");

            final Sample sample =
                    Sample.builder().response("hello").reference("hello").build();

            final StringSimilarityConfig config =
                    StringSimilarityConfig.builder().build();

            final Double score =
                    stringSimilarityMetric.singleTurnScoreAsync(config, sample).join();

            log.info("Async Score: {}", String.format("%.4f", score));

            assertNotNull(score);
            assertTrue(score >= 0.99, "Async scoring should work correctly. Received: " + score);
        }
    }
}
