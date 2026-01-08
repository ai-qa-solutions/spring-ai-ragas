package ai.qa.solutions.allure.explanation;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Result from a single model for a specific step.
 * <p>
 * Used for multi-model delta analysis to show where models agreed/disagreed.
 */
@Value
@Builder
public class ModelStepResult {

    /**
     * Model identifier.
     */
    String modelId;

    /**
     * Whether this model succeeded in this step.
     */
    boolean success;

    /**
     * The verdict from this model (for binary decisions).
     */
    Boolean verdict;

    /**
     * Numeric result from this model (if applicable).
     */
    Double numericResult;

    /**
     * Numerator for ratio display (e.g., "3 / 4 = 75%").
     */
    Integer numerator;

    /**
     * Denominator for ratio display (e.g., "3 / 4 = 75%").
     */
    Integer denominator;

    /**
     * Detailed items that contributed to this model's result.
     * Each item shows what was checked and whether it passed/failed.
     */
    @Builder.Default
    List<DetailItem> items = List.of();

    /**
     * The model's reasoning/explanation.
     */
    String reasoning;

    /**
     * Error message if the model failed.
     */
    String errorMessage;

    /**
     * A single item in the model's evaluation.
     */
    @Value
    @Builder
    public static class DetailItem {
        /**
         * The content being evaluated (e.g., statement text).
         */
        String content;

        /**
         * Whether this item passed the check.
         */
        boolean passed;

        /**
         * Short verdict label (e.g., "OK", "ERROR", "FAITHFUL", "UNFAITHFUL").
         */
        String verdict;

        /**
         * Additional explanation/reason for the verdict.
         */
        String reason;
    }

    /**
     * Gets display status for the model.
     *
     * @return status string
     */
    public String getDisplayStatus() {
        if (!success) {
            return "ERROR";
        }
        if (verdict != null) {
            return verdict ? "AGREE" : "DISAGREE";
        }
        return "OK";
    }

    /**
     * Gets icon for the model result.
     *
     * @return icon character
     */
    public String getIcon() {
        if (!success) {
            return "⚠";
        }
        if (verdict != null) {
            return verdict ? "✓" : "✗";
        }
        return "•";
    }
}
