package ai.qa.solutions.metric.metadata;

import ai.qa.solutions.execution.listener.dto.MetricMetadata;

/**
 * Typed metadata for Answer Correctness metric evaluation results.
 * <p>
 * Captures the factual and semantic components of the combined answer correctness score,
 * along with their configured weights.
 *
 * @param factualScore              the factual correctness component score (0.0-1.0)
 * @param semanticScore             the semantic similarity component score (0.0-1.0)
 * @param normalizedFactualWeight   the normalized weight applied to factual component
 * @param normalizedSemanticWeight  the normalized weight applied to semantic component
 */
public record AnswerCorrectnessMetadata(
        double factualScore, double semanticScore, double normalizedFactualWeight, double normalizedSemanticWeight)
        implements MetricMetadata {}
