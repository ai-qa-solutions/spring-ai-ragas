package ai.qa.solutions.metric.metadata;

import ai.qa.solutions.execution.listener.dto.MetricMetadata;
import java.util.List;
import java.util.Map;

/**
 * Typed metadata for Context Recall metric evaluation results.
 * <p>
 * Captures intermediate results for debugging and Allure report generation.
 *
 * @param classifications per-model classification results for each reference statement
 * @param attributedCount number of statements attributed to context (from aggregated/first model)
 * @param totalCount      total number of statements in the reference answer
 */
public record ContextRecallMetadata(
        Map<String, List<ClassificationSummary>> classifications, long attributedCount, long totalCount)
        implements MetricMetadata {

    /**
     * Summary of a single statement classification.
     *
     * @param statement  the statement from the reference answer
     * @param reason     the reasoning for the classification
     * @param attributed 1 if attributed to context, 0 otherwise
     */
    public record ClassificationSummary(String statement, String reason, int attributed) {}
}
