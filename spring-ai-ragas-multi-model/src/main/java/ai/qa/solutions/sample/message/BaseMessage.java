package ai.qa.solutions.sample.message;

/**
 * Base interface for all message types in a multi-turn conversation.
 * <p>
 * This sealed interface hierarchy provides type-safe message handling
 * for agent metrics evaluation. Supports pattern matching in switch expressions.
 * <p>
 * Example usage:
 * <pre>{@code
 * String formatted = switch (message) {
 *     case HumanMessage h -> "[USER]: " + h.content();
 *     case AIMessage a -> "[ASSISTANT]: " + a.content();
 *     case ToolMessage t -> "[TOOL]: " + t.content();
 * };
 * }</pre>
 *
 * @see HumanMessage
 * @see AIMessage
 * @see ToolMessage
 */
public sealed interface BaseMessage permits HumanMessage, AIMessage, ToolMessage {

    /**
     * Returns the text content of the message.
     *
     * @return the message content
     */
    String content();
}
