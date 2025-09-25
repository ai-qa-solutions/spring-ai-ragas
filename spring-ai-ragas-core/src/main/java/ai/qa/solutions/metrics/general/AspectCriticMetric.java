package ai.qa.solutions.metrics.general;

import ai.qa.solutions.llm.LLMEvaluationService;
import ai.qa.solutions.metric.AbstractLLMMetric;
import ai.qa.solutions.metric.MetricOutputType;
import ai.qa.solutions.sample.SingleTurnSample;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Set;

/**
 * AspectCritic Metric - Binary evaluation based on predefined aspects
 * Based on Ragas AspectCritic implementation
 */
import ai.qa.solutions.llm.LLMEvaluationService;
import ai.qa.solutions.metric.AbstractLLMMetric;
import ai.qa.solutions.metric.MetricOutputType;
import ai.qa.solutions.sample.SingleTurnSample;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * AspectCritic Metric - Binary evaluation based on predefined aspects
 * Updated to use structured output with inner DTO
 */
public class AspectCriticMetric extends AbstractLLMMetric {
    private String definition;
    private int strictness = 3; // Default strictness level

    /**
     * Response DTO for AspectCritic metric evaluation
     */
    public static record Response(
            @JsonProperty("criteria")
            @JsonPropertyDescription("The specific evaluation criteria that was applied to assess the response")
            String criteria,

            @JsonProperty("verdict")
            @JsonPropertyDescription("Boolean verdict: true if the response meets the criteria, false otherwise")
            Boolean verdict,

            @JsonProperty("reasoning")
            @JsonPropertyDescription("Detailed explanation and justification for the verdict decision")
            String reasoning
    ) {
        public Double getScore() {
            return verdict != null && verdict ? 1.0 : 0.0;
        }
    }

    public AspectCriticMetric() {
        super("aspect_critic", MetricOutputType.BINARY, Set.of("user_input", "response"));
        initializePromptTemplate();
    }

    public AspectCriticMetric(String name, String definition, LLMEvaluationService llmService) {
        super(name, MetricOutputType.BINARY, Set.of("user_input", "response"));
        this.definition = definition;
        this.llmService = llmService;
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
        this.promptTemplate = """
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
        return response.getScore();
    }

    @Override
    public CompletableFuture<Double> singleTurnScoreAsync(SingleTurnSample sample) {
        validateSample(sample);
        String prompt = buildPrompt(sample);
        return llmService.evaluateWithStructuredOutputAsync(prompt, Response.class)
                .thenApply(Response::getScore);
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