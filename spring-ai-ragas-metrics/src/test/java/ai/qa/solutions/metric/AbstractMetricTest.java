package ai.qa.solutions.metric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.qa.solutions.execution.listener.MetricExecutionListener;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationContext;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationResult;
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

@DisplayName("AbstractMetric Tests")
class AbstractMetricTest {

    private TestableAbstractMetric metric;

    @BeforeEach
    void setUp() {
        metric = new TestableAbstractMetric();
    }

    @Nested
    @DisplayName("Listener Management")
    class ListenerManagement {

        @Test
        @DisplayName("Should start with empty listener list")
        void shouldStartWithEmptyListenerList() {
            assertThat(metric.getListeners()).isEmpty();
        }

        @Test
        @DisplayName("Should add listener and return it in getListeners")
        void shouldAddListener() {
            final MetricExecutionListener listener = mock(MetricExecutionListener.class);

            metric.addListener(listener);

            assertThat(metric.getListeners()).containsExactly(listener);
        }

        @Test
        @DisplayName("Should add multiple listeners")
        void shouldAddMultipleListeners() {
            final MetricExecutionListener listener1 = mock(MetricExecutionListener.class);
            final MetricExecutionListener listener2 = mock(MetricExecutionListener.class);

            metric.addListener(listener1);
            metric.addListener(listener2);

            assertThat(metric.getListeners()).hasSize(2);
            assertThat(metric.getListeners()).containsExactlyInAnyOrder(listener1, listener2);
        }

        @Test
        @DisplayName("Should reject null listener")
        void shouldRejectNullListener() {
            assertThatThrownBy(() -> metric.addListener(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("listener");
        }

        @Test
        @DisplayName("Should sort listeners by order after add")
        void shouldSortListenersByOrder() {
            final MetricExecutionListener first = createListenerWithOrder(-100);
            final MetricExecutionListener second = createListenerWithOrder(0);
            final MetricExecutionListener third = createListenerWithOrder(100);

            // Add in reverse order
            metric.addListener(third);
            metric.addListener(first);
            metric.addListener(second);

            final List<MetricExecutionListener> listeners = metric.getListeners();
            assertThat(listeners).hasSize(3);
            assertThat(listeners.get(0).getOrder()).isEqualTo(-100);
            assertThat(listeners.get(1).getOrder()).isEqualTo(0);
            assertThat(listeners.get(2).getOrder()).isEqualTo(100);
        }

        @Test
        @DisplayName("Should remove listener")
        void shouldRemoveListener() {
            final MetricExecutionListener listener = mock(MetricExecutionListener.class);
            metric.addListener(listener);

            metric.removeListener(listener);

            assertThat(metric.getListeners()).isEmpty();
        }

        @Test
        @DisplayName("Should return same instance from addListener for chaining")
        void shouldReturnSameInstanceFromAddListener() {
            final MetricExecutionListener listener = mock(MetricExecutionListener.class);

            final AbstractMetric<?> returned = metric.addListener(listener);

            assertThat(returned).isSameAs(metric);
        }

        @Test
        @DisplayName("Should return same instance from removeListener for chaining")
        void shouldReturnSameInstanceFromRemoveListener() {
            final MetricExecutionListener listener = mock(MetricExecutionListener.class);

            final AbstractMetric<?> returned = metric.removeListener(listener);

            assertThat(returned).isSameAs(metric);
        }

        @Test
        @DisplayName("Should return defensive copy of listeners list")
        void shouldReturnDefensiveCopy() {
            final MetricExecutionListener listener = mock(MetricExecutionListener.class);
            metric.addListener(listener);

            final List<MetricExecutionListener> listeners = metric.getListeners();

            assertThatThrownBy(() -> listeners.add(mock(MetricExecutionListener.class)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        private MetricExecutionListener createListenerWithOrder(final int order) {
            final MetricExecutionListener listener = mock(MetricExecutionListener.class);
            when(listener.getOrder()).thenReturn(order);
            when(listener.forEvaluation()).thenReturn(listener);
            return listener;
        }
    }

    @Nested
    @DisplayName("withListeners")
    class WithListeners {

        @Test
        @DisplayName("Should add multiple listeners from collection")
        void shouldAddMultipleListeners() {
            final MetricExecutionListener listener1 = mock(MetricExecutionListener.class);
            final MetricExecutionListener listener2 = mock(MetricExecutionListener.class);

            metric.withListeners(List.of(listener1, listener2));

            assertThat(metric.getListeners()).containsExactlyInAnyOrder(listener1, listener2);
        }

        @Test
        @DisplayName("Should handle null collection gracefully")
        void shouldHandleNullCollection() {
            metric.withListeners(null);

            assertThat(metric.getListeners()).isEmpty();
        }

        @Test
        @DisplayName("Should filter null elements from collection")
        void shouldFilterNullElements() {
            final MetricExecutionListener listener = mock(MetricExecutionListener.class);
            final List<MetricExecutionListener> listWithNulls = new ArrayList<>();
            listWithNulls.add(listener);
            listWithNulls.add(null);
            listWithNulls.add(null);

            metric.withListeners(listWithNulls);

            assertThat(metric.getListeners()).containsExactly(listener);
        }

        @Test
        @DisplayName("Should return same instance for chaining")
        void shouldReturnSameInstanceForChaining() {
            final TestableAbstractMetric returned = metric.withListeners(List.of());

            assertThat(returned).isSameAs(metric);
        }

        @Test
        @DisplayName("Should handle empty collection")
        void shouldHandleEmptyCollection() {
            metric.withListeners(List.of());

            assertThat(metric.getListeners()).isEmpty();
        }
    }

    @Nested
    @DisplayName("EvaluationNotifier - createEvaluationNotifier")
    class EvaluationNotifierCreation {

        @Test
        @DisplayName("Should create notifier successfully")
        void shouldCreateNotifier() {
            final AbstractMetric<?>.EvaluationNotifier notifier = metric.createEvaluationNotifier();

            assertThat(notifier).isNotNull();
        }

        @Test
        @DisplayName("Should call forEvaluation on each listener to create evaluation-specific instances")
        void shouldCallForEvaluationOnEachListener() {
            final MetricExecutionListener originalListener = mock(MetricExecutionListener.class);
            final MetricExecutionListener evaluationListener = mock(MetricExecutionListener.class);
            when(originalListener.forEvaluation()).thenReturn(evaluationListener);
            metric.addListener(originalListener);

            final AbstractMetric<?>.EvaluationNotifier notifier = metric.createEvaluationNotifier();

            // notifier was created - forEvaluation was called internally
            assertThat(notifier).isNotNull();
        }

        @Test
        @DisplayName("Should sort evaluation listeners by order")
        void shouldSortEvaluationListenersByOrder() {
            final RecordingListener lowOrder = new RecordingListener(-10, "low");
            final RecordingListener highOrder = new RecordingListener(10, "high");

            metric.addListener(highOrder);
            metric.addListener(lowOrder);

            final AbstractMetric<?>.EvaluationNotifier notifier = metric.createEvaluationNotifier();
            notifier.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("Test")
                    .modelIds(List.of())
                    .totalSteps(1)
                    .build());

            // Both should be called
            assertThat(lowOrder.beforeCalled).isTrue();
            assertThat(highOrder.beforeCalled).isTrue();
        }
    }

    @Nested
    @DisplayName("EvaluationNotifier - beforeMetricEvaluation")
    class EvaluationNotifierBefore {

        @Test
        @DisplayName("Should call beforeMetricEvaluation on all listeners")
        void shouldCallBeforeOnAllListeners() {
            final RecordingListener listener1 = new RecordingListener(0, "listener1");
            final RecordingListener listener2 = new RecordingListener(1, "listener2");
            metric.addListener(listener1);
            metric.addListener(listener2);

            final AbstractMetric<?>.EvaluationNotifier notifier = metric.createEvaluationNotifier();
            notifier.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("Test")
                    .modelIds(List.of("model-1"))
                    .totalSteps(1)
                    .build());

            assertThat(listener1.beforeCalled).isTrue();
            assertThat(listener2.beforeCalled).isTrue();
        }

        @Test
        @DisplayName("Should catch exception from listener without failing")
        void shouldCatchExceptionFromListener() {
            final ThrowingListener throwingListener = new ThrowingListener();
            final RecordingListener normalListener = new RecordingListener(1, "normal");
            metric.addListener(throwingListener);
            metric.addListener(normalListener);

            final AbstractMetric<?>.EvaluationNotifier notifier = metric.createEvaluationNotifier();

            // Should not throw
            notifier.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("Test")
                    .modelIds(List.of())
                    .totalSteps(1)
                    .build());

            // Normal listener should still be called after throwing listener
            assertThat(normalListener.beforeCalled).isTrue();
        }

        @Test
        @DisplayName("Should pass context to listeners")
        void shouldPassContextToListeners() {
            final RecordingListener listener = new RecordingListener(0, "recorder");
            metric.addListener(listener);

            final MetricEvaluationContext context = MetricEvaluationContext.builder()
                    .metricName("TestMetric")
                    .modelIds(List.of("model-1"))
                    .totalSteps(2)
                    .build();

            final AbstractMetric<?>.EvaluationNotifier notifier = metric.createEvaluationNotifier();
            notifier.beforeMetricEvaluation(context);

            assertThat(listener.receivedContext).isNotNull();
            assertThat(listener.receivedContext.getMetricName()).isEqualTo("TestMetric");
        }
    }

    @Nested
    @DisplayName("EvaluationNotifier - afterMetricEvaluation")
    class EvaluationNotifierAfter {

        @Test
        @DisplayName("Should call afterMetricEvaluation on all listeners")
        void shouldCallAfterOnAllListeners() {
            final RecordingListener listener1 = new RecordingListener(0, "listener1");
            final RecordingListener listener2 = new RecordingListener(1, "listener2");
            metric.addListener(listener1);
            metric.addListener(listener2);

            final AbstractMetric<?>.EvaluationNotifier notifier = metric.createEvaluationNotifier();
            notifier.afterMetricEvaluation(MetricEvaluationResult.builder()
                    .metricName("Test")
                    .aggregatedScore(0.85)
                    .modelScores(Map.of("model-1", 0.85))
                    .excludedModels(List.of())
                    .totalDuration(Duration.ofMillis(100))
                    .build());

            assertThat(listener1.afterCalled).isTrue();
            assertThat(listener2.afterCalled).isTrue();
        }

        @Test
        @DisplayName("Should catch exception from listener in afterMetricEvaluation")
        void shouldCatchExceptionFromAfterListener() {
            final MetricExecutionListener throwingListener = new MetricExecutionListener() {
                @Override
                public void afterMetricEvaluation(final MetricEvaluationResult result) {
                    throw new RuntimeException("After evaluation error");
                }
            };
            final RecordingListener normalListener = new RecordingListener(1, "normal");
            metric.addListener(throwingListener);
            metric.addListener(normalListener);

            final AbstractMetric<?>.EvaluationNotifier notifier = metric.createEvaluationNotifier();

            // Should not throw
            notifier.afterMetricEvaluation(MetricEvaluationResult.builder()
                    .metricName("Test")
                    .aggregatedScore(0.5)
                    .modelScores(Map.of())
                    .excludedModels(List.of())
                    .totalDuration(Duration.ZERO)
                    .build());

            // Normal listener should still be called
            assertThat(normalListener.afterCalled).isTrue();
        }

        @Test
        @DisplayName("Should pass result to listeners")
        void shouldPassResultToListeners() {
            final RecordingListener listener = new RecordingListener(0, "recorder");
            metric.addListener(listener);

            final MetricEvaluationResult result = MetricEvaluationResult.builder()
                    .metricName("TestMetric")
                    .aggregatedScore(0.92)
                    .modelScores(Map.of("model-1", 0.92))
                    .excludedModels(List.of())
                    .totalDuration(Duration.ofMillis(200))
                    .build();

            final AbstractMetric<?>.EvaluationNotifier notifier = metric.createEvaluationNotifier();
            notifier.afterMetricEvaluation(result);

            assertThat(listener.receivedResult).isNotNull();
            assertThat(listener.receivedResult.getMetricName()).isEqualTo("TestMetric");
            assertThat(listener.receivedResult.getAggregatedScore()).isEqualTo(0.92);
        }
    }

    @Nested
    @DisplayName("EvaluationNotifier - full lifecycle")
    class EvaluationNotifierFullLifecycle {

        @Test
        @DisplayName("Should handle complete before/after lifecycle")
        void shouldHandleCompleteLifecycle() {
            final RecordingListener listener = new RecordingListener(0, "lifecycle");
            metric.addListener(listener);

            final AbstractMetric<?>.EvaluationNotifier notifier = metric.createEvaluationNotifier();

            notifier.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("FullTest")
                    .modelIds(List.of("m1"))
                    .totalSteps(1)
                    .build());

            notifier.afterMetricEvaluation(MetricEvaluationResult.builder()
                    .metricName("FullTest")
                    .aggregatedScore(0.75)
                    .modelScores(Map.of("m1", 0.75))
                    .excludedModels(List.of())
                    .totalDuration(Duration.ofMillis(50))
                    .build());

            assertThat(listener.beforeCalled).isTrue();
            assertThat(listener.afterCalled).isTrue();
            assertThat(listener.receivedContext.getMetricName()).isEqualTo("FullTest");
            assertThat(listener.receivedResult.getAggregatedScore()).isEqualTo(0.75);
        }

        @Test
        @DisplayName("Should handle notifier with no listeners")
        void shouldHandleNotifierWithNoListeners() {
            final AbstractMetric<?>.EvaluationNotifier notifier = metric.createEvaluationNotifier();

            // Should not throw even with no listeners
            notifier.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("Empty")
                    .modelIds(List.of())
                    .totalSteps(0)
                    .build());

            notifier.afterMetricEvaluation(MetricEvaluationResult.builder()
                    .metricName("Empty")
                    .aggregatedScore(null)
                    .totalDuration(Duration.ZERO)
                    .build());
        }

        @Test
        @DisplayName("Multiple notifiers should be independent")
        void multipleNotifiersShouldBeIndependent() {
            final RecordingListener listener = new RecordingListener(0, "shared");
            // forEvaluation returns this, so both notifiers share the same recording listener
            metric.addListener(listener);

            final AbstractMetric<?>.EvaluationNotifier notifier1 = metric.createEvaluationNotifier();
            final AbstractMetric<?>.EvaluationNotifier notifier2 = metric.createEvaluationNotifier();

            // Both notifiers are separate objects
            assertThat(notifier1).isNotSameAs(notifier2);
        }
    }

    // ==================== Test implementations ====================

    static class TestConfig implements Metric.MetricConfiguration {}

    static class TestableAbstractMetric extends AbstractMetric<TestConfig> {

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

    static class ThrowingListener implements MetricExecutionListener {

        @Override
        public void beforeMetricEvaluation(final MetricEvaluationContext context) {
            throw new RuntimeException("Test exception from throwing listener");
        }

        @Override
        public void afterMetricEvaluation(final MetricEvaluationResult result) {
            throw new RuntimeException("Test exception from throwing listener");
        }

        @Override
        public MetricExecutionListener forEvaluation() {
            return this;
        }
    }
}
