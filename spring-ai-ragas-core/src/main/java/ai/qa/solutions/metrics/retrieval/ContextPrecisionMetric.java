package ai.qa.solutions.metrics.retrieval;

import ai.qa.solutions.sample.Sample;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;

/**
 * Context Precision Metric - LLM-based evaluation of retriever's ability to rank relevant chunks higher
 * Automatically chooses between reference-based or response-based evaluation based on available data
 */
@Slf4j
public class ContextPrecisionMetric {
    private final ChatClient chatClient;
    private final String withReferencePrompt;
    private final String withoutReferencePrompt;

    public ContextPrecisionMetric(final ChatClient chatClient) {
        this.chatClient = chatClient;
        this.withReferencePrompt =
                """
                Given a user query, reference answer, and a retrieved context chunk, determine if the context chunk is relevant to answering the user query based on the reference answer.

                User Query: {user_input}
                Reference Answer: {reference}
                Retrieved Context Chunk: {context_chunk}

                Instructions:
                1. Analyze if the context chunk contains information that is relevant to providing the reference answer
                2. Use the reference answer as the gold standard for what constitutes a complete and correct response
                3. A chunk is relevant if it contains information that supports or contributes to the reference answer
                4. Be strict in your evaluation - only mark as relevant if the chunk genuinely helps answer the query as indicated by the reference

                Respond with a JSON object containing:
                - relevant: true if the context chunk is relevant to answering the user query based on the reference, false otherwise
                - reasoning: Your detailed explanation for why the chunk is or isn't relevant
                """;

        this.withoutReferencePrompt =
                """
                Given a user query, AI response, and a retrieved context chunk, determine if the context chunk is relevant to answering the user query based on the AI response.

                User Query: {user_input}
                AI Response: {response}
                Retrieved Context Chunk: {context_chunk}

                Instructions:
                1. Analyze if the context chunk contains information that is relevant to answering the user query
                2. Consider the AI response as guidance for what constitutes a relevant answer
                3. A chunk is relevant if it contains information that helps answer the query, even if not directly used in the response
                4. Be strict in your evaluation - only mark as relevant if the chunk genuinely contributes to answering the query

                Respond with a JSON object containing:
                - relevant: true if the context chunk is relevant to answering the user query, false otherwise
                - reasoning: Your detailed explanation for why the chunk is or isn't relevant
                """;
    }

    public Double singleTurnScore(final ContextPrecisionConfig config, final Sample sample) {
        List<String> retrievedContexts = sample.getRetrievedContexts();
        if (retrievedContexts == null || retrievedContexts.isEmpty()) {
            log.warn("No retrieved contexts provided for Context Precision evaluation");
            return 0.0;
        }

        // Determine strategy based on config preference and data availability
        EvaluationStrategy strategy = determineEvaluationStrategy(config, sample);

        log.debug(
                "Using LLM {}-based context precision evaluation",
                strategy == EvaluationStrategy.REFERENCE_BASED ? "reference" : "response");

        List<Boolean> relevanceScores = sample.getRetrievedContexts().stream()
                .map(context -> isContextRelevant(strategy, sample, context))
                .toList();

        return calculateContextPrecision(relevanceScores);
    }

    public CompletableFuture<Double> singleTurnScoreAsync(ContextPrecisionConfig config, Sample sample) {
        return CompletableFuture.supplyAsync(() -> singleTurnScore(config, sample));
    }

    private EvaluationStrategy determineEvaluationStrategy(ContextPrecisionConfig config, Sample sample) {
        if (config.getEvaluationStrategy() == null) {
            // Auto-detect based on available data
            boolean hasReference = sample.getReference() != null
                    && !sample.getReference().trim().isEmpty();
            return hasReference ? EvaluationStrategy.REFERENCE_BASED : EvaluationStrategy.RESPONSE_BASED;
        }

        // Validate that the required data is available for the chosen strategy
        if (config.getEvaluationStrategy() == EvaluationStrategy.REFERENCE_BASED) {
            if (sample.getReference() == null || sample.getReference().trim().isEmpty()) {
                log.warn(
                        "Reference-based evaluation requested but no reference provided, falling back to response-based");
                return EvaluationStrategy.RESPONSE_BASED;
            }
            return EvaluationStrategy.REFERENCE_BASED;
        }

        return EvaluationStrategy.RESPONSE_BASED;
    }

    private Boolean isContextRelevant(EvaluationStrategy strategy, Sample sample, String contextChunk) {
        final String template =
                strategy == EvaluationStrategy.REFERENCE_BASED ? this.withReferencePrompt : this.withoutReferencePrompt;

        final Map<String, Object> variables = strategy == EvaluationStrategy.REFERENCE_BASED
                ? Map.of(
                        "user_input", sample.getUserInput(),
                        "reference", sample.getReference(),
                        "context_chunk", contextChunk)
                : Map.of(
                        "user_input", sample.getUserInput(),
                        "response", sample.getResponse(),
                        "context_chunk", contextChunk);

        return chatClient
                .prompt(PromptTemplate.builder()
                        .template(template)
                        .variables(variables)
                        .build()
                        .create())
                .call()
                .entity(RelevanceResponse.class)
                .relevant();
    }

    private Double calculateContextPrecision(List<Boolean> relevanceScores) {
        if (relevanceScores.isEmpty()) {
            return 0.0;
        }
        double sum = IntStream.range(0, relevanceScores.size())
                .mapToDouble(k -> {
                    // Calculate precision@k (relevant items up to position k / total items up to position k)
                    long relevantUpToK = relevanceScores.subList(0, k + 1).stream()
                            .mapToInt(relevant -> relevant ? 1 : 0)
                            .sum();
                    return (double) relevantUpToK / (k + 1);
                })
                .sum();
        return sum / relevanceScores.size();
    }

    /**
     * Response DTO for LLM-based relevance evaluation
     */
    public record RelevanceResponse(
            @JsonPropertyDescription("Boolean indicating if the context chunk is relevant to answering the user query")
                    Boolean relevant,
            @JsonPropertyDescription("Detailed explanation of why the chunk is or isn't relevant") String reasoning) {}

    /**
     * Evaluation strategy enum
     */
    public enum EvaluationStrategy {
        REFERENCE_BASED, // Use reference answer for evaluation (preferred when available)
        RESPONSE_BASED // Use AI response for evaluation
    }

    @Data
    @Builder
    public static class ContextPrecisionConfig {
        /**
         * Evaluation strategy for LLM-based evaluation
         * If null, will auto-detect based on available data (reference preferred over response)
         */
        private EvaluationStrategy evaluationStrategy;
    }
}
