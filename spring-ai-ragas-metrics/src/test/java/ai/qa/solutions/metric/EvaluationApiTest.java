package ai.qa.solutions.metric;

import static org.assertj.core.api.Assertions.assertThat;

import ai.qa.solutions.execution.listener.MetricExecutionListener;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationContext;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationResult;
import ai.qa.solutions.sample.Sample;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Evaluate API Tests")
class EvaluationApiTest {

    private static final double EXPECTED_SCORE = 0.85;
    private static final String TEST_MODEL_ID = "test-model";
    private static final String TEST_METRIC_NAME = "NotifyingTestMetric";

    private NotifyingTestMetric metric;
    private TestConfig config;
    private Sample sample;

    @BeforeEach
    void setUp() {
        metric = new NotifyingTestMetric();
        config = new TestConfig();
        sample = Sample.builder()
                .userInput("What is Spring?")
                .response("Spring is a Java framework")
                .reference("Spring is a popular Java framework")
                .build();
    }

    @Nested
    @DisplayName("singleTurnEvaluate")
    class SingleTurnEvaluate {

        @Test
        @DisplayName("Should return result with correct score")
        void shouldReturnResultWithCorrectScore() {
            final EvaluationResult result = metric.singleTurnEvaluate(config, sample);

            assertThat(result.getScore()).isEqualTo(EXPECTED_SCORE);
        }

        @Test
        @DisplayName("Should return result with metricName")
        void shouldReturnResultWithMetricName() {
            final EvaluationResult result = metric.singleTurnEvaluate(config, sample);

            assertThat(result.getMetricName()).isEqualTo(TEST_METRIC_NAME);
        }

        @Test
        @DisplayName("Should return result with sample preserved")
        void shouldReturnResultWithSample() {
            final EvaluationResult result = metric.singleTurnEvaluate(config, sample);

            assertThat(result.getSample()).isSameAs(sample);
        }

        @Test
        @DisplayName("Should return result with modelScores")
        void shouldReturnResultWithModelScores() {
            final EvaluationResult result = metric.singleTurnEvaluate(config, sample);

            assertThat(result.getModelScores()).isNotNull();
            assertThat(result.getModelScores()).containsEntry(TEST_MODEL_ID, EXPECTED_SCORE);
        }

        @Test
        @DisplayName("Should return result with duration")
        void shouldReturnResultWithDuration() {
            final EvaluationResult result = metric.singleTurnEvaluate(config, sample);

            assertThat(result.getTotalDuration()).isNotNull();
            assertThat(result.getTotalDuration()).isEqualTo(Duration.ofMillis(50));
        }

        @Test
        @DisplayName("Should return result with modelIds")
        void shouldReturnResultWithModelIds() {
            final EvaluationResult result = metric.singleTurnEvaluate(config, sample);

            assertThat(result.getModelIds()).containsExactly(TEST_MODEL_ID);
        }

        @Test
        @DisplayName("Should return result with config")
        void shouldReturnResultWithConfig() {
            final EvaluationResult result = metric.singleTurnEvaluate(config, sample);

            assertThat(result.getConfig()).isSameAs(config);
        }
    }

    @Nested
    @DisplayName("singleTurnEvaluateAsync")
    class SingleTurnEvaluateAsync {

        @Test
        @DisplayName("Should return same result as synchronous version")
        void shouldReturnSameResultAsSync() throws ExecutionException, InterruptedException {
            final CompletableFuture<EvaluationResult> future = metric.singleTurnEvaluateAsync(config, sample);
            final EvaluationResult result = future.get();

            assertThat(result.getScore()).isEqualTo(EXPECTED_SCORE);
            assertThat(result.getMetricName()).isEqualTo(TEST_METRIC_NAME);
            assertThat(result.getSample()).isSameAs(sample);
            assertThat(result.getModelScores()).containsEntry(TEST_MODEL_ID, EXPECTED_SCORE);
        }
    }

    @Nested
    @DisplayName("Listener Interaction")
    class ListenerInteraction {

        @Test
        @DisplayName("Should work alongside existing listeners")
        void shouldWorkAlongsideExistingListeners() {
            final RecordingListener listener = new RecordingListener(0, "external");
            metric.addListener(listener);

            final EvaluationResult result = metric.singleTurnEvaluate(config, sample);

            // Verify the result is returned correctly
            assertThat(result.getScore()).isEqualTo(EXPECTED_SCORE);
            assertThat(result.getMetricName()).isEqualTo(TEST_METRIC_NAME);

            // Verify the external listener was also called
            assertThat(listener.beforeCalled).isTrue();
            assertThat(listener.afterCalled).isTrue();
            assertThat(listener.receivedResult).isNotNull();
            assertThat(listener.receivedResult.getAggregatedScore()).isEqualTo(EXPECTED_SCORE);
        }

        @Test
        @DisplayName("Should remove capturing listener after evaluation")
        void shouldRemoveCapturingListenerAfterEvaluation() {
            final int listenerCountBefore = metric.getListeners().size();

            metric.singleTurnEvaluate(config, sample);

            // The capturing listener should be removed after evaluation
            assertThat(metric.getListeners()).hasSize(listenerCountBefore);
        }
    }

    @Nested
    @DisplayName("Null Result Fallback")
    class NullResultFallback {

        @Test
        @DisplayName("Should return basic result when metric does not fire notifier")
        void shouldReturnBasicResultWhenNotifierNotFired() {
            final SilentTestMetric silentMetric = new SilentTestMetric();

            final EvaluationResult result = silentMetric.singleTurnEvaluate(config, sample);

            // When notifier is not fired, captured result is null -> fallback to basic result
            assertThat(result).isNotNull();
            assertThat(result.getMetricName()).isEqualTo("SilentTestMetric");
        }
    }

    @Nested
    @DisplayName("MetricConfiguration Default Language")
    class MetricConfigurationDefaultLanguage {

        @Test
        @DisplayName("Should default language to en")
        void shouldDefaultLanguageToEn() {
            final TestConfig testConfig = new TestConfig();

            assertThat(testConfig.getLanguage()).isEqualTo("en");
        }
    }

    // ==================== Test implementations ====================

    static class TestConfig implements Metric.MetricConfiguration {}

    /**
     * A test metric that properly fires the notifier pattern, simulating
     * how real metrics (both LLM and NLP) interact with the listener system.
     */
    static class NotifyingTestMetric extends AbstractMetric<TestConfig> {

        @Override
        public Double singleTurnScore(final TestConfig config, final Sample sample) {
            final EvaluationNotifier notifier = createEvaluationNotifier();
            notifier.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName(getName())
                    .sample(sample)
                    .config(config)
                    .modelIds(List.of(TEST_MODEL_ID))
                    .totalSteps(1)
                    .build());

            final double score = EXPECTED_SCORE;

            notifier.afterMetricEvaluation(MetricEvaluationResult.builder()
                    .metricName(getName())
                    .sample(sample)
                    .config(config)
                    .aggregatedScore(score)
                    .modelScores(Map.of(TEST_MODEL_ID, score))
                    .excludedModels(List.of())
                    .modelIds(List.of(TEST_MODEL_ID))
                    .totalDuration(Duration.ofMillis(50))
                    .build());

            return score;
        }

        @Override
        public CompletableFuture<Double> singleTurnScoreAsync(final TestConfig config, final Sample sample) {
            return CompletableFuture.completedFuture(singleTurnScore(config, sample));
        }
    }

    /**
     * A test metric that does NOT fire the notifier, simulating
     * a metric that only returns a score without listener callbacks.
     */
    static class SilentTestMetric extends AbstractMetric<TestConfig> {

        @Override
        public Double singleTurnScore(final TestConfig config, final Sample sample) {
            return 0.5;
        }

        @Override
        public CompletableFuture<Double> singleTurnScoreAsync(final TestConfig config, final Sample sample) {
            return CompletableFuture.completedFuture(0.5);
        }
    }

    static class RecordingListener implements MetricExecutionListener {

        final int order;
        final String name;
        boolean beforeCalled = false;
        boolean afterCalled = false;
        MetricEvaluationContext receivedContext;
        MetricEvaluationResult receivedResult;

        RecordingListener(final int order, final String name) {
            this.order = order;
            this.name = name;
        }

        @Override
        public void beforeMetricEvaluation(final MetricEvaluationContext context) {
            beforeCalled = true;
            receivedContext = context;
        }

        @Override
        public void afterMetricEvaluation(final MetricEvaluationResult result) {
            afterCalled = true;
            receivedResult = result;
        }

        @Override
        public int getOrder() {
            return order;
        }

        @Override
        public MetricExecutionListener forEvaluation() {
            return this;
        }
    }
}
