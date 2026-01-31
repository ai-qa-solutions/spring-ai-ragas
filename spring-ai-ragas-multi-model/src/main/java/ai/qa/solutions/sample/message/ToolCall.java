package ai.qa.solutions.sample.message;

import java.util.Map;

/**
 * Represents a tool/function call made by an AI agent.
 * <p>
 * Tool calls are typically embedded within {@link AIMessage} objects
 * to indicate which tools the assistant invoked during response generation.
 * <p>
 * Example:
 * <pre>{@code
 * ToolCall call = new ToolCall(
 *     "search_flights",
 *     Map.of("destination", "NYC", "date", "2024-03-15")
 * );
 * }</pre>
 *
 * @param name the name of the tool being called
 * @param arguments the arguments passed to the tool (key-value pairs)
 */
public record ToolCall(String name, Map<String, Object> arguments) {

    /**
     * Compact constructor to ensure arguments is never null.
     */
    public ToolCall {
        if (arguments == null) {
            arguments = Map.of();
        }
    }

    /**
     * Creates a tool call with no arguments.
     *
     * @param name the name of the tool being called
     */
    public ToolCall(final String name) {
        this(name, Map.of());
    }
}
