package ai.qa.solutions.metric.metadata;

import ai.qa.solutions.execution.listener.dto.MetricMetadata;
import java.util.List;
import java.util.Map;

/**
 * Typed metadata for TopicAdherence metric evaluation results.
 * <p>
 * Captures intermediate results for debugging and Allure report generation.
 *
 * @param mode                   scoring mode (F1, PRECISION, or RECALL)
 * @param referenceTopics        the expected/allowed topics
 * @param extractedTopics        topics extracted from the conversation
 * @param modelClassifications   per-model topic classification results
 */
public record TopicAdherenceMetadata(
        String mode,
        List<String> referenceTopics,
        List<String> extractedTopics,
        Map<String, List<TopicClassificationSummary>> modelClassifications)
        implements MetricMetadata {

    /**
     * Summary of a single topic classification.
     *
     * @param extractedTopic        the extracted topic being classified
     * @param onTopic               whether the topic matches a reference topic
     * @param matchedReferenceTopic the matching reference topic (null if off-topic)
     * @param reasoning             explanation for the classification
     */
    public record TopicClassificationSummary(
            String extractedTopic, boolean onTopic, String matchedReferenceTopic, String reasoning) {}
}
