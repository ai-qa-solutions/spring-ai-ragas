package ai.qa.solutions.execution.listener;

import ai.qa.solutions.execution.ModelResult;
import ai.qa.solutions.execution.visual.AsciiGanttChart;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

/**
 * Logging implementation of {@link MetricExecutionListener} for console visualization.
 * <p>
 * Outputs are formatted as single log statements for readability in parallel test execution.
 */
@Slf4j
public class LoggingMetricExecutionListener implements MetricExecutionListener {

    private static final int DEFAULT_CHART_HEIGHT = 0;
    private static final int BOX_WIDTH = 100;
    // Content width after "║ " prefix
    private static final int CONTENT_WIDTH = BOX_WIDTH - 2;
    // Prompt line width after "║   │ " prefix (6 chars)
    private static final int PROMPT_LINE_WIDTH = BOX_WIDTH - 8;

    private final int chartHeight;
    private final boolean showStepDetails;

    private final Map<String, Duration> modelTotalDurations = new ConcurrentHashMap<>();
    private final Map<String, Duration> embeddingModelDurations = new ConcurrentHashMap<>();
    private final Map<String, String> excludedModelReasons = new ConcurrentHashMap<>();
    private final List<StepResults> allStepResults = new ArrayList<>();
    private String currentMetricName;

    public LoggingMetricExecutionListener() {
        this(CONTENT_WIDTH, DEFAULT_CHART_HEIGHT, true);
    }

    /**
     * Creates a logging listener with custom settings.
     *
     * @param chartWidth      ignored (kept for backward compatibility, using fixed BOX_WIDTH)
     * @param chartHeight     chart height (0 for auto)
     * @param showStepDetails whether to show step progress details
     */
    public LoggingMetricExecutionListener(int chartWidth, int chartHeight, boolean showStepDetails) {
        // chartWidth is ignored - we use fixed CONTENT_WIDTH for consistent box formatting
        this.chartHeight = chartHeight;
        this.showStepDetails = showStepDetails;
    }

    @Override
    public void beforeMetricEvaluation(MetricEvaluationContext context) {
        modelTotalDurations.clear();
        embeddingModelDurations.clear();
        excludedModelReasons.clear();
        allStepResults.clear();
        currentMetricName = context.getMetricName();

        final List<String> sortedLlmModels =
                context.getModelIds().stream().sorted().toList();
        final List<String> sortedEmbeddingModels = context.getEmbeddingModelIds() != null
                ? context.getEmbeddingModelIds().stream().sorted().toList()
                : List.of();

        final StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("╔").append("═".repeat(BOX_WIDTH - 2)).append("╗\n");
        sb.append("║ ").append(context.getMetricName()).append(" | Steps: ").append(context.getTotalSteps());

        // Show "Models: X" if only LLM, or "LLM: X | Embedding: Y" if both
        if (sortedEmbeddingModels.isEmpty()) {
            sb.append(" | Models: ").append(sortedLlmModels.size());
        } else {
            sb.append(" | LLM: ")
                    .append(sortedLlmModels.size())
                    .append(" | Embedding: ")
                    .append(sortedEmbeddingModels.size());
        }
        sb.append("\n");

        // If both model types present, use section headers
        if (!sortedEmbeddingModels.isEmpty()) {
            sb.append("╠═══ LLM Models ").append("═".repeat(BOX_WIDTH - 17)).append("╣\n");
            for (String model : sortedLlmModels) {
                sb.append("║   ").append(model).append("\n");
            }
            sb.append("╠═══ Embedding Models ")
                    .append("═".repeat(BOX_WIDTH - 23))
                    .append("╣\n");
            for (String model : sortedEmbeddingModels) {
                sb.append("║   ").append(model).append("\n");
            }
        } else {
            // Just list models without section header
            for (String model : sortedLlmModels) {
                sb.append("║   ").append(model).append("\n");
            }
        }

        sb.append("╚").append("═".repeat(BOX_WIDTH - 2)).append("╝");
        log.info("{}", sb);
    }

    @Override
    public void beforeStep(StepContext context) {
        if (showStepDetails) {
            log.info(
                    "[{}] Step [{}/{}]: {} ...",
                    currentMetricName,
                    context.getStepIndex() + 1,
                    context.getTotalSteps(),
                    context.getStepName());
        }
    }

    @Override
    public void afterStep(StepResults results) {
        allStepResults.add(results);

        // Track LLM model durations
        for (ModelResult<?> result : results.getResults()) {
            if (result.duration() != null) {
                modelTotalDurations.merge(result.modelId(), result.duration(), Duration::plus);
            }
        }

        // Track embedding model durations separately (use max for parallel execution)
        if (results.getStepType() == StepType.EMBEDDING && results.getEmbeddingModelResults() != null) {
            for (ModelResult<?> embResult : results.getEmbeddingModelResults()) {
                if (embResult.duration() != null) {
                    // Use max instead of sum since embeddings run in parallel
                    embeddingModelDurations.merge(
                            embResult.modelId(),
                            embResult.duration(),
                            (old, neu) -> old.compareTo(neu) > 0 ? old : neu);
                }
            }
        }

        if (showStepDetails) {
            final int ok = results.getSuccessCount();
            final int fail = results.getFailCount();
            final long ms = results.getTotalDuration().toMillis();

            // For embedding steps, show embedding-specific stats
            if (results.getStepType() == StepType.EMBEDDING
                    && !results.getEmbeddingModelResults().isEmpty()) {
                final int embOk = results.getEmbeddingSuccessCount();
                final int embFail = results.getEmbeddingFailCount();
                final long embMs = results.getEmbeddingDuration().toMillis();

                if (embFail == 0) {
                    log.info(
                            "[{}] Step [{}]: {} models OK in {}ms (embedding: {} models, {}ms)",
                            currentMetricName,
                            results.getStepName(),
                            ok,
                            ms,
                            embOk,
                            embMs);
                } else {
                    log.warn(
                            "[{}] Step [{}]: {} OK, {} FAILED in {}ms (embedding: {} OK, {} FAILED, {}ms)",
                            currentMetricName,
                            results.getStepName(),
                            ok,
                            fail,
                            ms,
                            embOk,
                            embFail,
                            embMs);
                }
            } else {
                if (fail == 0) {
                    log.info("[{}] Step [{}]: {} models OK in {}ms", currentMetricName, results.getStepName(), ok, ms);
                } else {
                    log.warn(
                            "[{}] Step [{}]: {} OK, {} FAILED in {}ms",
                            currentMetricName,
                            results.getStepName(),
                            ok,
                            fail,
                            ms);
                }
            }
        }
    }

    @Override
    public void onModelExcluded(ModelExclusionEvent event) {
        final String shortReason = extractShortReason(event);
        excludedModelReasons.put(event.getModelId(), shortReason);

        log.warn(
                "[{}] EXCLUDED: {} at step '{}' - {}",
                currentMetricName,
                event.getModelId(),
                event.getFailedStepName(),
                shortReason);
    }

    @Override
    public void afterMetricEvaluation(MetricEvaluationResult result) {
        final StringBuilder sb = new StringBuilder();

        // Header
        sb.append("\n");
        sb.append("╔").append("═".repeat(BOX_WIDTH - 2)).append("╗\n");
        sb.append("║ RESULT: ").append(result.getMetricName());
        if (result.getAggregatedScore() != null) {
            sb.append(" = ").append(String.format("%.4f", result.getAggregatedScore()));
        } else {
            sb.append(" = N/A");
        }
        sb.append(" (").append(result.getTotalDuration().toMillis()).append("ms)\n");

        // Steps section
        if (!allStepResults.isEmpty()) {
            appendSectionHeader(sb, "STEPS", String.valueOf(allStepResults.size()));

            for (StepResults step : allStepResults) {
                final String type =
                        step.getStepType() != null ? step.getStepType().name() : "?";
                sb.append("║ [")
                        .append(step.getStepIndex() + 1)
                        .append("/")
                        .append(step.getTotalSteps())
                        .append("] ");
                sb.append(step.getStepName()).append(" [").append(type).append("] - ");
                sb.append(step.getSuccessCount()).append(" OK");
                if (step.getFailCount() > 0) {
                    sb.append(", ").append(step.getFailCount()).append(" FAILED");
                }
                sb.append(" (").append(step.getTotalDuration().toMillis()).append("ms)\n");

                // Prompt for LLM steps
                if (step.getStepType() == StepType.LLM && step.getRequest() != null) {
                    sb.append("║   ┌─ Prompt ")
                            .append("─".repeat(BOX_WIDTH - 16))
                            .append("\n");
                    for (String line : step.getRequest().split("\n")) {
                        appendWrappedLines(sb, "║   │ ", line, PROMPT_LINE_WIDTH);
                    }
                    sb.append("║   └").append("─".repeat(BOX_WIDTH - 6)).append("\n");
                } else if (step.getStepType() == StepType.EMBEDDING && step.getRequest() != null) {
                    appendWrappedLines(sb, "║   Text: ", step.getRequest().replace("\n", "\\n"), CONTENT_WIDTH - 8);
                }
            }
        }

        // LLM Execution timeline
        if (!modelTotalDurations.isEmpty()) {
            final long totalMs = modelTotalDurations.values().stream()
                    .mapToLong(Duration::toMillis)
                    .max()
                    .orElse(0);
            appendSectionHeader(sb, "LLM TIMELINE", totalMs + "ms");

            final List<AsciiGanttChart.Item> items = modelTotalDurations.entrySet().stream()
                    .map(e -> new AsciiGanttChart.Item(e.getKey(), e.getValue()))
                    .toList();
            final AsciiGanttChart.Result gantt = AsciiGanttChart.render(items, CONTENT_WIDTH);
            for (String line : gantt.text().split("\n")) {
                appendWrappedLines(sb, "║ ", line, CONTENT_WIDTH);
            }
        }

        // Embedding models timeline
        if (!embeddingModelDurations.isEmpty()) {
            final long embTotalMs = embeddingModelDurations.values().stream()
                    .mapToLong(Duration::toMillis)
                    .max()
                    .orElse(0);
            appendSectionHeader(sb, "EMBEDDING TIMELINE", embTotalMs + "ms");

            final List<AsciiGanttChart.Item> embItems = embeddingModelDurations.entrySet().stream()
                    .map(e -> new AsciiGanttChart.Item(e.getKey(), e.getValue()))
                    .toList();
            final AsciiGanttChart.Result embGantt = AsciiGanttChart.render(embItems, CONTENT_WIDTH);
            for (String line : embGantt.text().split("\n")) {
                appendWrappedLines(sb, "║ ", line, CONTENT_WIDTH);
            }
        }

        // Model scores
        if (result.getModelScores() != null && !result.getModelScores().isEmpty()) {
            appendSectionHeader(sb, "SCORES", null);
            appendScoresChart(sb, result.getModelScores());
        }

        // Excluded models with reasons
        if (!excludedModelReasons.isEmpty()) {
            appendSectionHeader(sb, "EXCLUDED", String.valueOf(excludedModelReasons.size()));
            excludedModelReasons.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        final String line = entry.getKey() + ": " + entry.getValue();
                        appendWrappedLines(sb, "║   ", line, CONTENT_WIDTH - 2);
                    });
        }

        // Footer
        sb.append("╚").append("═".repeat(BOX_WIDTH - 2)).append("╝");

        log.info("{}", sb);
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }

    /**
     * Creates a new listener instance for a single evaluation.
     * <p>
     * This listener is stateful (accumulates step results, durations, etc.),
     * so a fresh instance is required for each metric evaluation to ensure
     * thread-safety in parallel execution scenarios.
     *
     * @return a new LoggingMetricExecutionListener with the same configuration
     */
    @Override
    public MetricExecutionListener forEvaluation() {
        return new LoggingMetricExecutionListener(CONTENT_WIDTH, chartHeight, showStepDetails);
    }

    public static LoggingMetricExecutionListener minimal() {
        return new LoggingMetricExecutionListener(CONTENT_WIDTH, DEFAULT_CHART_HEIGHT, false);
    }

    public static LoggingMetricExecutionListener verbose() {
        return new LoggingMetricExecutionListener(CONTENT_WIDTH, 0, true);
    }

    /**
     * Appends a section header line: ╠═══ NAME (value) ═══...═══╣
     *
     * @param sb    StringBuilder to append to
     * @param name  section name (e.g., "TIMELINE", "SCORES")
     * @param value optional value to show in parentheses (can be null)
     */
    private static void appendSectionHeader(final StringBuilder sb, final String name, final String value) {
        // Format: ╠═══ NAME (value) ═══...═══╣  or  ╠═══ NAME ═══...═══╣
        // Total width = BOX_WIDTH, where last char is ╣
        final String prefix = "╠═══ " + name;
        final String suffix = value != null ? " (" + value + ") " : " ";
        // paddingLength = BOX_WIDTH - prefix.length() - suffix.length() - 1 (for ╣)
        final int paddingLength = BOX_WIDTH - 1 - prefix.length() - suffix.length();
        sb.append(prefix)
                .append(suffix)
                .append("═".repeat(Math.max(0, paddingLength)))
                .append("╣\n");
    }

    /**
     * Extracts a short, readable reason for model exclusion.
     */
    private static String extractShortReason(final ModelExclusionEvent event) {
        if (event.getCause() == null) {
            return "unknown error";
        }

        final String message = event.getCause().getMessage();
        if (message == null || message.isBlank()) {
            return event.getCause().getClass().getSimpleName();
        }

        // Clean up the message
        String clean = message.replace("\n", " ").replace("\r", "").trim();

        // Extract common error patterns - more specific patterns first
        if (clean.contains("empty") || clean.contains("Empty")) {
            return "empty response";
        }
        if (clean.contains("end-of-input") || clean.contains("No content to map")) {
            return "empty response";
        }
        if (clean.contains("code 96") || clean.contains("```json") || clean.contains("```")) {
            return "markdown in response";
        }
        if (clean.contains("Unrecognized token")) {
            return "text before JSON";
        }
        if (clean.contains("closing quote") || clean.contains("Unexpected end")) {
            return "truncated JSON";
        }
        if (clean.contains("No fallback setter/field") || clean.contains("duplicate")) {
            return "duplicate JSON key";
        }
        // Generic parse error - after more specific checks
        if (clean.contains("parse") || clean.contains("Parse") || clean.contains("JSON")) {
            return "parse error";
        }
        if (clean.contains("timeout") || clean.contains("Timeout")) {
            return "timeout";
        }
        if (clean.contains("rate limit") || clean.contains("429")) {
            return "rate limited";
        }
        if (clean.contains("401") || clean.contains("Unauthorized")) {
            return "auth error";
        }
        if (clean.contains("500") || clean.contains("Internal Server")) {
            return "server error";
        }

        // Truncate long messages
        if (clean.length() > 50) {
            return clean.substring(0, 47) + "...";
        }
        return clean;
    }

    /**
     * Appends wrapped lines with the given prefix.
     * Long lines are wrapped to fit within maxWidth.
     *
     * @param sb       StringBuilder to append to
     * @param prefix   prefix for each line (e.g., "║ " or "║   │ ")
     * @param text     text to wrap
     * @param maxWidth maximum width for the text content
     */
    private static void appendWrappedLines(
            final StringBuilder sb, final String prefix, final String text, final int maxWidth) {
        if (text == null || text.isEmpty()) {
            sb.append(prefix).append("\n");
            return;
        }

        int start = 0;
        while (start < text.length()) {
            final int end = Math.min(start + maxWidth, text.length());
            sb.append(prefix).append(text, start, end).append("\n");
            start = end;
        }
    }

    /**
     * Appends a scores chart in the same style as timeline chart.
     */
    private static void appendScoresChart(final StringBuilder sb, final Map<String, Double> modelScores) {
        // Constants for chart layout
        final int labelWidth = 35;
        final int barWidth = CONTENT_WIDTH - labelWidth - 8; // 8 for " 0.0000" suffix
        final char barChar = '█';
        final char emptyChar = '░';

        // Sort models alphabetically and calculate statistics
        final List<Map.Entry<String, Double>> sorted = modelScores.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .toList();

        final double minScore =
                sorted.stream().mapToDouble(Map.Entry::getValue).min().orElse(0);
        final double maxScore =
                sorted.stream().mapToDouble(Map.Entry::getValue).max().orElse(1);
        final double avgScore =
                sorted.stream().mapToDouble(Map.Entry::getValue).average().orElse(0);

        final String minModel = sorted.stream()
                .min(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse("");
        final String maxModel = sorted.stream()
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse("");

        // Determine scale (use actual range or 0-1 if range is small)
        final double scaleMin = Math.min(0, minScore);
        final double scaleMax = Math.max(maxScore, minScore + 0.01); // avoid division by zero

        // Score axis header
        sb.append("║ ").append(String.format("%-" + labelWidth + "s", "Score"));
        sb.append(String.format("%-10s", String.format("%.2f", scaleMin)));
        final int midPoints = 3;
        for (int i = 1; i <= midPoints; i++) {
            final double midVal = scaleMin + (scaleMax - scaleMin) * i / (midPoints + 1);
            sb.append(String.format("%-" + (barWidth / (midPoints + 1)) + "s", String.format("%.2f", midVal)));
        }
        sb.append(String.format("%.2f", scaleMax)).append("\n");

        // Scale line
        sb.append("║ ").append(" ".repeat(labelWidth));
        sb.append("├");
        for (int i = 0; i < midPoints; i++) {
            sb.append("─".repeat(barWidth / (midPoints + 1) - 1)).append("┼");
        }
        sb.append("─".repeat(barWidth - (barWidth / (midPoints + 1)) * midPoints - 1))
                .append("┤\n");

        // Render each model bar
        for (final Map.Entry<String, Double> entry : sorted) {
            final String label = truncateLabel(entry.getKey(), labelWidth - 1);
            final double score = entry.getValue();

            // Calculate bar length proportional to score
            final int filledLength = scaleMax > scaleMin
                    ? (int) Math.round((score - scaleMin) / (scaleMax - scaleMin) * barWidth)
                    : barWidth;
            final int emptyLength = barWidth - filledLength;

            sb.append("║ ");
            sb.append(String.format("%-" + labelWidth + "s", label));
            sb.append(String.valueOf(barChar).repeat(Math.max(0, filledLength)));
            sb.append(String.valueOf(emptyChar).repeat(Math.max(0, emptyLength)));
            sb.append(String.format(" %.4f", score)).append("\n");
        }

        // Summary line
        sb.append("║ min=").append(String.format("%.4f", minScore));
        sb.append(" (").append(truncateLabel(minModel, 20)).append(")");
        sb.append(" | max=").append(String.format("%.4f", maxScore));
        sb.append(" (").append(truncateLabel(maxModel, 20)).append(")");
        sb.append(" | avg=").append(String.format("%.4f", avgScore)).append("\n");
    }

    /**
     * Truncates a label to fit within maxLength, adding "..." if needed.
     */
    private static String truncateLabel(final String label, final int maxLength) {
        if (label == null) return "";
        if (label.length() <= maxLength) return label;
        return label.substring(0, maxLength - 3) + "...";
    }
}
