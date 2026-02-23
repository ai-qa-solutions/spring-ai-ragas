package ai.qa.solutions.metrics.general;

import ai.qa.solutions.execution.ModelResult;
import ai.qa.solutions.execution.MultiModelExecutor;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationContext;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationResult;
import ai.qa.solutions.execution.listener.dto.ModelExclusionEvent;
import ai.qa.solutions.execution.listener.dto.StepResults;
import ai.qa.solutions.execution.listener.dto.StepType;
import ai.qa.solutions.metric.AbstractMultiModelMetric;
import ai.qa.solutions.metric.metadata.RubricsMetadata;
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
                .build());

        return executor.runAsync(() -> {
            final List<StepResults> accumulatedSteps = new ArrayList<>();
            final List<ModelExclusionEvent> accumulatedExclusions = new ArrayList<>();

            // ========== Step 1: Evaluate ==========
            final String prompt = renderPrompt(config, sample);
            final List<ModelResult<Response>> results = executor.executeLlm(modelIds, prompt, Response.class);

            // Collect scores and build metadata
            final Map<String, Double> modelScores = new HashMap<>();
            final List<String> excludedModels = new ArrayList<>();
            final Map<String, Integer> metadataModelScores = new HashMap<>();
            final Map<String, String> metadataModelRubricLevels = new HashMap<>();
            final Map<String, String> metadataModelReasonings = new HashMap<>();

            for (final ModelResult<Response> result : results) {
                if (result.isSuccess()) {
                    modelScores.put(result.modelId(), result.result().getNormalizedScore());
                    metadataModelScores.put(
                            result.modelId(),
                            result.result().score() != null ? result.result().score() : 0);
                    metadataModelRubricLevels.put(
                            result.modelId(),
                            result.result().rubric_level() != null
                                    ? result.result().rubric_level()
                                    : "");
                    metadataModelReasonings.put(
                            result.modelId(),
                            result.result().reasoning() != null
                                    ? result.result().reasoning()
                                    : "");
                } else {
                    excludedModels.add(result.modelId());
                    final ModelExclusionEvent exclusion = ModelExclusionEvent.builder()
                            .modelId(result.modelId())
                            .failedStepName("Evaluate")
                            .failedStepIndex(0)
                            .cause(result.error())
                            .build();
                    accumulatedExclusions.add(exclusion);
                }
            }

            accumulatedSteps.add(StepResults.builder()
                    .stepName("Evaluate")
                    .stepIndex(0)
                    .totalSteps(1)
                    .stepType(StepType.LLM)
                    .request(prompt)
                    .results(List.copyOf(results))
                    .build());

            if (modelScores.isEmpty()) {
                throw new IllegalStateException("All models failed for metric: " + getName());
            }

            final double aggregatedScore = aggregate(modelScores);

            // Notify with full results
            final Duration duration = Duration.between(startTime, Instant.now());

            notifier.afterMetricEvaluation(MetricEvaluationResult.builder()
                    .metricName(getName())
                    .sample(sample)
                    .config(config)
                    .modelIds(modelIds)
                    .aggregatedScore(aggregatedScore)
                    .modelScores(modelScores)
                    .excludedModels(excludedModels)
                    .totalDuration(duration)
                    .steps(accumulatedSteps)
                    .exclusions(accumulatedExclusions)
                    .metadata(new RubricsMetadata(
                            config.rubrics, metadataModelScores, metadataModelRubricLevels, metadataModelReasonings))
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

        @Builder.Default
        private String language = "en";

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
