package ai.qa.solutions.metrics.nvidia;

import ai.qa.solutions.execution.ModelResult;
import ai.qa.solutions.execution.MultiModelExecutor;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationContext;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationResult;
import ai.qa.solutions.execution.listener.dto.ModelExclusionEvent;
import ai.qa.solutions.execution.listener.dto.StepResults;
import ai.qa.solutions.execution.listener.dto.StepType;
import ai.qa.solutions.metric.AbstractMultiModelMetric;
import ai.qa.solutions.metric.metadata.ResponseGroundednessMetadata;
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
 * Response Groundedness Metric - NVIDIA-style evaluation of response grounding in context.
 * <p>
 * This metric evaluates whether the response is grounded in (supported by) the retrieved contexts
 * using a 0-2 scoring scale that is normalized to 0-1.
 * <p>
 * Scoring scale:
 * <ul>
 *   <li>0 - Not grounded: Response contains information not found in contexts</li>
 *   <li>1 - Partially grounded: Response is partially supported by contexts</li>
 *   <li>2 - Fully grounded: Response is completely supported by contexts</li>
 * </ul>
 * <p>
 * Heuristic shortcuts (when enabled):
 * <ul>
 *   <li>Empty response -> score 0.0</li>
 *   <li>Response exactly matches context -> score 1.0</li>
 * </ul>
 * <p>
 * Required sample fields:
 * <ul>
 *   <li>{@code response} - The AI response to evaluate</li>
 *   <li>{@code retrievedContexts} - List of retrieved context chunks</li>
 * </ul>
 */
@Slf4j
public class ResponseGroundednessMetric
        extends AbstractMultiModelMetric<ResponseGroundednessMetric.ResponseGroundednessConfig> {

    public static final String DEFAULT_EVALUATE_GROUNDEDNESS_PROMPT =
            """
                    Evaluate whether the response is grounded in (supported by) the provided context.

                    Response:
                    {response}

                    Context:
                    {context}

                    Instructions:
                    1. Analyze whether the information in the response can be found in or inferred from the context
                    2. Check for any claims or statements in the response that are not supported by the context
                    3. Rate using the following scale:
                       - 0: Not grounded - Response contains significant information not found in context
                       - 1: Partially grounded - Response is partially supported by context but has some unsupported parts
                       - 2: Fully grounded - Response is completely supported by the context

                    Respond with a JSON object containing:
                    - score: Integer from 0 to 2 representing the groundedness level
                    - reasoning: Brief explanation of the groundedness assessment
                    """;

    private final String evaluateGroundednessPrompt;

    @Builder(toBuilder = true)
    protected ResponseGroundednessMetric(final MultiModelExecutor executor, final String evaluateGroundednessPrompt) {
        super(executor);
        this.evaluateGroundednessPrompt =
                evaluateGroundednessPrompt != null ? evaluateGroundednessPrompt : DEFAULT_EVALUATE_GROUNDEDNESS_PROMPT;
    }

    @Override
    public Double singleTurnScore(final ResponseGroundednessConfig config, final Sample sample) {
        return singleTurnScoreAsync(config, sample).join();
    }

    @Override
    public CompletableFuture<Double> singleTurnScoreAsync(
            final ResponseGroundednessConfig config, final Sample sample) {
        final Instant startTime = Instant.now();
        final List<String> modelIds =
                config.models != null && !config.models.isEmpty() ? config.models : executor.getModelIds();

        // Validate input
        if (sample.getResponse() == null || sample.getResponse().isEmpty()) {
            log.warn("No response provided for Response Groundedness evaluation");
            return CompletableFuture.completedFuture(config.useHeuristicShortcuts ? 0.0 : null);
        }

        if (sample.getRetrievedContexts() == null
                || sample.getRetrievedContexts().isEmpty()) {
            log.warn("No retrieved contexts provided for Response Groundedness evaluation");
            return CompletableFuture.completedFuture(null);
        }

        final EvaluationNotifier notifier = createEvaluationNotifier();
        final int totalSteps = config.useHeuristicShortcuts ? 2 : 1;

        notifier.beforeMetricEvaluation(MetricEvaluationContext.builder()
                .metricName(getName())
                .sample(sample)
                .config(config)
                .modelIds(modelIds)
                .totalSteps(totalSteps)
                .build());

        return executor.runAsync(() -> {
            final String response = sample.getResponse();
            final String combinedContext = String.join("\n\n", sample.getRetrievedContexts());

            final List<StepResults> accumulatedSteps = new ArrayList<>();
            final List<ModelExclusionEvent> accumulatedExclusions = new ArrayList<>();
            final List<String> excludedModels = new ArrayList<>();

            // Step 0: Apply heuristics if enabled
            if (config.useHeuristicShortcuts) {
                // Check for exact match
                for (final String context : sample.getRetrievedContexts()) {
                    if (response.trim().equalsIgnoreCase(context.trim())) {
                        log.debug("Response exactly matches context, returning 1.0");
                        accumulatedSteps.add(StepResults.builder()
                                .stepName("ApplyHeuristics")
                                .stepIndex(0)
                                .totalSteps(totalSteps)
                                .stepType(StepType.COMPUTE)
                                .results(List.of())
                                .build());
                        finishEvaluation(
                                notifier,
                                1.0,
                                excludedModels,
                                startTime,
                                sample,
                                config,
                                modelIds,
                                accumulatedSteps,
                                accumulatedExclusions,
                                true);
                        return 1.0;
                    }
                }

                // Check if response is contained in context
                if (combinedContext
                        .toLowerCase()
                        .contains(response.toLowerCase().trim())) {
                    log.debug("Response is fully contained in context, returning 1.0");
                    accumulatedSteps.add(StepResults.builder()
                            .stepName("ApplyHeuristics")
                            .stepIndex(0)
                            .totalSteps(totalSteps)
                            .stepType(StepType.COMPUTE)
                            .results(List.of())
                            .build());
                    finishEvaluation(
                            notifier,
                            1.0,
                            excludedModels,
                            startTime,
                            sample,
                            config,
                            modelIds,
                            accumulatedSteps,
                            accumulatedExclusions,
                            true);
                    return 1.0;
                }

                accumulatedSteps.add(StepResults.builder()
                        .stepName("ApplyHeuristics")
                        .stepIndex(0)
                        .totalSteps(totalSteps)
                        .stepType(StepType.COMPUTE)
                        .results(List.of())
                        .build());
            }

            // Step 1: Evaluate groundedness via LLM
            final int stepIndex = config.useHeuristicShortcuts ? 1 : 0;
            final String stepName = "EvaluateGroundedness";

            final String prompt = renderEvaluateGroundednessPrompt(response, combinedContext);
            final Map<String, Double> modelScores = new HashMap<>();
            final List<ModelResult<GroundednessEvaluationResponse>> results =
                    executor.executeLlm(modelIds, prompt, GroundednessEvaluationResponse.class);

            for (final ModelResult<GroundednessEvaluationResponse> result : results) {
                if (result.isSuccess() && result.result().score() != null) {
                    // Normalize score from 0-2 to 0-1
                    final double normalizedScore = result.result().score() / 2.0;
                    modelScores.put(result.modelId(), Math.min(1.0, Math.max(0.0, normalizedScore)));
                } else if (!result.isSuccess()) {
                    if (!excludedModels.contains(result.modelId())) {
                        excludedModels.add(result.modelId());
                    }
                    final ModelExclusionEvent exclusion = ModelExclusionEvent.builder()
                            .modelId(result.modelId())
                            .failedStepName(stepName)
                            .failedStepIndex(stepIndex)
                            .cause(result.error())
                            .build();
                    accumulatedExclusions.add(exclusion);
                }
            }

            accumulatedSteps.add(StepResults.builder()
                    .stepName(stepName)
                    .stepIndex(stepIndex)
                    .totalSteps(totalSteps)
                    .stepType(StepType.LLM)
                    .request(prompt)
                    .results(new ArrayList<ModelResult<?>>(results))
                    .build());

            if (modelScores.isEmpty()) {
                throw new IllegalStateException("All models failed in metric: " + getName());
            }

            // Average score across models
            final double aggregatedScore = modelScores.values().stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);

            finishEvaluation(
                    notifier,
                    aggregatedScore,
                    excludedModels,
                    startTime,
                    sample,
                    config,
                    modelIds,
                    accumulatedSteps,
                    accumulatedExclusions,
                    false);
            return aggregatedScore;
        });
    }

    private void finishEvaluation(
            final EvaluationNotifier notifier,
            final double score,
            final List<String> excludedModels,
            final Instant startTime,
            final Sample sample,
            final ResponseGroundednessConfig config,
            final List<String> modelIds,
            final List<StepResults> accumulatedSteps,
            final List<ModelExclusionEvent> accumulatedExclusions,
            final boolean heuristicMatch) {
        final Duration duration = Duration.between(startTime, Instant.now());

        notifier.afterMetricEvaluation(MetricEvaluationResult.builder()
                .metricName(getName())
                .sample(sample)
                .config(config)
                .modelIds(modelIds)
                .aggregatedScore(score)
                .modelScores(Map.of("aggregated", score))
                .excludedModels(excludedModels)
                .totalDuration(duration)
                .steps(accumulatedSteps)
                .exclusions(accumulatedExclusions)
                .metadata(new ResponseGroundednessMetadata(config.useHeuristicShortcuts, heuristicMatch))
                .build());
    }

    private String renderEvaluateGroundednessPrompt(final String response, final String context) {
        return PromptTemplate.builder()
                .template(this.evaluateGroundednessPrompt)
                .variables(Map.of("response", response, "context", context))
                .build()
                .render();
    }

    /**
     * Response DTO for groundedness evaluation.
     */
    public record GroundednessEvaluationResponse(
            @JsonPropertyDescription("Groundedness score from 0 (not grounded) to 2 (fully grounded)") Integer score,
            @JsonPropertyDescription("Explanation of the groundedness assessment") String reasoning) {}

    @Data
    @Builder
    public static class ResponseGroundednessConfig implements MetricConfiguration {
        @Singular
        private List<String> models;

        /** Enable heuristic shortcuts (exact match -> 1.0, empty -> 0.0). */
        @Builder.Default
        private boolean useHeuristicShortcuts = true;

        /** Temperature for LLM inference. Lower values produce more deterministic results. */
        @Builder.Default
        private double temperature = 0.1;

        @Builder.Default
        private String language = "en";
    }
}
