package ai.qa.solutions.metrics.general;

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
 * RubricsScore Metric - Detailed rubric-based evaluation.
 * <p>
 * Uses {@link MultiModelExecutor} for parallel execution across multiple models
 * with explicit flow control and listener notifications.
 */
public class RubricsScoreMetric extends AbstractMultiModelMetric<RubricsScoreMetric.RubricsConfig> {
    public static final String DEFAULT_PROMPT_TEMPLATE =
            """
            Evaluate the AI response using the provided detailed rubrics.

            User Input: {user_input}
            AI Response: {response}
            {reference}

            Evaluation Rubrics:
            {rubrics}

            Instructions:
            1. Evaluate the AI response against each rubric level based on its quality
            2. Select the rubric level that best describes the response quality
            3. Provide the corresponding score and detailed reasoning

            Respond with a JSON object containing:
            - score: The numerical score (integer) corresponding to the selected rubric level
            - rubric_level: The key of the selected rubric (e.g., "score3_description")
            - reasoning: Your detailed explanation for the score selection
            """;

    private final String promptTemplate;

    @Builder(toBuilder = true)
    protected RubricsScoreMetric(final MultiModelExecutor executor, final String promptTemplate) {
        super(executor);
        this.promptTemplate = promptTemplate != null ? promptTemplate : DEFAULT_PROMPT_TEMPLATE;
    }

    @Override
    public Double singleTurnScore(final RubricsConfig config, final Sample sample) {
        return singleTurnScoreAsync(config, sample).join();
    }

    @Override
    public CompletableFuture<Double> singleTurnScoreAsync(final RubricsConfig config, final Sample sample) {
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

    private String renderPrompt(final RubricsConfig config, final Sample sample) {
        String referenceText = "";
        if (sample.getReference() != null && !sample.getReference().isEmpty()) {
            referenceText = "Reference Context: " + sample.getReference();
        }

        return PromptTemplate.builder()
                .template(this.promptTemplate)
                .variables(Map.of(
                        "user_input",
                        sample.getUserInput(),
                        "response",
                        sample.getResponse(),
                        "reference",
                        referenceText,
                        "rubrics",
                        buildRubricsText(config.rubrics)))
                .build()
                .render();
    }

    private String buildRubricsText(Map<String, String> rubrics) {
        if (rubrics == null || rubrics.isEmpty()) {
            throw new IllegalStateException("Rubrics must be provided");
        }

        final StringBuilder rubricsText = new StringBuilder();
        rubrics.entrySet().stream()
                .sorted(Map.Entry.<String, String>comparingByKey())
                .forEach(entry -> {
                    String scoreKey = entry.getKey(); // e.g., "score1_description"
                    String score = scoreKey.replaceAll("[^0-9]", ""); // Extract number
                    rubricsText
                            .append("Score ")
                            .append(score)
                            .append(": ")
                            .append(entry.getValue())
                            .append("\n");
                });
        return rubricsText.toString();
    }

    /**
     * Response DTO for RubricsScore metric evaluation
     */
    public record Response(
            @JsonPropertyDescription(
                            "Integer score (1-5) corresponding to the selected rubric level that best matches the response quality")
                    Integer score,
            @JsonPropertyDescription(
                            "The key identifier of the selected rubric level (e.g., 'score3_description') that was used for scoring")
                    String rubric_level,
            @JsonPropertyDescription(
                            "Comprehensive explanation of why this rubric level was selected, including specific evidence from the response that supports the score")
                    String reasoning) {
        public Double getNormalizedScore() {
            return score != null ? score.doubleValue() : 0.0;
        }
    }

    @Data
    @Builder
    public static class RubricsConfig implements MetricConfiguration {
        @Singular
        private List<String> models;

        @NonNull
        @Singular
        private Map<String, String> rubrics;

        @SuppressWarnings("unused")
        public void validateRubrics() {
            if (rubrics.isEmpty()) {
                throw new IllegalArgumentException("Rubrics cannot be null or empty");
            }
            // Validate that rubrics follow expected pattern (scoreN_description)
            boolean hasValidKeys = rubrics.keySet().stream().anyMatch(key -> key.matches("score\\d+_description"));

            if (!hasValidKeys) {
                throw new IllegalArgumentException("Rubrics must contain keys in format 'scoreN_description'");
            }
        }
    }
}
