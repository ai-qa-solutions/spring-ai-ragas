package ai.qa.solutions.allure.explanation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

/**
 * Score explanation for AspectCriticMetric.
 * <p>
 * AspectCritic provides binary (PASS/FAIL) evaluation based on user-defined aspects.
 * The user can define ANY aspect/criteria (not just Harmlessness).
 * Score is 1.0 for PASS, 0.0 for FAIL.
 * <p>
 * Supports strictness parameter for self-consistency via majority voting.
 */
@Getter
public class AspectCriticExplanation extends AbstractScoreExplanation {

    private static final String METRIC_TYPE = "aspect-critic";

    private final String aspectName;
    private final String aspectDefinition;
    private final String aiResponse;
    private final boolean passed;
    private final String reasoning;
    private final int strictness;
    private final Map<String, List<Boolean>> modelIterationResults;

    @Builder
    public AspectCriticExplanation(
            final Double score,
            final String language,
            final String aspectName,
            final String aspectDefinition,
            final String aiResponse,
            final boolean passed,
            final String reasoning,
            final int strictness,
            final Map<String, List<Boolean>> modelIterationResults) {
        super(score, language);
        this.aspectName = aspectName != null ? aspectName : messages.get("aspectCritic.defaultAspect");
        this.aspectDefinition = aspectDefinition != null ? aspectDefinition : "";
        this.aiResponse = aiResponse != null ? aiResponse : "";
        this.passed = passed;
        this.reasoning = reasoning != null ? reasoning : "";
        this.strictness = strictness > 0 ? strictness : 1;
        this.modelIterationResults = modelIterationResults != null ? modelIterationResults : Map.of();
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
        int stepNumber = 1;

        // Step 1: Show the aspect being evaluated and the AI response
        steps.add(StepExplanation.builder()
                .stepName("DefineAspect")
                .stepNumber(stepNumber++)
                .title(messages.get("aspectCritic.step1.title"))
                .description(messages.get("aspectCritic.step1.desc"))
                .outputSummary(aspectDefinition)
                .inputData(aiResponse)
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 2: Evaluation result
        steps.add(StepExplanation.builder()
                .stepName("EvaluateAspect")
                .stepNumber(stepNumber++)
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

        // Step 3: Majority voting (only if strictness > 1)
        if (strictness > 1 && !modelIterationResults.isEmpty()) {
            steps.add(StepExplanation.builder()
                    .stepName("MajorityVoting")
                    .stepNumber(stepNumber++)
                    .title(messages.get("aspectCritic.step3.majority.title"))
                    .description(messages.get("aspectCritic.step3.majority.desc", strictness))
                    .items(buildMajorityVotingItems())
                    .hasModelDisagreement(hasModelDisagreement())
                    .agreementPercent(calculateAgreementPercent())
                    .build());
        }

        // Step 4 (or 3): Score calculation
        steps.add(StepExplanation.builder()
                .stepName("ComputeScore")
                .stepNumber(stepNumber)
                .title(messages.get("aspectCritic.step3.title"))
                .description(messages.get("aspectCritic.step3.desc"))
                .outputSummary(formatPercent(score))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());
    }

    private List<ExplanationItem> buildMajorityVotingItems() {
        final List<ExplanationItem> items = new ArrayList<>();
        int index = 1;

        for (final Map.Entry<String, List<Boolean>> entry : modelIterationResults.entrySet()) {
            final String modelId = entry.getKey();
            final List<Boolean> iterations = entry.getValue();
            final long passCount =
                    iterations.stream().filter(Boolean::booleanValue).count();
            final long failCount = iterations.size() - passCount;
            final boolean modelPassed = passCount > failCount;

            // Format: "model: 3/4 PASS → PASS" or "model: 1/4 PASS → FAIL"
            final String votingSummary = String.format(
                    "%d/%d %s → %s",
                    passCount,
                    iterations.size(),
                    messages.get("verdict.pass"),
                    modelPassed ? messages.get("verdict.pass") : messages.get("verdict.fail"));

            items.add(ExplanationItem.builder()
                    .content(modelId)
                    .passed(modelPassed)
                    .verdict(votingSummary)
                    .index(index++)
                    .build());
        }

        return items;
    }

    private boolean hasModelDisagreement() {
        if (modelIterationResults.isEmpty()) {
            return false;
        }
        // Check if any model had mixed results across iterations
        return modelIterationResults.values().stream().anyMatch(iterations -> {
            final long passCount =
                    iterations.stream().filter(Boolean::booleanValue).count();
            return passCount > 0 && passCount < iterations.size();
        });
    }

    private double calculateAgreementPercent() {
        if (modelIterationResults.isEmpty()) {
            return 100.0;
        }
        // Calculate average agreement within each model's iterations
        double totalAgreement = 0;
        for (final List<Boolean> iterations : modelIterationResults.values()) {
            final long passCount =
                    iterations.stream().filter(Boolean::booleanValue).count();
            final double majorityPercent =
                    Math.max(passCount, iterations.size() - passCount) / (double) iterations.size() * 100;
            totalAgreement += majorityPercent;
        }
        return totalAgreement / modelIterationResults.size();
    }

    private void buildInterpretation() {
        final String formula = "PASS → 1.0, FAIL → 0.0";

        // Count PASS/FAIL across all models for explicit calculation
        int passCount = 0;
        int failCount = 0;
        for (final List<Boolean> iterations : modelIterationResults.values()) {
            // For each model, determine final verdict via majority voting
            final long passVotes =
                    iterations.stream().filter(Boolean::booleanValue).count();
            if (passVotes > iterations.size() / 2) {
                passCount++;
            } else {
                failCount++;
            }
        }
        final int totalModels = passCount + failCount;

        // Build explicit calculation string
        final String calculation;
        if (totalModels > 1) {
            // Multi-model: show "N PASS + M FAIL = N/total = X%"
            calculation = String.format(
                    "%d %s + %d %s = %d/%d = %.2f",
                    passCount,
                    messages.get("verdict.pass"),
                    failCount,
                    messages.get("verdict.fail"),
                    passCount,
                    totalModels,
                    score != null ? score : 0.0);
        } else {
            // Single model: show simple "PASS → 1.0" or "FAIL → 0.0"
            calculation = passed ? "PASS → 1.0" : "FAIL → 0.0";
        }

        final String meaning = passed
                ? messages.get("aspectCritic.meaning.pass", aspectName)
                : messages.get("aspectCritic.meaning.fail", aspectName);

        interpretation = ScoreInterpretation.builder()
                .formula(formula)
                .calculation(calculation)
                .numerator(totalModels > 1 ? passCount : null)
                .denominator(totalModels > 1 ? totalModels : null)
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
