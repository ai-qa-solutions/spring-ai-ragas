package ai.qa.solutions.allure.listener;

import ai.qa.solutions.allure.config.AllureRagasProperties;
import ai.qa.solutions.allure.methodology.MethodologyLoader;
import ai.qa.solutions.allure.model.*;
import ai.qa.solutions.allure.template.FreemarkerTemplateEngine;
import ai.qa.solutions.allure.util.AllureJsonUtils;
import ai.qa.solutions.execution.listener.MetricExecutionListener;
import ai.qa.solutions.execution.listener.dto.*;
import ai.qa.solutions.metric.explanation.ScoreExplanation;
import ai.qa.solutions.metric.explanation.ScoreExplanationFactory;
import ai.qa.solutions.sample.Sample;
import ai.qa.solutions.sample.message.AIMessage;
import ai.qa.solutions.sample.message.BaseMessage;
import ai.qa.solutions.sample.message.HumanMessage;
import ai.qa.solutions.sample.message.ToolCall;
import ai.qa.solutions.sample.message.ToolMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

/**
 * Allure integration listener for metric evaluation.
 * <p>
 * This listener collects data during metric evaluation and generates
 * HTML and Markdown attachments for Allure reports.
 * <p>
 * <b>Important:</b> This listener is stateful and captures context in
 * {@link #beforeMetricEvaluation}. The {@link #forEvaluation()} method returns
 * a new instance for each evaluation to ensure thread safety in parallel execution.
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * // Automatic (via Spring Boot autoconfiguration)
 * spring.ai.ragas.allure.enabled=true
 *
 * // Manual
 * AllureMetricExecutionListener listener = new AllureMetricExecutionListener(...);
 * metric.addListener(listener);
 * }</pre>
 */
@Slf4j
public class AllureMetricExecutionListener implements MetricExecutionListener {

    private final AllureRagasProperties properties;
    private final FreemarkerTemplateEngine templateEngine;
    private final AllureAttachmentWriter attachmentWriter;
    private final MethodologyLoader methodologyLoader;
    private final ScoreExplanationFactory explanationFactory;
    private final AllureLifecycle lifecycle;

    // Mutable state (reset per evaluation via forEvaluation())
    private MetricEvaluationContext evaluationContext;
    private Instant startTime;

    // Allure context captured from main thread for async execution
    private String parentUuid;
    private String metricStepUuid;

    /**
     * Creates an AllureMetricExecutionListener with the given dependencies.
     *
     * @param properties the configuration properties
     * @param templateEngine the Freemarker template engine
     * @param attachmentWriter the Allure attachment writer
     * @param methodologyLoader the methodology documentation loader
     */
    public AllureMetricExecutionListener(
            final AllureRagasProperties properties,
            final FreemarkerTemplateEngine templateEngine,
            final AllureAttachmentWriter attachmentWriter,
            final MethodologyLoader methodologyLoader) {
        this(properties, templateEngine, attachmentWriter, methodologyLoader, Allure.getLifecycle());
    }

    /**
     * Creates an AllureMetricExecutionListener with explicit lifecycle (for testing).
     *
     * @param properties the configuration properties
     * @param templateEngine the Freemarker template engine
     * @param attachmentWriter the Allure attachment writer
     * @param methodologyLoader the methodology documentation loader
     * @param lifecycle the Allure lifecycle instance
     */
    AllureMetricExecutionListener(
            final AllureRagasProperties properties,
            final FreemarkerTemplateEngine templateEngine,
            final AllureAttachmentWriter attachmentWriter,
            final MethodologyLoader methodologyLoader,
            final AllureLifecycle lifecycle) {
        this.properties = properties;
        this.templateEngine = templateEngine;
        this.attachmentWriter = attachmentWriter;
        this.methodologyLoader = methodologyLoader;
        this.explanationFactory = new ScoreExplanationFactory();
        this.lifecycle = lifecycle;
    }

    @Override
    public void beforeMetricEvaluation(final MetricEvaluationContext context) {
        this.startTime = Instant.now();
        this.evaluationContext = context;

        // Capture parent context from main thread (before async execution)
        this.parentUuid = lifecycle.getCurrentTestCaseOrStep().orElse(null);
        this.metricStepUuid = UUID.randomUUID().toString();

        // Create Allure step for this metric evaluation (in main thread where context exists)
        if (parentUuid != null) {
            final StepResult stepResult =
                    new StepResult().setName(context.getMetricName()).setStatus(Status.PASSED);
            lifecycle.startStep(parentUuid, metricStepUuid, stepResult);
            log.debug(
                    "Allure listener: Created step '{}' under parent '{}' for metric '{}'",
                    metricStepUuid,
                    parentUuid,
                    context.getMetricName());
        } else {
            log.warn(
                    "Allure listener: No parent context found for metric '{}', attachments may not be linked",
                    context.getMetricName());
        }

        log.debug(
                "Allure listener: Starting evaluation of metric '{}' with {} models",
                context.getMetricName(),
                context.getModelIds().size());
    }

    @Override
    public void afterMetricEvaluation(final MetricEvaluationResult result) {
        try {
            final EvaluationReportData reportData = buildReportData(result);

            if (metricStepUuid != null) {
                // Use UUID-based methods to bypass ThreadLocal (safe from async threads)
                attachmentWriter.writeHtmlAttachmentToStep(metricStepUuid, reportData);
                attachmentWriter.writeMarkdownAttachmentToStep(metricStepUuid, reportData);

                // Update step status and stop it
                final Status status = result.getAggregatedScore() != null ? Status.PASSED : Status.BROKEN;
                lifecycle.updateStep(metricStepUuid, step -> step.setStatus(status));
                lifecycle.stopStep(metricStepUuid);

                log.debug(
                        "Allure listener: Generated attachments and closed step '{}' for metric '{}' with score {}",
                        metricStepUuid,
                        result.getMetricName(),
                        result.getAggregatedScore());
            } else {
                // Fallback to ThreadLocal-based methods (may not work from async threads)
                attachmentWriter.writeHtmlAttachment(reportData);
                attachmentWriter.writeMarkdownAttachment(reportData);
                log.debug(
                        "Allure listener: Generated attachments for metric '{}' with score {} (no step UUID)",
                        result.getMetricName(),
                        result.getAggregatedScore());
            }
        } catch (final Exception e) {
            log.error("Allure listener: Failed to generate attachments for metric '{}'", result.getMetricName(), e);
            // Try to stop step even on error
            if (metricStepUuid != null) {
                try {
                    lifecycle.updateStep(metricStepUuid, step -> step.setStatus(Status.BROKEN));
                    lifecycle.stopStep(metricStepUuid);
                } catch (final Exception stopError) {
                    log.error("Failed to stop step '{}'", metricStepUuid, stopError);
                }
            }
        }
    }

    @Override
    public MetricExecutionListener forEvaluation() {
        // CRITICAL: Return new instance for thread safety
        return new AllureMetricExecutionListener(
                properties, templateEngine, attachmentWriter, methodologyLoader, lifecycle);
    }

    @Override
    public int getOrder() {
        return 100; // Run after logging listener
    }

    private EvaluationReportData buildReportData(final MetricEvaluationResult result) {
        final Instant endTime = Instant.now();
        final Duration totalDuration =
                result.getTotalDuration() != null ? result.getTotalDuration() : Duration.between(startTime, endTime);

        final String metricName = result.getMetricName();
        final String methodologyHtml = methodologyLoader.loadMethodologyHtml(metricName);
        final String methodologyMarkdown = methodologyLoader.loadMethodologyMarkdown(metricName);

        // Use result fields, falling back to evaluationContext where needed
        final Sample sample = result.getSample() != null ? result.getSample() : evaluationContext.getSample();
        final Object config = result.getConfig() != null ? result.getConfig() : evaluationContext.getConfig();
        final List<String> modelIds =
                result.getModelIds() != null && !result.getModelIds().isEmpty()
                        ? result.getModelIds()
                        : evaluationContext.getModelIds();

        // Convert StepResults to StepExecutionData for template compatibility
        final List<StepExecutionData> stepData = result.getSteps() != null
                ? result.getSteps().stream().map(StepExecutionData::from).toList()
                : List.of();

        // Build exclusion data from result
        final List<ModelExclusionData> exclusionData = result.getExclusions() != null
                ? result.getExclusions().stream().map(ModelExclusionData::from).toList()
                : List.of();

        // Build timeline from result steps
        final List<ChartData.TimelineEntry> timelineEntries = buildTimelineFromSteps(result.getSteps());

        final ChartData chartData = buildChartData(result, timelineEntries);

        // Use pre-built explanation if available (from evaluate() API), otherwise build via factory
        final Optional<ScoreExplanation> explanationOpt;
        if (result.getExplanation() instanceof ScoreExplanation preBuilt) {
            explanationOpt = Optional.of(preBuilt);
        } else {
            explanationOpt = explanationFactory.create(result, properties.getLanguage());
        }

        // Format conversation messages for agent metrics
        final List<FormattedMessage> conversationMessages = formatConversationMessages(sample.getUserInputMessages());

        return EvaluationReportData.builder()
                .metricName(metricName)
                .userInput(sample.getUserInput())
                .response(sample.getResponse())
                .reference(sample.getReference())
                .retrievedContexts(sample.getRetrievedContexts() != null ? sample.getRetrievedContexts() : List.of())
                .conversationMessages(conversationMessages)
                .config(config)
                .configJson(EvaluationReportData.configToJson(config))
                .startTime(startTime)
                .endTime(endTime)
                .totalDuration(totalDuration)
                .modelIds(modelIds)
                .embeddingModelIds(
                        evaluationContext.getEmbeddingModelIds() != null
                                ? evaluationContext.getEmbeddingModelIds()
                                : List.of())
                .excludedModels(result.getExcludedModels() != null ? result.getExcludedModels() : List.of())
                .aggregatedScore(result.getAggregatedScore())
                .modelScores(result.getModelScores() != null ? result.getModelScores() : Map.of())
                .steps(new ArrayList<>(stepData))
                .exclusions(exclusionData)
                .methodologyHtml(methodologyHtml)
                .methodologyMarkdown(methodologyMarkdown)
                .chartData(chartData)
                .scoreExplanation(explanationOpt.orElse(null))
                .language(properties.getLanguage())
                .build();
    }

    private List<FormattedMessage> formatConversationMessages(final List<BaseMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        final List<FormattedMessage> formatted = new ArrayList<>();
        for (final BaseMessage message : messages) {
            if (message instanceof HumanMessage h) {
                formatted.add(FormattedMessage.builder()
                        .type("human")
                        .content(h.content())
                        .build());
            } else if (message instanceof AIMessage a) {
                formatted.add(FormattedMessage.builder()
                        .type("ai")
                        .content(a.content())
                        .toolCalls(formatToolCalls(a.toolCalls()))
                        .build());
            } else if (message instanceof ToolMessage t) {
                formatted.add(FormattedMessage.builder()
                        .type("tool")
                        .content(t.content())
                        .build());
            }
        }
        return formatted;
    }

    private List<FormattedMessage.ToolCallData> formatToolCalls(final List<ToolCall> toolCalls) {
        if (toolCalls == null || toolCalls.isEmpty()) {
            return List.of();
        }

        return toolCalls.stream()
                .map(tc -> FormattedMessage.ToolCallData.builder()
                        .name(tc.name())
                        .arguments(formatArguments(tc.arguments()))
                        .build())
                .toList();
    }

    private String formatArguments(final Map<String, Object> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return "{}";
        }
        try {
            return AllureJsonUtils.writeValueAsString(arguments);
        } catch (final JsonProcessingException e) {
            return arguments.toString();
        }
    }

    private List<ChartData.TimelineEntry> buildTimelineFromSteps(final List<StepResults> steps) {
        if (steps == null || steps.isEmpty()) {
            return List.of();
        }

        final List<ChartData.TimelineEntry> entries = new ArrayList<>();
        long currentStepStartOffset = 0;

        for (final StepResults stepResults : steps) {
            final String stepType = stepResults.getStepType() != null
                    ? stepResults.getStepType().name()
                    : "LLM";

            // Track iteration count per model to add "(iter N)" suffix for strictness iterations
            final Map<String, Integer> modelIterationCount = new HashMap<>();

            // Add entries for each model result
            for (final var modelResult : stepResults.getResults()) {
                final String baseModelId = modelResult.modelId();
                final int iteration = modelIterationCount.merge(baseModelId, 1, Integer::sum);
                final boolean hasMultiple = hasMultipleIterations(stepResults, baseModelId);

                // Add iteration suffix if this model appears multiple times (strictness > 1)
                final String displayModelId =
                        iteration > 1 || hasMultiple ? baseModelId + " (iter " + iteration + ")" : baseModelId;

                entries.add(ChartData.TimelineEntry.builder()
                        .modelId(displayModelId)
                        .stepName(stepResults.getStepName())
                        .stepType(stepType)
                        .startOffsetMs(currentStepStartOffset)
                        .durationMs(
                                modelResult.duration() != null
                                        ? modelResult.duration().toMillis()
                                        : 0)
                        .success(modelResult.isSuccess())
                        .build());
            }

            // Advance offset by this step's total duration
            currentStepStartOffset += stepResults.getTotalDuration().toMillis();
        }

        return entries;
    }

    private boolean hasMultipleIterations(final StepResults results, final String modelId) {
        return results.getResults().stream()
                        .filter(r -> r.modelId().equals(modelId))
                        .count()
                > 1;
    }

    private ChartData buildChartData(
            final MetricEvaluationResult result, final List<ChartData.TimelineEntry> timelineEntries) {
        final List<ChartData.ScoreEntry> scoreEntries = buildScoreEntries(result);
        final long maxDuration = timelineEntries.stream()
                .mapToLong(e -> e.getStartOffsetMs() + e.getDurationMs())
                .max()
                .orElse(0);

        return ChartData.builder()
                .scoreEntries(scoreEntries)
                .timelineEntries(new ArrayList<>(timelineEntries))
                .maxDurationMs(maxDuration)
                .timeScale(maxDuration > 0 ? 600.0 / maxDuration : 1.0)
                // Heatmap data will be populated by specific metrics that have verdict matrices
                .heatmapRowLabels(List.of())
                .heatmapColLabels(List.of())
                .heatmapValues(List.of())
                .build();
    }

    private List<ChartData.ScoreEntry> buildScoreEntries(final MetricEvaluationResult result) {
        final List<ChartData.ScoreEntry> entries = new ArrayList<>();

        // Add successful model scores
        if (result.getModelScores() != null) {
            result.getModelScores()
                    .forEach((modelId, score) -> entries.add(ChartData.ScoreEntry.builder()
                            .modelId(modelId)
                            .score(score)
                            .excluded(false)
                            .build()));
        }

        // Add excluded models
        if (result.getExcludedModels() != null) {
            result.getExcludedModels()
                    .forEach(modelId -> entries.add(ChartData.ScoreEntry.builder()
                            .modelId(modelId)
                            .score(0.0)
                            .excluded(true)
                            .build()));
        }

        return entries;
    }
}
