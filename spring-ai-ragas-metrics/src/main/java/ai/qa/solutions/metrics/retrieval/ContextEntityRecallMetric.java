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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.PromptTemplate;

/**
 * Context Entity Recall Metric - LLM-based evaluation measuring the recall of entities
 * present in both reference and retrieved contexts relative to entities in reference alone.
 * <p>
 * Uses {@link MultiModelExecutor} for parallel execution across multiple models
 * with explicit flow control and listener notifications.
 * <p>
 * This metric is particularly useful in fact-based use cases like tourism help desk,
 * historical QA, etc., where entity coverage is crucial for evaluating retrieval mechanisms.
 * <p>
 * Score ranges from 0.0 to 1.0, where higher scores indicate better entity coverage.
 */
@Slf4j
public class ContextEntityRecallMetric
        extends AbstractMultiModelMetric<ContextEntityRecallMetric.ContextEntityRecallConfig> {
    public static final String DEFAULT_ENTITY_EXTRACTION_PROMPT =
            """
                    Given a text, extract unique entities without repetition. Ensure you consider different forms or mentions of the same entity as a single entity.

                    Text: {text}

                    Instructions:
                    1. Extract all named entities including:
                       - Person names (e.g., "Albert Einstein", "Napoleon")
                       - Place names (e.g., "Paris", "Eiffel Tower", "France")
                       - Organizations (e.g., "UNESCO", "European Union")
                       - Dates and times (e.g., "1889", "July 16, 1969", "7th century BC")
                       - Events (e.g., "World War II", "Apollo 11 mission")
                       - Products/objects (e.g., "iPhone", "Great Wall of China")
                       - Numbers and measurements (e.g., "21,196 kilometers", "50,000 spectators")
                    2. Avoid duplicates - treat different forms of the same entity as one
                    3. Focus on factual, concrete entities rather than abstract concepts
                    4. Include proper nouns, specific dates, numbers, and measurable quantities
                    5. Exclude common words, pronouns, and generic terms

                    Examples:
                    - "The Eiffel Tower, located in Paris, France, was completed in 1889 for the World's Fair."
                      Entities: ["Eiffel Tower", "Paris", "France", "1889", "World's Fair"]

                    - "Neil Armstrong and Buzz Aldrin landed on the Moon during Apollo 11 on July 16, 1969."
                      Entities: ["Neil Armstrong", "Buzz Aldrin", "Moon", "Apollo 11", "July 16, 1969"]

                    Respond with a JSON object containing:
                    - entities: A list of unique entities extracted from the text
                    """;

    private final String entityExtractionPrompt;

    @Builder(toBuilder = true)
    protected ContextEntityRecallMetric(final MultiModelExecutor executor, final String entityExtractionPrompt) {
        super(executor);
        this.entityExtractionPrompt =
                entityExtractionPrompt != null ? entityExtractionPrompt : DEFAULT_ENTITY_EXTRACTION_PROMPT;
    }

    /**
     * Convenience method for single-turn scoring with default configuration.
     *
     * @param sample the sample to evaluate
     * @return the context entity recall score
     */
    public Double singleTurnScore(final Sample sample) {
        return singleTurnScore(ContextEntityRecallConfig.builder().build(), sample);
    }

    /**
     * Convenience method for async single-turn scoring with default configuration.
     *
     * @param sample the sample to evaluate
     * @return future with the context entity recall score
     */
    public CompletableFuture<Double> singleTurnScoreAsync(final Sample sample) {
        return singleTurnScoreAsync(ContextEntityRecallConfig.builder().build(), sample);
    }

    @Override
    public Double singleTurnScore(final ContextEntityRecallConfig config, final Sample sample) {
        return singleTurnScoreAsync(config, sample).join();
    }

    @Override
    public CompletableFuture<Double> singleTurnScoreAsync(final ContextEntityRecallConfig config, final Sample sample) {
        // Validate required inputs
        final String reference = sample.getReference();
        if (reference == null || reference.trim().isEmpty()) {
            log.warn("No reference provided for Context Entity Recall evaluation");
            return CompletableFuture.completedFuture(0.0);
        }

        final List<String> retrievedContexts = sample.getRetrievedContexts();
        if (retrievedContexts == null || retrievedContexts.isEmpty()) {
            log.warn("No retrieved contexts provided for Context Entity Recall evaluation");
            return CompletableFuture.completedFuture(0.0);
        }

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
                .totalSteps(3) // Extract reference entities -> Extract context entities -> Compute recall
                .metadata(Map.of("sample", sample, "config", config))
                .build());

        return CompletableFuture.supplyAsync(() -> {
            log.debug("Computing context entity recall evaluation with explicit flow");

            // Track excluded models across all steps
            final List<String> excludedModels = new java.util.ArrayList<>();

            // ========== Step 1: Extract entities from reference ==========
            notifier.beforeStep("ExtractReferenceEntities", 0, 3);

            final String referencePrompt = renderEntityExtractionPrompt(reference);
            final List<ModelResult<EntitiesResponse>> step1Results =
                    executor.executeLlm(modelIds, referencePrompt, EntitiesResponse.class);

            notifier.afterLlmStep("ExtractReferenceEntities", 0, 3, referencePrompt, step1Results);

            // Collect successful results from step 1
            final Map<String, EntitiesResponse> step1Successful = new HashMap<>();
            for (final ModelResult<EntitiesResponse> result : step1Results) {
                if (result.isSuccess()) {
                    step1Successful.put(result.modelId(), result.result());
                } else {
                    excludedModels.add(result.modelId());
                    notifier.onModelExcluded(ModelExclusionEvent.builder()
                            .modelId(result.modelId())
                            .failedStepName("ExtractReferenceEntities")
                            .failedStepIndex(0)
                            .cause(result.error())
                            .build());
                }
            }

            if (step1Successful.isEmpty()) {
                throw new IllegalStateException(
                        "All models failed at step ExtractReferenceEntities for metric: " + getName());
            }

            // ========== Step 2: Extract entities from retrieved contexts ==========
            notifier.beforeStep("ExtractContextEntities", 1, 3);

            final String combinedContexts = String.join("\n\n", retrievedContexts);
            final String contextPrompt = renderEntityExtractionPrompt(combinedContexts);

            // Execute in parallel for all models that succeeded in step 1
            final List<String> step1ModelIds = new java.util.ArrayList<>(step1Successful.keySet());
            final List<CompletableFuture<ModelResult<EntitiesResponse>>> step2Futures = step1ModelIds.stream()
                    .map(modelId -> executor.executeLlmOnModelAsync(modelId, contextPrompt, EntitiesResponse.class))
                    .toList();
            CompletableFuture.allOf(step2Futures.toArray(new CompletableFuture[0]))
                    .join();
            final List<ModelResult<EntitiesResponse>> step2Results =
                    step2Futures.stream().map(CompletableFuture::join).toList();

            notifier.afterLlmStep("ExtractContextEntities", 1, 3, contextPrompt, step2Results);

            // Collect successful results from step 2
            final Map<String, EntitiesResponse> step2Successful = new HashMap<>();
            for (final ModelResult<EntitiesResponse> result : step2Results) {
                if (result.isSuccess()) {
                    step2Successful.put(result.modelId(), result.result());
                } else {
                    excludedModels.add(result.modelId());
                    notifier.onModelExcluded(ModelExclusionEvent.builder()
                            .modelId(result.modelId())
                            .failedStepName("ExtractContextEntities")
                            .failedStepIndex(1)
                            .cause(result.error())
                            .build());
                }
            }

            if (step2Successful.isEmpty()) {
                throw new IllegalStateException(
                        "All models failed at step ExtractContextEntities for metric: " + getName());
            }

            // ========== Step 3: Compute entity recall ==========
            notifier.beforeStep("ComputeEntityRecall", 2, 3);

            final Map<String, Double> modelScores = new HashMap<>();
            for (final String modelId : step2Successful.keySet()) {
                final EntitiesResponse referenceEntitiesResponse = step1Successful.get(modelId);
                final EntitiesResponse contextEntitiesResponse = step2Successful.get(modelId);

                final double score = calculateEntityRecall(referenceEntitiesResponse, contextEntitiesResponse);
                modelScores.put(modelId, score);
            }

            // Create synthetic results for notification
            final List<ModelResult<Double>> step3Results = modelScores.entrySet().stream()
                    .map(e -> ModelResult.success(e.getKey(), e.getValue(), Duration.ZERO, "compute"))
                    .toList();

            notifier.afterComputeStep("ComputeEntityRecall", 2, 3, step3Results);

            final double aggregatedScore = aggregate(modelScores);

            // Notify with full results
            final Duration duration = Duration.between(startTime, Instant.now());
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

    private String renderEntityExtractionPrompt(final String text) {
        return PromptTemplate.builder()
                .template(this.entityExtractionPrompt)
                .variables(Map.of("text", text))
                .build()
                .render();
    }

    private double calculateEntityRecall(
            final EntitiesResponse referenceEntitiesResponse, final EntitiesResponse contextEntitiesResponse) {
        if (referenceEntitiesResponse == null || referenceEntitiesResponse.entities() == null) {
            log.warn("No reference entities found");
            return 0.0;
        }

        if (contextEntitiesResponse == null || contextEntitiesResponse.entities() == null) {
            log.warn("No context entities found");
            return 0.0;
        }

        // Convert to lowercase sets for case-insensitive comparison
        final Set<String> referenceEntities = normalizeEntities(referenceEntitiesResponse.entities());
        final Set<String> contextEntities = normalizeEntities(contextEntitiesResponse.entities());

        if (referenceEntities.isEmpty()) {
            log.warn("No entities extracted from reference");
            return 0.0;
        }

        // Find intersection of entities
        final Set<String> commonEntities = new HashSet<>(referenceEntities);
        commonEntities.retainAll(contextEntities);

        log.debug("Reference entities: {}", referenceEntities);
        log.debug("Context entities: {}", contextEntities);
        log.debug("Common entities: {}", commonEntities);

        // Entity recall = |intersection| / |reference entities|
        final double recall = (double) commonEntities.size() / referenceEntities.size();

        log.debug("Entity recall: {} / {} = {}", commonEntities.size(), referenceEntities.size(), recall);

        return recall;
    }

    /**
     * Normalizes a list of entities to lowercase for case-insensitive comparison.
     */
    private Set<String> normalizeEntities(final List<String> entities) {
        final Set<String> normalized = new HashSet<>();
        for (String entity : entities) {
            if (entity != null && !entity.trim().isEmpty()) {
                normalized.add(entity.trim().toLowerCase());
            }
        }
        return normalized;
    }

    /**
     * Response DTO for entity extraction
     */
    public record EntitiesResponse(
            @JsonPropertyDescription("List of unique entities extracted from the text") List<String> entities) {}

    @Data
    @Builder
    public static class ContextEntityRecallConfig implements MetricConfiguration {
        @Singular
        private List<String> models;
    }
}
