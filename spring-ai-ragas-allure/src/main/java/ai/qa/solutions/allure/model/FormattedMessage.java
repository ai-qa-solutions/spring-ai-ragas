package ai.qa.solutions.allure.model;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Represents a formatted conversation message for Allure reports.
 * <p>
 * Used to display multi-turn conversations in agent metrics reports.
 */
@Value
@Builder
public class FormattedMessage {

    /**
     * Message type: "human", "ai", or "tool".
     */
    String type;

    /**
     * Message content.
     */
    String content;

    /**
     * Tool calls made by AI (only for AI messages).
     */
    @Builder.Default
    List<ToolCallData> toolCalls = List.of();

    /**
     * Checks if this is a human message.
     *
     * @return true if type is "human"
     */
    public boolean isHuman() {
        return "human".equals(type);
    }

    /**
     * Checks if this is an AI message.
     *
     * @return true if type is "ai"
     */
    public boolean isAi() {
        return "ai".equals(type);
    }

    /**
     * Checks if this is a tool message.
     *
     * @return true if type is "tool"
     */
    public boolean isTool() {
        return "tool".equals(type);
    }

    /**
     * Checks if this AI message has tool calls.
     *
     * @return true if toolCalls is not empty
     */
    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }

    /**
     * Represents a tool call made by the AI.
     */
    @Value
    @Builder
    public static class ToolCallData {

        /**
         * Tool name.
         */
        String name;

        /**
         * Tool arguments as JSON string.
         */
        String arguments;
    }
}
