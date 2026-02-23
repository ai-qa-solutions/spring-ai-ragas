package ai.qa.solutions.execution.listener.dto;

import ai.qa.solutions.execution.listener.MetricExecutionListener;
import ai.qa.solutions.sample.Sample;
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
 *   <li>The sample and configuration used for evaluation</li>
 *   <li>All step results accumulated during metric execution</li>
 *   <li>All model exclusion events</li>
 *   <li>Model IDs (LLM and embedding) used for evaluation</li>
 *   <li>Typed metadata from the metric</li>
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
 *     log.info("Steps: {}", result.getSteps().size());
 *     log.info("Exclusions: {}", result.getExclusions().size());
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
     * The sample being evaluated.
     * <p>
     * Contains the user input, response, retrieved contexts, and reference
     * that were used for this evaluation.
     */
    Sample sample;

    /**
     * The metric configuration used for this evaluation.
     * <p>
     * The actual type depends on the metric (e.g., FaithfulnessConfig, AspectCriticConfig).
     * Consumers can cast to the expected config type.
     */
    Object config;

    /**
     * LLM model IDs used for this evaluation.
     * <p>
     * The list of chat model identifiers that were configured for multi-model execution.
     */
    @Builder.Default
    List<String> modelIds = List.of();

    /**
     * Embedding model IDs used for this evaluation (if any).
     * <p>
     * Non-empty only for metrics that use embedding models (e.g., SemanticSimilarity).
     */
    @Builder.Default
    List<String> embeddingModelIds = List.of();

    /**
     * All step results accumulated during metric execution.
     * <p>
     * Contains results from every step in the evaluation pipeline,
     * including per-model results, durations, and request data.
     * Steps are ordered by execution sequence.
     */
    @Builder.Default
    List<StepResults> steps = List.of();

    /**
     * All model exclusion events that occurred during evaluation.
     * <p>
     * Contains details about which models were excluded, at which step,
     * and the cause of failure. Empty if all models completed successfully.
     */
    @Builder.Default
    List<ModelExclusionEvent> exclusions = List.of();

    /**
     * Typed metadata provided by the metric.
     * <p>
     * Contains metric-specific data records (e.g., FaithfulnessMetadata, AnswerCorrectnessMetadata).
     * Null if no metadata is provided.
     */
    MetricMetadata metadata;

    /**
     * Score explanation built during evaluation.
     * <p>
     * The actual type is {@code ScoreExplanation} from the metrics module.
     * Since the multi-model module does not depend on the metrics module,
     * the field type is {@code Object}. Consumers should cast:
     * {@code (ScoreExplanation) result.getExplanation()}.
     * <p>
     * Null if explanation could not be built or was not requested.
     */
    Object explanation;
}
