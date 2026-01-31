package ai.qa.solutions.sample.message;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Message Types Tests")
class MessageTypesTest {

    @Nested
    @DisplayName("HumanMessage Tests")
    class HumanMessageTests {

        @Test
        @DisplayName("Should create human message with content")
        void shouldCreateHumanMessageWithContent() {
            final HumanMessage message = new HumanMessage("Hello, world!");

            assertThat(message.content()).isEqualTo("Hello, world!");
            assertThat(message).isInstanceOf(BaseMessage.class);
        }

        @Test
        @DisplayName("Should support null content")
        void shouldSupportNullContent() {
            final HumanMessage message = new HumanMessage(null);

            assertThat(message.content()).isNull();
        }
    }

    @Nested
    @DisplayName("AIMessage Tests")
    class AIMessageTests {

        @Test
        @DisplayName("Should create AI message with content only")
        void shouldCreateAIMessageWithContentOnly() {
            final AIMessage message = new AIMessage("I can help you with that.");

            assertThat(message.content()).isEqualTo("I can help you with that.");
            assertThat(message.toolCalls()).isEmpty();
            assertThat(message).isInstanceOf(BaseMessage.class);
        }

        @Test
        @DisplayName("Should create AI message with tool calls")
        void shouldCreateAIMessageWithToolCalls() {
            final List<ToolCall> toolCalls =
                    List.of(new ToolCall("search", Map.of("query", "weather")), new ToolCall("calendar", Map.of()));

            final AIMessage message = new AIMessage("Let me check that.", toolCalls);

            assertThat(message.content()).isEqualTo("Let me check that.");
            assertThat(message.toolCalls()).hasSize(2);
            assertThat(message.toolCalls().get(0).name()).isEqualTo("search");
        }

        @Test
        @DisplayName("Should default to empty tool calls when null")
        void shouldDefaultToEmptyToolCallsWhenNull() {
            final AIMessage message = new AIMessage("Response", null);

            assertThat(message.toolCalls()).isNotNull();
            assertThat(message.toolCalls()).isEmpty();
        }
    }

    @Nested
    @DisplayName("ToolMessage Tests")
    class ToolMessageTests {

        @Test
        @DisplayName("Should create tool message with content")
        void shouldCreateToolMessageWithContent() {
            final ToolMessage message = new ToolMessage("Search results: 5 items found");

            assertThat(message.content()).isEqualTo("Search results: 5 items found");
            assertThat(message).isInstanceOf(BaseMessage.class);
        }
    }

    @Nested
    @DisplayName("ToolCall Tests")
    class ToolCallTests {

        @Test
        @DisplayName("Should create tool call with name and arguments")
        void shouldCreateToolCallWithNameAndArguments() {
            final ToolCall toolCall =
                    new ToolCall("search_flights", Map.of("destination", "NYC", "date", "2024-03-15"));

            assertThat(toolCall.name()).isEqualTo("search_flights");
            assertThat(toolCall.arguments()).containsEntry("destination", "NYC");
            assertThat(toolCall.arguments()).containsEntry("date", "2024-03-15");
        }

        @Test
        @DisplayName("Should create tool call with name only")
        void shouldCreateToolCallWithNameOnly() {
            final ToolCall toolCall = new ToolCall("get_current_time");

            assertThat(toolCall.name()).isEqualTo("get_current_time");
            assertThat(toolCall.arguments()).isEmpty();
        }

        @Test
        @DisplayName("Should default to empty arguments when null")
        void shouldDefaultToEmptyArgumentsWhenNull() {
            final ToolCall toolCall = new ToolCall("test", null);

            assertThat(toolCall.arguments()).isNotNull();
            assertThat(toolCall.arguments()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Sealed Interface Tests")
    class SealedInterfaceTests {

        @Test
        @DisplayName("All message types should implement BaseMessage")
        void allMessageTypesShouldImplementBaseMessage() {
            final List<BaseMessage> messages = List.of(
                    new HumanMessage("User input"), new AIMessage("AI response"), new ToolMessage("Tool output"));

            assertThat(messages).allMatch(m -> m instanceof BaseMessage);
            assertThat(messages).hasSize(3);
        }

        @Test
        @DisplayName("Should support instanceof pattern matching")
        void shouldSupportInstanceOfPatternMatching() {
            final BaseMessage message = new AIMessage("Test", List.of(new ToolCall("api", Map.of())));

            String result = "";
            if (message instanceof HumanMessage h) {
                result = "human: " + h.content();
            } else if (message instanceof AIMessage a) {
                result = "ai: " + a.content() + " with " + a.toolCalls().size() + " tool calls";
            } else if (message instanceof ToolMessage t) {
                result = "tool: " + t.content();
            }

            assertThat(result).isEqualTo("ai: Test with 1 tool calls");
        }
    }

    @Nested
    @DisplayName("Immutability Tests")
    class ImmutabilityTests {

        @Test
        @DisplayName("Messages should be immutable records")
        void messagesShouldBeImmutableRecords() {
            final HumanMessage human = new HumanMessage("test");
            final AIMessage ai = new AIMessage("test", List.of());
            final ToolMessage tool = new ToolMessage("test");

            // Records are inherently immutable
            assertThat(human.getClass().isRecord()).isTrue();
            assertThat(ai.getClass().isRecord()).isTrue();
            assertThat(tool.getClass().isRecord()).isTrue();
        }

        @Test
        @DisplayName("ToolCall should be immutable record")
        void toolCallShouldBeImmutableRecord() {
            final ToolCall toolCall = new ToolCall("test", Map.of("key", "value"));

            assertThat(toolCall.getClass().isRecord()).isTrue();
        }
    }
}
