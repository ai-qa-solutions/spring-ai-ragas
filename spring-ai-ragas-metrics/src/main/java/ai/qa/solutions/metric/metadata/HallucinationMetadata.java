package ai.qa.solutions.metric.metadata;

import ai.qa.solutions.execution.listener.dto.MetricMetadata;
import java.util.List;
import java.util.Map;

/**
 * Typed metadata for Hallucination metric evaluation results.
 * <p>
 * Captures per-model hallucination analysis results for debugging and Allure report generation.
 *
 * @param claimAnalyses per-model claim analysis results
 */
public record HallucinationMetadata(Map<String, List<ClaimAnalysisSummary>> claimAnalyses) implements MetricMetadata {

    /**
     * Summary of a single claim analysis.
     *
     * @param claim  the specific claim extracted from the response
     * @param status the status (SUPPORTED, CONTRADICTED, or HALLUCINATED)
     * @param reason the explanation for the classification
     */
    public record ClaimAnalysisSummary(String claim, String status, String reason) {}
}
