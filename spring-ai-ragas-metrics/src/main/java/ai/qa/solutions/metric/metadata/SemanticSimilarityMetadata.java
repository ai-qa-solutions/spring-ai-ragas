package ai.qa.solutions.metric.metadata;

import ai.qa.solutions.execution.listener.dto.MetricMetadata;
import java.util.Map;

/**
 * Typed metadata for Semantic Similarity metric evaluation results.
 * <p>
 * Captures per-embedding-model similarity scores and the threshold configuration.
 *
 * @param embeddingModelScores per-embedding-model cosine similarity scores
 * @param threshold            the configured threshold (null if not set)
 */
public record SemanticSimilarityMetadata(Map<String, Double> embeddingModelScores, Double threshold)
        implements MetricMetadata {}
