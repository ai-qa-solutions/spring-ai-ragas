package ai.qa.solutions.metrics.retrieval;

import ai.qa.solutions.sample.Sample;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;

/**
 * Response Relevancy Metric - measures how relevant a response is to the user input
 * <p>
 * Higher scores indicate better alignment with the user input, while lower scores are given
 * if the response is incomplete or includes redundant information.
 * <p>
 * This metric works by:
 * 1. Generating multiple artificial questions based on the response using LLM (with context of original user question)
 * 2. Computing cosine similarity between embeddings of the user input and generated questions
 * 3. Averaging the similarity scores to get the final relevancy score
 * <p>
 * Key improvements:
 * - All questions generated in one LLM call to ensure diversity
 * - Generated questions reflect ONLY what the response addresses
 * - Off-topic or incomplete responses generate questions that differ from the original user input
 * <p>
 * Score range: typically 0-1, though mathematically can be -1 to 1 due to cosine similarity
 */
@Slf4j
public class ResponseRelevancyMetric {
    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;
    private final String questionGenerationPrompt;

    public ResponseRelevancyMetric(final ChatClient chatClient, final EmbeddingModel embeddingModel) {
        this.chatClient = chatClient;
        this.embeddingModel = embeddingModel;
        this.questionGenerationPrompt =
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
    }

    /**
     * Calculate response relevancy score for a single sample
     *
     * @param config Configuration for the metric
     * @param sample Sample containing user_input and response
     * @return Relevancy score (typically 0-1, higher is better)
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
     * Asynchronous version of singleTurnScore
     */
    public CompletableFuture<Double> singleTurnScoreAsync(ResponseRelevancyConfig config, Sample sample) {
        return CompletableFuture.supplyAsync(() -> singleTurnScore(config, sample));
    }

    /**
     * Generate questions from response using LLM (single call for all N questions)
     * Includes user input context to better detect off-topic and incomplete responses
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
     * Get embedding vector for a text using the embedding model
     * Returns as double[] for mathematical operations
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
     * Calculate cosine similarity between two vectors
     * <p>
     * Formula: cos(θ) = (A · B) / (||A|| * ||B||)
     *
     * @param vectorA First vector
     * @param vectorB Second vector
     * @return Cosine similarity score (-1 to 1, typically 0 to 1 for text embeddings)
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
     * Response DTO for multiple generated questions
     */
    public record GeneratedQuestionsResponse(
            @JsonPropertyDescription("Array of generated questions with noncommittal flags")
                    List<GeneratedQuestion> questions) {}

    /**
     * Response DTO for a single generated question with noncommittal flag
     */
    public record GeneratedQuestion(
            @JsonPropertyDescription("The generated question string") String question,
            @JsonPropertyDescription("1 if the answer is noncommittal (evasive, vague), 0 if committal")
                    Integer noncommittal) {}

    /**
     * Configuration for Response Relevancy metric
     */
    @Data
    @Builder
    public static class ResponseRelevancyConfig {
        /**
         * Number of questions to generate from the response for similarity comparison
         * Default: 3 (as per Ragas implementation)
         */
        @Builder.Default
        private int numberOfQuestions = 3;

        /**
         * Create default configuration
         */
        public static ResponseRelevancyConfig defaultConfig() {
            return ResponseRelevancyConfig.builder().build();
        }
    }
}
