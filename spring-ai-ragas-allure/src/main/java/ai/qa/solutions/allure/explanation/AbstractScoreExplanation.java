package ai.qa.solutions.allure.explanation;

import ai.qa.solutions.allure.i18n.ExplanationMessages;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.Getter;

/**
 * Abstract base class for metric-specific score explanations.
 * <p>
 * Provides common functionality for building step explanations
 * and score interpretations.
 */
@Getter
public abstract class AbstractScoreExplanation implements ScoreExplanation {

    protected final Double score;
    protected final String language;
    protected final ExplanationMessages messages;
    protected final List<StepExplanation> steps = new ArrayList<>();
    protected ScoreInterpretation interpretation;

    protected AbstractScoreExplanation(final Double score, final String language) {
        this.score = score;
        this.language = language != null ? language : "en";
        this.messages = new ExplanationMessages(this.language);
    }

    /**
     * Formats score as percentage string.
     *
     * @param value the score value (0.0 to 1.0)
     * @return formatted percentage (e.g., "66.67%")
     */
    protected String formatPercent(final Double value) {
        if (value == null) {
            return messages.get("common.na");
        }
        return String.format(Locale.US, "%.2f%%", value * 100);
    }

    /**
     * Checks if the current language is Russian.
     *
     * @return true if language is "ru"
     */
    protected boolean isRussian() {
        return messages.isRussian();
    }

    /**
     * Creates standard scale levels for metrics with relative interpretation.
     *
     * @param score the current score
     * @return list of scale levels with current level marked
     */
    protected List<ScoreInterpretation.ScaleLevel> createStandardScale(final Double score) {
        final List<ScoreInterpretation.ScaleLevel> levels = new ArrayList<>();

        levels.add(ScoreInterpretation.ScaleLevel.builder()
                .name(messages.get("scale.excellent"))
                .range("90-100%")
                .description(messages.get("scale.excellent.desc"))
                .current(score != null && score >= 0.9)
                .build());
        levels.add(ScoreInterpretation.ScaleLevel.builder()
                .name(messages.get("scale.good"))
                .range("70-90%")
                .description(messages.get("scale.good.desc"))
                .current(score != null && score >= 0.7 && score < 0.9)
                .build());
        levels.add(ScoreInterpretation.ScaleLevel.builder()
                .name(messages.get("scale.moderate"))
                .range("50-70%")
                .description(messages.get("scale.moderate.desc"))
                .current(score != null && score >= 0.5 && score < 0.7)
                .build());
        levels.add(ScoreInterpretation.ScaleLevel.builder()
                .name(messages.get("scale.poor"))
                .range("0-50%")
                .description(messages.get("scale.poor.desc"))
                .current(score != null && score < 0.5)
                .build());

        return levels;
    }

    /**
     * Gets the index of the current level in the scale.
     *
     * @param score the score value
     * @return index (0-3) based on score
     */
    protected int getCurrentLevelIndex(final Double score) {
        if (score == null) {
            return 3;
        }
        if (score >= 0.9) {
            return 0;
        }
        if (score >= 0.7) {
            return 1;
        }
        if (score >= 0.5) {
            return 2;
        }
        return 3;
    }

    /**
     * Gets the level name based on score.
     *
     * @param score the score value
     * @return level name
     */
    protected String getLevelName(final Double score) {
        if (score == null) {
            return messages.get("common.unknown");
        }
        if (score >= 0.9) {
            return messages.get("scale.excellent");
        }
        if (score >= 0.7) {
            return messages.get("scale.good");
        }
        if (score >= 0.5) {
            return messages.get("scale.moderate");
        }
        return messages.get("scale.poor");
    }
}
