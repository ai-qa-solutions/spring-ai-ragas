package ai.qa.solutions.metric.explanation;

import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    private final String aiResponse;
    private final Map<String, Integer> modelScores;
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
            final String aiResponse,
            final Map<String, Integer> modelScores,
            final int rawScore,
            final int minScore,
            final int maxScore,
            final String reasoning) {
        super(score, language);
        this.criteriaName = criteriaName != null ? criteriaName : messages.get("simpleCriteria.defaultCriteria");
        this.criteriaDefinition = criteriaDefinition != null ? criteriaDefinition : "";
        this.aiResponse = aiResponse != null ? aiResponse : "";
        this.modelScores = modelScores != null ? modelScores : Map.of();
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
        // Step 1: Show the criteria and AI response being evaluated
        steps.add(StepExplanation.builder()
                .stepName("DefineCriteria")
                .stepNumber(1)
                .title(messages.get("simpleCriteria.step1.title"))
                .description(messages.get("simpleCriteria.step1.desc"))
                .outputSummary(criteriaDefinition)
                .inputData(aiResponse)
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

        // Build calculation showing the actual score normalization
        // The score is already correctly calculated by the metric, just show the formula with actual values
        final String calculation;
        if (modelScores.size() > 1) {
            // Multi-model: show model scores and the final result
            final String modelScoresList = modelScores.entrySet().stream()
                    .map(e -> e.getValue().toString())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("-");
            calculation = String.format(
                    Locale.US,
                    "%s (%d %s) → %s",
                    modelScoresList,
                    modelScores.size(),
                    messages.get("common.models"),
                    formatPercent(score));
        } else {
            calculation = formatPercent(score);
        }

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
