package ai.qa.solutions.metric.metadata;

import ai.qa.solutions.execution.listener.dto.MetricMetadata;
import java.util.List;

/**
 * Typed metadata for Context Relevance (NVIDIA) metric evaluation results.
 * <p>
 * Captures per-context relevance scores for debugging and Allure report generation.
 *
 * @param contextScores  per-context averaged relevance scores (one per context chunk)
 * @param contextCount   total number of context chunks evaluated
 */
public record ContextRelevanceMetadata(List<Double> contextScores, int contextCount) implements MetricMetadata {}
