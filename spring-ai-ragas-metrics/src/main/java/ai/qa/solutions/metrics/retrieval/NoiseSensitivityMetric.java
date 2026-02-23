package ai.qa.solutions.metrics.retrieval;

import ai.qa.solutions.execution.ModelResult;
import ai.qa.solutions.execution.MultiModelExecutor;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationContext;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationResult;
import ai.qa.solutions.execution.listener.dto.ModelExclusionEvent;
import ai.qa.solutions.execution.listener.dto.StepResults;
import ai.qa.solutions.execution.listener.dto.StepType;
import ai.qa.solutions.metric.AbstractMultiModelMetric;
import ai.qa.solutions.metric.metadata.NoiseSensitivityMetadata;
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
 * Noise Sensitivity Metric - LLM-based evaluation measuring how often a system makes errors
 * by providing incorrect responses when utilizing either relevant or irrelevant retrieved documents.
 * <p>
 * Uses {@link MultiModelExecutor} for parallel execution across multiple models
 * with explicit flow control and listener notifications.
 * <p>
 * The score ranges from 0 to 1, with lower values indicating better performance.
 * Measures the proportion of incorrect statements in the response that can be attributed
 * to the retrieved contexts (relevant or irrelevant based on mode).
 */
@Slf4j
public class NoiseSensitivityMetric extends AbstractMultiModelMetric<NoiseSensitivityMetric.NoiseSensitivityConfig> {
    public static final String DEFAULT_STATEMENT_GENERATOR_PROMPT =
            """
                Given a question and an answer, create a list of statements that are present in the answer.
                Each statement should be atomic, meaning it contains only one piece of information.

                Question: {question}
                Answer: {answer}

                Instructions:
                1. Break down the answer into individual, atomic statements
                2. Each statement should be a complete, standalone assertion
                3. Avoid compound statements - split them into separate statements
                4. Focus on factual claims that can be verified
                5. Maintain the original meaning from the answer
                6. Do not add information not present in the answer

                Respond with a JSON object containing:
                - statements: A list of atomic statements extracted from the answer
                """;

    public static final String DEFAULT_STATEMENT_FAITHFULNESS_PROMPT =
            """
            IMPORTANT: You are evaluating a RAG system with SYNTHETIC TEST DATA containing INTENTIONALLY INCORRECT facts.

                Context: {context}
                Statements: {statements}

                CRITICAL INSTRUCTIONS - READ CAREFULLY:

                YOU MUST COMPLETELY IGNORE ALL REAL-WORLD KNOWLEDGE FOR THIS EVALUATION.

                CRITICAL: Evaluate ONLY using the context provided above. DO NOT use information from any previous contexts you may have seen.
                Each evaluation is independent. Forget all previous contexts.

                This is a test environment. The context contains deliberately wrong information to test system behavior.
                Your job is NOT to correct facts, but to check if statements match the context.

                EVALUATION RULES (MANDATORY):
                1. Pretend you have complete amnesia about real-world facts AND previous contexts
                2. The ONLY truth for this evaluation is what THIS SPECIFIC context states
                3. If context says "Paris is in Germany" and statement says "Paris, Germany" → verdict: TRUE (they match the context)
                4. If context says "built in 1899" and statement says "built in 1889" → verdict: FALSE (numbers don't match)
                5. If context doesn't mention something (e.g., location), statement can't be verified → verdict: FALSE
                6. Compare word-by-word and fact-by-fact against THIS context ONLY
                7. Even if reality contradicts the context, use ONLY this context as your source of truth
                8. If the context is about a different topic than the statement, verdict: FALSE

                EXAMPLES OF CORRECT EVALUATION:
                - Context: "The tower is in Paris, capital of Germany"
                  Statement: "It is located in Paris, Germany"
                  Verdict: TRUE (both say Paris + Germany, they match)

                - Context: "Built in 1899"
                  Statement: "Built in 1889"
                  Verdict: FALSE (1899 ≠ 1889, they don't match)

                - Context: "The tower was designed by Gustav"
                  Statement: "It is located in Paris, France"
                  Verdict: FALSE (location not mentioned in this context)

                For EACH statement:
                Step 1: What EXACTLY does THIS context say? (not what you remember from before)
                Step 2: Does the statement match what THIS context says (word-for-word, fact-for-fact)?
                Step 3: Is there ANY detail that contradicts or isn't mentioned in THIS context?
                Step 4: Verdict: TRUE only if EVERYTHING in the statement matches THIS context. FALSE if ANY part differs or is missing.

                DO NOT let your knowledge of real-world facts OR previous contexts influence your verdict. Each evaluation is completely independent.

                Respond with a JSON object containing:
                   - verdicts: A list of verdicts for each statement, where each verdict contains:
                   - statement: The original statement
                   - verdict: true if the ENTIRE statement matches THIS context exactly, false otherwise
                   - reason: Explanation comparing statement to THIS context (not to real-world facts or previous contexts)
            """;

    private final String statementGeneratorPrompt;
    private final String statementFaithfulnessPrompt;

    @Builder(toBuilder = true)
    protected NoiseSensitivityMetric(
            final MultiModelExecutor executor,
            final String statementGeneratorPrompt,
            final String statementFaithfulnessPrompt) {
        super(executor);
        this.statementGeneratorPrompt =
                statementGeneratorPrompt != null ? statementGeneratorPrompt : DEFAULT_STATEMENT_GENERATOR_PROMPT;
        this.statementFaithfulnessPrompt = statementFaithfulnessPrompt != null
                ? statementFaithfulnessPrompt
                : DEFAULT_STATEMENT_FAITHFULNESS_PROMPT;
    }

    /**
     * Convenience method for single-turn scoring with default configuration.
     */
    public Double singleTurnScore(final Sample sample) {
        return singleTurnScore(NoiseSensitivityConfig.builder().build(), sample);
    }

    /**
     * Convenience method for async single-turn scoring with default configuration.
     */
    public CompletableFuture<Double> singleTurnScoreAsync(final Sample sample) {
        return singleTurnScoreAsync(NoiseSensitivityConfig.builder().build(), sample);
    }

    @Override
    public Double singleTurnScore(final NoiseSensitivityConfig config, final Sample sample) {
        return singleTurnScoreAsync(config, sample).join();
    }

    @Override
    public CompletableFuture<Double> singleTurnScoreAsync(final NoiseSensitivityConfig config, final Sample sample) {
        // Validation
        if (sample.getUserInput() == null || sample.getUserInput().trim().isEmpty()) {
            log.warn("No user input provided for Noise Sensitivity evaluation");
            return CompletableFuture.completedFuture(0.0);
        }
        if (sample.getResponse() == null || sample.getResponse().trim().isEmpty()) {
            log.warn("No response provided for Noise Sensitivity evaluation");
            return CompletableFuture.completedFuture(0.0);
        }
        if (sample.getReference() == null || sample.getReference().trim().isEmpty()) {
            log.warn("No reference provided for Noise Sensitivity evaluation");
            return CompletableFuture.completedFuture(0.0);
        }
        if (sample.getRetrievedContexts() == null
                || sample.getRetrievedContexts().isEmpty()) {
            log.warn("No retrieved contexts provided for Noise Sensitivity evaluation");
            return CompletableFuture.completedFuture(0.0);
        }

        final Instant startTime = Instant.now();
        final List<String> modelIds =
                config.models != null && !config.models.isEmpty() ? config.models : executor.getModelIds();

        final List<String> retrievedContexts = sample.getRetrievedContexts();
        final int numContexts = retrievedContexts.size();
        // 2 decompose steps + 1 groundTruth eval + 1 parallel context eval + 1 compute
        final int totalSteps = 5;

        log.debug("Computing noise sensitivity with {} contexts in {} mode", numContexts, config.getMode());

        // Create evaluation-specific notifier for thread-safe parallel execution
        final EvaluationNotifier notifier = createEvaluationNotifier();

        // Notify listeners
        notifier.beforeMetricEvaluation(MetricEvaluationContext.builder()
                .metricName(getName())
                .sample(sample)
                .config(config)
                .modelIds(modelIds)
                .totalSteps(totalSteps)
                .build());

        return executor.runAsync(() -> {
            log.debug("Computing noise sensitivity evaluation with explicit flow");

            // Local accumulators for steps and exclusions
            final List<StepResults> accumulatedSteps = new ArrayList<>();
            final List<ModelExclusionEvent> accumulatedExclusions = new ArrayList<>();

            // Track excluded models across all steps
            final java.util.Set<String> excludedModelIds = new java.util.HashSet<>();

            int stepIndex = 0;

            // ========== Step 1: Decompose reference into statements ==========
            final String decomposeRefPrompt = renderDecomposePrompt(sample.getUserInput(), sample.getReference());
            final List<ModelResult<StatementsResponse>> decomposeRefResults =
                    executor.executeLlm(modelIds, decomposeRefPrompt, StatementsResponse.class);

            accumulatedSteps.add(StepResults.builder()
                    .stepName("DecomposeReference")
                    .stepIndex(stepIndex)
                    .totalSteps(totalSteps)
                    .stepType(StepType.LLM)
                    .request(decomposeRefPrompt)
                    .results(new ArrayList<>(decomposeRefResults))
                    .build());

            final Map<String, StatementsResponse> refStatementsMap = new HashMap<>();
            for (final ModelResult<StatementsResponse> result : decomposeRefResults) {
                if (result.isSuccess()) {
                    refStatementsMap.put(result.modelId(), result.result());
                } else {
                    excludedModelIds.add(result.modelId());
                    accumulatedExclusions.add(ModelExclusionEvent.builder()
                            .modelId(result.modelId())
                            .failedStepName("DecomposeReference")
                            .failedStepIndex(stepIndex)
                            .cause(result.error())
                            .build());
                }
            }

            if (refStatementsMap.isEmpty()) {
                throw new IllegalStateException(
                        "All models failed at step DecomposeReference for metric: " + getName());
            }

            stepIndex++;

            // ========== Step 2: Decompose response into statements ==========
            final String decomposeRespPrompt = renderDecomposePrompt(sample.getUserInput(), sample.getResponse());

            // Execute in parallel for all models
            final List<String> activeModelIds = new ArrayList<>(refStatementsMap.keySet());
            final List<CompletableFuture<ModelResult<StatementsResponse>>> decomposeRespFutures =
                    activeModelIds.stream()
                            .map(modelId -> executor.executeLlmOnModelAsync(
                                    modelId, decomposeRespPrompt, StatementsResponse.class))
                            .toList();
            CompletableFuture.allOf(decomposeRespFutures.toArray(new CompletableFuture[0]))
                    .join();
            final List<ModelResult<StatementsResponse>> decomposeRespResults =
                    decomposeRespFutures.stream().map(CompletableFuture::join).toList();

            accumulatedSteps.add(StepResults.builder()
                    .stepName("DecomposeResponse")
                    .stepIndex(stepIndex)
                    .totalSteps(totalSteps)
                    .stepType(StepType.LLM)
                    .request(decomposeRespPrompt)
                    .results(new ArrayList<>(decomposeRespResults))
                    .build());

            final Map<String, StatementsResponse> respStatementsMap = new HashMap<>();
            for (final ModelResult<StatementsResponse> result : decomposeRespResults) {
                if (result.isSuccess()) {
                    respStatementsMap.put(result.modelId(), result.result());
                } else {
                    excludedModelIds.add(result.modelId());
                    accumulatedExclusions.add(ModelExclusionEvent.builder()
                            .modelId(result.modelId())
                            .failedStepName("DecomposeResponse")
                            .failedStepIndex(stepIndex)
                            .cause(result.error())
                            .build());
                }
            }

            if (respStatementsMap.isEmpty()) {
                throw new IllegalStateException("All models failed at step DecomposeResponse for metric: " + getName());
            }

            stepIndex++;

            // ========== Step 3: Evaluate response statements against reference (groundTruthToAnswer)
            // ==========
            final Map<String, FaithfulnessVerdictsResponse> groundTruthToAnswerMap = new HashMap<>();

            // Execute in parallel for all models
            final List<String> respModelIds = new ArrayList<>(respStatementsMap.keySet());
            final List<CompletableFuture<ModelResult<FaithfulnessVerdictsResponse>>> groundTruthFutures =
                    respModelIds.stream()
                            .map(modelId -> {
                                final StatementsResponse respStmts = respStatementsMap.get(modelId);
                                final String statementsFormatted = formatStatements(respStmts.statements());
                                final String prompt =
                                        renderFaithfulnessPrompt(sample.getReference(), statementsFormatted);
                                return executor.executeLlmOnModelAsync(
                                        modelId, prompt, FaithfulnessVerdictsResponse.class);
                            })
                            .toList();
            CompletableFuture.allOf(groundTruthFutures.toArray(new CompletableFuture[0]))
                    .join();
            final List<ModelResult<FaithfulnessVerdictsResponse>> groundTruthResults =
                    groundTruthFutures.stream().map(CompletableFuture::join).toList();

            for (final ModelResult<FaithfulnessVerdictsResponse> result : groundTruthResults) {
                if (result.isSuccess()) {
                    groundTruthToAnswerMap.put(result.modelId(), result.result());
                }
            }

            // Use first model's prompt as example for logging
            final String examplePrompt = respStatementsMap.isEmpty()
                    ? statementFaithfulnessPrompt
                    : renderFaithfulnessPrompt(
                            sample.getReference(),
                            formatStatements(
                                    respStatementsMap.values().iterator().next().statements()));

            accumulatedSteps.add(StepResults.builder()
                    .stepName("EvaluateGroundTruthToAnswer")
                    .stepIndex(stepIndex)
                    .totalSteps(totalSteps)
                    .stepType(StepType.LLM)
                    .request(examplePrompt)
                    .results(new ArrayList<>(groundTruthResults))
                    .build());

            stepIndex++;

            // ========== Step 4: Evaluate ALL context comparisons IN PARALLEL ==========
            // (retrievedToGroundTruth + retrievedToAnswer)

            // Pre-initialize result maps with null lists of correct size
            final Map<String, List<FaithfulnessVerdictsResponse>> retrievedToGroundTruthMap = new HashMap<>();
            for (final String modelId : refStatementsMap.keySet()) {
                final List<FaithfulnessVerdictsResponse> list = new ArrayList<>();
                for (int i = 0; i < numContexts; i++) {
                    list.add(null);
                }
                retrievedToGroundTruthMap.put(modelId, list);
            }

            final Map<String, List<FaithfulnessVerdictsResponse>> retrievedToAnswerMap = new HashMap<>();
            for (final String modelId : respStatementsMap.keySet()) {
                final List<FaithfulnessVerdictsResponse> list = new ArrayList<>();
                for (int i = 0; i < numContexts; i++) {
                    list.add(null);
                }
                retrievedToAnswerMap.put(modelId, list);
            }

            final List<String> refModelIds = new ArrayList<>(refStatementsMap.keySet());
            final List<String> respModelIdsList = new ArrayList<>(respStatementsMap.keySet());

            // Launch ALL futures for ALL contexts in parallel
            final List<List<CompletableFuture<ModelResult<FaithfulnessVerdictsResponse>>>> refContextFuturesList =
                    new ArrayList<>();
            final List<List<CompletableFuture<ModelResult<FaithfulnessVerdictsResponse>>>> respContextFuturesList =
                    new ArrayList<>();

            for (int contextIdx = 0; contextIdx < numContexts; contextIdx++) {
                final String context = retrievedContexts.get(contextIdx);

                // Launch ref-to-context futures for this context
                final List<CompletableFuture<ModelResult<FaithfulnessVerdictsResponse>>> refFutures =
                        refModelIds.stream()
                                .map(modelId -> {
                                    final StatementsResponse refStmts = refStatementsMap.get(modelId);
                                    final String statementsFormatted = formatStatements(refStmts.statements());
                                    final String prompt = renderFaithfulnessPrompt(context, statementsFormatted);
                                    return executor.executeLlmOnModelAsync(
                                            modelId, prompt, FaithfulnessVerdictsResponse.class);
                                })
                                .toList();
                refContextFuturesList.add(refFutures);

                // Launch resp-to-context futures for this context
                final List<CompletableFuture<ModelResult<FaithfulnessVerdictsResponse>>> respFutures =
                        respModelIdsList.stream()
                                .map(modelId -> {
                                    final StatementsResponse respStmts = respStatementsMap.get(modelId);
                                    final String statementsFormatted = formatStatements(respStmts.statements());
                                    final String prompt = renderFaithfulnessPrompt(context, statementsFormatted);
                                    return executor.executeLlmOnModelAsync(
                                            modelId, prompt, FaithfulnessVerdictsResponse.class);
                                })
                                .toList();
                respContextFuturesList.add(respFutures);
            }

            // Flatten all futures and wait for ALL at once
            final List<CompletableFuture<?>> allFutures = new ArrayList<>();
            refContextFuturesList.forEach(allFutures::addAll);
            respContextFuturesList.forEach(allFutures::addAll);
            CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0]))
                    .join();

            // Process ref-to-context results
            final List<ModelResult<?>> allContextEvalResults = new ArrayList<>();
            for (int contextIdx = 0; contextIdx < numContexts; contextIdx++) {
                final List<CompletableFuture<ModelResult<FaithfulnessVerdictsResponse>>> refFutures =
                        refContextFuturesList.get(contextIdx);
                for (int modelIdx = 0; modelIdx < refModelIds.size(); modelIdx++) {
                    final ModelResult<FaithfulnessVerdictsResponse> result =
                            refFutures.get(modelIdx).join();
                    final String modelId = refModelIds.get(modelIdx);
                    allContextEvalResults.add(result);
                    if (result.isSuccess()) {
                        retrievedToGroundTruthMap.get(modelId).set(contextIdx, result.result());
                    }
                }
            }

            // Process resp-to-context results
            for (int contextIdx = 0; contextIdx < numContexts; contextIdx++) {
                final List<CompletableFuture<ModelResult<FaithfulnessVerdictsResponse>>> respFutures =
                        respContextFuturesList.get(contextIdx);
                for (int modelIdx = 0; modelIdx < respModelIdsList.size(); modelIdx++) {
                    final ModelResult<FaithfulnessVerdictsResponse> result =
                            respFutures.get(modelIdx).join();
                    final String modelId = respModelIdsList.get(modelIdx);
                    allContextEvalResults.add(result);
                    if (result.isSuccess()) {
                        retrievedToAnswerMap.get(modelId).set(contextIdx, result.result());
                    }
                }
            }

            // Build step for context evaluations
            final String contextEvalPrompt = !refStatementsMap.isEmpty() && !retrievedContexts.isEmpty()
                    ? renderFaithfulnessPrompt(
                            retrievedContexts.get(0),
                            formatStatements(
                                    refStatementsMap.values().iterator().next().statements()))
                    : statementFaithfulnessPrompt;

            accumulatedSteps.add(StepResults.builder()
                    .stepName("EvaluateAllContexts")
                    .stepIndex(stepIndex)
                    .totalSteps(totalSteps)
                    .stepType(StepType.LLM)
                    .request(contextEvalPrompt)
                    .results(allContextEvalResults)
                    .build());

            stepIndex++;

            // ========== Final step: Compute noise sensitivity ==========
            final Map<String, Double> modelScores = new HashMap<>();

            for (final String modelId : respStatementsMap.keySet()) {
                // Skip models that failed any required step
                if (!groundTruthToAnswerMap.containsKey(modelId)
                        || !refStatementsMap.containsKey(modelId)
                        || !retrievedToGroundTruthMap.containsKey(modelId)
                        || !retrievedToAnswerMap.containsKey(modelId)) {
                    continue;
                }

                final StatementsResponse refStmts = refStatementsMap.get(modelId);
                final StatementsResponse respStmts = respStatementsMap.get(modelId);
                final FaithfulnessVerdictsResponse groundTruthToAnswer = groundTruthToAnswerMap.get(modelId);
                final List<FaithfulnessVerdictsResponse> retrievedToGroundTruth =
                        retrievedToGroundTruthMap.get(modelId);
                final List<FaithfulnessVerdictsResponse> retrievedToAnswer = retrievedToAnswerMap.get(modelId);

                // Convert to 2D arrays
                final boolean[][] gtToAnswer = convertToGroundTruthToAnswer(groundTruthToAnswer, respStmts);
                final boolean[][] rtToGt = convertToRetrievedToGroundTruth(retrievedToGroundTruth, refStmts);
                final boolean[][] rtToAnswer = convertToRetrievedToAnswer(retrievedToAnswer, respStmts);

                final FaithfulnessResults results = new FaithfulnessResults(gtToAnswer, rtToGt, rtToAnswer);
                final double score = calculateNoiseSensitivity(results, config.getMode());
                modelScores.put(modelId, score);
            }

            // Create synthetic results for compute step
            final List<ModelResult<Double>> computeResults = modelScores.entrySet().stream()
                    .map(e -> ModelResult.success(e.getKey(), e.getValue(), Duration.ZERO, "compute"))
                    .toList();

            accumulatedSteps.add(StepResults.builder()
                    .stepName("ComputeNoiseSensitivity")
                    .stepIndex(stepIndex)
                    .totalSteps(totalSteps)
                    .stepType(StepType.COMPUTE)
                    .results(new ArrayList<>(computeResults))
                    .build());

            if (modelScores.isEmpty()) {
                throw new IllegalStateException("All models failed for metric: " + getName());
            }

            final double aggregatedScore = aggregate(modelScores);

            // Build metadata
            final Map<String, List<String>> refStatementsMetadata = new HashMap<>();
            for (final Map.Entry<String, StatementsResponse> entry : refStatementsMap.entrySet()) {
                refStatementsMetadata.put(
                        entry.getKey(),
                        entry.getValue().statements() != null ? entry.getValue().statements() : List.of());
            }
            final Map<String, List<String>> respStatementsMetadata = new HashMap<>();
            for (final Map.Entry<String, StatementsResponse> entry : respStatementsMap.entrySet()) {
                respStatementsMetadata.put(
                        entry.getKey(),
                        entry.getValue().statements() != null ? entry.getValue().statements() : List.of());
            }

            // Notify with full results
            final Duration duration = Duration.between(startTime, Instant.now());
            notifier.afterMetricEvaluation(MetricEvaluationResult.builder()
                    .metricName(getName())
                    .sample(sample)
                    .config(config)
                    .modelIds(modelIds)
                    .aggregatedScore(aggregatedScore)
                    .modelScores(modelScores)
                    .excludedModels(new ArrayList<>(excludedModelIds))
                    .totalDuration(duration)
                    .steps(accumulatedSteps)
                    .exclusions(accumulatedExclusions)
                    .metadata(new NoiseSensitivityMetadata(
                            config.getMode().name(), refStatementsMetadata, respStatementsMetadata, numContexts))
                    .build());

            return aggregatedScore;
        });
    }

    private String renderDecomposePrompt(final String question, final String answer) {
        return PromptTemplate.builder()
                .template(this.statementGeneratorPrompt)
                .variables(Map.of("question", question, "answer", answer))
                .build()
                .render();
    }

    private String renderFaithfulnessPrompt(final String context, final String statementsFormatted) {
        return PromptTemplate.builder()
                .template(this.statementFaithfulnessPrompt)
                .variables(Map.of("context", context, "statements", statementsFormatted))
                .build()
                .render();
    }

    private String formatStatements(final List<String> statements) {
        if (statements == null || statements.isEmpty()) {
            return "";
        }
        return String.join("\n", statements);
    }

    private boolean[][] convertToGroundTruthToAnswer(
            final FaithfulnessVerdictsResponse response, final StatementsResponse statements) {
        final List<String> stmtsList = statements.statements() != null ? statements.statements() : List.of();
        final boolean[][] result = new boolean[1][stmtsList.size()];

        if (response != null && response.verdicts() != null) {
            final List<StatementVerdict> verdicts = response.verdicts();
            for (int i = 0; i < stmtsList.size() && i < verdicts.size(); i++) {
                result[0][i] =
                        verdicts.get(i).verdict() != null ? verdicts.get(i).verdict() : false;
            }
        }

        return result;
    }

    private boolean[][] convertToRetrievedToGroundTruth(
            final List<FaithfulnessVerdictsResponse> responses, final StatementsResponse statements) {
        final List<String> stmtsList = statements.statements() != null ? statements.statements() : List.of();
        final int numStatements = stmtsList.size();
        final int numCtxs = responses.size();

        final boolean[][] result = new boolean[numStatements][numCtxs];

        for (int contextIdx = 0; contextIdx < numCtxs; contextIdx++) {
            final FaithfulnessVerdictsResponse response = responses.get(contextIdx);
            if (response != null && response.verdicts() != null) {
                final List<StatementVerdict> verdicts = response.verdicts();
                for (int stmtIdx = 0; stmtIdx < numStatements && stmtIdx < verdicts.size(); stmtIdx++) {
                    result[stmtIdx][contextIdx] = verdicts.get(stmtIdx).verdict() != null
                            ? verdicts.get(stmtIdx).verdict()
                            : false;
                }
            }
        }

        return result;
    }

    private boolean[][] convertToRetrievedToAnswer(
            final List<FaithfulnessVerdictsResponse> responses, final StatementsResponse statements) {
        final List<String> stmtsList = statements.statements() != null ? statements.statements() : List.of();
        final int numStatements = stmtsList.size();
        final int numCtxs = responses.size();

        final boolean[][] result = new boolean[numStatements][numCtxs];

        for (int contextIdx = 0; contextIdx < numCtxs; contextIdx++) {
            final FaithfulnessVerdictsResponse response = responses.get(contextIdx);
            if (response != null && response.verdicts() != null) {
                final List<StatementVerdict> verdicts = response.verdicts();
                for (int stmtIdx = 0; stmtIdx < numStatements && stmtIdx < verdicts.size(); stmtIdx++) {
                    result[stmtIdx][contextIdx] = verdicts.get(stmtIdx).verdict() != null
                            ? verdicts.get(stmtIdx).verdict()
                            : false;
                }
            }
        }

        return result;
    }

    @SuppressWarnings("DuplicatedCode")
    private Double calculateNoiseSensitivity(final FaithfulnessResults results, final NoiseSensitivityMode mode) {
        final boolean[][] groundTruthToAnswer = results.groundTruthToAnswer();
        final boolean[][] retrievedToGroundTruth = results.retrievedToGroundTruth();
        final boolean[][] retrievedToAnswer = results.retrievedToAnswer();

        if (groundTruthToAnswer.length == 0 || groundTruthToAnswer[0].length == 0 || retrievedToAnswer.length == 0) {
            return 0.0;
        }

        final int numResponseStatements = groundTruthToAnswer[0].length;
        final int numCtxs = retrievedToGroundTruth.length > 0 ? retrievedToGroundTruth[0].length : 0;

        if (numCtxs == 0) {
            return 0.0;
        }

        // Create incorrect array by inverting ground_truth2answer
        final boolean[] incorrect = new boolean[numResponseStatements];
        for (int i = 0; i < numResponseStatements; i++) {
            incorrect[i] = !groundTruthToAnswer[0][i];
        }

        // Compute relevant retrievals using max over axis 0 (ground truth statements)
        final boolean[] relevantRetrieved = new boolean[numCtxs];
        for (int contextIdx = 0; contextIdx < numCtxs; contextIdx++) {
            boolean hasRelevantStatement = false;
            for (final boolean[] booleans : retrievedToGroundTruth) {
                if (booleans[contextIdx]) {
                    hasRelevantStatement = true;
                    break;
                }
            }
            relevantRetrieved[contextIdx] = hasRelevantStatement;
        }

        // Compute relevant faithful using max over axis 1 (contexts)
        final boolean[] relevantFaithful = new boolean[numResponseStatements];
        for (int answerStatementIdx = 0; answerStatementIdx < numResponseStatements; answerStatementIdx++) {
            boolean hasFaithfulContext = false;
            for (int contextIdx = 0; contextIdx < numCtxs; contextIdx++) {
                if (relevantRetrieved[contextIdx] && retrievedToAnswer[answerStatementIdx][contextIdx]) {
                    hasFaithfulContext = true;
                    break;
                }
            }
            relevantFaithful[answerStatementIdx] = hasFaithfulContext;
        }

        if (mode == NoiseSensitivityMode.IRRELEVANT) {
            // Compute irrelevant retrievals: ~relevant_retrieved
            final boolean[] irrelevantRetrieved = new boolean[numCtxs];
            for (int i = 0; i < numCtxs; i++) {
                irrelevantRetrieved[i] = !relevantRetrieved[i];
            }

            // Compute irrelevant faithful using max over axis 1 (contexts)
            final boolean[] irrelevantFaithful = new boolean[numResponseStatements];
            for (int answerStatementIdx = 0; answerStatementIdx < numResponseStatements; answerStatementIdx++) {
                boolean hasFaithfulIrrelevantContext = false;
                for (int contextIdx = 0; contextIdx < numCtxs; contextIdx++) {
                    if (irrelevantRetrieved[contextIdx] && retrievedToAnswer[answerStatementIdx][contextIdx]) {
                        hasFaithfulIrrelevantContext = true;
                        break;
                    }
                }
                irrelevantFaithful[answerStatementIdx] = hasFaithfulIrrelevantContext;
            }

            // Keep them exclusive (irrelevant should not include relevant)
            for (int i = 0; i < numResponseStatements; i++) {
                irrelevantFaithful[i] = irrelevantFaithful[i] && !relevantFaithful[i];
            }

            // Return mean of (irrelevant_faithful & incorrect)
            int count = 0;
            for (int i = 0; i < numResponseStatements; i++) {
                if (irrelevantFaithful[i] && incorrect[i]) {
                    count++;
                }
            }
            return (double) count / numResponseStatements;

        } else { // RELEVANT mode
            // Return mean of (relevant_faithful & incorrect)
            int count = 0;
            for (int i = 0; i < numResponseStatements; i++) {
                if (relevantFaithful[i] && incorrect[i]) {
                    count++;
                }
            }
            return (double) count / numResponseStatements;
        }
    }

    /**
     * Data structure to hold faithfulness evaluation results
     */
    private record FaithfulnessResults(
            boolean[][] groundTruthToAnswer, // Shape: (1, num_response_statements)
            boolean[][] retrievedToGroundTruth, // Shape: (num_reference_statements, num_contexts)
            boolean[][] retrievedToAnswer // Shape: (num_response_statements, num_contexts)
            ) {}

    /**
     * Response DTO for statement decomposition
     */
    public record StatementsResponse(
            @JsonPropertyDescription("List of atomic statements extracted from the answer") List<String> statements) {}

    /**
     * Response DTO for individual statement verdict
     */
    public record StatementVerdict(
            @JsonPropertyDescription("The original statement") String statement,
            @JsonPropertyDescription("True if the statement can be inferred from context, false otherwise")
                    Boolean verdict,
            @JsonPropertyDescription("Explanation for the verdict") String reason) {}

    /**
     * Response DTO for statement faithfulness evaluation
     */
    public record FaithfulnessVerdictsResponse(
            @JsonPropertyDescription("List of verdicts for each statement") List<StatementVerdict> verdicts) {}

    /**
     * Noise sensitivity evaluation mode
     */
    public enum NoiseSensitivityMode {
        RELEVANT, // Measures errors from relevant retrieved contexts
        IRRELEVANT // Measures errors from irrelevant retrieved contexts
    }

    @Data
    @Builder
    public static class NoiseSensitivityConfig implements MetricConfiguration {
        /**
         * Evaluation mode for noise sensitivity
         * RELEVANT: measures errors from relevant contexts
         * IRRELEVANT: measures errors from irrelevant contexts
         */
        @Builder.Default
        private NoiseSensitivityMode mode = NoiseSensitivityMode.RELEVANT;

        /**
         * List of model IDs to use for multimodel execution.
         * If not specified, uses default models from executor configuration.
         */
        @Singular
        private List<String> models;
    }
}
