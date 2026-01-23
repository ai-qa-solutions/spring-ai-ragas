package ai.qa.solutions.metrics.response;

import ai.qa.solutions.execution.ModelResult;
import ai.qa.solutions.execution.MultiModelExecutor;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationContext;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationResult;
import ai.qa.solutions.execution.listener.dto.ModelExclusionEvent;
import ai.qa.solutions.metric.AbstractMultiModelMetric;
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
import lombok.Getter;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.PromptTemplate;

/**
 * Factual Correctness Metric - measures factual accuracy of response compared to reference.
 * <p>
 * This metric decomposes both the response and reference into individual claims,
 * then uses Natural Language Inference (NLI) to verify which claims from the response
 * are supported by the reference and vice versa.
 * <p>
 * <strong>Algorithm:</strong>
 * <ol>
 *   <li>Decompose response into atomic claims</li>
 *   <li>Decompose reference into atomic claims</li>
 *   <li>Verify response claims against reference (for precision)</li>
 *   <li>Verify reference claims against response (for recall)</li>
 *   <li>Compute precision, recall, and F1 score</li>
 * </ol>
 * <p>
 * <strong>Score interpretation:</strong>
 * <ul>
 *   <li>1.0 - All claims are factually correct and complete</li>
 *   <li>0.8-1.0 - High factual accuracy with minor omissions</li>
 *   <li>0.5-0.8 - Moderate accuracy, some facts missing or incorrect</li>
 *   <li>0.0-0.5 - Low accuracy, many factual errors or omissions</li>
 * </ul>
 * <p>
 * Based on RAGAS FactualCorrectness metric.
 *
 * @author Artem Simeshin
 * @see Sample
 * @see FactualCorrectnessConfig
 * @since 1.0
 */
@Slf4j
public class FactualCorrectnessMetric
        extends AbstractMultiModelMetric<FactualCorrectnessMetric.FactualCorrectnessConfig> {

    public static final String DEFAULT_CLAIMS_DECOMPOSITION_TEMPLATE =
            """
                    Given a text, decompose it into atomic, independent claims.
                    Each claim should be a single factual statement that can be verified independently.
                    Ensure claims are self-contained (no pronouns without clear referents).

                    Text:
                    {text}

                    Instructions:
                    1. Break down the text into individual factual claims
                    2. Each claim should be atomic (cannot be split further)
                    3. Replace pronouns with explicit entities
                    4. Keep claims factual (not opinions or subjective statements)

                    Example:
                    Text: "Albert Einstein was a German-born physicist. He developed the theory of relativity and won the Nobel Prize in 1921."

                    Claims:
                    - Albert Einstein was a German-born physicist.
                    - Albert Einstein developed the theory of relativity.
                    - Albert Einstein won the Nobel Prize in 1921.

                    Now decompose the given text into claims.
                    Respond with a JSON object containing a 'claims' array with the list of extracted claims.
                    """;

    public static final String DEFAULT_NLI_VERIFICATION_TEMPLATE =
            """
                    Your task is to verify claims using Natural Language Inference (NLI).
                    For each claim, determine if it is SUPPORTED, CONTRADICTED, or NEUTRAL based on the given context.

                    Context (source of truth):
                    {context}

                    Claims to verify:
                    {claims}

                    Instructions:
                    For each claim, determine:
                    - SUPPORTED: The claim can be directly inferred from the context
                    - CONTRADICTED: The claim is directly contradicted by the context
                    - NEUTRAL: The claim cannot be verified from the context (not enough information)

                    Example:
                    Context: "Paris is the capital of France. The Eiffel Tower is located in Paris."

                    Claims:
                    1. Paris is the capital of France.
                    2. Paris is the largest city in Europe.
                    3. The Eiffel Tower is not in Paris.

                    Verdicts:
                    1. claim: "Paris is the capital of France." verdict: SUPPORTED reason: "Directly stated in context"
                    2. claim: "Paris is the largest city in Europe." verdict: NEUTRAL reason: "Context doesn't mention city size"
                    3. claim: "The Eiffel Tower is not in Paris." verdict: CONTRADICTED reason: "Context states Eiffel Tower IS in Paris"

                    Now verify the given claims against the context.
                    Respond with a JSON object containing a 'verdicts' array where each item has 'claim', 'verdict' (SUPPORTED/CONTRADICTED/NEUTRAL), and 'reason' fields.
                    """;

    private final String claimsDecompositionTemplate;
    private final String nliVerificationTemplate;

    @Builder(toBuilder = true)
    protected FactualCorrectnessMetric(
            final MultiModelExecutor executor,
            final String claimsDecompositionTemplate,
            final String nliVerificationTemplate) {
        super(executor);
        this.claimsDecompositionTemplate = claimsDecompositionTemplate != null
                ? claimsDecompositionTemplate
                : DEFAULT_CLAIMS_DECOMPOSITION_TEMPLATE;
        this.nliVerificationTemplate =
                nliVerificationTemplate != null ? nliVerificationTemplate : DEFAULT_NLI_VERIFICATION_TEMPLATE;
    }

    /**
     * Convenience method for single-turn scoring with default configuration.
     *
     * @param sample the sample to evaluate
     * @return the factual correctness score
     */
    public Double singleTurnScore(final Sample sample) {
        return singleTurnScore(FactualCorrectnessConfig.builder().build(), sample);
    }

    /**
     * Convenience method for async single-turn scoring with default configuration.
     *
     * @param sample the sample to evaluate
     * @return future with the factual correctness score
     */
    public CompletableFuture<Double> singleTurnScoreAsync(final Sample sample) {
        return singleTurnScoreAsync(FactualCorrectnessConfig.builder().build(), sample);
    }

    @Override
    public Double singleTurnScore(final FactualCorrectnessConfig config, final Sample sample) {
        return singleTurnScoreAsync(config, sample).join();
    }

    @Override
    public CompletableFuture<Double> singleTurnScoreAsync(final FactualCorrectnessConfig config, final Sample sample) {

        // Validate required inputs
        final String response = sample.getResponse();
        if (response == null || response.trim().isEmpty()) {
            log.warn("No response provided for Factual Correctness evaluation");
            return CompletableFuture.completedFuture(0.0);
        }

        final String reference = sample.getReference();
        if (reference == null || reference.trim().isEmpty()) {
            log.warn("No reference provided for Factual Correctness evaluation");
            return CompletableFuture.completedFuture(0.0);
        }

        final Instant startTime = Instant.now();
        final List<String> modelIds =
                config.models != null && !config.models.isEmpty() ? config.models : executor.getModelIds();

        // Create evaluation-specific notifier for thread-safe parallel execution
        final EvaluationNotifier notifier = createEvaluationNotifier();

        // Notify listeners before evaluation
        notifier.beforeMetricEvaluation(MetricEvaluationContext.builder()
                .metricName(getName())
                .sample(sample)
                .config(config)
                .modelIds(modelIds)
                .totalSteps(4) // DecomposeResponse -> DecomposeReference -> VerifyNLI -> ComputeScore
                .metadata(Map.of("sample", sample, "config", config))
                .build());

        return executor.runAsync(() -> {
            log.debug("Computing factual correctness evaluation with explicit flow");

            // Track excluded models across all steps
            final List<String> excludedModels = new ArrayList<>();

            // ========== Step 1: Decompose response into claims ==========
            notifier.beforeStep("DecomposeResponseClaims", 0, 4);

            final String decomposeResponsePrompt = renderDecomposeClaimsPrompt(response);
            final List<ModelResult<ClaimsResponse>> step1Results =
                    executor.executeLlm(modelIds, decomposeResponsePrompt, ClaimsResponse.class);

            notifier.afterLlmStep("DecomposeResponseClaims", 0, 4, decomposeResponsePrompt, step1Results);

            // Collect successful results from step 1
            final Map<String, ClaimsResponse> responseClaims = new HashMap<>();
            for (final ModelResult<ClaimsResponse> result : step1Results) {
                if (result.isSuccess()
                        && result.result() != null
                        && result.result().claims() != null) {
                    responseClaims.put(result.modelId(), result.result());
                } else {
                    excludedModels.add(result.modelId());
                    notifier.onModelExcluded(ModelExclusionEvent.builder()
                            .modelId(result.modelId())
                            .failedStepName("DecomposeResponseClaims")
                            .failedStepIndex(0)
                            .cause(result.error())
                            .build());
                }
            }

            if (responseClaims.isEmpty()) {
                throw new IllegalStateException(
                        "All models failed at step DecomposeResponseClaims for metric: " + getName());
            }

            // ========== Step 2: Decompose reference into claims ==========
            notifier.beforeStep("DecomposeReferenceClaims", 1, 4);

            final List<String> step1SuccessfulModels = new ArrayList<>(responseClaims.keySet());
            final String decomposeReferencePrompt = renderDecomposeClaimsPrompt(reference);
            final List<ModelResult<ClaimsResponse>> step2Results =
                    executor.executeLlm(step1SuccessfulModels, decomposeReferencePrompt, ClaimsResponse.class);

            notifier.afterLlmStep("DecomposeReferenceClaims", 1, 4, decomposeReferencePrompt, step2Results);

            // Collect successful results from step 2
            final Map<String, ClaimsResponse> referenceClaims = new HashMap<>();
            for (final ModelResult<ClaimsResponse> result : step2Results) {
                if (result.isSuccess()
                        && result.result() != null
                        && result.result().claims() != null) {
                    referenceClaims.put(result.modelId(), result.result());
                } else {
                    excludedModels.add(result.modelId());
                    responseClaims.remove(result.modelId()); // Remove from previous step too
                    notifier.onModelExcluded(ModelExclusionEvent.builder()
                            .modelId(result.modelId())
                            .failedStepName("DecomposeReferenceClaims")
                            .failedStepIndex(1)
                            .cause(result.error())
                            .build());
                }
            }

            if (referenceClaims.isEmpty()) {
                throw new IllegalStateException(
                        "All models failed at step DecomposeReferenceClaims for metric: " + getName());
            }

            // ========== Step 3: Verify claims with NLI ==========
            notifier.beforeStep("VerifyClaimsNLI", 2, 4);

            // For each model, verify response claims against reference (precision)
            // and reference claims against response (recall)
            final List<String> step2SuccessfulModels = new ArrayList<>(referenceClaims.keySet());
            final Map<String, NliVerificationResult> nliResults = new HashMap<>();

            final List<CompletableFuture<Void>> nliVerificationFutures = step2SuccessfulModels.stream()
                    .map(modelId -> CompletableFuture.runAsync(() -> {
                        try {
                            // Verify response claims against reference (for precision)
                            final String referenceText = reference;
                            final List<String> respClaims =
                                    responseClaims.get(modelId).claims();
                            final String precisionPrompt = renderNliVerificationPrompt(referenceText, respClaims);
                            final ModelResult<NliResponse> precisionResult =
                                    executor.executeLlmOnModel(modelId, precisionPrompt, NliResponse.class);

                            // Verify reference claims against response (for recall)
                            final String responseText = response;
                            final List<String> refClaims =
                                    referenceClaims.get(modelId).claims();
                            final String recallPrompt = renderNliVerificationPrompt(responseText, refClaims);
                            final ModelResult<NliResponse> recallResult =
                                    executor.executeLlmOnModel(modelId, recallPrompt, NliResponse.class);

                            if (precisionResult.isSuccess() && recallResult.isSuccess()) {
                                synchronized (nliResults) {
                                    nliResults.put(
                                            modelId,
                                            new NliVerificationResult(precisionResult.result(), recallResult.result()));
                                }
                            }
                        } catch (final Exception e) {
                            log.warn("NLI verification failed for model {}: {}", modelId, e.getMessage());
                        }
                    }))
                    .toList();

            CompletableFuture.allOf(nliVerificationFutures.toArray(new CompletableFuture[0]))
                    .join();

            // Create synthetic results for notification
            final List<ModelResult<NliVerificationResult>> step3Results = nliResults.entrySet().stream()
                    .map(e -> ModelResult.success(e.getKey(), e.getValue(), Duration.ZERO, "nli"))
                    .toList();

            // Use first model's prompt as example
            final String exampleNliPrompt = step2SuccessfulModels.isEmpty()
                    ? nliVerificationTemplate
                    : renderNliVerificationPrompt(
                            reference,
                            responseClaims.get(step2SuccessfulModels.get(0)).claims());
            notifier.afterLlmStep("VerifyClaimsNLI", 2, 4, exampleNliPrompt, step3Results);

            // Update excluded models
            for (final String modelId : step2SuccessfulModels) {
                if (!nliResults.containsKey(modelId)) {
                    excludedModels.add(modelId);
                    notifier.onModelExcluded(ModelExclusionEvent.builder()
                            .modelId(modelId)
                            .failedStepName("VerifyClaimsNLI")
                            .failedStepIndex(2)
                            .build());
                }
            }

            if (nliResults.isEmpty()) {
                throw new IllegalStateException("All models failed at step VerifyClaimsNLI for metric: " + getName());
            }

            // ========== Step 4: Compute score ==========
            notifier.beforeStep("ComputeScore", 3, 4);

            final Map<String, Double> modelScores = new HashMap<>();
            for (final Map.Entry<String, NliVerificationResult> entry : nliResults.entrySet()) {
                final double score = calculateScore(entry.getValue(), config.getMode());
                modelScores.put(entry.getKey(), score);
            }

            // Create synthetic results for notification
            final List<ModelResult<Double>> step4Results = modelScores.entrySet().stream()
                    .map(e -> ModelResult.success(e.getKey(), e.getValue(), Duration.ZERO, "compute"))
                    .toList();

            notifier.afterComputeStep("ComputeScore", 3, 4, step4Results);

            final double aggregatedScore = aggregate(modelScores);

            // Notify with full results
            final Duration duration = Duration.between(startTime, Instant.now());
            notifier.afterMetricEvaluation(MetricEvaluationResult.builder()
                    .metricName(getName())
                    .aggregatedScore(aggregatedScore)
                    .modelScores(modelScores)
                    .excludedModels(excludedModels)
                    .totalDuration(duration)
                    .metadata(Map.of("sample", sample, "config", config))
                    .build());

            return aggregatedScore;
        });
    }

    private String renderDecomposeClaimsPrompt(final String text) {
        return PromptTemplate.builder()
                .template(this.claimsDecompositionTemplate)
                .variables(Map.of("text", text))
                .build()
                .render();
    }

    private String renderNliVerificationPrompt(final String context, final List<String> claims) {
        final String formattedClaims = formatClaims(claims);
        return PromptTemplate.builder()
                .template(this.nliVerificationTemplate)
                .variables(Map.of("context", context, "claims", formattedClaims))
                .build()
                .render();
    }

    private String formatClaims(final List<String> claims) {
        if (claims == null || claims.isEmpty()) {
            return "";
        }
        final StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < claims.size(); i++) {
            formatted.append(i + 1).append(". ").append(claims.get(i)).append("\n");
        }
        return formatted.toString();
    }

    private double calculateScore(final NliVerificationResult result, final Mode mode) {
        // Calculate precision: how many response claims are supported by reference
        final double precision = calculateSupportedRatio(result.precisionVerdicts());

        // Calculate recall: how many reference claims are supported by response
        final double recall = calculateSupportedRatio(result.recallVerdicts());

        return switch (mode) {
            case PRECISION -> precision;
            case RECALL -> recall;
            case F1 -> {
                if (precision + recall == 0) {
                    yield 0.0;
                }
                yield 2 * (precision * recall) / (precision + recall);
            }
        };
    }

    private double calculateSupportedRatio(final NliResponse response) {
        if (response == null
                || response.verdicts() == null
                || response.verdicts().isEmpty()) {
            return 0.0;
        }

        final long supported = response.verdicts().stream()
                .filter(v -> "SUPPORTED".equalsIgnoreCase(v.verdict()))
                .count();

        return (double) supported / response.verdicts().size();
    }

    /**
     * Response DTO for claims decomposition.
     */
    public record ClaimsResponse(
            @JsonPropertyDescription("List of atomic claims extracted from the text") List<String> claims) {}

    /**
     * Response DTO for NLI verification.
     */
    public record NliResponse(
            @JsonPropertyDescription("List of NLI verdicts for each claim") List<NliVerdict> verdicts) {}

    /**
     * Individual NLI verdict for a claim.
     */
    public record NliVerdict(
            @JsonPropertyDescription("The claim being verified") String claim,
            @JsonPropertyDescription("NLI verdict: SUPPORTED, CONTRADICTED, or NEUTRAL") String verdict,
            @JsonPropertyDescription("Explanation for the verdict") String reason) {}

    /**
     * Internal result holder for NLI verification step.
     */
    private record NliVerificationResult(NliResponse precisionVerdicts, NliResponse recallVerdicts) {}

    /**
     * Scoring mode for factual correctness.
     */
    @Getter
    public enum Mode {
        F1("F1 score - harmonic mean of precision and recall"),
        PRECISION("Precision - ratio of correct claims in response"),
        RECALL("Recall - ratio of reference claims covered in response");

        private final String description;

        Mode(final String description) {
            this.description = description;
        }
    }

    /**
     * Configuration class for Factual Correctness metric parameters.
     */
    @Data
    @Builder
    public static class FactualCorrectnessConfig implements MetricConfiguration {

        /**
         * Scoring mode: F1 (default), PRECISION, or RECALL.
         */
        @Builder.Default
        private Mode mode = Mode.F1;

        /**
         * List of model IDs to use for multi-model execution.
         * If empty, all available models from executor will be used.
         */
        @Singular
        private List<String> models;
    }
}
