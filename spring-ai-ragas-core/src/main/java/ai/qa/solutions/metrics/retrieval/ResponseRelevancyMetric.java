package ai.qa.solutions.metrics.retrieval;

import ai.qa.solutions.metric.Metric;
import ai.qa.solutions.sample.Sample;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;

/**
 * Response Relevancy Metric - measures how relevant a response is to the user input.
 * <p>
 * <strong>⚠️ IMPORTANT LIMITATIONS:</strong> This metric has significant limitations for edge cases
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
 * <strong>What this metric CANNOT reliably do:</strong>
 * <ul>
 *   <li>❌ Detect partial answers to multipart questions (scores 0.75-0.97 instead of expected ~0.5)</li>
 *   <li>❌ Distinguish different aspects of same topic</li>
 *   <li>❌ Identify off-topic answers with similar linguistic patterns (scores 0.43-0.63)</li>
 *   <li>❌ Validate factual correctness (incorrect but on-topic answers score identically to correct ones)</li>
 *   <li>❌ Reliably handle single-word nonsense responses</li>
 * </ul>
 * <p>
 * <strong>How it works:</strong>
 * <ol>
 *   <li>Generates multiple artificial questions based on the response using LLM (with context of original user question)</li>
 *   <li>Detects if all generated questions indicate noncommittal answers</li>
 *   <li>Computes cosine similarity between embeddings of the user input and generated questions</li>
 *   <li>Averages the similarity scores to get the final relevancy score</li>
 * </ol>
 * <p>
 * <strong>Score interpretation:</strong>
 * <ul>
 *   <li><strong>0.0:</strong> Noncommittal answer - "I don't know" (RELIABLE)</li>
 *   <li><strong>0.90-0.98:</strong> Could be perfect answer OR incorrect answer with same topic (UNRELIABLE for correctness)</li>
 *   <li><strong>0.30-0.90:</strong> Mixed quality - requires additional verification</li>
 *   <li><strong>Below 0.30:</strong> Likely irrelevant or nonsensical</li>
 * </ul>
 * <p>
 * <strong>Score range:</strong> Typically 0-1, though mathematically can be -1 to 1 due to cosine similarity.
 * In practice, negative scores are extremely rare and indicate highly divergent embeddings.
 * <p>
 * <strong>Performance considerations:</strong>
 * <ul>
 *   <li>Default configuration uses 3 questions for balance of accuracy and speed</li>
 *   <li>More questions (5-10) provide more stable results but increase latency</li>
 *   <li>Embedding model choice significantly affects accuracy - see documentation for recommendations</li>
 * </ul>
 *
 * @author Artem Simeshin
 * @see Sample
 * @see ResponseRelevancyConfig
 * @since 1.0
 */
@Slf4j
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ResponseRelevancyMetric implements Metric<ResponseRelevancyMetric.ResponseRelevancyConfig> {

    /**
     * Default prompt template for generating questions from response.
     * <p>
     * This prompt is carefully crafted to:
     * <ul>
     *   <li>Generate diverse questions that reflect only what the response actually addresses</li>
     *   <li>Handle off-topic responses by generating questions about the different topic</li>
     *   <li>Detect incomplete responses by focusing only on answered portions</li>
     *   <li>Identify noncommittal answers (evasive, vague, or ambiguous responses)</li>
     * </ul>
     * <p>
     * The prompt includes multiple examples covering various edge cases:
     * perfect answers, off-topic responses, partial answers, and multilingual scenarios.
     */
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
                    """;

    /**
     * ChatClient for LLM operations - used to generate artificial questions from responses.
     */
    @NonNull
    private final ChatClient chatClient;

    /**
     * EmbeddingModel for vector operations - used to compute semantic similarity between texts.
     */
    @NonNull
    private final EmbeddingModel embeddingModel;

    /**
     * Custom prompt template for question generation.
     * <p>
     * Defaults to {@link #DEFAULT_QUESTION_GENERATION_PROMPT} but can be customized
     * for specific use cases or languages. The prompt must include placeholders for:
     * <ul>
     *   <li>{@code {userInput}} - the original user question</li>
     *   <li>{@code {response}} - the system response to evaluate</li>
     *   <li>{@code {numberOfQuestions}} - number of questions to generate</li>
     * </ul>
     */
    @NonNull
    @Builder.Default
    private final String questionGenerationPrompt = DEFAULT_QUESTION_GENERATION_PROMPT;

    /**
     * Calculates response relevancy score for a single sample.
     * <p>
     * This method evaluates how relevant a system's response is to the user's input
     * using artificial question generation and semantic similarity comparison.
     * <p>
     * <strong>Algorithm:</strong>
     * <ol>
     *   <li>Validates input parameters (user input and response must not be null/empty)</li>
     *   <li>Generates N artificial questions from the response using LLM</li>
     *   <li>Checks if all questions indicate noncommittal response (returns 0.0 if so)</li>
     *   <li>Computes embeddings for user input and generated questions</li>
     *   <li>Calculates cosine similarity between user input and each generated question</li>
     *   <li>Returns average similarity as relevancy score</li>
     * </ol>
     * <p>
     * <strong>Edge cases handled:</strong>
     * <ul>
     *   <li>Null or empty user input/response → returns 0.0</li>
     *   <li>No questions generated → returns 0.0</li>
     *   <li>All noncommittal questions → returns 0.0</li>
     *   <li>Embedding failures → skips problematic questions, logs warnings</li>
     * </ul>
     * <p>
     * <strong>⚠️ Important limitations:</strong>
     * <ul>
     *   <li>High scores (0.9+) do not guarantee factual correctness</li>
     *   <li>Partial answers may still receive high scores on some embedding models</li>
     *   <li>Off-topic answers may receive moderate scores due to linguistic similarity</li>
     * </ul>
     *
     * @param config Configuration parameters for the metric (number of questions, etc.)
     * @param sample Sample containing user_input and response to evaluate
     * @return Relevancy score typically in range 0-1, where:
     * <ul>
     *   <li>0.0 = noncommittal answer (reliable)</li>
     *   <li>0.9-1.0 = high relevancy (but may be incorrect facts)</li>
     *   <li>0.3-0.9 = moderate relevancy (requires verification)</li>
     *   <li>0.0-0.3 = low relevancy (likely off-topic)</li>
     * </ul>
     * @throws IllegalArgumentException if config is null
     * @throws NullPointerException     if sample is null
     * @see #singleTurnScoreAsync(ResponseRelevancyConfig, Sample) for asynchronous version
     */
    public Double singleTurnScore(final ResponseRelevancyConfig config, final Sample sample) {
        // Validate required inputs
        String userInput = sample.getUserInput();
        if (userInput == null || userInput.trim().isEmpty()) {
            log.warn("No user input provided for Response Relevancy evaluation");
            return 0.0;
        }

        String response = sample.getResponse();
        if (response == null || response.trim().isEmpty()) {
            log.warn("No response provided for Response Relevancy evaluation");
            return 0.0;
        }

        log.debug("Computing response relevancy with {} generated questions", config.getNumberOfQuestions());

        // Step 1: Generate artificial questions from the response (with user input context)
        GeneratedQuestionsResponse questionsResponse =
                generateQuestionsFromResponse(userInput, response, config.getNumberOfQuestions());

        if (questionsResponse == null
                || questionsResponse.questions() == null
                || questionsResponse.questions().isEmpty()) {
            log.warn("No questions generated from response");
            return 0.0;
        }

        List<GeneratedQuestion> generatedQuestions = questionsResponse.questions();
        log.debug("Generated {} questions from response", generatedQuestions.size());

        // Step 2: Check if all answers are noncommittal
        boolean allNoncommittal = generatedQuestions.stream().allMatch(q -> q.noncommittal() == 1);

        if (allNoncommittal) {
            log.debug("All generated questions indicate noncommittal response - returning score 0.0");
            return 0.0;
        }

        // Step 3: Get embeddings for user input
        double[] userInputEmbedding = getEmbedding(userInput);

        // Step 4: Calculate cosine similarity for each generated question
        double totalSimilarity = 0.0;
        int validQuestions = 0;

        for (GeneratedQuestion genQuestion : generatedQuestions) {
            String question = genQuestion.question();
            if (question == null || question.trim().isEmpty()) {
                continue;
            }

            try {
                double[] questionEmbedding = getEmbedding(question);
                double similarity = cosineSimilarity(userInputEmbedding, questionEmbedding);
                totalSimilarity += similarity;
                validQuestions++;

                log.debug(
                        "Question: '{}' (noncommittal: {}) - Similarity: {}",
                        question.substring(0, Math.min(50, question.length())),
                        genQuestion.noncommittal(),
                        similarity);
            } catch (Exception e) {
                log.warn("Failed to calculate similarity for question: {}", question, e);
            }
        }

        if (validQuestions == 0) {
            log.warn("No valid questions for similarity calculation");
            return 0.0;
        }

        // Step 5: Calculate average similarity as relevancy score
        double relevancyScore = totalSimilarity / validQuestions;
        log.debug("Final relevancy score: {} (from {} questions)", relevancyScore, validQuestions);

        return relevancyScore;
    }

    /**
     * Asynchronous version of {@link #singleTurnScore(ResponseRelevancyConfig, Sample)}.
     * <p>
     * Executes the relevancy calculation in a separate thread using CompletableFuture.
     * Useful for processing multiple samples concurrently or integrating with reactive pipelines.
     * <p>
     * <strong>Usage example:</strong>
     * <pre>{@code
     * CompletableFuture<Double> futureScore = metric.singleTurnScoreAsync(config, sample);
     * futureScore.thenAccept(score -> {
     *     if (score == 0.0) {
     *         handleNoncommittalResponse();
     *     }
     * });
     * }</pre>
     *
     * @param config Configuration parameters for the metric
     * @param sample Sample containing user_input and response to evaluate
     * @return CompletableFuture that will complete with the relevancy score
     * @see #singleTurnScore(ResponseRelevancyConfig, Sample) for synchronous version
     */
    public CompletableFuture<Double> singleTurnScoreAsync(ResponseRelevancyConfig config, Sample sample) {
        return CompletableFuture.supplyAsync(() -> singleTurnScore(config, sample));
    }

    /**
     * Generates artificial questions from response using LLM in a single call.
     * <p>
     * This method uses the configured prompt template to generate multiple questions
     * that the given response could be answering. The questions are generated with
     * context of the original user input to better detect off-topic and incomplete responses.
     * <p>
     * <strong>Key features:</strong>
     * <ul>
     *   <li>Single LLM call generates all N questions for efficiency</li>
     *   <li>Questions reflect ONLY what the response actually addresses</li>
     *   <li>Includes noncommittal detection for each generated question</li>
     *   <li>Handles multilingual scenarios</li>
     * </ul>
     *
     * @param userInput         Original user question for context
     * @param response          System response to generate questions from
     * @param numberOfQuestions Number of questions to generate (typically 3-10)
     * @return GeneratedQuestionsResponse containing list of questions with noncommittal flags,
     * or null if generation failed
     * @see GeneratedQuestionsResponse
     * @see GeneratedQuestion
     */
    private GeneratedQuestionsResponse generateQuestionsFromResponse(
            String userInput, String response, int numberOfQuestions) {
        final Map<String, Object> variables = Map.of(
                "userInput", userInput,
                "response", response,
                "numberOfQuestions", numberOfQuestions);

        try {
            GeneratedQuestionsResponse questionsResponse = chatClient
                    .prompt(PromptTemplate.builder()
                            .template(questionGenerationPrompt)
                            .variables(variables)
                            .build()
                            .create())
                    .call()
                    .entity(GeneratedQuestionsResponse.class);

            if (questionsResponse != null && questionsResponse.questions() != null) {
                log.debug(
                        "Successfully generated {} questions in one LLM call",
                        questionsResponse.questions().size());
                for (int i = 0; i < questionsResponse.questions().size(); i++) {
                    GeneratedQuestion q = questionsResponse.questions().get(i);
                    log.debug(
                            "  Question {}: '{}' (noncommittal: {})",
                            i + 1,
                            q.question().substring(0, Math.min(70, q.question().length())),
                            q.noncommittal());
                }
            }

            return questionsResponse;
        } catch (Exception e) {
            log.error("Failed to generate questions from response", e);
            return null;
        }
    }

    /**
     * Retrieves embedding vector for a text using the configured embedding model.
     * <p>
     * Converts the text into a numerical vector representation that captures
     * semantic meaning. The embedding is used for cosine similarity calculations.
     * <p>
     * <strong>Note:</strong> The method converts float arrays from the embedding model
     * to double arrays for more precise mathematical operations.
     *
     * @param text Input text to embed (user question or generated question)
     * @return Embedding vector as double array
     * @throws IllegalStateException if embedding generation fails or returns empty results
     * @see EmbeddingModel
     */
    private double[] getEmbedding(String text) {
        EmbeddingResponse embeddingResponse = embeddingModel.embedForResponse(List.of(text));

        if (embeddingResponse.getResults().isEmpty()) {
            throw new IllegalStateException("Failed to get embedding for text: " + text);
        }

        float[] floatArray = embeddingResponse.getResults().get(0).getOutput();

        // Convert float[] to double[] for mathematical operations
        double[] doubleArray = new double[floatArray.length];
        for (int i = 0; i < floatArray.length; i++) {
            doubleArray[i] = floatArray[i];
        }

        return doubleArray;
    }

    /**
     * Calculates cosine similarity between two embedding vectors.
     * <p>
     * Cosine similarity measures the cosine of the angle between two vectors,
     * providing a measure of how similar they are irrespective of their magnitude.
     * <p>
     * <strong>Mathematical formula:</strong>
     * <pre>
     * cos(θ) = (A · B) / (||A|| × ||B||)
     *
     * Where:
     * - A · B is the dot product of vectors A and B
     * - ||A|| and ||B|| are the Euclidean norms of the vectors
     * </pre>
     * <p>
     * <strong>Score interpretation:</strong>
     * <ul>
     *   <li>1.0 = vectors point in same direction (identical semantic meaning)</li>
     *   <li>0.0 = vectors are orthogonal (unrelated meanings)</li>
     *   <li>-1.0 = vectors point in opposite directions (opposite meanings)</li>
     * </ul>
     * <p>
     * In practice, text embeddings rarely produce negative similarities.
     *
     * @param vectorA First embedding vector
     * @param vectorB Second embedding vector
     * @return Cosine similarity score in range [-1, 1], typically [0, 1] for text embeddings
     * @throws IllegalArgumentException if vectors have different dimensions
     */
    private double cosineSimilarity(double[] vectorA, double[] vectorB) {
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
     * Response DTO for multiple generated questions returned by LLM.
     * <p>
     * Contains a list of questions that the evaluated response could be answering,
     * along with flags indicating whether each question represents a noncommittal answer.
     *
     * @param questions List of generated questions with noncommittal flags
     * @see GeneratedQuestion
     */
    public record GeneratedQuestionsResponse(
            @JsonPropertyDescription("Array of generated questions with noncommittal flags")
                    List<GeneratedQuestion> questions) {}

    /**
     * Response DTO for a single generated question with metadata.
     * <p>
     * Represents one artificial question that could have led to the evaluated response,
     * along with a flag indicating whether the answer to this question is noncommittal.
     *
     * @param question     The generated question string in natural language
     * @param noncommittal Integer flag: 1 if the answer is noncommittal (evasive, vague), 0 if committal
     * @see GeneratedQuestionsResponse
     */
    public record GeneratedQuestion(
            @JsonPropertyDescription("The generated question string") String question,
            @JsonPropertyDescription("1 if the answer is noncommittal (evasive, vague), 0 if committal")
                    Integer noncommittal) {}

    /**
     * Configuration class for Response Relevancy metric parameters.
     * <p>
     * Allows customization of the metric behavior, primarily the number of artificial
     * questions generated for relevancy evaluation.
     * <p>
     * <strong>Performance considerations:</strong>
     * <ul>
     *   <li><strong>3 questions (default):</strong> Good balance of accuracy and speed</li>
     *   <li><strong>5-7 questions:</strong> More stable results, slightly slower</li>
     *   <li><strong>10+ questions:</strong> Most stable, but significantly slower</li>
     * </ul>
     * <p>
     * <strong>Usage examples:</strong>
     * <pre>{@code
     * // Default configuration
     * ResponseRelevancyConfig config = ResponseRelevancyConfig.defaultConfig();
     *
     * // Custom configuration for more stable results
     * ResponseRelevancyConfig customConfig = ResponseRelevancyConfig.builder()
     *     .numberOfQuestions(5)
     *     .build();
     * }</pre>
     */
    @Data
    @Builder
    public static class ResponseRelevancyConfig implements MetricConfiguration {
        /**
         * Number of artificial questions to generate from the response for similarity comparison.
         * <p>
         * <strong>Default:</strong> 3 (as per Ragas implementation)
         * <p>
         * <strong>Range:</strong> 1-10 (higher values provide more stable results but increase latency)
         * <p>
         * <strong>Recommendations:</strong>
         * <ul>
         *   <li>Use 3 for most cases (good balance)</li>
         *   <li>Use 5-7 for critical applications requiring stability</li>
         *   <li>Use 1-2 only for high-throughput scenarios where speed is critical</li>
         * </ul>
         */
        @Builder.Default
        private int numberOfQuestions = 3;

        /**
         * Creates a default configuration instance.
         * <p>
         * Equivalent to {@code ResponseRelevancyConfig.builder().build()}.
         *
         * @return Default configuration with 3 questions
         */
        public static ResponseRelevancyConfig defaultConfig() {
            return ResponseRelevancyConfig.builder().build();
        }
    }
}
