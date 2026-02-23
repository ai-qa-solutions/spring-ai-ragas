package ai.qa.solutions.metric.metadata;

import ai.qa.solutions.execution.listener.dto.MetricMetadata;
import java.util.List;
import java.util.Map;

/**
 * Typed metadata for AspectCritic metric evaluation results.
 * <p>
 * Captures intermediate results for debugging and Allure report generation.
 *
 * @param definition       the evaluation criteria definition
 * @param strictness       number of iterations per model for majority voting
 * @param modelVerdicts    per-model list of verdicts from each iteration (true = pass, false = fail)
 * @param modelReasonings  per-model list of reasoning strings from each iteration
 */
public record AspectCriticMetadata(
        String definition,
        int strictness,
        Map<String, List<Boolean>> modelVerdicts,
        Map<String, List<String>> modelReasonings)
        implements MetricMetadata {}
