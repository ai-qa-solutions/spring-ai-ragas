package ai.qa.solutions.metric.explanation;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Score explanation for ContextPrecisionMetric.
 * <p>
 * ContextPrecision measures the ranking quality of retrieved contexts.
 * It checks if relevant contexts appear earlier in the list using
 * Average Precision calculation.
 */
@Getter
public class ContextPrecisionExplanation extends AbstractScoreExplanation {

    private static final String METRIC_TYPE = "context-precision";

    private final String userInput;
    private final List<ContextRelevance> contexts;
    private final List<Double> precisionAtK;
    private final int relevantCount;

    @Builder
    public ContextPrecisionExplanation(
            final Double score,
            final String language,
            final String userInput,
            final List<ContextRelevance> contexts,
            final List<Double> precisionAtK) {
        super(score, language);
        this.userInput = userInput != null ? userInput : "";
        this.contexts = contexts != null ? contexts : List.of();
        this.precisionAtK = precisionAtK != null ? precisionAtK : List.of();
        this.relevantCount =
                (int) this.contexts.stream().filter(c -> c.relevant).count();
        buildSteps();
        buildInterpretation();
    }

    @Override
    public String getMetricType() {
        return METRIC_TYPE;
    }

    @Override
    public String getSimpleDescription() {
        return messages.get("contextPrecision.description");
    }

    private void buildSteps() {
        // Step 1: Evaluate each context against user question
        steps.add(StepExplanation.builder()
                .stepName("EvaluateContexts")
                .stepNumber(1)
                .title(messages.get("contextPrecision.step1.title"))
                .description(messages.get("contextPrecision.step1.desc"))
                .inputData(userInput)
                .outputSummary(messages.get("contextPrecision.step1.output", relevantCount, contexts.size()))
                .items(contexts.stream()
                        .map((c) -> ExplanationItem.builder()
                                .content(truncate(c.contextText, 100))
                                .passed(c.relevant)
                                .verdict(
                                        c.relevant
                                                ? messages.get("verdict.relevant")
                                                : messages.get("verdict.notRelevant"))
                                .reason(c.reason)
                                .index(c.position)
                                .build())
                        .toList())
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 2: Calculate Precision@K
        // Show precision at each position with cumulative relevant count
        final List<ExplanationItem> precisionItems = new ArrayList<>();
        int cumulativeRelevant = 0;
        for (int i = 0; i < contexts.size(); i++) {
            final boolean isRelevant = contexts.get(i).isRelevant();
            if (isRelevant) {
                cumulativeRelevant++;
            }
            final double precisionValue = i < precisionAtK.size() ? precisionAtK.get(i) : 0.0;
            final String precisionPercent = String.format("%.1f%%", precisionValue * 100);
            precisionItems.add(ExplanationItem.builder()
                    .content(String.format(
                            "K=%d: %d/%d relevant → %s", i + 1, cumulativeRelevant, i + 1, precisionPercent))
                    .numericValue(precisionValue)
                    .passed(isRelevant)
                    .verdict(isRelevant ? "+" : "−")
                    .index(i + 1)
                    .build());
        }
        steps.add(StepExplanation.builder()
                .stepName("CalculatePrecision")
                .stepNumber(2)
                .title(messages.get("contextPrecision.step2.title"))
                .description(messages.get("contextPrecision.step2.desc"))
                .outputSummary(messages.get("contextPrecision.step2.output", contexts.size(), relevantCount))
                .items(precisionItems)
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 3: Calculate Average Precision
        steps.add(StepExplanation.builder()
                .stepName("ComputeScore")
                .stepNumber(3)
                .title(messages.get("contextPrecision.step3.title"))
                .description(messages.get("contextPrecision.step3.desc"))
                .outputSummary(formatPercent(score))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());
    }

    private void buildInterpretation() {
        final String formula = messages.get("contextPrecision.formula");

        interpretation = ScoreInterpretation.builder()
                .formula(formula)
                .calculation(formatPercent(score))
                .numerator(relevantCount)
                .denominator(contexts.size())
                .score(score)
                .scorePercent(formatPercent(score))
                .level(getLevelName(score))
                .meaning(getMeaning())
                .scaleLevels(createStandardScale(score))
                .currentLevelIndex(getCurrentLevelIndex(score))
                .build();
    }

    private String getMeaning() {
        if (score == null) {
            return messages.get("common.scoreNotCalculated");
        }
        if (score >= 0.9) {
            return messages.get("contextPrecision.meaning.excellent");
        }
        if (score >= 0.7) {
            return messages.get("contextPrecision.meaning.good");
        }
        if (score >= 0.5) {
            return messages.get("contextPrecision.meaning.moderate");
        }
        return messages.get("contextPrecision.meaning.poor");
    }

    private String truncate(final String text, final int maxLen) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "...";
    }

    /**
     * Context relevance evaluation result.
     */
    @Builder
    @Getter
    public static class ContextRelevance {
        private final int position;
        private final String contextText;
        private final boolean relevant;
        private final String reason;
    }
}
