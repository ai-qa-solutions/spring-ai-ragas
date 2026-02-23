package ai.qa.solutions.metric.metadata;

import ai.qa.solutions.execution.listener.dto.MetricMetadata;
import java.util.Map;

/**
 * Typed metadata for AgentGoalAccuracy metric evaluation results.
 * <p>
 * Captures intermediate results for debugging and Allure report generation.
 *
 * @param mode              evaluation mode (WITH_REFERENCE or WITHOUT_REFERENCE)
 * @param inferredGoal      the inferred goal (only for WITHOUT_REFERENCE mode), null otherwise
 * @param modelVerdicts     per-model goal achievement verdicts (true = achieved, false = not)
 * @param modelReasonings   per-model reasoning strings for the verdict
 */
public record AgentGoalAccuracyMetadata(
        String mode, String inferredGoal, Map<String, Boolean> modelVerdicts, Map<String, String> modelReasonings)
        implements MetricMetadata {}
