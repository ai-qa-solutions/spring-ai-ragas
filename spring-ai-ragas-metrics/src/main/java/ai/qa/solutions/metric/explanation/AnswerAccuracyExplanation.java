package ai.qa.solutions.metric.explanation;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Score explanation for NVIDIA-style AnswerAccuracyMetric.
 * <p>
 * Answer Accuracy evaluates whether the AI response accurately matches the reference answer.
 * It uses a 0-2 scoring scale normalized to 0-1:
 * <ul>
 *   <li>0 - Incorrect: Response is factually wrong or contradicts the reference</li>
 *   <li>1 - Partially correct: Response is partially correct but incomplete or has minor errors</li>
 *   <li>2 - Fully correct: Response accurately matches the reference answer</li>
 * </ul>
 * <p>
 * Optional dual-judge mode performs a confirmation judgment for higher reliability.
 */
@Getter
public class AnswerAccuracyExplanation extends AbstractScoreExplanation {

    private static final String METRIC_TYPE = "answer-accuracy";

    private final String response;
    private final String reference;
    private final int rawScore;
    private final String reasoning;
    private final boolean usedDualJudge;
    private final Integer confirmationScore;
    private final String confirmationReasoning;

    @Builder
    public AnswerAccuracyExplanation(
            final Double score,
            final String language,
            final String response,
            final String reference,
            final int rawScore,
            final String reasoning,
            final boolean usedDualJudge,
            final Integer confirmationScore,
            final String confirmationReasoning) {
        super(score, language);
        this.response = response != null ? response : "";
        this.reference = reference != null ? reference : "";
        this.rawScore = rawScore;
        this.reasoning = reasoning != null ? reasoning : "";
        this.usedDualJudge = usedDualJudge;
        this.confirmationScore = confirmationScore;
        this.confirmationReasoning = confirmationReasoning != null ? confirmationReasoning : "";
        buildSteps();
        buildInterpretation();
    }

    @Override
    public String getMetricType() {
        return METRIC_TYPE;
    }

    @Override
    public String getSimpleDescription() {
        return messages.get("answerAccuracy.description");
    }

    private void buildSteps() {
        final int totalSteps = usedDualJudge ? 2 : 1;

        // Step 1: Initial Judgment
        steps.add(StepExplanation.builder()
                .stepName("InitialJudgment")
                .stepNumber(1)
                .title(messages.get("answerAccuracy.step.initial.title"))
                .description(messages.get("answerAccuracy.step.initial.desc"))
                .inputData(truncateText(response, 200))
                .outputSummary(String.format(
                        "%s: %d/2 (%.1f%%)",
                        messages.get("answerAccuracy.rawScoreLabel"), rawScore, (rawScore / 2.0) * 100))
                .items(List.of(ExplanationItem.builder()
                        .content(reasoning)
                        .passed(rawScore >= 1)
                        .verdict(getAccuracyVerdict(rawScore))
                        .build()))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 2: Confirmation Judgment (if dual judge enabled)
        if (usedDualJudge && confirmationScore != null) {
            steps.add(StepExplanation.builder()
                    .stepName("ConfirmJudgment")
                    .stepNumber(2)
                    .title(messages.get("answerAccuracy.step.confirm.title"))
                    .description(messages.get("answerAccuracy.step.confirm.desc"))
                    .inputData(String.format(
                            "%s: %d - %s",
                            messages.get("answerAccuracy.initialAssessmentLabel"),
                            rawScore,
                            truncateText(reasoning, 100)))
                    .outputSummary(String.format(
                            "%s: %d/2 (%.1f%%)",
                            messages.get("answerAccuracy.finalScoreLabel"),
                            confirmationScore,
                            (confirmationScore / 2.0) * 100))
                    .items(List.of(ExplanationItem.builder()
                            .content(confirmationReasoning)
                            .passed(confirmationScore >= 1)
                            .verdict(getAccuracyVerdict(confirmationScore))
                            .build()))
                    .hasModelDisagreement(false)
                    .agreementPercent(100.0)
                    .build());
        }
    }

    private String truncateText(final String text, final int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text != null ? text : "";
        }
        return text.substring(0, maxLength) + "...";
    }

    private String getAccuracyVerdict(final int scoreValue) {
        return switch (scoreValue) {
            case 2 -> messages.get("answerAccuracy.verdict.fullyCorrect");
            case 1 -> messages.get("answerAccuracy.verdict.partiallyCorrect");
            default -> messages.get("answerAccuracy.verdict.incorrect");
        };
    }

    private void buildInterpretation() {
        final String formula = messages.get("answerAccuracy.formula");
        final String calculation = buildCalculation();
        final String meaning = getMeaning();

        interpretation = ScoreInterpretation.builder()
                .formula(formula)
                .calculation(calculation)
                .score(score)
                .scorePercent(formatPercent(score))
                .level(getLevel())
                .meaning(meaning)
                .scaleLevels(createStandardScale())
                .currentLevelIndex(getCurrentLevelIndex())
                .build();
    }

    private String buildCalculation() {
        final int finalScore = usedDualJudge && confirmationScore != null ? confirmationScore : rawScore;
        return String.format("%s / 2 = %.2f", finalScore, score != null ? score : 0.0);
    }

    private String getMeaning() {
        if (score == null) {
            return messages.get("common.scoreNotCalculated");
        }
        if (score >= 0.9) {
            return messages.get("answerAccuracy.meaning.excellent");
        } else if (score >= 0.7) {
            return messages.get("answerAccuracy.meaning.good");
        } else if (score >= 0.5) {
            return messages.get("answerAccuracy.meaning.moderate");
        } else {
            return messages.get("answerAccuracy.meaning.poor");
        }
    }

    private String getLevel() {
        if (score == null) {
            return messages.get("common.unknown");
        }
        if (score >= 0.9) {
            return messages.get("scale.excellent");
        } else if (score >= 0.7) {
            return messages.get("scale.good");
        } else if (score >= 0.5) {
            return messages.get("scale.moderate");
        } else {
            return messages.get("scale.poor");
        }
    }

    private int getCurrentLevelIndex() {
        if (score == null) {
            return 3;
        }
        if (score >= 0.9) {
            return 0;
        } else if (score >= 0.7) {
            return 1;
        } else if (score >= 0.5) {
            return 2;
        } else {
            return 3;
        }
    }

    private List<ScoreInterpretation.ScaleLevel> createStandardScale() {
        return List.of(
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.excellent"))
                        .range("90-100%")
                        .description(messages.get("answerAccuracy.scale.excellent"))
                        .current(score != null && score >= 0.9)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.good"))
                        .range("70-89%")
                        .description(messages.get("answerAccuracy.scale.good"))
                        .current(score != null && score >= 0.7 && score < 0.9)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.moderate"))
                        .range("50-69%")
                        .description(messages.get("answerAccuracy.scale.moderate"))
                        .current(score != null && score >= 0.5 && score < 0.7)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.poor"))
                        .range("0-49%")
                        .description(messages.get("answerAccuracy.scale.poor"))
                        .current(score != null && score < 0.5)
                        .build());
    }
}
