package ai.qa.solutions.sample.message;

/**
 * Represents a message from the user in a multi-turn conversation.
 * <p>
 * Example:
 * <pre>{@code
 * HumanMessage message = new HumanMessage("Book a flight to New York");
 * }</pre>
 *
 * @param content the text content of the user's message
 */
public record HumanMessage(String content) implements BaseMessage {}
