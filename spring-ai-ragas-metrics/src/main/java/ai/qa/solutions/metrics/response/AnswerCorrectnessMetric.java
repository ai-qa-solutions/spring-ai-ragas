package ai.qa.solutions.metrics.response;

import ai.qa.solutions.execution.ModelResult;
import ai.qa.solutions.execution.MultiModelExecutor;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationContext;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationResult;
import ai.qa.solutions.metric.AbstractMultiModelMetric;
import ai.qa.solutions.sample.Sample;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;

/**
 * Answer Correctness Metric - combines factual correctness and semantic similarity.
 * <p>
 * This metric provides a comprehensive evaluation of response correctness by
 * combining two complementary metrics:
 * <ul>
 *   <li>Factual Correctness (75% default weight) - verifies factual accuracy via NLI</li>
 *   <li>Semantic Similarity (25% default weight) - measures semantic closeness via embeddings</li>
 * </ul>
 * <p>
 * The weighted combination captures both:
 * <ul>
 *   <li>Whether specific facts are correct (factual)</li>
 *   <li>Whether the overall meaning is similar (semantic)</li>
 * </ul>
 * <p>
 * <strong>Algorithm:</strong>
 * <ol>
 *   <li>Compute FactualCorrectness score (decompose claims + NLI verification)</li>
 *   <li>Compute SemanticSimilarity score (embeddings + cosine similarity)</li>
 *   <li>Return weighted average: factualWeight * factual + semanticWeight * semantic</li>
 * </ol>
 * <p>
 * <strong>Score interpretation:</strong>
 * <ul>
 *   <li>1.0 - Perfect factual accuracy and semantic match</li>
 *   <li>0.8-1.0 - High correctness, minor differences</li>
 *   <li>0.5-0.8 - Moderate correctness, some factual or semantic gaps</li>
 *   <li>0.0-0.5 - Low correctness, significant factual errors or semantic divergence</li>
 * </ul>
 * <p>
 * Based on RAGAS AnswerCorrectness metric.
 *
 * @author Artem Simeshin
 * @see FactualCorrectnessMetric
 * @see SemanticSimilarityMetric
 * @see Sample
 * @see AnswerCorrectnessConfig
 * @since 1.0
 */
@Slf4j
public class AnswerCorrectnessMetric extends AbstractMultiModelMetric<AnswerCorrectnessMetric.AnswerCorrectnessConfig> {

    private final FactualCorrectnessMetric factualCorrectnessMetric;
    private final SemanticSimilarityMetric semanticSimilarityMetric;

    @Builder(toBuilder = true)
    protected AnswerCorrectnessMetric(
            final MultiModelExecutor executor,
            final FactualCorrectnessMetric factualCorrectnessMetric,
            final SemanticSimilarityMetric semanticSimilarityMetric) {
        super(executor);
        this.factualCorrectnessMetric = factualCorrectnessMetric != null
                ? factualCorrectnessMetric
                : FactualCorrectnessMetric.builder().executor(executor).build();
        this.semanticSimilarityMetric = semanticSimilarityMetric != null
                ? semanticSimilarityMetric
                : SemanticSimilarityMetric.builder().executor(executor).build();
    }

    /**
     * Convenience method for single-turn scoring with default configuration.
     *
     * @param sample the sample to evaluate
     * @return the answer correctness score
     */
    public Double singleTurnScore(final Sample sample) {
        return singleTurnScore(AnswerCorrectnessConfig.builder().build(), sample);
    }

    /**
     * Convenience method for async single-turn scoring with default configuration.
     *
     * @param sample the sample to evaluate
     * @return future with the answer correctness score
     */
    public CompletableFuture<Double> singleTurnScoreAsync(final Sample sample) {
        return singleTurnScoreAsync(AnswerCorrectnessConfig.builder().build(), sample);
    }

    @Override
    public Double singleTurnScore(final AnswerCorrectnessConfig config, final Sample sample) {
        return singleTurnScoreAsync(config, sample).join();
    }

    @Override
    public CompletableFuture<Double> singleTurnScoreAsync(final AnswerCorrectnessConfig config, final Sample sample) {

        // Validate required inputs
        final String response = sample.getResponse();
        if (response == null || response.trim().isEmpty()) {
            log.warn("No response provided for Answer Correctness evaluation");
            return CompletableFuture.completedFuture(0.0);
        }

        final String reference = sample.getReference();
        if (reference == null || reference.trim().isEmpty()) {
            log.warn("No reference provided for Answer Correctness evaluation");
            return CompletableFuture.completedFuture(0.0);
        }

        final Instant startTime = Instant.now();
        final List<String> modelIds =
                config.models != null && !config.models.isEmpty() ? config.models : executor.getModelIds();
        final List<String> embeddingModelIds = executor.getEmbeddingModelIds();

        // Create evaluation-specific notifier for thread-safe parallel execution
        final EvaluationNotifier notifier = createEvaluationNotifier();

        // Notify listeners before evaluation
        notifier.beforeMetricEvaluation(MetricEvaluationContext.builder()
                .metricName(getName())
                .sample(sample)
                .config(config)
                .modelIds(modelIds)
                .embeddingModelIds(embeddingModelIds)
                .totalSteps(3) // ComputeFactualCorrectness -> ComputeSemanticSimilarity -> CombineScores
                .metadata(Map.of("sample", sample, "config", config))
                .build());

        return executor.runAsync(() -> {
            log.debug("Computing answer correctness evaluation");

            // Validate weights
            final double factualWeight = config.getFactualWeight();
            final double semanticWeight = config.getSemanticWeight();
            final double totalWeight = factualWeight + semanticWeight;

            if (totalWeight <= 0) {
                log.warn("Invalid weights: factual={}, semantic={}. Using defaults.", factualWeight, semanticWeight);
                return 0.0;
            }

            // Normalize weights to sum to 1
            final double normalizedFactualWeight = factualWeight / totalWeight;
            final double normalizedSemanticWeight = semanticWeight / totalWeight;

            // ========== Step 1: Compute Factual Correctness ==========
            notifier.beforeStep("ComputeFactualCorrectness", 0, 3);

            final FactualCorrectnessMetric.FactualCorrectnessConfig factualConfig =
                    FactualCorrectnessMetric.FactualCorrectnessConfig.builder()
                            .mode(FactualCorrectnessMetric.Mode.F1)
                            .build();

            Double factualScore;
            try {
                factualScore = factualCorrectnessMetric.singleTurnScore(factualConfig, sample);
                if (factualScore == null) {
                    factualScore = 0.0;
                }
            } catch (final Exception e) {
                log.warn("Factual correctness evaluation failed: {}", e.getMessage());
                factualScore = 0.0;
            }

            // Create result for notification
            final List<ModelResult<Double>> step1Results =
                    List.of(ModelResult.success("factual", factualScore, Duration.ZERO, "compute"));
            notifier.afterComputeStep("ComputeFactualCorrectness", 0, 3, step1Results);

            // ========== Step 2: Compute Semantic Similarity ==========
            notifier.beforeStep("ComputeSemanticSimilarity", 1, 3);

            final SemanticSimilarityMetric.SemanticSimilarityConfig semanticConfig =
                    SemanticSimilarityMetric.SemanticSimilarityConfig.builder().build();

            Double semanticScore;
            try {
                semanticScore = semanticSimilarityMetric.singleTurnScore(semanticConfig, sample);
                if (semanticScore == null) {
                    semanticScore = 0.0;
                }
            } catch (final Exception e) {
                log.warn("Semantic similarity evaluation failed: {}", e.getMessage());
                semanticScore = 0.0;
            }

            // Create result for notification
            final List<ModelResult<Double>> step2Results =
                    List.of(ModelResult.success("semantic", semanticScore, Duration.ZERO, "compute"));
            notifier.afterComputeStep("ComputeSemanticSimilarity", 1, 3, step2Results);

            // ========== Step 3: Combine Scores ==========
            notifier.beforeStep("CombineScores", 2, 3);

            final double combinedScore =
                    (normalizedFactualWeight * factualScore) + (normalizedSemanticWeight * semanticScore);

            log.debug(
                    "Answer correctness: factual={:.4f} (weight={:.2f}), semantic={:.4f} (weight={:.2f}) -> combined={:.4f}",
                    factualScore,
                    normalizedFactualWeight,
                    semanticScore,
                    normalizedSemanticWeight,
                    combinedScore);

            // Create result for notification
            final List<ModelResult<Double>> step3Results =
                    List.of(ModelResult.success("combined", combinedScore, Duration.ZERO, "compute"));
            notifier.afterComputeStep("CombineScores", 2, 3, step3Results);

            // Build model scores map for result (use combined score for the "aggregated" entry)
            final Map<String, Double> modelScores = new HashMap<>();
            modelScores.put("factual", factualScore);
            modelScores.put("semantic", semanticScore);
            modelScores.put("combined", combinedScore);

            // Notify with full results
            final Duration duration = Duration.between(startTime, Instant.now());
            notifier.afterMetricEvaluation(MetricEvaluationResult.builder()
                    .metricName(getName())
                    .aggregatedScore(combinedScore)
                    .modelScores(modelScores)
                    .totalDuration(duration)
                    .metadata(Map.of(
                            "sample",
                            sample,
                            "config",
                            config,
                            "factualScore",
                            factualScore,
                            "semanticScore",
                            semanticScore,
                            "factualWeight",
                            normalizedFactualWeight,
                            "semanticWeight",
                            normalizedSemanticWeight))
                    .build());

            return combinedScore;
        });
    }

    /**
     * Configuration class for Answer Correctness metric parameters.
     */
    @Data
    @Builder
    public static class AnswerCorrectnessConfig implements MetricConfiguration {

        /**
         * Weight for factual correctness component.
         * Default is 0.75 (75%).
         */
        @Builder.Default
        private double factualWeight = 0.75;

        /**
         * Weight for semantic similarity component.
         * Default is 0.25 (25%).
         */
        @Builder.Default
        private double semanticWeight = 0.25;

        /**
         * List of model IDs to use for multi-model execution.
         * If empty, all available models from executor will be used.
         */
        @Singular
        private List<String> models;

        /**
         * Creates a default configuration instance.
         *
         * @return Default configuration with 75/25 weights
         */
        public static AnswerCorrectnessConfig defaultConfig() {
            return AnswerCorrectnessConfig.builder().build();
        }

        /**
         * Creates a configuration with equal weights.
         *
         * @return Configuration with 50/50 weights
         */
        public static AnswerCorrectnessConfig equalWeights() {
            return AnswerCorrectnessConfig.builder()
                    .factualWeight(0.5)
                    .semanticWeight(0.5)
                    .build();
        }

        /**
         * Creates a configuration focused on factual correctness.
         *
         * @return Configuration with 90/10 weights
         */
        public static AnswerCorrectnessConfig factualFocused() {
            return AnswerCorrectnessConfig.builder()
                    .factualWeight(0.9)
                    .semanticWeight(0.1)
                    .build();
        }

        /**
         * Creates a configuration focused on semantic similarity.
         *
         * @return Configuration with 10/90 weights
         */
        public static AnswerCorrectnessConfig semanticFocused() {
            return AnswerCorrectnessConfig.builder()
                    .factualWeight(0.1)
                    .semanticWeight(0.9)
                    .build();
        }
    }
}
