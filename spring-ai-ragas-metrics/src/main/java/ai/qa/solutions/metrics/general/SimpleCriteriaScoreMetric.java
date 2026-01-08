package ai.qa.solutions.metrics.general;

import ai.qa.solutions.execution.ModelResult;
import ai.qa.solutions.execution.MultiModelExecutor;
import ai.qa.solutions.execution.ScoreAggregator;
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
import java.util.stream.IntStream;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Singular;
import org.springframework.ai.chat.prompt.PromptTemplate;

/**
 * SimpleCriteriaScore Metric - Continuous scoring based on simple criteria.
 * <p>
 * Returns normalized score in [0, 1] range according to RAGAS methodology.
 * The LLM evaluates responses using a configurable score range (default 0-5),
 * and the result is normalized to [0, 1] for consistency with other metrics.
 * <p>
 * Uses {@link MultiModelExecutor} for parallel execution across multiple models
 * with explicit flow control and listener notifications.
 */
public class SimpleCriteriaScoreMetric
        extends AbstractMultiModelMetric<SimpleCriteriaScoreMetric.SimpleCriteriaConfig> {
    public static final String DEFAULT_PROMPT_TEMPLATE =
            """
            Evaluate the AI response based on the given criteria and score it accordingly.

            Evaluation Criteria: {definition}

            User Input: {user_input}
            AI Response: {response}
            Reference Answer: {reference}

            Instructions:
            1. Compare the AI response with the reference answer
            2. Evaluate based on the specified criteria: {definition}
            3. Provide a score between {min_score} and {max_score}
            4. Higher scores indicate better alignment with the criteria
            5. Provide detailed reasoning for your score

            Respond with a JSON object containing:
            - criteria: The evaluation criteria being applied
            - score: A numerical score between {min_score} and {max_score}
            - reasoning: Your detailed explanation for the score
            """;

    private final String promptTemplate;

    @Builder(toBuilder = true)
    protected SimpleCriteriaScoreMetric(final MultiModelExecutor executor, final String promptTemplate) {
        super(executor);
        this.promptTemplate = promptTemplate != null ? promptTemplate : DEFAULT_PROMPT_TEMPLATE;
    }

    @Override
    public Double singleTurnScore(final SimpleCriteriaConfig config, final Sample sample) {
        return singleTurnScoreAsync(config, sample).join();
    }

    @Override
    public CompletableFuture<Double> singleTurnScoreAsync(final SimpleCriteriaConfig config, final Sample sample) {
        final Instant startTime = Instant.now();
        final List<String> modelIds =
                config.models != null && !config.models.isEmpty() ? config.models : executor.getModelIds();

        // Create evaluation-specific notifier for thread-safe parallel execution
        final EvaluationNotifier notifier = createEvaluationNotifier();

        final int iterations = config.strictness != null ? config.strictness : 1;

        // Notify listeners before evaluation
        notifier.beforeMetricEvaluation(MetricEvaluationContext.builder()
                .metricName(getName())
                .sample(sample)
                .config(config)
                .modelIds(modelIds)
                .totalSteps(1)
                .metadata(Map.of("sample", sample, "config", config, "iterations", iterations))
                .build());

        return CompletableFuture.supplyAsync(() -> {
            // ========== Step 1: Evaluate with strictness iterations per model ==========
            notifier.beforeStep("Evaluate", 0, 1);

            final String prompt = renderPrompt(config, sample);
            final Map<String, Double> modelScores = new HashMap<>();
            final List<String> excludedModels = new ArrayList<>();
            final List<ModelResult<Response>> allResults = new ArrayList<>();

            // Launch ALL iterations for ALL models in parallel
            final Map<String, List<CompletableFuture<ModelResult<Response>>>> allFutures = new HashMap<>();
            for (final String modelId : modelIds) {
                final List<CompletableFuture<ModelResult<Response>>> modelFutures = IntStream.range(0, iterations)
                        .mapToObj(i -> executor.executeLlmOnModelAsync(modelId, prompt, Response.class))
                        .toList();
                allFutures.put(modelId, modelFutures);
            }

            // Wait for ALL futures to complete at once
            final List<CompletableFuture<ModelResult<Response>>> flatFutures =
                    allFutures.values().stream().flatMap(List::stream).toList();
            CompletableFuture.allOf(flatFutures.toArray(new CompletableFuture[0]))
                    .join();

            // Process results for each model and apply MEDIAN voting
            for (final String modelId : modelIds) {
                final List<Double> iterationScores = new ArrayList<>();
                for (final CompletableFuture<ModelResult<Response>> future : allFutures.get(modelId)) {
                    final ModelResult<Response> result = future.join();
                    allResults.add(result);
                    if (result.isSuccess()) {
                        // Normalize raw score to [0, 1] range (RAGAS methodology)
                        final double normalizedScore =
                                normalize(result.result().score(), config.minScore, config.maxScore);
                        iterationScores.add(normalizedScore);
                    }
                }

                if (!iterationScores.isEmpty()) {
                    // Apply MEDIAN to iterations of this model (voting for continuous scores)
                    final double modelScore = ScoreAggregator.MEDIAN.aggregate(iterationScores);
                    modelScores.put(modelId, modelScore);
                } else {
                    // All iterations failed for this model
                    excludedModels.add(modelId);
                    notifier.onModelExcluded(ModelExclusionEvent.builder()
                            .modelId(modelId)
                            .failedStepName("Evaluate")
                            .failedStepIndex(0)
                            .cause(new IllegalStateException("All " + iterations + " iterations failed"))
                            .build());
                }
            }

            notifier.afterLlmStep("Evaluate", 0, 1, prompt, allResults);

            if (modelScores.isEmpty()) {
                throw new IllegalStateException("All models failed for metric: " + getName());
            }

            // Multi-model aggregation
            final double aggregatedScore = aggregate(modelScores);

            // Notify with full results
            final Duration duration = Duration.between(startTime, Instant.now());

            notifier.afterMetricEvaluation(MetricEvaluationResult.builder()
                    .metricName(getName())
                    .aggregatedScore(aggregatedScore)
                    .modelScores(modelScores)
                    .excludedModels(excludedModels)
                    .totalDuration(duration)
                    .metadata(Map.of("sample", sample, "config", config, "iterations", iterations))
                    .build());

            return aggregatedScore;
        });
    }

    private String renderPrompt(final SimpleCriteriaConfig config, final Sample sample) {
        return PromptTemplate.builder()
                .template(this.promptTemplate)
                .variables(Map.of(
                        "definition", config.definition,
                        "min_score", config.minScore.toString(),
                        "max_score", config.maxScore.toString(),
                        "user_input", sample.getUserInput(),
                        "response", sample.getResponse(),
                        "reference", sample.getReference() != null ? sample.getReference() : ""))
                .build()
                .render();
    }

    /**
     * Normalizes a raw score to [0, 1] range according to RAGAS methodology.
     *
     * @param rawScore the raw score from LLM
     * @param minScore the minimum possible score
     * @param maxScore the maximum possible score
     * @return normalized score in [0, 1] range, guaranteed to be within bounds
     */
    private double normalize(final Double rawScore, final double minScore, final double maxScore) {
        if (rawScore == null || rawScore.isNaN() || rawScore.isInfinite()) {
            return 0.0;
        }
        // Protect against division by zero
        final double range = maxScore - minScore;
        if (range <= 0) {
            return 0.0;
        }
        // Clamp to valid range first
        final double clampedScore = Math.max(minScore, Math.min(maxScore, rawScore));
        // Normalize to [0, 1]
        final double normalized = (clampedScore - minScore) / range;
        // Final safety clamp to ensure [0, 1] bounds
        return Math.max(0.0, Math.min(1.0, normalized));
    }

    /**
     * Response DTO for SimpleCriteriaScore metric evaluation.
     * The score field contains the raw LLM score within [minScore, maxScore] range.
     * Normalization to [0, 1] is performed externally using the config's score range.
     */
    public record Response(
            @JsonPropertyDescription("The evaluation criteria that was used to score the response") String criteria,
            @JsonPropertyDescription(
                            "Numerical score within the specified range (e.g., 0-5) based on how well the response meets the criteria")
                    Double score,
            @JsonPropertyDescription(
                            "Detailed explanation of why this specific score was assigned, including analysis of strengths and weaknesses")
                    String reasoning) {}

    @Data
    @Builder
    public static class SimpleCriteriaConfig implements MetricConfiguration {
        @Singular
        private List<String> models;

        @NonNull
        private String definition;

        @Builder.Default
        private Double minScore = 0.0;

        @Builder.Default
        private Double maxScore = 5.0;

        /**
         * Number of iterations per model for self-consistency voting.
         * Each model will be called strictness times and results aggregated using MEDIAN.
         * Default is 1 (no voting, single call per model).
         */
        @Builder.Default
        private Integer strictness = 1;

        @SuppressWarnings("unused")
        public void setScoreRange(double minScore, double maxScore) {
            if (minScore >= maxScore) {
                throw new IllegalArgumentException("minScore must be less than maxScore");
            }
            this.minScore = minScore;
            this.maxScore = maxScore;
        }
    }
}
