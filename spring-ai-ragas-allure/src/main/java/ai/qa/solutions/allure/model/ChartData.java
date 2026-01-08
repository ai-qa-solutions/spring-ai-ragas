package ai.qa.solutions.allure.model;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Data container for chart visualizations in the Allure report.
 * <p>
 * Contains pre-computed data for rendering various SVG charts:
 * <ul>
 *   <li>Heatmap matrix (models x verdicts)</li>
 *   <li>Timeline/Gantt diagram</li>
 *   <li>Bar chart for score comparison</li>
 * </ul>
 */
@Value
@Builder
public class ChartData {

    /**
     * Row labels for heatmap (model IDs).
     */
    @Builder.Default
    List<String> heatmapRowLabels = List.of();

    /**
     * Column labels for heatmap (statements, contexts, or other items).
     */
    @Builder.Default
    List<String> heatmapColLabels = List.of();

    /**
     * Heatmap values as a 2D matrix.
     * <p>
     * Values represent verdicts: 1 = pass/true, 0 = fail/false, -1 = not applicable.
     * Outer list is rows (models), inner list is columns (items).
     */
    @Builder.Default
    List<List<Integer>> heatmapValues = List.of();

    /**
     * Timeline entries for Gantt chart visualization.
     */
    @Builder.Default
    List<TimelineEntry> timelineEntries = List.of();

    /**
     * Score entries for bar chart visualization.
     */
    @Builder.Default
    List<ScoreEntry> scoreEntries = List.of();

    /**
     * Time scale factor for timeline rendering (pixels per millisecond).
     */
    @Builder.Default
    double timeScale = 0.1;

    /**
     * Maximum duration in milliseconds (for timeline scaling).
     */
    @Builder.Default
    long maxDurationMs = 0;

    /**
     * Computed heatmap width in pixels.
     */
    public int getHeatmapWidth() {
        final int labelWidth = 150;
        final int cellWidth = 30;
        return labelWidth + heatmapColLabels.size() * cellWidth + 20;
    }

    /**
     * Computed heatmap height in pixels.
     */
    public int getHeatmapHeight() {
        final int headerHeight = 50;
        final int cellHeight = 30;
        return headerHeight + heatmapRowLabels.size() * cellHeight + 20;
    }

    /**
     * Checks if heatmap data is available for rendering.
     *
     * @return true if heatmap has data to display
     */
    public boolean hasHeatmapData() {
        return !heatmapValues.isEmpty() && !heatmapRowLabels.isEmpty() && !heatmapColLabels.isEmpty();
    }

    /**
     * Checks if timeline data is available for rendering.
     *
     * @return true if timeline has entries to display
     */
    public boolean hasTimelineData() {
        return !timelineEntries.isEmpty();
    }

    /**
     * Checks if score data is available for rendering.
     *
     * @return true if score entries exist
     */
    public boolean hasScoreData() {
        return !scoreEntries.isEmpty();
    }

    /**
     * Entry for timeline/Gantt chart visualization.
     */
    @Value
    @Builder
    public static class TimelineEntry {

        /**
         * Model identifier.
         */
        String modelId;

        /**
         * Name of the step.
         */
        String stepName;

        /**
         * Type of the step (LLM, EMBEDDING, COMPUTE).
         */
        String stepType;

        /**
         * Start offset in milliseconds from evaluation start.
         */
        long startOffsetMs;

        /**
         * Duration of this execution in milliseconds.
         */
        long durationMs;

        /**
         * Whether this execution was successful.
         */
        boolean success;
    }

    /**
     * Entry for score bar chart visualization.
     */
    @Value
    @Builder
    public static class ScoreEntry {

        /**
         * Model identifier.
         */
        String modelId;

        /**
         * Score value (0.0 to 1.0).
         */
        double score;

        /**
         * Whether this model was excluded from evaluation.
         */
        @Builder.Default
        boolean excluded = false;
    }
}
