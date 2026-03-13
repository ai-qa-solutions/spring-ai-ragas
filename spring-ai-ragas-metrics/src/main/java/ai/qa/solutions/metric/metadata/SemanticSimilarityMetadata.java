package ai.qa.solutions.metric.metadata;

import ai.qa.solutions.execution.listener.dto.MetricMetadata;
import java.util.Map;

/**
 * Typed metadata for Semantic Similarity metric evaluation results.
 * <p>
 * Captures per-embedding-model similarity scores, the threshold configuration,
 * and chunking information for long text handling.
 *
 * @param embeddingModelScores per-embedding-model cosine similarity scores
 * @param threshold            the configured threshold (null if not set)
 * @param chunkingApplied      whether text chunking was applied
 * @param responseChunkCount   number of chunks the response was split into
 * @param referenceChunkCount  number of chunks the reference was split into
 * @param longTextStrategy     the long text strategy name used (e.g. CHUNK, TRUNCATE, FAIL_FAST)
 */
public record SemanticSimilarityMetadata(
        Map<String, Double> embeddingModelScores,
        Double threshold,
        boolean chunkingApplied,
        int responseChunkCount,
        int referenceChunkCount,
        String longTextStrategy)
        implements MetricMetadata {}
