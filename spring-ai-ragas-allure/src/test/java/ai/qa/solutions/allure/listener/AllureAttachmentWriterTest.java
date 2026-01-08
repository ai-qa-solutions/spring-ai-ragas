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
                            eq("TestMetric [85.00%] Report"),
                            eq("text/html"),
                            eq("html"),
                            eq("<html>content</html>".getBytes(StandardCharsets.UTF_8)));
        }

        @Test
        @DisplayName("should handle null score in attachment name")
        void shouldHandleNullScore() {
            final EvaluationReportData data = createTestData(null);
            when(templateEngine.renderHtml(data)).thenReturn("<html>content</html>");

            writer.writeHtmlAttachment(data);

            verify(lifecycle)
                    .addAttachment(eq("TestMetric [N/A] Report"), eq("text/html"), eq("html"), any(byte[].class));
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
                            eq("TestMetric [72.00%] MD"),
                            eq("text/markdown"),
                            eq("md"),
                            eq("# Report".getBytes(StandardCharsets.UTF_8)));
        }

        @Test
        @DisplayName("should handle null score in attachment name")
        void shouldHandleNullScore() {
            final EvaluationReportData data = createTestData(null);
            when(templateEngine.renderMarkdown(data)).thenReturn("# Report");

            writer.writeMarkdownAttachment(data);

            verify(lifecycle)
                    .addAttachment(eq("TestMetric [N/A] MD"), eq("text/markdown"), eq("md"), any(byte[].class));
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

    private EvaluationReportData createTestData(final Double score) {
        return EvaluationReportData.builder()
                .metricName("TestMetric")
                .aggregatedScore(score)
                .build();
    }
}
