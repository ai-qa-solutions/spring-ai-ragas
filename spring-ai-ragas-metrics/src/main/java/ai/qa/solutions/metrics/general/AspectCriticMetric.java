package ai.qa.solutions.metrics.general;

import ai.qa.solutions.execution.ModelResult;
import ai.qa.solutions.execution.MultiModelExecutor;
import ai.qa.solutions.execution.ScoreAggregator;
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
import lombok.NonNull;
import lombok.Singular;
import org.springframework.ai.chat.prompt.PromptTemplate;

/**
 * AspectCritic Metric - Binary evaluation based on predefined aspects.
 * <p>
 * Uses {@link MultiModelExecutor} for parallel execution across multiple models
 * with explicit flow control and listener notifications.
 */
public class AspectCriticMetric extends AbstractMultiModelMetric<AspectCriticMetric.AspectCriticConfig> {

    public static final String DEFAULT_PROMPT_TEMPLATE =
            """
                    Given a user input and an AI response, evaluate whether the response meets the specified criteria.

                    Criteria: {definition}

                    User Input: {user_input}

                    AI Response: {response}

                    Instructions:
                    1. Carefully analyze the AI response against the given criteria
                    2. Consider the context provided by the user input
                    3. Apply a strictness level of {strictness} (1=lenient, 5=very strict)
                    4. Provide your evaluation with the criteria, verdict (true/false), and detailed reasoning

                    Respond with a JSON object containing:
                    - criteria: The evaluation criteria being applied
                    - verdict: true if the response meets the criteria, false otherwise
                    - reasoning: Your detailed explanation for the verdict
                    """;

    private final String promptTemplate;

    @Builder(toBuilder = true)
    protected AspectCriticMetric(final MultiModelExecutor executor, final String promptTemplate) {
        super(executor);
        this.promptTemplate = promptTemplate != null ? promptTemplate : DEFAULT_PROMPT_TEMPLATE;
    }

    @Override
    public Double singleTurnScore(final AspectCriticConfig config, final Sample sample) {
        return singleTurnScoreAsync(config, sample).join();
    }

    @Override
    public CompletableFuture<Double> singleTurnScoreAsync(final AspectCriticConfig config, final Sample sample) {
        final Instant startTime = Instant.now();
        final List<String> modelIds =
                config.models != null && !config.models.isEmpty() ? config.models : executor.getModelIds();

        // Create evaluation-specific notifier for thread-safe parallel execution
        final EvaluationNotifier notifier = createEvaluationNotifier();

        final int iterations = config.strictness != null ? config.strictness : 3;

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

            // Process results for each model and apply majority voting
            for (final String modelId : modelIds) {
                final List<Double> iterationScores = new ArrayList<>();
                for (final CompletableFuture<ModelResult<Response>> future : allFutures.get(modelId)) {
                    final ModelResult<Response> result = future.join();
                    allResults.add(result);
                    if (result.isSuccess()) {
                        iterationScores.add(result.result().getScore());
                    }
                }

                if (!iterationScores.isEmpty()) {
                    // Apply majority voting to iterations of this model
                    final double modelScore = ScoreAggregator.MAJORITY_VOTING.aggregate(iterationScores);
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

    private String renderPrompt(final AspectCriticConfig config, final Sample sample) {
        return PromptTemplate.builder()
                .template(this.promptTemplate)
                .variables(Map.of(
                        "definition",
                        config.definition,
                        "strictness",
                        config.strictness,
                        "user_input",
                        sample.getUserInput(),
                        "response",
                        sample.getResponse()))
                .build()
                .render();
    }

    /**
     * Response DTO for AspectCritic metric evaluation
     */
    public record Response(
            @JsonPropertyDescription("The specific evaluation criteria that was applied to assess the response")
                    String criteria,
            @JsonPropertyDescription("Boolean verdict: true if the response meets the criteria, false otherwise")
                    Boolean verdict,
            @JsonPropertyDescription("Detailed explanation and justification for the verdict decision")
                    String reasoning) {
        public Double getScore() {
            return verdict != null && verdict ? 1.0 : 0.0;
        }
    }

    @Data
    @Builder
    public static class AspectCriticConfig implements MetricConfiguration {
        @Singular
        private List<String> models;

        @NonNull
        private String definition;

        @Builder.Default
        private Integer strictness = 3;

        @SuppressWarnings("unused")
        public void setStrictness(final int strictness) {
            if (strictness < 1 || strictness > 5) {
                throw new IllegalArgumentException("Strictness must be between 1 and 5");
            }
            this.strictness = strictness;
        }
    }
}
