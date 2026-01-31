package ai.qa.solutions.sample.message;

import java.util.List;

/**
 * Represents a message from the AI assistant in a multi-turn conversation.
 * <p>
 * AI messages can optionally include tool calls that the assistant invoked
 * during its response generation.
 * <p>
 * Example with tool calls:
 * <pre>{@code
 * AIMessage message = new AIMessage(
 *     "Let me search for flights...",
 *     List.of(new ToolCall("search_flights", Map.of("destination", "NYC")))
 * );
 * }</pre>
 * <p>
 * Example without tool calls:
 * <pre>{@code
 * AIMessage message = new AIMessage("I found 5 available flights.");
 * }</pre>
 *
 * @param content the text content of the assistant's response
 * @param toolCalls list of tool calls made by the assistant (empty list if none)
 */
public record AIMessage(String content, List<ToolCall> toolCalls) implements BaseMessage {

    /**
     * Creates an AI message without tool calls.
     *
     * @param content the text content of the assistant's response
     */
    public AIMessage(final String content) {
        this(content, List.of());
    }

    /**
     * Compact constructor to ensure toolCalls is never null.
     */
    public AIMessage {
        if (toolCalls == null) {
            toolCalls = List.of();
        }
    }
}
