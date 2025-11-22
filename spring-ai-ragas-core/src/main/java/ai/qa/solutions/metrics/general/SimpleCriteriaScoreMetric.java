package ai.qa.solutions.metrics.general;

import ai.qa.solutions.metric.Metric;
import ai.qa.solutions.sample.Sample;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;

/**
 * SimpleCriteriaScore Metric - Continuous scoring based on simple criteria
 * Updated to use structured output with inner DTO and stateless design
 */
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class SimpleCriteriaScoreMetric implements Metric<SimpleCriteriaScoreMetric.SimpleCriteriaConfig> {
    public static final String DEFAULT_PROMPT_TEMPLATE =
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

            Respond with a JSON object containing:
            - criteria: The evaluation criteria being applied
            - score: A numerical score between {min_score} and {max_score}
            - reasoning: Your detailed explanation for the score
            """;

    @NonNull
    private final ChatClient chatClient;

    @NonNull
    @Builder.Default
    private final String promptTemplate = DEFAULT_PROMPT_TEMPLATE;

    public Double singleTurnScore(final SimpleCriteriaConfig config, final Sample sample) {
        return chatClient
                .prompt(PromptTemplate.builder()
                        .template(this.promptTemplate)
                        .variables(Map.of(
                                "definition", config.definition,
                                "min_score", config.minScore.toString(),
                                "max_score", config.maxScore.toString(),
                                "user_input", sample.getUserInput(),
                                "response", sample.getResponse(),
                                "reference", sample.getReference() != null ? sample.getReference() : ""))
                        .build()
                        .create())
                .call()
                .entity(Response.class)
                .getNormalizedScore();
    }

    public CompletableFuture<Double> singleTurnScoreAsync(SimpleCriteriaConfig config, Sample sample) {
        return CompletableFuture.supplyAsync(() -> singleTurnScore(config, sample));
    }

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

    @Data
    @Builder
    public static class SimpleCriteriaConfig implements MetricConfiguration {
        @NonNull
        private String definition;

        @Builder.Default
        private Double minScore = 0.0;

        @Builder.Default
        private Double maxScore = 5.0;

        @SuppressWarnings("unused")
        public void setScoreRange(double minScore, double maxScore) {
            if (minScore >= maxScore) {
                throw new IllegalArgumentException("minScore must be less than maxScore");
            }
            this.minScore = minScore;
            this.maxScore = maxScore;
        }
    }
}
