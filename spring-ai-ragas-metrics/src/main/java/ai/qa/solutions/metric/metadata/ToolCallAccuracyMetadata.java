package ai.qa.solutions.metric.metadata;

import ai.qa.solutions.execution.listener.dto.MetricMetadata;
import java.util.List;

/**
 * Typed metadata for ToolCallAccuracy metric evaluation results.
 * <p>
 * Captures intermediate results for debugging and Allure report generation.
 *
 * @param mode                the matching mode (STRICT or FLEXIBLE)
 * @param argumentMatchThreshold threshold for flexible matching
 * @param actualCallCount     number of actual tool calls made by the agent
 * @param referenceCallCount  number of expected/reference tool calls
 * @param truePositives       number of correctly matched tool calls
 * @param falsePositives      number of actual calls not matching any reference
 * @param falseNegatives      number of reference calls not matched by any actual
 * @param precision           precision score (truePositives / actualCallCount)
 * @param recall              recall score (truePositives / referenceCallCount)
 * @param matches             details of each tool call matching attempt
 */
public record ToolCallAccuracyMetadata(
        String mode,
        double argumentMatchThreshold,
        int actualCallCount,
        int referenceCallCount,
        int truePositives,
        int falsePositives,
        int falseNegatives,
        double precision,
        double recall,
        List<ToolCallMatchSummary> matches)
        implements MetricMetadata {

    /**
     * Summary of a single tool call match.
     *
     * @param actualCallName     name of the actual tool call
     * @param referenceCallName  name of the matched reference tool call (null if unmatched)
     * @param matched            whether the call was successfully matched
     * @param matchScore         the match score (0.0-1.0)
     */
    public record ToolCallMatchSummary(
            String actualCallName, String referenceCallName, boolean matched, double matchScore) {}
}
