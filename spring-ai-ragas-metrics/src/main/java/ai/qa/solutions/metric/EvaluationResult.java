package ai.qa.solutions.metric;

import ai.qa.solutions.execution.listener.dto.MetricMetadata;
import ai.qa.solutions.metric.explanation.ScoreExplanation;
import ai.qa.solutions.sample.Sample;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

/**
 * Rich evaluation result containing score, explanation, metadata, and execution details.
 * <p>
 * Returned by {@link Metric#singleTurnEvaluate} and {@link Metric#multiTurnEvaluate} methods.
 * Unlike the simple {@code Double} returned by score methods, this class provides complete
 * evaluation information including human-readable explanations.
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * EvaluationResult result = metric.singleTurnEvaluate(config, sample);
 *
 * log.info("Metric: {}", result.getMetricName());
 * log.info("Score: {}", result.getScore());
 * log.info("Explanation: {}", result.getExplanation().getSimpleDescription());
 * log.info("Model scores: {}", result.getModelScores());
 * log.info("Duration: {}ms", result.getTotalDuration().toMillis());
 * }</pre>
 *
 * @see Metric#singleTurnEvaluate(Metric.MetricConfiguration, Sample)
 * @see Metric#multiTurnEvaluate(Metric.MetricConfiguration, Sample)
 */
@Value
@Builder
public class EvaluationResult {

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
     * Null if all models failed.
     */
    Double score;

    /**
     * Individual scores for each successful model.
     * <p>
     * Map key: model ID (e.g., "anthropic-claude-3-5-sonnet").
     * Map value: the final score from that model.
     * Models that failed are not included here.
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
     * The sample that was evaluated.
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
     * Human-readable score explanation with step-by-step breakdown.
     * <p>
     * Contains interpretation, metric-specific details, and visualization data.
     * Null if explanation could not be built (e.g., all models failed).
     *
     * @see ScoreExplanation
     */
    ScoreExplanation explanation;

    /**
     * Typed metadata provided by the metric.
     * <p>
     * Contains metric-specific data records (e.g., FaithfulnessMetadata, AnswerCorrectnessMetadata).
     * Null if no metadata is provided.
     */
    MetricMetadata metadata;

    /**
     * LLM model IDs used for this evaluation.
     * <p>
     * The list of chat model identifiers that were configured for multi-model execution.
     * Empty for NLP metrics that do not use LLM calls.
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
}
