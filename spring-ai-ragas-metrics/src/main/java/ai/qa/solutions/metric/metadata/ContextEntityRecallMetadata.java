package ai.qa.solutions.metric.metadata;

import ai.qa.solutions.execution.listener.dto.MetricMetadata;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Typed metadata for Context Entity Recall metric evaluation results.
 * <p>
 * Captures intermediate results for debugging and Allure report generation.
 *
 * @param referenceEntities per-model entities extracted from the reference answer
 * @param contextEntities   per-model entities extracted from the retrieved contexts
 * @param commonEntities    per-model intersection of reference and context entities (normalized)
 * @param recallNumerator   size of the entity intersection (from aggregated/first model)
 * @param recallDenominator size of the reference entity set (from aggregated/first model)
 */
public record ContextEntityRecallMetadata(
        Map<String, List<String>> referenceEntities,
        Map<String, List<String>> contextEntities,
        Map<String, Set<String>> commonEntities,
        int recallNumerator,
        int recallDenominator)
        implements MetricMetadata {}
