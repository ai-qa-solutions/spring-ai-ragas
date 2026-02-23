package ai.qa.solutions.metric.metadata;

import ai.qa.solutions.execution.listener.dto.MetricMetadata;
import java.util.List;
import java.util.Map;

/**
 * Typed metadata for Context Precision metric evaluation results.
 * <p>
 * Captures intermediate results for debugging and Allure report generation.
 *
 * @param evaluationStrategy the strategy used (REFERENCE_BASED or RESPONSE_BASED)
 * @param modelRelevanceResults per-model relevance verdicts for each context chunk (ordered)
 * @param contextCount          number of retrieved context chunks evaluated
 */
public record ContextPrecisionMetadata(
        String evaluationStrategy, Map<String, List<Boolean>> modelRelevanceResults, int contextCount)
        implements MetricMetadata {}
