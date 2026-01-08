package ai.qa.solutions.execution.listener;

import static org.assertj.core.api.Assertions.assertThat;

import ai.qa.solutions.execution.listener.dto.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("MetricExecutionListener Tests")
class MetricExecutionListenerTest {

    @Nested
    @DisplayName("Default Methods")
    class DefaultMethods {

        @Test
        @DisplayName("Default getOrder should return 0")
        void defaultGetOrderShouldReturnZero() {
            // Given
            final MetricExecutionListener listener = new MetricExecutionListener() {};

            // When/Then
            assertThat(listener.getOrder()).isZero();
        }

        @Test
        @DisplayName("Default forEvaluation should return this")
        void defaultForEvaluationShouldReturnThis() {
            // Given
            final MetricExecutionListener listener = new MetricExecutionListener() {};

            // When
            final MetricExecutionListener result = listener.forEvaluation();

            // Then
            assertThat(result).isSameAs(listener);
        }

        @Test
        @DisplayName("Default beforeMetricEvaluation should do nothing")
        void defaultBeforeMetricEvaluationShouldDoNothing() {
            // Given
            final MetricExecutionListener listener = new MetricExecutionListener() {};
            final MetricEvaluationContext context = MetricEvaluationContext.builder()
                    .metricName("TestMetric")
                    .modelIds(List.of("model-1"))
                    .totalSteps(1)
                    .build();

            // When/Then - should not throw
            listener.beforeMetricEvaluation(context);
        }

        @Test
        @DisplayName("Default beforeStep should do nothing")
        void defaultBeforeStepShouldDoNothing() {
            // Given
            final MetricExecutionListener listener = new MetricExecutionListener() {};
            final StepContext context = StepContext.builder()
                    .stepName("Step1")
                    .stepIndex(0)
                    .totalSteps(1)
                    .build();

            // When/Then - should not throw
            listener.beforeStep(context);
        }

        @Test
        @DisplayName("Default afterStep should do nothing")
        void defaultAfterStepShouldDoNothing() {
            // Given
            final MetricExecutionListener listener = new MetricExecutionListener() {};
            final StepResults results = StepResults.builder()
                    .stepName("Step1")
                    .stepIndex(0)
                    .totalSteps(1)
                    .stepType(StepType.LLM)
                    .build();

            // When/Then - should not throw
            listener.afterStep(results);
        }

        @Test
        @DisplayName("Default onModelExcluded should do nothing")
        void defaultOnModelExcludedShouldDoNothing() {
            // Given
            final MetricExecutionListener listener = new MetricExecutionListener() {};
            final ModelExclusionEvent event = ModelExclusionEvent.builder()
                    .modelId("model-1")
                    .failedStepName("Step1")
                    .failedStepIndex(0)
                    .cause(new RuntimeException("error"))
                    .build();

            // When/Then - should not throw
            listener.onModelExcluded(event);
        }

        @Test
        @DisplayName("Default afterMetricEvaluation should do nothing")
        void defaultAfterMetricEvaluationShouldDoNothing() {
            // Given
            final MetricExecutionListener listener = new MetricExecutionListener() {};
            final MetricEvaluationResult result = MetricEvaluationResult.builder()
                    .metricName("TestMetric")
                    .aggregatedScore(0.8)
                    .modelScores(Map.of("model-1", 0.8))
                    .excludedModels(List.of())
                    .totalDuration(Duration.ofMillis(100))
                    .build();

            // When/Then - should not throw
            listener.afterMetricEvaluation(result);
        }
    }

    @Nested
    @DisplayName("Custom Implementation")
    class CustomImplementation {

        @Test
        @DisplayName("Should be able to override all methods")
        void shouldBeAbleToOverrideAllMethods() {
            // Given
            final List<String> callLog = new ArrayList<>();

            final MetricExecutionListener listener = new MetricExecutionListener() {
                @Override
                public void beforeMetricEvaluation(MetricEvaluationContext context) {
                    callLog.add("before:" + context.getMetricName());
                }

                @Override
                public void beforeStep(StepContext context) {
                    callLog.add("beforeStep:" + context.getStepName());
                }

                @Override
                public void afterStep(StepResults results) {
                    callLog.add("afterStep:" + results.getStepName());
                }

                @Override
                public void onModelExcluded(ModelExclusionEvent event) {
                    callLog.add("excluded:" + event.getModelId());
                }

                @Override
                public void afterMetricEvaluation(MetricEvaluationResult result) {
                    callLog.add("after:" + result.getMetricName());
                }

                @Override
                public int getOrder() {
                    return 10;
                }
            };

            // When
            listener.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("TestMetric")
                    .modelIds(List.of("m1"))
                    .totalSteps(1)
                    .build());
            listener.beforeStep(StepContext.builder()
                    .stepName("Step1")
                    .stepIndex(0)
                    .totalSteps(1)
                    .build());
            listener.afterStep(StepResults.builder()
                    .stepName("Step1")
                    .stepIndex(0)
                    .totalSteps(1)
                    .stepType(StepType.LLM)
                    .build());
            listener.onModelExcluded(ModelExclusionEvent.builder()
                    .modelId("model-1")
                    .failedStepName("Step1")
                    .failedStepIndex(0)
                    .cause(new RuntimeException("error"))
                    .build());
            listener.afterMetricEvaluation(MetricEvaluationResult.builder()
                    .metricName("TestMetric")
                    .aggregatedScore(0.8)
                    .modelScores(Map.of())
                    .excludedModels(List.of())
                    .totalDuration(Duration.ZERO)
                    .build());

            // Then
            assertThat(callLog)
                    .containsExactly(
                            "before:TestMetric",
                            "beforeStep:Step1",
                            "afterStep:Step1",
                            "excluded:model-1",
                            "after:TestMetric");
            assertThat(listener.getOrder()).isEqualTo(10);
        }

        @Test
        @DisplayName("Stateful listener should return new instance from forEvaluation")
        void statefulListenerShouldReturnNewInstance() {
            // Given
            final MetricExecutionListener statefulListener = new MetricExecutionListener() {
                private int callCount = 0;

                @Override
                public void afterStep(StepResults results) {
                    callCount++;
                }

                @Override
                public MetricExecutionListener forEvaluation() {
                    return new MetricExecutionListener() {
                        private int callCount = 0;

                        @Override
                        public void afterStep(StepResults results) {
                            callCount++;
                        }
                    };
                }
            };

            // When
            final MetricExecutionListener instance1 = statefulListener.forEvaluation();
            final MetricExecutionListener instance2 = statefulListener.forEvaluation();

            // Then
            assertThat(instance1).isNotSameAs(statefulListener);
            assertThat(instance2).isNotSameAs(statefulListener);
            assertThat(instance1).isNotSameAs(instance2);
        }
    }

    @Nested
    @DisplayName("Listener Ordering")
    class ListenerOrdering {

        @Test
        @DisplayName("Listeners with different order values should be distinguishable")
        void listenersWithDifferentOrderShouldBeDistinguishable() {
            // Given
            final MetricExecutionListener first = new MetricExecutionListener() {
                @Override
                public int getOrder() {
                    return -100;
                }
            };

            final MetricExecutionListener second = new MetricExecutionListener() {
                @Override
                public int getOrder() {
                    return 0;
                }
            };

            final MetricExecutionListener third = new MetricExecutionListener() {
                @Override
                public int getOrder() {
                    return 100;
                }
            };

            // When/Then
            assertThat(first.getOrder()).isLessThan(second.getOrder());
            assertThat(second.getOrder()).isLessThan(third.getOrder());
        }

        @Test
        @DisplayName("Can use Integer.MIN_VALUE for highest priority")
        void canUseMinValueForHighestPriority() {
            // Given
            final MetricExecutionListener highestPriority = new MetricExecutionListener() {
                @Override
                public int getOrder() {
                    return Integer.MIN_VALUE;
                }
            };

            // Then
            assertThat(highestPriority.getOrder()).isEqualTo(Integer.MIN_VALUE);
        }
    }
}
