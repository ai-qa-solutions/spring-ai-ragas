package ai.qa.solutions.metric.metadata;

import ai.qa.solutions.execution.listener.dto.MetricMetadata;
import java.util.Map;

/**
 * Typed metadata for RubricsScore metric evaluation results.
 * <p>
 * Captures intermediate results for debugging and Allure report generation.
 *
 * @param rubrics           the rubric definitions (e.g., "score1_description" -> "Poor quality...")
 * @param modelScores       per-model raw scores from evaluation
 * @param modelRubricLevels per-model selected rubric level keys
 * @param modelReasonings   per-model reasoning strings
 */
public record RubricsMetadata(
        Map<String, String> rubrics,
        Map<String, Integer> modelScores,
        Map<String, String> modelRubricLevels,
        Map<String, String> modelReasonings)
        implements MetricMetadata {}
