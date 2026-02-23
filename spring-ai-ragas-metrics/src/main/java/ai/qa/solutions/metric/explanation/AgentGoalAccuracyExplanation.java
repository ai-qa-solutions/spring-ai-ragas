package ai.qa.solutions.metric.explanation;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Score explanation for AgentGoalAccuracyMetric.
 * <p>
 * Agent Goal Accuracy evaluates whether an AI agent achieved its intended goal
 * based on multi-turn conversation analysis. The metric supports two modes:
 * <ul>
 *   <li>WITH_REFERENCE - compares outcome with provided expected goal</li>
 *   <li>WITHOUT_REFERENCE - infers goal from conversation and evaluates achievement</li>
 * </ul>
 * <p>
 * Returns a binary score: 1.0 if goal was achieved, 0.0 otherwise.
 */
@Getter
public class AgentGoalAccuracyExplanation extends AbstractScoreExplanation {

    private static final String METRIC_TYPE = "agent-goal-accuracy";

    private final String mode;
    private final String conversation;
    private final String referenceGoal;
    private final String inferredGoal;
    private final boolean goalAchieved;
    private final List<ModelStepResult> inferGoalModelResults;
    private final List<ModelStepResult> modelResults;
    private final boolean hasModelDisagreement;
    private final double agreementPercent;

    @Builder
    public AgentGoalAccuracyExplanation(
            final Double score,
            final String language,
            final String mode,
            final String conversation,
            final String referenceGoal,
            final String inferredGoal,
            final boolean goalAchieved,
            final List<ModelStepResult> inferGoalModelResults,
            final List<ModelStepResult> modelResults,
            final boolean hasModelDisagreement,
            final double agreementPercent) {
        super(score, language);
        this.mode = mode != null ? mode : "WITH_REFERENCE";
        this.conversation = conversation != null ? conversation : "";
        this.referenceGoal = referenceGoal != null ? referenceGoal : "";
        this.inferredGoal = inferredGoal;
        this.goalAchieved = goalAchieved;
        this.inferGoalModelResults = inferGoalModelResults != null ? inferGoalModelResults : List.of();
        this.modelResults = modelResults != null ? modelResults : List.of();
        this.hasModelDisagreement = hasModelDisagreement;
        this.agreementPercent = agreementPercent;
        buildSteps();
        buildInterpretation();
    }

    @Override
    public String getMetricType() {
        return METRIC_TYPE;
    }

    @Override
    public String getSimpleDescription() {
        return messages.get("agentGoalAccuracy.description");
    }

    private void buildSteps() {
        // Build items from model results for EvaluateOutcome/CompareOutcome step
        final List<ExplanationItem> evaluateItems = new ArrayList<>();
        for (final ModelStepResult result : modelResults) {
            if (result.isSuccess()) {
                evaluateItems.add(ExplanationItem.builder()
                        .content(result.getReasoning())
                        .passed(Boolean.TRUE.equals(result.getVerdict()))
                        .verdict(
                                Boolean.TRUE.equals(result.getVerdict())
                                        ? messages.get("agentGoalAccuracy.verdict.achieved")
                                        : messages.get("agentGoalAccuracy.verdict.notAchieved"))
                        .source(result.getModelId())
                        .build());
            }
        }

        if ("WITHOUT_REFERENCE".equals(mode)) {
            // Build items from model results for InferGoal step
            final List<ExplanationItem> inferGoalItems = new ArrayList<>();
            for (final ModelStepResult result : inferGoalModelResults) {
                if (result.isSuccess()) {
                    // Show inferred goal as content, reasoning as reason
                    final String goalText = result.getInferredGoal() != null ? result.getInferredGoal() : "";
                    final String reasoningText = result.getReasoning() != null ? result.getReasoning() : "";
                    inferGoalItems.add(ExplanationItem.builder()
                            .content(goalText)
                            .reason(reasoningText)
                            .passed(true)
                            .source(result.getModelId())
                            .build());
                }
            }

            // Step 1: Infer goal from conversation
            steps.add(StepExplanation.builder()
                    .stepName("InferGoal")
                    .stepNumber(1)
                    .title(messages.get("agentGoalAccuracy.step.inferGoal.title"))
                    .description(messages.get("agentGoalAccuracy.step.inferGoal.desc"))
                    .inputData(truncateConversation(conversation))
                    .outputSummary(inferredGoal != null ? inferredGoal : messages.get("common.notAvailable"))
                    .items(inferGoalItems)
                    .modelResults(inferGoalModelResults)
                    .hasModelDisagreement(false)
                    .agreementPercent(100.0)
                    .build());

            // Step 2: Evaluate outcome
            steps.add(StepExplanation.builder()
                    .stepName("EvaluateOutcome")
                    .stepNumber(2)
                    .title(messages.get("agentGoalAccuracy.step.evaluateOutcome.title"))
                    .description(messages.get("agentGoalAccuracy.step.evaluateOutcome.desc"))
                    .inputData(messages.get("agentGoalAccuracy.goal") + ": " + inferredGoal)
                    .outputSummary(getVerdictText())
                    .items(evaluateItems)
                    .modelResults(modelResults)
                    .hasModelDisagreement(hasModelDisagreement)
                    .agreementPercent(agreementPercent)
                    .build());
        } else {
            // WITH_REFERENCE mode - single step
            // Step 1: Compare outcome
            steps.add(StepExplanation.builder()
                    .stepName("CompareOutcome")
                    .stepNumber(1)
                    .title(messages.get("agentGoalAccuracy.step.compareOutcome.title"))
                    .description(messages.get("agentGoalAccuracy.step.compareOutcome.desc"))
                    .inputData(messages.get("agentGoalAccuracy.expectedOutcome") + ": " + referenceGoal)
                    .outputSummary(getVerdictText())
                    .items(evaluateItems)
                    .modelResults(modelResults)
                    .hasModelDisagreement(hasModelDisagreement)
                    .agreementPercent(agreementPercent)
                    .build());
        }
    }

    private void buildInterpretation() {
        final String formula = messages.get("agentGoalAccuracy.formula");
        final String calculation = goalAchieved ? "1.0" : "0.0";
        final String meaning = getMeaning();

        interpretation = ScoreInterpretation.builder()
                .formula(formula)
                .calculation(calculation)
                .score(score)
                .scorePercent(formatPercent(score))
                .level(
                        goalAchieved
                                ? messages.get("agentGoalAccuracy.level.achieved")
                                : messages.get("agentGoalAccuracy.level.notAchieved"))
                .meaning(meaning)
                .scaleLevels(createAgentGoalAccuracyScale())
                .currentLevelIndex(goalAchieved ? 0 : 1)
                .build();
    }

    private String getMeaning() {
        if (score == null) {
            return messages.get("common.scoreNotCalculated");
        }
        return goalAchieved
                ? messages.get("agentGoalAccuracy.meaning.achieved")
                : messages.get("agentGoalAccuracy.meaning.notAchieved");
    }

    private String getVerdictText() {
        return goalAchieved
                ? messages.get("agentGoalAccuracy.verdict.achieved")
                : messages.get("agentGoalAccuracy.verdict.notAchieved");
    }

    private String truncateConversation(final String text) {
        if (text == null) {
            return "";
        }
        if (text.length() <= 500) {
            return text;
        }
        return text.substring(0, 500) + "...";
    }

    private List<ScoreInterpretation.ScaleLevel> createAgentGoalAccuracyScale() {
        return List.of(
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("agentGoalAccuracy.level.achieved"))
                        .range("100%")
                        .description(messages.get("agentGoalAccuracy.scale.achieved"))
                        .current(goalAchieved)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("agentGoalAccuracy.level.notAchieved"))
                        .range("0%")
                        .description(messages.get("agentGoalAccuracy.scale.notAchieved"))
                        .current(!goalAchieved)
                        .build());
    }
}
