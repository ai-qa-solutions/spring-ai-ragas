package ai.qa.solutions.metrics.general;

import ai.qa.solutions.metric.AbstractLLMMetric;
import ai.qa.solutions.metric.MetricOutputType;
import ai.qa.solutions.sample.SingleTurnSample;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.springframework.ai.chat.client.ChatClient;

/**
 * SimpleCriteriaScore Metric - Continuous scoring based on simple criteria
 * Updated to use structured output with inner DTO
 */
public class SimpleCriteriaScoreMetric extends AbstractLLMMetric {
    private final ChatClient chatClient;
    private String definition;
    private double minScore = 0.0;
    private double maxScore = 5.0;

    /**
     * Response DTO for SimpleCriteriaScore metric evaluation
     */
    public record Response(
            @JsonPropertyDescription("The evaluation criteria that was used to score the response") String criteria,
            @JsonPropertyDescription(
                            "Numerical score within the specified range (e.g., 0-5) based on how well the response meets the criteria")
                    Double score,
            @JsonPropertyDescription(
                            "Detailed explanation of why this specific score was assigned, including analysis of strengths and weaknesses")
                    String reasoning) {
        public Double getNormalizedScore() {
            return score != null ? score : 0.0;
        }
    }

    public SimpleCriteriaScoreMetric(final ChatClient chatClient) {
        super("simple_criteria_score", MetricOutputType.DISCRETE, Set.of("user_input", "response", "reference"));
        this.chatClient = chatClient;
        initializePromptTemplate();
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public void setScoreRange(double minScore, double maxScore) {
        this.minScore = minScore;
        this.maxScore = maxScore;
    }

    private void initializePromptTemplate() {
        this.promptTemplate =
                """
            Evaluate the AI response based on the given criteria and score it accordingly.

            Evaluation Criteria: {definition}

            User Input: {user_input}
            AI Response: {response}
            Reference Answer: {reference}

            Instructions:
            1. Compare the AI response with the reference answer
            2. Evaluate based on the specified criteria: {definition}
            3. Provide a score between {min_score} and {max_score}
            4. Higher scores indicate better alignment with the criteria
            5. Provide detailed reasoning for your score
            """;
    }

    @Override
    protected String buildPrompt(SingleTurnSample sample) {
        if (definition == null || definition.trim().isEmpty()) {
            throw new IllegalStateException("Definition must be set before scoring");
        }

        return promptTemplate
                .replace("{definition}", definition)
                .replace("{user_input}", sample.getUserInput())
                .replace("{response}", sample.getResponse())
                .replace("{reference}", sample.getReference() != null ? sample.getReference() : "")
                .replace("{min_score}", String.valueOf(minScore))
                .replace("{max_score}", String.valueOf(maxScore));
    }

    @Override
    public Double singleTurnScore(SingleTurnSample sample) {
        validateSample(sample);
        String prompt = buildPrompt(sample);
        return chatClient.prompt(prompt).call().entity(Response.class).getNormalizedScore();
    }

    @Override
    public CompletableFuture<Double> singleTurnScoreAsync(SingleTurnSample sample) {
        return CompletableFuture.supplyAsync(() -> singleTurnScore(sample));
    }

    /**
     * Get detailed evaluation response with reasoning
     */
    public Response getDetailedResponse(SingleTurnSample sample) {
        validateSample(sample);
        String prompt = buildPrompt(sample);
        return chatClient.prompt(prompt).call().entity(Response.class);
    }

    /**
     * Get detailed evaluation response asynchronously
     */
    public CompletableFuture<Response> getDetailedResponseAsync(SingleTurnSample sample) {
        return CompletableFuture.supplyAsync(() -> getDetailedResponse(sample));
    }
}
