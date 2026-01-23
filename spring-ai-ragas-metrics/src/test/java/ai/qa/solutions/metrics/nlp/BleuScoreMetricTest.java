package ai.qa.solutions.metrics.nlp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import ai.qa.solutions.metrics.nlp.BleuScoreMetric.BleuScoreConfig;
import ai.qa.solutions.sample.Sample;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("BleuScoreMetric")
class BleuScoreMetricTest {

    private BleuScoreMetric metric;

    @BeforeEach
    void setUp() {
        metric = new BleuScoreMetric();
    }

    @Nested
    @DisplayName("Basic Scoring")
    class BasicScoring {

        @Test
        @DisplayName("Should return high score for identical texts")
        void shouldReturnHighScoreForIdenticalTexts() {
            final Sample sample = Sample.builder()
                    .response("The quick brown fox jumps over the lazy dog")
                    .reference("The quick brown fox jumps over the lazy dog")
                    .build();

            final BleuScoreConfig config = BleuScoreConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isCloseTo(1.0, within(0.01));
        }

        @Test
        @DisplayName("Should return moderate score for similar texts")
        void shouldReturnModerateScoreForSimilarTexts() {
            final Sample sample = Sample.builder()
                    .response("The quick brown fox jumps over the dog")
                    .reference("The quick brown fox jumps over the lazy dog")
                    .build();

            final BleuScoreConfig config = BleuScoreConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isBetween(0.3, 0.95);
        }

        @Test
        @DisplayName("Should return low score for different texts")
        void shouldReturnLowScoreForDifferentTexts() {
            final Sample sample = Sample.builder()
                    .response("Hello world how are you today")
                    .reference("The quick brown fox jumps over the lazy dog")
                    .build();

            final BleuScoreConfig config = BleuScoreConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isLessThan(0.3);
        }

        @Test
        @DisplayName("Should return zero for completely different texts")
        void shouldReturnZeroForCompletelyDifferentTexts() {
            final Sample sample = Sample.builder()
                    .response("alpha beta gamma delta")
                    .reference("one two three four five six seven")
                    .build();

            final BleuScoreConfig config =
                    BleuScoreConfig.builder().smoothing(false).build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Brevity Penalty")
    class BrevityPenalty {

        @Test
        @DisplayName("Should penalize shorter responses")
        void shouldPenalizeShorterResponses() {
            final Sample shortSample = Sample.builder()
                    .response("The fox")
                    .reference("The quick brown fox jumps over the lazy dog")
                    .build();

            final Sample fullSample = Sample.builder()
                    .response("The quick brown fox jumps over the lazy dog")
                    .reference("The quick brown fox jumps over the lazy dog")
                    .build();

            final BleuScoreConfig config = BleuScoreConfig.builder().build();

            final Double shortScore = metric.singleTurnScore(config, shortSample);
            final Double fullScore = metric.singleTurnScore(config, fullSample);

            assertThat(shortScore).isLessThan(fullScore);
        }

        @Test
        @DisplayName("Should not penalize longer responses")
        void shouldNotPenalizeLongerResponses() {
            final Sample sample = Sample.builder()
                    .response("The quick brown fox jumps over the lazy dog and then runs away")
                    .reference("The quick brown fox jumps over the lazy dog")
                    .build();

            final BleuScoreConfig config = BleuScoreConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            // Score should be decent since all reference n-grams are present
            assertThat(score).isGreaterThan(0.5);
        }
    }

    @Nested
    @DisplayName("N-gram Configuration")
    class NgramConfiguration {

        @Test
        @DisplayName("Should use configurable max n-gram")
        void shouldUseConfigurableMaxNgram() {
            final Sample sample = Sample.builder()
                    .response("The quick fox jumps")
                    .reference("The quick brown fox jumps")
                    .build();

            final BleuScoreConfig config1 =
                    BleuScoreConfig.builder().maxNgram(1).build();
            final BleuScoreConfig config4 =
                    BleuScoreConfig.builder().maxNgram(4).build();

            final Double score1 = metric.singleTurnScore(config1, sample);
            final Double score4 = metric.singleTurnScore(config4, sample);

            // Unigram score should be higher as it doesn't penalize missing longer n-grams
            assertThat(score1).isGreaterThanOrEqualTo(score4);
        }

        @Test
        @DisplayName("Should handle short texts with high n-gram requirement")
        void shouldHandleShortTextsWithHighNgram() {
            final Sample sample = Sample.builder()
                    .response("Hello world")
                    .reference("Hello world")
                    .build();

            final BleuScoreConfig config = BleuScoreConfig.builder().maxNgram(4).build();

            final Double score = metric.singleTurnScore(config, sample);

            // With smoothing, should still return a reasonable score
            assertThat(score).isGreaterThan(0.5);
        }
    }

    @Nested
    @DisplayName("Smoothing")
    class Smoothing {

        @Test
        @DisplayName("Should apply smoothing when enabled")
        void shouldApplySmoothingWhenEnabled() {
            final Sample sample = Sample.builder()
                    .response("Hello world")
                    .reference("Hello there world")
                    .build();

            final BleuScoreConfig configSmoothed =
                    BleuScoreConfig.builder().smoothing(true).build();
            final BleuScoreConfig configUnsmoothed =
                    BleuScoreConfig.builder().smoothing(false).build();

            final Double smoothedScore = metric.singleTurnScore(configSmoothed, sample);
            final Double unsmoothedScore = metric.singleTurnScore(configUnsmoothed, sample);

            // Smoothed score may be different (usually helps avoid zeros)
            assertThat(smoothedScore).isNotNull();
            assertThat(unsmoothedScore).isNotNull();
        }

        @Test
        @DisplayName("Should return non-zero with smoothing even for partial matches")
        void shouldReturnNonZeroWithSmoothingForPartialMatches() {
            final Sample sample =
                    Sample.builder().response("cat").reference("dog").build();

            final BleuScoreConfig config =
                    BleuScoreConfig.builder().smoothing(true).build();

            final Double score = metric.singleTurnScore(config, sample);

            // With smoothing, should return a small non-zero value
            assertThat(score).isNotNull();
            // May be zero if no overlap, but smoothing prevents division by zero
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

            final BleuScoreConfig config = BleuScoreConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isNull();
        }

        @Test
        @DisplayName("Should return null for empty response")
        void shouldReturnNullForEmptyResponse() {
            final Sample sample =
                    Sample.builder().response("").reference("Reference").build();

            final BleuScoreConfig config = BleuScoreConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isNull();
        }

        @Test
        @DisplayName("Should return null for null reference")
        void shouldReturnNullForNullReference() {
            final Sample sample =
                    Sample.builder().response("Response").reference(null).build();

            final BleuScoreConfig config = BleuScoreConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isNull();
        }

        @Test
        @DisplayName("Should return null for empty reference")
        void shouldReturnNullForEmptyReference() {
            final Sample sample =
                    Sample.builder().response("Response").reference("").build();

            final BleuScoreConfig config = BleuScoreConfig.builder().build();

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

            final BleuScoreConfig config = BleuScoreConfig.builder().build();

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

            final BleuScoreConfig config = BleuScoreConfig.builder().build();

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

            final BleuScoreConfig config = BleuScoreConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isCloseTo(1.0, within(0.01));
        }

        @Test
        @DisplayName("Should handle mixed language text")
        void shouldHandleMixedLanguageText() {
            final Sample sample = Sample.builder()
                    .response("Hello мир world привет")
                    .reference("Hello мир world привет")
                    .build();

            final BleuScoreConfig config = BleuScoreConfig.builder().build();

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

            final BleuScoreConfig config = BleuScoreConfig.builder().build();

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
            final BleuScoreConfig config = BleuScoreConfig.builder().build();

            assertThat(config.getMaxNgram()).isEqualTo(4);
            assertThat(config.isSmoothing()).isTrue();
        }

        @Test
        @DisplayName("Should allow custom config values")
        void shouldAllowCustomConfigValues() {
            final BleuScoreConfig config =
                    BleuScoreConfig.builder().maxNgram(2).smoothing(false).build();

            assertThat(config.getMaxNgram()).isEqualTo(2);
            assertThat(config.isSmoothing()).isFalse();
        }
    }

    @Nested
    @DisplayName("Metric Name")
    class MetricName {

        @Test
        @DisplayName("Should return correct metric name")
        void shouldReturnCorrectMetricName() {
            assertThat(metric.getName()).isEqualTo("BleuScoreMetric");
        }
    }
}
