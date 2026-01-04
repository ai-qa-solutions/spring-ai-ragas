package ai.qa.solutions.execution.visual;

import ai.qa.solutions.execution.ModelExecutionResult;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Renders an ASCII Gantt chart showing the execution timeline of multiple models.
 * <p>
 * This visualizer displays:
 * <ul>
 *   <li>Temporal sequence of model executions</li>
 *   <li>Parallel execution patterns</li>
 *   <li>Duration of each execution</li>
 *   <li>Success/failure status</li>
 * </ul>
 */
public class AsciiGanttChart {

    private static final int DEFAULT_WIDTH = 100;
    private static final char SUCCESS_CHAR = '█';
    private static final char FAILURE_CHAR = '▓';
    private static final char EMPTY_CHAR = ' ';

    /**
     * Renders a Gantt chart from execution results.
     *
     * @param results list of execution results
     * @return rendered Gantt chart as a string
     */
    public static String render(final List<ModelExecutionResult> results) {
        return render(results, DEFAULT_WIDTH);
    }

    /**
     * Renders a Gantt chart with custom width.
     *
     * @param results list of execution results
     * @param width   width of the chart in characters
     * @return rendered Gantt chart as a string
     */
    public static String render(final List<ModelExecutionResult> results, final int width) {
        if (results == null || results.isEmpty()) {
            return "";
        }

        // Sort alphabetically by model ID
        final List<ModelExecutionResult> sortedResults = new ArrayList<>(results);
        sortedResults.sort(Comparator.comparing(r -> r.getContext().getModelId()));

        // Calculate time boundaries
        final Instant firstStart = sortedResults.stream()
                .map(r -> r.getContext().getStartedAt())
                .min(Instant::compareTo)
                .orElseThrow();
        final Instant lastEnd = sortedResults.stream()
                .map(r -> r.getContext().getStartedAt().plus(r.getDuration()))
                .max(Instant::compareTo)
                .orElse(firstStart);

        long totalDurationMs = Duration.between(firstStart, lastEnd).toMillis();
        // Handle edge case when all executions complete instantly
        if (totalDurationMs == 0) {
            totalDurationMs = 1; // Use 1ms minimum to avoid division by zero
        }

        // Find the longest model name for padding
        final int maxNameLength = sortedResults.stream()
                .mapToInt(r -> r.getContext().getModelId().length())
                .max()
                .orElse(0);

        final StringBuilder sb = new StringBuilder();

        // Render header with time markers
        sb.append(renderHeader(totalDurationMs, maxNameLength, width));

        // Render each execution
        for (final ModelExecutionResult result : sortedResults) {
            sb.append(renderRow(result, firstStart, totalDurationMs, maxNameLength, width));
        }

        // Render footer
        sb.append(renderFooter(maxNameLength, width));

        return sb.toString();
    }

    private static String renderHeader(final long totalDurationMs, final int maxNameLength, final int width) {
        final StringBuilder sb = new StringBuilder();

        // Title and timescale header on the same line
        final String title = "Execution Timeline:";
        final int titlePadding = Math.max(0, maxNameLength + 1 - title.length());

        sb.append("\n");
        sb.append(title);
        sb.append(" ".repeat(titlePadding));
        sb.append("│0s");
        sb.append(" ".repeat(width - 4));
        sb.append(String.format("%.1fs│\n", totalDurationMs / 1000.0));

        // Top border
        sb.append(" ".repeat(maxNameLength + 1));
        sb.append("┌");
        sb.append("─".repeat(width));
        sb.append("┐\n");

        return sb.toString();
    }

    private static String renderRow(
            final ModelExecutionResult result,
            final Instant firstStart,
            final long totalDurationMs,
            final int maxNameLength,
            final int width) {

        final String modelId = result.getContext().getModelId();
        final Instant start = result.getContext().getStartedAt();
        final long durationMs = result.getDuration().toMillis();

        // Calculate positions
        final long startOffsetMs = Duration.between(firstStart, start).toMillis();
        final int startPos = (int) ((startOffsetMs * width) / totalDurationMs);
        final int barLength = Math.max(1, (int) ((durationMs * width) / totalDurationMs));

        // Build row
        final StringBuilder sb = new StringBuilder();

        // Model name (left-padded)
        sb.append(String.format("%-" + maxNameLength + "s", modelId));
        sb.append(" │");

        // Empty space before bar
        sb.append(String.valueOf(EMPTY_CHAR).repeat(Math.max(0, startPos)));

        // Execution bar
        final char barChar = result.isSuccess() ? SUCCESS_CHAR : FAILURE_CHAR;
        for (int i = 0; i < barLength && (startPos + i) < width; i++) {
            sb.append(barChar);
        }

        // Fill remaining space
        final int remaining = width - startPos - barLength;
        sb.append(String.valueOf(EMPTY_CHAR).repeat(Math.max(0, remaining)));

        // Duration annotation
        sb.append(String.format("│ %.1fs", durationMs / 1000.0));

        // Status indicator
        if (!result.isSuccess()) {
            sb.append(" ✗");
        }

        sb.append("\n");

        return sb.toString();
    }

    private static String renderFooter(final int maxNameLength, final int width) {
        final StringBuilder sb = new StringBuilder();

        // Bottom border
        sb.append(" ".repeat(maxNameLength + 1));
        sb.append("└");
        sb.append("─".repeat(width));
        sb.append("┘\n");

        return sb.toString();
    }
}
