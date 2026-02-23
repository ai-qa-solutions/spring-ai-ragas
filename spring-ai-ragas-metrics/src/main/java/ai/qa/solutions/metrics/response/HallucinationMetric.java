package ai.qa.solutions.metrics.response;

import ai.qa.solutions.execution.ModelResult;
import ai.qa.solutions.execution.MultiModelExecutor;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationContext;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationResult;
import ai.qa.solutions.execution.listener.dto.ModelExclusionEvent;
import ai.qa.solutions.execution.listener.dto.StepResults;
import ai.qa.solutions.execution.listener.dto.StepType;
import ai.qa.solutions.metric.AbstractMultiModelMetric;
import ai.qa.solutions.metric.metadata.HallucinationMetadata;
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
 * Hallucination Metric - Measures the rate of hallucinated content in LLM responses.
 *
 * <p>Hallucination occurs when the model generates content that is not grounded
 * in the provided context or contradicts factual information.
 *
 * <p>Uses {@link MultiModelExecutor} for parallel execution across multiple models
 * with explicit flow control and listener notifications.
 *
 * <p>Score ranges from 0.0 to 1.0, where:
 * <ul>
 *   <li>0.0 = No hallucination detected (fully grounded response)</li>
 *   <li>1.0 = Complete hallucination (response not grounded in context)</li>
 * </ul>
 */
@Slf4j
public class HallucinationMetric extends AbstractMultiModelMetric<HallucinationMetric.HallucinationConfig> {

    public static final String DEFAULT_HALLUCINATION_DETECTION_TEMPLATE =
            """
            Your task is to analyze a response and detect hallucinations - content that is not supported
            by the provided context or contradicts factual information.

            Context:
            {context}

            Question: {question}

            Response to analyze:
            {response}

            Analyze each claim in the response and determine if it is:
            1. SUPPORTED - The claim is directly supported by the context
            2. CONTRADICTED - The claim contradicts information in the context
            3. HALLUCINATED - The claim is not mentioned in the context and cannot be verified

            For each claim, provide:
            - claim: The specific claim from the response
            - status: One of SUPPORTED, CONTRADICTED, or HALLUCINATED
            - reason: Explanation of why this classification was made

            Respond with a JSON object containing a 'claims' array with the analysis.
            """;

    private final String hallucinationDetectionTemplate;

    @Builder(toBuilder = true)
    protected HallucinationMetric(final MultiModelExecutor executor, final String hallucinationDetectionTemplate) {
        super(executor);
        this.hallucinationDetectionTemplate = hallucinationDetectionTemplate != null
                ? hallucinationDetectionTemplate
                : DEFAULT_HALLUCINATION_DETECTION_TEMPLATE;
    }

    /**
     * Convenience method for single-turn scoring with default configuration.
     *
     * @param sample the sample to evaluate
     * @return the hallucination score (0.0 = no hallucination, 1.0 = complete hallucination)
     */
    public Double singleTurnScore(final Sample sample) {
        return singleTurnScore(HallucinationConfig.builder().build(), sample);
    }

    /**
     * Convenience method for async single-turn scoring with default configuration.
     *
     * @param sample the sample to evaluate
     * @return future with the hallucination score
     */
    public CompletableFuture<Double> singleTurnScoreAsync(final Sample sample) {
        return singleTurnScoreAsync(HallucinationConfig.builder().build(), sample);
    }

    @Override
    public Double singleTurnScore(final HallucinationConfig config, final Sample sample) {
        return singleTurnScoreAsync(config, sample).join();
    }

    @Override
    public CompletableFuture<Double> singleTurnScoreAsync(final HallucinationConfig config, final Sample sample) {
        final Instant startTime = Instant.now();
        final List<String> modelIds =
                config.models != null && !config.models.isEmpty() ? config.models : executor.getModelIds();

        final EvaluationNotifier notifier = createEvaluationNotifier();

        notifier.beforeMetricEvaluation(MetricEvaluationContext.builder()
                .metricName(getName())
                .sample(sample)
                .config(config)
                .modelIds(modelIds)
                .totalSteps(2)
                .build());

        return executor.runAsync(() -> {
            log.debug("Computing hallucination evaluation with explicit flow");

            final List<StepResults> accumulatedSteps = new ArrayList<>();
            final List<ModelExclusionEvent> accumulatedExclusions = new ArrayList<>();
            final List<String> excludedModels = new ArrayList<>();

            // ========== Step 1: Detect hallucinations ==========
            final String context = String.join("\n", sample.getRetrievedContexts());
            final String detectPrompt = renderDetectHallucinationsPrompt(sample, context);
            final List<ModelResult<HallucinationAnalysis>> step1Results =
                    executor.executeLlm(modelIds, detectPrompt, HallucinationAnalysis.class);

            accumulatedSteps.add(StepResults.builder()
                    .stepName("DetectHallucinations")
                    .stepIndex(0)
                    .totalSteps(2)
                    .stepType(StepType.LLM)
                    .request(detectPrompt)
                    .results(new ArrayList<ModelResult<?>>(step1Results))
                    .build());

            final Map<String, HallucinationAnalysis> step1Successful = new HashMap<>();
            for (final ModelResult<HallucinationAnalysis> result : step1Results) {
                if (result.isSuccess()) {
                    step1Successful.put(result.modelId(), result.result());
                } else {
                    excludedModels.add(result.modelId());
                    final ModelExclusionEvent exclusion = ModelExclusionEvent.builder()
                            .modelId(result.modelId())
                            .failedStepName("DetectHallucinations")
                            .failedStepIndex(0)
                            .cause(result.error())
                            .build();
                    accumulatedExclusions.add(exclusion);
                }
            }

            if (step1Successful.isEmpty()) {
                throw new IllegalStateException(
                        "All models failed at step DetectHallucinations for metric: " + getName());
            }

            // ========== Step 2: Compute score ==========
            final Map<String, Double> modelScores = new HashMap<>();
            for (final Map.Entry<String, HallucinationAnalysis> entry : step1Successful.entrySet()) {
                final double score = calculateHallucinationRate(entry.getValue());
                modelScores.put(entry.getKey(), score);
            }

            final List<ModelResult<?>> step2Results = new ArrayList<>();
            for (final Map.Entry<String, Double> e : modelScores.entrySet()) {
                step2Results.add(ModelResult.success(e.getKey(), e.getValue(), Duration.ZERO, "compute"));
            }

            accumulatedSteps.add(StepResults.builder()
                    .stepName("ComputeScore")
                    .stepIndex(1)
                    .totalSteps(2)
                    .stepType(StepType.COMPUTE)
                    .results(step2Results)
                    .build());

            final double aggregatedScore = aggregate(modelScores);

            // Build typed metadata
            final Map<String, List<HallucinationMetadata.ClaimAnalysisSummary>> claimAnalyses = new HashMap<>();
            for (final Map.Entry<String, HallucinationAnalysis> entry : step1Successful.entrySet()) {
                if (entry.getValue().claims() != null) {
                    claimAnalyses.put(
                            entry.getKey(),
                            entry.getValue().claims().stream()
                                    .map(c -> new HallucinationMetadata.ClaimAnalysisSummary(
                                            c.claim(), c.status(), c.reason()))
                                    .toList());
                }
            }

            final Duration duration = Duration.between(startTime, Instant.now());
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
                    .metadata(new HallucinationMetadata(claimAnalyses))
                    .build());

            return aggregatedScore;
        });
    }

    private String renderDetectHallucinationsPrompt(final Sample sample, final String context) {
        return PromptTemplate.builder()
                .template(this.hallucinationDetectionTemplate)
                .variables(Map.of(
                        "context", context,
                        "question", sample.getUserInput(),
                        "response", sample.getResponse()))
                .build()
                .render();
    }

    private Double calculateHallucinationRate(final HallucinationAnalysis analysis) {
        if (analysis == null || analysis.claims() == null || analysis.claims().isEmpty()) {
            log.warn("No claims returned from hallucination analysis");
            return 0.0;
        }

        final long hallucinatedCount = analysis.claims().stream()
                .filter(c -> c.status() != null
                        && (c.status().equalsIgnoreCase("HALLUCINATED")
                                || c.status().equalsIgnoreCase("CONTRADICTED")))
                .count();

        return (double) hallucinatedCount / analysis.claims().size();
    }

    /**
     * Response DTO for hallucination analysis.
     */
    public record HallucinationAnalysis(
            @JsonPropertyDescription("List of claims analyzed from the response") List<ClaimAnalysis> claims) {}

    /**
     * Individual claim analysis result.
     */
    public record ClaimAnalysis(
            @JsonPropertyDescription("The specific claim extracted from the response") String claim,
            @JsonPropertyDescription("Status: SUPPORTED, CONTRADICTED, or HALLUCINATED") String status,
            @JsonPropertyDescription("Explanation for the classification") String reason) {}

    @Data
    @Builder
    public static class HallucinationConfig implements MetricConfiguration {
        @Singular
        private List<String> models;

        @Builder.Default
        private String language = "en";
    }
}
