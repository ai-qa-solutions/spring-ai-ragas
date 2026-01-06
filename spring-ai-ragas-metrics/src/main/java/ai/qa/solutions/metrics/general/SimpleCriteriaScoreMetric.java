package ai.qa.solutions.metrics.general;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Singular;
import org.springframework.ai.chat.prompt.PromptTemplate;

/**
 * SimpleCriteriaScore Metric - Continuous scoring based on simple criteria.
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

        // Notify listeners before evaluation
        notifier.beforeMetricEvaluation(MetricEvaluationContext.builder()
                .metricName(getName())
                .sample(sample)
                .config(config)
                .modelIds(modelIds)
                .totalSteps(1)
                .metadata(Map.of("sample", sample, "config", config))
                .build());

        return CompletableFuture.supplyAsync(() -> {
            // ========== Step 1: Evaluate ==========
            notifier.beforeStep("Evaluate", 0, 1);

            final String prompt = renderPrompt(config, sample);
            final List<ModelResult<Response>> results = executor.executeLlm(modelIds, prompt, Response.class);

            notifier.afterLlmStep("Evaluate", 0, 1, prompt, results);

            // Collect scores and notify about excluded models
            final Map<String, Double> modelScores = new HashMap<>();
            for (final ModelResult<Response> result : results) {
                if (result.isSuccess()) {
                    modelScores.put(result.modelId(), result.result().getNormalizedScore());
                } else {
                    notifier.onModelExcluded(ModelExclusionEvent.builder()
                            .modelId(result.modelId())
                            .failedStepName("Evaluate")
                            .failedStepIndex(0)
                            .cause(result.error())
                            .build());
                }
            }

            if (modelScores.isEmpty()) {
                throw new IllegalStateException("All models failed for metric: " + getName());
            }

            final double aggregatedScore = aggregate(modelScores);

            // Notify with full results
            final Duration duration = Duration.between(startTime, Instant.now());
            final List<String> excludedModels = results.stream()
                    .filter(ModelResult::isFailure)
                    .map(ModelResult::modelId)
                    .toList();

            notifier.afterMetricEvaluation(MetricEvaluationResult.builder()
                    .metricName(getName())
                    .aggregatedScore(aggregatedScore)
                    .modelScores(modelScores)
                    .excludedModels(excludedModels)
                    .totalDuration(duration)
                    .metadata(Map.of("sample", sample, "config", config))
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
     * Response DTO for SimpleCriteriaScore metric evaluation
     */
    public record Response(
            @JsonPropertyDescription("The evaluation criteria that was used to score the response") String criteria,
            @JsonPropertyDescription(
                            "Numerical score within the specified range (e.g., 0-5) based on how well the response meets the criteria")
                    Double score,
            @JsonPropertyDescription(
                            "Detailed explanation of why this specific score was assigned, including analysis of strengths and weaknesses")
                    String reasoning) {
        public Double getNormalizedScore() {
            return score != null ? score : 0.0;
        }
    }

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
