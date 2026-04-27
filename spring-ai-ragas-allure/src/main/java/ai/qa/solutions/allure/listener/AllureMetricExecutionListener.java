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
 * // Manual via builder (recommended)
 * AllureMetricExecutionListener listener = AllureMetricExecutionListener.builder()
 *         .properties(properties)
 *         .templateEngine(templateEngine)
 *         .attachmentWriter(attachmentWriter)
 *         .methodologyLoader(methodologyLoader)
 *         .withStep(false)                  // optional, default true
 *         .withHtmlAttachment(true)         // optional, default true
 *         .withMarkdownAttachment(false)    // optional, default true
 *         .withSummary(true)                // optional, default true
 *         .withExplanation(true)            // optional, default true
 *         .withMethodology(true)            // optional, default FALSE (see migration note below)
 *         .withExecutionLog(true)           // optional, default true
 *         .withExcludedModels(true)         // optional, default true
 *         .build();
 * metric.addListener(listener);
 * }</pre>
 *
 * <h3>{@code withStep} flag</h3>
 * <p>
 * Controls whether the listener wraps each metric evaluation in its own Allure
 * step (named after the metric). Default is {@code true}.
 * <ul>
 *   <li>{@code withStep=true} (default): the listener calls
 *       {@link AllureLifecycle#startStep(String, String, StepResult) startStep} /
 *       {@link AllureLifecycle#stopStep(String) stopStep} around the HTML/Markdown
 *       attachments. If no Allure parent context is found, the listener falls back
 *       to {@link io.qameta.allure.Allure}'s ThreadLocal-based attachment methods.</li>
 *   <li>{@code withStep=false}: the listener skips {@code startStep}/{@code stopStep}
 *       and writes attachments directly to the captured parent UUID. If no Allure
 *       parent context is found, the listener logs a warning and skips the
 *       attachments entirely (no ThreadLocal fallback).</li>
 * </ul>
 * Use {@code withStep=false} when the host test framework already wraps each
 * metric call in its own outer Allure step and the inner step from this listener
 * would be a redundant nesting level.
 *
 * <h3>Attachment and section toggles</h3>
 * <p>
 * The builder exposes seven additional flags that control which attachments are
 * written and which sections appear inside each rendered report. Two of them
 * gate the Allure attachments themselves ({@code withHtmlAttachment},
 * {@code withMarkdownAttachment}); the other five toggle individual sections
 * within the HTML and Markdown templates ({@code withSummary},
 * {@code withExplanation}, {@code withMethodology}, {@code withExecutionLog},
 * {@code withExcludedModels}). All defaults are {@code true} <b>except
 * {@code withMethodology}, which defaults to {@code false}</b>.
 * <p>
 * If both attachment toggles are {@code false}, no Allure step is created even
 * when {@code withStep=true} (the listener performs an early return).
 *
 * <h3>Breaking change: methodology section default</h3>
 * <p>
 * Earlier versions of this listener always rendered the methodology section in
 * both attachments. Starting with the {@link RenderConfig}-aware build, the
 * methodology section is omitted by default to keep attachments compact.
 * <p>
 * Migration: callers that need the methodology section must opt in explicitly
 * via {@code .withMethodology(true)} on the builder.
 */
@Slf4j
public class AllureMetricExecutionListener implements MetricExecutionListener {

    private final AllureRagasProperties properties;
    private final FreemarkerTemplateEngine templateEngine;
    private final AllureAttachmentWriter attachmentWriter;
    private final MethodologyLoader methodologyLoader;
    private final ScoreExplanationFactory explanationFactory;
    private final AllureLifecycle lifecycle;
    private final boolean withStep;
    private final RenderConfig renderConfig;

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
     * @deprecated since 0.4.0. Use {@link #builder()} instead. This constructor is retained
     *     for backward compatibility and delegates to the builder defaults
     *     ({@code lifecycle = Allure.getLifecycle()}, {@code withStep = true}).
     */
    @Deprecated(since = "0.4.0", forRemoval = false)
    public AllureMetricExecutionListener(
            final AllureRagasProperties properties,
            final FreemarkerTemplateEngine templateEngine,
            final AllureAttachmentWriter attachmentWriter,
            final MethodologyLoader methodologyLoader) {
        this(properties, templateEngine, attachmentWriter, methodologyLoader, Allure.getLifecycle(), true);
    }

    /**
     * Creates an AllureMetricExecutionListener with explicit lifecycle (for testing).
     * Delegates to the 6-arg constructor with {@code withStep = true} (default behaviour).
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
        this(properties, templateEngine, attachmentWriter, methodologyLoader, lifecycle, true);
    }

    /**
     * Creates an AllureMetricExecutionListener with explicit lifecycle and step toggle.
     * Package-private — production code should use {@link #builder()}.
     * Delegates to the 7-arg constructor with {@link RenderConfig#defaults()}.
     *
     * @param properties the configuration properties
     * @param templateEngine the Freemarker template engine
     * @param attachmentWriter the Allure attachment writer
     * @param methodologyLoader the methodology documentation loader
     * @param lifecycle the Allure lifecycle instance
     * @param withStep whether to wrap metric evaluation in an Allure step (see class javadoc
     *     for the no-parent-context asymmetry between {@code withStep=true} and {@code false})
     */
    AllureMetricExecutionListener(
            final AllureRagasProperties properties,
            final FreemarkerTemplateEngine templateEngine,
            final AllureAttachmentWriter attachmentWriter,
            final MethodologyLoader methodologyLoader,
            final AllureLifecycle lifecycle,
            final boolean withStep) {
        this(
                properties,
                templateEngine,
                attachmentWriter,
                methodologyLoader,
                lifecycle,
                withStep,
                RenderConfig.defaults());
    }

    /**
     * Creates an AllureMetricExecutionListener with explicit lifecycle, step toggle, and
     * render configuration. Package-private — production code should use {@link #builder()}.
     *
     * @param properties the configuration properties
     * @param templateEngine the Freemarker template engine
     * @param attachmentWriter the Allure attachment writer
     * @param methodologyLoader the methodology documentation loader
     * @param lifecycle the Allure lifecycle instance
     * @param withStep whether to wrap metric evaluation in an Allure step (see class javadoc
     *     for the no-parent-context asymmetry between {@code withStep=true} and {@code false})
     * @param renderConfig the attachment/section render configuration (see {@link RenderConfig})
     */
    AllureMetricExecutionListener(
            final AllureRagasProperties properties,
            final FreemarkerTemplateEngine templateEngine,
            final AllureAttachmentWriter attachmentWriter,
            final MethodologyLoader methodologyLoader,
            final AllureLifecycle lifecycle,
            final boolean withStep,
            final RenderConfig renderConfig) {
        this.properties = properties;
        this.templateEngine = templateEngine;
        this.attachmentWriter = attachmentWriter;
        this.methodologyLoader = methodologyLoader;
        this.explanationFactory = new ScoreExplanationFactory();
        this.lifecycle = lifecycle;
        this.withStep = withStep;
        this.renderConfig = renderConfig;
    }

    /**
     * Creates a new {@link Builder} for configuring an {@code AllureMetricExecutionListener}.
     *
     * @return a fresh builder instance with default {@code lifecycle = Allure.getLifecycle()}
     *     and {@code withStep = true}
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void beforeMetricEvaluation(final MetricEvaluationContext context) {
        this.startTime = Instant.now();
        this.evaluationContext = context;

        // Capture parent context from main thread (before async execution)
        // Do NOT call startStep here — it would push to main thread's ThreadLocal
        // and stopStep would remove from storage, breaking UUID-based operations
        this.parentUuid = lifecycle.getCurrentTestCaseOrStep().orElse(null);
        // metricStepUuid is only relevant when withStep=true; leave null otherwise so the
        // catch-block cleanup (lifecycle.updateStep / stopStep) naturally no-ops via the
        // existing `if (metricStepUuid != null)` guard.
        this.metricStepUuid = withStep ? UUID.randomUUID().toString() : null;

        if (parentUuid == null) {
            log.warn(
                    "Allure listener: No parent context found for metric '{}', attachments may not be linked",
                    context.getMetricName());
        }

        log.debug(
                "Allure listener: Starting evaluation of metric '{}' with {} models, parent='{}'",
                context.getMetricName(),
                context.getModelIds().size(),
                parentUuid);
    }

    @Override
    public void afterMetricEvaluation(final MetricEvaluationResult result) {
        if (!renderConfig.anyAttachmentEnabled()) {
            log.warn(
                    "Allure listener: both HTML and Markdown attachments disabled for metric '{}', skipping",
                    result.getMetricName());
            return;
        }
        if (!renderConfig.anySectionEnabled()) {
            log.warn(
                    "Allure listener: all report sections disabled for metric '{}', skipping attachments",
                    result.getMetricName());
            return;
        }
        try {
            if (withStep) {
                final EvaluationReportData reportData = buildReportData(result);

                if (parentUuid != null && metricStepUuid != null) {
                    // Create Allure step on THIS thread (async) — avoids main thread ThreadLocal corruption
                    final Status status = result.getAggregatedScore() != null ? Status.PASSED : Status.BROKEN;
                    final StepResult stepResult =
                            new StepResult().setName(result.getMetricName()).setStatus(status);
                    lifecycle.startStep(parentUuid, metricStepUuid, stepResult);

                    // Write attachments — step is in storage, UUID-based methods work
                    if (renderConfig.htmlAttachment()) {
                        attachmentWriter.writeHtmlAttachmentToStep(metricStepUuid, reportData, renderConfig);
                    }
                    if (renderConfig.markdownAttachment()) {
                        attachmentWriter.writeMarkdownAttachmentToStep(metricStepUuid, reportData, renderConfig);
                    }

                    // Finalize step — sets stop time, removes from storage, pops from THIS thread's ThreadLocal
                    lifecycle.stopStep(metricStepUuid);

                    log.debug(
                            "Allure listener: Created step '{}' with attachments for metric '{}', score {}",
                            metricStepUuid,
                            result.getMetricName(),
                            result.getAggregatedScore());
                } else {
                    // Fallback to ThreadLocal-based methods
                    if (renderConfig.htmlAttachment()) {
                        attachmentWriter.writeHtmlAttachment(reportData, renderConfig);
                    }
                    if (renderConfig.markdownAttachment()) {
                        attachmentWriter.writeMarkdownAttachment(reportData, renderConfig);
                    }
                    log.debug(
                            "Allure listener: Generated attachments for metric '{}' with score {} (no parent context)",
                            result.getMetricName(),
                            result.getAggregatedScore());
                }
            } else {
                // withStep == false — write attachments directly to parent UUID without wrapping step
                if (parentUuid == null) {
                    log.warn(
                            "Allure listener: withStep=false but no parent context for metric '{}', skipping attachments",
                            result.getMetricName());
                    return;
                }

                final EvaluationReportData reportData = buildReportData(result);
                if (renderConfig.htmlAttachment()) {
                    attachmentWriter.writeHtmlAttachmentToStep(parentUuid, reportData, renderConfig);
                }
                if (renderConfig.markdownAttachment()) {
                    attachmentWriter.writeMarkdownAttachmentToStep(parentUuid, reportData, renderConfig);
                }

                log.debug(
                        "Allure listener: Attached HTML/Markdown reports directly to parent '{}' for metric '{}', score {} (withStep=false)",
                        parentUuid,
                        result.getMetricName(),
                        result.getAggregatedScore());
            }
        } catch (final Exception e) {
            log.error("Allure listener: Failed to generate attachments for metric '{}'", result.getMetricName(), e);
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
                properties, templateEngine, attachmentWriter, methodologyLoader, lifecycle, withStep, renderConfig);
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

    /**
     * Fluent builder for {@link AllureMetricExecutionListener}.
     * <p>
     * Required fields: {@code properties}, {@code templateEngine},
     * {@code attachmentWriter}, {@code methodologyLoader}.
     * <p>
     * Optional fields: {@code lifecycle} (default {@link Allure#getLifecycle()}),
     * {@code withStep} (default {@code true}). See class-level javadoc on
     * {@link AllureMetricExecutionListener} for the no-parent-context asymmetry
     * between {@code withStep=true} and {@code withStep=false}.
     */
    public static class Builder {
        private AllureRagasProperties properties;
        private FreemarkerTemplateEngine templateEngine;
        private AllureAttachmentWriter attachmentWriter;
        private MethodologyLoader methodologyLoader;
        private AllureLifecycle lifecycle = Allure.getLifecycle();
        private boolean withStep = true;

        private boolean withHtmlAttachment = true;
        private boolean withMarkdownAttachment = true;
        private boolean withSummary = true;
        private boolean withExplanation = true;
        private boolean withMethodology = false;
        private boolean withExecutionLog = true;
        private boolean withExcludedModels = true;

        /**
         * Sets the Ragas Allure configuration properties (required).
         *
         * @param properties the configuration properties
         * @return this builder
         */
        public Builder properties(final AllureRagasProperties properties) {
            this.properties = properties;
            return this;
        }

        /**
         * Sets the Freemarker template engine (required).
         *
         * @param templateEngine the template engine
         * @return this builder
         */
        public Builder templateEngine(final FreemarkerTemplateEngine templateEngine) {
            this.templateEngine = templateEngine;
            return this;
        }

        /**
         * Sets the Allure attachment writer (required).
         *
         * @param attachmentWriter the attachment writer
         * @return this builder
         */
        public Builder attachmentWriter(final AllureAttachmentWriter attachmentWriter) {
            this.attachmentWriter = attachmentWriter;
            return this;
        }

        /**
         * Sets the methodology documentation loader (required).
         *
         * @param methodologyLoader the methodology loader
         * @return this builder
         */
        public Builder methodologyLoader(final MethodologyLoader methodologyLoader) {
            this.methodologyLoader = methodologyLoader;
            return this;
        }

        /**
         * Sets the Allure lifecycle (optional, default {@link Allure#getLifecycle()}).
         *
         * @param lifecycle the lifecycle instance
         * @return this builder
         */
        public Builder lifecycle(final AllureLifecycle lifecycle) {
            this.lifecycle = lifecycle;
            return this;
        }

        /**
         * Sets whether the listener wraps each metric evaluation in its own Allure step
         * (optional, default {@code true}).
         * <p>
         * When {@code true} (default), the listener calls
         * {@link AllureLifecycle#startStep(String, String, StepResult) startStep} /
         * {@link AllureLifecycle#stopStep(String) stopStep} around the HTML/Markdown
         * attachments. If no Allure parent context is found, the listener falls back
         * to {@link Allure}'s ThreadLocal-based attachment methods.
         * <p>
         * When {@code false}, the listener writes attachments directly to the captured
         * parent UUID and skips {@code startStep}/{@code stopStep}. If no Allure parent
         * context is found, the listener logs a warning and skips the attachments
         * entirely (no ThreadLocal fallback). Use this when the host test framework
         * already wraps each metric call in its own outer Allure step.
         *
         * @param withStep whether to wrap evaluation in an Allure step
         * @return this builder
         */
        public Builder withStep(final boolean withStep) {
            this.withStep = withStep;
            return this;
        }

        /**
         * Sets whether the rendered HTML report should be attached to the Allure step
         * (optional, default {@code true}).
         * <p>
         * If both this flag and {@link #withMarkdownAttachment(boolean)} are disabled,
         * no Allure step is created even when {@code withStep=true} (the listener
         * performs an early return in {@code afterMetricEvaluation}).
         *
         * @param withHtmlAttachment whether to attach the HTML report
         * @return this builder
         */
        public Builder withHtmlAttachment(final boolean withHtmlAttachment) {
            this.withHtmlAttachment = withHtmlAttachment;
            return this;
        }

        /**
         * Sets whether the rendered Markdown report should be attached to the Allure
         * step (optional, default {@code true}).
         * <p>
         * If both this flag and {@link #withHtmlAttachment(boolean)} are disabled,
         * no Allure step is created even when {@code withStep=true} (the listener
         * performs an early return in {@code afterMetricEvaluation}).
         *
         * @param withMarkdownAttachment whether to attach the Markdown report
         * @return this builder
         */
        public Builder withMarkdownAttachment(final boolean withMarkdownAttachment) {
            this.withMarkdownAttachment = withMarkdownAttachment;
            return this;
        }

        /**
         * Sets whether the summary section (scores, timing, models) is rendered in
         * both attachments (optional, default {@code true}).
         *
         * @param withSummary whether to render the summary section
         * @return this builder
         */
        public Builder withSummary(final boolean withSummary) {
            this.withSummary = withSummary;
            return this;
        }

        /**
         * Sets whether the per-step / per-model explanation section is rendered in
         * both attachments (optional, default {@code true}).
         *
         * @param withExplanation whether to render the explanation section
         * @return this builder
         */
        public Builder withExplanation(final boolean withExplanation) {
            this.withExplanation = withExplanation;
            return this;
        }

        /**
         * Sets whether the methodology section is rendered in both attachments
         * (optional, default <b>{@code false}</b>).
         * <p>
         * <b>Breaking change:</b> earlier listener versions always rendered the
         * methodology section. The default has been flipped to {@code false} to keep
         * attachments compact.
         * <p>
         * Migration: callers that need the previous behavior must call
         * {@code .withMethodology(true)} explicitly when configuring the builder.
         *
         * @param withMethodology whether to render the methodology section
         * @return this builder
         */
        public Builder withMethodology(final boolean withMethodology) {
            this.withMethodology = withMethodology;
            return this;
        }

        /**
         * Sets whether the execution log / steps timeline section is rendered in
         * both attachments (optional, default {@code true}).
         *
         * @param withExecutionLog whether to render the execution log section
         * @return this builder
         */
        public Builder withExecutionLog(final boolean withExecutionLog) {
            this.withExecutionLog = withExecutionLog;
            return this;
        }

        /**
         * Sets whether the excluded models section is rendered in both attachments
         * (optional, default {@code true}).
         *
         * @param withExcludedModels whether to render the excluded models section
         * @return this builder
         */
        public Builder withExcludedModels(final boolean withExcludedModels) {
            this.withExcludedModels = withExcludedModels;
            return this;
        }

        /**
         * Builds the {@link AllureMetricExecutionListener} instance.
         *
         * @return a new listener configured with the values set on this builder
         * @throws NullPointerException if any required field ({@code properties},
         *     {@code templateEngine}, {@code attachmentWriter}, {@code methodologyLoader})
         *     is {@code null}. The exception message contains the missing field name.
         */
        public AllureMetricExecutionListener build() {
            java.util.Objects.requireNonNull(properties, "properties must not be null");
            java.util.Objects.requireNonNull(templateEngine, "templateEngine must not be null");
            java.util.Objects.requireNonNull(attachmentWriter, "attachmentWriter must not be null");
            java.util.Objects.requireNonNull(methodologyLoader, "methodologyLoader must not be null");
            final RenderConfig renderConfig = new RenderConfig(
                    withHtmlAttachment,
                    withMarkdownAttachment,
                    withSummary,
                    withExplanation,
                    withMethodology,
                    withExecutionLog,
                    withExcludedModels);
            return new AllureMetricExecutionListener(
                    properties, templateEngine, attachmentWriter, methodologyLoader, lifecycle, withStep, renderConfig);
        }
    }
}
