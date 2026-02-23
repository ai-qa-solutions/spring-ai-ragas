package ai.qa.solutions.metric.metadata;

import ai.qa.solutions.execution.listener.dto.MetricMetadata;

/**
 * Typed metadata for String Similarity metric evaluation results.
 * <p>
 * Captures configuration for debugging and Allure report generation.
 * Response and reference texts are available from the {@code Sample} in
 * {@link ai.qa.solutions.execution.listener.dto.MetricEvaluationResult#getSample()}.
 *
 * @param distanceMeasure the distance algorithm used (LEVENSHTEIN, HAMMING, JARO, JARO_WINKLER)
 * @param caseSensitive   whether comparison was case sensitive
 */
public record StringSimilarityMetadata(String distanceMeasure, boolean caseSensitive) implements MetricMetadata {}
