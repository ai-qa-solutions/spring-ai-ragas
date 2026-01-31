package ai.qa.solutions.metrics.agent.en;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.agent.AgentGoalAccuracyMetric;
import ai.qa.solutions.sample.Sample;
import ai.qa.solutions.sample.message.AIMessage;
import ai.qa.solutions.sample.message.HumanMessage;
import ai.qa.solutions.sample.message.ToolCall;
import ai.qa.solutions.sample.message.ToolMessage;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

/**
 * Integration tests for Agent Goal Accuracy Metric - English Language.
 * <p>
 * Tests the evaluation of whether an AI agent achieved its intended goal
 * based on multi-turn conversation analysis.
 * <p>
 * Key characteristics:
 * - WITH_REFERENCE mode: compares outcome with provided expected goal
 * - WITHOUT_REFERENCE mode: infers goal from conversation and evaluates achievement
 * - Returns binary score: 1.0 (achieved) or 0.0 (not achieved)
 */
@Slf4j
@EnableAutoConfiguration
@SuppressWarnings("LoggingSimilarMessage")
@DisplayName("Agent Goal Accuracy Metric - English Language Validation")
@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".*")
@SpringBootTest(classes = EnAgentGoalAccuracyIntegrationIT.AgentGoalAccuracyIntegrationTestConfiguration.class)
class EnAgentGoalAccuracyIntegrationIT {

    @Configuration
    public static class AgentGoalAccuracyIntegrationTestConfiguration {}

    @Autowired
    private AgentGoalAccuracyMetric agentGoalAccuracyMetric;

    @Nested
    @DisplayName("WITH_REFERENCE Mode Tests")
    class WithReferenceModeTests {

        @Test
        @DisplayName("Goal clearly achieved - EXPECTED HIGH SCORE (1.0)")
        void testGoalClearlyAchieved() {
            log.info("=== Goal Clearly Achieved Test ===");

            Sample sample = Sample.builder()
                    .userInputMessages(
                            List.of(
                                    new HumanMessage(
                                            "I need to book a flight from New York to Los Angeles for tomorrow."),
                                    new AIMessage(
                                            "I'll help you book that flight. Let me search for available options.",
                                            List.of(new ToolCall(
                                                    "search_flights",
                                                    Map.of(
                                                            "origin",
                                                            "JFK",
                                                            "destination",
                                                            "LAX",
                                                            "date",
                                                            "tomorrow")))),
                                    new ToolMessage(
                                            """
                                    {"flights": [
                                        {"id": "UA123", "airline": "United", "time": "08:00", "price": 299},
                                        {"id": "AA456", "airline": "American", "time": "10:30", "price": 325}
                                    ]}"""),
                                    new AIMessage(
                                            """
                                    I found several flights for tomorrow. The best option is United Airlines \
                                    departing at 8:00 AM for $299."""),
                                    new HumanMessage("That sounds good, please book it."),
                                    new AIMessage(
                                            "Booking your flight...",
                                            List.of(new ToolCall("book_flight", Map.of("flight_id", "UA123")))),
                                    new ToolMessage(
                                            """
                                    {"status": "confirmed", "confirmation": "UA12345", "seat": "14A"}"""),
                                    new AIMessage(
                                            """
                                    I've successfully booked your United Airlines flight from New York (JFK) \
                                    to Los Angeles (LAX) for tomorrow at 8:00 AM. Confirmation number: UA12345. \
                                    You'll receive an email confirmation shortly.""")))
                    .reference("Book a flight from New York to Los Angeles")
                    .build();

            AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITH_REFERENCE)
                            .build();

            Double score = agentGoalAccuracyMetric.singleTurnScore(config, sample);

            log.info("Goal: Book a flight from New York to Los Angeles");
            log.info("Score: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.5, "Goal clearly achieved should have high score. Received: " + score);
        }

        @Test
        @DisplayName("Goal not achieved - EXPECTED LOW SCORE (0.0)")
        void testGoalNotAchieved() {
            log.info("=== Goal Not Achieved Test ===");

            Sample sample = Sample.builder()
                    .userInputMessages(
                            List.of(
                                    new HumanMessage(
                                            "I need to book a flight from New York to Los Angeles for tomorrow."),
                                    new AIMessage(
                                            "I'll help you with that. Let me check available flights.",
                                            List.of(new ToolCall(
                                                    "search_flights",
                                                    Map.of(
                                                            "origin",
                                                            "JFK",
                                                            "destination",
                                                            "LAX",
                                                            "date",
                                                            "tomorrow")))),
                                    new ToolMessage(
                                            """
                                    {"error": "SERVICE_UNAVAILABLE", "message": "Flight API is temporarily down"}"""),
                                    new AIMessage(
                                            """
                                    I'm sorry, but I'm currently unable to access the flight booking system. \
                                    The service is temporarily unavailable."""),
                                    new HumanMessage("Is there anything else you can do?"),
                                    new AIMessage(
                                            """
                                    Unfortunately, I cannot complete the booking at this time. \
                                    Please try again later or contact the airline directly.""")))
                    .reference("Book a flight from New York to Los Angeles")
                    .build();

            AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITH_REFERENCE)
                            .build();

            Double score = agentGoalAccuracyMetric.singleTurnScore(config, sample);

            log.info("Goal: Book a flight from New York to Los Angeles");
            log.info("Score: {}", score);

            assertNotNull(score);
            assertTrue(score <= 0.5, "Goal not achieved should have low score. Received: " + score);
        }

        @Test
        @DisplayName("Customer support resolution - EXPECTED HIGH SCORE")
        void testCustomerSupportResolution() {
            log.info("=== Customer Support Resolution Test ===");

            Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("My order #12345 hasn't arrived yet. It's been 2 weeks."),
                            new AIMessage(
                                    "I'm sorry to hear about the delay. Let me look into your order.",
                                    List.of(new ToolCall("get_order_status", Map.of("order_id", "12345")))),
                            new ToolMessage(
                                    """
                                    {"order_id": "12345", "status": "LOST", "amount": 75.50,
                                     "details": "Package lost during shipping"}"""),
                            new AIMessage(
                                    "I found your order. It appears there was a shipping issue. Processing refund...",
                                    List.of(new ToolCall(
                                            "process_refund", Map.of("order_id", "12345", "amount", 75.50)))),
                            new ToolMessage(
                                    """
                                    {"status": "REFUND_INITIATED", "amount": 75.50, "eta_days": "3-5"}"""),
                            new AIMessage(
                                    """
                                    I've processed a full refund of $75.50 to your original payment method. \
                                    The refund should appear within 3-5 business days."""),
                            new HumanMessage("Thank you! That works for me."),
                            new AIMessage("You're welcome! Is there anything else I can help you with today?")))
                    .reference(
                            "Resolve the customer's complaint about missing order by providing refund or replacement")
                    .build();

            AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITH_REFERENCE)
                            .build();

            Double score = agentGoalAccuracyMetric.singleTurnScore(config, sample);

            log.info("Goal: Resolve the customer's complaint about missing order");
            log.info("Score: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.5, "Customer support issue resolved should have high score. Received: " + score);
        }
    }

    @Nested
    @DisplayName("WITHOUT_REFERENCE Mode Tests")
    class WithoutReferenceModeTests {

        @Test
        @DisplayName("Infer goal and evaluate - successful task")
        void testInferGoalSuccessfulTask() {
            log.info("=== Infer Goal - Successful Task Test ===");

            Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage(
                                    "Can you help me write an email to my boss requesting time off next week?"),
                            new AIMessage(
                                    """
                                    I'd be happy to help you draft that email. Here's a professional \
                                    request for time off:

                                    Subject: Time Off Request - Next Week

                                    Dear [Manager's Name],

                                    I am writing to request time off from [start date] to [end date]. \
                                    I have ensured that my current projects are on track and have arranged \
                                    for coverage during my absence.

                                    Please let me know if you need any additional information.

                                    Best regards,
                                    [Your name]"""),
                            new HumanMessage("That's perfect! Thank you!")))
                    .build();

            AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITHOUT_REFERENCE)
                            .build();

            Double score = agentGoalAccuracyMetric.singleTurnScore(config, sample);

            log.info("Conversation: Help write time off email");
            log.info("Score: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.5, "Goal inferred and achieved should have high score. Received: " + score);
        }

        @Test
        @DisplayName("Infer goal and evaluate - failed task")
        void testInferGoalFailedTask() {
            log.info("=== Infer Goal - Failed Task Test ===");

            Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("I need you to translate this document from French to English."),
                            new AIMessage("I'd be happy to help with the translation. Please share the document."),
                            new HumanMessage("Here it is: 'Bonjour, je m'appelle Pierre...' [rest of document]"),
                            new AIMessage(
                                    """
                                    I apologize, but I'm currently experiencing issues and cannot process \
                                    the translation at this moment. Could you try again later?"""),
                            new HumanMessage("That's disappointing. I really needed this done today.")))
                    .build();

            AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITHOUT_REFERENCE)
                            .build();

            Double score = agentGoalAccuracyMetric.singleTurnScore(config, sample);

            log.info("Conversation: Failed translation request");
            log.info("Score: {}", score);

            assertNotNull(score);
            assertTrue(score <= 0.5, "Goal inferred but not achieved should have low score. Received: " + score);
        }

        @Test
        @DisplayName("Multi-step task completion")
        void testMultiStepTaskCompletion() {
            log.info("=== Multi-Step Task Completion Test ===");

            Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("I want to set up a meeting with my team for next Friday."),
                            new AIMessage("I'll help you set up that meeting. What time works best for you?"),
                            new HumanMessage("Let's do 2 PM."),
                            new AIMessage("Got it. Who should I invite to this meeting?"),
                            new HumanMessage("The entire engineering team - John, Sarah, and Mike."),
                            new AIMessage(
                                    "Creating the meeting and sending invitations...",
                                    List.of(
                                            new ToolCall(
                                                    "create_meeting",
                                                    Map.of(
                                                            "date",
                                                            "next_friday",
                                                            "time",
                                                            "14:00",
                                                            "attendees",
                                                            List.of(
                                                                    "john@company.com",
                                                                    "sarah@company.com",
                                                                    "mike@company.com"))),
                                            new ToolCall("book_room", Map.of("room", "A", "date", "next_friday")))),
                            new ToolMessage(
                                    """
                                    {"meeting_id": "MTG-123", "status": "created", "invites_sent": 3}"""),
                            new ToolMessage(
                                    """
                                    {"room": "A", "status": "booked", "date": "next_friday", "time": "14:00"}"""),
                            new AIMessage(
                                    """
                                    Perfect. I've created a meeting for next Friday at 2 PM with John, Sarah, \
                                    and Mike. I've sent calendar invitations to all participants. The meeting \
                                    will be held in Conference Room A. Would you like me to add an agenda?"""),
                            new HumanMessage("No, that's all I needed. Thanks!")))
                    .build();

            AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITHOUT_REFERENCE)
                            .build();

            Double score = agentGoalAccuracyMetric.singleTurnScore(config, sample);

            log.info("Conversation: Multi-step meeting setup");
            log.info("Score: {}", score);

            assertNotNull(score);
            assertTrue(
                    score >= 0.5, "Multi-step task successfully completed should have high score. Received: " + score);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Complex conversation with implicit goal")
        void testComplexConversationImplicitGoal() {
            log.info("=== Complex Conversation with Implicit Goal Test ===");

            Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("Hey, I'm feeling overwhelmed with work lately."),
                            new AIMessage(
                                    """
                                    I'm sorry to hear you're feeling that way. Would you like to talk about \
                                    what's making you feel overwhelmed, or would you prefer some practical \
                                    tips for managing workload?"""),
                            new HumanMessage("Some tips would be helpful."),
                            new AIMessage(
                                    """
                                    Here are some strategies that might help:

                                    1. Prioritize tasks using the Eisenhower Matrix - focus on urgent \
                                    and important tasks first.
                                    2. Break large projects into smaller, manageable steps.
                                    3. Set boundaries - learn to say no to non-essential requests.
                                    4. Take regular breaks using the Pomodoro technique.
                                    5. Consider delegating where possible.

                                    Would you like me to elaborate on any of these?"""),
                            new HumanMessage("These are really helpful. I'll try the Pomodoro technique first.")))
                    .reference("Help the user manage their work-related stress with practical advice")
                    .build();

            AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITH_REFERENCE)
                            .build();

            Double score = agentGoalAccuracyMetric.singleTurnScore(config, sample);

            log.info("Goal: Help with work-related stress");
            log.info("Score: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.5, "Implicit goal achieved should have high score. Received: " + score);
        }

        @Test
        @DisplayName("Async scoring works correctly")
        void testAsyncScoring() {
            log.info("=== Async Scoring Test ===");

            Sample sample = Sample.builder()
                    .userInputMessages(List.of(new HumanMessage("What's 2 + 2?"), new AIMessage("2 + 2 equals 4.")))
                    .reference("Provide the correct answer to the math question")
                    .build();

            AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITH_REFERENCE)
                            .build();

            Double score =
                    agentGoalAccuracyMetric.singleTurnScoreAsync(config, sample).join();

            log.info("Async Score: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.5, "Async scoring should work identically to sync. Received: " + score);
        }
    }

    @Nested
    @DisplayName("Typed Messages API Tests")
    class TypedMessagesApiTests {

        @Test
        @DisplayName("Should evaluate with typed messages using multiTurnScore")
        void shouldEvaluateWithTypedMessages() {
            log.info("=== Typed Messages API Test ===");

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("I need to book a flight from New York to Los Angeles"),
                            new AIMessage(
                                    "I'll help you book that flight. Let me search.",
                                    List.of(new ToolCall(
                                            "search_flights", Map.of("origin", "JFK", "destination", "LAX")))),
                            new ToolMessage("Found 5 flights: UA123, AA456, DL789, SW101, NK202"),
                            new AIMessage(
                                    "I found 5 flights. The best option is UA123 departing at 8:00 AM for $299.")))
                    .reference("Book a flight from New York to Los Angeles")
                    .build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITH_REFERENCE)
                            .build();

            final Double score = agentGoalAccuracyMetric.multiTurnScore(config, sample);

            log.info("Typed Messages API Score: {}", score);
            assertNotNull(score);
            assertTrue(score >= 0.0 && score <= 1.0, "Score should be between 0 and 1");
        }

        @Test
        @DisplayName("Should evaluate with typed messages - goal not achieved")
        void shouldEvaluateWithTypedMessagesGoalNotAchieved() {
            log.info("=== Typed Messages API - Goal Not Achieved Test ===");

            final Sample sample = Sample.builder()
                    .userInputMessages(
                            List.of(
                                    new HumanMessage("I need to cancel my flight booking"),
                                    new AIMessage(
                                            "Let me check your booking.",
                                            List.of(new ToolCall("search_booking", Map.of("user_id", "12345")))),
                                    new ToolMessage("Error: No active bookings found"),
                                    new AIMessage(
                                            """
                                    I'm sorry, but I couldn't find any active bookings to cancel. \
                                    The cancellation could not be completed.""")))
                    .reference("Cancel the user's flight booking")
                    .build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITH_REFERENCE)
                            .build();

            final Double score = agentGoalAccuracyMetric.multiTurnScore(config, sample);

            log.info("Typed Messages API Score (Goal Not Achieved): {}", score);
            assertNotNull(score);
            assertTrue(score >= 0.0 && score <= 1.0, "Score should be between 0 and 1");
        }

        @Test
        @DisplayName("Should evaluate with typed messages in WITHOUT_REFERENCE mode")
        void shouldEvaluateWithTypedMessagesWithoutReference() {
            log.info("=== Typed Messages API - Without Reference Test ===");

            final Sample sample = Sample.builder()
                    .userInputMessages(
                            List.of(
                                    new HumanMessage("Find me the cheapest hotel in Paris for next weekend"),
                                    new AIMessage(
                                            "Searching for hotels...",
                                            List.of(new ToolCall(
                                                    "search_hotels",
                                                    Map.of(
                                                            "city",
                                                            "Paris",
                                                            "check_in",
                                                            "2024-02-10",
                                                            "check_out",
                                                            "2024-02-12")))),
                                    new ToolMessage("Found 15 hotels. Cheapest: Hotel Paris Budget at $89/night"),
                                    new AIMessage(
                                            """
                                    I found Hotel Paris Budget at $89 per night for next weekend. \
                                    Would you like me to book it?"""),
                                    new HumanMessage("Yes, please book it"),
                                    new AIMessage(
                                            "Booking confirmed!",
                                            List.of(new ToolCall(
                                                    "book_hotel", Map.of("hotel_id", "HPB123", "nights", 2)))),
                                    new ToolMessage("Booking successful. Confirmation: HPB-456789"),
                                    new AIMessage(
                                            """
                                    Your booking is confirmed! Confirmation number: HPB-456789. \
                                    You'll receive an email shortly.""")))
                    .build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITHOUT_REFERENCE)
                            .build();

            final Double score = agentGoalAccuracyMetric.multiTurnScore(config, sample);

            log.info("Typed Messages API Score (Without Reference): {}", score);
            assertNotNull(score);
            assertTrue(score >= 0.0 && score <= 1.0, "Score should be between 0 and 1");
        }
    }
}
