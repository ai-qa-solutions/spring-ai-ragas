package ai.qa.solutions.allure.listener;

import ai.qa.solutions.allure.model.EvaluationReportData;
import ai.qa.solutions.allure.template.FreemarkerTemplateEngine;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Attachment;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

/**
 * Writer for creating Allure attachments from evaluation report data.
 * <p>
 * Uses the Allure lifecycle API to add HTML and Markdown attachments
 * to the current test context.
 */
@Slf4j
public class AllureAttachmentWriter {

    private static final String HTML_CONTENT_TYPE = "text/html";
    private static final String HTML_EXTENSION = "html";
    private static final String MARKDOWN_CONTENT_TYPE = "text/markdown";
    private static final String MARKDOWN_EXTENSION = "md";

    private final AllureLifecycle lifecycle;
    private final FreemarkerTemplateEngine templateEngine;

    /**
     * Creates an AllureAttachmentWriter with the given dependencies.
     *
     * @param lifecycle the Allure lifecycle for adding attachments
     * @param templateEngine the template engine for rendering reports
     */
    public AllureAttachmentWriter(final AllureLifecycle lifecycle, final FreemarkerTemplateEngine templateEngine) {
        this.lifecycle = lifecycle;
        this.templateEngine = templateEngine;
    }

    /**
     * Writes an HTML attachment for the evaluation report.
     *
     * @param data the evaluation report data
     */
    public void writeHtmlAttachment(final EvaluationReportData data) {
        try {
            final String html = templateEngine.renderHtml(data);
            final String name = buildAttachmentName(data, "Report");
            lifecycle.addAttachment(name, HTML_CONTENT_TYPE, HTML_EXTENSION, html.getBytes(StandardCharsets.UTF_8));
            log.debug("Added HTML attachment '{}' for metric '{}'", name, data.getMetricName());
        } catch (final Exception e) {
            log.error("Failed to write HTML attachment for metric '{}'", data.getMetricName(), e);
        }
    }

    /**
     * Writes a Markdown attachment for the evaluation report.
     *
     * @param data the evaluation report data
     */
    public void writeMarkdownAttachment(final EvaluationReportData data) {
        try {
            final String markdown = templateEngine.renderMarkdown(data);
            final String name = buildAttachmentName(data, "MD");
            lifecycle.addAttachment(
                    name, MARKDOWN_CONTENT_TYPE, MARKDOWN_EXTENSION, markdown.getBytes(StandardCharsets.UTF_8));
            log.debug("Added Markdown attachment '{}' for metric '{}'", name, data.getMetricName());
        } catch (final Exception e) {
            log.error("Failed to write Markdown attachment for metric '{}'", data.getMetricName(), e);
        }
    }

    /**
     * Writes an HTML attachment for the evaluation report to a specific step by UUID.
     * <p>
     * This method bypasses ThreadLocal context and directly attaches to the specified step,
     * making it safe to use from async threads.
     *
     * @param stepUuid the UUID of the step to attach to
     * @param data the evaluation report data
     */
    public void writeHtmlAttachmentToStep(final String stepUuid, final EvaluationReportData data) {
        try {
            final String html = templateEngine.renderHtml(data);
            final String name = buildAttachmentName(data, "Report");
            addAttachmentToStep(
                    stepUuid, name, HTML_CONTENT_TYPE, HTML_EXTENSION, html.getBytes(StandardCharsets.UTF_8));
            log.debug("Added HTML attachment '{}' to step '{}' for metric '{}'", name, stepUuid, data.getMetricName());
        } catch (final Exception e) {
            log.error("Failed to write HTML attachment for metric '{}'", data.getMetricName(), e);
        }
    }

    /**
     * Writes a Markdown attachment for the evaluation report to a specific step by UUID.
     * <p>
     * This method bypasses ThreadLocal context and directly attaches to the specified step,
     * making it safe to use from async threads.
     *
     * @param stepUuid the UUID of the step to attach to
     * @param data the evaluation report data
     */
    public void writeMarkdownAttachmentToStep(final String stepUuid, final EvaluationReportData data) {
        try {
            final String markdown = templateEngine.renderMarkdown(data);
            final String name = buildAttachmentName(data, "MD");
            addAttachmentToStep(
                    stepUuid,
                    name,
                    MARKDOWN_CONTENT_TYPE,
                    MARKDOWN_EXTENSION,
                    markdown.getBytes(StandardCharsets.UTF_8));
            log.debug(
                    "Added Markdown attachment '{}' to step '{}' for metric '{}'",
                    name,
                    stepUuid,
                    data.getMetricName());
        } catch (final Exception e) {
            log.error("Failed to write Markdown attachment for metric '{}'", data.getMetricName(), e);
        }
    }

    /**
     * Adds an attachment to a specific step by UUID, bypassing ThreadLocal context.
     *
     * @param stepUuid the UUID of the step to attach to
     * @param name the attachment name
     * @param contentType the MIME content type
     * @param extension the file extension
     * @param content the attachment content
     */
    private void addAttachmentToStep(
            final String stepUuid,
            final String name,
            final String contentType,
            final String extension,
            final byte[] content) {
        final String source = UUID.randomUUID() + "-attachment." + extension;

        final Attachment attachment =
                new Attachment().setName(name).setSource(source).setType(contentType);

        lifecycle.updateStep(stepUuid, step -> step.getAttachments().add(attachment));
        lifecycle.writeAttachment(source, new ByteArrayInputStream(content));
    }

    private String buildAttachmentName(final EvaluationReportData data, final String suffix) {
        final String score = data.getAggregatedScore() != null
                ? String.format(Locale.US, "%.2f", data.getAggregatedScore() * 100) + "%"
                : "N/A";
        return String.format("%s [%s] %s", data.getMetricName(), score, suffix);
    }
}
