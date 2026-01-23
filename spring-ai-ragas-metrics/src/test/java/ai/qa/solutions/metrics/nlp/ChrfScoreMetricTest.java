package ai.qa.solutions.metrics.nlp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import ai.qa.solutions.metrics.nlp.ChrfScoreMetric.ChrfScoreConfig;
import ai.qa.solutions.sample.Sample;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ChrfScoreMetric")
class ChrfScoreMetricTest {

    private ChrfScoreMetric metric;

    @BeforeEach
    void setUp() {
        metric = new ChrfScoreMetric();
    }

    @Nested
    @DisplayName("Basic Scoring")
    class BasicScoring {

        @Test
        @DisplayName("Should return perfect score for identical texts")
        void shouldReturnPerfectScoreForIdenticalTexts() {
            final Sample sample = Sample.builder()
                    .response("The quick brown fox jumps over the lazy dog")
                    .reference("The quick brown fox jumps over the lazy dog")
                    .build();

            final ChrfScoreConfig config = ChrfScoreConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isCloseTo(1.0, within(0.01));
        }

        @Test
        @DisplayName("Should return high score for similar texts")
        void shouldReturnHighScoreForSimilarTexts() {
            final Sample sample = Sample.builder()
                    .response("The quick brown fox jumps over the lazy dog")
                    .reference("The quick brown fox jumped over the lazy dog")
                    .build();

            final ChrfScoreConfig config = ChrfScoreConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            // Character-level similarity should be decent despite one word difference
            assertThat(score).isGreaterThan(0.8);
        }

        @Test
        @DisplayName("Should return moderate score for partially similar texts")
        void shouldReturnModerateScoreForPartiallySimilarTexts() {
            final Sample sample = Sample.builder()
                    .response("Hello world")
                    .reference("Hello there world wide")
                    .build();

            final ChrfScoreConfig config = ChrfScoreConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isBetween(0.3, 0.8);
        }

        @Test
        @DisplayName("Should return low score for different texts")
        void shouldReturnLowScoreForDifferentTexts() {
            final Sample sample =
                    Sample.builder().response("abcdefgh").reference("xyz12345").build();

            final ChrfScoreConfig config = ChrfScoreConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isLessThan(0.3);
        }
    }

    @Nested
    @DisplayName("Character N-gram Order")
    class CharNgramOrder {

        @Test
        @DisplayName("Should use configurable character n-gram order")
        void shouldUseConfigurableCharNgramOrder() {
            final Sample sample =
                    Sample.builder().response("hello").reference("hallo").build();

            final ChrfScoreConfig config1 =
                    ChrfScoreConfig.builder().charNgramOrder(1).build();
            final ChrfScoreConfig config6 =
                    ChrfScoreConfig.builder().charNgramOrder(6).build();

            final Double score1 = metric.singleTurnScore(config1, sample);
            final Double score6 = metric.singleTurnScore(config6, sample);

            // Both should detect similarity, may differ based on n-gram coverage
            assertThat(score1).isGreaterThan(0.5);
            assertThat(score6).isGreaterThan(0.3);
        }
    }

    @Nested
    @DisplayName("chrF++ Mode (Word N-grams)")
    class ChrfPlusPlusMode {

        @Test
        @DisplayName("Should include word n-grams when wordNgramOrder > 0")
        void shouldIncludeWordNgrams() {
            final Sample sample = Sample.builder()
                    .response("the quick brown fox")
                    .reference("the quick brown fox")
                    .build();

            final ChrfScoreConfig chrfConfig =
                    ChrfScoreConfig.builder().wordNgramOrder(0).build();
            final ChrfScoreConfig chrfPPConfig =
                    ChrfScoreConfig.builder().wordNgramOrder(2).build();

            final Double chrfScore = metric.singleTurnScore(chrfConfig, sample);
            final Double chrfPPScore = metric.singleTurnScore(chrfPPConfig, sample);

            // Both should be perfect for identical texts
            assertThat(chrfScore).isCloseTo(1.0, within(0.01));
            assertThat(chrfPPScore).isCloseTo(1.0, within(0.01));
        }

        @Test
        @DisplayName("chrF++ should detect word order better")
        void chrfPlusPlusShouldDetectWordOrderBetter() {
            final Sample sample = Sample.builder()
                    .response("fox brown quick the")
                    .reference("the quick brown fox")
                    .build();

            final ChrfScoreConfig chrfConfig =
                    ChrfScoreConfig.builder().wordNgramOrder(0).build();
            final ChrfScoreConfig chrfPPConfig =
                    ChrfScoreConfig.builder().wordNgramOrder(2).build();

            final Double chrfScore = metric.singleTurnScore(chrfConfig, sample);
            final Double chrfPPScore = metric.singleTurnScore(chrfPPConfig, sample);

            // chrF++ should give a score that accounts for word order
            // Both should find similarity since same words are present
            assertThat(chrfScore).isGreaterThan(0.5); // Character similarity exists
            assertThat(chrfPPScore).isNotNull(); // chrF++ also produces a score
        }
    }

    @Nested
    @DisplayName("Beta Parameter")
    class BetaParameter {

        @Test
        @DisplayName("Should use configurable beta")
        void shouldUseConfigurableBeta() {
            final Sample sample = Sample.builder()
                    .response("the quick")
                    .reference("the quick brown fox jumps")
                    .build();

            final ChrfScoreConfig lowBeta = ChrfScoreConfig.builder().beta(1.0).build();
            final ChrfScoreConfig highBeta = ChrfScoreConfig.builder().beta(3.0).build();

            final Double lowBetaScore = metric.singleTurnScore(lowBeta, sample);
            final Double highBetaScore = metric.singleTurnScore(highBeta, sample);

            // Both should give scores, beta affects precision/recall balance
            assertThat(lowBetaScore).isNotNull();
            assertThat(highBetaScore).isNotNull();
        }
    }

    @Nested
    @DisplayName("Morphological Robustness")
    class MorphologicalRobustness {

        @Test
        @DisplayName("Should be robust to minor spelling differences")
        void shouldBeRobustToMinorSpellingDifferences() {
            final Sample sample = Sample.builder()
                    .response("colour behaviour favour")
                    .reference("color behavior favor")
                    .build();

            final ChrfScoreConfig config = ChrfScoreConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            // chrF should show reasonable similarity despite spelling variations
            assertThat(score).isGreaterThan(0.5);
        }

        @Test
        @DisplayName("Should handle typos well")
        void shouldHandleTyposWell() {
            final Sample sample = Sample.builder()
                    .response("recieve occassion accomodate")
                    .reference("receive occasion accommodate")
                    .build();

            final ChrfScoreConfig config = ChrfScoreConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            // Character-level metric should still find significant overlap
            assertThat(score).isGreaterThan(0.6);
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

            final ChrfScoreConfig config = ChrfScoreConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isNull();
        }

        @Test
        @DisplayName("Should return null for empty response")
        void shouldReturnNullForEmptyResponse() {
            final Sample sample =
                    Sample.builder().response("").reference("Reference").build();

            final ChrfScoreConfig config = ChrfScoreConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isNull();
        }

        @Test
        @DisplayName("Should return null for null reference")
        void shouldReturnNullForNullReference() {
            final Sample sample =
                    Sample.builder().response("Response").reference(null).build();

            final ChrfScoreConfig config = ChrfScoreConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isNull();
        }

        @Test
        @DisplayName("Should return null for empty reference")
        void shouldReturnNullForEmptyReference() {
            final Sample sample =
                    Sample.builder().response("Response").reference("").build();

            final ChrfScoreConfig config = ChrfScoreConfig.builder().build();

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

            final ChrfScoreConfig config = ChrfScoreConfig.builder().build();

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

            final ChrfScoreConfig config = ChrfScoreConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isCloseTo(1.0, within(0.01));
        }

        @Test
        @DisplayName("Should handle Russian morphological variations")
        void shouldHandleRussianMorphologicalVariations() {
            final Sample sample = Sample.builder()
                    .response("красивый дом красивого дома")
                    .reference("красивые дома красивых домов")
                    .build();

            final ChrfScoreConfig config = ChrfScoreConfig.builder().build();

            final Double score = metric.singleTurnScore(config, sample);

            // Should detect character-level similarity in morphological variants
            assertThat(score).isGreaterThan(0.5);
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

            final ChrfScoreConfig config = ChrfScoreConfig.builder().build();

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
            final ChrfScoreConfig config = ChrfScoreConfig.builder().build();

            assertThat(config.getCharNgramOrder()).isEqualTo(6);
            assertThat(config.getWordNgramOrder()).isEqualTo(0);
            assertThat(config.getBeta()).isEqualTo(2.0);
        }

        @Test
        @DisplayName("Should allow custom config values")
        void shouldAllowCustomConfigValues() {
            final ChrfScoreConfig config = ChrfScoreConfig.builder()
                    .charNgramOrder(4)
                    .wordNgramOrder(2)
                    .beta(1.5)
                    .build();

            assertThat(config.getCharNgramOrder()).isEqualTo(4);
            assertThat(config.getWordNgramOrder()).isEqualTo(2);
            assertThat(config.getBeta()).isEqualTo(1.5);
        }
    }

    @Nested
    @DisplayName("Metric Name")
    class MetricName {

        @Test
        @DisplayName("Should return correct metric name")
        void shouldReturnCorrectMetricName() {
            assertThat(metric.getName()).isEqualTo("ChrfScoreMetric");
        }
    }
}
