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
 * Noise Sensitivity Metric - LLM-based evaluation measuring how often a system makes errors
 * by providing incorrect responses when utilizing either relevant or irrelevant retrieved documents.
 * <p>
 * The score ranges from 0 to 1, with lower values indicating better performance.
 * Measures the proportion of incorrect statements in the response that can be attributed
 * to the retrieved contexts (relevant or irrelevant based on mode).
 */
@Slf4j
public class NoiseSensitivityMetric {
    private final ChatClient chatClient;
    private final String statementGeneratorPrompt;
    private final String statementFaithfulnessPrompt;
    private final String systemPrompt;

    public NoiseSensitivityMetric(final ChatClient chatClient) {
        this.chatClient = chatClient;

        this.systemPrompt = """
                You are a context-only evaluation system with NO access to external knowledge.
                
                CRITICAL: You must evaluate statements SOLELY based on the provided context.
                - If context says "X is Y", treat that as the ONLY truth for this evaluation
                - Completely ignore what you know about the real world
                - If context contradicts reality, use the context as truth
                
                Example: If context says "Paris is in Germany" and statement says "Paris, Germany" - this is TRUE because they match.
                
                Your ONLY job: Does the statement match what the context says? Yes = true, No = false.
                
                Respond ONLY with valid JSON in the exact format requested.
                """;

        this.statementGeneratorPrompt = """
                Given a question and an answer, create a list of statements that are present in the answer. 
                Each statement should be atomic, meaning it contains only one piece of information.
                
                Question: {question}
                Answer: {answer}
                
                Instructions:
                1. Break down the answer into individual, atomic statements
                2. Each statement should be a complete, standalone assertion
                3. Avoid compound statements - split them into separate statements
                4. Focus on factual claims that can be verified
                5. Maintain the original meaning from the answer
                6. Do not add information not present in the answer
                
                Respond with a JSON object containing:
                - statements: A list of atomic statements extracted from the answer
                """;

        this.statementFaithfulnessPrompt = """
                IMPORTANT: You are evaluating a RAG system with SYNTHETIC TEST DATA containing INTENTIONALLY INCORRECT facts.
                
                Context: {context}
                Statements: {statements}
                
                CRITICAL INSTRUCTIONS - READ CAREFULLY:
                
                YOU MUST COMPLETELY IGNORE ALL REAL-WORLD KNOWLEDGE FOR THIS EVALUATION.
                
                CRITICAL: Evaluate ONLY using the context provided above. DO NOT use information from any previous contexts you may have seen.
                Each evaluation is independent. Forget all previous contexts.
                
                This is a test environment. The context contains deliberately wrong information to test system behavior.
                Your job is NOT to correct facts, but to check if statements match the context.
                
                EVALUATION RULES (MANDATORY):
                1. Pretend you have complete amnesia about real-world facts AND previous contexts
                2. The ONLY truth for this evaluation is what THIS SPECIFIC context states
                3. If context says "Paris is in Germany" and statement says "Paris, Germany" → verdict: TRUE (they match the context)
                4. If context says "built in 1899" and statement says "built in 1889" → verdict: FALSE (numbers don't match)
                5. If context doesn't mention something (e.g., location), statement can't be verified → verdict: FALSE
                6. Compare word-by-word and fact-by-fact against THIS context ONLY
                7. Even if reality contradicts the context, use ONLY this context as your source of truth
                8. If the context is about a different topic than the statement, verdict: FALSE
                
                EXAMPLES OF CORRECT EVALUATION:
                - Context: "The tower is in Paris, capital of Germany"
                  Statement: "It is located in Paris, Germany"
                  Verdict: TRUE (both say Paris + Germany, they match)
                
                - Context: "Built in 1899"
                  Statement: "Built in 1889"
                  Verdict: FALSE (1899 ≠ 1889, they don't match)
                
                - Context: "The tower was designed by Gustav"
                  Statement: "It is located in Paris, France"
                  Verdict: FALSE (location not mentioned in this context)
                
                For EACH statement:
                Step 1: What EXACTLY does THIS context say? (not what you remember from before)
                Step 2: Does the statement match what THIS context says (word-for-word, fact-for-fact)?
                Step 3: Is there ANY detail that contradicts or isn't mentioned in THIS context?
                Step 4: Verdict: TRUE only if EVERYTHING in the statement matches THIS context. FALSE if ANY part differs or is missing.
                
                DO NOT let your knowledge of real-world facts OR previous contexts influence your verdict. Each evaluation is completely independent.
                
                Respond with a JSON object containing:
                   - verdicts: A list of verdicts for each statement, where each verdict contains:
                   - statement: The original statement
                   - verdict: true if the ENTIRE statement matches THIS context exactly, false otherwise
                   - reason: Explanation comparing statement to THIS context (not to real-world facts or previous contexts)
                """;
    }

    public Double singleTurnScore(final NoiseSensitivityConfig config, final Sample sample) {
        // Validate required inputs
        String userInput = sample.getUserInput();
        String response = sample.getResponse();
        String reference = sample.getReference();
        List<String> retrievedContexts = sample.getRetrievedContexts();

        if (userInput == null || userInput.trim().isEmpty()) {
            log.warn("No user input provided for Noise Sensitivity evaluation");
            return 0.0;
        }

        if (response == null || response.trim().isEmpty()) {
            log.warn("No response provided for Noise Sensitivity evaluation");
            return 0.0;
        }

        if (reference == null || reference.trim().isEmpty()) {
            log.warn("No reference provided for Noise Sensitivity evaluation");
            return 0.0;
        }

        if (retrievedContexts == null || retrievedContexts.isEmpty()) {
            log.warn("No retrieved contexts provided for Noise Sensitivity evaluation");
            return 0.0;
        }

        log.debug("Computing LLM-based noise sensitivity evaluation in {} mode", config.getMode());

        try {
            // Step 1: Decompose reference and response into atomic statements
            List<String> referenceStatements = decomposeIntoStatements(userInput, reference);
            List<String> responseStatements = decomposeIntoStatements(userInput, response);

            if (responseStatements.isEmpty()) {
                log.warn("No statements extracted from response");
                return 0.0;
            }

            log.debug("Extracted {} statements from reference, {} from response",
                    referenceStatements.size(), responseStatements.size());

            // Step 2: Evaluate faithfulness of statements against contexts and reference
            FaithfulnessResults faithfulnessResults = evaluateStatementFaithfulness(
                    referenceStatements, responseStatements, retrievedContexts, reference);

            // Step 3: Calculate noise sensitivity score
            return calculateNoiseSensitivity(faithfulnessResults, config.getMode());

        } catch (Exception e) {
            log.error("Error during noise sensitivity evaluation", e);
            return 0.0;
        }
    }

    public CompletableFuture<Double> singleTurnScoreAsync(NoiseSensitivityConfig config, Sample sample) {
        return CompletableFuture.supplyAsync(() -> singleTurnScore(config, sample));
    }

    private List<String> decomposeIntoStatements(String question, String answer) {
        final Map<String, Object> variables = Map.of(
                "question", question,
                "answer", answer
        );

        StatementsResponse statementsResponse = chatClient
                .mutate()
                .build()
                .prompt(PromptTemplate.builder()
                        .template(statementGeneratorPrompt)
                        .variables(variables)
                        .build()
                        .create())
                .system(systemPrompt)
                .call()
                .entity(StatementsResponse.class);

        return statementsResponse != null && statementsResponse.statements() != null
                ? statementsResponse.statements()
                : List.of();
    }

    private FaithfulnessResults evaluateStatementFaithfulness(
            List<String> referenceStatements,
            List<String> responseStatements,
            List<String> retrievedContexts,
            String reference) {

        // Evaluate response statements against reference (ground truth)
        // This creates a 2D array with shape (1, num_response_statements) to match Python
        boolean[][] groundTruthToAnswer = evaluateStatementsAs2D(responseStatements, reference);

        // Evaluate reference statements against each retrieved context
        // Shape: (num_reference_statements, num_contexts)
        boolean[][] retrievedToGroundTruth = new boolean[referenceStatements.size()][retrievedContexts.size()];
        for (int contextIdx = 0; contextIdx < retrievedContexts.size(); contextIdx++) {
            List<Boolean> verdicts = evaluateStatements(referenceStatements, retrievedContexts.get(contextIdx));
            for (int statementIdx = 0; statementIdx < referenceStatements.size(); statementIdx++) {
                if (statementIdx < verdicts.size()) {
                    retrievedToGroundTruth[statementIdx][contextIdx] = verdicts.get(statementIdx);
                }
            }
        }

        // Evaluate response statements against each retrieved context
        // Shape: (num_response_statements, num_contexts)
        boolean[][] retrievedToAnswer = new boolean[responseStatements.size()][retrievedContexts.size()];
        for (int contextIdx = 0; contextIdx < retrievedContexts.size(); contextIdx++) {
            List<Boolean> verdicts = evaluateStatements(responseStatements, retrievedContexts.get(contextIdx));
            for (int statementIdx = 0; statementIdx < responseStatements.size(); statementIdx++) {
                if (statementIdx < verdicts.size()) {
                    retrievedToAnswer[statementIdx][contextIdx] = verdicts.get(statementIdx);
                }
            }
        }

        return new FaithfulnessResults(
                groundTruthToAnswer,
                retrievedToGroundTruth,
                retrievedToAnswer
        );
    }

    private boolean[][] evaluateStatementsAs2D(List<String> statements, String context) {
        List<Boolean> verdicts = evaluateStatements(statements, context);

        // Create 2D array with shape (1, num_statements) to match Python behavior
        boolean[][] result = new boolean[1][statements.size()];
        for (int i = 0; i < statements.size() && i < verdicts.size(); i++) {
            result[0][i] = verdicts.get(i);
        }

        return result;
    }

    private List<Boolean> evaluateStatements(List<String> statements, String context) {
        if (statements.isEmpty()) {
            return List.of();
        }

        final Map<String, Object> variables = Map.of(
                "context", context,
                "statements", String.join("\n", statements)
        );

        FaithfulnessVerdictsResponse verdictsResponse = chatClient.mutate()
                .build()
                .prompt(PromptTemplate.builder()
                        .template(statementFaithfulnessPrompt)
                        .variables(variables)
                        .build()
                        .create())
                .system(systemPrompt)
                .call()
                .entity(FaithfulnessVerdictsResponse.class);

        if (verdictsResponse == null || verdictsResponse.verdicts() == null) {
            log.warn("Received null or empty verdicts response. Returning all false.");
            return statements.stream().map(s -> false).toList();
        }

        List<Boolean> results = verdictsResponse.verdicts().stream()
                .map(v -> v.verdict() != null ? v.verdict() : false)
                .toList();

        // Handle case where LLM returns fewer verdicts than statements
        if (results.size() != statements.size()) {
            log.warn("Expected {} verdicts but got {}. Padding with false.",
                    statements.size(), results.size());
            // Pad with false if needed
            if (results.size() < statements.size()) {
                Boolean[] padded = new Boolean[statements.size()];
                for (int i = 0; i < statements.size(); i++) {
                    padded[i] = i < results.size() ? results.get(i) : false;
                }
                return List.of(padded);
            }
        }

        return results;
    }

    private Double calculateNoiseSensitivity(FaithfulnessResults results, NoiseSensitivityMode mode) {
        boolean[][] groundTruthToAnswer = results.groundTruthToAnswer();
        boolean[][] retrievedToGroundTruth = results.retrievedToGroundTruth();
        boolean[][] retrievedToAnswer = results.retrievedToAnswer();

        if (groundTruthToAnswer.length == 0 || groundTruthToAnswer[0].length == 0 || retrievedToAnswer.length == 0) {
            return 0.0;
        }

        int numResponseStatements = groundTruthToAnswer[0].length;
        int numContexts = retrievedToGroundTruth.length > 0 ? retrievedToGroundTruth[0].length : 0;

        // FIXED: Added validation
        if (numContexts == 0) {
            return 0.0;
        }

        // Create incorrect array by inverting ground_truth2answer
        // Python: incorrect = ~answers["ground_truth2answer"]
        boolean[] incorrect = new boolean[numResponseStatements];
        for (int i = 0; i < numResponseStatements; i++) {
            incorrect[i] = !groundTruthToAnswer[0][i];
        }

        // Compute relevant retrievals using max over axis 0 (ground truth statements)
        // Python: relevant_retrieved = np.max(answers["retrieved2ground_truth"], axis=0, keepdims=True)
        boolean[] relevantRetrieved = new boolean[numContexts];
        for (int contextIdx = 0; contextIdx < numContexts; contextIdx++) {
            boolean hasRelevantStatement = false;
            for (int gtStatementIdx = 0; gtStatementIdx < retrievedToGroundTruth.length; gtStatementIdx++) {
                if (retrievedToGroundTruth[gtStatementIdx][contextIdx]) {
                    hasRelevantStatement = true;
                    break;
                }
            }
            relevantRetrieved[contextIdx] = hasRelevantStatement;
        }

        // Compute relevant faithful using max over axis 1 (contexts)
        // Python: relevant_faithful = np.max(relevant_retrieved & answers["retrieved2answer"], axis=1)
        boolean[] relevantFaithful = new boolean[numResponseStatements];
        for (int answerStatementIdx = 0; answerStatementIdx < numResponseStatements; answerStatementIdx++) {
            boolean hasFaithfulContext = false;
            for (int contextIdx = 0; contextIdx < numContexts; contextIdx++) {
                if (relevantRetrieved[contextIdx] && retrievedToAnswer[answerStatementIdx][contextIdx]) {
                    hasFaithfulContext = true;
                    break;
                }
            }
            relevantFaithful[answerStatementIdx] = hasFaithfulContext;
        }

        if (mode == NoiseSensitivityMode.IRRELEVANT) {
            // Compute irrelevant retrievals: ~relevant_retrieved
            boolean[] irrelevantRetrieved = new boolean[numContexts];
            for (int i = 0; i < numContexts; i++) {
                irrelevantRetrieved[i] = !relevantRetrieved[i];
            }

            // Compute irrelevant faithful using max over axis 1 (contexts)
            // Python: irrelevant_faithful = np.max(irrelevant_retrieved & answers["retrieved2answer"], axis=1)
            boolean[] irrelevantFaithful = new boolean[numResponseStatements];
            for (int answerStatementIdx = 0; answerStatementIdx < numResponseStatements; answerStatementIdx++) {
                boolean hasFaithfulIrrelevantContext = false;
                for (int contextIdx = 0; contextIdx < numContexts; contextIdx++) {
                    if (irrelevantRetrieved[contextIdx] && retrievedToAnswer[answerStatementIdx][contextIdx]) {
                        hasFaithfulIrrelevantContext = true;
                        break;
                    }
                }
                irrelevantFaithful[answerStatementIdx] = hasFaithfulIrrelevantContext;
            }

            // Keep them exclusive (irrelevant should not include relevant)
            // Python: irrelevant_faithful &= ~relevant_faithful
            for (int i = 0; i < numResponseStatements; i++) {
                irrelevantFaithful[i] = irrelevantFaithful[i] && !relevantFaithful[i];
            }

            // Return mean of (irrelevant_faithful & incorrect)
            // Python: return float(np.mean(irrelevant_faithful & incorrect))
            int count = 0;
            for (int i = 0; i < numResponseStatements; i++) {
                if (irrelevantFaithful[i] && incorrect[i]) {
                    count++;
                }
            }
            return (double) count / numResponseStatements;

        } else { // RELEVANT mode
            // Return mean of (relevant_faithful & incorrect)
            // Python: return float(np.mean(relevant_faithful & incorrect))
            int count = 0;
            for (int i = 0; i < numResponseStatements; i++) {
                if (relevantFaithful[i] && incorrect[i]) {
                    count++;
                }
            }
            return (double) count / numResponseStatements;
        }
    }

    /**
     * Data structure to hold faithfulness evaluation results
     */
    private record FaithfulnessResults(
            boolean[][] groundTruthToAnswer,      // Shape: (1, num_response_statements)
            boolean[][] retrievedToGroundTruth,   // Shape: (num_reference_statements, num_contexts)
            boolean[][] retrievedToAnswer         // Shape: (num_response_statements, num_contexts)
    ) {
    }

    /**
     * Response DTO for statement decomposition
     */
    public record StatementsResponse(
            @JsonPropertyDescription("List of atomic statements extracted from the answer")
            List<String> statements
    ) {
    }

    /**
     * Response DTO for individual statement verdict
     */
    public record StatementVerdict(
            @JsonPropertyDescription("The original statement")
            String statement,
            @JsonPropertyDescription("True if the statement can be inferred from context, false otherwise")
            Boolean verdict,
            @JsonPropertyDescription("Explanation for the verdict")
            String reason
    ) {
    }

    /**
     * Response DTO for statement faithfulness evaluation
     */
    public record FaithfulnessVerdictsResponse(
            @JsonPropertyDescription("List of verdicts for each statement")
            List<StatementVerdict> verdicts
    ) {
    }

    /**
     * Noise sensitivity evaluation mode
     */
    public enum NoiseSensitivityMode {
        RELEVANT,   // Measures errors from relevant retrieved contexts
        IRRELEVANT  // Measures errors from irrelevant retrieved contexts
    }

    @Data
    @Builder
    public static class NoiseSensitivityConfig {
        /**
         * Evaluation mode for noise sensitivity
         * RELEVANT: measures errors from relevant contexts
         * IRRELEVANT: measures errors from irrelevant contexts
         */
        @Builder.Default
        private NoiseSensitivityMode mode = NoiseSensitivityMode.RELEVANT;
    }
}