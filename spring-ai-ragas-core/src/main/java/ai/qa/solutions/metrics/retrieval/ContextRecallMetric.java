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

/**
 * Context Recall Metric - LLM-based evaluation of retriever's ability to retrieve all relevant information
 * Measures how many statements in the reference answer can be attributed to the retrieved contexts
 */
@Slf4j
public class ContextRecallMetric {
    private final ChatClient chatClient;
    private final String contextRecallPrompt;

    public ContextRecallMetric(final ChatClient chatClient) {
        this.chatClient = chatClient;
        this.contextRecallPrompt =
                """
                        Given a question, context, and a reference answer, analyze each sentence in the reference answer and classify if the sentence can be attributed to the given context or not. Use only 'Yes' (1) or 'No' (0) as a binary classification.

                        Question: {question}
                        Context: {context}
                        Reference Answer: {reference_answer}

                        Instructions:
                        1. Break down the reference answer into individual sentences
                        2. For each sentence, determine if it can be attributed to the provided context
                        3. A sentence is attributable (1) if the information can be found or inferred from the context
                        4. A sentence is not attributable (0) if the information is not present in the context
                        5. Be strict in your evaluation - only mark as attributable if there is clear supporting evidence
                        6. Consider paraphrases and semantically equivalent information as supporting evidence

                        Respond with a JSON object containing:
                        - classifications: A list of classification objects, each containing:
                          - statement: The individual sentence from the reference answer
                          - reason: Detailed explanation for the classification
                          - attributed: 1 if the statement can be attributed to the context, 0 otherwise
                        """;
    }

    public Double singleTurnScore(final ContextRecallConfig config, final Sample sample) {
        // Validate required inputs
        String reference = sample.getReference();
        if (reference == null || reference.trim().isEmpty()) {
            log.warn("No reference provided for Context Recall evaluation - this metric requires a reference answer");
            return 0.0;
        }

        List<String> retrievedContexts = sample.getRetrievedContexts();
        if (retrievedContexts == null || retrievedContexts.isEmpty()) {
            log.warn("No retrieved contexts provided for Context Recall evaluation");
            return 0.0;
        }

        String userInput = sample.getUserInput();
        if (userInput == null || userInput.trim().isEmpty()) {
            log.warn("No user input provided for Context Recall evaluation");
            return 0.0;
        }

        log.debug("Computing LLM-based context recall evaluation");

        // Classify each statement in the reference answer
        ContextRecallClassifications classifications =
                classifyReferenceStatements(userInput, String.join("\n\n", retrievedContexts), reference);

        if (classifications.classifications() == null
                || classifications.classifications().isEmpty()) {
            log.warn("No classifications returned from LLM");
            return 0.0;
        }

        log.debug(
                "Classified {} statements from reference answer",
                classifications.classifications().size());

        // Calculate recall as the fraction of statements that are attributable
        return calculateContextRecall(classifications.classifications());
    }

    public CompletableFuture<Double> singleTurnScoreAsync(ContextRecallConfig config, Sample sample) {
        return CompletableFuture.supplyAsync(() -> singleTurnScore(config, sample));
    }

    private ContextRecallClassifications classifyReferenceStatements(
            String question, String context, String referenceAnswer) {
        final Map<String, Object> variables = Map.of(
                "question", question,
                "context", context,
                "reference_answer", referenceAnswer);

        return chatClient
                .prompt(PromptTemplate.builder()
                        .template(contextRecallPrompt)
                        .variables(variables)
                        .build()
                        .create())
                .call()
                .entity(ContextRecallClassifications.class);
    }

    private Double calculateContextRecall(List<ContextRecallClassification> classifications) {
        if (classifications.isEmpty()) {
            return 0.0;
        }

        long attributedStatements = classifications.stream()
                .mapToInt(ContextRecallClassification::attributed)
                .sum();

        return (double) attributedStatements / classifications.size();
    }

    /**
     * Response DTO for individual statement classification
     */
    public record ContextRecallClassification(
            @JsonPropertyDescription("The individual statement from the reference answer") String statement,
            @JsonPropertyDescription("Detailed explanation for the classification") String reason,
            @JsonPropertyDescription("1 if the statement can be attributed to the context, 0 otherwise")
                    Integer attributed) {}

    /**
     * Response DTO for all statement classifications
     */
    public record ContextRecallClassifications(
            @JsonPropertyDescription("List of classification objects for each statement in the reference answer")
                    List<ContextRecallClassification> classifications) {}

    @Data
    @Builder
    public static class ContextRecallConfig {}
}
