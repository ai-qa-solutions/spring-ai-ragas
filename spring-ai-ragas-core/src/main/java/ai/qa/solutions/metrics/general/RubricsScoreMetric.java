package ai.qa.solutions.metrics.general;

import ai.qa.solutions.sample.Sample;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Singular;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;

/**
 * RubricsScore Metric - Detailed rubric-based evaluation
 * Updated to use structured output with inner DTO and stateless design
 */
public class RubricsScoreMetric {
    private final ChatClient chatClient;
    private final String promptTemplate;

    public RubricsScoreMetric(final ChatClient chatClient) {
        this.chatClient = chatClient;
        this.promptTemplate =
                """
                Evaluate the AI response using the provided detailed rubrics.

                User Input: {user_input}
                AI Response: {response}
                Reference Answer: {reference}

                Evaluation Rubrics:
                {rubrics}

                Instructions:
                1. Compare the AI response with the reference answer
                2. Evaluate the response against each rubric level
                3. Select the rubric level that best describes the response quality
                4. Provide the corresponding score and detailed reasoning

                Respond with a JSON object containing:
                - score: The numerical score (integer) corresponding to the selected rubric level
                - rubric_level: The key of the selected rubric (e.g., "score3_description")
                - reasoning: Your detailed explanation for the score selection
                """;
    }

    public Double singleTurnScore(final RubricsConfig config, final Sample sample) {
        return chatClient
                .prompt(PromptTemplate.builder()
                        .template(this.promptTemplate)
                        .variables(Map.of(
                                "user_input", sample.getUserInput(),
                                "response", sample.getResponse(),
                                "reference", sample.getReference() != null ? sample.getReference() : "",
                                "rubrics", buildRubricsText(config.rubrics)))
                        .build()
                        .create())
                .call()
                .entity(Response.class)
                .getNormalizedScore();
    }

    public CompletableFuture<Double> singleTurnScoreAsync(RubricsConfig config, Sample sample) {
        return CompletableFuture.supplyAsync(() -> singleTurnScore(config, sample));
    }

    private String buildRubricsText(Map<String, String> rubrics) {
        if (rubrics == null || rubrics.isEmpty()) {
            throw new IllegalStateException("Rubrics must be provided");
        }

        StringBuilder rubricsText = new StringBuilder();
        rubrics.entrySet().stream()
                .sorted(Map.Entry.<String, String>comparingByKey())
                .forEach(entry -> {
                    String scoreKey = entry.getKey(); // e.g., "score1_description"
                    String score = scoreKey.replaceAll("[^0-9]", ""); // Extract number
                    rubricsText
                            .append("Score ")
                            .append(score)
                            .append(": ")
                            .append(entry.getValue())
                            .append("\n");
                });
        return rubricsText.toString();
    }

    /**
     * Response DTO for RubricsScore metric evaluation
     */
    public record Response(
            @JsonPropertyDescription(
                            "Integer score (1-5) corresponding to the selected rubric level that best matches the response quality")
                    Integer score,
            @JsonPropertyDescription(
                            "The key identifier of the selected rubric level (e.g., 'score3_description') that was used for scoring")
                    String rubric_level,
            @JsonPropertyDescription(
                            "Comprehensive explanation of why this rubric level was selected, including specific evidence from the response that supports the score")
                    String reasoning) {
        public Double getNormalizedScore() {
            return score != null ? score.doubleValue() : 0.0;
        }
    }

    @Data
    @Builder
    public static class RubricsConfig {
        @NonNull
        @Singular
        private Map<String, String> rubrics;

        @SuppressWarnings("unused")
        public void validateRubrics() {
            if (rubrics.isEmpty()) {
                throw new IllegalArgumentException("Rubrics cannot be null or empty");
            }
            // Validate that rubrics follow expected pattern (scoreN_description)
            boolean hasValidKeys = rubrics.keySet().stream().anyMatch(key -> key.matches("score\\d+_description"));

            if (!hasValidKeys) {
                throw new IllegalArgumentException("Rubrics must contain keys in format 'scoreN_description'");
            }
        }
    }
}
