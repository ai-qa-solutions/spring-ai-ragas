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
                    modelScores.put(result.modelId(), result.result().getScore());
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
