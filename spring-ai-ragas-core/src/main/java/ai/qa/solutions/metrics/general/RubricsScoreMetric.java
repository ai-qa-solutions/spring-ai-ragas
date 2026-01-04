package ai.qa.solutions.metrics.general;

import ai.qa.solutions.execution.MultiModelExecutor;
import ai.qa.solutions.execution.MultiModelExecutor.ExecutionRequest;
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
import lombok.NonNull;
import lombok.Singular;
import org.springframework.ai.chat.prompt.PromptTemplate;

/**
 * RubricsScore Metric - Detailed rubric-based evaluation.
 * <p>
 * Uses {@link MultiModelExecutor} for parallel execution across multiple models
 * with support for listeners and custom aggregation strategies.
 */
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class RubricsScoreMetric implements Metric<RubricsScoreMetric.RubricsConfig> {
    public static final String DEFAULT_PROMPT_TEMPLATE =
            """
            Evaluate the AI response using the provided detailed rubrics.

            User Input: {user_input}
            AI Response: {response}
            {reference}

            Evaluation Rubrics:
            {rubrics}

            Instructions:
            1. Evaluate the AI response against each rubric level based on its quality
            2. Select the rubric level that best describes the response quality
            3. Provide the corresponding score and detailed reasoning

            Respond with a JSON object containing:
            - score: The numerical score (integer) corresponding to the selected rubric level
            - rubric_level: The key of the selected rubric (e.g., "score3_description")
            - reasoning: Your detailed explanation for the score selection
            """;

    @Builder.Default
    private final String promptTemplate = DEFAULT_PROMPT_TEMPLATE;

    private final MultiModelExecutor executor;

    @Override
    public Double singleTurnScore(final RubricsConfig config, final Sample sample) {
        return singleTurnScoreAsync(config, sample).join();
    }

    @Override
    public CompletableFuture<Double> singleTurnScoreAsync(final RubricsConfig config, final Sample sample) {
        final String prompt = renderPrompt(config, sample);

        return executor.execute(ExecutionRequest.<Response>builder()
                .metricName(getName())
                .prompt(prompt)
                .responseType(Response.class)
                .scoreExtractor(Response::getNormalizedScore)
                .modelIds(config.models != null ? config.models : List.of())
                .metadata(Map.of("sample", sample, "config", config))
                .build());
    }

    private String renderPrompt(final RubricsConfig config, final Sample sample) {
        String referenceText = "";
        if (sample.getReference() != null && !sample.getReference().isEmpty()) {
            referenceText = "Reference Context: " + sample.getReference();
        }

        return PromptTemplate.builder()
                .template(this.promptTemplate)
                .variables(Map.of(
                        "user_input",
                        sample.getUserInput(),
                        "response",
                        sample.getResponse(),
                        "reference",
                        referenceText,
                        "rubrics",
                        buildRubricsText(config.rubrics)))
                .build()
                .render();
    }

    private String buildRubricsText(Map<String, String> rubrics) {
        if (rubrics == null || rubrics.isEmpty()) {
            throw new IllegalStateException("Rubrics must be provided");
        }

        final StringBuilder rubricsText = new StringBuilder();
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
    public static class RubricsConfig implements MetricConfiguration {
        @Singular
        private List<String> models;

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
