package ai.qa.solutions.sample.message;

/**
 * Represents a tool execution result in a multi-turn conversation.
 * <p>
 * Tool messages contain the output from a tool that was invoked
 * by an {@link AIMessage}.
 * <p>
 * Example:
 * <pre>{@code
 * ToolMessage result = new ToolMessage("Found 5 flights: UA123, AA456, ...");
 * }</pre>
 *
 * @param content the text content of the tool's output
 */
public record ToolMessage(String content) implements BaseMessage {}
