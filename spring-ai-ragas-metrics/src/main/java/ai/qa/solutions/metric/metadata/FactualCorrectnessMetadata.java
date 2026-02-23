package ai.qa.solutions.metric.metadata;

import ai.qa.solutions.execution.listener.dto.MetricMetadata;
import java.util.List;
import java.util.Map;

/**
 * Typed metadata for Factual Correctness metric evaluation results.
 * <p>
 * Captures the claims decomposition and NLI verification results for debugging
 * and Allure report generation.
 *
 * @param mode             the scoring mode used (F1, PRECISION, or RECALL)
 * @param responseClaims   per-model decomposed claims from the response
 * @param referenceClaims  per-model decomposed claims from the reference
 * @param precisionVerdicts per-model NLI verdicts for response claims against reference
 * @param recallVerdicts    per-model NLI verdicts for reference claims against response
 */
public record FactualCorrectnessMetadata(
        String mode,
        Map<String, List<String>> responseClaims,
        Map<String, List<String>> referenceClaims,
        Map<String, List<NliVerdictSummary>> precisionVerdicts,
        Map<String, List<NliVerdictSummary>> recallVerdicts)
        implements MetricMetadata {

    /**
     * Summary of a single NLI verdict.
     *
     * @param claim   the claim being verified
     * @param verdict the NLI verdict (SUPPORTED, CONTRADICTED, or NEUTRAL)
     * @param reason  the reasoning for the verdict
     */
    public record NliVerdictSummary(String claim, String verdict, String reason) {}
}
