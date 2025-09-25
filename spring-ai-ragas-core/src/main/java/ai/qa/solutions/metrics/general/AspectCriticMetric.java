package ai.qa.solutions.metrics.general;

import ai.qa.solutions.metric.AbstractLLMMetric;
import ai.qa.solutions.metric.MetricOutputType;
import ai.qa.solutions.sample.SingleTurnSample;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.springframework.ai.chat.client.ChatClient;

/**
 * AspectCritic Metric - Binary evaluation based on predefined aspects
 * Updated to use structured output with inner DTO
 */
public class AspectCriticMetric extends AbstractLLMMetric {
    private String definition;
    private int strictness = 3; // Default strictness level
    private final ChatClient chatClient;

    /**
     * Response DTO for AspectCritic metric evaluation
     */
    public record Response(
            @JsonPropertyDescription("The specific evaluation criteria that was applied to assess the response")
                    String criteria,
            @JsonPropertyDescription("Boolean verdict: true if the response meets the criteria, false otherwise")
                    Boolean verdict,
            @JsonPropertyDescription("Detailed explanation and justification for the verdict decision")
                    String reasoning) {
        public Double getScore() {
            return verdict != null && verdict ? 1.0 : 0.0;
        }
    }

    public AspectCriticMetric(final ChatClient chatClient) {
        super("aspect_critic", MetricOutputType.BINARY, Set.of("user_input", "response"));
        this.chatClient = chatClient;
        initializePromptTemplate();
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public void setStrictness(int strictness) {
        if (strictness < 1 || strictness > 5) {
            throw new IllegalArgumentException("Strictness must be between 1 and 5");
        }
        this.strictness = strictness;
    }

    private void initializePromptTemplate() {
        this.promptTemplate =
                """
            Given a user input and an AI response, evaluate whether the response meets the specified criteria.

            Criteria: {definition}

            User Input: {user_input}

            AI Response: {response}

            Instructions:
            1. Carefully analyze the AI response against the given criteria
            2. Consider the context provided by the user input
            3. Apply a strictness level of {strictness} (1=lenient, 5=very strict)
            4. Provide your evaluation with the criteria, verdict (true/false), and detailed reasoning

            Respond with a JSON object containing:
            - criteria: The evaluation criteria being applied
            - verdict: true if the response meets the criteria, false otherwise
            - reasoning: Your detailed explanation for the verdict
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
                .replace("{strictness}", String.valueOf(strictness));
    }

    @Override
    public Double singleTurnScore(SingleTurnSample sample) {
        validateSample(sample);
        String prompt = buildPrompt(sample);
        return chatClient.prompt(prompt).call().entity(Response.class).getScore();
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
