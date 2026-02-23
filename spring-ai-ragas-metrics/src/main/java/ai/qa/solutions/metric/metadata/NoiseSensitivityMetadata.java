package ai.qa.solutions.metric.metadata;

import ai.qa.solutions.execution.listener.dto.MetricMetadata;
import java.util.List;
import java.util.Map;

/**
 * Typed metadata for Noise Sensitivity metric evaluation results.
 * <p>
 * Captures intermediate results for debugging and Allure report generation.
 *
 * @param mode                   evaluation mode (RELEVANT or IRRELEVANT)
 * @param referenceStatements    per-model decomposed statements from the reference answer
 * @param responseStatements     per-model decomposed statements from the AI response
 * @param numContexts            number of retrieved contexts evaluated
 */
public record NoiseSensitivityMetadata(
        String mode,
        Map<String, List<String>> referenceStatements,
        Map<String, List<String>> responseStatements,
        int numContexts)
        implements MetricMetadata {}
