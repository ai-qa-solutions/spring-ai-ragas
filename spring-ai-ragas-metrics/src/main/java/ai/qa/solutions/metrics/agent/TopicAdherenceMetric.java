package ai.qa.solutions.metrics.agent;

import ai.qa.solutions.execution.ModelResult;
import ai.qa.solutions.execution.MultiModelExecutor;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationContext;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationResult;
import ai.qa.solutions.execution.listener.dto.ModelExclusionEvent;
import ai.qa.solutions.metric.AbstractMultiTurnMetric;
import ai.qa.solutions.sample.Sample;
import ai.qa.solutions.sample.message.BaseMessage;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.PromptTemplate;

/**
 * Topic Adherence Metric - Evaluates whether conversation topics adhere to expected topics.
 * <p>
 * This metric extracts topics discussed in a conversation and classifies them against
 * a list of reference topics to determine if the conversation stayed on topic.
 * <p>
 * Supports three modes:
 * <ul>
 *   <li>{@code F1} - Harmonic mean of precision and recall (default)</li>
 *   <li>{@code PRECISION} - Focus on avoiding off-topic discussions</li>
 *   <li>{@code RECALL} - Focus on covering all reference topics</li>
 * </ul>
 * <p>
 * Required sample fields:
 * <ul>
 *   <li>{@code messages} - The conversation history</li>
 *   <li>{@code referenceTopics} - List of expected/allowed topics</li>
 * </ul>
 */
@Slf4j
public class TopicAdherenceMetric extends AbstractMultiTurnMetric<TopicAdherenceMetric.TopicAdherenceConfig> {

    public static final String DEFAULT_EXTRACT_TOPICS_PROMPT =
            """
                    Analyze the following conversation and extract all distinct topics discussed.

                    Conversation:
                    {conversation}

                    Instructions:
                    1. Identify all topics mentioned or discussed in the conversation
                    2. Each topic should be a concise phrase (2-5 words)
                    3. Include both explicitly mentioned topics and implied topics
                    4. Do not include topics that were only briefly mentioned without discussion

                    Respond with a JSON object containing:
                    - topics: A list of distinct topic strings
                    """;

    public static final String DEFAULT_CLASSIFY_TOPICS_PROMPT =
            """
                    Classify each extracted topic against the reference topics to determine if the discussion stayed on topic.

                    Extracted Topics from Conversation:
                    {extractedTopics}

                    Reference Topics (allowed topics):
                    {referenceTopics}

                    Instructions:
                    1. For each extracted topic, determine if it matches or is closely related to any reference topic
                    2. A topic is "on topic" if it directly matches or is a reasonable subtopic of a reference topic
                    3. A topic is "off topic" if it doesn't relate to any reference topic

                    Respond with a JSON object containing:
                    - classifications: A list of objects, each with:
                      - extractedTopic: The topic being classified
                      - onTopic: true if on topic, false if off topic
                      - matchedReferenceTopic: The matching reference topic (if on topic), or null
                      - reasoning: Brief explanation of the classification
                    """;

    private final String extractTopicsPrompt;
    private final String classifyTopicsPrompt;

    @Builder(toBuilder = true)
    protected TopicAdherenceMetric(
            final MultiModelExecutor executor, final String extractTopicsPrompt, final String classifyTopicsPrompt) {
        super(executor);
        this.extractTopicsPrompt = extractTopicsPrompt != null ? extractTopicsPrompt : DEFAULT_EXTRACT_TOPICS_PROMPT;
        this.classifyTopicsPrompt =
                classifyTopicsPrompt != null ? classifyTopicsPrompt : DEFAULT_CLASSIFY_TOPICS_PROMPT;
    }

    @Override
    public Double multiTurnScore(final TopicAdherenceConfig config, final Sample sample) {
        return multiTurnScoreAsync(config, sample).join();
    }

    @Override
    public CompletableFuture<Double> multiTurnScoreAsync(final TopicAdherenceConfig config, final Sample sample) {
        final Instant startTime = Instant.now();
        final List<String> modelIds =
                config.models != null && !config.models.isEmpty() ? config.models : executor.getModelIds();

        // Validate input
        final List<BaseMessage> conversationMessages = sample.getUserInputMessages();
        if (conversationMessages == null || conversationMessages.isEmpty()) {
            log.warn("No messages provided for Topic Adherence evaluation");
            return CompletableFuture.completedFuture(null);
        }

        if (sample.getReferenceTopics() == null || sample.getReferenceTopics().isEmpty()) {
            log.warn("No reference topics provided for Topic Adherence evaluation");
            return CompletableFuture.completedFuture(null);
        }

        final EvaluationNotifier notifier = createEvaluationNotifier();
        final Mode mode = config.mode != null ? config.mode : Mode.F1;

        notifier.beforeMetricEvaluation(MetricEvaluationContext.builder()
                .metricName(getName())
                .sample(sample)
                .config(config)
                .modelIds(modelIds)
                .totalSteps(2)
                .metadata(Map.of("sample", sample, "config", config, "mode", mode))
                .build());

        return executor.runAsync(() -> {
            final String conversation = formatConversation(conversationMessages);
            final List<String> referenceTopics = sample.getReferenceTopics();
            final List<String> excludedModels = new ArrayList<>();

            // ========== Step 1: Extract Topics ==========
            notifier.beforeStep("ExtractTopics", 0, 2);

            final String extractPrompt = renderExtractTopicsPrompt(conversation);
            final List<ModelResult<ExtractedTopicsResponse>> extractResults =
                    executor.executeLlm(modelIds, extractPrompt, ExtractedTopicsResponse.class);

            List<String> extractedTopics = null;

            for (final ModelResult<ExtractedTopicsResponse> result : extractResults) {
                if (result.isSuccess() && result.result().topics() != null) {
                    extractedTopics = result.result().topics();
                    break; // Use first successful result
                } else if (!result.isSuccess()) {
                    excludedModels.add(result.modelId());
                    notifier.onModelExcluded(ModelExclusionEvent.builder()
                            .modelId(result.modelId())
                            .failedStepName("ExtractTopics")
                            .failedStepIndex(0)
                            .cause(result.error())
                            .build());
                }
            }

            notifier.afterLlmStep("ExtractTopics", 0, 2, extractPrompt, extractResults);

            if (extractedTopics == null || extractedTopics.isEmpty()) {
                log.warn("No topics extracted from conversation");
                // Return 0.0 for precision (no topics = nothing on topic), but consider recall
                final Duration duration = Duration.between(startTime, Instant.now());
                notifier.afterMetricEvaluation(MetricEvaluationResult.builder()
                        .metricName(getName())
                        .aggregatedScore(0.0)
                        .modelScores(Map.of())
                        .excludedModels(excludedModels)
                        .totalDuration(duration)
                        .metadata(Map.of("sample", sample, "config", config, "mode", mode, "noTopicsExtracted", true))
                        .build());
                return 0.0;
            }

            // ========== Step 2: Classify Topics ==========
            notifier.beforeStep("ClassifyTopics", 1, 2);

            final String classifyPrompt = renderClassifyTopicsPrompt(extractedTopics, referenceTopics);
            final Map<String, Double> modelScores = new HashMap<>();
            final List<ModelResult<TopicClassificationResponse>> classifyResults =
                    executor.executeLlm(modelIds, classifyPrompt, TopicClassificationResponse.class);

            List<TopicClassification> classifications = null;

            for (final ModelResult<TopicClassificationResponse> result : classifyResults) {
                if (result.isSuccess() && result.result().classifications() != null) {
                    classifications = result.result().classifications();
                    final double score = computeScore(classifications, referenceTopics, mode);
                    modelScores.put(result.modelId(), score);
                } else if (!result.isSuccess()) {
                    if (!excludedModels.contains(result.modelId())) {
                        excludedModels.add(result.modelId());
                    }
                    notifier.onModelExcluded(ModelExclusionEvent.builder()
                            .modelId(result.modelId())
                            .failedStepName("ClassifyTopics")
                            .failedStepIndex(1)
                            .cause(result.error())
                            .build());
                }
            }

            notifier.afterLlmStep("ClassifyTopics", 1, 2, classifyPrompt, classifyResults);

            if (modelScores.isEmpty()) {
                throw new IllegalStateException("All models failed at step ClassifyTopics for metric: " + getName());
            }

            final double aggregatedScore = aggregate(modelScores);
            final Duration duration = Duration.between(startTime, Instant.now());

            notifier.afterMetricEvaluation(MetricEvaluationResult.builder()
                    .metricName(getName())
                    .aggregatedScore(aggregatedScore)
                    .modelScores(modelScores)
                    .excludedModels(excludedModels)
                    .totalDuration(duration)
                    .metadata(Map.of(
                            "sample",
                            sample,
                            "config",
                            config,
                            "mode",
                            mode,
                            "extractedTopics",
                            extractedTopics,
                            "classifications",
                            classifications != null ? classifications : List.of()))
                    .build());

            return aggregatedScore;
        });
    }

    private double computeScore(
            final List<TopicClassification> classifications, final List<String> referenceTopics, final Mode mode) {
        if (classifications == null || classifications.isEmpty()) {
            return 0.0;
        }

        final long onTopicCount = classifications.stream()
                .filter(c -> c.onTopic() != null && c.onTopic())
                .count();

        final int totalExtracted = classifications.size();
        final int totalReference = referenceTopics.size();

        // Precision: what fraction of extracted topics are on topic
        final double precision = totalExtracted == 0 ? 0.0 : (double) onTopicCount / totalExtracted;

        // Recall: what fraction of reference topics are covered
        // Count unique reference topics that were matched
        final long coveredReferenceCount = classifications.stream()
                .filter(c -> c.onTopic() != null && c.onTopic())
                .map(TopicClassification::matchedReferenceTopic)
                .filter(t -> t != null && !t.isEmpty())
                .distinct()
                .count();

        final double recall = totalReference == 0 ? 0.0 : (double) coveredReferenceCount / totalReference;

        return switch (mode) {
            case PRECISION -> precision;
            case RECALL -> recall;
            case F1 -> (precision + recall) == 0 ? 0.0 : 2 * precision * recall / (precision + recall);
        };
    }

    private String renderExtractTopicsPrompt(final String conversation) {
        return PromptTemplate.builder()
                .template(this.extractTopicsPrompt)
                .variables(Map.of("conversation", conversation))
                .build()
                .render();
    }

    private String renderClassifyTopicsPrompt(final List<String> extractedTopics, final List<String> referenceTopics) {
        return PromptTemplate.builder()
                .template(this.classifyTopicsPrompt)
                .variables(Map.of(
                        "extractedTopics", String.join("\n- ", extractedTopics),
                        "referenceTopics", String.join("\n- ", referenceTopics)))
                .build()
                .render();
    }

    /**
     * Response DTO for topic extraction.
     */
    public record ExtractedTopicsResponse(
            @JsonPropertyDescription("List of distinct topics discussed in the conversation") List<String> topics) {}

    /**
     * Response DTO for topic classification.
     */
    public record TopicClassificationResponse(
            @JsonPropertyDescription("List of topic classifications") List<TopicClassification> classifications) {}

    /**
     * Individual topic classification result.
     */
    public record TopicClassification(
            @JsonPropertyDescription("The extracted topic being classified") String extractedTopic,
            @JsonPropertyDescription("True if the topic matches a reference topic") Boolean onTopic,
            @JsonPropertyDescription("The matching reference topic, if on topic") String matchedReferenceTopic,
            @JsonPropertyDescription("Explanation of the classification") String reasoning) {}

    /**
     * Scoring mode for the metric.
     */
    public enum Mode {
        /** Harmonic mean of precision and recall. */
        F1,
        /** Focus on avoiding off-topic discussions. */
        PRECISION,
        /** Focus on covering all reference topics. */
        RECALL
    }

    @Data
    @Builder
    public static class TopicAdherenceConfig implements MetricConfiguration {
        @Singular
        private List<String> models;

        /** Scoring mode: F1, PRECISION, or RECALL. */
        @Builder.Default
        private Mode mode = Mode.F1;
    }
}
