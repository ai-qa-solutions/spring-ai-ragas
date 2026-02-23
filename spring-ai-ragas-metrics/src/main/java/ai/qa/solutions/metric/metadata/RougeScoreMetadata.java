package ai.qa.solutions.metric.metadata;

import ai.qa.solutions.execution.listener.dto.MetricMetadata;

/**
 * Typed metadata for ROUGE Score metric evaluation results.
 * <p>
 * Captures configuration for debugging and Allure report generation.
 * Response and reference texts are available from the {@code Sample} in
 * {@link ai.qa.solutions.execution.listener.dto.MetricEvaluationResult#getSample()}.
 *
 * @param rougeType the ROUGE variant used (ROUGE_1, ROUGE_2, ROUGE_L)
 * @param mode      the scoring mode (RECALL, PRECISION, FMEASURE)
 */
public record RougeScoreMetadata(String rougeType, String mode) implements MetricMetadata {}
