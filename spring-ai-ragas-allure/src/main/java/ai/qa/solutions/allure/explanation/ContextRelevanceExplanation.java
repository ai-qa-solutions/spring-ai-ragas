package ai.qa.solutions.allure.explanation;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Score explanation for NVIDIA-style ContextRelevanceMetric.
 * <p>
 * Context Relevance evaluates whether retrieved contexts are relevant to the user's question.
 * It uses a 0-2 scoring scale normalized to 0-1:
 * <ul>
 *   <li>0 - Not relevant: Context does not contain information to answer the question</li>
 *   <li>1 - Partially relevant: Context contains some relevant information</li>
 *   <li>2 - Fully relevant: Context contains comprehensive information</li>
 * </ul>
 */
@Getter
public class ContextRelevanceExplanation extends AbstractScoreExplanation {

    private static final String METRIC_TYPE = "context-relevance";

    private final String userInput;
    private final List<ContextEvaluation> contextEvaluations;

    @Builder
    public ContextRelevanceExplanation(
            final Double score,
            final String language,
            final String userInput,
            final List<ContextEvaluation> contextEvaluations) {
        super(score, language);
        this.userInput = userInput != null ? userInput : "";
        this.contextEvaluations = contextEvaluations != null ? contextEvaluations : List.of();
        buildSteps();
        buildInterpretation();
    }

    @Override
    public String getMetricType() {
        return METRIC_TYPE;
    }

    @Override
    public String getSimpleDescription() {
        return messages.get("contextRelevance.description");
    }

    private void buildSteps() {
        // Build step for each context evaluation
        for (int i = 0; i < contextEvaluations.size(); i++) {
            final ContextEvaluation eval = contextEvaluations.get(i);
            final String stepName = String.format("EvaluateRelevance_%d", i + 1);

            steps.add(StepExplanation.builder()
                    .stepName(stepName)
                    .stepNumber(i + 1)
                    .title(String.format("%s %d", messages.get("contextRelevance.step.evaluateContext.title"), i + 1))
                    .description(messages.get("contextRelevance.step.evaluateContext.desc"))
                    .inputData(truncateText(eval.getContext(), 200))
                    .outputSummary(String.format(
                            "%s: %d/2 (%.1f%%)",
                            messages.get("contextRelevance.rawScoreLabel"),
                            eval.getRawScore(),
                            eval.getNormalizedScore() * 100))
                    .items(List.of(ExplanationItem.builder()
                            .content(eval.getReasoning())
                            .passed(eval.getRawScore() >= 1)
                            .verdict(getRelevanceVerdict(eval.getRawScore()))
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

    private String getRelevanceVerdict(final int rawScore) {
        return switch (rawScore) {
            case 2 -> messages.get("contextRelevance.verdict.fullyRelevant");
            case 1 -> messages.get("contextRelevance.verdict.partiallyRelevant");
            default -> messages.get("contextRelevance.verdict.notRelevant");
        };
    }

    private void buildInterpretation() {
        final String formula = messages.get("contextRelevance.formula");
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
        if (contextEvaluations.isEmpty()) {
            return "N/A";
        }

        final StringBuilder sb = new StringBuilder();
        sb.append(messages.get("contextRelevance.avgLabel")).append(" = (");
        for (int i = 0; i < contextEvaluations.size(); i++) {
            if (i > 0) {
                sb.append(" + ");
            }
            sb.append(String.format("%.2f", contextEvaluations.get(i).getNormalizedScore()));
        }
        sb.append(") / ").append(contextEvaluations.size());
        sb.append(String.format(" = %.2f", score != null ? score : 0.0));
        return sb.toString();
    }

    private String getMeaning() {
        if (score == null) {
            return messages.get("common.scoreNotCalculated");
        }
        if (score >= 0.9) {
            return messages.get("contextRelevance.meaning.excellent");
        } else if (score >= 0.7) {
            return messages.get("contextRelevance.meaning.good");
        } else if (score >= 0.5) {
            return messages.get("contextRelevance.meaning.moderate");
        } else {
            return messages.get("contextRelevance.meaning.poor");
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
                        .description(messages.get("contextRelevance.scale.excellent"))
                        .current(score != null && score >= 0.9)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.good"))
                        .range("70-89%")
                        .description(messages.get("contextRelevance.scale.good"))
                        .current(score != null && score >= 0.7 && score < 0.9)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.moderate"))
                        .range("50-69%")
                        .description(messages.get("contextRelevance.scale.moderate"))
                        .current(score != null && score >= 0.5 && score < 0.7)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.poor"))
                        .range("0-49%")
                        .description(messages.get("contextRelevance.scale.poor"))
                        .current(score != null && score < 0.5)
                        .build());
    }

    /**
     * Represents a single context's relevance evaluation.
     */
    @Getter
    @Builder
    public static class ContextEvaluation {
        private final String context;
        private final int rawScore;
        private final double normalizedScore;
        private final String reasoning;
    }
}
