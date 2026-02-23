package ai.qa.solutions.metric.metadata;

import ai.qa.solutions.execution.listener.dto.MetricMetadata;
import java.util.List;
import java.util.Map;

/**
 * Typed metadata for Faithfulness metric evaluation results.
 * <p>
 * Captures intermediate results for debugging and Allure report generation.
 *
 * @param extractedStatements per-model extracted statements from the response
 * @param verdicts            per-model faithfulness verdicts for each statement
 * @param faithfulCount       number of statements judged faithful (from aggregated/first model)
 * @param totalCount          total number of statements evaluated
 */
public record FaithfulnessMetadata(
        Map<String, List<String>> extractedStatements,
        Map<String, List<StatementVerdictSummary>> verdicts,
        long faithfulCount,
        long totalCount)
        implements MetricMetadata {

    /**
     * Summary of a single statement verdict.
     *
     * @param statement the original statement text
     * @param reason    the reasoning for the verdict
     * @param verdict   1 if faithful, 0 otherwise
     */
    public record StatementVerdictSummary(String statement, String reason, int verdict) {}
}
