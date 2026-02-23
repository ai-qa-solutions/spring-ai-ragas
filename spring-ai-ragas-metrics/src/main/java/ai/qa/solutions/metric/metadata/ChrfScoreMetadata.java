package ai.qa.solutions.metric.metadata;

import ai.qa.solutions.execution.listener.dto.MetricMetadata;

/**
 * Typed metadata for chrF Score metric evaluation results.
 * <p>
 * Captures configuration for debugging and Allure report generation.
 * Response and reference texts are available from the {@code Sample} in
 * {@link ai.qa.solutions.execution.listener.dto.MetricEvaluationResult#getSample()}.
 *
 * @param charNgramOrder maximum character n-gram order
 * @param wordNgramOrder maximum word n-gram order (0 = chrF, &gt;0 = chrF++)
 * @param beta           beta parameter for F-score weighting
 */
public record ChrfScoreMetadata(int charNgramOrder, int wordNgramOrder, double beta) implements MetricMetadata {}
