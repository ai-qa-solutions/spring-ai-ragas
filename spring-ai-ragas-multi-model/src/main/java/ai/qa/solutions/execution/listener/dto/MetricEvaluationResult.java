package ai.qa.solutions.execution.listener.dto;

import ai.qa.solutions.execution.listener.MetricExecutionListener;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

/**
 * Complete result of metric evaluation across all models and steps.
 * <p>
 * This class aggregates all execution information including:
 * <ul>
 *   <li>The final aggregated score</li>
 *   <li>Individual scores for each successful model</li>
 *   <li>All step results for all models</li>
 *   <li>List of models that were excluded due to failures</li>
 *   <li>Total execution time</li>
 * </ul>
 * <p>
 * Passed to {@link MetricExecutionListener#afterMetricEvaluation(MetricEvaluationResult)}
 * after all execution completes.
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * public void afterMetricEvaluation(MetricEvaluationResult result) {
 *     log.info("=== {} Evaluation Complete ===", result.getMetricName());
 *     log.info("Final Score: {}", result.getAggregatedScore());
 *     log.info("Successful Models: {}/{}",
 *         result.getModelScores().size(),
 *         result.getModelScores().size() + result.getExcludedModels().size());
 *     log.info("Total Time: {}ms", result.getTotalDuration().toMillis());
 *
 *     if (!result.getExcludedModels().isEmpty()) {
 *         log.warn("Excluded Models: {}", result.getExcludedModels());
 *     }
 * }
 * }</pre>
 */
@Value
@Builder
public class MetricEvaluationResult {

    /**
     * The name of the metric that was evaluated.
     * <p>
     * Examples: "Faithfulness", "ContextRecall", "AspectCritic"
     */
    String metricName;

    /**
     * The final aggregated score from all successful models.
     * <p>
     * This is the result of applying the score aggregation strategy
     * (AVERAGE, MEDIAN, etc.) to all model scores.
     * <p>
     * Null if all models failed.
     */
    Double aggregatedScore;

    /**
     * Individual scores for each successful model.
     * <p>
     * Map key: model ID (e.g., "anthropic-claude-3-5-sonnet")
     * Map value: the final score from that model
     * <p>
     * Models that failed any step are not included here.
     */
    Map<String, Double> modelScores;

    /**
     * List of model IDs that were excluded due to failures.
     * <p>
     * These models failed at some step and were removed from further execution.
     * Empty if all models completed successfully.
     */
    List<String> excludedModels;

    /**
     * Total time taken for the entire metric evaluation.
     * <p>
     * Includes all steps for all models, measured from the start of the first
     * step to the completion of the last step.
     */
    Duration totalDuration;

    /**
     * Additional metadata provided by the metric.
     * <p>
     * May include custom data specific to the metric implementation.
     */
    @Builder.Default
    Map<String, Object> metadata = Map.of();
}
