package ai.qa.solutions.execution.listener.dto;

import ai.qa.solutions.execution.listener.MetricExecutionListener;
import ai.qa.solutions.sample.Sample;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

/**
 * Context provided before metric evaluation begins.
 * <p>
 * Contains all information about the metric being evaluated, the input sample,
 * the models that will be used, and the number of execution steps.
 * <p>
 * This immutable context is passed to {@link MetricExecutionListener#beforeMetricEvaluation(MetricEvaluationContext)}
 * to allow listeners to prepare for the upcoming evaluation.
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * public void beforeMetricEvaluation(MetricEvaluationContext context) {
 *     log.info("Evaluating {} on {} models with {} steps",
 *         context.getMetricName(),
 *         context.getModelIds().size(),
 *         context.getTotalSteps());
 * }
 * }</pre>
 */
@Value
@Builder
public class MetricEvaluationContext {

    /**
     * The name of the metric being evaluated.
     * <p>
     * Examples: "Faithfulness", "ContextRecall", "AspectCritic"
     */
    String metricName;

    /**
     * The sample being evaluated.
     * <p>
     * Contains user input, response, retrieved contexts, and reference answer.
     */
    Sample sample;

    /**
     * The metric configuration object.
     * <p>
     * Type depends on the specific metric (e.g., FaithfulnessConfig, AspectCriticConfig).
     * Can be null if no configuration is provided.
     */
    Object config;

    /**
     * List of LLM model IDs that will be used for evaluation.
     * <p>
     * If empty or null, all available models from the store will be used.
     */
    List<String> modelIds;

    /**
     * List of embedding model IDs that will be used for evaluation.
     * <p>
     * Only populated for metrics that use embeddings (e.g., ResponseRelevancy).
     * If empty or null, no embeddings will be used or all available embedding models will be used.
     */
    @Builder.Default
    List<String> embeddingModelIds = List.of();

    /**
     * Total number of execution steps in this metric.
     * <p>
     * For single-step metrics (e.g., AspectCritic), this is 1.
     * For multi-step metrics (e.g., Faithfulness), this is the number of LLM calls + compute steps.
     */
    int totalSteps;

    /**
     * Additional metadata provided by the metric.
     * <p>
     * May include custom data specific to the metric implementation.
     */
    @Builder.Default
    Map<String, Object> metadata = Map.of();
}
