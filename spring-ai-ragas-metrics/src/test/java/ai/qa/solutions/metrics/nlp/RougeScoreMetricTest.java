package ai.qa.solutions.metrics.nlp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import ai.qa.solutions.metrics.nlp.RougeScoreMetric.Mode;
import ai.qa.solutions.metrics.nlp.RougeScoreMetric.RougeScoreConfig;
import ai.qa.solutions.metrics.nlp.RougeScoreMetric.RougeType;
import ai.qa.solutions.sample.Sample;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RougeScoreMetric")
class RougeScoreMetricTest {

    private RougeScoreMetric metric;

    @BeforeEach
    void setUp() {
        metric = new RougeScoreMetric();
    }

    @Nested
    @DisplayName("ROUGE-1 (Unigram)")
    class Rouge1Tests {

        @Test
        @DisplayName("Should return perfect score for identical texts")
        void shouldReturnPerfectScoreForIdenticalTexts() {
            final Sample sample = Sample.builder()
                    .response("The quick brown fox jumps over the lazy dog")
                    .reference("The quick brown fox jumps over the lazy dog")
                    .build();

            final RougeScoreConfig config =
                    RougeScoreConfig.builder().rougeType(RougeType.ROUGE_1).build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isCloseTo(1.0, within(0.01));
        }

        @Test
        @DisplayName("Should return moderate score for similar texts")
        void shouldReturnModerateScoreForSimilarTexts() {
            final Sample sample = Sample.builder()
                    .response("The quick fox jumps")
                    .reference("The quick brown fox jumps over the lazy dog")
                    .build();

            final RougeScoreConfig config =
                    RougeScoreConfig.builder().rougeType(RougeType.ROUGE_1).build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isBetween(0.3, 0.9);
        }

        @Test
        @DisplayName("Should return zero for completely different texts")
        void shouldReturnZeroForDifferentTexts() {
            final Sample sample = Sample.builder()
                    .response("alpha beta gamma")
                    .reference("one two three four five")
                    .build();

            final RougeScoreConfig config =
                    RougeScoreConfig.builder().rougeType(RougeType.ROUGE_1).build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("ROUGE-2 (Bigram)")
    class Rouge2Tests {

        @Test
        @DisplayName("Should return perfect score for identical texts")
        void shouldReturnPerfectScoreForIdenticalTexts() {
            final Sample sample = Sample.builder()
                    .response("The quick brown fox")
                    .reference("The quick brown fox")
                    .build();

            final RougeScoreConfig config =
                    RougeScoreConfig.builder().rougeType(RougeType.ROUGE_2).build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isCloseTo(1.0, within(0.01));
        }

        @Test
        @DisplayName("Should be stricter than ROUGE-1")
        void shouldBeStricterThanRouge1() {
            final Sample sample = Sample.builder()
                    .response("The fox quick brown")
                    .reference("The quick brown fox")
                    .build();

            final RougeScoreConfig config1 =
                    RougeScoreConfig.builder().rougeType(RougeType.ROUGE_1).build();
            final RougeScoreConfig config2 =
                    RougeScoreConfig.builder().rougeType(RougeType.ROUGE_2).build();

            final Double rouge1Score = metric.singleTurnScore(config1, sample);
            final Double rouge2Score = metric.singleTurnScore(config2, sample);

            // ROUGE-2 should be more sensitive to word order
            assertThat(rouge2Score).isLessThanOrEqualTo(rouge1Score);
        }
    }

    @Nested
    @DisplayName("ROUGE-L (LCS)")
    class RougeLTests {

        @Test
        @DisplayName("Should return perfect score for identical texts")
        void shouldReturnPerfectScoreForIdenticalTexts() {
            final Sample sample = Sample.builder()
                    .response("The quick brown fox")
                    .reference("The quick brown fox")
                    .build();

            final RougeScoreConfig config =
                    RougeScoreConfig.builder().rougeType(RougeType.ROUGE_L).build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isCloseTo(1.0, within(0.01));
        }

        @Test
        @DisplayName("Should detect longest common subsequence")
        void shouldDetectLongestCommonSubsequence() {
            final Sample sample = Sample.builder()
                    .response("The quick brown fox")
                    .reference("The very quick and brown fox runs")
                    .build();

            final RougeScoreConfig config =
                    RougeScoreConfig.builder().rougeType(RougeType.ROUGE_L).build();

            final Double score = metric.singleTurnScore(config, sample);

            // LCS should find "The quick brown fox"
            assertThat(score).isGreaterThan(0.5);
        }

        @Test
        @DisplayName("Should handle word order differences")
        void shouldHandleWordOrderDifferences() {
            final Sample sample = Sample.builder()
                    .response("fox brown quick the")
                    .reference("the quick brown fox")
                    .build();

            final RougeScoreConfig config =
                    RougeScoreConfig.builder().rougeType(RougeType.ROUGE_L).build();

            final Double score = metric.singleTurnScore(config, sample);

            // LCS only finds common subsequence, not contiguous match
            assertThat(score).isBetween(0.1, 0.6);
        }
    }

    @Nested
    @DisplayName("Scoring Modes")
    class ScoringModes {

        @Test
        @DisplayName("Should compute recall correctly")
        void shouldComputeRecallCorrectly() {
            final Sample sample = Sample.builder()
                    .response("The quick brown fox and something extra")
                    .reference("The quick brown fox")
                    .build();

            final RougeScoreConfig config = RougeScoreConfig.builder()
                    .rougeType(RougeType.ROUGE_1)
                    .mode(Mode.RECALL)
                    .build();

            final Double score = metric.singleTurnScore(config, sample);

            // All reference words are in response -> recall should be 1.0
            assertThat(score).isCloseTo(1.0, within(0.01));
        }

        @Test
        @DisplayName("Should compute precision correctly")
        void shouldComputePrecisionCorrectly() {
            final Sample sample = Sample.builder()
                    .response("The quick brown fox")
                    .reference("The quick brown fox and many more words")
                    .build();

            final RougeScoreConfig config = RougeScoreConfig.builder()
                    .rougeType(RougeType.ROUGE_1)
                    .mode(Mode.PRECISION)
                    .build();

            final Double score = metric.singleTurnScore(config, sample);

            // All response words are in reference -> precision should be 1.0
            assertThat(score).isCloseTo(1.0, within(0.01));
        }

        @Test
        @DisplayName("Should compute F-measure correctly")
        void shouldComputeFMeasureCorrectly() {
            final Sample sample = Sample.builder()
                    .response("The quick brown fox")
                    .reference("The quick brown fox")
                    .build();

            final RougeScoreConfig config = RougeScoreConfig.builder()
                    .rougeType(RougeType.ROUGE_1)
                    .mode(Mode.FMEASURE)
                    .build();

            final Double score = metric.singleTurnScore(config, sample);

            // Identical texts -> F1 should be 1.0
            assertThat(score).isCloseTo(1.0, within(0.01));
        }

        @Test
        @DisplayName("F-measure should be between precision and recall")
        void fMeasureShouldBeBetweenPrecisionAndRecall() {
            final Sample sample = Sample.builder()
                    .response("The quick fox jumps high and far")
                    .reference("The quick brown fox jumps over the lazy dog")
                    .build();

            final RougeScoreConfig precConfig = RougeScoreConfig.builder()
                    .rougeType(RougeType.ROUGE_1)
                    .mode(Mode.PRECISION)
                    .build();
            final RougeScoreConfig recConfig = RougeScoreConfig.builder()
                    .rougeType(RougeType.ROUGE_1)
                    .mode(Mode.RECALL)
                    .build();
            final RougeScoreConfig f1Config = RougeScoreConfig.builder()
                    .rougeType(RougeType.ROUGE_1)
                    .mode(Mode.FMEASURE)
                    .build();

            final Double precision = metric.singleTurnScore(precConfig, sample);
            final Double recall = metric.singleTurnScore(recConfig, sample);
            final Double f1 = metric.singleTurnScore(f1Config, sample);

            assertThat(f1).isBetween(Math.min(precision, recall), Math.max(precision, recall));
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

            final RougeScoreConfig config = RougeScoreConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isNull();
        }

        @Test
        @DisplayName("Should return null for empty response")
        void shouldReturnNullForEmptyResponse() {
            final Sample sample =
                    Sample.builder().response("").reference("Reference").build();

            final RougeScoreConfig config = RougeScoreConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isNull();
        }

        @Test
        @DisplayName("Should return null for null reference")
        void shouldReturnNullForNullReference() {
            final Sample sample =
                    Sample.builder().response("Response").reference(null).build();

            final RougeScoreConfig config = RougeScoreConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isNull();
        }

        @Test
        @DisplayName("Should return null for empty reference")
        void shouldReturnNullForEmptyReference() {
            final Sample sample =
                    Sample.builder().response("Response").reference("").build();

            final RougeScoreConfig config = RougeScoreConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isNull();
        }
    }

    @Nested
    @DisplayName("Case Sensitivity")
    class CaseSensitivity {

        @Test
        @DisplayName("Should be case insensitive")
        void shouldBeCaseInsensitive() {
            final Sample sample = Sample.builder()
                    .response("THE QUICK BROWN FOX")
                    .reference("the quick brown fox")
                    .build();

            final RougeScoreConfig config = RougeScoreConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isCloseTo(1.0, within(0.01));
        }
    }

    @Nested
    @DisplayName("Punctuation Handling")
    class PunctuationHandling {

        @Test
        @DisplayName("Should ignore punctuation")
        void shouldIgnorePunctuation() {
            final Sample sample = Sample.builder()
                    .response("Hello, world! How are you?")
                    .reference("Hello world How are you")
                    .build();

            final RougeScoreConfig config = RougeScoreConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isCloseTo(1.0, within(0.01));
        }
    }

    @Nested
    @DisplayName("Multilingual Support")
    class MultilingualSupport {

        @Test
        @DisplayName("Should handle Russian text")
        void shouldHandleRussianText() {
            final Sample sample = Sample.builder()
                    .response("Привет мир как дела")
                    .reference("Привет мир как дела")
                    .build();

            final RougeScoreConfig config = RougeScoreConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isCloseTo(1.0, within(0.01));
        }
    }

    @Nested
    @DisplayName("Async Execution")
    class AsyncExecution {

        @Test
        @DisplayName("Should execute asynchronously")
        void shouldExecuteAsynchronously() {
            final Sample sample = Sample.builder()
                    .response("The quick brown fox")
                    .reference("The quick brown fox")
                    .build();

            final RougeScoreConfig config = RougeScoreConfig.builder().build();

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
            final RougeScoreConfig config = RougeScoreConfig.builder().build();

            assertThat(config.getRougeType()).isEqualTo(RougeType.ROUGE_L);
            assertThat(config.getMode()).isEqualTo(Mode.FMEASURE);
        }

        @Test
        @DisplayName("Should allow custom config values")
        void shouldAllowCustomConfigValues() {
            final RougeScoreConfig config = RougeScoreConfig.builder()
                    .rougeType(RougeType.ROUGE_2)
                    .mode(Mode.RECALL)
                    .build();

            assertThat(config.getRougeType()).isEqualTo(RougeType.ROUGE_2);
            assertThat(config.getMode()).isEqualTo(Mode.RECALL);
        }
    }

    @Nested
    @DisplayName("Metric Name")
    class MetricName {

        @Test
        @DisplayName("Should return correct metric name")
        void shouldReturnCorrectMetricName() {
            assertThat(metric.getName()).isEqualTo("RougeScoreMetric");
        }
    }
}
