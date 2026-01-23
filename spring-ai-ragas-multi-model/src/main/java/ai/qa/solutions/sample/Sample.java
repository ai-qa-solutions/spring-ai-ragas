package ai.qa.solutions.sample;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single-turn or multi-turn interaction sample for metric evaluation.
 * <p>
 * Single-turn fields (used by RAG and response quality metrics):
 * <ul>
 *   <li>{@code userInput} - The user's question or query</li>
 *   <li>{@code retrievedContexts} - Retrieved context documents for RAG evaluation</li>
 *   <li>{@code response} - The AI-generated response</li>
 *   <li>{@code reference} - The ground truth/expected answer</li>
 * </ul>
 * <p>
 * Multi-turn fields (used by agent metrics):
 * <ul>
 *   <li>{@code messages} - Conversation history for multi-turn evaluation</li>
 *   <li>{@code toolCalls} - Actual tool calls made by the agent</li>
 *   <li>{@code referenceToolCalls} - Expected/correct tool calls</li>
 *   <li>{@code referenceTopics} - Expected topics for topic adherence evaluation</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sample {

    // ==================== Single-turn fields ====================

    /** The user's question or query. */
    private String userInput;

    /** Retrieved context documents for RAG evaluation. */
    private List<String> retrievedContexts;

    /** The AI-generated response. */
    private String response;

    /** The ground truth/expected answer. */
    private String reference;

    /** Custom rubric for evaluation (key-value pairs). */
    private Map<String, String> rubric;

    /** Additional metadata for tracking or filtering. */
    private Map<String, Object> metadata;

    // ==================== Multi-turn fields ====================

    /** Conversation history for multi-turn evaluation. */
    private List<Message> messages;

    /** Actual tool calls made by the agent. */
    private List<ToolCall> toolCalls;

    /** Expected/correct tool calls for comparison. */
    private List<ToolCall> referenceToolCalls;

    /** Expected topics for topic adherence evaluation. */
    private List<String> referenceTopics;

    /**
     * Represents a single message in a conversation.
     *
     * @param role The role of the message sender (e.g., "user", "assistant", "system")
     * @param content The message content
     */
    public record Message(String role, String content) {}

    /**
     * Represents a tool call made by an agent.
     *
     * @param name The name of the tool being called
     * @param arguments The arguments passed to the tool (key-value pairs)
     */
    public record ToolCall(String name, Map<String, Object> arguments) {}
}
