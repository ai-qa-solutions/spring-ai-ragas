package ai.qa.solutions.metric.explanation;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Interpretation of the final score with context.
 * <p>
 * Provides both the raw calculation breakdown and the human-readable
 * interpretation of what the score means.
 */
@Value
@Builder
public class ScoreInterpretation {

    /**
     * The formula used to calculate the score (human-readable).
     * Example: "(верные утверждения) / (всего утверждений)"
     */
    String formula;

    /**
     * The actual calculation with numbers.
     * Example: "2 / 3 = 0.6667 = 66.67%"
     */
    String calculation;

    /**
     * Numerator value (e.g., number of passed items).
     */
    Integer numerator;

    /**
     * Denominator value (e.g., total items).
     */
    Integer denominator;

    /**
     * The final score value (0.0 to 1.0).
     */
    Double score;

    /**
     * Score as formatted percentage.
     */
    String scorePercent;

    /**
     * Interpretation level (e.g., "excellent", "good", "moderate", "poor").
     */
    String level;

    /**
     * Whether the score is considered "good" for this metric.
     * <p>
     * For metrics like NoiseSensitivity, 0 is good.
     * For metrics like Faithfulness, higher is better.
     * Null if interpretation is relative (user-defined criteria).
     */
    Boolean isGood;

    /**
     * Human-readable interpretation of what the score means.
     */
    String meaning;

    /**
     * Scale levels for relative metrics.
     */
    @Builder.Default
    List<ScaleLevel> scaleLevels = List.of();

    /**
     * The index of the current level in the scale (0-based).
     */
    int currentLevelIndex;

    /**
     * Minimum level in the rubric scale (e.g., 1).
     * Used for rubric-based metrics to calculate normalized scores.
     */
    Integer minLevel;

    /**
     * Maximum level in the rubric scale (e.g., 5 or 9).
     * Used for rubric-based metrics to calculate normalized scores.
     */
    Integer maxLevel;

    /**
     * A single level in the interpretation scale.
     */
    @Value
    @Builder
    public static class ScaleLevel {
        /**
         * Level name (e.g., "Excellent", "Good", "Moderate", "Poor").
         */
        String name;

        /**
         * Score range for this level (e.g., "90-100%").
         */
        String range;

        /**
         * Description of what this level means.
         */
        String description;

        /**
         * Whether this is the current level based on the score.
         */
        boolean current;
    }
}
