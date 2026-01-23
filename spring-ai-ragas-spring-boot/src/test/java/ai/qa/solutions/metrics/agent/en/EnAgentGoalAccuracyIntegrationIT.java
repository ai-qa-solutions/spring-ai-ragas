package ai.qa.solutions.metrics.agent.en;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.agent.AgentGoalAccuracyMetric;
import ai.qa.solutions.sample.Sample;
import java.util.List;
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
                    .messages(List.of(
                            new Sample.Message(
                                    "user", "I need to book a flight from New York to Los Angeles for tomorrow."),
                            new Sample.Message(
                                    "assistant",
                                    "I'll help you book that flight. Let me search for available options."),
                            new Sample.Message(
                                    "assistant",
                                    "I found several flights for tomorrow. The best option is United Airlines "
                                            + "departing at 8:00 AM for $299."),
                            new Sample.Message("user", "That sounds good, please book it."),
                            new Sample.Message(
                                    "assistant",
                                    "I've successfully booked your United Airlines flight from New York (JFK) "
                                            + "to Los Angeles (LAX) for tomorrow at 8:00 AM. Confirmation number: UA12345. "
                                            + "You'll receive an email confirmation shortly.")))
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
                    .messages(List.of(
                            new Sample.Message(
                                    "user", "I need to book a flight from New York to Los Angeles for tomorrow."),
                            new Sample.Message("assistant", "I'll help you with that. Let me check available flights."),
                            new Sample.Message(
                                    "assistant",
                                    "I'm sorry, but I'm currently unable to access the flight booking system. "
                                            + "The service is temporarily unavailable."),
                            new Sample.Message("user", "Is there anything else you can do?"),
                            new Sample.Message(
                                    "assistant",
                                    "Unfortunately, I cannot complete the booking at this time. "
                                            + "Please try again later or contact the airline directly.")))
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
                    .messages(List.of(
                            new Sample.Message("user", "My order #12345 hasn't arrived yet. It's been 2 weeks."),
                            new Sample.Message(
                                    "assistant", "I'm sorry to hear about the delay. Let me look into your order."),
                            new Sample.Message(
                                    "assistant",
                                    "I found your order. It appears there was a shipping issue. I've processed "
                                            + "a full refund of $75.50 to your original payment method. The refund "
                                            + "should appear within 3-5 business days."),
                            new Sample.Message("user", "Thank you! That works for me."),
                            new Sample.Message(
                                    "assistant", "You're welcome! Is there anything else I can help you with today?")))
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
                    .messages(List.of(
                            new Sample.Message(
                                    "user", "Can you help me write an email to my boss requesting time off next week?"),
                            new Sample.Message(
                                    "assistant",
                                    "I'd be happy to help you draft that email. Here's a professional "
                                            + "request for time off:\n\n"
                                            + "Subject: Time Off Request - Next Week\n\n"
                                            + "Dear [Manager's Name],\n\n"
                                            + "I am writing to request time off from [start date] to [end date]. "
                                            + "I have ensured that my current projects are on track and have arranged "
                                            + "for coverage during my absence.\n\n"
                                            + "Please let me know if you need any additional information.\n\n"
                                            + "Best regards,\n[Your name]"),
                            new Sample.Message("user", "That's perfect! Thank you!")))
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
                    .messages(List.of(
                            new Sample.Message("user", "I need you to translate this document from French to English."),
                            new Sample.Message(
                                    "assistant",
                                    "I'd be happy to help with the translation. Please share the document."),
                            new Sample.Message(
                                    "user", "Here it is: 'Bonjour, je m'appelle Pierre...' [rest of document]"),
                            new Sample.Message(
                                    "assistant",
                                    "I apologize, but I'm currently experiencing issues and cannot process "
                                            + "the translation at this moment. Could you try again later?"),
                            new Sample.Message("user", "That's disappointing. I really needed this done today.")))
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
                    .messages(List.of(
                            new Sample.Message("user", "I want to set up a meeting with my team for next Friday."),
                            new Sample.Message(
                                    "assistant", "I'll help you set up that meeting. What time works best for you?"),
                            new Sample.Message("user", "Let's do 2 PM."),
                            new Sample.Message("assistant", "Got it. Who should I invite to this meeting?"),
                            new Sample.Message("user", "The entire engineering team - John, Sarah, and Mike."),
                            new Sample.Message(
                                    "assistant",
                                    "Perfect. I've created a meeting for next Friday at 2 PM with John, Sarah, "
                                            + "and Mike. I've sent calendar invitations to all participants. The meeting "
                                            + "will be held in Conference Room A. Would you like me to add an agenda?"),
                            new Sample.Message("user", "No, that's all I needed. Thanks!")))
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
                    .messages(List.of(
                            new Sample.Message("user", "Hey, I'm feeling overwhelmed with work lately."),
                            new Sample.Message(
                                    "assistant",
                                    "I'm sorry to hear you're feeling that way. Would you like to talk about "
                                            + "what's making you feel overwhelmed, or would you prefer some practical "
                                            + "tips for managing workload?"),
                            new Sample.Message("user", "Some tips would be helpful."),
                            new Sample.Message(
                                    "assistant",
                                    "Here are some strategies that might help:\n\n"
                                            + "1. Prioritize tasks using the Eisenhower Matrix - focus on urgent "
                                            + "and important tasks first.\n"
                                            + "2. Break large projects into smaller, manageable steps.\n"
                                            + "3. Set boundaries - learn to say no to non-essential requests.\n"
                                            + "4. Take regular breaks using the Pomodoro technique.\n"
                                            + "5. Consider delegating where possible.\n\n"
                                            + "Would you like me to elaborate on any of these?"),
                            new Sample.Message(
                                    "user", "These are really helpful. I'll try the Pomodoro technique first.")))
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
                    .messages(List.of(
                            new Sample.Message("user", "What's 2 + 2?"),
                            new Sample.Message("assistant", "2 + 2 equals 4.")))
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
}
