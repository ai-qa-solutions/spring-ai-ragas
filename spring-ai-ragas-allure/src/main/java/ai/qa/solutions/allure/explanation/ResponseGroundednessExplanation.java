package ai.qa.solutions.allure.explanation;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Score explanation for NVIDIA-style ResponseGroundednessMetric.
 * <p>
 * Response Groundedness evaluates whether the response is grounded in the retrieved contexts.
 * It uses a 0-2 scoring scale normalized to 0-1:
 * <ul>
 *   <li>0 - Not grounded: Response contains information not found in contexts</li>
 *   <li>1 - Partially grounded: Response is partially supported by contexts</li>
 *   <li>2 - Fully grounded: Response is completely supported by contexts</li>
 * </ul>
 */
@Getter
public class ResponseGroundednessExplanation extends AbstractScoreExplanation {

    private static final String METRIC_TYPE = "response-groundedness";

    private final String response;
    private final String context;
    private final int rawScore;
    private final String reasoning;
    private final boolean usedHeuristics;

    @Builder
    public ResponseGroundednessExplanation(
            final Double score,
            final String language,
            final String response,
            final String context,
            final int rawScore,
            final String reasoning,
            final boolean usedHeuristics) {
        super(score, language);
        this.response = response != null ? response : "";
        this.context = context != null ? context : "";
        this.rawScore = rawScore;
        this.reasoning = reasoning != null ? reasoning : "";
        this.usedHeuristics = usedHeuristics;
        buildSteps();
        buildInterpretation();
    }

    @Override
    public String getMetricType() {
        return METRIC_TYPE;
    }

    @Override
    public String getSimpleDescription() {
        return messages.get("responseGroundedness.description");
    }

    private void buildSteps() {
        if (usedHeuristics) {
            steps.add(StepExplanation.builder()
                    .stepName("ApplyHeuristics")
                    .stepNumber(1)
                    .title(messages.get("responseGroundedness.step.heuristics.title"))
                    .description(messages.get("responseGroundedness.step.heuristics.desc"))
                    .inputData(truncateText(response, 200))
                    .outputSummary(
                            score != null && score >= 1.0
                                    ? messages.get("responseGroundedness.heuristics.match")
                                    : messages.get("responseGroundedness.heuristics.noMatch"))
                    .items(List.of())
                    .hasModelDisagreement(false)
                    .agreementPercent(100.0)
                    .build());
        }

        if (!usedHeuristics || (score != null && score < 1.0)) {
            final int stepNum = usedHeuristics ? 2 : 1;
            steps.add(StepExplanation.builder()
                    .stepName("EvaluateGroundedness")
                    .stepNumber(stepNum)
                    .title(messages.get("responseGroundedness.step.evaluate.title"))
                    .description(messages.get("responseGroundedness.step.evaluate.desc"))
                    .inputData(truncateText(response, 200))
                    .outputSummary(String.format(
                            "%s: %d/2 (%.1f%%)",
                            messages.get("responseGroundedness.rawScoreLabel"), rawScore, (rawScore / 2.0) * 100))
                    .items(List.of(ExplanationItem.builder()
                            .content(reasoning)
                            .passed(rawScore >= 1)
                            .verdict(getGroundednessVerdict(rawScore))
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

    private String getGroundednessVerdict(final int rawScore) {
        return switch (rawScore) {
            case 2 -> messages.get("responseGroundedness.verdict.fullyGrounded");
            case 1 -> messages.get("responseGroundedness.verdict.partiallyGrounded");
            default -> messages.get("responseGroundedness.verdict.notGrounded");
        };
    }

    private void buildInterpretation() {
        final String formula = messages.get("responseGroundedness.formula");
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
        if (usedHeuristics && score != null && score >= 1.0) {
            return messages.get("responseGroundedness.calculation.heuristic");
        }
        return String.format("%s / 2 = %.2f", rawScore, score != null ? score : 0.0);
    }

    private String getMeaning() {
        if (score == null) {
            return messages.get("common.scoreNotCalculated");
        }
        if (score >= 0.9) {
            return messages.get("responseGroundedness.meaning.excellent");
        } else if (score >= 0.7) {
            return messages.get("responseGroundedness.meaning.good");
        } else if (score >= 0.5) {
            return messages.get("responseGroundedness.meaning.moderate");
        } else {
            return messages.get("responseGroundedness.meaning.poor");
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
                        .description(messages.get("responseGroundedness.scale.excellent"))
                        .current(score != null && score >= 0.9)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.good"))
                        .range("70-89%")
                        .description(messages.get("responseGroundedness.scale.good"))
                        .current(score != null && score >= 0.7 && score < 0.9)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.moderate"))
                        .range("50-69%")
                        .description(messages.get("responseGroundedness.scale.moderate"))
                        .current(score != null && score >= 0.5 && score < 0.7)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.poor"))
                        .range("0-49%")
                        .description(messages.get("responseGroundedness.scale.poor"))
                        .current(score != null && score < 0.5)
                        .build());
    }
}
