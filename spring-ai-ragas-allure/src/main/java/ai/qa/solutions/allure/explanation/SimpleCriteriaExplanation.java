package ai.qa.solutions.allure.explanation;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Score explanation for SimpleCriteriaScoreMetric.
 * <p>
 * SimpleCriteria provides continuous scale (0-1) evaluation based on
 * user-defined criteria. The interpretation depends on user requirements.
 * <p>
 * This is a relative metric - we don't judge good/bad, just show the result.
 */
@Getter
public class SimpleCriteriaExplanation extends AbstractScoreExplanation {

    private static final String METRIC_TYPE = "simple-criteria";

    private final String criteriaName;
    private final String criteriaDefinition;
    private final int rawScore;
    private final int minScore;
    private final int maxScore;
    private final String reasoning;

    @Builder
    public SimpleCriteriaExplanation(
            final Double score,
            final String language,
            final String criteriaName,
            final String criteriaDefinition,
            final int rawScore,
            final int minScore,
            final int maxScore,
            final String reasoning) {
        super(score, language);
        this.criteriaName = criteriaName != null ? criteriaName : messages.get("simpleCriteria.defaultCriteria");
        this.criteriaDefinition = criteriaDefinition != null ? criteriaDefinition : "";
        this.rawScore = rawScore;
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.reasoning = reasoning != null ? reasoning : "";
        buildSteps();
        buildInterpretation();
    }

    @Override
    public String getMetricType() {
        return METRIC_TYPE;
    }

    @Override
    public String getSimpleDescription() {
        return messages.get("simpleCriteria.description");
    }

    private void buildSteps() {
        // Step 1: Show the criteria
        steps.add(StepExplanation.builder()
                .stepName("DefineCriteria")
                .stepNumber(1)
                .title(messages.get("simpleCriteria.step1.title"))
                .description(messages.get("simpleCriteria.step1.desc"))
                .outputSummary(criteriaName)
                .items(List.of(ExplanationItem.builder()
                        .content(criteriaDefinition)
                        .index(1)
                        .build()))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 2: LLM evaluation
        steps.add(StepExplanation.builder()
                .stepName("EvaluateCriteria")
                .stepNumber(2)
                .title(messages.get("simpleCriteria.step2.title"))
                .description(messages.get("simpleCriteria.step2.desc", minScore, maxScore))
                .outputSummary(rawScore + " / " + maxScore)
                .items(List.of(ExplanationItem.builder()
                        .content(reasoning)
                        .numericValue((double) rawScore)
                        .index(1)
                        .build()))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 3: Normalize score
        final String normFormula =
                String.format("(%d - %d) / (%d - %d) = %.4f", rawScore, minScore, maxScore, minScore, score);
        steps.add(StepExplanation.builder()
                .stepName("ComputeScore")
                .stepNumber(3)
                .title(messages.get("simpleCriteria.step3.title"))
                .description(messages.get("simpleCriteria.step3.desc"))
                .outputSummary(normFormula + " = " + formatPercent(score))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());
    }

    private void buildInterpretation() {
        final String formula =
                String.format("(%s - %d) / (%d - %d)", messages.get("common.score"), minScore, maxScore, minScore);

        // Score is aggregated across models, so show simplified calculation
        final String calculation = formatPercent(score);

        final String meaning = messages.get("simpleCriteria.meaning", rawScore, maxScore, criteriaName);

        interpretation = ScoreInterpretation.builder()
                .formula(formula)
                .calculation(calculation)
                .numerator(rawScore - minScore)
                .denominator(maxScore - minScore)
                .score(score)
                .scorePercent(formatPercent(score))
                .level(String.format("%d/%d", rawScore, maxScore))
                .isGood(null) // No fixed interpretation
                .meaning(meaning)
                .scaleLevels(List.of()) // No predefined scale
                .build();
    }
}
