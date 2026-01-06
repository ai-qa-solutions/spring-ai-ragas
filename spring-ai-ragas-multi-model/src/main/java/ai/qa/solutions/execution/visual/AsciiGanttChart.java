package ai.qa.solutions.execution.visual;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * ASCII Gantt chart for visualizing model execution timelines.
 * <p>
 * Renders a timeline showing when each model started and how long it ran.
 * Models are sorted alphabetically for consistent display.
 *
 * <h3>Example Output:</h3>
 * <pre>
 * Model Execution Timeline (total: 5954ms)
 * ──────────────────────────────────────────────────────────────────────────────
 * Time(ms)  0         1000      2000      3000      4000      5000      6000
 *           ├─────────┼─────────┼─────────┼─────────┼─────────┼─────────┤
 * anthropic/claude-4.5-sonnet     ████████████████████████████████░░░░ 4850ms
 * anthropic/claude-haiku-4.5      ██████████░░░░░░░░░░░░░░░░░░░░░░░░░░ 1523ms
 * google/gemini-2.0-flash-001     █████████████████████████████████░░░ 5102ms
 * openai/gpt-4o-mini              ████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░ 1203ms
 * ──────────────────────────────────────────────────────────────────────────────
 * </pre>
 */
public final class AsciiGanttChart {

    private static final char BAR_CHAR = '█';
    private static final char EMPTY_CHAR = '░';
    private static final int DEFAULT_WIDTH = 100;
    private static final int MIN_LABEL_WIDTH = 35;

    private AsciiGanttChart() {}

    /**
     * Item representing a model execution with timing information.
     *
     * @param modelId  the model identifier
     * @param duration execution duration
     */
    public record Item(String modelId, Duration duration) {
        public long durationMs() {
            return duration != null ? duration.toMillis() : 0;
        }
    }

    /**
     * Result of rendering a Gantt chart.
     *
     * @param text      the rendered chart as a string
     * @param maxTimeMs the maximum time shown on the chart
     * @param itemCount number of items rendered
     */
    public record Result(String text, long maxTimeMs, int itemCount) {}

    /**
     * Renders a Gantt chart from execution items.
     *
     * @param items list of model execution items
     * @return rendered chart result
     */
    public static Result render(final List<Item> items) {
        return render(items, DEFAULT_WIDTH);
    }

    /**
     * Renders a Gantt chart from execution items with custom width.
     *
     * @param items list of model execution items
     * @param width total chart width in characters
     * @return rendered chart result
     */
    public static Result render(final List<Item> items, final int width) {
        if (items == null || items.isEmpty()) {
            return new Result("No execution data", 0, 0);
        }

        // Sort items alphabetically by modelId
        final List<Item> sorted = new ArrayList<>(items);
        sorted.sort(Comparator.comparing(Item::modelId));

        // Find max duration for scaling
        final long maxDuration =
                sorted.stream().mapToLong(Item::durationMs).max().orElse(1);

        // Calculate dimensions
        final int labelWidth = Math.max(
                MIN_LABEL_WIDTH,
                sorted.stream().mapToInt(i -> i.modelId().length()).max().orElse(MIN_LABEL_WIDTH) + 2);
        final int barWidth = Math.max(20, width - labelWidth - 10); // 10 for duration suffix

        final StringBuilder sb = new StringBuilder();

        // Time axis (no header - caller provides section title)
        renderTimeAxis(sb, labelWidth, barWidth, maxDuration);

        // Render each model bar
        for (final Item item : sorted) {
            renderBar(sb, item, labelWidth, barWidth, maxDuration);
        }

        return new Result(sb.toString(), maxDuration, sorted.size());
    }

    private static void renderTimeAxis(
            final StringBuilder sb, final int labelWidth, final int barWidth, final long maxDuration) {
        // Time labels row
        sb.append(String.format("%-" + labelWidth + "s", "Time(ms)"));

        // Calculate nice time intervals
        final int numMarkers = Math.min(7, barWidth / 10);
        final long interval = calculateNiceInterval(maxDuration, numMarkers);

        // Build time markers
        final StringBuilder markers = new StringBuilder();
        final StringBuilder tickLine = new StringBuilder();

        for (int i = 0; i <= numMarkers; i++) {
            final long time = i * interval;
            if (time > maxDuration) break;

            final int position = (int) ((double) time / maxDuration * barWidth);
            final String timeLabel = formatTime(time);

            // Pad to position
            while (markers.length() < position) {
                markers.append(' ');
                tickLine.append(i == 0 ? '├' : (markers.length() == position ? '┼' : '─'));
            }

            markers.append(timeLabel);
            tickLine.append('┼');
        }

        // Pad to full width
        while (markers.length() < barWidth) {
            markers.append(' ');
        }
        while (tickLine.length() < barWidth) {
            tickLine.append('─');
        }
        if (!tickLine.isEmpty()) {
            tickLine.setCharAt(0, '├');
            tickLine.setCharAt(tickLine.length() - 1, '┤');
        }

        sb.append(markers, 0, Math.min(markers.length(), barWidth)).append("\n");
        sb.append(" ".repeat(labelWidth))
                .append(tickLine, 0, Math.min(tickLine.length(), barWidth))
                .append("\n");
    }

    private static void renderBar(
            final StringBuilder sb, final Item item, final int labelWidth, final int barWidth, final long maxDuration) {
        final String label = item.modelId();
        final long duration = item.durationMs();

        // Calculate bar length
        final int filledLength = maxDuration > 0 ? (int) Math.round((double) duration / maxDuration * barWidth) : 0;
        final int emptyLength = barWidth - filledLength;

        // Build the bar
        final String bar = String.valueOf(BAR_CHAR).repeat(Math.max(0, filledLength))
                + String.valueOf(EMPTY_CHAR).repeat(Math.max(0, emptyLength));

        // Format: label + bar + duration
        sb.append(String.format("%-" + labelWidth + "s%s %dms%n", truncateLabel(label, labelWidth - 1), bar, duration));
    }

    private static long calculateNiceInterval(final long maxValue, final int numIntervals) {
        if (maxValue <= 0 || numIntervals <= 0) return 1000;

        final double rawInterval = (double) maxValue / numIntervals;

        // Round to nice number (1, 2, 5, 10, 20, 50, 100, 200, 500, 1000, ...)
        final double magnitude = Math.pow(10, Math.floor(Math.log10(rawInterval)));
        final double normalized = rawInterval / magnitude;

        final double niceNormalized;
        if (normalized <= 1) {
            niceNormalized = 1;
        } else if (normalized <= 2) {
            niceNormalized = 2;
        } else if (normalized <= 5) {
            niceNormalized = 5;
        } else {
            niceNormalized = 10;
        }

        return Math.max(1, (long) (niceNormalized * magnitude));
    }

    private static String formatTime(final long ms) {
        if (ms >= 10000) {
            return String.format("%.1fs", ms / 1000.0);
        }
        return String.valueOf(ms);
    }

    private static String truncateLabel(final String label, final int maxLength) {
        if (label == null) return "";
        if (label.length() <= maxLength) return label;
        return label.substring(0, maxLength - 3) + "...";
    }
}
