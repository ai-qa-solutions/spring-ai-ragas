package ai.qa.solutions.metric.metadata;

import ai.qa.solutions.execution.listener.dto.MetricMetadata;

/**
 * Typed metadata for BLEU Score metric evaluation results.
 * <p>
 * Captures configuration for debugging and Allure report generation.
 * Response and reference texts are available from the {@code Sample} in
 * {@link ai.qa.solutions.execution.listener.dto.MetricEvaluationResult#getSample()}.
 *
 * @param maxNgram  maximum n-gram size used for BLEU computation
 * @param smoothing whether smoothing was applied
 */
public record BleuScoreMetadata(int maxNgram, boolean smoothing) implements MetricMetadata {}
