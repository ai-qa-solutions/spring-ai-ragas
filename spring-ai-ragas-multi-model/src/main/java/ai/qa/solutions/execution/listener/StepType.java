package ai.qa.solutions.execution.listener;

/**
 * Type of step in metric evaluation.
 * <p>
 * Used to determine how to display step information in logs and reports.
 */
public enum StepType {

    /**
     * LLM call step - sends a prompt to language models.
     * <p>
     * The request field in StepResults contains the prompt.
     */
    LLM,

    /**
     * Embedding step - generates embeddings for text.
     * <p>
     * The request field in StepResults contains the text(s) to embed.
     */
    EMBEDDING,

    /**
     * Computation step - pure computation without external calls.
     * <p>
     * No request is sent; the step just processes existing data.
     */
    COMPUTE
}
