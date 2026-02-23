package ai.qa.solutions.metric.explanation;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Score explanation for RougeScoreMetric.
 * <p>
 * ROUGE (Recall-Oriented Understudy for Gisting Evaluation) measures overlap
 * between response and reference using various methods (ROUGE-1, ROUGE-2, ROUGE-L).
 * This is a non-LLM metric.
 */
@Getter
public class RougeScoreExplanation extends AbstractScoreExplanation {

    private static final String METRIC_TYPE = "rouge-score";

    private final String response;
    private final String reference;
    private final String rougeType;
    private final String mode;

    @Builder
    public RougeScoreExplanation(
            final Double score,
            final String language,
            final String response,
            final String reference,
            final String rougeType,
            final String mode) {
        super(score, language);
        this.response = response != null ? response : "";
        this.reference = reference != null ? reference : "";
        this.rougeType = rougeType != null ? rougeType : "ROUGE_L";
        this.mode = mode != null ? mode : "FMEASURE";
        buildSteps();
        buildInterpretation();
    }

    @Override
    public String getMetricType() {
        return METRIC_TYPE;
    }

    @Override
    public String getSimpleDescription() {
        return messages.get("rougeScore.description");
    }

    private void buildSteps() {
        // Step 1: Show response and reference
        steps.add(StepExplanation.builder()
                .stepName("InputTexts")
                .stepNumber(1)
                .title(messages.get("rougeScore.step1.title"))
                .description(messages.get("rougeScore.step1.desc"))
                .inputData(String.format(
                        "%s: %s\n%s: %s",
                        messages.get("common.response"),
                        truncate(response, 200),
                        messages.get("common.reference"),
                        truncate(reference, 200)))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 2: Configuration
        final String rougeTypeDisplay = formatRougeType(rougeType);
        final String modeDisplay = formatMode(mode);
        steps.add(StepExplanation.builder()
                .stepName("Configuration")
                .stepNumber(2)
                .title(messages.get("rougeScore.step2.title"))
                .description(messages.get("rougeScore.step2.desc"))
                .outputSummary(messages.get("rougeScore.step2.output", rougeTypeDisplay, modeDisplay))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 3: Compute overlap
        steps.add(StepExplanation.builder()
                .stepName("ComputeOverlap")
                .stepNumber(3)
                .title(messages.get("rougeScore.step3.title", rougeTypeDisplay))
                .description(getOverlapDescription())
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 4: Compute score
        steps.add(StepExplanation.builder()
                .stepName("ComputeScore")
                .stepNumber(4)
                .title(messages.get("rougeScore.step4.title"))
                .description(messages.get("rougeScore.step4.desc", modeDisplay))
                .outputSummary(formatPercent(score))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());
    }

    private String getOverlapDescription() {
        return switch (rougeType.toUpperCase()) {
            case "ROUGE_1" -> messages.get("rougeScore.rouge1.desc");
            case "ROUGE_2" -> messages.get("rougeScore.rouge2.desc");
            default -> messages.get("rougeScore.rougeL.desc");
        };
    }

    private String formatRougeType(final String type) {
        if (type == null) {
            return "ROUGE-L";
        }
        return type.replace("_", "-");
    }

    private String formatMode(final String m) {
        if (m == null) {
            return "F-measure";
        }
        return switch (m.toUpperCase()) {
            case "PRECISION" -> messages.get("rougeScore.mode.precision");
            case "RECALL" -> messages.get("rougeScore.mode.recall");
            default -> messages.get("rougeScore.mode.fmeasure");
        };
    }

    private void buildInterpretation() {
        final String formula = getFormula();

        interpretation = ScoreInterpretation.builder()
                .formula(formula)
                .calculation(formatPercent(score))
                .score(score)
                .scorePercent(formatPercent(score))
                .level(getLevelName(score))
                .meaning(getMeaning())
                .scaleLevels(createRougeScale())
                .currentLevelIndex(getCurrentLevelIndex(score))
                .build();
    }

    private String getFormula() {
        return switch (mode.toUpperCase()) {
            case "PRECISION" -> messages.get("rougeScore.formula.precision");
            case "RECALL" -> messages.get("rougeScore.formula.recall");
            default -> messages.get("rougeScore.formula.fmeasure");
        };
    }

    private String getMeaning() {
        if (score == null) {
            return messages.get("common.scoreNotCalculated");
        }
        if (score >= 0.9) {
            return messages.get("rougeScore.meaning.excellent");
        }
        if (score >= 0.7) {
            return messages.get("rougeScore.meaning.good");
        }
        if (score >= 0.5) {
            return messages.get("rougeScore.meaning.moderate");
        }
        return messages.get("rougeScore.meaning.poor");
    }

    private List<ScoreInterpretation.ScaleLevel> createRougeScale() {
        return List.of(
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.excellent"))
                        .range("90-100%")
                        .description(messages.get("rougeScore.scale.excellent"))
                        .current(score != null && score >= 0.9)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.good"))
                        .range("70-90%")
                        .description(messages.get("rougeScore.scale.good"))
                        .current(score != null && score >= 0.7 && score < 0.9)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.moderate"))
                        .range("50-70%")
                        .description(messages.get("rougeScore.scale.moderate"))
                        .current(score != null && score >= 0.5 && score < 0.7)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.poor"))
                        .range("0-50%")
                        .description(messages.get("rougeScore.scale.poor"))
                        .current(score != null && score < 0.5)
                        .build());
    }

    private String truncate(final String text, final int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
