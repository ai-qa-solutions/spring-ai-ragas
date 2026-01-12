package ai.qa.solutions.allure.explanation;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

/**
 * Score explanation for ContextRecallMetric.
 * <p>
 * ContextRecall measures how well the retrieved contexts cover
 * the information in the reference answer. It checks if each
 * sentence from the reference can be found in the contexts.
 */
@Getter
public class ContextRecallExplanation extends AbstractScoreExplanation {

    private static final String METRIC_TYPE = "context-recall";

    private final String reference;
    private final String contexts;
    private final List<ReferenceClassification> classifications;
    private final int foundCount;
    private final int totalCount;

    @Builder
    public ContextRecallExplanation(
            final Double score,
            final String language,
            final String reference,
            final String contexts,
            final List<ReferenceClassification> classifications) {
        super(score, language);
        this.reference = reference != null ? reference : "";
        this.contexts = contexts != null ? contexts : "";
        this.classifications = classifications != null ? classifications : List.of();
        this.foundCount =
                (int) this.classifications.stream().filter(c -> c.found).count();
        this.totalCount = this.classifications.size();
        buildSteps();
        buildInterpretation();
    }

    @Override
    public String getMetricType() {
        return METRIC_TYPE;
    }

    @Override
    public String getSimpleDescription() {
        return messages.get("contextRecall.description");
    }

    private void buildSteps() {
        // Step 1: Classify reference sentences
        steps.add(StepExplanation.builder()
                .stepName("ClassifyStatements")
                .stepNumber(1)
                .title(messages.get("contextRecall.step1.title"))
                .description(messages.get("contextRecall.step1.desc"))
                .inputData(reference)
                .outputSummary(messages.get("contextRecall.step1.output", foundCount, totalCount))
                .items(classifications.stream()
                        .map((c) -> ExplanationItem.builder()
                                .content(c.statement)
                                .passed(c.found)
                                .verdict(c.found ? messages.get("verdict.found") : messages.get("verdict.missing"))
                                .reason(c.reason)
                                .index(classifications.indexOf(c) + 1)
                                .build())
                        .toList())
                .metadata(Map.of("contexts", contexts))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 2: Calculate score
        steps.add(StepExplanation.builder()
                .stepName("ComputeScore")
                .stepNumber(2)
                .title(messages.get("contextRecall.step2.title"))
                .description(messages.get("contextRecall.step2.desc"))
                .outputSummary(formatPercent(score))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());
    }

    private void buildInterpretation() {
        final String formula = messages.get("contextRecall.formula");

        // Score is aggregated across models, so show simplified calculation
        // Show N/A when no data to calculate
        final String calculation = totalCount > 0 ? formatPercent(score) : messages.get("common.na");

        interpretation = ScoreInterpretation.builder()
                .formula(formula)
                .calculation(calculation)
                .numerator(foundCount)
                .denominator(totalCount)
                .score(score)
                .scorePercent(formatPercent(score))
                .level(getLevelName(score))
                .meaning(getMeaning())
                .scaleLevels(createContextRecallScale())
                .currentLevelIndex(getCurrentLevelIndex(score))
                .build();
    }

    private String getMeaning() {
        if (score == null) {
            return messages.get("common.scoreNotCalculated");
        }
        if (score >= 0.9) {
            return messages.get("contextRecall.meaning.excellent");
        }
        if (score >= 0.7) {
            return messages.get("contextRecall.meaning.good");
        }
        if (score >= 0.5) {
            return messages.get("contextRecall.meaning.moderate");
        }
        return messages.get("contextRecall.meaning.poor");
    }

    private List<ScoreInterpretation.ScaleLevel> createContextRecallScale() {
        return List.of(
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.excellent"))
                        .range("90-100%")
                        .description(messages.get("contextRecall.scale.excellent"))
                        .current(score != null && score >= 0.9)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.good"))
                        .range("70-90%")
                        .description(messages.get("contextRecall.scale.good"))
                        .current(score != null && score >= 0.7 && score < 0.9)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.moderate"))
                        .range("50-70%")
                        .description(messages.get("contextRecall.scale.moderate"))
                        .current(score != null && score >= 0.5 && score < 0.7)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.poor"))
                        .range("0-50%")
                        .description(messages.get("contextRecall.scale.poor"))
                        .current(score != null && score < 0.5)
                        .build());
    }

    /**
     * Classification of a reference sentence.
     */
    @Builder
    @Getter
    public static class ReferenceClassification {
        private final String statement;
        private final boolean found;
        private final String reason;
    }
}
