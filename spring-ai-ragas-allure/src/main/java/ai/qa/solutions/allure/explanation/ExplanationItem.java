package ai.qa.solutions.allure.explanation;

import lombok.Builder;
import lombok.Value;

/**
 * A single item in the step explanation (e.g., one statement with its verdict).
 * <p>
 * Used to display individual elements like:
 * - Statement + verdict (for Faithfulness)
 * - Context + relevance (for ContextPrecision)
 * - Reference sentence + found status (for ContextRecall)
 */
@Value
@Builder
public class ExplanationItem {

    /**
     * The main content of the item (e.g., statement text, context text).
     */
    String content;

    /**
     * Whether this item passed verification (true/false/null if not applicable).
     */
    Boolean passed;

    /**
     * Human-readable verdict (e.g., "ВЕРНО", "НЕВЕРНО", "YES", "NO").
     */
    String verdict;

    /**
     * Reason/explanation for the verdict.
     */
    String reason;

    /**
     * Additional context (e.g., which context supported this item).
     */
    String source;

    /**
     * Numeric value associated with this item (e.g., similarity score).
     */
    Double numericValue;

    /**
     * Index/position of this item in the list (1-based).
     */
    int index;

    /**
     * Gets CSS class for styling based on passed status.
     *
     * @return CSS class name
     */
    public String getStatusClass() {
        if (passed == null) {
            return "neutral";
        }
        return passed ? "passed" : "failed";
    }

    /**
     * Gets icon character based on passed status.
     *
     * @return Unicode icon character
     */
    public String getStatusIcon() {
        if (passed == null) {
            return "-";
        }
        return passed ? "✓" : "✗";
    }
}
