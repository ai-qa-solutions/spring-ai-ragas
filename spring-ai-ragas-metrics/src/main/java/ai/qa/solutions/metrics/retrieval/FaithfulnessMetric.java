package ai.qa.solutions.metrics.retrieval;

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
 * Faithfulness Metric - Measures factual consistency of response with retrieved context.
 * <p>
 * Uses {@link MultiModelExecutor} for parallel execution across multiple models
 * with explicit flow control and listener notifications.
 * <p>
 * Score ranges from 0.0 to 1.0, where higher scores indicate better consistency.
 */
@Slf4j
public class FaithfulnessMetric extends AbstractMultiModelMetric<FaithfulnessMetric.FaithfulnessConfig> {

    public static final String DEFAULT_STATEMENT_GENERATOR_TEMPLATE =
            """
                    Given a question and an answer, analyze the complexity of each sentence in the answer.
                    Break down each sentence into one or more fully understandable statements.
                    Ensure that no pronouns are used in any statement.

                    Question: {question}
                    Answer: {answer}

                    Example:
                    Question: Who was Albert Einstein and what is he best known for?
                    Answer: He was a German-born theoretical physicist, widely acknowledged to be one of the greatest and most influential physicists of all time. He was best known for developing the theory of relativity, he also made important contributions to the development of the theory of quantum mechanics.

                    Output:
                    - Albert Einstein was a German-born theoretical physicist.
                    - Albert Einstein is recognized as one of the greatest and most influential physicists of all time.
                    - Albert Einstein was best known for developing the theory of relativity.
                    - Albert Einstein also made important contributions to the development of the theory of quantum mechanics.

                    Now generate statements for the given question and answer.
                    Respond with a JSON object containing a 'statements' array with the list of extracted statements.
                    """;

    public static final String DEFAULT_NLI_STATEMENT_TEMPLATE =
            """
                    Your task is to judge the faithfulness of a series of statements based on a given context.
                    For each statement you must return verdict as 1 if the statement can be directly inferred based on the context
                    or 0 if the statement cannot be directly inferred based on the context.

                    Context:
                    {context}

                    Statements to evaluate:
                    {statements}

                    Example:
                    Context: John is a student at XYZ University. He is pursuing a degree in Computer Science. He is enrolled in several courses this semester, including Data Structures, Algorithms, and Database Management. John is a diligent student and spends a significant amount of time studying and completing assignments. He often stays late in the library to work on his projects.

                    Statements:
                    1. John is majoring in Biology.
                    2. John is taking a course on Artificial Intelligence.
                    3. John is a dedicated student.
                    4. John has a part-time job.

                    Expected Output:
                    For "John is majoring in Biology.":
                    - statement: the original text
                    - reason: John's major is explicitly mentioned as Computer Science. There is no information suggesting he is majoring in Biology.
                    - verdict: 0

                    For "John is taking a course on Artificial Intelligence.":
                    - statement: the original text
                    - reason: The context mentions the courses John is currently enrolled in, and Artificial Intelligence is not mentioned. Therefore, it cannot be deduced that John is taking a course on AI.
                    - verdict: 0

                    For "John is a dedicated student.":
                    - statement: the original text
                    - reason: The context states that he spends a significant amount of time studying and completing assignments. Additionally, it mentions that he often stays late in the library to work on his projects, which implies dedication.
                    - verdict: 1

                    For "John has a part-time job.":
                    - statement: the original text
                    - reason: There is no information given in the context about John having a part-time job.
                    - verdict: 0

                    Now evaluate the given statements based on the provided context.
                    Respond with a JSON object containing a 'verdicts' array where each item has 'statement', 'reason', and 'verdict' fields.
                    """;

    private final String statementGeneratorTemplate;
    private final String nliStatementTemplate;

    @Builder(toBuilder = true)
    protected FaithfulnessMetric(
            final MultiModelExecutor executor,
            final String statementGeneratorTemplate,
            final String nliStatementTemplate) {
        super(executor);
        this.statementGeneratorTemplate =
                statementGeneratorTemplate != null ? statementGeneratorTemplate : DEFAULT_STATEMENT_GENERATOR_TEMPLATE;
        this.nliStatementTemplate =
                nliStatementTemplate != null ? nliStatementTemplate : DEFAULT_NLI_STATEMENT_TEMPLATE;
    }

    /**
     * Convenience method for single-turn scoring with default configuration.
     *
     * @param sample the sample to evaluate
     * @return the faithfulness score
     */
    public Double singleTurnScore(final Sample sample) {
        return singleTurnScore(FaithfulnessConfig.builder().build(), sample);
    }

    /**
     * Convenience method for async single-turn scoring with default configuration.
     *
     * @param sample the sample to evaluate
     * @return future with the faithfulness score
     */
    public CompletableFuture<Double> singleTurnScoreAsync(final Sample sample) {
        return singleTurnScoreAsync(FaithfulnessConfig.builder().build(), sample);
    }

    @Override
    public Double singleTurnScore(final FaithfulnessConfig config, final Sample sample) {
        return singleTurnScoreAsync(config, sample).join();
    }

    @Override
    public CompletableFuture<Double> singleTurnScoreAsync(final FaithfulnessConfig config, final Sample sample) {
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
                .totalSteps(3) // Generate statements -> Evaluate faithfulness -> Compute score
                .metadata(Map.of("sample", sample, "config", config))
                .build());

        return executor.runAsync(() -> {
            log.debug("Computing faithfulness evaluation with explicit flow");

            // Track excluded models across all steps
            final List<String> excludedModels = new java.util.ArrayList<>();

            // ========== Step 1: Generate statements ==========
            notifier.beforeStep("GenerateStatements", 0, 3);

            final String generatePrompt = renderGenerateStatementsPrompt(sample);
            final List<ModelResult<StatementsResponse>> step1Results =
                    executor.executeLlm(modelIds, generatePrompt, StatementsResponse.class);

            notifier.afterLlmStep("GenerateStatements", 0, 3, generatePrompt, step1Results);

            // Collect successful results from step 1
            final Map<String, StatementsResponse> step1Successful = new HashMap<>();
            for (final ModelResult<StatementsResponse> result : step1Results) {
                if (result.isSuccess()) {
                    step1Successful.put(result.modelId(), result.result());
                } else {
                    excludedModels.add(result.modelId());
                    notifier.onModelExcluded(ModelExclusionEvent.builder()
                            .modelId(result.modelId())
                            .failedStepName("GenerateStatements")
                            .failedStepIndex(0)
                            .cause(result.error())
                            .build());
                }
            }

            if (step1Successful.isEmpty()) {
                throw new IllegalStateException(
                        "All models failed at step GenerateStatements for metric: " + getName());
            }

            // ========== Step 2: Evaluate faithfulness ==========
            notifier.beforeStep("EvaluateFaithfulness", 1, 3);

            final String context = String.join("\n", sample.getRetrievedContexts());

            // Execute in parallel for all models that succeeded in step 1
            final List<String> step1ModelIds = new java.util.ArrayList<>(step1Successful.keySet());
            final List<CompletableFuture<ModelResult<VerdictsResponse>>> step2Futures = step1ModelIds.stream()
                    .map(modelId -> {
                        final String statementsFormatted =
                                formatStatements(step1Successful.get(modelId).statements());
                        final String evaluatePrompt = renderEvaluateFaithfulnessPrompt(context, statementsFormatted);
                        return executor.executeLlmOnModelAsync(modelId, evaluatePrompt, VerdictsResponse.class);
                    })
                    .toList();
            CompletableFuture.allOf(step2Futures.toArray(new CompletableFuture[0]))
                    .join();
            final List<ModelResult<VerdictsResponse>> step2Results =
                    step2Futures.stream().map(CompletableFuture::join).toList();

            // Use first model's prompt as example for logging (all use same template)
            final String exampleEvaluatePrompt = step1Successful.isEmpty()
                    ? nliStatementTemplate
                    : renderEvaluateFaithfulnessPrompt(
                            context,
                            formatStatements(
                                    step1Successful.values().iterator().next().statements()));
            notifier.afterLlmStep("EvaluateFaithfulness", 1, 3, exampleEvaluatePrompt, step2Results);

            // Collect successful results from step 2
            final Map<String, VerdictsResponse> step2Successful = new HashMap<>();
            for (final ModelResult<VerdictsResponse> result : step2Results) {
                if (result.isSuccess()) {
                    step2Successful.put(result.modelId(), result.result());
                } else {
                    excludedModels.add(result.modelId());
                    notifier.onModelExcluded(ModelExclusionEvent.builder()
                            .modelId(result.modelId())
                            .failedStepName("EvaluateFaithfulness")
                            .failedStepIndex(1)
                            .cause(result.error())
                            .build());
                }
            }

            if (step2Successful.isEmpty()) {
                throw new IllegalStateException(
                        "All models failed at step EvaluateFaithfulness for metric: " + getName());
            }

            // ========== Step 3: Compute score ==========
            notifier.beforeStep("ComputeScore", 2, 3);

            final Map<String, Double> modelScores = new HashMap<>();
            for (final Map.Entry<String, VerdictsResponse> entry : step2Successful.entrySet()) {
                final double score = calculateFaithfulness(entry.getValue());
                modelScores.put(entry.getKey(), score);
            }

            // Create synthetic results for notification
            final List<ModelResult<Double>> step3Results = modelScores.entrySet().stream()
                    .map(e -> ModelResult.success(e.getKey(), e.getValue(), Duration.ZERO, "compute"))
                    .toList();

            notifier.afterComputeStep("ComputeScore", 2, 3, step3Results);

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

    private String renderGenerateStatementsPrompt(final Sample sample) {
        return PromptTemplate.builder()
                .template(this.statementGeneratorTemplate)
                .variables(Map.of("question", sample.getUserInput(), "answer", sample.getResponse()))
                .build()
                .render();
    }

    private String renderEvaluateFaithfulnessPrompt(final String context, final String statementsFormatted) {
        return PromptTemplate.builder()
                .template(this.nliStatementTemplate)
                .variables(Map.of("context", context, "statements", statementsFormatted))
                .build()
                .render();
    }

    private String formatStatements(final List<String> statements) {
        if (statements == null || statements.isEmpty()) {
            return "";
        }
        final StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < statements.size(); i++) {
            formatted.append(i + 1).append(". ").append(statements.get(i)).append("\n");
        }
        return formatted.toString();
    }

    private Double calculateFaithfulness(final VerdictsResponse verdicts) {
        if (verdicts == null
                || verdicts.verdicts() == null
                || verdicts.verdicts().isEmpty()) {
            log.warn("No verdicts returned from faithfulness evaluation");
            return 0.0;
        }

        final long faithfulStatements = verdicts.verdicts().stream()
                .filter(v -> v.verdict() != null && v.verdict() == 1)
                .count();

        return (double) faithfulStatements / verdicts.verdicts().size();
    }

    /**
     * Response DTO for statement generation
     */
    public record StatementsResponse(
            @JsonPropertyDescription(
                            "List of extracted statements from the answer, with pronouns replaced by explicit entities")
                    List<String> statements) {}

    /**
     * Response DTO for faithfulness verdicts
     */
    public record VerdictsResponse(
            @JsonPropertyDescription("List of faithfulness evaluations for each statement")
                    List<StatementVerdict> verdicts) {}

    /**
     * Individual statement faithfulness verdict
     */
    public record StatementVerdict(
            @JsonPropertyDescription("The original statement being evaluated, word-by-word") String statement,
            @JsonPropertyDescription(
                            "Detailed reasoning explaining why the statement can or cannot be inferred from the context")
                    String reason,
            @JsonPropertyDescription(
                            "Binary verdict: 1 if the statement can be directly inferred from the context, 0 otherwise")
                    Integer verdict) {}

    @Data
    @Builder
    public static class FaithfulnessConfig implements MetricConfiguration {
        @Singular
        private List<String> models;
    }
}
