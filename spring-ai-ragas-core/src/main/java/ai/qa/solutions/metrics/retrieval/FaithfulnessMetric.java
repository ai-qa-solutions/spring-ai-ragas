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
 * Faithfulness Metric - Measures factual consistency of response with retrieved context
 * Score ranges from 0.0 to 1.0, where higher scores indicate better consistency
 */
@Slf4j
public class FaithfulnessMetric {
    private final ChatClient chatClient;
    private final String statementGeneratorTemplate;
    private final String nliStatementTemplate;

    public FaithfulnessMetric(final ChatClient chatClient) {
        this.chatClient = chatClient;
        this.statementGeneratorTemplate =
                """
                        Given a question and an answer, analyze the complexity of each sentence in the answer.
                        Break down each sentence into one or more fully understandable statements.
                        Ensure that no pronouns are used in any statement.

                        Question: {question}
                        Answer: {answer}

                        Example:
                        Question: Who was Albert Einstein and what is he best known for?
                        Answer: He was a German-born theoretical physicist, widely acknowledged to be one of the greatest and most influential physicists of all time. He was best known for developing the theory of relativity, he also made important contributions to the development of the theory of quantum mechanics.

                        Output:
                        - Albert Einstein was a German-born theoretical physicist.
                        - Albert Einstein is recognized as one of the greatest and most influential physicists of all time.
                        - Albert Einstein was best known for developing the theory of relativity.
                        - Albert Einstein also made important contributions to the development of the theory of quantum mechanics.

                        Now generate statements for the given question and answer.
                        """;

        this.nliStatementTemplate =
                """
                        Your task is to judge the faithfulness of a series of statements based on a given context.
                        For each statement you must return verdict as 1 if the statement can be directly inferred based on the context
                        or 0 if the statement cannot be directly inferred based on the context.

                        Context:
                        {context}

                        Statements to evaluate:
                        {statements}

                        Example:
                        Context: John is a student at XYZ University. He is pursuing a degree in Computer Science. He is enrolled in several courses this semester, including Data Structures, Algorithms, and Database Management. John is a diligent student and spends a significant amount of time studying and completing assignments. He often stays late in the library to work on his projects.

                        Statements:
                        1. John is majoring in Biology.
                        2. John is taking a course on Artificial Intelligence.
                        3. John is a dedicated student.
                        4. John has a part-time job.

                        Expected Output:
                        For "John is majoring in Biology.":
                        - statement: the original text
                        - reason: John's major is explicitly mentioned as Computer Science. There is no information suggesting he is majoring in Biology.
                        - verdict: 0

                        For "John is taking a course on Artificial Intelligence.":
                        - statement: the original text
                        - reason: The context mentions the courses John is currently enrolled in, and Artificial Intelligence is not mentioned. Therefore, it cannot be deduced that John is taking a course on AI.
                        - verdict: 0

                        For "John is a dedicated student.":
                        - statement: the original text
                        - reason: The context states that he spends a significant amount of time studying and completing assignments. Additionally, it mentions that he often stays late in the library to work on his projects, which implies dedication.
                        - verdict: 1

                        For "John has a part-time job.":
                        - statement: the original text
                        - reason: There is no information given in the context about John having a part-time job.
                        - verdict: 0

                        Now evaluate the given statements based on the provided context.
                        Respond with a JSON object containing a 'verdicts' array where each item has 'statement', 'reason', and 'verdict' fields.
                        """;
    }

    public Double singleTurnScore(final Sample sample) {
        return singleTurnScore(FaithfulnessConfig.builder().build(), sample);
    }

    public Double singleTurnScore(final FaithfulnessConfig config, final Sample sample) {
        try {
            // Step 1: Generate statements from the response
            StatementsResponse statementsResponse = generateStatements(sample);

            if (statementsResponse.statements() == null
                    || statementsResponse.statements().isEmpty()) {
                log.warn("No statements were generated from the answer");
                return Double.NaN;
            }

            // Step 2: Evaluate faithfulness of each statement against retrieved contexts
            VerdictsResponse verdictsResponse = evaluateStatements(sample, statementsResponse.statements());

            // Step 3: Calculate final score
            return computeScore(verdictsResponse);

        } catch (Exception e) {
            log.error("Error calculating faithfulness score", e);
            return Double.NaN;
        }
    }

    public CompletableFuture<Double> singleTurnScoreAsync(final Sample sample) {
        return CompletableFuture.supplyAsync(() -> singleTurnScore(sample));
    }

    public CompletableFuture<Double> singleTurnScoreAsync(final FaithfulnessConfig config, final Sample sample) {
        return CompletableFuture.supplyAsync(() -> singleTurnScore(config, sample));
    }

    private StatementsResponse generateStatements(final Sample sample) {
        return chatClient
                .prompt(PromptTemplate.builder()
                        .template(this.statementGeneratorTemplate)
                        .variables(Map.of(
                                "question", sample.getUserInput(),
                                "answer", sample.getResponse()))
                        .build()
                        .create())
                .call()
                .entity(StatementsResponse.class);
    }

    private VerdictsResponse evaluateStatements(final Sample sample, final List<String> statements) {
        String context = String.join("\n", sample.getRetrievedContexts());
        String statementsFormatted = formatStatements(statements);

        return chatClient
                .prompt(PromptTemplate.builder()
                        .template(this.nliStatementTemplate)
                        .variables(Map.of(
                                "context", context,
                                "statements", statementsFormatted))
                        .build()
                        .create())
                .call()
                .entity(VerdictsResponse.class);
    }

    private String formatStatements(final List<String> statements) {
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < statements.size(); i++) {
            formatted.append(i + 1).append(". ").append(statements.get(i)).append("\n");
        }
        return formatted.toString();
    }

    private Double computeScore(final VerdictsResponse verdictsResponse) {
        if (verdictsResponse.verdicts() == null || verdictsResponse.verdicts().isEmpty()) {
            return Double.NaN;
        }

        long faithfulStatements = verdictsResponse.verdicts().stream()
                .filter(v -> v.verdict() != null && v.verdict() == 1)
                .count();

        return (double) faithfulStatements / verdictsResponse.verdicts().size();
    }

    /**
     * Response DTO for statement generation
     */
    public record StatementsResponse(
            @JsonPropertyDescription(
                            "List of extracted statements from the answer, with pronouns replaced by explicit entities")
                    List<String> statements) {}

    /**
     * Response DTO for faithfulness verdicts
     */
    public record VerdictsResponse(
            @JsonPropertyDescription("List of faithfulness evaluations for each statement")
                    List<StatementVerdict> verdicts) {}

    /**
     * Individual statement faithfulness verdict
     */
    public record StatementVerdict(
            @JsonPropertyDescription("The original statement being evaluated, word-by-word") String statement,
            @JsonPropertyDescription(
                            "Detailed reasoning explaining why the statement can or cannot be inferred from the context")
                    String reason,
            @JsonPropertyDescription(
                            "Binary verdict: 1 if the statement can be directly inferred from the context, 0 otherwise")
                    Integer verdict) {}

    @Data
    @Builder
    public static class FaithfulnessConfig {
        // Placeholder for future configuration options
        // Can be extended with parameters like:
        // - Custom prompts
        // - Batch size
        // - Timeout settings
        // - etc.
    }
}
