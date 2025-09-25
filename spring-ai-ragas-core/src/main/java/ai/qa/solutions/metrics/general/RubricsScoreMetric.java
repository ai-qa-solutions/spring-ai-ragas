package ai.qa.solutions.metrics.general;

import ai.qa.solutions.llm.LLMEvaluationService;
import ai.qa.solutions.metric.AbstractLLMMetric;
import ai.qa.solutions.metric.MetricOutputType;
import ai.qa.solutions.sample.SingleTurnSample;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * RubricsScore Metric - Detailed rubric-based evaluation
 * Updated to use structured output with inner DTO
 */
public class RubricsScoreMetric extends AbstractLLMMetric {
    private Map<String, String> rubrics;

    /**
     * Response DTO for RubricsScore metric evaluation
     */
    public static record Response(
            @JsonPropertyDescription("Integer score (1-5) corresponding to the selected rubric level that best matches the response quality")
            Integer score,
            @JsonPropertyDescription("The key identifier of the selected rubric level (e.g., 'score3_description') that was used for scoring")
            String rubric_level,
            @JsonPropertyDescription("Comprehensive explanation of why this rubric level was selected, including specific evidence from the response that supports the score")
            String reasoning
    ) {
        public Double getNormalizedScore() {
            return score != null ? score.doubleValue() : 0.0;
        }
    }

    public RubricsScoreMetric() {
        super("rubrics_score", MetricOutputType.DISCRETE,
                Set.of("user_input", "response", "reference"));
        initializePromptTemplate();
    }

    public RubricsScoreMetric(String name, Map<String, String> rubrics, LLMEvaluationService llmService) {
        super(name, MetricOutputType.DISCRETE, Set.of("user_input", "response", "reference"));
        this.rubrics = rubrics;
        this.llmService = llmService;
        initializePromptTemplate();
    }

    public void setRubrics(Map<String, String> rubrics) {
        this.rubrics = rubrics;
    }

    private void initializePromptTemplate() {
        this.promptTemplate = """
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
            """;
    }

    @Override
    protected String buildPrompt(SingleTurnSample sample) {
        if (rubrics == null || rubrics.isEmpty()) {
            throw new IllegalStateException("Rubrics must be set before scoring");
        }

        StringBuilder rubricsText = new StringBuilder();
        rubrics.entrySet().stream()
                .sorted(Map.Entry.<String, String>comparingByKey())
                .forEach(entry -> {
                    String scoreKey = entry.getKey(); // e.g., "score1_description"
                    String score = scoreKey.replaceAll("[^0-9]", ""); // Extract number
                    rubricsText.append("Score ").append(score).append(": ")
                            .append(entry.getValue()).append("\n");
                });

        return promptTemplate
                .replace("{user_input}", sample.getUserInput())
                .replace("{response}", sample.getResponse())
                .replace("{reference}", sample.getReference() != null ? sample.getReference() : "")
                .replace("{rubrics}", rubricsText.toString());
    }

    @Override
    protected Double parseScore(String llmResponse) {
        // This method is now deprecated in favor of structured output
        throw new UnsupportedOperationException(
                "Use singleTurnScore() method instead, which uses structured output"
        );
    }

    @Override
    public Double singleTurnScore(SingleTurnSample sample) {
        validateSample(sample);
        String prompt = buildPrompt(sample);
        Response response = llmService.evaluateWithStructuredOutput(prompt, Response.class);
        return response.getNormalizedScore();
    }

    @Override
    public CompletableFuture<Double> singleTurnScoreAsync(SingleTurnSample sample) {
        validateSample(sample);
        String prompt = buildPrompt(sample);
        return llmService.evaluateWithStructuredOutputAsync(prompt, Response.class)
                .thenApply(Response::getNormalizedScore);
    }

    /**
     * Get detailed evaluation response with reasoning
     */
    public Response getDetailedResponse(SingleTurnSample sample) {
        validateSample(sample);
        String prompt = buildPrompt(sample);
        return llmService.evaluateWithStructuredOutput(prompt, Response.class);
    }

    /**
     * Get detailed evaluation response asynchronously
     */
    public CompletableFuture<Response> getDetailedResponseAsync(SingleTurnSample sample) {
        validateSample(sample);
        String prompt = buildPrompt(sample);
        return llmService.evaluateWithStructuredOutputAsync(prompt, Response.class);
    }
}