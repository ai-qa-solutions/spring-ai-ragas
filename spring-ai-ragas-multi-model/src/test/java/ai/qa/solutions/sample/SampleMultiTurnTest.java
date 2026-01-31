package ai.qa.solutions.sample;

import static org.assertj.core.api.Assertions.assertThat;

import ai.qa.solutions.sample.message.AIMessage;
import ai.qa.solutions.sample.message.BaseMessage;
import ai.qa.solutions.sample.message.HumanMessage;
import ai.qa.solutions.sample.message.ToolCall;
import ai.qa.solutions.sample.message.ToolMessage;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Sample Multi-Turn Support Tests")
class SampleMultiTurnTest {

    @Nested
    @DisplayName("Typed Messages API Tests")
    class TypedMessagesApiTests {

        @Test
        @DisplayName("Should build sample with typed userInputMessages")
        void shouldBuildSampleWithTypedUserInputMessages() {
            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("Book a flight to NYC"),
                            new AIMessage(
                                    "Searching flights...",
                                    List.of(new ToolCall("search_flights", Map.of("destination", "NYC")))),
                            new ToolMessage("Found 5 flights"),
                            new AIMessage("I found 5 flights to NYC.")))
                    .reference("Flight booked")
                    .build();

            assertThat(sample.getUserInputMessages()).hasSize(4);
            assertThat(sample.getUserInputMessages().get(0)).isInstanceOf(HumanMessage.class);
            assertThat(sample.getUserInputMessages().get(1)).isInstanceOf(AIMessage.class);
            assertThat(sample.getUserInputMessages().get(2)).isInstanceOf(ToolMessage.class);
        }

        @Test
        @DisplayName("Should extract tool calls from AI messages")
        void shouldExtractToolCallsFromAIMessages() {
            final AIMessage aiWithTools = new AIMessage(
                    "Let me search...",
                    List.of(new ToolCall("search", Map.of("q", "test")), new ToolCall("fetch", Map.of())));

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(new HumanMessage("Search for test"), aiWithTools))
                    .build();

            final List<BaseMessage> messages = sample.getUserInputMessages();
            assertThat(messages).hasSize(2);

            final AIMessage ai = (AIMessage) messages.get(1);
            assertThat(ai.toolCalls()).hasSize(2);
            assertThat(ai.toolCalls().get(0).name()).isEqualTo("search");
        }
    }

    @Nested
    @DisplayName("Reference Tool Calls Tests")
    class ReferenceToolCallsTests {

        @Test
        @DisplayName("Should support reference tool calls with new ToolCall")
        void shouldSupportReferenceToolCallsWithNewToolCall() {
            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(new HumanMessage("Book flight")))
                    .referenceToolCalls(List.of(
                            new Sample.ToolCall("search_flights", Map.of("dest", "NYC")),
                            new Sample.ToolCall("book_flight", Map.of("flightId", "UA123"))))
                    .build();

            assertThat(sample.getReferenceToolCalls()).hasSize(2);
            assertThat(sample.getReferenceToolCalls().get(0).name()).isEqualTo("search_flights");
        }
    }

    @Nested
    @DisplayName("Combined Fields Tests")
    class CombinedFieldsTests {

        @Test
        @DisplayName("Should support all multi-turn fields together")
        void shouldSupportAllMultiTurnFieldsTogether() {
            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("Find restaurants"),
                            new AIMessage(
                                    "Searching...",
                                    List.of(new ToolCall("search_restaurants", Map.of("cuisine", "Italian")))),
                            new ToolMessage("Found 10 restaurants"),
                            new AIMessage("I found 10 Italian restaurants.")))
                    .referenceToolCalls(
                            List.of(new Sample.ToolCall("search_restaurants", Map.of("cuisine", "Italian"))))
                    .referenceTopics(List.of("restaurant search", "Italian cuisine"))
                    .reference("List of Italian restaurants provided")
                    .build();

            assertThat(sample.getUserInputMessages()).hasSize(4);
            assertThat(sample.getReferenceToolCalls()).hasSize(1);
            assertThat(sample.getReferenceTopics()).hasSize(2);
            assertThat(sample.getReference()).isNotEmpty();
        }
    }
}
