package ai.qa.solutions.execution.visual;

import com.indvd00m.ascii.render.Point;
import com.indvd00m.ascii.render.Render;
import com.indvd00m.ascii.render.api.ICanvas;
import com.indvd00m.ascii.render.api.IContextBuilder;
import com.indvd00m.ascii.render.api.IRender;
import com.indvd00m.ascii.render.elements.Label;
import com.indvd00m.ascii.render.elements.Line;
import com.indvd00m.ascii.render.elements.Rectangle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Utility class for rendering ASCII bar charts with automatic layout optimization.
 * <p>
 * This class generates horizontal bar charts in ASCII format, useful for displaying
 * score distributions across different models in console output or logs. The charts
 * automatically adjust label widths, scale bars based on data ranges, and include
 * visual indicators for average values.
 */
public class AsciiRenderBarsAuto {

    /**
     * Represents a single item to be displayed in the bar chart.
     * <p>
     * Each item consists of a label (typically a model name) and a numeric value (score).
     *
     * @param label the display label for this item (null-safe, defaults to empty string)
     * @param value the numeric value to be visualized as a bar
     */
    public record Item(String label, double value) {
        /**
         * Creates an item with null-safe label handling.
         *
         * @param label the item label
         * @param value the item value
         */
        public Item(String label, double value) {
            this.label = label == null ? "" : label;
            this.value = value;
        }
    }

    /**
     * Statistical summary of the data being visualized.
     * <p>
     * Contains minimum, maximum, and average values along with the labels
     * of the items that produced the min and max values.
     *
     * @param min      the minimum value in the dataset
     * @param max      the maximum value in the dataset
     * @param avg      the average value of all items
     * @param minLabel the label of the item with the minimum value
     * @param maxLabel the label of the item with the maximum value
     */
    public record Summary(double min, double max, double avg, String minLabel, String maxLabel) {}

    /**
     * Renders an ASCII bar chart with automatic layout optimization.
     * <p>
     * Features:
     * <ul>
     *   <li>Automatically adjusts label width based on longest label (with constraints)</li>
     *   <li>Scales bars based on min/max value range</li>
     *   <li>Displays a vertical line indicating the average value</li>
     *   <li>Shows min/max values at the top of the chart</li>
     *   <li>Displays numeric values next to each bar</li>
     * </ul>
     *
     * @param items  list of items to visualize (models and their scores)
     * @param width  total width of the chart (e.g., 100-120 characters)
     * @param height total height; 0 for auto-sizing based on number of items,
     *               otherwise uses the specified fixed height
     * @return result containing the rendered ASCII chart text and statistical summary
     */
    public static Result renderBarsAuto(final List<Item> items, final int width, int height) {
        if (items == null || items.isEmpty()) {
            return new Result("(no data)", new Summary(Double.NaN, Double.NaN, Double.NaN, "", ""));
        }
        Locale.setDefault(Locale.ROOT);

        final Item minItem =
                items.stream().min(Comparator.comparingDouble(i -> i.value)).orElse(items.get(0));
        final Item maxItem =
                items.stream().max(Comparator.comparingDouble(i -> i.value)).orElse(items.get(0));
        double min = minItem.value;
        double max = maxItem.value;
        final double avg = items.stream().mapToDouble(i -> i.value).average().orElse(0.0);

        if (max == min) {
            final double eps = Math.max(1e-9, Math.abs(max) * 1e-6);
            min -= eps;
            max += eps;
        }

        final int topPad = 1;
        final int bottomPad = 1;
        final int rightPad = 2;
        final int rows = items.size();
        final int autoHeight = topPad + rows + bottomPad;
        if (height <= 0) height = Math.max(5, autoHeight);
        height = Math.max(height, autoHeight);

        final int longest = items.stream()
                .map(i -> i.label)
                .mapToInt(s -> s == null ? 0 : s.length())
                .max()
                .orElse(6);
        final int labelsWCap = Math.max(6, width / 2 - 2);
        final int labelsW = Math.min(Math.max(6, longest), labelsWCap);

        final int labelsX = 1;
        final int barsX0 = labelsX + labelsW + 1;
        final int barsX1 = width - 1 - rightPad;
        final int barsW = Math.max(1, barsX1 - barsX0 + 1);

        final IRender render = new Render();
        final IContextBuilder b = render.newBuilder();
        b.width(width).height(height);

        b.element(new Rectangle(0, 0, width, height));
        b.element(new Line(new Point(labelsX + labelsW, topPad), new Point(labelsX + labelsW, height - 2)));

        final int avgX = valueToX(avg, min, max, barsX0, barsW);
        b.element(new Line(new Point(avgX, topPad), new Point(avgX, height - 2)));

        final String minLabel = fmt(min);
        final String maxLabel = fmt(max);
        b.element(new Label(minLabel, barsX0, 0, Math.min(minLabel.length(), Math.max(0, width - barsX0))));
        final int maxPos = Math.max(barsX0, barsX1 - maxLabel.length() + 1);
        b.element(new Label(maxLabel, maxPos, 0, Math.min(maxLabel.length(), Math.max(0, width - maxPos))));

        for (int i = 0; i < rows; i++) {
            final int y = topPad + i;
            final Item it = items.get(i);

            final String lbl = ellipsizeRight(it.label, labelsW);
            b.element(new Label(lbl, labelsX, y, labelsW));

            final int xVal = valueToX(it.value, min, max, barsX0, barsW);
            final int xStart = barsX0;
            if (xVal >= xStart) {
                b.element(new Line(new Point(xStart, y), new Point(xVal, y), '█'));
            } else {
                // Handle rounding edge case where xVal < xStart - just render a point at xStart
                b.element(new Line(new Point(xStart, y), new Point(xStart, y), '█'));
            }

            // Display numeric value to the right of the bar (within right boundary)
            final String vStr = fmt(it.value);
            final int vX = Math.min(barsX1 - vStr.length() + 1, Math.max(barsX0, xVal + 1));
            b.element(new Label(vStr, vX, y, Math.min(vStr.length(), Math.max(0, barsX1 - vX + 1))));
        }

        final ICanvas canvas = render.render(b.build());

        final Summary summary = new Summary(minItem.value, maxItem.value, avg, minItem.label, maxItem.label);
        return new Result(canvas.getText(), summary);
    }

    /**
     * Result of rendering a bar chart.
     * <p>
     * Contains both the ASCII art text representation and statistical summary.
     *
     * @param text    the rendered ASCII chart as a string
     * @param summary statistical summary of the visualized data
     */
    public record Result(String text, Summary summary) {}

    /**
     * Converts a numeric value to an X coordinate position within the bar area.
     * <p>
     * Maps the value from the data range [min, max] to the pixel range [x0, x0+width-1].
     *
     * @param v     the value to convert
     * @param min   the minimum value in the data range
     * @param max   the maximum value in the data range
     * @param x0    the starting X coordinate of the bar area
     * @param width the width of the bar area
     * @return the X coordinate corresponding to the value
     */
    private static int valueToX(final double v, final double min, final double max, final int x0, final int width) {
        if (max <= min) return x0;
        double norm = (v - min) / (max - min);
        norm = Math.max(0.0, Math.min(1.0, norm));
        return x0 + (int) Math.round(norm * (width - 1));
    }

    /**
     * Truncates a string to fit within a specified width, adding an ellipsis if needed.
     * <p>
     * If the string is longer than the width, it's truncated and an ellipsis (…) is
     * appended to indicate truncation.
     *
     * @param s the string to truncate
     * @param w the maximum width
     * @return the truncated string, or the original if it fits within the width
     */
    private static String ellipsizeRight(final String s, final int w) {
        if (w <= 0) return "";
        if (s == null) return "";
        if (s.length() <= w) return s;
        if (w <= 1) return s.substring(0, w);
        return s.substring(0, Math.max(0, w - 1)) + "…";
    }

    /**
     * Formats a double value as a string with two decimal places.
     * <p>
     * Uses ROOT locale to ensure consistent decimal formatting (period as separator).
     *
     * @param d the value to format
     * @return formatted string with two decimal places (e.g., "3.14")
     */
    private static String fmt(final double d) {
        return String.format(Locale.ROOT, "%.2f", d);
    }
}
