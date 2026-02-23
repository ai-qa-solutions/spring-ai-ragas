package ai.qa.solutions.metrics.response;

import ai.qa.solutions.execution.ModelResult;
import ai.qa.solutions.execution.MultiModelExecutor;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationContext;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationResult;
import ai.qa.solutions.execution.listener.dto.ModelExclusionEvent;
import ai.qa.solutions.execution.listener.dto.StepResults;
import ai.qa.solutions.execution.listener.dto.StepType;
import ai.qa.solutions.metric.AbstractMultiModelMetric;
import ai.qa.solutions.metric.metadata.SemanticSimilarityMetadata;
import ai.qa.solutions.sample.Sample;
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

/**
 * Semantic Similarity Metric - measures the semantic similarity between response and reference.
 * <p>
 * This metric uses embedding models to compute vector representations of the response and reference
 * texts, then calculates the cosine similarity between them. It does not require any LLM calls,
 * making it fast and cost-effective for large-scale evaluations.
 * <p>
 * <strong>Algorithm:</strong>
 * <ol>
 *   <li>Compute embeddings for both response and reference texts</li>
 *   <li>Calculate cosine similarity between the embedding vectors</li>
 *   <li>Optionally apply threshold for binary pass/fail classification</li>
 * </ol>
 * <p>
 * <strong>Score interpretation:</strong>
 * <ul>
 *   <li>1.0 - Semantically identical (vectors point in same direction)</li>
 *   <li>0.8-1.0 - High similarity, very close in meaning</li>
 *   <li>0.5-0.8 - Moderate similarity, related but different phrasing</li>
 *   <li>0.0-0.5 - Low similarity, different meanings</li>
 *   <li>-1.0 to 0.0 - Negative similarity (rare in practice for text embeddings)</li>
 * </ul>
 * <p>
 * Based on the Sentence Transformers paper: <a href="https://arxiv.org/pdf/2108.06130.pdf">SAS</a>
 *
 * @author Artem Simeshin
 * @see Sample
 * @see SemanticSimilarityConfig
 * @since 1.0
 */
@Slf4j
public class SemanticSimilarityMetric
        extends AbstractMultiModelMetric<SemanticSimilarityMetric.SemanticSimilarityConfig> {

    @Builder(toBuilder = true)
    protected SemanticSimilarityMetric(final MultiModelExecutor executor) {
        super(executor);
    }

    /**
     * Convenience method for single-turn scoring with default configuration.
     *
     * @param sample the sample to evaluate
     * @return the semantic similarity score
     */
    public Double singleTurnScore(final Sample sample) {
        return singleTurnScore(SemanticSimilarityConfig.builder().build(), sample);
    }

    /**
     * Convenience method for async single-turn scoring with default configuration.
     *
     * @param sample the sample to evaluate
     * @return future with the semantic similarity score
     */
    public CompletableFuture<Double> singleTurnScoreAsync(final Sample sample) {
        return singleTurnScoreAsync(SemanticSimilarityConfig.builder().build(), sample);
    }

    @Override
    public Double singleTurnScore(final SemanticSimilarityConfig config, final Sample sample) {
        return singleTurnScoreAsync(config, sample).join();
    }

    @Override
    public CompletableFuture<Double> singleTurnScoreAsync(final SemanticSimilarityConfig config, final Sample sample) {

        // Validate required inputs
        final String response = sample.getResponse();
        if (response == null || response.trim().isEmpty()) {
            log.warn("No response provided for Semantic Similarity evaluation");
            return CompletableFuture.completedFuture(0.0);
        }

        final String reference = sample.getReference();
        if (reference == null || reference.trim().isEmpty()) {
            log.warn("No reference provided for Semantic Similarity evaluation");
            return CompletableFuture.completedFuture(0.0);
        }

        final Instant startTime = Instant.now();
        final List<String> embeddingModelIds = executor.getEmbeddingModelIds();

        if (embeddingModelIds.isEmpty()) {
            log.error("No embedding models available for Semantic Similarity evaluation");
            return CompletableFuture.completedFuture(0.0);
        }

        // Create evaluation-specific notifier for thread-safe parallel execution
        final EvaluationNotifier notifier = createEvaluationNotifier();

        // Notify listeners before evaluation
        notifier.beforeMetricEvaluation(MetricEvaluationContext.builder()
                .metricName(getName())
                .sample(sample)
                .config(config)
                .modelIds(List.of())
                .embeddingModelIds(embeddingModelIds)
                .totalSteps(2) // ComputeEmbeddings -> ComputeCosineSimilarity
                .build());

        return executor.runAsync(() -> {
            log.debug("Computing semantic similarity evaluation with explicit flow");

            // Local accumulators for step results and exclusions
            final List<StepResults> accumulatedSteps = new ArrayList<>();
            final List<ModelExclusionEvent> accumulatedExclusions = new ArrayList<>();

            // Track excluded models across all steps
            final List<String> excludedModels = new ArrayList<>();

            // ========== Step 1: Compute embeddings ==========
            // Prepare texts for embedding: [response, reference]
            final List<String> textsToEmbed = List.of(
                    response.trim().isEmpty() ? " " : response, // Handle empty strings
                    reference.trim().isEmpty() ? " " : reference);

            // Execute embeddings asynchronously
            final CompletableFuture<List<ModelResult<List<float[]>>>> embeddingsFuture =
                    executor.executeEmbeddingsAsync(textsToEmbed);

            final List<ModelResult<List<float[]>>> embeddingResults = embeddingsFuture.join();

            // Convert to step results for accumulation
            final List<ModelResult<?>> step1LlmResults = new ArrayList<>();
            final Map<String, EmbeddingsResult> step1Successful = new HashMap<>();

            for (final ModelResult<List<float[]>> result : embeddingResults) {
                if (result.isSuccess()) {
                    final List<float[]> embeddings = result.result();
                    if (embeddings != null && embeddings.size() >= 2) {
                        final double[] responseEmbedding = convertToDoubleArray(embeddings.get(0));
                        final double[] referenceEmbedding = convertToDoubleArray(embeddings.get(1));

                        final EmbeddingsResult embResult = new EmbeddingsResult(responseEmbedding, referenceEmbedding);
                        step1Successful.put(result.modelId(), embResult);
                        step1LlmResults.add(
                                ModelResult.success(result.modelId(), embResult, result.duration(), result.request()));
                    } else {
                        log.warn("Insufficient embeddings returned from model {}", result.modelId());
                        excludedModels.add(result.modelId());
                        final ModelExclusionEvent exclusion = ModelExclusionEvent.builder()
                                .modelId(result.modelId())
                                .failedStepName("ComputeEmbeddings")
                                .failedStepIndex(0)
                                .cause(new IllegalStateException("Insufficient embeddings returned"))
                                .build();
                        accumulatedExclusions.add(exclusion);
                    }
                } else {
                    excludedModels.add(result.modelId());
                    log.warn(
                            "Embedding model {} failed: {}",
                            result.modelId(),
                            result.error() != null ? result.error().getMessage() : "unknown error");
                    final ModelExclusionEvent exclusion = ModelExclusionEvent.builder()
                            .modelId(result.modelId())
                            .failedStepName("ComputeEmbeddings")
                            .failedStepIndex(0)
                            .cause(result.error())
                            .build();
                    accumulatedExclusions.add(exclusion);
                }
            }

            // Build embedding model results for the step
            final List<ModelResult<?>> embeddingModelResultsList = new ArrayList<ModelResult<?>>(embeddingResults);

            accumulatedSteps.add(StepResults.builder()
                    .stepName("ComputeEmbeddings")
                    .stepIndex(0)
                    .totalSteps(2)
                    .stepType(StepType.EMBEDDING)
                    .request("response + reference")
                    .results(step1LlmResults)
                    .embeddingModelResults(embeddingModelResultsList)
                    .build());

            if (step1Successful.isEmpty()) {
                throw new IllegalStateException(
                        "All embedding models failed at step ComputeEmbeddings for metric: " + getName());
            }

            // ========== Step 2: Compute cosine similarity ==========
            final Map<String, Double> modelScores = new HashMap<>();

            for (final Map.Entry<String, EmbeddingsResult> entry : step1Successful.entrySet()) {
                final String modelId = entry.getKey();
                final EmbeddingsResult embResult = entry.getValue();

                try {
                    final double similarity =
                            cosineSimilarity(embResult.responseEmbedding(), embResult.referenceEmbedding());

                    // Apply threshold if configured
                    final double finalScore;
                    if (config.getThreshold() != null && config.getThreshold() > 0) {
                        finalScore = similarity >= config.getThreshold() ? 1.0 : 0.0;
                    } else {
                        finalScore = similarity;
                    }

                    modelScores.put(modelId, finalScore);
                    log.debug("Semantic similarity for model {}: {} (raw: {})", modelId, finalScore, similarity);

                } catch (final Exception e) {
                    log.warn("Failed to calculate similarity for model {}: {}", modelId, e.getMessage());
                    excludedModels.add(modelId);
                    final ModelExclusionEvent exclusion = ModelExclusionEvent.builder()
                            .modelId(modelId)
                            .failedStepName("ComputeCosineSimilarity")
                            .failedStepIndex(1)
                            .cause(e)
                            .build();
                    accumulatedExclusions.add(exclusion);
                }
            }

            // Create synthetic results for step accumulation
            final List<ModelResult<?>> step2Results = new ArrayList<>();
            for (final Map.Entry<String, Double> e : modelScores.entrySet()) {
                step2Results.add(ModelResult.success(e.getKey(), e.getValue(), Duration.ZERO, "compute"));
            }

            accumulatedSteps.add(StepResults.builder()
                    .stepName("ComputeCosineSimilarity")
                    .stepIndex(1)
                    .totalSteps(2)
                    .stepType(StepType.COMPUTE)
                    .results(step2Results)
                    .build());

            if (modelScores.isEmpty()) {
                throw new IllegalStateException("All models failed to compute similarity for metric: " + getName());
            }

            final double aggregatedScore = aggregate(modelScores);

            // Notify with full results
            final Duration duration = Duration.between(startTime, Instant.now());
            notifier.afterMetricEvaluation(MetricEvaluationResult.builder()
                    .metricName(getName())
                    .sample(sample)
                    .config(config)
                    .embeddingModelIds(embeddingModelIds)
                    .aggregatedScore(aggregatedScore)
                    .modelScores(modelScores)
                    .excludedModels(excludedModels)
                    .totalDuration(duration)
                    .steps(accumulatedSteps)
                    .exclusions(accumulatedExclusions)
                    .metadata(new SemanticSimilarityMetadata(new HashMap<>(modelScores), config.getThreshold()))
                    .build());

            return aggregatedScore;
        });
    }

    /**
     * Converts float array to double array for more precise mathematical operations.
     */
    private double[] convertToDoubleArray(final float[] floatArray) {
        final double[] doubleArray = new double[floatArray.length];
        for (int i = 0; i < floatArray.length; i++) {
            doubleArray[i] = floatArray[i];
        }
        return doubleArray;
    }

    /**
     * Calculates cosine similarity between two embedding vectors.
     * <p>
     * The vectors are L2-normalized before computing the dot product,
     * following the standard practice for semantic similarity.
     *
     * @param vectorA First embedding vector
     * @param vectorB Second embedding vector
     * @return Cosine similarity score in range [-1, 1], typically [0, 1] for text embeddings
     * @throws IllegalArgumentException if vectors have different dimensions
     */
    private double cosineSimilarity(final double[] vectorA, final double[] vectorB) {
        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException(
                    "Vectors must have same length: " + vectorA.length + " vs " + vectorB.length);
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        normA = Math.sqrt(normA);
        normB = Math.sqrt(normB);

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (normA * normB);
    }

    /**
     * Result DTO for embeddings computation step.
     *
     * @param responseEmbedding  Embedding vector for the response
     * @param referenceEmbedding Embedding vector for the reference
     */
    public record EmbeddingsResult(double[] responseEmbedding, double[] referenceEmbedding) {}

    /**
     * Configuration class for Semantic Similarity metric parameters.
     */
    @Data
    @Builder
    public static class SemanticSimilarityConfig implements MetricConfiguration {

        /**
         * Optional threshold for binary pass/fail classification.
         * If set, scores above threshold return 1.0, below return 0.0.
         * If null or 0, returns the raw cosine similarity score.
         */
        @Builder.Default
        private Double threshold = null;

        /**
         * List of embedding model IDs to use for multi-model execution.
         * If empty, all available embedding models from executor will be used.
         */
        @Singular
        private List<String> models;

        /**
         * Creates a default configuration instance.
         *
         * @return Default configuration without threshold
         */
        public static SemanticSimilarityConfig defaultConfig() {
            return SemanticSimilarityConfig.builder().build();
        }
    }
}
