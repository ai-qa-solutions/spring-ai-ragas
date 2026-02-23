package ai.qa.solutions.metrics.nvidia;

import ai.qa.solutions.execution.ModelResult;
import ai.qa.solutions.execution.MultiModelExecutor;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationContext;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationResult;
import ai.qa.solutions.execution.listener.dto.ModelExclusionEvent;
import ai.qa.solutions.execution.listener.dto.StepResults;
import ai.qa.solutions.execution.listener.dto.StepType;
import ai.qa.solutions.metric.AbstractMultiModelMetric;
import ai.qa.solutions.metric.metadata.AnswerAccuracyMetadata;
import ai.qa.solutions.sample.Sample;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.PromptTemplate;

/**
 * Answer Accuracy Metric - NVIDIA-style evaluation of answer accuracy against reference.
 * <p>
 * This metric evaluates whether the AI response accurately matches the reference answer
 * using a 0-2 scoring scale that is normalized to 0-1.
 * <p>
 * Scoring scale:
 * <ul>
 *   <li>0 - Incorrect: Response is factually wrong or contradicts the reference</li>
 *   <li>1 - Partially correct: Response is partially correct but incomplete or has minor errors</li>
 *   <li>2 - Fully correct: Response accurately matches the reference answer</li>
 * </ul>
 * <p>
 * Optional dual-judge mode performs a confirmation judgment for higher reliability.
 * <p>
 * Required sample fields:
 * <ul>
 *   <li>{@code response} - The AI response to evaluate</li>
 *   <li>{@code reference} - The ground truth reference answer</li>
 * </ul>
 */
@Slf4j
public class AnswerAccuracyMetric extends AbstractMultiModelMetric<AnswerAccuracyMetric.AnswerAccuracyConfig> {

    public static final String DEFAULT_INITIAL_JUDGMENT_PROMPT =
            """
                    Compare the AI response with the reference answer and evaluate its accuracy.

                    AI Response:
                    {response}

                    Reference Answer:
                    {reference}

                    Instructions:
                    1. Compare the factual content of the response with the reference
                    2. Consider completeness, correctness, and relevance
                    3. Rate using the following scale:
                       - 0: Incorrect - Response is factually wrong or contradicts the reference
                       - 1: Partially correct - Response is partially correct but incomplete or has minor errors
                       - 2: Fully correct - Response accurately matches the reference answer

                    Respond with a JSON object containing:
                    - score: Integer from 0 to 2 representing the accuracy level
                    - reasoning: Brief explanation of the accuracy assessment
                    """;

    public static final String DEFAULT_CONFIRMATION_JUDGMENT_PROMPT =
            """
                    Review this accuracy assessment and confirm or adjust the score.

                    AI Response:
                    {response}

                    Reference Answer:
                    {reference}

                    Initial Assessment:
                    Score: {initialScore}
                    Reasoning: {initialReasoning}

                    Instructions:
                    1. Review whether the initial assessment is correct
                    2. Consider if any aspects were missed or incorrectly evaluated
                    3. Provide your final score (0-2) and reasoning

                    Respond with a JSON object containing:
                    - score: Integer from 0 to 2 (your final score)
                    - reasoning: Brief explanation of your assessment
                    - confirmedInitial: Boolean indicating if you agree with the initial assessment
                    """;

    private final String initialJudgmentPrompt;
    private final String confirmationJudgmentPrompt;

    @Builder(toBuilder = true)
    protected AnswerAccuracyMetric(
            final MultiModelExecutor executor,
            final String initialJudgmentPrompt,
            final String confirmationJudgmentPrompt) {
        super(executor);
        this.initialJudgmentPrompt =
                initialJudgmentPrompt != null ? initialJudgmentPrompt : DEFAULT_INITIAL_JUDGMENT_PROMPT;
        this.confirmationJudgmentPrompt =
                confirmationJudgmentPrompt != null ? confirmationJudgmentPrompt : DEFAULT_CONFIRMATION_JUDGMENT_PROMPT;
    }

    @Override
    public Double singleTurnScore(final AnswerAccuracyConfig config, final Sample sample) {
        return singleTurnScoreAsync(config, sample).join();
    }

    @Override
    public CompletableFuture<Double> singleTurnScoreAsync(final AnswerAccuracyConfig config, final Sample sample) {
        final Instant startTime = Instant.now();
        final List<String> modelIds =
                config.models != null && !config.models.isEmpty() ? config.models : executor.getModelIds();

        // Validate input
        if (sample.getResponse() == null || sample.getResponse().isEmpty()) {
            log.warn("No response provided for Answer Accuracy evaluation");
            return CompletableFuture.completedFuture(null);
        }

        if (sample.getReference() == null || sample.getReference().isEmpty()) {
            log.warn("No reference provided for Answer Accuracy evaluation");
            return CompletableFuture.completedFuture(null);
        }

        final EvaluationNotifier notifier = createEvaluationNotifier();
        final int totalSteps = config.useDualJudge ? 2 : 1;

        notifier.beforeMetricEvaluation(MetricEvaluationContext.builder()
                .metricName(getName())
                .sample(sample)
                .config(config)
                .modelIds(modelIds)
                .totalSteps(totalSteps)
                .build());

        return executor.runAsync(() -> {
            final String response = sample.getResponse();
            final String reference = sample.getReference();

            final List<StepResults> accumulatedSteps = new ArrayList<>();
            final List<ModelExclusionEvent> accumulatedExclusions = new ArrayList<>();
            final List<String> excludedModels = new ArrayList<>();

            // Step 1: Initial judgment
            final String initialPrompt = renderInitialJudgmentPrompt(response, reference);
            final Map<String, AccuracyEvaluationResponse> initialResponses = new HashMap<>();
            final Map<String, Double> modelScores = new HashMap<>();
            final List<ModelResult<AccuracyEvaluationResponse>> initialResults =
                    executor.executeLlm(modelIds, initialPrompt, AccuracyEvaluationResponse.class);

            // Build metadata for initial judgments
            final Map<String, AnswerAccuracyMetadata.JudgmentSummary> initialJudgments = new HashMap<>();

            for (final ModelResult<AccuracyEvaluationResponse> result : initialResults) {
                if (result.isSuccess() && result.result().score() != null) {
                    final double normalizedScore = result.result().score() / 2.0;
                    modelScores.put(result.modelId(), Math.min(1.0, Math.max(0.0, normalizedScore)));
                    initialResponses.put(result.modelId(), result.result());
                    initialJudgments.put(
                            result.modelId(),
                            new AnswerAccuracyMetadata.JudgmentSummary(
                                    result.result().score(), result.result().reasoning()));
                } else if (!result.isSuccess()) {
                    if (!excludedModels.contains(result.modelId())) {
                        excludedModels.add(result.modelId());
                    }
                    final ModelExclusionEvent exclusion = ModelExclusionEvent.builder()
                            .modelId(result.modelId())
                            .failedStepName("InitialJudgment")
                            .failedStepIndex(0)
                            .cause(result.error())
                            .build();
                    accumulatedExclusions.add(exclusion);
                }
            }

            accumulatedSteps.add(StepResults.builder()
                    .stepName("InitialJudgment")
                    .stepIndex(0)
                    .totalSteps(totalSteps)
                    .stepType(StepType.LLM)
                    .request(initialPrompt)
                    .results(new ArrayList<ModelResult<?>>(initialResults))
                    .build());

            if (modelScores.isEmpty()) {
                throw new IllegalStateException("All models failed in metric: " + getName());
            }

            // Step 2: Confirmation judgment (if enabled)
            Map<String, AnswerAccuracyMetadata.JudgmentSummary> confirmedJudgments = null;
            if (config.useDualJudge && !initialResponses.isEmpty()) {
                confirmedJudgments = new HashMap<>();

                // Use average initial score and reasoning for confirmation
                final double avgInitialScore = modelScores.values().stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0);
                final int initialScoreInt = (int) Math.round(avgInitialScore * 2);
                final String initialReasoning = initialResponses.values().stream()
                        .findFirst()
                        .map(AccuracyEvaluationResponse::reasoning)
                        .orElse("");

                final String confirmPrompt =
                        renderConfirmationJudgmentPrompt(response, reference, initialScoreInt, initialReasoning);
                final Map<String, Double> confirmedScores = new HashMap<>();
                final List<ModelResult<ConfirmationEvaluationResponse>> confirmResults =
                        executor.executeLlm(modelIds, confirmPrompt, ConfirmationEvaluationResponse.class);

                for (final ModelResult<ConfirmationEvaluationResponse> result : confirmResults) {
                    if (result.isSuccess() && result.result().score() != null) {
                        final double normalizedScore = result.result().score() / 2.0;
                        confirmedScores.put(result.modelId(), Math.min(1.0, Math.max(0.0, normalizedScore)));
                        confirmedJudgments.put(
                                result.modelId(),
                                new AnswerAccuracyMetadata.JudgmentSummary(
                                        result.result().score(), result.result().reasoning()));
                    } else if (!result.isSuccess()) {
                        if (!excludedModels.contains(result.modelId())) {
                            excludedModels.add(result.modelId());
                        }
                        final ModelExclusionEvent exclusion = ModelExclusionEvent.builder()
                                .modelId(result.modelId())
                                .failedStepName("ConfirmJudgment")
                                .failedStepIndex(1)
                                .cause(result.error())
                                .build();
                        accumulatedExclusions.add(exclusion);
                    }
                }

                accumulatedSteps.add(StepResults.builder()
                        .stepName("ConfirmJudgment")
                        .stepIndex(1)
                        .totalSteps(totalSteps)
                        .stepType(StepType.LLM)
                        .request(confirmPrompt)
                        .results(new ArrayList<ModelResult<?>>(confirmResults))
                        .build());

                // Use confirmed scores if available
                if (!confirmedScores.isEmpty()) {
                    modelScores.clear();
                    modelScores.putAll(confirmedScores);
                }
            }

            // Average score across models
            final double aggregatedScore = modelScores.values().stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);

            final Duration duration = Duration.between(startTime, Instant.now());

            notifier.afterMetricEvaluation(MetricEvaluationResult.builder()
                    .metricName(getName())
                    .sample(sample)
                    .config(config)
                    .modelIds(modelIds)
                    .aggregatedScore(aggregatedScore)
                    .modelScores(new HashMap<>(modelScores))
                    .excludedModels(excludedModels)
                    .totalDuration(duration)
                    .steps(accumulatedSteps)
                    .exclusions(accumulatedExclusions)
                    .metadata(new AnswerAccuracyMetadata(initialJudgments, confirmedJudgments, config.useDualJudge))
                    .build());

            return aggregatedScore;
        });
    }

    private String renderInitialJudgmentPrompt(final String response, final String reference) {
        return PromptTemplate.builder()
                .template(this.initialJudgmentPrompt)
                .variables(Map.of("response", response, "reference", reference))
                .build()
                .render();
    }

    private String renderConfirmationJudgmentPrompt(
            final String response, final String reference, final int initialScore, final String initialReasoning) {
        return PromptTemplate.builder()
                .template(this.confirmationJudgmentPrompt)
                .variables(Map.of(
                        "response", response,
                        "reference", reference,
                        "initialScore", String.valueOf(initialScore),
                        "initialReasoning", initialReasoning))
                .build()
                .render();
    }

    /**
     * Response DTO for accuracy evaluation.
     */
    public record AccuracyEvaluationResponse(
            @JsonPropertyDescription("Accuracy score from 0 (incorrect) to 2 (fully correct)") Integer score,
            @JsonPropertyDescription("Explanation of the accuracy assessment") String reasoning) {}

    /**
     * Response DTO for confirmation evaluation.
     */
    public record ConfirmationEvaluationResponse(
            @JsonPropertyDescription("Final accuracy score from 0 (incorrect) to 2 (fully correct)") Integer score,
            @JsonPropertyDescription("Explanation of the final assessment") String reasoning,
            @JsonPropertyDescription("Whether the initial assessment was confirmed") Boolean confirmedInitial) {}

    @Data
    @Builder
    public static class AnswerAccuracyConfig implements MetricConfiguration {
        @Singular
        private List<String> models;

        /** Enable dual-judge mode for higher reliability. */
        @Builder.Default
        private boolean useDualJudge = false;

        /** Temperature for LLM inference. Lower values produce more deterministic results. */
        @Builder.Default
        private double temperature = 0.1;

        @Builder.Default
        private String language = "en";
    }
}
