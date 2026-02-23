package ai.qa.solutions.metric.explanation;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

/**
 * Score explanation for ToolCallAccuracyMetric.
 * <p>
 * Tool Call Accuracy evaluates the accuracy of agent's tool calls against
 * expected reference tool calls. It supports two modes:
 * <ul>
 *   <li>STRICT - Exact matching of tool names and arguments</li>
 *   <li>FLEXIBLE - Allows partial argument matching based on threshold</li>
 * </ul>
 * <p>
 * The score is computed as F1 (harmonic mean of precision and recall).
 */
@Getter
public class ToolCallAccuracyExplanation extends AbstractScoreExplanation {

    private static final String METRIC_TYPE = "tool-call-accuracy";

    private final String mode;
    private final double precision;
    private final double recall;
    private final int truePositives;
    private final int falsePositives;
    private final int falseNegatives;
    private final List<ToolCallMatch> matches;

    @Builder
    public ToolCallAccuracyExplanation(
            final Double score,
            final String language,
            final String mode,
            final double precision,
            final double recall,
            final int truePositives,
            final int falsePositives,
            final int falseNegatives,
            final List<ToolCallMatch> matches) {
        super(score, language);
        this.mode = mode != null ? mode : "STRICT";
        this.precision = precision;
        this.recall = recall;
        this.truePositives = truePositives;
        this.falsePositives = falsePositives;
        this.falseNegatives = falseNegatives;
        this.matches = matches != null ? matches : List.of();
        buildSteps();
        buildInterpretation();
    }

    @Override
    public String getMetricType() {
        return METRIC_TYPE;
    }

    @Override
    public String getSimpleDescription() {
        return messages.get("toolCallAccuracy.description");
    }

    private void buildSteps() {
        // Step 1: Align tool calls
        steps.add(StepExplanation.builder()
                .stepName("AlignToolCalls")
                .stepNumber(1)
                .title(messages.get("toolCallAccuracy.step.alignToolCalls.title"))
                .description(messages.get("toolCallAccuracy.step.alignToolCalls.desc"))
                .inputData(messages.get("toolCallAccuracy.modeLabel") + ": " + getLocalizedMode())
                .outputSummary(buildMatchesSummary())
                .items(buildMatchItems())
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 2: Compute precision and recall
        steps.add(StepExplanation.builder()
                .stepName("ComputePrecisionRecall")
                .stepNumber(2)
                .title(messages.get("toolCallAccuracy.step.computePrecisionRecall.title"))
                .description(messages.get("toolCallAccuracy.step.computePrecisionRecall.desc"))
                .inputData(String.format("TP=%d, FP=%d, FN=%d", truePositives, falsePositives, falseNegatives))
                .outputSummary(String.format(
                        "%s: %.2f, %s: %.2f",
                        messages.get("toolCallAccuracy.precisionLabel"),
                        precision,
                        messages.get("toolCallAccuracy.recallLabel"),
                        recall))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 3: Compute F1 score
        steps.add(StepExplanation.builder()
                .stepName("ComputeScore")
                .stepNumber(3)
                .title(messages.get("toolCallAccuracy.step.computeScore.title"))
                .description(messages.get("toolCallAccuracy.step.computeScore.desc"))
                .inputData(String.format(
                        "%s=%.2f, %s=%.2f",
                        messages.get("toolCallAccuracy.precisionLabel"),
                        precision,
                        messages.get("toolCallAccuracy.recallLabel"),
                        recall))
                .outputSummary(String.format("F1 = %.2f", score != null ? score : 0.0))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());
    }

    private String getLocalizedMode() {
        if ("STRICT".equalsIgnoreCase(mode)) {
            return messages.get("toolCallAccuracy.mode.strict");
        } else {
            return messages.get("toolCallAccuracy.mode.flexible");
        }
    }

    private String buildMatchesSummary() {
        final long matched = matches.stream().filter(ToolCallMatch::isMatched).count();
        return String.format("%s: %d/%d", messages.get("toolCallAccuracy.matchedLabel"), matched, matches.size());
    }

    private List<ExplanationItem> buildMatchItems() {
        return matches.stream()
                .map(match -> ExplanationItem.builder()
                        .content(formatToolCall(match))
                        .passed(match.isMatched())
                        .verdict(
                                match.isMatched()
                                        ? messages.get("toolCallAccuracy.matchedLabel")
                                        : messages.get("toolCallAccuracy.notMatchedLabel"))
                        .build())
                .toList();
    }

    private String formatToolCall(final ToolCallMatch match) {
        final StringBuilder sb = new StringBuilder();
        sb.append(messages.get("toolCallAccuracy.toolName")).append(": ").append(match.getToolName());
        if (match.getArguments() != null && !match.getArguments().isEmpty()) {
            sb.append(" | ")
                    .append(messages.get("toolCallAccuracy.arguments"))
                    .append(": ")
                    .append(match.getArguments());
        }
        if (match.getMatchScore() > 0 && match.getMatchScore() < 1.0) {
            sb.append(" | ")
                    .append(messages.get("toolCallAccuracy.matchScore"))
                    .append(": ")
                    .append(String.format("%.0f%%", match.getMatchScore() * 100));
        }
        return sb.toString();
    }

    private void buildInterpretation() {
        final String formula = messages.get("toolCallAccuracy.formula");
        final String calculation = String.format(
                "2 × (%.2f × %.2f) / (%.2f + %.2f) = %.2f",
                precision, recall, precision, recall, score != null ? score : 0.0);
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

    private String getMeaning() {
        if (score == null) {
            return messages.get("common.scoreNotCalculated");
        }
        if (score >= 0.9) {
            return messages.get("toolCallAccuracy.meaning.excellent");
        } else if (score >= 0.7) {
            return messages.get("toolCallAccuracy.meaning.good");
        } else if (score >= 0.5) {
            return messages.get("toolCallAccuracy.meaning.moderate");
        } else {
            return messages.get("toolCallAccuracy.meaning.poor");
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
                        .description(messages.get("toolCallAccuracy.scale.excellent"))
                        .current(score != null && score >= 0.9)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.good"))
                        .range("70-89%")
                        .description(messages.get("toolCallAccuracy.scale.good"))
                        .current(score != null && score >= 0.7 && score < 0.9)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.moderate"))
                        .range("50-69%")
                        .description(messages.get("toolCallAccuracy.scale.moderate"))
                        .current(score != null && score >= 0.5 && score < 0.7)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.poor"))
                        .range("0-49%")
                        .description(messages.get("toolCallAccuracy.scale.poor"))
                        .current(score != null && score < 0.5)
                        .build());
    }

    /**
     * Represents a match between actual and reference tool calls for explanation.
     */
    @Getter
    @Builder
    public static class ToolCallMatch {
        private final String toolName;
        private final Map<String, Object> arguments;
        private final boolean matched;
        private final double matchScore;
    }
}
