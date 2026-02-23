package ai.qa.solutions.metrics.retrieval;

import ai.qa.solutions.execution.ModelResult;
import ai.qa.solutions.execution.MultiModelExecutor;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationContext;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationResult;
import ai.qa.solutions.execution.listener.dto.ModelExclusionEvent;
import ai.qa.solutions.execution.listener.dto.StepResults;
import ai.qa.solutions.execution.listener.dto.StepType;
import ai.qa.solutions.metric.AbstractMultiModelMetric;
import ai.qa.solutions.metric.metadata.ResponseRelevancyMetadata;
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
 * Response Relevancy Metric - measures how relevant a response is to the user input.
 * <p>
 * Uses {@link MultiModelExecutor} for parallel execution across multiple models
 * with explicit flow control, listener notifications, and embeddings.
 * <p>
 * <strong>IMPORTANT LIMITATIONS:</strong> This metric has significant limitations for edge cases
 * and should be used as a <strong>screening tool only</strong> before expensive and time-consuming
 * metrics, not for final decision-making. Always combine with other metrics like Answer Correctness
 * and Faithfulness.
 * <p>
 * <strong>What this metric CAN reliably do:</strong>
 * <ul>
 *   <li>Detect noncommittal/evasive answers (returns 0.0)</li>
 *   <li>Identify perfect direct answers (returns 0.95-0.98)</li>
 *   <li>Compare complete vs incomplete answers (relative scoring)</li>
 *   <li>Work without reference answers (reference-free)</li>
 *   <li>Support multiple languages</li>
 * </ul>
 * <p>
 * <strong>Score range:</strong> Typically 0-1, though mathematically can be -1 to 1 due to cosine similarity.
 *
 * @author Artem Simeshin
 * @see Sample
 * @see ResponseRelevancyConfig
 * @since 1.0
 */
@Slf4j
public class ResponseRelevancyMetric extends AbstractMultiModelMetric<ResponseRelevancyMetric.ResponseRelevancyConfig> {

    public static final String DEFAULT_QUESTION_GENERATION_PROMPT =
            """
                    Given a user's original question and a response to it, generate {numberOfQuestions} different questions that this response could be answering.

                    User Question: {userInput}
                    Response: {response}

                    CRITICAL INSTRUCTIONS:
                    1. Generate {numberOfQuestions} DIFFERENT questions that could have led to this response
                    2. Each question MUST reflect ONLY what the response actually addresses
                    3. If the response is about a DIFFERENT topic than the user's question, generate questions about that different topic
                    4. If the response is incomplete or doesn't fully answer the user's question, generate questions that ask ONLY about the parts that were answered
                    5. DO NOT try to match the user's question - match what the response actually says
                    6. Each question should capture a different aspect or angle of the response
                    7. For each question, identify if the answer is noncommittal (evasive, vague, or ambiguous)
                    8. Examples of noncommittal answers: "I don't know", "I'm not sure", "I cannot answer that", "That's unclear"
                    9. Mark noncommittal as 1 if the answer is noncommittal, 0 if the answer is committal
                    10. The questions should be natural and reflect how a user might ask
                    11. If the response only PARTIALLY answers the user's question, ensure generated questions
                        reflect ONLY the answered portion and mark the response as incomplete.
                    12. If the response is about the same general topic but answers a DIFFERENT specific question,
                        generate questions about that different specific aspect.
                    13. When generating questions, consider if the answer addresses:
                        - The EXACT information requested (not just related information)
                        - ALL parts of a multi-part question
                        - The SPECIFIC aspect asked about (not just the general topic)
                    14. If the answer is about the same topic but addresses a DIFFERENT specific question,
                        your generated questions must reflect that difference explicitly.

                    Respond with a JSON object containing:
                    - questions: Array of objects, each with:
                      * question: The generated question string
                      * noncommittal: Integer value (1 for noncommittal, 0 for committal)

                    Examples:

                    Example 1:
                    User Question: "Where was Albert Einstein born?"
                    Response: "Albert Einstein was born in Germany."
                    Output: {{
                      "questions": [
                        {{"question": "Where was Albert Einstein born?", "noncommittal": 0}},
                        {{"question": "In which country was Einstein born?", "noncommittal": 0}},
                        {{"question": "What is Albert Einstein's birthplace?", "noncommittal": 0}}
                      ]
                    }}

                    Example 2:
                    User Question: "What is the capital of France?"
                    Response: "Italy is famous for its pasta and pizza."
                    Output: {{
                      "questions": [
                        {{"question": "What is Italy famous for?", "noncommittal": 0}},
                        {{"question": "What are Italy's culinary specialties?", "noncommittal": 0}},
                        {{"question": "What traditional foods is Italy known for?", "noncommittal": 0}}
                      ]
                    }}
                    Reasoning: Response talks about Italy's cuisine, NOT about France. All questions must be about what response actually addresses.

                    Example 3:
                    User Question: "Where is France located and what is its capital?"
                    Response: "France is located in Western Europe."
                    Output: {{
                      "questions": [
                        {{"question": "Where is France located?", "noncommittal": 0}},
                        {{"question": "In which part of Europe is France?", "noncommittal": 0}},
                        {{"question": "What is France's geographical location?", "noncommittal": 0}}
                      ]
                    }}
                    Reasoning: Response only answers the location part, ignoring the capital. All questions must ask ONLY about location.

                    Example 4:
                    User Question: "Какая столица Франции?"
                    Response: "Италия славится своей пастой и пиццей."
                    Output: {{
                      "questions": [
                        {{"question": "Чем славится Италия?", "noncommittal": 0}},
                        {{"question": "Какие блюда популярны в Италии?", "noncommittal": 0}},
                        {{"question": "Что известно об итальянской кухне?", "noncommittal": 0}}
                      ]
                    }}
                    Reasoning: Response is about Italy's food, NOT France. All questions must be about Italy.

                    Example 5 (NONCOMMITTAL - all questions must have noncommittal: 1):
                    User Question: "What is the capital of France?"
                    Response: "I don't know the answer to that question."
                    Output: {{
                      "questions": [
                        {{"question": "What question can you not answer?", "noncommittal": 1}},
                        {{"question": "What do you not know?", "noncommittal": 1}},
                        {{"question": "What information is unavailable to you?", "noncommittal": 1}}
                      ]
                    }}
                    Reasoning: The response is a noncommittal "I don't know" answer. ALL questions MUST have noncommittal: 1.

                    Example 6 (NONCOMMITTAL - another example):
                    User Question: "How do I configure Spring Boot?"
                    Response: "I'm not sure about that."
                    Output: {{
                      "questions": [
                        {{"question": "What are you uncertain about?", "noncommittal": 1}},
                        {{"question": "What do you lack confidence answering?", "noncommittal": 1}},
                        {{"question": "What topic makes you unsure?", "noncommittal": 1}}
                      ]
                    }}
                    Reasoning: "I'm not sure" is a noncommittal response. ALL questions MUST have noncommittal: 1.
                    """;

    private final String questionGenerationPrompt;

    @Builder(toBuilder = true)
    protected ResponseRelevancyMetric(final MultiModelExecutor executor, final String questionGenerationPrompt) {
        super(executor);
        this.questionGenerationPrompt =
                questionGenerationPrompt != null ? questionGenerationPrompt : DEFAULT_QUESTION_GENERATION_PROMPT;
    }

    /**
     * Convenience method for single-turn scoring with default configuration.
     *
     * @param sample the sample to evaluate
     * @return the response relevancy score
     */
    public Double singleTurnScore(final Sample sample) {
        return singleTurnScore(ResponseRelevancyConfig.builder().build(), sample);
    }

    /**
     * Convenience method for async single-turn scoring with default configuration.
     *
     * @param sample the sample to evaluate
     * @return future with the response relevancy score
     */
    public CompletableFuture<Double> singleTurnScoreAsync(final Sample sample) {
        return singleTurnScoreAsync(ResponseRelevancyConfig.builder().build(), sample);
    }

    @Override
    public Double singleTurnScore(final ResponseRelevancyConfig config, final Sample sample) {
        return singleTurnScoreAsync(config, sample).join();
    }

    @Override
    public CompletableFuture<Double> singleTurnScoreAsync(final ResponseRelevancyConfig config, final Sample sample) {
        // Validate required inputs
        final String userInput = sample.getUserInput();
        if (userInput == null || userInput.trim().isEmpty()) {
            log.warn("No user input provided for Response Relevancy evaluation");
            return CompletableFuture.completedFuture(0.0);
        }

        final String response = sample.getResponse();
        if (response == null || response.trim().isEmpty()) {
            log.warn("No response provided for Response Relevancy evaluation");
            return CompletableFuture.completedFuture(0.0);
        }

        final Instant startTime = Instant.now();
        final List<String> modelIds =
                config.models != null && !config.models.isEmpty() ? config.models : executor.getModelIds();
        final List<String> embeddingModelIds = executor.getEmbeddingModelIds();

        // Create evaluation-specific notifier for thread-safe parallel execution
        final EvaluationNotifier notifier = createEvaluationNotifier();

        // Notify listeners before evaluation
        notifier.beforeMetricEvaluation(MetricEvaluationContext.builder()
                .metricName(getName())
                .sample(sample)
                .config(config)
                .modelIds(modelIds)
                .embeddingModelIds(embeddingModelIds)
                .totalSteps(3) // Generate questions -> Compute embeddings -> Compute similarity
                .build());

        return executor.runAsync(() -> {
            log.debug("Computing response relevancy evaluation with explicit flow");

            // Local accumulators for steps and exclusions
            final List<StepResults> accumulatedSteps = new ArrayList<>();
            final List<ModelExclusionEvent> accumulatedExclusions = new ArrayList<>();

            // Track excluded models across all steps
            final List<String> excludedModels = new ArrayList<>();

            // ========== Step 1: Generate questions ==========
            final String generatePrompt = renderQuestionGenerationPrompt(config, sample);
            final List<ModelResult<GeneratedQuestionsResponse>> step1Results =
                    executor.executeLlm(modelIds, generatePrompt, GeneratedQuestionsResponse.class);

            accumulatedSteps.add(StepResults.builder()
                    .stepName("GenerateQuestions")
                    .stepIndex(0)
                    .totalSteps(3)
                    .stepType(StepType.LLM)
                    .request(generatePrompt)
                    .results(new ArrayList<>(step1Results))
                    .build());

            // Collect successful results from step 1
            final Map<String, GeneratedQuestionsResponse> step1Successful = new HashMap<>();
            for (final ModelResult<GeneratedQuestionsResponse> result : step1Results) {
                if (result.isSuccess()) {
                    step1Successful.put(result.modelId(), result.result());
                } else {
                    excludedModels.add(result.modelId());
                    accumulatedExclusions.add(ModelExclusionEvent.builder()
                            .modelId(result.modelId())
                            .failedStepName("GenerateQuestions")
                            .failedStepIndex(0)
                            .cause(result.error())
                            .build());
                }
            }

            if (step1Successful.isEmpty()) {
                throw new IllegalStateException("All models failed at step GenerateQuestions for metric: " + getName());
            }

            // ========== Step 2: Compute embeddings ==========
            final Map<String, EmbeddingsResult> step2Successful = new HashMap<>();
            final List<ModelResult<EmbeddingsResult>> step2Results = new ArrayList<>();
            final List<ModelResult<?>> embeddingModelResults = new ArrayList<>();

            // Prepare embedding tasks for parallel execution
            final Map<String, List<String>> modelTexts = new HashMap<>();
            for (final Map.Entry<String, GeneratedQuestionsResponse> entry : step1Successful.entrySet()) {
                final String modelId = entry.getKey();
                final GeneratedQuestionsResponse questionsResponse = entry.getValue();

                // Check for noncommittal questions early
                if (questionsResponse == null
                        || questionsResponse.questions() == null
                        || questionsResponse.questions().isEmpty()) {
                    log.warn("No questions generated from response for model {}", modelId);
                    step2Results.add(ModelResult.success(
                            modelId, new EmbeddingsResult(null, List.of()), Duration.ZERO, "no questions"));
                    continue;
                }

                // Check if all answers are noncommittal
                // Per Python RAGAS: score = cosine_sim.mean() * int(not all_noncommittal)
                // If all questions are noncommittal, the model contributes 0.0 to aggregation
                final boolean allNoncommittal =
                        questionsResponse.questions().stream().allMatch(q -> q.noncommittal() == 1);

                if (allNoncommittal) {
                    log.debug("All generated questions indicate noncommittal response for model {}", modelId);
                    // Add to step2Successful with empty embeddings so it gets score 0.0 in step 3
                    // This ensures noncommittal responses contribute 0.0 to the aggregated score
                    final EmbeddingsResult noncommittalResult = new EmbeddingsResult(null, List.of());
                    step2Successful.put(modelId, noncommittalResult);
                    step2Results.add(ModelResult.success(modelId, noncommittalResult, Duration.ZERO, "noncommittal"));
                    continue;
                }

                // Prepare texts for embedding: user input + generated questions
                final List<String> texts = new ArrayList<>();
                texts.add(sample.getUserInput());

                for (final GeneratedQuestion q : questionsResponse.questions()) {
                    if (q.question() != null && !q.question().trim().isEmpty()) {
                        texts.add(q.question());
                    }
                }

                modelTexts.put(modelId, texts);
            }

            // Execute all embeddings in parallel
            if (!modelTexts.isEmpty()) {
                final Map<String, CompletableFuture<List<ModelResult<List<float[]>>>>> embeddingFutures =
                        new HashMap<>();

                for (final Map.Entry<String, List<String>> entry : modelTexts.entrySet()) {
                    embeddingFutures.put(entry.getKey(), executor.executeEmbeddingsAsync(entry.getValue()));
                }

                // Wait for all embedding tasks to complete
                CompletableFuture.allOf(embeddingFutures.values().toArray(new CompletableFuture[0]))
                        .join();

                // Process embedding results
                for (final Map.Entry<String, CompletableFuture<List<ModelResult<List<float[]>>>>> entry :
                        embeddingFutures.entrySet()) {
                    final String modelId = entry.getKey();
                    final List<ModelResult<List<float[]>>> embeddingResults =
                            entry.getValue().join();

                    // Collect all embedding model results for timeline
                    embeddingModelResults.addAll(embeddingResults);

                    // Take first successful embedding result
                    for (final ModelResult<List<float[]>> embResult : embeddingResults) {
                        if (embResult.isSuccess()) {
                            final List<float[]> embeddings = embResult.result();
                            if (embeddings != null && !embeddings.isEmpty()) {
                                // First embedding is for user input
                                final double[] userInputEmbedding = convertToDoubleArray(embeddings.get(0));

                                // Rest are for generated questions
                                final List<double[]> questionEmbeddings = new ArrayList<>();
                                for (int i = 1; i < embeddings.size(); i++) {
                                    questionEmbeddings.add(convertToDoubleArray(embeddings.get(i)));
                                }

                                final EmbeddingsResult embeddingsResult =
                                        new EmbeddingsResult(userInputEmbedding, questionEmbeddings);
                                step2Successful.put(modelId, embeddingsResult);
                                step2Results.add(ModelResult.success(
                                        modelId, embeddingsResult, embResult.duration(), embResult.request()));
                            }
                            break; // Use first successful embedding
                        }
                    }
                }
            }

            accumulatedSteps.add(StepResults.builder()
                    .stepName("ComputeEmbeddings")
                    .stepIndex(1)
                    .totalSteps(3)
                    .stepType(StepType.EMBEDDING)
                    .request(sample.getUserInput() + " + generated questions")
                    .results(new ArrayList<>(step2Results))
                    .embeddingModelResults(new ArrayList<>(embeddingModelResults))
                    .build());

            // ========== Step 3: Compute cosine similarity ==========
            final Map<String, Double> modelScores = new HashMap<>();

            for (final Map.Entry<String, EmbeddingsResult> entry : step2Successful.entrySet()) {
                final String modelId = entry.getKey();
                final EmbeddingsResult embeddingsResult = entry.getValue();

                if (embeddingsResult == null
                        || embeddingsResult.userInputEmbedding() == null
                        || embeddingsResult.questionEmbeddings().isEmpty()) {
                    // Noncommittal or no embeddings
                    modelScores.put(modelId, 0.0);
                    continue;
                }

                // Calculate cosine similarity for each generated question
                double totalSimilarity = 0.0;
                int validQuestions = 0;

                for (final double[] questionEmbedding : embeddingsResult.questionEmbeddings()) {
                    try {
                        final double similarity =
                                cosineSimilarity(embeddingsResult.userInputEmbedding(), questionEmbedding);
                        totalSimilarity += similarity;
                        validQuestions++;

                        log.debug("Question embedding similarity: {}", similarity);
                    } catch (final Exception e) {
                        log.warn("Failed to calculate similarity for question embedding", e);
                    }
                }

                if (validQuestions == 0) {
                    log.warn("No valid questions for similarity calculation for model {}", modelId);
                    modelScores.put(modelId, 0.0);
                } else {
                    final double relevancyScore = totalSimilarity / validQuestions;
                    log.debug(
                            "Final relevancy score for model {}: {} (from {} questions)",
                            modelId,
                            relevancyScore,
                            validQuestions);
                    modelScores.put(modelId, relevancyScore);
                }
            }

            // Create synthetic results for compute step
            final List<ModelResult<Double>> step3Results = modelScores.entrySet().stream()
                    .map(e -> ModelResult.success(e.getKey(), e.getValue(), Duration.ZERO, "compute"))
                    .toList();

            accumulatedSteps.add(StepResults.builder()
                    .stepName("ComputeCosineSimilarity")
                    .stepIndex(2)
                    .totalSteps(3)
                    .stepType(StepType.COMPUTE)
                    .results(new ArrayList<>(step3Results))
                    .build());

            if (modelScores.isEmpty()) {
                throw new IllegalStateException("All models failed to compute similarity for metric: " + getName());
            }

            final double aggregatedScore = aggregate(modelScores);

            // Build metadata
            final Map<String, List<String>> generatedQuestionsMap = new HashMap<>();
            final Map<String, List<Boolean>> noncommittalFlagsMap = new HashMap<>();
            for (final Map.Entry<String, GeneratedQuestionsResponse> entry : step1Successful.entrySet()) {
                final String modelId = entry.getKey();
                final GeneratedQuestionsResponse qr = entry.getValue();
                if (qr.questions() != null) {
                    generatedQuestionsMap.put(
                            modelId,
                            qr.questions().stream()
                                    .map(q -> q.question() != null ? q.question() : "")
                                    .toList());
                    noncommittalFlagsMap.put(
                            modelId,
                            qr.questions().stream()
                                    .map(q -> q.noncommittal() != null && q.noncommittal() == 1)
                                    .toList());
                }
            }

            // Notify with full results
            final Duration duration = Duration.between(startTime, Instant.now());
            notifier.afterMetricEvaluation(MetricEvaluationResult.builder()
                    .metricName(getName())
                    .sample(sample)
                    .config(config)
                    .modelIds(modelIds)
                    .embeddingModelIds(embeddingModelIds)
                    .aggregatedScore(aggregatedScore)
                    .modelScores(modelScores)
                    .excludedModels(excludedModels)
                    .totalDuration(duration)
                    .steps(accumulatedSteps)
                    .exclusions(accumulatedExclusions)
                    .metadata(new ResponseRelevancyMetadata(
                            generatedQuestionsMap, noncommittalFlagsMap, modelScores, config.getNumberOfQuestions()))
                    .build());

            return aggregatedScore;
        });
    }

    private String renderQuestionGenerationPrompt(final ResponseRelevancyConfig config, final Sample sample) {
        return PromptTemplate.builder()
                .template(this.questionGenerationPrompt)
                .variables(Map.of(
                        "userInput", sample.getUserInput(),
                        "response", sample.getResponse(),
                        "numberOfQuestions", config.getNumberOfQuestions()))
                .build()
                .render();
    }

    /**
     * Converts float array to double array for more precise mathematical operations.
     */
    private double[] convertToDoubleArray(final float[] floatArray) {
        final double[] doubleArray = new double[floatArray.length];
        for (int i = 0; i < floatArray.length; i++) {
            doubleArray[i] = floatArray[i];
        }
        return doubleArray;
    }

    /**
     * Calculates cosine similarity between two embedding vectors.
     *
     * @param vectorA First embedding vector
     * @param vectorB Second embedding vector
     * @return Cosine similarity score in range [-1, 1], typically [0, 1] for text embeddings
     * @throws IllegalArgumentException if vectors have different dimensions
     */
    private double cosineSimilarity(final double[] vectorA, final double[] vectorB) {
        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException(
                    "Vectors must have same length: " + vectorA.length + " vs " + vectorB.length);
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        normA = Math.sqrt(normA);
        normB = Math.sqrt(normB);

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (normA * normB);
    }

    /**
     * Result DTO for embeddings computation step.
     *
     * @param userInputEmbedding Embedding vector for the user input
     * @param questionEmbeddings Embedding vectors for generated questions
     */
    public record EmbeddingsResult(double[] userInputEmbedding, List<double[]> questionEmbeddings) {}

    /**
     * Response DTO for multiple generated questions returned by LLM.
     *
     * @param questions List of generated questions with noncommittal flags
     */
    public record GeneratedQuestionsResponse(
            @JsonPropertyDescription("Array of generated questions with noncommittal flags")
                    List<GeneratedQuestion> questions) {}

    /**
     * Response DTO for a single generated question with metadata.
     *
     * @param question     The generated question string in natural language
     * @param noncommittal Integer flag: 1 if the answer is noncommittal (evasive, vague), 0 if committal
     */
    public record GeneratedQuestion(
            @JsonPropertyDescription("The generated question string") String question,
            @JsonPropertyDescription("1 if the answer is noncommittal (evasive, vague), 0 if committal")
                    Integer noncommittal) {}

    /**
     * Configuration class for Response Relevancy metric parameters.
     */
    @Data
    @Builder
    public static class ResponseRelevancyConfig implements MetricConfiguration {
        /**
         * Number of artificial questions to generate from the response for similarity comparison.
         */
        @Builder.Default
        private int numberOfQuestions = 3;

        /**
         * List of model IDs to use for multi-model execution.
         */
        @Singular
        private List<String> models;

        /**
         * Creates a default configuration instance.
         *
         * @return Default configuration with 3 questions
         */
        public static ResponseRelevancyConfig defaultConfig() {
            return ResponseRelevancyConfig.builder().build();
        }
    }
}
