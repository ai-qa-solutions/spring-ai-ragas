package ai.qa.solutions.allure.template;

import ai.qa.solutions.allure.i18n.ReportMessages;
import ai.qa.solutions.allure.model.EvaluationReportData;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Freemarker-based template engine for rendering Allure report attachments.
 * <p>
 * Renders HTML and Markdown reports from evaluation data using Freemarker templates.
 * Templates are loaded from the classpath at {@code /ai/qa/solutions/allure/templates/}.
 */
@Slf4j
public class FreemarkerTemplateEngine {

    private static final String TEMPLATE_BASE_PATH = "/ai/qa/solutions/allure/templates";
    private static final String HTML_TEMPLATE = "report-html.ftl";
    private static final String MARKDOWN_TEMPLATE = "report-markdown.ftl";

    private final Configuration configuration;

    /**
     * Creates a new FreemarkerTemplateEngine with default configuration.
     */
    public FreemarkerTemplateEngine() {
        this.configuration = createConfiguration();
    }

    /**
     * Renders the HTML report for the given evaluation data.
     *
     * @param data the evaluation report data
     * @return rendered HTML content
     * @throws TemplateRenderException if rendering fails
     */
    public String renderHtml(final EvaluationReportData data) {
        return render(HTML_TEMPLATE, data);
    }

    /**
     * Renders the Markdown report for the given evaluation data.
     *
     * @param data the evaluation report data
     * @return rendered Markdown content
     * @throws TemplateRenderException if rendering fails
     */
    public String renderMarkdown(final EvaluationReportData data) {
        return render(MARKDOWN_TEMPLATE, data);
    }

    /**
     * Renders a template with the given data.
     *
     * @param templateName the template file name
     * @param data the evaluation report data
     * @return rendered content
     * @throws TemplateRenderException if rendering fails
     */
    public String render(final String templateName, final EvaluationReportData data) {
        try {
            final Template template = configuration.getTemplate(templateName);
            final Map<String, Object> model = createTemplateModel(data);

            try (final StringWriter writer = new StringWriter()) {
                template.process(model, writer);
                return writer.toString();
            }
        } catch (final IOException | TemplateException e) {
            log.error("Failed to render template '{}' for metric '{}'", templateName, data.getMetricName(), e);
            throw new TemplateRenderException("Failed to render template: " + templateName, e);
        }
    }

    private Map<String, Object> createTemplateModel(final EvaluationReportData data) {
        final Map<String, Object> model = new HashMap<>();
        model.put("data", data);
        model.put("metricName", data.getMetricName());
        model.put("aggregatedScore", data.getAggregatedScore());
        model.put("totalDuration", data.getTotalDuration());
        model.put("totalDurationMs", data.getTotalDurationMs());
        model.put("startTime", data.getStartTime());
        model.put("endTime", data.getEndTime());
        model.put("userInput", data.getUserInput());
        model.put("response", data.getResponse());
        model.put("reference", data.getReference());
        model.put("retrievedContexts", data.getRetrievedContexts());
        model.put("modelIds", data.getModelIds());
        model.put("embeddingModelIds", data.getEmbeddingModelIds());
        model.put("excludedModels", data.getExcludedModels());
        model.put("modelScores", data.getModelScores());
        model.put("steps", data.getSteps());
        model.put("exclusions", data.getExclusions());
        model.put("methodologyHtml", data.getMethodologyHtml());
        model.put("methodologyMarkdown", data.getMethodologyMarkdown());
        model.put("chartData", data.getChartData());
        model.put("configJson", data.getConfigJson());
        model.put("language", data.getLanguage());
        // Add localized messages
        model.put("i18n", ReportMessages.forLanguage(data.getLanguage()));
        return model;
    }

    private Configuration createConfiguration() {
        final Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setClassForTemplateLoading(getClass(), TEMPLATE_BASE_PATH);
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
        cfg.setWrapUncheckedExceptions(true);
        cfg.setFallbackOnNullLoopVariable(false);
        // Use computer format for numbers (no grouping separators)
        cfg.setNumberFormat("computer");
        return cfg;
    }

    /**
     * Exception thrown when template rendering fails.
     */
    public static class TemplateRenderException extends RuntimeException {
        public TemplateRenderException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
