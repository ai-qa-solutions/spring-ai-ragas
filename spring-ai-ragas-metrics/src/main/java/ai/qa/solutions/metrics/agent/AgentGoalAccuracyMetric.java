package ai.qa.solutions.metrics.agent;

import ai.qa.solutions.execution.ModelResult;
import ai.qa.solutions.execution.MultiModelExecutor;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationContext;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationResult;
import ai.qa.solutions.execution.listener.dto.ModelExclusionEvent;
import ai.qa.solutions.metric.AbstractMultiModelMetric;
import ai.qa.solutions.sample.Sample;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.PromptTemplate;

/**
 * Agent Goal Accuracy Metric - Evaluates whether an agent achieved its intended goal.
 * <p>
 * This metric supports two modes:
 * <ul>
 *   <li>{@code WITH_REFERENCE} - Compares the conversation outcome with a provided reference goal</li>
 *   <li>{@code WITHOUT_REFERENCE} - Infers the goal from the conversation and evaluates if it was achieved</li>
 * </ul>
 * <p>
 * Returns a binary score: 1.0 if the goal was achieved, 0.0 otherwise.
 * <p>
 * Required sample fields:
 * <ul>
 *   <li>{@code messages} - The conversation history (list of Message records with role and content)</li>
 *   <li>{@code reference} - (Optional) The expected goal/outcome for WITH_REFERENCE mode</li>
 * </ul>
 */
@Slf4j
public class AgentGoalAccuracyMetric extends AbstractMultiModelMetric<AgentGoalAccuracyMetric.AgentGoalAccuracyConfig> {

    public static final String DEFAULT_INFER_GOAL_PROMPT =
            """
                    Analyze the following conversation and infer what the user's goal or intent was.

                    Conversation:
                    {conversation}

                    Instructions:
                    1. Carefully read through the entire conversation
                    2. Identify the user's primary goal or objective
                    3. Consider any implicit goals or expectations

                    Respond with a JSON object containing:
                    - inferredGoal: A clear description of the user's goal
                    - reasoning: Your explanation of how you determined this goal
                    """;

    public static final String DEFAULT_EVALUATE_OUTCOME_PROMPT =
            """
                    Evaluate whether the agent successfully achieved the specified goal based on the conversation.

                    Goal: {goal}

                    Conversation:
                    {conversation}

                    Instructions:
                    1. Review the conversation to understand the interaction
                    2. Determine if the agent's actions and responses successfully achieved the goal
                    3. Consider partial achievements - the goal must be fully achieved for a positive verdict

                    Respond with a JSON object containing:
                    - goalAchieved: true if the goal was fully achieved, false otherwise
                    - reasoning: Detailed explanation of your assessment
                    """;

    public static final String DEFAULT_COMPARE_OUTCOME_PROMPT =
            """
                    Compare the actual outcome of this conversation with the expected outcome.

                    Expected Outcome: {reference}

                    Conversation:
                    {conversation}

                    Instructions:
                    1. Analyze the conversation to determine what actually happened
                    2. Compare the actual outcome with the expected outcome
                    3. Determine if the expected outcome was achieved

                    Respond with a JSON object containing:
                    - goalAchieved: true if the expected outcome was achieved, false otherwise
                    - reasoning: Detailed explanation of your comparison
                    """;

    private final String inferGoalPrompt;
    private final String evaluateOutcomePrompt;
    private final String compareOutcomePrompt;

    @Builder(toBuilder = true)
    protected AgentGoalAccuracyMetric(
            final MultiModelExecutor executor,
            final String inferGoalPrompt,
            final String evaluateOutcomePrompt,
            final String compareOutcomePrompt) {
        super(executor);
        this.inferGoalPrompt = inferGoalPrompt != null ? inferGoalPrompt : DEFAULT_INFER_GOAL_PROMPT;
        this.evaluateOutcomePrompt =
                evaluateOutcomePrompt != null ? evaluateOutcomePrompt : DEFAULT_EVALUATE_OUTCOME_PROMPT;
        this.compareOutcomePrompt =
                compareOutcomePrompt != null ? compareOutcomePrompt : DEFAULT_COMPARE_OUTCOME_PROMPT;
    }

    @Override
    public Double singleTurnScore(final AgentGoalAccuracyConfig config, final Sample sample) {
        return singleTurnScoreAsync(config, sample).join();
    }

    @Override
    public CompletableFuture<Double> singleTurnScoreAsync(final AgentGoalAccuracyConfig config, final Sample sample) {
        final Instant startTime = Instant.now();
        final List<String> modelIds =
                config.models != null && !config.models.isEmpty() ? config.models : executor.getModelIds();

        // Validate input
        if (sample.getMessages() == null || sample.getMessages().isEmpty()) {
            log.warn("No messages provided for Agent Goal Accuracy evaluation");
            return CompletableFuture.completedFuture(null);
        }

        final EvaluationNotifier notifier = createEvaluationNotifier();
        final Mode mode = config.mode != null ? config.mode : Mode.WITH_REFERENCE;
        final int totalSteps = mode == Mode.WITH_REFERENCE ? 1 : 2;

        notifier.beforeMetricEvaluation(MetricEvaluationContext.builder()
                .metricName(getName())
                .sample(sample)
                .config(config)
                .modelIds(modelIds)
                .totalSteps(totalSteps)
                .metadata(Map.of("sample", sample, "config", config, "mode", mode))
                .build());

        return executor.runAsync(() -> {
            final String conversation = formatConversation(sample.getMessages());

            if (mode == Mode.WITH_REFERENCE) {
                return evaluateWithReference(config, sample, conversation, modelIds, notifier, startTime);
            } else {
                return evaluateWithoutReference(config, sample, conversation, modelIds, notifier, startTime);
            }
        });
    }

    private Double evaluateWithReference(
            final AgentGoalAccuracyConfig config,
            final Sample sample,
            final String conversation,
            final List<String> modelIds,
            final EvaluationNotifier notifier,
            final Instant startTime) {

        if (sample.getReference() == null || sample.getReference().isEmpty()) {
            log.warn("No reference provided for WITH_REFERENCE mode - falling back to WITHOUT_REFERENCE");
            return evaluateWithoutReference(config, sample, conversation, modelIds, notifier, startTime);
        }

        // ========== Step 1: Compare Outcome ==========
        notifier.beforeStep("CompareOutcome", 0, 1);

        final String prompt = renderCompareOutcomePrompt(sample.getReference(), conversation);
        final Map<String, Double> modelScores = new HashMap<>();
        final List<String> excludedModels = new ArrayList<>();
        final List<ModelResult<GoalComparisonResponse>> results =
                executor.executeLlm(modelIds, prompt, GoalComparisonResponse.class);

        for (final ModelResult<GoalComparisonResponse> result : results) {
            if (result.isSuccess()) {
                final double score = result.result().getScore();
                modelScores.put(result.modelId(), score);
            } else {
                excludedModels.add(result.modelId());
                notifier.onModelExcluded(ModelExclusionEvent.builder()
                        .modelId(result.modelId())
                        .failedStepName("CompareOutcome")
                        .failedStepIndex(0)
                        .cause(result.error())
                        .build());
            }
        }

        notifier.afterLlmStep("CompareOutcome", 0, 1, prompt, results);

        if (modelScores.isEmpty()) {
            throw new IllegalStateException("All models failed at step CompareOutcome for metric: " + getName());
        }

        final double aggregatedScore = aggregate(modelScores);
        final Duration duration = Duration.between(startTime, Instant.now());

        notifier.afterMetricEvaluation(MetricEvaluationResult.builder()
                .metricName(getName())
                .aggregatedScore(aggregatedScore)
                .modelScores(modelScores)
                .excludedModels(excludedModels)
                .totalDuration(duration)
                .metadata(Map.of("sample", sample, "config", config, "mode", Mode.WITH_REFERENCE))
                .build());

        return aggregatedScore;
    }

    private Double evaluateWithoutReference(
            final AgentGoalAccuracyConfig config,
            final Sample sample,
            final String conversation,
            final List<String> modelIds,
            final EvaluationNotifier notifier,
            final Instant startTime) {

        // ========== Step 1: Infer Goal ==========
        notifier.beforeStep("InferGoal", 0, 2);

        final String inferPrompt = renderInferGoalPrompt(conversation);
        final List<ModelResult<InferredGoalResponse>> inferResults =
                executor.executeLlm(modelIds, inferPrompt, InferredGoalResponse.class);

        String inferredGoal = null;
        final List<String> excludedModels = new ArrayList<>();

        for (final ModelResult<InferredGoalResponse> result : inferResults) {
            if (result.isSuccess() && result.result().inferredGoal() != null) {
                inferredGoal = result.result().inferredGoal();
                break; // Use first successful result
            } else if (!result.isSuccess()) {
                excludedModels.add(result.modelId());
                notifier.onModelExcluded(ModelExclusionEvent.builder()
                        .modelId(result.modelId())
                        .failedStepName("InferGoal")
                        .failedStepIndex(0)
                        .cause(result.error())
                        .build());
            }
        }

        notifier.afterLlmStep("InferGoal", 0, 2, inferPrompt, inferResults);

        if (inferredGoal == null) {
            throw new IllegalStateException("Failed to infer goal at step InferGoal for metric: " + getName());
        }

        // ========== Step 2: Evaluate Outcome ==========
        notifier.beforeStep("EvaluateOutcome", 1, 2);

        final String evalPrompt = renderEvaluateOutcomePrompt(inferredGoal, conversation);
        final Map<String, Double> modelScores = new HashMap<>();
        final List<ModelResult<GoalComparisonResponse>> evalResults =
                executor.executeLlm(modelIds, evalPrompt, GoalComparisonResponse.class);

        for (final ModelResult<GoalComparisonResponse> result : evalResults) {
            if (result.isSuccess()) {
                final double score = result.result().getScore();
                modelScores.put(result.modelId(), score);
            } else {
                if (!excludedModels.contains(result.modelId())) {
                    excludedModels.add(result.modelId());
                }
                notifier.onModelExcluded(ModelExclusionEvent.builder()
                        .modelId(result.modelId())
                        .failedStepName("EvaluateOutcome")
                        .failedStepIndex(1)
                        .cause(result.error())
                        .build());
            }
        }

        notifier.afterLlmStep("EvaluateOutcome", 1, 2, evalPrompt, evalResults);

        if (modelScores.isEmpty()) {
            throw new IllegalStateException("All models failed at step EvaluateOutcome for metric: " + getName());
        }

        final double aggregatedScore = aggregate(modelScores);
        final Duration duration = Duration.between(startTime, Instant.now());

        notifier.afterMetricEvaluation(MetricEvaluationResult.builder()
                .metricName(getName())
                .aggregatedScore(aggregatedScore)
                .modelScores(modelScores)
                .excludedModels(excludedModels)
                .totalDuration(duration)
                .metadata(Map.of(
                        "sample", sample,
                        "config", config,
                        "mode", Mode.WITHOUT_REFERENCE,
                        "inferredGoal", inferredGoal))
                .build());

        return aggregatedScore;
    }

    private String formatConversation(final List<Sample.Message> messages) {
        return messages.stream()
                .map(m -> String.format("[%s]: %s", m.role().toUpperCase(), m.content()))
                .collect(Collectors.joining("\n"));
    }

    private String renderInferGoalPrompt(final String conversation) {
        return PromptTemplate.builder()
                .template(this.inferGoalPrompt)
                .variables(Map.of("conversation", conversation))
                .build()
                .render();
    }

    private String renderEvaluateOutcomePrompt(final String goal, final String conversation) {
        return PromptTemplate.builder()
                .template(this.evaluateOutcomePrompt)
                .variables(Map.of("goal", goal, "conversation", conversation))
                .build()
                .render();
    }

    private String renderCompareOutcomePrompt(final String reference, final String conversation) {
        return PromptTemplate.builder()
                .template(this.compareOutcomePrompt)
                .variables(Map.of("reference", reference, "conversation", conversation))
                .build()
                .render();
    }

    /**
     * Response DTO for goal inference.
     */
    public record InferredGoalResponse(
            @JsonPropertyDescription("The inferred goal or intent of the user based on the conversation")
                    String inferredGoal,
            @JsonPropertyDescription("Explanation of how the goal was determined from the conversation")
                    String reasoning) {}

    /**
     * Response DTO for goal comparison/evaluation.
     */
    public record GoalComparisonResponse(
            @JsonPropertyDescription("True if the goal was achieved, false otherwise") Boolean goalAchieved,
            @JsonPropertyDescription("Detailed explanation of the assessment") String reasoning) {
        public Double getScore() {
            return goalAchieved != null && goalAchieved ? 1.0 : 0.0;
        }
    }

    /**
     * Evaluation mode for the metric.
     */
    public enum Mode {
        /** Compare with provided reference goal. */
        WITH_REFERENCE,
        /** Infer goal from conversation and evaluate achievement. */
        WITHOUT_REFERENCE
    }

    @Data
    @Builder
    public static class AgentGoalAccuracyConfig implements MetricConfiguration {
        @Singular
        private List<String> models;

        /** Evaluation mode: WITH_REFERENCE or WITHOUT_REFERENCE. */
        @Builder.Default
        private Mode mode = Mode.WITH_REFERENCE;
    }
}
