package ai.qa.solutions.allure.explanation;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Score explanation for AspectCriticMetric.
 * <p>
 * AspectCritic provides binary (PASS/FAIL) evaluation based on user-defined aspects.
 * The user can define ANY aspect/criteria (not just Harmlessness).
 * Score is 1.0 for PASS, 0.0 for FAIL.
 */
@Getter
public class AspectCriticExplanation extends AbstractScoreExplanation {

    private static final String METRIC_TYPE = "aspect-critic";

    private final String aspectName;
    private final String aspectDefinition;
    private final boolean passed;
    private final String reasoning;

    @Builder
    public AspectCriticExplanation(
            final Double score,
            final String language,
            final String aspectName,
            final String aspectDefinition,
            final boolean passed,
            final String reasoning) {
        super(score, language);
        this.aspectName = aspectName != null ? aspectName : messages.get("aspectCritic.defaultAspect");
        this.aspectDefinition = aspectDefinition != null ? aspectDefinition : "";
        this.passed = passed;
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
        return messages.get("aspectCritic.description");
    }

    @Override
    public boolean hasFixedInterpretation() {
        return true;
    }

    private void buildSteps() {
        // Step 1: Show the aspect being evaluated
        steps.add(StepExplanation.builder()
                .stepName("DefineAspect")
                .stepNumber(1)
                .title(messages.get("aspectCritic.step1.title"))
                .description(messages.get("aspectCritic.step1.desc"))
                .outputSummary(aspectName)
                .items(List.of(ExplanationItem.builder()
                        .content(aspectDefinition)
                        .index(1)
                        .build()))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 2: Evaluation result
        steps.add(StepExplanation.builder()
                .stepName("EvaluateAspect")
                .stepNumber(2)
                .title(messages.get("aspectCritic.step2.title"))
                .description(messages.get("aspectCritic.step2.desc"))
                .outputSummary(passed ? messages.get("verdict.pass") : messages.get("verdict.fail"))
                .items(List.of(ExplanationItem.builder()
                        .content(reasoning)
                        .passed(passed)
                        .verdict(passed ? messages.get("verdict.pass") : messages.get("verdict.fail"))
                        .index(1)
                        .build()))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 3: Score calculation
        steps.add(StepExplanation.builder()
                .stepName("ComputeScore")
                .stepNumber(3)
                .title(messages.get("aspectCritic.step3.title"))
                .description(messages.get("aspectCritic.step3.desc"))
                .outputSummary(formatPercent(score))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());
    }

    private void buildInterpretation() {
        final String formula = "PASS → 1.0, FAIL → 0.0";
        final String calculation = passed ? "PASS → 1.0" : "FAIL → 0.0";

        final String meaning = passed
                ? messages.get("aspectCritic.meaning.pass", aspectName)
                : messages.get("aspectCritic.meaning.fail", aspectName);

        interpretation = ScoreInterpretation.builder()
                .formula(formula)
                .calculation(calculation)
                .score(score)
                .scorePercent(formatPercent(score))
                .level(passed ? messages.get("verdict.pass") : messages.get("verdict.fail"))
                .isGood(passed)
                .meaning(meaning)
                .scaleLevels(createBinaryScale())
                .currentLevelIndex(passed ? 0 : 1)
                .build();
    }

    private List<ScoreInterpretation.ScaleLevel> createBinaryScale() {
        return List.of(
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("verdict.pass"))
                        .range("1.0")
                        .description(messages.get("aspectCritic.scale.pass"))
                        .current(passed)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("verdict.fail"))
                        .range("0.0")
                        .description(messages.get("aspectCritic.scale.fail"))
                        .current(!passed)
                        .build());
    }
}
