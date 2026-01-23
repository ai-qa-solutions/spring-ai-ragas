package ai.qa.solutions.metrics.nlp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import ai.qa.solutions.metrics.nlp.StringSimilarityMetric.DistanceMeasure;
import ai.qa.solutions.metrics.nlp.StringSimilarityMetric.StringSimilarityConfig;
import ai.qa.solutions.sample.Sample;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("StringSimilarityMetric")
class StringSimilarityMetricTest {

    private StringSimilarityMetric metric;

    @BeforeEach
    void setUp() {
        metric = new StringSimilarityMetric();
    }

    @Nested
    @DisplayName("Levenshtein Distance")
    class LevenshteinTests {

        @Test
        @DisplayName("Should return perfect score for identical strings")
        void shouldReturnPerfectScoreForIdenticalStrings() {
            final Sample sample = Sample.builder()
                    .response("hello world")
                    .reference("hello world")
                    .build();

            final StringSimilarityConfig config = StringSimilarityConfig.builder()
                    .distanceMeasure(DistanceMeasure.LEVENSHTEIN)
                    .build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isCloseTo(1.0, within(0.01));
        }

        @Test
        @DisplayName("Should detect single character difference")
        void shouldDetectSingleCharacterDifference() {
            final Sample sample =
                    Sample.builder().response("hello").reference("hallo").build();

            final StringSimilarityConfig config = StringSimilarityConfig.builder()
                    .distanceMeasure(DistanceMeasure.LEVENSHTEIN)
                    .build();

            final Double score = metric.singleTurnScore(config, sample);

            // 1 edit out of 5 characters = 0.8 similarity
            assertThat(score).isCloseTo(0.8, within(0.01));
        }

        @Test
        @DisplayName("Should return zero for completely different strings")
        void shouldReturnZeroForCompletelyDifferentStrings() {
            final Sample sample =
                    Sample.builder().response("abc").reference("xyz").build();

            final StringSimilarityConfig config = StringSimilarityConfig.builder()
                    .distanceMeasure(DistanceMeasure.LEVENSHTEIN)
                    .build();

            final Double score = metric.singleTurnScore(config, sample);

            // 3 substitutions out of 3 = 0.0 similarity
            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should handle insertions and deletions")
        void shouldHandleInsertionsAndDeletions() {
            final Sample sample =
                    Sample.builder().response("kitten").reference("sitting").build();

            final StringSimilarityConfig config = StringSimilarityConfig.builder()
                    .distanceMeasure(DistanceMeasure.LEVENSHTEIN)
                    .build();

            final Double score = metric.singleTurnScore(config, sample);

            // 3 edits out of 7 characters
            assertThat(score).isBetween(0.5, 0.7);
        }
    }

    @Nested
    @DisplayName("Hamming Distance")
    class HammingTests {

        @Test
        @DisplayName("Should return perfect score for identical strings")
        void shouldReturnPerfectScoreForIdenticalStrings() {
            final Sample sample =
                    Sample.builder().response("hello").reference("hello").build();

            final StringSimilarityConfig config = StringSimilarityConfig.builder()
                    .distanceMeasure(DistanceMeasure.HAMMING)
                    .build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isCloseTo(1.0, within(0.01));
        }

        @Test
        @DisplayName("Should detect substitutions")
        void shouldDetectSubstitutions() {
            final Sample sample =
                    Sample.builder().response("hello").reference("hallo").build();

            final StringSimilarityConfig config = StringSimilarityConfig.builder()
                    .distanceMeasure(DistanceMeasure.HAMMING)
                    .build();

            final Double score = metric.singleTurnScore(config, sample);

            // 1 substitution out of 5 = 0.8
            assertThat(score).isCloseTo(0.8, within(0.01));
        }

        @Test
        @DisplayName("Should handle different length strings with padding")
        void shouldHandleDifferentLengthStringsWithPadding() {
            final Sample sample =
                    Sample.builder().response("hello").reference("hello world").build();

            final StringSimilarityConfig config = StringSimilarityConfig.builder()
                    .distanceMeasure(DistanceMeasure.HAMMING)
                    .build();

            final Double score = metric.singleTurnScore(config, sample);

            // "hello" padded to "hello      " vs "hello world"
            // Positions 5-10 are different = 6 differences out of 11
            assertThat(score).isBetween(0.3, 0.6);
        }
    }

    @Nested
    @DisplayName("Jaro Similarity")
    class JaroTests {

        @Test
        @DisplayName("Should return perfect score for identical strings")
        void shouldReturnPerfectScoreForIdenticalStrings() {
            final Sample sample =
                    Sample.builder().response("hello").reference("hello").build();

            final StringSimilarityConfig config = StringSimilarityConfig.builder()
                    .distanceMeasure(DistanceMeasure.JARO)
                    .build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isCloseTo(1.0, within(0.01));
        }

        @Test
        @DisplayName("Should detect similar strings")
        void shouldDetectSimilarStrings() {
            final Sample sample =
                    Sample.builder().response("martha").reference("marhta").build();

            final StringSimilarityConfig config = StringSimilarityConfig.builder()
                    .distanceMeasure(DistanceMeasure.JARO)
                    .build();

            final Double score = metric.singleTurnScore(config, sample);

            // Classic Jaro example - should be around 0.944
            assertThat(score).isGreaterThan(0.9);
        }
    }

    @Nested
    @DisplayName("Jaro-Winkler Similarity")
    class JaroWinklerTests {

        @Test
        @DisplayName("Should return perfect score for identical strings")
        void shouldReturnPerfectScoreForIdenticalStrings() {
            final Sample sample =
                    Sample.builder().response("hello").reference("hello").build();

            final StringSimilarityConfig config = StringSimilarityConfig.builder()
                    .distanceMeasure(DistanceMeasure.JARO_WINKLER)
                    .build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isCloseTo(1.0, within(0.01));
        }

        @Test
        @DisplayName("Should give bonus for common prefix")
        void shouldGiveBonusForCommonPrefix() {
            final Sample sample = Sample.builder()
                    .response("prefix_abc")
                    .reference("prefix_xyz")
                    .build();

            final StringSimilarityConfig jaroConfig = StringSimilarityConfig.builder()
                    .distanceMeasure(DistanceMeasure.JARO)
                    .build();
            final StringSimilarityConfig jWConfig = StringSimilarityConfig.builder()
                    .distanceMeasure(DistanceMeasure.JARO_WINKLER)
                    .build();

            final Double jaroScore = metric.singleTurnScore(jaroConfig, sample);
            final Double jWScore = metric.singleTurnScore(jWConfig, sample);

            // Jaro-Winkler should be higher due to common prefix
            assertThat(jWScore).isGreaterThanOrEqualTo(jaroScore);
        }

        @Test
        @DisplayName("Should handle typical typos well")
        void shouldHandleTypicalTyposWell() {
            final Sample sample =
                    Sample.builder().response("receive").reference("recieve").build();

            final StringSimilarityConfig config = StringSimilarityConfig.builder()
                    .distanceMeasure(DistanceMeasure.JARO_WINKLER)
                    .build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isGreaterThan(0.9);
        }
    }

    @Nested
    @DisplayName("Case Sensitivity")
    class CaseSensitivity {

        @Test
        @DisplayName("Should be case insensitive by default")
        void shouldBeCaseInsensitiveByDefault() {
            final Sample sample =
                    Sample.builder().response("HELLO").reference("hello").build();

            final StringSimilarityConfig config =
                    StringSimilarityConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isCloseTo(1.0, within(0.01));
        }

        @Test
        @DisplayName("Should be case sensitive when configured")
        void shouldBeCaseSensitiveWhenConfigured() {
            final Sample sample =
                    Sample.builder().response("HELLO").reference("hello").build();

            final StringSimilarityConfig config = StringSimilarityConfig.builder()
                    .caseSensitive(true)
                    .distanceMeasure(DistanceMeasure.LEVENSHTEIN)
                    .build();

            final Double score = metric.singleTurnScore(config, sample);

            // All 5 characters are different when case sensitive
            assertThat(score).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Input Validation")
    class InputValidation {

        @Test
        @DisplayName("Should return null for null response")
        void shouldReturnNullForNullResponse() {
            final Sample sample =
                    Sample.builder().response(null).reference("Reference").build();

            final StringSimilarityConfig config =
                    StringSimilarityConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isNull();
        }

        @Test
        @DisplayName("Should return null for empty response")
        void shouldReturnNullForEmptyResponse() {
            final Sample sample =
                    Sample.builder().response("").reference("Reference").build();

            final StringSimilarityConfig config =
                    StringSimilarityConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isNull();
        }

        @Test
        @DisplayName("Should return null for null reference")
        void shouldReturnNullForNullReference() {
            final Sample sample =
                    Sample.builder().response("Response").reference(null).build();

            final StringSimilarityConfig config =
                    StringSimilarityConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isNull();
        }

        @Test
        @DisplayName("Should return null for empty reference")
        void shouldReturnNullForEmptyReference() {
            final Sample sample =
                    Sample.builder().response("Response").reference("").build();

            final StringSimilarityConfig config =
                    StringSimilarityConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isNull();
        }
    }

    @Nested
    @DisplayName("Async Execution")
    class AsyncExecution {

        @Test
        @DisplayName("Should execute asynchronously")
        void shouldExecuteAsynchronously() {
            final Sample sample =
                    Sample.builder().response("hello").reference("hello").build();

            final StringSimilarityConfig config =
                    StringSimilarityConfig.builder().build();

            final CompletableFuture<Double> future = metric.singleTurnScoreAsync(config, sample);

            assertThat(future).isNotNull();
            assertThat(future.join()).isCloseTo(1.0, within(0.01));
        }
    }

    @Nested
    @DisplayName("Configuration")
    class Configuration {

        @Test
        @DisplayName("Should use default config values")
        void shouldUseDefaultConfigValues() {
            final StringSimilarityConfig config =
                    StringSimilarityConfig.builder().build();

            assertThat(config.getDistanceMeasure()).isEqualTo(DistanceMeasure.JARO_WINKLER);
            assertThat(config.isCaseSensitive()).isFalse();
        }

        @Test
        @DisplayName("Should allow custom config values")
        void shouldAllowCustomConfigValues() {
            final StringSimilarityConfig config = StringSimilarityConfig.builder()
                    .distanceMeasure(DistanceMeasure.LEVENSHTEIN)
                    .caseSensitive(true)
                    .build();

            assertThat(config.getDistanceMeasure()).isEqualTo(DistanceMeasure.LEVENSHTEIN);
            assertThat(config.isCaseSensitive()).isTrue();
        }
    }

    @Nested
    @DisplayName("Metric Name")
    class MetricName {

        @Test
        @DisplayName("Should return correct metric name")
        void shouldReturnCorrectMetricName() {
            assertThat(metric.getName()).isEqualTo("StringSimilarityMetric");
        }
    }
}
