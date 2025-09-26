package ai.qa.solutions.metrics.general;

import ai.qa.solutions.sample.Sample;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;

/**
 * AspectCritic Metric - Binary evaluation based on predefined aspects
 * Updated to use structured output with inner DTO
 */
public class AspectCriticMetric {
    private final ChatClient chatClient;
    private final String promptTemplate;

    public AspectCriticMetric(final ChatClient chatClient) {
        this.chatClient = chatClient;
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

    public Double singleTurnScore(final AspectCriticConfig config, final Sample sample) {
        return chatClient
                .prompt(PromptTemplate.builder()
                        .template(this.promptTemplate)
                        .variables(Map.of(
                                "definition",
                                config.definition,
                                "strictness",
                                config.strictness,
                                "user_input",
                                sample.getUserInput(),
                                "response",
                                sample.getResponse()))
                        .build()
                        .create())
                .call()
                .entity(Response.class)
                .getScore();
    }

    public CompletableFuture<Double> singleTurnScoreAsync(AspectCriticConfig config, Sample sample) {
        return CompletableFuture.supplyAsync(() -> singleTurnScore(config, sample));
    }

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

    @Data
    @Builder
    public static class AspectCriticConfig {
        @NonNull
        private String definition;

        @Builder.Default
        private Integer strictness = 3;

        @SuppressWarnings("unused")
        public void setStrictness(final int strictness) {
            if (strictness < 1 || strictness > 5) {
                throw new IllegalArgumentException("Strictness must be between 1 and 5");
            }
            this.strictness = strictness;
        }
    }
}
