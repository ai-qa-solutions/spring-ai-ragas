package ai.qa.solutions.allure.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import ai.qa.solutions.allure.config.AllureRagasProperties;
import ai.qa.solutions.allure.methodology.MethodologyLoader;
import ai.qa.solutions.allure.model.EvaluationReportData;
import ai.qa.solutions.allure.template.FreemarkerTemplateEngine;
import ai.qa.solutions.execution.ModelResult;
import ai.qa.solutions.execution.listener.MetricExecutionListener;
import ai.qa.solutions.execution.listener.dto.*;
import ai.qa.solutions.sample.Sample;
import io.qameta.allure.AllureLifecycle;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AllureMetricExecutionListener")
class AllureMetricExecutionListenerTest {

    private static final String PARENT_UUID = "parent-test-uuid";

    @Mock
    private FreemarkerTemplateEngine templateEngine;

    @Mock
    private AllureAttachmentWriter attachmentWriter;

    @Mock
    private MethodologyLoader methodologyLoader;

    @Mock
    private AllureLifecycle lifecycle;

    private AllureRagasProperties properties;
    private AllureMetricExecutionListener listener;

    @BeforeEach
    void setUp() {
        properties = new AllureRagasProperties();
        properties.setLanguage("en");
        lenient().when(lifecycle.getCurrentTestCaseOrStep()).thenReturn(Optional.of(PARENT_UUID));
        listener = new AllureMetricExecutionListener(
                properties, templateEngine, attachmentWriter, methodologyLoader, lifecycle);
    }

    @Nested
    @DisplayName("forEvaluation")
    class ForEvaluation {

        @Test
        @DisplayName("should return new instance for thread safety")
        void shouldReturnNewInstance() {
            final MetricExecutionListener instance1 = listener.forEvaluation();
            final MetricExecutionListener instance2 = listener.forEvaluation();

            assertThat(instance1).isNotSameAs(listener);
            assertThat(instance2).isNotSameAs(listener);
            assertThat(instance1).isNotSameAs(instance2);
        }

        @Test
        @DisplayName("should return AllureMetricExecutionListener instance")
        void shouldReturnCorrectType() {
            final MetricExecutionListener instance = listener.forEvaluation();

            assertThat(instance).isInstanceOf(AllureMetricExecutionListener.class);
        }
    }

    @Nested
    @DisplayName("getOrder")
    class GetOrder {

        @Test
        @DisplayName("should return order 100")
        void shouldReturnOrder100() {
            assertThat(listener.getOrder()).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("lifecycle")
    class Lifecycle {

        @Test
        @DisplayName("should generate attachments after evaluation")
        void shouldGenerateAttachmentsAfterEvaluation() {
            // Setup
            when(methodologyLoader.loadMethodologyHtml(any())).thenReturn("<p>Methodology</p>");
            when(methodologyLoader.loadMethodologyMarkdown(any())).thenReturn("# Methodology");

            final Sample sample = Sample.builder()
                    .userInput("What is AI?")
                    .response("AI is artificial intelligence")
                    .build();

            final MetricEvaluationContext context = MetricEvaluationContext.builder()
                    .metricName("Faithfulness")
                    .sample(sample)
                    .modelIds(List.of("model-1", "model-2"))
                    .totalSteps(2)
                    .build();

            final StepResults stepResults = StepResults.builder()
                    .stepName("GenerateStatements")
                    .stepIndex(0)
                    .totalSteps(2)
                    .stepType(StepType.LLM)
                    .request("Generate statements from: AI is artificial intelligence")
                    .results(List.of(
                            ModelResult.success("model-1", "statements", Duration.ofMillis(100), "prompt"),
                            ModelResult.success("model-2", "statements", Duration.ofMillis(150), "prompt")))
                    .build();

            final MetricEvaluationResult result = MetricEvaluationResult.builder()
                    .metricName("Faithfulness")
                    .aggregatedScore(0.85)
                    .modelScores(Map.of("model-1", 0.9, "model-2", 0.8))
                    .totalDuration(Duration.ofMillis(500))
                    .sample(sample)
                    .steps(List.of(stepResults))
                    .build();

            // Execute lifecycle
            listener.beforeMetricEvaluation(context);
            listener.afterMetricEvaluation(result);

            // Verify step was created and attachments were written
            verify(lifecycle).startStep(eq(PARENT_UUID), anyString(), any());
            verify(lifecycle).stopStep(anyString());

            final ArgumentCaptor<EvaluationReportData> captor = ArgumentCaptor.forClass(EvaluationReportData.class);
            verify(attachmentWriter).writeHtmlAttachmentToStep(anyString(), captor.capture());
            verify(attachmentWriter).writeMarkdownAttachmentToStep(anyString(), any());

            final EvaluationReportData reportData = captor.getValue();
            assertThat(reportData.getMetricName()).isEqualTo("Faithfulness");
            assertThat(reportData.getAggregatedScore()).isEqualTo(0.85);
            assertThat(reportData.getSteps()).hasSize(1);
            assertThat(reportData.getUserInput()).isEqualTo("What is AI?");
        }

        @Test
        @DisplayName("should track model exclusions from enriched result")
        void shouldTrackModelExclusions() {
            when(methodologyLoader.loadMethodologyHtml(any())).thenReturn("<p>Methodology</p>");
            when(methodologyLoader.loadMethodologyMarkdown(any())).thenReturn("# Methodology");

            final Sample sample = Sample.builder().userInput("test").build();

            final MetricEvaluationContext context = MetricEvaluationContext.builder()
                    .metricName("TestMetric")
                    .sample(sample)
                    .modelIds(List.of("model-1"))
                    .totalSteps(1)
                    .build();

            final ModelExclusionEvent exclusion = ModelExclusionEvent.builder()
                    .modelId("model-1")
                    .failedStepName("Step1")
                    .failedStepIndex(0)
                    .cause(new RuntimeException("API error"))
                    .build();

            final MetricEvaluationResult result = MetricEvaluationResult.builder()
                    .metricName("TestMetric")
                    .excludedModels(List.of("model-1"))
                    .totalDuration(Duration.ofMillis(100))
                    .sample(sample)
                    .exclusions(List.of(exclusion))
                    .build();

            listener.beforeMetricEvaluation(context);
            listener.afterMetricEvaluation(result);

            final ArgumentCaptor<EvaluationReportData> captor = ArgumentCaptor.forClass(EvaluationReportData.class);
            verify(attachmentWriter).writeHtmlAttachmentToStep(anyString(), captor.capture());

            final EvaluationReportData reportData = captor.getValue();
            assertThat(reportData.getExclusions()).hasSize(1);
            assertThat(reportData.getExclusions().get(0).getModelId()).isEqualTo("model-1");
            assertThat(reportData.getExclusions().get(0).getErrorMessage()).contains("API error");
        }
    }

    @Nested
    @DisplayName("error handling")
    class ErrorHandling {

        @Test
        @DisplayName("should not throw when attachment writing fails")
        void shouldNotThrowWhenAttachmentWritingFails() {
            when(methodologyLoader.loadMethodologyHtml(any())).thenReturn("<p>Methodology</p>");
            when(methodologyLoader.loadMethodologyMarkdown(any())).thenReturn("# Methodology");
            doThrow(new RuntimeException("Write failed"))
                    .when(attachmentWriter)
                    .writeHtmlAttachmentToStep(anyString(), any());

            final Sample sample = Sample.builder().userInput("test").build();
            final MetricEvaluationContext context = MetricEvaluationContext.builder()
                    .metricName("TestMetric")
                    .sample(sample)
                    .modelIds(List.of("model-1"))
                    .totalSteps(1)
                    .build();

            final MetricEvaluationResult result = MetricEvaluationResult.builder()
                    .metricName("TestMetric")
                    .aggregatedScore(0.5)
                    .totalDuration(Duration.ofMillis(100))
                    .sample(sample)
                    .build();

            listener.beforeMetricEvaluation(context);
            // Should not throw
            listener.afterMetricEvaluation(result);

            // Verify step was still stopped even after error
            verify(lifecycle).stopStep(anyString());
        }
    }

    @Nested
    @DisplayName("ThreadLocal stack safety (GitHub issue #1)")
    class ThreadLocalStackSafety {

        /**
         * Creates an AllureLifecycle mock that simulates real ThreadLocal step stack behavior.
         * <p>
         * Real AllureLifecycle maintains a per-thread stack of step UUIDs:
         * <ul>
         *   <li>startStep() pushes the step UUID onto the calling thread's stack</li>
         *   <li>stopStep() removes the step UUID from the calling thread's stack</li>
         *   <li>getCurrentTestCaseOrStep() returns the top of the calling thread's stack</li>
         *   <li>updateStep() only modifies global storage, does NOT touch the ThreadLocal stack</li>
         * </ul>
         * The main thread's stack is pre-populated with PARENT_UUID (simulating an active test case).
         */
        private AllureLifecycle createThreadAwareLifecycleMock() {
            final ThreadLocal<Deque<String>> threadStacks = ThreadLocal.withInitial(ArrayDeque::new);
            threadStacks.get().push(PARENT_UUID);

            final AllureLifecycle mock = mock(AllureLifecycle.class);

            when(mock.getCurrentTestCaseOrStep()).thenAnswer(invocation -> {
                final Deque<String> stack = threadStacks.get();
                return stack.isEmpty() ? Optional.empty() : Optional.of(stack.peek());
            });

            doAnswer(invocation -> {
                        final String stepUuid = invocation.getArgument(1);
                        threadStacks.get().push(stepUuid);
                        return null;
                    })
                    .when(mock)
                    .startStep(anyString(), anyString(), any());

            doAnswer(invocation -> {
                        final String stepUuid = invocation.getArgument(0);
                        threadStacks.get().remove(stepUuid);
                        return null;
                    })
                    .when(mock)
                    .stopStep(anyString());

            return mock;
        }

        @Test
        @DisplayName("sequential evaluations with cross-thread callbacks should each use correct parent UUID")
        void sequentialEvaluationsWithCrossThreadCallbacksShouldUseCorrectParentUuid() {
            final AllureLifecycle stackLifecycle = createThreadAwareLifecycleMock();
            when(methodologyLoader.loadMethodologyHtml(any())).thenReturn("<p>Methodology</p>");
            when(methodologyLoader.loadMethodologyMarkdown(any())).thenReturn("# Methodology");

            final AllureMetricExecutionListener baseListener = new AllureMetricExecutionListener(
                    properties, templateEngine, attachmentWriter, methodologyLoader, stackLifecycle);

            final Sample sample = Sample.builder()
                    .userInput("test input")
                    .response("test response")
                    .build();

            // --- First metric evaluation ---
            final AllureMetricExecutionListener eval1 = (AllureMetricExecutionListener) baseListener.forEvaluation();

            // beforeMetricEvaluation runs on main thread (captures parent UUID)
            eval1.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("GoalAccuracy")
                    .sample(sample)
                    .modelIds(List.of("model-1"))
                    .totalSteps(1)
                    .build());

            // afterMetricEvaluation runs on async thread (simulates executor.runAsync())
            CompletableFuture.runAsync(() -> eval1.afterMetricEvaluation(MetricEvaluationResult.builder()
                            .metricName("GoalAccuracy")
                            .aggregatedScore(0.8)
                            .totalDuration(Duration.ofMillis(100))
                            .sample(sample)
                            .build()))
                    .join();

            // --- Second metric evaluation ---
            final AllureMetricExecutionListener eval2 = (AllureMetricExecutionListener) baseListener.forEvaluation();

            // beforeMetricEvaluation runs on main thread — should see PARENT_UUID, not stale step UUID
            eval2.beforeMetricEvaluation(MetricEvaluationContext.builder()
                    .metricName("AspectCritic")
                    .sample(sample)
                    .modelIds(List.of("model-1"))
                    .totalSteps(1)
                    .build());

            CompletableFuture.runAsync(() -> eval2.afterMetricEvaluation(MetricEvaluationResult.builder()
                            .metricName("AspectCritic")
                            .aggregatedScore(0.9)
                            .totalDuration(Duration.ofMillis(100))
                            .sample(sample)
                            .build()))
                    .join();

            // Verify: both startStep calls used PARENT_UUID as parent (not a stale metric step UUID)
            final ArgumentCaptor<String> parentCaptor = ArgumentCaptor.forClass(String.class);
            verify(stackLifecycle, times(2)).startStep(parentCaptor.capture(), anyString(), any());
            assertThat(parentCaptor.getAllValues()).containsExactly(PARENT_UUID, PARENT_UUID);

            // Verify: attachments generated for both metrics
            final ArgumentCaptor<EvaluationReportData> reportCaptor =
                    ArgumentCaptor.forClass(EvaluationReportData.class);
            verify(attachmentWriter, times(2)).writeHtmlAttachmentToStep(anyString(), reportCaptor.capture());
            assertThat(reportCaptor.getAllValues())
                    .extracting(EvaluationReportData::getMetricName)
                    .containsExactly("GoalAccuracy", "AspectCritic");
        }

        @Test
        @DisplayName("three sequential evaluations with cross-thread callbacks should all use correct parent UUID")
        void threeSequentialEvaluationsShouldAllUseCorrectParentUuid() {
            // Reproduces the exact scenario from GitHub issue #1
            final AllureLifecycle stackLifecycle = createThreadAwareLifecycleMock();
            when(methodologyLoader.loadMethodologyHtml(any())).thenReturn("<p>Methodology</p>");
            when(methodologyLoader.loadMethodologyMarkdown(any())).thenReturn("# Methodology");

            final AllureMetricExecutionListener baseListener = new AllureMetricExecutionListener(
                    properties, templateEngine, attachmentWriter, methodologyLoader, stackLifecycle);

            final Sample sample = Sample.builder()
                    .userInput("test input")
                    .response("test response")
                    .build();

            // Evaluate three metrics sequentially (as in the issue's reproduction case)
            for (final String metricName : List.of("GoalAccuracy", "AspectCritic", "RubricsScore")) {
                final AllureMetricExecutionListener evalListener =
                        (AllureMetricExecutionListener) baseListener.forEvaluation();

                // before: main thread
                evalListener.beforeMetricEvaluation(MetricEvaluationContext.builder()
                        .metricName(metricName)
                        .sample(sample)
                        .modelIds(List.of("model-1"))
                        .totalSteps(1)
                        .build());

                // after: async thread
                CompletableFuture.runAsync(() -> evalListener.afterMetricEvaluation(MetricEvaluationResult.builder()
                                .metricName(metricName)
                                .aggregatedScore(0.7)
                                .totalDuration(Duration.ofMillis(100))
                                .sample(sample)
                                .build()))
                        .join();
            }

            // Verify: all 3 startStep calls used PARENT_UUID (no stack corruption)
            final ArgumentCaptor<String> parentCaptor = ArgumentCaptor.forClass(String.class);
            verify(stackLifecycle, times(3)).startStep(parentCaptor.capture(), anyString(), any());
            assertThat(parentCaptor.getAllValues()).containsOnly(PARENT_UUID);

            // Verify: all 3 metrics got their attachments
            verify(attachmentWriter, times(3)).writeHtmlAttachmentToStep(anyString(), any());
            verify(attachmentWriter, times(3)).writeMarkdownAttachmentToStep(anyString(), any());
        }

        @Test
        @DisplayName("beforeMetricEvaluation should not call startStep or stopStep (Option B)")
        void beforeShouldNotCallStartStepOrStopStep() {
            final Sample sample = Sample.builder().userInput("test").build();

            final MetricEvaluationContext context = MetricEvaluationContext.builder()
                    .metricName("TestMetric")
                    .sample(sample)
                    .modelIds(List.of("model-1"))
                    .totalSteps(1)
                    .build();

            listener.beforeMetricEvaluation(context);

            // Verify: only getCurrentTestCaseOrStep called, NO startStep or stopStep
            verify(lifecycle).getCurrentTestCaseOrStep();
            verify(lifecycle, never()).startStep(anyString(), anyString(), any());
            verify(lifecycle, never()).stopStep(anyString());
        }

        @Test
        @DisplayName("afterMetricEvaluation should startStep then stopStep on same invocation (Option B)")
        void afterShouldStartAndStopStepOnSameInvocation() {
            when(methodologyLoader.loadMethodologyHtml(any())).thenReturn("<p>Methodology</p>");
            when(methodologyLoader.loadMethodologyMarkdown(any())).thenReturn("# Methodology");

            final Sample sample = Sample.builder().userInput("test").build();

            final MetricEvaluationContext context = MetricEvaluationContext.builder()
                    .metricName("TestMetric")
                    .sample(sample)
                    .modelIds(List.of("model-1"))
                    .totalSteps(1)
                    .build();

            final MetricEvaluationResult result = MetricEvaluationResult.builder()
                    .metricName("TestMetric")
                    .aggregatedScore(0.85)
                    .totalDuration(Duration.ofMillis(100))
                    .sample(sample)
                    .build();

            listener.beforeMetricEvaluation(context);
            listener.afterMetricEvaluation(result);

            // Verify: startStep with captured PARENT_UUID, then stopStep with same step UUID
            final ArgumentCaptor<String> startUuidCaptor = ArgumentCaptor.forClass(String.class);
            final ArgumentCaptor<String> stopUuidCaptor = ArgumentCaptor.forClass(String.class);

            final InOrder inOrder = inOrder(lifecycle);
            inOrder.verify(lifecycle).startStep(eq(PARENT_UUID), startUuidCaptor.capture(), any());
            inOrder.verify(lifecycle).stopStep(stopUuidCaptor.capture());

            assertThat(stopUuidCaptor.getValue()).isEqualTo(startUuidCaptor.getValue());
        }

        @Test
        @DisplayName("afterMetricEvaluation error path should still stop step for cleanup")
        void afterErrorPathShouldStopStep() {
            when(methodologyLoader.loadMethodologyHtml(any())).thenReturn("<p>Methodology</p>");
            when(methodologyLoader.loadMethodologyMarkdown(any())).thenReturn("# Methodology");
            doThrow(new RuntimeException("Write failed"))
                    .when(attachmentWriter)
                    .writeHtmlAttachmentToStep(anyString(), any());

            final Sample sample = Sample.builder().userInput("test").build();

            final MetricEvaluationContext context = MetricEvaluationContext.builder()
                    .metricName("TestMetric")
                    .sample(sample)
                    .modelIds(List.of("model-1"))
                    .totalSteps(1)
                    .build();

            final MetricEvaluationResult result = MetricEvaluationResult.builder()
                    .metricName("TestMetric")
                    .aggregatedScore(0.5)
                    .totalDuration(Duration.ofMillis(100))
                    .sample(sample)
                    .build();

            listener.beforeMetricEvaluation(context);
            listener.afterMetricEvaluation(result);

            // Verify: even on error, stopStep is called for cleanup
            verify(lifecycle).stopStep(anyString());
        }
    }
}
