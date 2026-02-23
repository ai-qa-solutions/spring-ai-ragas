package ai.qa.solutions.metric.metadata;

import ai.qa.solutions.execution.listener.dto.MetricMetadata;
import java.util.List;
import java.util.Map;

/**
 * Typed metadata for Response Relevancy metric evaluation results.
 * <p>
 * Captures intermediate results for debugging and Allure report generation.
 *
 * @param generatedQuestions per-model generated questions from the response
 * @param noncommittalFlags per-model noncommittal flags for each generated question
 * @param similarityScores  per-model average cosine similarity scores
 * @param numberOfQuestions  configured number of questions to generate
 */
public record ResponseRelevancyMetadata(
        Map<String, List<String>> generatedQuestions,
        Map<String, List<Boolean>> noncommittalFlags,
        Map<String, Double> similarityScores,
        int numberOfQuestions)
        implements MetricMetadata {}
