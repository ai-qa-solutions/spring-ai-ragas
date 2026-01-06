package ai.qa.solutions.metrics.retrieval;

import ai.qa.solutions.execution.ModelResult;
import ai.qa.solutions.execution.MultiModelExecutor;
import ai.qa.solutions.execution.listener.MetricEvaluationContext;
import ai.qa.solutions.execution.listener.MetricEvaluationResult;
import ai.qa.solutions.execution.listener.ModelExclusionEvent;
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
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.PromptTemplate;

/**
 * Context Precision Metric - LLM-based evaluation of retriever's ability to rank relevant chunks higher.
 * <p>
 * Uses {@link MultiModelExecutor} for parallel execution across multiple models
 * with explicit flow control and listener notifications.
 * <p>
 * Automatically chooses between reference-based or response-based evaluation based on available data.
 */
@Slf4j
public class ContextPrecisionMetric extends AbstractMultiModelMetric<ContextPrecisionMetric.ContextPrecisionConfig> {
    public static final String DEFAULT_WITH_REFERENCE_PROMPT =
            """
            Given a user query, reference answer, and a retrieved context chunk, determine if the context chunk is relevant to answering the user query based on the reference answer.

            User Query: {user_input}
            Reference Answer: {reference}
            Retrieved Context Chunk: {context_chunk}

            Instructions:
            1. Analyze if the context chunk contains information that is relevant to providing the reference answer
            2. Use the reference answer as the gold standard for what constitutes a complete and correct response
            3. A chunk is relevant if it contains information that supports or contributes to the reference answer
            4. Be strict in your evaluation - only mark as relevant if the chunk genuinely helps answer the query as indicated by the reference

            Respond with a JSON object containing:
            - relevant: true if the context chunk is relevant to answering the user query based on the reference, false otherwise
            - reasoning: Your detailed explanation for why the chunk is or isn't relevant
            """;

    public static final String DEFAULT_WITHOUT_REFERENCE_PROMPT =
            """
            Given a user query, AI response, and a retrieved context chunk, determine if the context chunk is relevant to answering the user query based on the AI response.

            User Query: {user_input}
            AI Response: {response}
            Retrieved Context Chunk: {context_chunk}

            Instructions:
            1. Analyze if the context chunk contains information that is relevant to answering the user query
            2. Consider the AI response as guidance for what constitutes a relevant answer
            3. A chunk is relevant if it contains information that helps answer the query, even if not directly used in the response
            4. Be strict in your evaluation - only mark as relevant if the chunk genuinely contributes to answering the query

            Respond with a JSON object containing:
            - relevant: true if the context chunk is relevant to answering the user query, false otherwise
            - reasoning: Your detailed explanation for why the chunk is or isn't relevant
            """;

    private final String withReferencePrompt;
    private final String withoutReferencePrompt;

    @Builder(toBuilder = true)
    protected ContextPrecisionMetric(
            final MultiModelExecutor executor, final String withReferencePrompt, final String withoutReferencePrompt) {
        super(executor);
        this.withReferencePrompt = withReferencePrompt != null ? withReferencePrompt : DEFAULT_WITH_REFERENCE_PROMPT;
        this.withoutReferencePrompt =
                withoutReferencePrompt != null ? withoutReferencePrompt : DEFAULT_WITHOUT_REFERENCE_PROMPT;
    }

    /**
     * Convenience method for single-turn scoring with default configuration.
     */
    public Double singleTurnScore(final Sample sample) {
        return singleTurnScore(ContextPrecisionConfig.builder().build(), sample);
    }

    /**
     * Convenience method for async single-turn scoring with default configuration.
     */
    public CompletableFuture<Double> singleTurnScoreAsync(final Sample sample) {
        return singleTurnScoreAsync(ContextPrecisionConfig.builder().build(), sample);
    }

    @Override
    public Double singleTurnScore(final ContextPrecisionConfig config, final Sample sample) {
        return singleTurnScoreAsync(config, sample).join();
    }

    @Override
    public CompletableFuture<Double> singleTurnScoreAsync(final ContextPrecisionConfig config, final Sample sample) {
        final List<String> retrievedContexts = sample.getRetrievedContexts();
        if (retrievedContexts == null || retrievedContexts.isEmpty()) {
            log.warn("No retrieved contexts provided for Context Precision evaluation");
            return CompletableFuture.completedFuture(0.0);
        }

        final Instant startTime = Instant.now();
        final List<String> modelIds =
                config.models != null && !config.models.isEmpty() ? config.models : executor.getModelIds();

        // Determine strategy based on config preference and data availability
        final EvaluationStrategy strategy = determineEvaluationStrategy(config, sample);

        log.debug(
                "Using LLM {}-based context precision evaluation with {} contexts",
                strategy == EvaluationStrategy.REFERENCE_BASED ? "reference" : "response",
                retrievedContexts.size());

        final int totalSteps = retrievedContexts.size() + 1; // N evaluation steps + 1 compute step

        // Create evaluation-specific notifier for thread-safe parallel execution
        final EvaluationNotifier notifier = createEvaluationNotifier();

        // Notify listeners before evaluation
        notifier.beforeMetricEvaluation(MetricEvaluationContext.builder()
                .metricName(getName())
                .sample(sample)
                .config(config)
                .modelIds(modelIds)
                .totalSteps(totalSteps)
                .metadata(Map.of("sample", sample, "config", config, "strategy", strategy))
                .build());

        return CompletableFuture.supplyAsync(() -> {
            log.debug("Computing context precision evaluation with explicit flow");

            // Track excluded models across all steps
            final java.util.Set<String> excludedModelIds = new java.util.HashSet<>();

            final String template = strategy == EvaluationStrategy.REFERENCE_BASED
                    ? this.withReferencePrompt
                    : this.withoutReferencePrompt;

            // Track relevance results per model
            final Map<String, List<Boolean>> modelRelevanceResults = new HashMap<>();
            for (String modelId : modelIds) {
                modelRelevanceResults.put(modelId, new ArrayList<>());
            }

            // ========== Steps 0 to N-1: Evaluate each context ==========
            for (int contextIdx = 0; contextIdx < retrievedContexts.size(); contextIdx++) {
                final String contextChunk = retrievedContexts.get(contextIdx);
                final String stepName = "EvaluateContext_" + contextIdx;

                notifier.beforeStep(stepName, contextIdx, totalSteps);

                final String prompt = renderPrompt(template, strategy, sample, contextChunk);
                final List<ModelResult<RelevanceResponse>> results =
                        executor.executeLlm(modelIds, prompt, RelevanceResponse.class);

                notifier.afterLlmStep(stepName, contextIdx, totalSteps, prompt, results);

                // Collect results for each model
                for (final ModelResult<RelevanceResponse> result : results) {
                    if (result.isSuccess()) {
                        final boolean relevant = result.result().relevant() != null
                                && result.result().relevant();
                        modelRelevanceResults.get(result.modelId()).add(relevant);
                    } else {
                        // Model failed for this context - mark as not relevant for this context
                        modelRelevanceResults.get(result.modelId()).add(false);
                        excludedModelIds.add(result.modelId());
                        notifier.onModelExcluded(ModelExclusionEvent.builder()
                                .modelId(result.modelId())
                                .failedStepName(stepName)
                                .failedStepIndex(contextIdx)
                                .cause(result.error())
                                .build());
                    }
                }
            }

            // ========== Final step: Compute precision ==========
            notifier.beforeStep("ComputePrecision", retrievedContexts.size(), totalSteps);

            final Map<String, Double> modelScores = new HashMap<>();

            for (final Map.Entry<String, List<Boolean>> entry : modelRelevanceResults.entrySet()) {
                final String modelId = entry.getKey();
                final List<Boolean> relevanceScores = entry.getValue();

                if (relevanceScores.size() == retrievedContexts.size()) {
                    final double precision = calculateContextPrecision(relevanceScores);
                    modelScores.put(modelId, precision);
                }
            }

            // Create synthetic results for notification
            final List<ModelResult<Double>> computeResults = modelScores.entrySet().stream()
                    .map(e -> ModelResult.success(e.getKey(), e.getValue(), Duration.ZERO, "compute"))
                    .toList();

            notifier.afterComputeStep("ComputePrecision", retrievedContexts.size(), totalSteps, computeResults);

            if (modelScores.isEmpty()) {
                throw new IllegalStateException("All models failed for metric: " + getName());
            }

            final double aggregatedScore = aggregate(modelScores);

            // Notify with full results
            final Duration duration = Duration.between(startTime, Instant.now());
            notifier.afterMetricEvaluation(MetricEvaluationResult.builder()
                    .metricName(getName())
                    .aggregatedScore(aggregatedScore)
                    .modelScores(modelScores)
                    .excludedModels(new java.util.ArrayList<>(excludedModelIds))
                    .totalDuration(duration)
                    .metadata(Map.of("sample", sample, "config", config, "strategy", strategy))
                    .build());

            return aggregatedScore;
        });
    }

    private EvaluationStrategy determineEvaluationStrategy(final ContextPrecisionConfig config, final Sample sample) {
        if (config.getEvaluationStrategy() == null) {
            // Auto-detect based on available data
            final boolean hasReference = sample.getReference() != null
                    && !sample.getReference().trim().isEmpty();
            return hasReference ? EvaluationStrategy.REFERENCE_BASED : EvaluationStrategy.RESPONSE_BASED;
        }

        // Validate that the required data is available for the chosen strategy
        if (config.getEvaluationStrategy() == EvaluationStrategy.REFERENCE_BASED) {
            if (sample.getReference() == null || sample.getReference().trim().isEmpty()) {
                log.warn(
                        "Reference-based evaluation requested but no reference provided, falling back to response-based");
                return EvaluationStrategy.RESPONSE_BASED;
            }
            return EvaluationStrategy.REFERENCE_BASED;
        }

        return EvaluationStrategy.RESPONSE_BASED;
    }

    private String renderPrompt(
            final String template, final EvaluationStrategy strategy, final Sample sample, final String contextChunk) {
        final Map<String, Object> variables;
        if (strategy == EvaluationStrategy.REFERENCE_BASED) {
            variables = Map.of(
                    "user_input", sample.getUserInput(),
                    "reference", sample.getReference(),
                    "context_chunk", contextChunk);
        } else {
            variables = Map.of(
                    "user_input", sample.getUserInput(),
                    "response", sample.getResponse(),
                    "context_chunk", contextChunk);
        }

        return PromptTemplate.builder()
                .template(template)
                .variables(variables)
                .build()
                .render();
    }

    /**
     * Calculates Average Precision (AP) for context relevance ranking.
     * <p>
     * Formula: AP = sum(precision@k * relevance@k for all k) / total_relevant_items
     * <p>
     * This rewards relevant contexts appearing earlier in the ranking.
     *
     * @param relevanceScores list of boolean relevance scores in ranked order
     * @return Average Precision score (0.0 to 1.0)
     */
    private Double calculateContextPrecision(List<Boolean> relevanceScores) {
        if (relevanceScores.isEmpty()) {
            return 0.0;
        }

        // Count total relevant items
        long totalRelevant = relevanceScores.stream().filter(r -> r).count();

        if (totalRelevant == 0) {
            return 0.0;
        }

        // Sum precision@k only for positions where item is relevant (relevance@k = 1)
        double sum = IntStream.range(0, relevanceScores.size())
                .filter(k -> relevanceScores.get(k)) // Only relevant positions
                .mapToDouble(k -> {
                    // Calculate precision@k (relevant items up to position k / total items up to position k)
                    long relevantUpToK = relevanceScores.subList(0, k + 1).stream()
                            .mapToInt(relevant -> relevant ? 1 : 0)
                            .sum();
                    return (double) relevantUpToK / (k + 1);
                })
                .sum();

        // Divide by number of relevant items (not total count)
        return sum / totalRelevant;
    }

    /**
     * Response DTO for LLM-based relevance evaluation
     */
    public record RelevanceResponse(
            @JsonPropertyDescription("Boolean indicating if the context chunk is relevant to answering the user query")
                    Boolean relevant,
            @JsonPropertyDescription("Detailed explanation of why the chunk is or isn't relevant") String reasoning) {}

    /**
     * Evaluation strategy enum
     */
    public enum EvaluationStrategy {
        REFERENCE_BASED, // Use reference answer for evaluation (preferred when available)
        RESPONSE_BASED // Use AI response for evaluation
    }

    @Data
    @Builder
    public static class ContextPrecisionConfig implements MetricConfiguration {
        /**
         * Evaluation strategy for LLM-based evaluation.
         * If null, will auto-detect based on available data (reference preferred over response).
         */
        private EvaluationStrategy evaluationStrategy;

        /**
         * List of model IDs to use for multi-model execution.
         * If not specified, uses default models from executor configuration.
         */
        @Singular
        private List<String> models;
    }
}
