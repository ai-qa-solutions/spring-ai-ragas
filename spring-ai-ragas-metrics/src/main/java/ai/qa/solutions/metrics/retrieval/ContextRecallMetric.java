package ai.qa.solutions.metrics.retrieval;

import ai.qa.solutions.execution.ModelResult;
import ai.qa.solutions.execution.MultiModelExecutor;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationContext;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationResult;
import ai.qa.solutions.execution.listener.dto.ModelExclusionEvent;
import ai.qa.solutions.execution.listener.dto.StepResults;
import ai.qa.solutions.execution.listener.dto.StepType;
import ai.qa.solutions.metric.AbstractMultiModelMetric;
import ai.qa.solutions.metric.metadata.ContextRecallMetadata;
import ai.qa.solutions.sample.Sample;
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
 * Context Recall Metric - LLM-based evaluation of retriever's ability to retrieve all relevant information.
 * <p>
 * Measures how many statements in the reference answer can be attributed to the retrieved contexts.
 * Uses {@link MultiModelExecutor} for parallel execution across multiple models
 * with explicit flow control and listener notifications.
 */
@Slf4j
public class ContextRecallMetric extends AbstractMultiModelMetric<ContextRecallMetric.ContextRecallConfig> {
    public static final String DEFAULT_CONTEXT_RECALL_PROMPT =
            """
                    Given a question, context, and a reference answer, analyze each sentence in the reference answer and classify if the sentence can be attributed to the given context or not. Use only 'Yes' (1) or 'No' (0) as a binary classification.

                    Question: {question}
                    Context: {context}
                    Reference Answer: {reference_answer}

                    Instructions:
                    1. Break down the reference answer into individual sentences
                    2. For each sentence, determine if it can be attributed to the provided context
                    3. A sentence is attributable (1) if the information can be found or inferred from the context
                    4. A sentence is not attributable (0) if the information is not present in the context
                    5. Be strict in your evaluation - only mark as attributable if there is clear supporting evidence
                    6. Consider paraphrases and semantically equivalent information as supporting evidence

                    Respond with a JSON object containing:
                    - classifications: A list of classification objects, each containing:
                      - statement: The individual sentence from the reference answer
                      - reason: Detailed explanation for the classification
                      - attributed: 1 if the statement can be attributed to the context, 0 otherwise
                    """;

    private final String contextRecallPrompt;

    @Builder(toBuilder = true)
    protected ContextRecallMetric(final MultiModelExecutor executor, final String contextRecallPrompt) {
        super(executor);
        this.contextRecallPrompt = contextRecallPrompt != null ? contextRecallPrompt : DEFAULT_CONTEXT_RECALL_PROMPT;
    }

    @Override
    public Double singleTurnScore(final ContextRecallConfig config, final Sample sample) {
        return singleTurnScoreAsync(config, sample).join();
    }

    @Override
    public CompletableFuture<Double> singleTurnScoreAsync(final ContextRecallConfig config, final Sample sample) {
        final Instant startTime = Instant.now();
        final List<String> modelIds =
                config.models != null && !config.models.isEmpty() ? config.models : executor.getModelIds();

        // Validate required inputs
        final String reference = sample.getReference();
        if (reference == null || reference.trim().isEmpty()) {
            log.warn("No reference provided for Context Recall evaluation - this metric requires a reference answer");
            return CompletableFuture.completedFuture(0.0);
        }

        final List<String> retrievedContexts = sample.getRetrievedContexts();
        if (retrievedContexts == null || retrievedContexts.isEmpty()) {
            log.warn("No retrieved contexts provided for Context Recall evaluation");
            return CompletableFuture.completedFuture(0.0);
        }

        final String userInput = sample.getUserInput();
        if (userInput == null || userInput.trim().isEmpty()) {
            log.warn("No user input provided for Context Recall evaluation");
            return CompletableFuture.completedFuture(0.0);
        }

        // Create evaluation-specific notifier for thread-safe parallel execution
        final EvaluationNotifier notifier = createEvaluationNotifier();

        // Notify listeners before evaluation
        notifier.beforeMetricEvaluation(MetricEvaluationContext.builder()
                .metricName(getName())
                .sample(sample)
                .config(config)
                .modelIds(modelIds)
                .totalSteps(1)
                .build());

        return executor.runAsync(() -> {
            log.debug("Computing LLM-based context recall evaluation");

            // Local accumulators for steps and exclusions
            final List<StepResults> accumulatedSteps = new ArrayList<>();
            final List<ModelExclusionEvent> accumulatedExclusions = new ArrayList<>();

            // ========== Step 1: Classify statements ==========
            final String prompt = renderPrompt(userInput, retrievedContexts, reference);
            final List<ModelResult<ContextRecallClassifications>> results =
                    executor.executeLlm(modelIds, prompt, ContextRecallClassifications.class);

            accumulatedSteps.add(StepResults.builder()
                    .stepName("ClassifyStatements")
                    .stepIndex(0)
                    .totalSteps(1)
                    .stepType(StepType.LLM)
                    .request(prompt)
                    .results(new ArrayList<>(results))
                    .build());

            // Collect scores and track excluded models
            final Map<String, Double> modelScores = new HashMap<>();
            for (final ModelResult<ContextRecallClassifications> result : results) {
                if (result.isSuccess()) {
                    modelScores.put(result.modelId(), calculateContextRecall(result.result()));
                } else {
                    accumulatedExclusions.add(ModelExclusionEvent.builder()
                            .modelId(result.modelId())
                            .failedStepName("ClassifyStatements")
                            .failedStepIndex(0)
                            .cause(result.error())
                            .build());
                }
            }

            if (modelScores.isEmpty()) {
                throw new IllegalStateException("All models failed for metric: " + getName());
            }

            final double aggregatedScore = aggregate(modelScores);

            // Build metadata
            final Map<String, List<ContextRecallMetadata.ClassificationSummary>> classificationsSummary =
                    new HashMap<>();
            long attributedCount = 0;
            long totalCount = 0;
            for (final ModelResult<ContextRecallClassifications> result : results) {
                if (result.isSuccess() && result.result().classifications() != null) {
                    final List<ContextRecallMetadata.ClassificationSummary> summaries = new ArrayList<>();
                    for (final ContextRecallClassification c : result.result().classifications()) {
                        summaries.add(new ContextRecallMetadata.ClassificationSummary(
                                c.statement(), c.reason(), c.attributed() != null ? c.attributed() : 0));
                    }
                    classificationsSummary.put(result.modelId(), summaries);
                }
            }
            // Use first successful model's data for aggregate counts
            final ModelResult<ContextRecallClassifications> firstSuccessful =
                    results.stream().filter(ModelResult::isSuccess).findFirst().orElse(null);
            if (firstSuccessful != null && firstSuccessful.result().classifications() != null) {
                totalCount = firstSuccessful.result().classifications().size();
                attributedCount = firstSuccessful.result().classifications().stream()
                        .mapToInt(ContextRecallClassification::attributed)
                        .sum();
            }

            // Notify with full results
            final Duration duration = Duration.between(startTime, Instant.now());
            final List<String> excludedModels = results.stream()
                    .filter(ModelResult::isFailure)
                    .map(ModelResult::modelId)
                    .toList();

            notifier.afterMetricEvaluation(MetricEvaluationResult.builder()
                    .metricName(getName())
                    .sample(sample)
                    .config(config)
                    .modelIds(modelIds)
                    .aggregatedScore(aggregatedScore)
                    .modelScores(modelScores)
                    .excludedModels(excludedModels)
                    .totalDuration(duration)
                    .steps(accumulatedSteps)
                    .exclusions(accumulatedExclusions)
                    .metadata(new ContextRecallMetadata(classificationsSummary, attributedCount, totalCount))
                    .build());

            return aggregatedScore;
        });
    }

    private String renderPrompt(final String question, final List<String> retrievedContexts, final String reference) {
        return PromptTemplate.builder()
                .template(this.contextRecallPrompt)
                .variables(Map.of(
                        "question", question,
                        "context", String.join("\n\n", retrievedContexts),
                        "reference_answer", reference))
                .build()
                .render();
    }

    private Double calculateContextRecall(final ContextRecallClassifications classifications) {
        if (classifications == null
                || classifications.classifications() == null
                || classifications.classifications().isEmpty()) {
            log.warn("No classifications returned from LLM");
            return 0.0;
        }

        final List<ContextRecallClassification> classificationList = classifications.classifications();
        log.debug("Classified {} statements from reference answer", classificationList.size());

        final long attributedStatements = classificationList.stream()
                .mapToInt(ContextRecallClassification::attributed)
                .sum();

        return (double) attributedStatements / classificationList.size();
    }

    /**
     * Response DTO for individual statement classification
     */
    public record ContextRecallClassification(
            @JsonPropertyDescription("The individual statement from the reference answer") String statement,
            @JsonPropertyDescription("Detailed explanation for the classification") String reason,
            @JsonPropertyDescription("1 if the statement can be attributed to the context, 0 otherwise")
                    Integer attributed) {}

    /**
     * Response DTO for all statement classifications
     */
    public record ContextRecallClassifications(
            @JsonPropertyDescription("List of classification objects for each statement in the reference answer")
                    List<ContextRecallClassification> classifications) {}

    @Data
    @Builder
    public static class ContextRecallConfig implements MetricConfiguration {
        @Singular
        private List<String> models;

        @Builder.Default
        private String language = "en";
    }
}
