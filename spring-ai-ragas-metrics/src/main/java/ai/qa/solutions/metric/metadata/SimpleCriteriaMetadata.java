package ai.qa.solutions.metric.metadata;

import ai.qa.solutions.execution.listener.dto.MetricMetadata;
import java.util.List;
import java.util.Map;

/**
 * Typed metadata for SimpleCriteriaScore metric evaluation results.
 * <p>
 * Captures intermediate results for debugging and Allure report generation.
 *
 * @param definition          the evaluation criteria definition
 * @param minScore            the minimum possible raw score
 * @param maxScore            the maximum possible raw score
 * @param strictness          number of iterations per model
 * @param modelRawScores      per-model list of raw scores from each iteration (before normalization)
 * @param modelReasonings     per-model list of reasoning strings from each iteration
 */
public record SimpleCriteriaMetadata(
        String definition,
        double minScore,
        double maxScore,
        int strictness,
        Map<String, List<Double>> modelRawScores,
        Map<String, List<String>> modelReasonings)
        implements MetricMetadata {}
