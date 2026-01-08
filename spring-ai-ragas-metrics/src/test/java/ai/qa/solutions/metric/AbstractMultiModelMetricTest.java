package ai.qa.solutions.metric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.qa.solutions.chatclient.ChatClientStore;
import ai.qa.solutions.execution.MultiModelExecutor;
import ai.qa.solutions.execution.ScoreAggregator;
import ai.qa.solutions.execution.listener.MetricExecutionListener;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationContext;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationResult;
import ai.qa.solutions.execution.listener.dto.ModelExclusionEvent;
import ai.qa.solutions.execution.listener.dto.StepContext;
import ai.qa.solutions.execution.listener.dto.StepResults;
import ai.qa.solutions.sample.Sample;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

@DisplayName("AbstractMultiModelMetric Tests")
class AbstractMultiModelMetricTest {

    private MultiModelExecutor executor;
    private TestableMetric metric;

    @BeforeEach
    void setUp() {
        ChatClient mockClient = mock(ChatClient.class);
        ChatClientStore store = new ChatClientStore(Map.of("model-1", mockClient), mockClient);
        executor = new MultiModelExecutor(store, null, new SimpleAsyncTaskExecutor());
        metric = new TestableMetric(executor);
    }

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("Should require non-null executor")
        void shouldRequireNonNullExecutor() {
            assertThatThrownBy(() -> new TestableMetric(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("executor");
        }

        @Test
        @DisplayName("Should use AVERAGE aggregator by default")
        void shouldUseAverageAggregatorByDefault() {
            TestableMetric metric = new TestableMetric(executor);
            assertThat(metric.getDefaultAggregator()).isEqualTo(ScoreAggregator.AVERAGE);
        }

        @Test
        @DisplayName("Should allow custom aggregator")
        void shouldAllowCustomAggregator() {
            TestableMetric metric = new TestableMetric(executor, ScoreAggregator.MEDIAN);
            assertThat(metric.getDefaultAggregator()).isEqualTo(ScoreAggregator.MEDIAN);
        }

        @Test
        @DisplayName("Should require non-null aggregator")
        void shouldRequireNonNullAggregator() {
            assertThatThrownBy(() -> new TestableMetric(executor, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("defaultAggregator");
        }
    }

    @Nested
    @DisplayName("Listener Management")
    class ListenerManagementTests {

        @Test
        @DisplayName("Should start with empty listener list")
        void shouldStartWithEmptyListenerList() {
            assertThat(metric.getListeners()).isEmpty();
        }

        @Test
        @DisplayName("Should add listener")
        void shouldAddListener() {
            MetricExecutionListener listener = mock(MetricExecutionListener.class);

            metric.addListener(listener);

            assertThat(metric.getListeners()).containsExactly(listener);
        }

        @Test
        @DisplayName("Should reject null listener")
        void shouldRejectNullListener() {
            assertThatThrownBy(() -> metric.addListener(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("listener");
        }

        @Test
        @DisplayName("Should sort listeners by order")
        void shouldSortListenersByOrder() {
            MetricExecutionListener first = createListenerWithOrder(-100);
            MetricExecutionListener second = createListenerWithOrder(0);
            MetricExecutionListener third = createListenerWithOrder(100);

            // Add in random order
            metric.addListener(third);
            metric.addListener(first);
            metric.addListener(second);

            List<MetricExecutionListener> listeners = metric.getListeners();
            assertThat(listeners.get(0).getOrder()).isEqualTo(-100);
            assertThat(listeners.get(1).getOrder()).isEqualTo(0);
            assertThat(listeners.get(2).getOrder()).isEqualTo(100);
        }

        @Test
        @DisplayName("Should remove listener")
        void shouldRemoveListener() {
            MetricExecutionListener listener = mock(MetricExecutionListener.class);
            metric.addListener(listener);

            metric.removeListener(listener);

            assertThat(metric.getListeners()).isEmpty();
        }

        @Test
        @DisplayName("Should return defensive copy of listeners")
        void shouldReturnDefensiveCopy() {
            MetricExecutionListener listener = mock(MetricExecutionListener.class);
            metric.addListener(listener);

            List<MetricExecutionListener> listeners = metric.getListeners();

            assertThatThrownBy(() -> listeners.add(mock(MetricExecutionListener.class)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("withListeners should add multiple listeners")
        void withListenersShouldAddMultiple() {
            MetricExecutionListener listener1 = mock(MetricExecutionListener.class);
            MetricExecutionListener listener2 = mock(MetricExecutionListener.class);

            metric.withListeners(List.of(listener1, listener2));

            assertThat(metric.getListeners()).containsExactlyInAnyOrder(listener1, listener2);
        }

        @Test
        @DisplayName("withListeners should handle null collection")
        void withListenersShouldHandleNull() {
            metric.withListeners(null);
            assertThat(metric.getListeners()).isEmpty();
        }

        @Test
        @DisplayName("withListeners should filter null elements")
        void withListenersShouldFilterNulls() {
            MetricExecutionListener listener = mock(MetricExecutionListener.class);
            List<MetricExecutionListener> listWithNulls = new ArrayList<>();
            listWithNulls.add(listener);
            listWithNulls.add(null);

            metric.withListeners(listWithNulls);

            assertThat(metric.getListeners()).containsExactly(listener);
        }

        @Test
        @DisplayName("withListeners should return same instance for chaining")
        void withListenersShouldReturnSameInstance() {
            TestableMetric result = metric.withListeners(List.of());
            assertThat(result).isSameAs(metric);
        }

        private MetricExecutionListener createListenerWithOrder(int order) {
            MetricExecutionListener listener = mock(MetricExecutionListener.class);
            when(listener.getOrder()).thenReturn(order);
            when(listener.forEvaluation()).thenReturn(listener);
            return listener;
        }
    }

    @Nested
    @DisplayName("EvaluationNotifier")
    class EvaluationNotifierTests {

        @Test
        @DisplayName("Should create notifier with evaluation-specific listeners")
        void shouldCreateNotifierWithEvaluationListeners() {
            MetricExecutionListener listener = mock(MetricExecutionListener.class);
            MetricExecutionListener evaluationListener = mock(MetricExecutionListener.class);
            when(listener.forEvaluation()).thenReturn(evaluationListener);
            metric.addListener(listener);

            AbstractMultiModelMetric<?>.EvaluationNotifier notifier = metric.createEvaluationNotifier();

            assertThat(notifier).isNotNull();
        }

        @Test
        @DisplayName("Notifier should call beforeMetricEvaluation on all listeners")
        void notifierShouldCallBeforeMetricEvaluation() {
            RecordingListener listener = new RecordingListener();
            metric.addListener(listener);

            AbstractMultiModelMetric<?>.EvaluationNotifier notifier = metric.createEvaluationNotifier();
            MetricEvaluationContext context = MetricEvaluationContext.builder()
                    .metricName("Test")
                    .modelIds(List.of("model-1"))
                    .totalSteps(1)
                    .build();
            notifier.beforeMetricEvaluation(context);

            assertThat(listener.beforeMetricCalled).isTrue();
        }

        @Test
        @DisplayName("Notifier should call beforeStep")
        void notifierShouldCallBeforeStep() {
            RecordingListener listener = new RecordingListener();
            metric.addListener(listener);

            AbstractMultiModelMetric<?>.EvaluationNotifier notifier = metric.createEvaluationNotifier();
            notifier.beforeStep("TestStep", 0, 1);

            assertThat(listener.beforeStepCalled).isTrue();
        }

        @Test
        @DisplayName("Notifier should call afterStep")
        void notifierShouldCallAfterStep() {
            RecordingListener listener = new RecordingListener();
            metric.addListener(listener);

            AbstractMultiModelMetric<?>.EvaluationNotifier notifier = metric.createEvaluationNotifier();
            notifier.afterLlmStep("TestStep", 0, 1, "prompt", List.of());

            assertThat(listener.afterStepCalled).isTrue();
        }

        @Test
        @DisplayName("Notifier should call onModelExcluded")
        void notifierShouldCallOnModelExcluded() {
            RecordingListener listener = new RecordingListener();
            metric.addListener(listener);

            AbstractMultiModelMetric<?>.EvaluationNotifier notifier = metric.createEvaluationNotifier();
            notifier.onModelExcluded(ModelExclusionEvent.builder()
                    .modelId("model-1")
                    .failedStepName("Step")
                    .failedStepIndex(0)
                    .cause(new RuntimeException("error"))
                    .build());

            assertThat(listener.onModelExcludedCalled).isTrue();
        }

        @Test
        @DisplayName("Notifier should call afterMetricEvaluation")
        void notifierShouldCallAfterMetricEvaluation() {
            RecordingListener listener = new RecordingListener();
            metric.addListener(listener);

            AbstractMultiModelMetric<?>.EvaluationNotifier notifier = metric.createEvaluationNotifier();
            notifier.afterMetricEvaluation(MetricEvaluationResult.builder()
                    .metricName("Test")
                    .aggregatedScore(0.8)
                    .modelScores(Map.of())
                    .excludedModels(List.of())
                    .totalDuration(Duration.ZERO)
                    .build());

            assertThat(listener.afterMetricCalled).isTrue();
        }

        @Test
        @DisplayName("Notifier should handle listener exceptions gracefully")
        void notifierShouldHandleListenerExceptions() {
            MetricExecutionListener throwingListener = new MetricExecutionListener() {
                @Override
                public void beforeMetricEvaluation(MetricEvaluationContext context) {
                    throw new RuntimeException("Test exception");
                }
            };
            RecordingListener normalListener = new RecordingListener();
            metric.addListener(throwingListener);
            metric.addListener(normalListener);

            AbstractMultiModelMetric<?>.EvaluationNotifier notifier = metric.createEvaluationNotifier();
            MetricEvaluationContext context = MetricEvaluationContext.builder()
                    .metricName("Test")
                    .modelIds(List.of())
                    .totalSteps(1)
                    .build();

            // Should not throw
            notifier.beforeMetricEvaluation(context);

            // Second listener should still be called
            assertThat(normalListener.beforeMetricCalled).isTrue();
        }
    }

    @Nested
    @DisplayName("Aggregation")
    class AggregationTests {

        @Test
        @DisplayName("Should aggregate scores with default aggregator")
        void shouldAggregateWithDefaultAggregator() {
            Map<String, Double> scores = Map.of("model-1", 0.6, "model-2", 0.8);

            double result = metric.testAggregate(scores);

            assertThat(result).isEqualTo(0.7); // AVERAGE of 0.6 and 0.8
        }

        @Test
        @DisplayName("Should aggregate scores with custom aggregator")
        void shouldAggregateWithCustomAggregator() {
            Map<String, Double> scores = Map.of("model-1", 0.6, "model-2", 0.8, "model-3", 0.9);

            double result = metric.testAggregate(scores, ScoreAggregator.MEDIAN);

            assertThat(result).isEqualTo(0.8); // MEDIAN of 0.6, 0.8, 0.9
        }

        @Test
        @DisplayName("Should throw when scores map is empty")
        void shouldThrowWhenScoresEmpty() {
            assertThatThrownBy(() -> metric.testAggregate(Map.of()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No successful model scores to aggregate");
        }

        @Test
        @DisplayName("Should handle single score")
        void shouldHandleSingleScore() {
            Map<String, Double> scores = Map.of("model-1", 0.75);

            double result = metric.testAggregate(scores);

            assertThat(result).isEqualTo(0.75);
        }
    }

    // Test implementations

    static class TestConfig implements Metric.MetricConfiguration {}

    static class TestableMetric extends AbstractMultiModelMetric<TestConfig> {
        TestableMetric(MultiModelExecutor executor) {
            super(executor);
        }

        TestableMetric(MultiModelExecutor executor, ScoreAggregator aggregator) {
            super(executor, aggregator);
        }

        @Override
        public Double singleTurnScore(TestConfig config, Sample sample) {
            return 0.0;
        }

        @Override
        public CompletableFuture<Double> singleTurnScoreAsync(TestConfig config, Sample sample) {
            return CompletableFuture.completedFuture(0.0);
        }

        ScoreAggregator getDefaultAggregator() {
            return defaultAggregator;
        }

        double testAggregate(Map<String, Double> scores) {
            return aggregate(scores);
        }

        double testAggregate(Map<String, Double> scores, ScoreAggregator aggregator) {
            return aggregate(scores, aggregator);
        }
    }

    static class RecordingListener implements MetricExecutionListener {
        boolean beforeMetricCalled = false;
        boolean beforeStepCalled = false;
        boolean afterStepCalled = false;
        boolean onModelExcludedCalled = false;
        boolean afterMetricCalled = false;

        @Override
        public void beforeMetricEvaluation(MetricEvaluationContext context) {
            beforeMetricCalled = true;
        }

        @Override
        public void beforeStep(StepContext context) {
            beforeStepCalled = true;
        }

        @Override
        public void afterStep(StepResults results) {
            afterStepCalled = true;
        }

        @Override
        public void onModelExcluded(ModelExclusionEvent event) {
            onModelExcludedCalled = true;
        }

        @Override
        public void afterMetricEvaluation(MetricEvaluationResult result) {
            afterMetricCalled = true;
        }

        @Override
        public MetricExecutionListener forEvaluation() {
            return this;
        }
    }
}
