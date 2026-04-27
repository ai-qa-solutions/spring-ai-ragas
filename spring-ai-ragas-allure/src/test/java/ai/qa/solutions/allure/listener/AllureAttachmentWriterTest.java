package ai.qa.solutions.allure.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import ai.qa.solutions.allure.model.EvaluationReportData;
import ai.qa.solutions.allure.template.FreemarkerTemplateEngine;
import io.qameta.allure.AllureLifecycle;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AllureAttachmentWriter")
class AllureAttachmentWriterTest {

    @Mock
    private AllureLifecycle lifecycle;

    @Mock
    private FreemarkerTemplateEngine templateEngine;

    private AllureAttachmentWriter writer;

    @BeforeEach
    void setUp() {
        writer = new AllureAttachmentWriter(lifecycle, templateEngine);
    }

    @Nested
    @DisplayName("writeHtmlAttachment")
    class WriteHtmlAttachment {

        @Test
        @DisplayName("should write HTML attachment with correct name")
        void shouldWriteHtmlAttachment() {
            final EvaluationReportData data = createTestData(0.85);
            when(templateEngine.renderHtml(data)).thenReturn("<html>content</html>");

            writer.writeHtmlAttachment(data);

            verify(lifecycle)
                    .addAttachment(
                            eq("TestMetric Report"),
                            eq("text/html"),
                            eq("html"),
                            eq("<html>content</html>".getBytes(StandardCharsets.UTF_8)));
        }

        @Test
        @DisplayName("should not throw when rendering fails")
        void shouldNotThrowWhenRenderingFails() {
            final EvaluationReportData data = createTestData(0.5);
            when(templateEngine.renderHtml(data)).thenThrow(new RuntimeException("Render failed"));

            // Should not throw
            writer.writeHtmlAttachment(data);

            verify(lifecycle, never()).addAttachment(any(), any(), any(), any(byte[].class));
        }
    }

    @Nested
    @DisplayName("writeMarkdownAttachment")
    class WriteMarkdownAttachment {

        @Test
        @DisplayName("should write Markdown attachment with correct name")
        void shouldWriteMarkdownAttachment() {
            final EvaluationReportData data = createTestData(0.72);
            when(templateEngine.renderMarkdown(data)).thenReturn("# Report");

            writer.writeMarkdownAttachment(data);

            verify(lifecycle)
                    .addAttachment(
                            eq("TestMetric MD"),
                            eq("text/markdown"),
                            eq("md"),
                            eq("# Report".getBytes(StandardCharsets.UTF_8)));
        }

        @Test
        @DisplayName("should not throw when rendering fails")
        void shouldNotThrowWhenRenderingFails() {
            final EvaluationReportData data = createTestData(0.5);
            when(templateEngine.renderMarkdown(data)).thenThrow(new RuntimeException("Render failed"));

            // Should not throw
            writer.writeMarkdownAttachment(data);

            verify(lifecycle, never()).addAttachment(any(), any(), any(), any(byte[].class));
        }
    }

    @Nested
    @DisplayName("RenderConfig overloads")
    class RenderConfigOverloads {

        @Test
        @DisplayName("writeHtmlAttachmentToStep with RenderConfig should propagate config to template engine")
        void writeHtmlAttachmentToStepWithRenderConfigShouldPropagateConfig() {
            final EvaluationReportData data = createTestData(0.85);
            final RenderConfig customConfig = new RenderConfig(true, true, true, true, true, true, true);
            when(templateEngine.renderHtml(eq(data), eq(customConfig))).thenReturn("<html>cfg</html>");

            writer.writeHtmlAttachmentToStep("step-uuid", data, customConfig);

            verify(templateEngine).renderHtml(eq(data), eq(customConfig));
        }

        @Test
        @DisplayName("writeMarkdownAttachmentToStep with RenderConfig should propagate config to template engine")
        void writeMarkdownAttachmentToStepWithRenderConfigShouldPropagateConfig() {
            final EvaluationReportData data = createTestData(0.85);
            final RenderConfig customConfig = new RenderConfig(true, true, true, true, true, true, true);
            when(templateEngine.renderMarkdown(eq(data), eq(customConfig))).thenReturn("# cfg");

            writer.writeMarkdownAttachmentToStep("step-uuid", data, customConfig);

            verify(templateEngine).renderMarkdown(eq(data), eq(customConfig));
        }

        @Test
        @DisplayName("writeHtmlAttachment with RenderConfig should propagate config to template engine")
        void writeHtmlAttachmentWithRenderConfigShouldPropagateConfig() {
            final EvaluationReportData data = createTestData(0.85);
            final RenderConfig customConfig = new RenderConfig(true, true, false, true, true, true, true);
            when(templateEngine.renderHtml(eq(data), eq(customConfig))).thenReturn("<html>cfg</html>");

            writer.writeHtmlAttachment(data, customConfig);

            verify(templateEngine).renderHtml(eq(data), eq(customConfig));
        }

        @Test
        @DisplayName("writeMarkdownAttachment with RenderConfig should propagate config to template engine")
        void writeMarkdownAttachmentWithRenderConfigShouldPropagateConfig() {
            final EvaluationReportData data = createTestData(0.85);
            final RenderConfig customConfig = new RenderConfig(true, true, true, false, true, true, true);
            when(templateEngine.renderMarkdown(eq(data), eq(customConfig))).thenReturn("# cfg");

            writer.writeMarkdownAttachment(data, customConfig);

            verify(templateEngine).renderMarkdown(eq(data), eq(customConfig));
        }

        @Test
        @DisplayName("legacy 2-arg writeHtmlAttachmentToStep should call template engine without explicit RenderConfig")
        void legacyTwoArgWriteHtmlAttachmentToStepShouldUseEngineDefaults() {
            final EvaluationReportData data = createTestData(0.85);
            when(templateEngine.renderHtml(eq(data))).thenReturn("<html>legacy</html>");

            writer.writeHtmlAttachmentToStep("step-uuid", data);

            verify(templateEngine).renderHtml(eq(data));
        }

        @Test
        @DisplayName(
                "legacy 2-arg writeMarkdownAttachmentToStep should call template engine without explicit RenderConfig")
        void legacyTwoArgWriteMarkdownAttachmentToStepShouldUseEngineDefaults() {
            final EvaluationReportData data = createTestData(0.85);
            when(templateEngine.renderMarkdown(eq(data))).thenReturn("# legacy");

            writer.writeMarkdownAttachmentToStep("step-uuid", data);

            verify(templateEngine).renderMarkdown(eq(data));
        }
    }

    private EvaluationReportData createTestData(final Double score) {
        return EvaluationReportData.builder()
                .metricName("TestMetric")
                .aggregatedScore(score)
                .build();
    }
}
