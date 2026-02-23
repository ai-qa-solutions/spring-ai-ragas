package ai.qa.solutions.metric;

import ai.qa.solutions.execution.MultiModelExecutor;
import ai.qa.solutions.execution.ScoreAggregator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract base class for LLM-based metrics with multi-model execution support.
 * <p>
 * Extends {@link AbstractMetric} with:
 * <ul>
 *   <li>Access to the {@link MultiModelExecutor} for parallel LLM/embedding calls</li>
 *   <li>Score aggregation helpers for combining results from multiple models</li>
 * </ul>
 * <p>
 * Listener management and {@link EvaluationNotifier} are inherited from {@link AbstractMetric}.
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * public class FaithfulnessMetric extends AbstractMultiModelMetric<FaithfulnessConfig> {
 *
 *     @Override
 *     public CompletableFuture<Double> singleTurnScoreAsync(
 *             FaithfulnessConfig config, Sample sample) {
 *
 *         // Create thread-safe notifier for this evaluation
 *         EvaluationNotifier notifier = createEvaluationNotifier();
 *
 *         notifier.beforeMetricEvaluation(MetricEvaluationContext.builder()
 *             .metricName(getName())
 *             .sample(sample)
 *             .totalSteps(3)
 *             .build());
 *
 *         // Execute LLM calls via executor
 *         List<ModelResult<StatementsResponse>> results =
 *             executor.executeLlm(modelIds, prompt, StatementsResponse.class);
 *
 *         // Aggregate
 *         double score = aggregate(modelScores);
 *
 *         notifier.afterMetricEvaluation(MetricEvaluationResult.builder()
 *             .metricName(getName())
 *             .aggregatedScore(score)
 *             .build());
 *
 *         return CompletableFuture.completedFuture(score);
 *     }
 * }
 * }</pre>
 *
 * @param <T> the configuration type for this metric
 */
@Slf4j
public abstract class AbstractMultiModelMetric<T extends Metric.MetricConfiguration> extends AbstractMetric<T> {

    /**
     * The executor for multi-model parallel execution.
     * <p>
     * Provides methods for:
     * <ul>
     *   <li>{@link MultiModelExecutor#executeLlm} - execute LLM calls</li>
     *   <li>{@link MultiModelExecutor#executeEmbedding} - execute embeddings</li>
     * </ul>
     */
    protected final MultiModelExecutor executor;

    /**
     * Default aggregation strategy for combining scores from multiple models.
     */
    protected final ScoreAggregator defaultAggregator;

    /**
     * Creates a new metric with the specified executor and AVERAGE aggregation.
     *
     * @param executor the multi-model executor for parallel evaluation
     */
    protected AbstractMultiModelMetric(final MultiModelExecutor executor) {
        this(executor, ScoreAggregator.AVERAGE);
    }

    /**
     * Creates a new metric with the specified executor and aggregation strategy.
     *
     * @param executor          the multi-model executor for parallel evaluation
     * @param defaultAggregator the default aggregation strategy
     */
    protected AbstractMultiModelMetric(final MultiModelExecutor executor, final ScoreAggregator defaultAggregator) {
        this.executor = Objects.requireNonNull(executor, "executor");
        this.defaultAggregator = Objects.requireNonNull(defaultAggregator, "defaultAggregator");
    }

    // ============ Aggregation Helpers ============

    /**
     * Aggregates scores from successful model results using the default aggregator.
     *
     * @param modelScores map of model ID to score
     * @return aggregated score
     * @throws IllegalStateException if no successful scores to aggregate
     */
    protected double aggregate(final Map<String, Double> modelScores) {
        return aggregate(modelScores, defaultAggregator);
    }

    /**
     * Aggregates scores from successful model results using a custom aggregator.
     *
     * @param modelScores map of model ID to score
     * @param aggregator  the aggregation strategy to use
     * @return aggregated score
     * @throws IllegalStateException if no successful scores to aggregate
     */
    protected double aggregate(final Map<String, Double> modelScores, final ScoreAggregator aggregator) {
        if (modelScores.isEmpty()) {
            throw new IllegalStateException("No successful model scores to aggregate for metric: " + getName());
        }
        return aggregator.aggregate(List.copyOf(modelScores.values()));
    }
}
