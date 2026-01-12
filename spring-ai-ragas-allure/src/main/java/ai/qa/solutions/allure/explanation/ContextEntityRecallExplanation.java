package ai.qa.solutions.allure.explanation;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Score explanation for ContextEntityRecallMetric.
 * <p>
 * ContextEntityRecall measures how well the retrieved contexts cover
 * the entities mentioned in the reference answer.
 */
@Getter
public class ContextEntityRecallExplanation extends AbstractScoreExplanation {

    private static final String METRIC_TYPE = "context-entity-recall";

    private final String reference;
    private final String contexts;
    private final List<String> referenceEntities;
    private final List<String> contextEntities;
    private final List<String> foundEntities;
    private final List<String> missingEntities;

    @Builder
    public ContextEntityRecallExplanation(
            final Double score,
            final String language,
            final String reference,
            final String contexts,
            final List<String> referenceEntities,
            final List<String> contextEntities,
            final List<String> foundEntities,
            final List<String> missingEntities) {
        super(score, language);
        this.reference = reference != null ? reference : "";
        this.contexts = contexts != null ? contexts : "";
        this.referenceEntities = referenceEntities != null ? referenceEntities : List.of();
        this.contextEntities = contextEntities != null ? contextEntities : List.of();
        this.foundEntities = foundEntities != null ? foundEntities : List.of();
        this.missingEntities = missingEntities != null ? missingEntities : List.of();
        buildSteps();
        buildInterpretation();
    }

    @Override
    public String getMetricType() {
        return METRIC_TYPE;
    }

    @Override
    public String getSimpleDescription() {
        return messages.get("contextEntityRecall.description");
    }

    private void buildSteps() {
        // Step 1: Extract entities from reference
        steps.add(StepExplanation.builder()
                .stepName("ExtractReferenceEntities")
                .stepNumber(1)
                .title(messages.get("contextEntityRecall.step1.title"))
                .description(messages.get("contextEntityRecall.step1.desc"))
                .inputData(reference)
                .outputSummary(messages.get("contextEntityRecall.step1.output", referenceEntities.size()))
                .items(referenceEntities.stream()
                        .map((e) -> ExplanationItem.builder()
                                .content(e)
                                .index(referenceEntities.indexOf(e) + 1)
                                .build())
                        .toList())
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 2: Extract entities from contexts
        steps.add(StepExplanation.builder()
                .stepName("ExtractContextEntities")
                .stepNumber(2)
                .title(messages.get("contextEntityRecall.step2.title"))
                .description(messages.get("contextEntityRecall.step2.desc"))
                .inputData(contexts)
                .outputSummary(messages.get("contextEntityRecall.step1.output", contextEntities.size()))
                .items(contextEntities.stream()
                        .map((e) -> ExplanationItem.builder()
                                .content(e)
                                .index(contextEntities.indexOf(e) + 1)
                                .build())
                        .toList())
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 3: Compare entities
        steps.add(StepExplanation.builder()
                .stepName("CompareEntities")
                .stepNumber(3)
                .title(messages.get("contextEntityRecall.step3.title"))
                .description(messages.get("contextEntityRecall.step3.desc"))
                .outputSummary(messages.get(
                        "contextEntityRecall.step3.output", foundEntities.size(), referenceEntities.size()))
                .items(referenceEntities.stream()
                        .map((e) -> {
                            final boolean found = foundEntities.contains(e);
                            return ExplanationItem.builder()
                                    .content(e)
                                    .passed(found)
                                    .verdict(found ? messages.get("verdict.found") : messages.get("verdict.missing"))
                                    .index(referenceEntities.indexOf(e) + 1)
                                    .build();
                        })
                        .toList())
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 4: Calculate score
        steps.add(StepExplanation.builder()
                .stepName("ComputeScore")
                .stepNumber(4)
                .title(messages.get("contextEntityRecall.step4.title"))
                .description(messages.get("contextEntityRecall.step4.desc"))
                .outputSummary(formatPercent(score))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());
    }

    private void buildInterpretation() {
        final String formula = messages.get("contextEntityRecall.formula");

        final int total = referenceEntities.size();
        final int found = foundEntities.size();
        // Score is aggregated across models, so show simplified calculation
        final String calculation = formatPercent(score);

        interpretation = ScoreInterpretation.builder()
                .formula(formula)
                .calculation(calculation)
                .numerator(found)
                .denominator(total)
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
            return messages.get("contextEntityRecall.meaning.excellent");
        }
        if (score >= 0.7) {
            return messages.get("contextEntityRecall.meaning.good");
        }
        if (score >= 0.5) {
            return messages.get("contextEntityRecall.meaning.moderate");
        }
        return messages.get("contextEntityRecall.meaning.poor");
    }
}
