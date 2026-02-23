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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
}
