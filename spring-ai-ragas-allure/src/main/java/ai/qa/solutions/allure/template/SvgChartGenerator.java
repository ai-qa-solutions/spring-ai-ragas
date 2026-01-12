package ai.qa.solutions.allure.template;

import ai.qa.solutions.allure.model.ChartData;
import ai.qa.solutions.allure.model.ChartData.ScoreEntry;
import ai.qa.solutions.allure.model.ChartData.TimelineEntry;
import java.util.List;
import java.util.Locale;

/**
 * Generator for SVG chart visualizations.
 * <p>
 * Creates inline SVG elements for:
 * <ul>
 *   <li>Heatmap matrices (models x verdicts)</li>
 *   <li>Timeline/Gantt diagrams</li>
 *   <li>Bar charts for score comparison</li>
 * </ul>
 * <p>
 * All generated SVGs are self-contained with embedded styles,
 * requiring no external CSS or JavaScript.
 */
public class SvgChartGenerator {

    private static final int CELL_SIZE = 28;
    private static final int CELL_GAP = 2;
    private static final int LABEL_WIDTH = 150;
    private static final int HEADER_HEIGHT = 60;
    private static final int BAR_HEIGHT = 24;
    private static final int BAR_GAP = 6;

    // Allure 3 color scheme
    private static final String COLOR_PASS = "#09a232";
    private static final String COLOR_FAIL = "#eb5146";
    private static final String COLOR_NA = "#7281a1";
    private static final String COLOR_LLM = "#2c67e8";
    private static final String COLOR_EMBEDDING = "#7a2ce8";
    private static final String COLOR_COMPUTE = "#eb9b46";
    private static final String COLOR_ERROR = "#eb5146";

    /**
     * Generates an SVG heatmap from chart data.
     *
     * @param chartData the chart data containing heatmap values
     * @return SVG string or empty string if no data
     */
    public String generateHeatmap(final ChartData chartData) {
        if (!chartData.hasHeatmapData()) {
            return "";
        }

        final List<String> rowLabels = chartData.getHeatmapRowLabels();
        final List<String> colLabels = chartData.getHeatmapColLabels();
        final List<List<Integer>> values = chartData.getHeatmapValues();

        final int width = LABEL_WIDTH + colLabels.size() * (CELL_SIZE + CELL_GAP) + 20;
        final int height = HEADER_HEIGHT + rowLabels.size() * (CELL_SIZE + CELL_GAP) + 20;

        final StringBuilder svg = new StringBuilder();
        svg.append(String.format(
                "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 %d %d\" class=\"heatmap-chart\">%n",
                width, height));
        svg.append(generateHeatmapStyles());

        // Column labels (rotated)
        for (int col = 0; col < colLabels.size(); col++) {
            final int x = LABEL_WIDTH + col * (CELL_SIZE + CELL_GAP) + CELL_SIZE / 2;
            final String label = truncateLabel(colLabels.get(col), 15);
            svg.append(String.format(
                    "  <text x=\"%d\" y=\"%d\" class=\"col-label\" transform=\"rotate(-45 %d %d)\">%s</text>%n",
                    x, HEADER_HEIGHT - 10, x, HEADER_HEIGHT - 10, escapeXml(label)));
        }

        // Rows
        for (int row = 0; row < rowLabels.size(); row++) {
            final int y = HEADER_HEIGHT + row * (CELL_SIZE + CELL_GAP);

            // Row label
            svg.append(String.format(
                    "  <text x=\"%d\" y=\"%d\" class=\"row-label\">%s</text>%n",
                    LABEL_WIDTH - 5, y + CELL_SIZE / 2 + 4, escapeXml(truncateLabel(rowLabels.get(row), 20))));

            // Cells
            final List<Integer> rowValues = values.get(row);
            for (int col = 0; col < rowValues.size(); col++) {
                final int x = LABEL_WIDTH + col * (CELL_SIZE + CELL_GAP);
                final int value = rowValues.get(col);
                final String colorClass = value == 1 ? "cell-pass" : (value == 0 ? "cell-fail" : "cell-na");
                final String symbol = value == 1 ? "&#10003;" : (value == 0 ? "&#10007;" : "-");

                svg.append(String.format(
                        "  <rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" class=\"%s\" rx=\"3\"/>%n",
                        x, y, CELL_SIZE, CELL_SIZE, colorClass));
                svg.append(String.format(
                        "  <text x=\"%d\" y=\"%d\" class=\"cell-text\">%s</text>%n",
                        x + CELL_SIZE / 2, y + CELL_SIZE / 2 + 5, symbol));
            }
        }

        svg.append("</svg>");
        return svg.toString();
    }

    /**
     * Generates an SVG timeline/Gantt chart from chart data.
     *
     * @param chartData the chart data containing timeline entries
     * @return SVG string or empty string if no data
     */
    public String generateTimeline(final ChartData chartData) {
        if (!chartData.hasTimelineData()) {
            return "";
        }

        final List<TimelineEntry> entries = chartData.getTimelineEntries();
        final long maxDuration = chartData.getMaxDurationMs();
        final double timeScale = maxDuration > 0 ? 600.0 / maxDuration : 1.0;

        final int width = LABEL_WIDTH + 650;
        final int height = entries.size() * (BAR_HEIGHT + BAR_GAP) + 80;

        final StringBuilder svg = new StringBuilder();
        svg.append(String.format(
                "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 %d %d\" class=\"timeline-chart\">%n",
                width, height));
        svg.append(generateTimelineStyles());

        // Time axis
        svg.append(String.format(
                "  <line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" class=\"axis\"/>%n",
                LABEL_WIDTH, height - 30, width - 20, height - 30));

        // Time labels
        final int intervals = 5;
        for (int i = 0; i <= intervals; i++) {
            final int x = LABEL_WIDTH + (int) (i * 600.0 / intervals);
            final long time = maxDuration * i / intervals;
            svg.append(String.format(
                    "  <text x=\"%d\" y=\"%d\" class=\"time-label\">%dms</text>%n", x, height - 10, time));
            svg.append(String.format(
                    "  <line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" class=\"grid-line\"/>%n", x, 30, x, height - 35));
        }

        // Bars
        for (int i = 0; i < entries.size(); i++) {
            final TimelineEntry entry = entries.get(i);
            final int y = 30 + i * (BAR_HEIGHT + BAR_GAP);
            final int barX = LABEL_WIDTH + (int) (entry.getStartOffsetMs() * timeScale);
            final int barWidth = Math.max(2, (int) (entry.getDurationMs() * timeScale));
            final String colorClass = getTimelineColorClass(entry);

            // Label
            svg.append(String.format(
                    "  <text x=\"%d\" y=\"%d\" class=\"row-label\">%s</text>%n",
                    LABEL_WIDTH - 5, y + BAR_HEIGHT / 2 + 4, escapeXml(truncateLabel(entry.getModelId(), 20))));

            // Bar
            svg.append(String.format(
                    "  <rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" class=\"%s\" rx=\"3\">%n",
                    barX, y, barWidth, BAR_HEIGHT, colorClass));
            svg.append(String.format(
                    "    <title>%s: %dms (%s)</title>%n",
                    escapeXml(entry.getStepName()), entry.getDurationMs(), entry.getStepType()));
            svg.append("  </rect>\n");
        }

        // Legend
        svg.append(generateTimelineLegend(width));

        svg.append("</svg>");
        return svg.toString();
    }

    /**
     * Generates an SVG bar chart for score comparison.
     *
     * @param chartData the chart data containing score entries
     * @return SVG string or empty string if no data
     */
    public String generateBarChart(final ChartData chartData) {
        if (!chartData.hasScoreData()) {
            return "";
        }

        final List<ScoreEntry> entries = chartData.getScoreEntries();
        final int width = LABEL_WIDTH + 450;
        final int height = entries.size() * (BAR_HEIGHT + BAR_GAP) + 60;

        final StringBuilder svg = new StringBuilder();
        svg.append(String.format(
                "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 %d %d\" class=\"bar-chart\">%n",
                width, height));
        svg.append(generateBarChartStyles());

        // Scale markers
        for (int i = 0; i <= 10; i++) {
            final int x = LABEL_WIDTH + i * 40;
            svg.append(String.format(
                    "  <line x1=\"%d\" y1=\"20\" x2=\"%d\" y2=\"%d\" class=\"grid-line\"/>%n", x, x, height - 30));
            if (i % 2 == 0) {
                svg.append(String.format(
                        "  <text x=\"%d\" y=\"%d\" class=\"scale-label\">%.1f</text>%n", x, height - 10, i / 10.0));
            }
        }

        // Bars
        for (int i = 0; i < entries.size(); i++) {
            final ScoreEntry entry = entries.get(i);
            final int y = 20 + i * (BAR_HEIGHT + BAR_GAP);
            final int barWidth = (int) (entry.getScore() * 400);
            final String colorClass = getScoreColorClass(entry.getScore(), entry.isExcluded());

            // Label
            svg.append(String.format(
                    "  <text x=\"%d\" y=\"%d\" class=\"row-label\">%s</text>%n",
                    LABEL_WIDTH - 5, y + BAR_HEIGHT / 2 + 4, escapeXml(truncateLabel(entry.getModelId(), 20))));

            // Bar
            svg.append(String.format(
                    "  <rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" class=\"%s\" rx=\"3\"/>%n",
                    LABEL_WIDTH, y, barWidth, BAR_HEIGHT, colorClass));

            // Score label
            svg.append(String.format(
                    Locale.US,
                    "  <text x=\"%d\" y=\"%d\" class=\"score-label\">%.4f</text>%n",
                    LABEL_WIDTH + barWidth + 5,
                    y + BAR_HEIGHT / 2 + 4,
                    entry.getScore()));
        }

        svg.append("</svg>");
        return svg.toString();
    }

    private String generateHeatmapStyles() {
        return """
              <style>
                .row-label { font-size: 11px; fill: #333; text-anchor: end; font-family: sans-serif; }
                .col-label { font-size: 10px; fill: #333; text-anchor: start; font-family: sans-serif; }
                .cell-pass { fill: %s; }
                .cell-fail { fill: %s; }
                .cell-na { fill: %s; }
                .cell-text { font-size: 14px; fill: white; text-anchor: middle; font-family: sans-serif; }
              </style>
            """
                .formatted(COLOR_PASS, COLOR_FAIL, COLOR_NA);
    }

    private String generateTimelineStyles() {
        return """
              <style>
                .row-label { font-size: 11px; fill: #333; text-anchor: end; font-family: sans-serif; }
                .time-label { font-size: 10px; fill: #666; text-anchor: middle; font-family: sans-serif; }
                .axis { stroke: #333; stroke-width: 1; }
                .grid-line { stroke: #e0e0e0; stroke-width: 1; stroke-dasharray: 2,2; }
                .bar-llm { fill: %s; }
                .bar-embedding { fill: %s; }
                .bar-compute { fill: %s; }
                .bar-error { fill: %s; }
                .legend-text { font-size: 10px; fill: #333; font-family: sans-serif; }
              </style>
            """
                .formatted(COLOR_LLM, COLOR_EMBEDDING, COLOR_COMPUTE, COLOR_ERROR);
    }

    private String generateBarChartStyles() {
        return """
              <style>
                .row-label { font-size: 11px; fill: #333; text-anchor: end; font-family: sans-serif; }
                .scale-label { font-size: 10px; fill: #666; text-anchor: middle; font-family: sans-serif; }
                .score-label { font-size: 10px; fill: #333; font-family: sans-serif; }
                .grid-line { stroke: #e0e0e0; stroke-width: 1; }
                .bar-excellent { fill: #2e7d32; }
                .bar-good { fill: #4caf50; }
                .bar-moderate { fill: #ff9800; }
                .bar-poor { fill: #f44336; }
                .bar-excluded { fill: #9e9e9e; }
              </style>
            """;
    }

    private String generateTimelineLegend(final int width) {
        return String.format(
                """
              <g transform="translate(%d, 5)">
                <rect x="0" y="0" width="12" height="12" class="bar-llm"/>
                <text x="16" y="10" class="legend-text">LLM</text>
                <rect x="60" y="0" width="12" height="12" class="bar-embedding"/>
                <text x="76" y="10" class="legend-text">Embedding</text>
                <rect x="150" y="0" width="12" height="12" class="bar-compute"/>
                <text x="166" y="10" class="legend-text">Compute</text>
              </g>
            """,
                width - 220);
    }

    private String getTimelineColorClass(final TimelineEntry entry) {
        if (!entry.isSuccess()) {
            return "bar-error";
        }
        return switch (entry.getStepType().toUpperCase()) {
            case "LLM" -> "bar-llm";
            case "EMBEDDING" -> "bar-embedding";
            case "COMPUTE" -> "bar-compute";
            default -> "bar-llm";
        };
    }

    private String getScoreColorClass(final double score, final boolean excluded) {
        if (excluded) {
            return "bar-excluded";
        }
        if (score >= 0.8) {
            return "bar-excellent";
        }
        if (score >= 0.6) {
            return "bar-good";
        }
        if (score >= 0.4) {
            return "bar-moderate";
        }
        return "bar-poor";
    }

    private String truncateLabel(final String label, final int maxLength) {
        if (label == null) {
            return "";
        }
        if (label.length() <= maxLength) {
            return label;
        }
        return label.substring(0, maxLength - 3) + "...";
    }

    private String escapeXml(final String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
