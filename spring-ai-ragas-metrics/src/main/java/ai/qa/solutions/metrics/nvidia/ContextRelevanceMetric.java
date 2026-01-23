package ai.qa.solutions.metrics.nvidia;

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
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.PromptTemplate;

/**
 * Context Relevance Metric - NVIDIA-style evaluation of retrieved context relevance.
 * <p>
 * This metric evaluates whether the retrieved contexts are relevant to the user's question
 * using a 0-2 scoring scale that is normalized to 0-1.
 * <p>
 * Scoring scale:
 * <ul>
 *   <li>0 - Not relevant: Context does not contain information to answer the question</li>
 *   <li>1 - Partially relevant: Context contains some relevant information</li>
 *   <li>2 - Fully relevant: Context contains comprehensive information to answer the question</li>
 * </ul>
 * <p>
 * Required sample fields:
 * <ul>
 *   <li>{@code userInput} - The user's question</li>
 *   <li>{@code retrievedContexts} - List of retrieved context chunks</li>
 * </ul>
 */
@Slf4j
public class ContextRelevanceMetric extends AbstractMultiModelMetric<ContextRelevanceMetric.ContextRelevanceConfig> {

    public static final String DEFAULT_EVALUATE_RELEVANCE_PROMPT =
            """
                    Evaluate the relevance of the provided context for answering the user's question.

                    User Question:
                    {userInput}

                    Retrieved Context:
                    {context}

                    Instructions:
                    1. Analyze whether the context contains information relevant to answering the question
                    2. Consider both direct relevance and supporting information
                    3. Rate using the following scale:
                       - 0: Not relevant - Context does not contain information to answer the question
                       - 1: Partially relevant - Context contains some relevant information but may be incomplete
                       - 2: Fully relevant - Context contains comprehensive information to answer the question

                    Respond with a JSON object containing:
                    - score: Integer from 0 to 2 representing the relevance level
                    - reasoning: Brief explanation of the relevance assessment
                    """;

    private final String evaluateRelevancePrompt;

    @Builder(toBuilder = true)
    protected ContextRelevanceMetric(final MultiModelExecutor executor, final String evaluateRelevancePrompt) {
        super(executor);
        this.evaluateRelevancePrompt =
                evaluateRelevancePrompt != null ? evaluateRelevancePrompt : DEFAULT_EVALUATE_RELEVANCE_PROMPT;
    }

    @Override
    public Double singleTurnScore(final ContextRelevanceConfig config, final Sample sample) {
        return singleTurnScoreAsync(config, sample).join();
    }

    @Override
    public CompletableFuture<Double> singleTurnScoreAsync(final ContextRelevanceConfig config, final Sample sample) {
        final Instant startTime = Instant.now();
        final List<String> modelIds =
                config.models != null && !config.models.isEmpty() ? config.models : executor.getModelIds();

        // Validate input
        if (sample.getUserInput() == null || sample.getUserInput().isEmpty()) {
            log.warn("No user input provided for Context Relevance evaluation");
            return CompletableFuture.completedFuture(null);
        }

        if (sample.getRetrievedContexts() == null
                || sample.getRetrievedContexts().isEmpty()) {
            log.warn("No retrieved contexts provided for Context Relevance evaluation");
            return CompletableFuture.completedFuture(null);
        }

        final EvaluationNotifier notifier = createEvaluationNotifier();
        final int totalSteps = sample.getRetrievedContexts().size();

        notifier.beforeMetricEvaluation(MetricEvaluationContext.builder()
                .metricName(getName())
                .sample(sample)
                .config(config)
                .modelIds(modelIds)
                .totalSteps(totalSteps)
                .metadata(Map.of("sample", sample, "config", config))
                .build());

        return executor.runAsync(() -> {
            final String userInput = sample.getUserInput();
            final List<String> contexts = sample.getRetrievedContexts();
            final List<String> excludedModels = new ArrayList<>();
            final List<Double> contextScores = new ArrayList<>();

            // Evaluate each context chunk
            for (int i = 0; i < contexts.size(); i++) {
                final String context = contexts.get(i);
                final String stepName = String.format("EvaluateRelevance_%d", i + 1);
                notifier.beforeStep(stepName, i, totalSteps);

                final String prompt = renderEvaluateRelevancePrompt(userInput, context);
                final Map<String, Double> modelScores = new HashMap<>();
                final List<ModelResult<RelevanceEvaluationResponse>> results =
                        executor.executeLlm(modelIds, prompt, RelevanceEvaluationResponse.class);

                for (final ModelResult<RelevanceEvaluationResponse> result : results) {
                    if (result.isSuccess() && result.result().score() != null) {
                        // Normalize score from 0-2 to 0-1
                        final double normalizedScore = result.result().score() / 2.0;
                        modelScores.put(result.modelId(), Math.min(1.0, Math.max(0.0, normalizedScore)));
                    } else if (!result.isSuccess()) {
                        if (!excludedModels.contains(result.modelId())) {
                            excludedModels.add(result.modelId());
                        }
                        notifier.onModelExcluded(ModelExclusionEvent.builder()
                                .modelId(result.modelId())
                                .failedStepName(stepName)
                                .failedStepIndex(i)
                                .cause(result.error())
                                .build());
                    }
                }

                notifier.afterLlmStep(stepName, i, totalSteps, prompt, results);

                if (!modelScores.isEmpty()) {
                    // Average across models for this context
                    final double avgScore = modelScores.values().stream()
                            .mapToDouble(Double::doubleValue)
                            .average()
                            .orElse(0.0);
                    contextScores.add(avgScore);
                }
            }

            if (contextScores.isEmpty()) {
                throw new IllegalStateException("All models failed for all contexts in metric: " + getName());
            }

            // Average score across all contexts
            final double aggregatedScore = contextScores.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
            final Duration duration = Duration.between(startTime, Instant.now());

            notifier.afterMetricEvaluation(MetricEvaluationResult.builder()
                    .metricName(getName())
                    .aggregatedScore(aggregatedScore)
                    .modelScores(Map.of("aggregated", aggregatedScore))
                    .excludedModels(excludedModels)
                    .totalDuration(duration)
                    .metadata(Map.of("sample", sample, "config", config, "contextScores", contextScores))
                    .build());

            return aggregatedScore;
        });
    }

    private String renderEvaluateRelevancePrompt(final String userInput, final String context) {
        return PromptTemplate.builder()
                .template(this.evaluateRelevancePrompt)
                .variables(Map.of("userInput", userInput, "context", context))
                .build()
                .render();
    }

    /**
     * Response DTO for relevance evaluation.
     */
    public record RelevanceEvaluationResponse(
            @JsonPropertyDescription("Relevance score from 0 (not relevant) to 2 (fully relevant)") Integer score,
            @JsonPropertyDescription("Explanation of the relevance assessment") String reasoning) {}

    @Data
    @Builder
    public static class ContextRelevanceConfig implements MetricConfiguration {
        @Singular
        private List<String> models;

        /** Temperature for LLM inference. Lower values produce more deterministic results. */
        @Builder.Default
        private double temperature = 0.1;
    }
}
