package ai.qa.solutions.metrics.agent.en;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.agent.TopicAdherenceMetric;
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
 * Integration tests for Topic Adherence Metric - English Language.
 * <p>
 * Tests the evaluation of whether conversation topics adhere to expected reference topics.
 * <p>
 * Key characteristics:
 * - F1 mode: harmonic mean of precision and recall (default)
 * - PRECISION mode: focus on avoiding off-topic discussions
 * - RECALL mode: focus on covering all reference topics
 * - Returns score from 0.0 to 1.0
 */
@Slf4j
@EnableAutoConfiguration
@SuppressWarnings("LoggingSimilarMessage")
@DisplayName("Topic Adherence Metric - English Language Validation")
@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".*")
@SpringBootTest(classes = EnTopicAdherenceIntegrationIT.TopicAdherenceIntegrationTestConfiguration.class)
class EnTopicAdherenceIntegrationIT {

    @Configuration
    public static class TopicAdherenceIntegrationTestConfiguration {}

    @Autowired
    private TopicAdherenceMetric topicAdherenceMetric;

    @Nested
    @DisplayName("F1 Mode Tests")
    class F1ModeTests {

        @Test
        @DisplayName("All topics on topic - EXPECTED HIGH SCORE")
        void testAllTopicsOnTopic() {
            log.info("=== All Topics On Topic Test ===");

            final Sample sample = Sample.builder()
                    .userInputMessages(
                            List.of(
                                    new HumanMessage("I'd like to book a flight to Paris for next week."),
                                    new AIMessage(
                                            """
                                    I'd be happy to help you book a flight to Paris. What dates \
                                    are you looking at, and which airport would you prefer to fly from?"""),
                                    new HumanMessage("I'll fly from New York, departing Monday and returning Friday."),
                                    new AIMessage(
                                            """
                                    I found several flight options for you. There's a direct Air France \
                                    flight departing at 7 PM for $850 round trip. Would you like me to book that?"""),
                                    new HumanMessage("Yes, please book that flight for me."),
                                    new AIMessage(
                                            """
                                    Your flight has been booked! Confirmation number: AF12345. \
                                    You'll receive an email with your e-ticket shortly.""")))
                    .referenceTopics(List.of("flight booking", "travel arrangements", "airline tickets"))
                    .build();

            final TopicAdherenceMetric.TopicAdherenceConfig config = TopicAdherenceMetric.TopicAdherenceConfig.builder()
                    .mode(TopicAdherenceMetric.Mode.F1)
                    .build();

            final Double score = topicAdherenceMetric.singleTurnScore(config, sample);

            log.info("Reference topics: flight booking, travel arrangements, airline tickets");
            log.info("Score: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.5, "All topics on topic should have high score. Received: " + score);
        }

        @Test
        @DisplayName("Conversation drifts off topic - EXPECTED LOW SCORE")
        void testConversationDriftsOffTopic() {
            log.info("=== Conversation Drifts Off Topic Test ===");

            final Sample sample = Sample.builder()
                    .userInputMessages(
                            List.of(
                                    new HumanMessage("I want to learn about cooking Italian pasta."),
                                    new AIMessage(
                                            """
                                    Great choice! Italian pasta is delicious. By the way, \
                                    have you tried the new cryptocurrency Bitcoin?"""),
                                    new HumanMessage("No, I'm interested in pasta, not crypto."),
                                    new AIMessage(
                                            """
                                    Sure, but speaking of investments, the stock market \
                                    has been quite volatile lately. You should consider investing."""),
                                    new HumanMessage("Can we please focus on cooking?"),
                                    new AIMessage(
                                            """
                                    Of course! Also, have you heard about the latest football \
                                    match? It was quite exciting.""")))
                    .referenceTopics(List.of("Italian cuisine", "pasta cooking", "recipes"))
                    .build();

            final TopicAdherenceMetric.TopicAdherenceConfig config = TopicAdherenceMetric.TopicAdherenceConfig.builder()
                    .mode(TopicAdherenceMetric.Mode.F1)
                    .build();

            final Double score = topicAdherenceMetric.singleTurnScore(config, sample);

            log.info("Reference topics: Italian cuisine, pasta cooking, recipes");
            log.info("Score: {}", score);

            assertNotNull(score);
            assertTrue(score <= 0.6, "Off-topic conversation should have low score. Received: " + score);
        }

        @Test
        @DisplayName("Technical support conversation - EXPECTED HIGH SCORE")
        void testTechnicalSupportConversation() {
            log.info("=== Technical Support Conversation Test ===");

            final Sample sample = Sample.builder()
                    .userInputMessages(
                            List.of(
                                    new HumanMessage("My laptop won't turn on after the latest Windows update."),
                                    new AIMessage(
                                            """
                                    I understand that's frustrating. Let's troubleshoot this together. \
                                    Can you tell me if any lights appear when you press the power button?"""),
                                    new HumanMessage("Yes, the power light blinks briefly then nothing."),
                                    new AIMessage(
                                            """
                                    That indicates the laptop is receiving power but failing to boot. \
                                    Let's try a hard reset: hold the power button for 15 seconds, then release."""),
                                    new HumanMessage("I did that and now it's showing the Windows recovery screen."),
                                    new AIMessage(
                                            """
                                    Perfect! The recovery screen means we can fix this. \
                                    Select 'Startup Repair' to automatically fix boot issues.""")))
                    .referenceTopics(List.of("computer troubleshooting", "Windows operating system", "laptop hardware"))
                    .build();

            final TopicAdherenceMetric.TopicAdherenceConfig config = TopicAdherenceMetric.TopicAdherenceConfig.builder()
                    .mode(TopicAdherenceMetric.Mode.F1)
                    .build();

            final Double score = topicAdherenceMetric.singleTurnScore(config, sample);

            log.info("Reference topics: computer troubleshooting, Windows operating system, laptop hardware");
            log.info("Score: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.5, "Technical support conversation should be on topic. Received: " + score);
        }
    }

    @Nested
    @DisplayName("PRECISION Mode Tests")
    class PrecisionModeTests {

        @Test
        @DisplayName("High precision - no off-topic discussions")
        void testHighPrecisionNoOffTopic() {
            log.info("=== High Precision Test ===");

            final Sample sample = Sample.builder()
                    .userInputMessages(
                            List.of(
                                    new HumanMessage("Tell me about machine learning algorithms."),
                                    new AIMessage(
                                            """
                                    Machine learning algorithms can be categorized into three types: \
                                    supervised learning, unsupervised learning, and reinforcement learning."""),
                                    new HumanMessage("What's the difference between them?"),
                                    new AIMessage(
                                            """
                                    Supervised learning uses labeled data to make predictions. \
                                    Unsupervised learning finds patterns in unlabeled data. \
                                    Reinforcement learning learns through trial and error rewards.""")))
                    .referenceTopics(List.of("machine learning", "artificial intelligence", "data science"))
                    .build();

            final TopicAdherenceMetric.TopicAdherenceConfig config = TopicAdherenceMetric.TopicAdherenceConfig.builder()
                    .mode(TopicAdherenceMetric.Mode.PRECISION)
                    .build();

            final Double score = topicAdherenceMetric.singleTurnScore(config, sample);

            log.info("Reference topics: machine learning, artificial intelligence, data science");
            log.info("Precision Score: {}", score);

            assertNotNull(score);
            assertTrue(
                    score >= 0.6, "High precision expected when all discussed topics are on topic. Received: " + score);
        }

        @Test
        @DisplayName("Low precision - many off-topic discussions")
        void testLowPrecisionManyOffTopic() {
            log.info("=== Low Precision Test ===");

            final Sample sample = Sample.builder()
                    .userInputMessages(
                            List.of(
                                    new HumanMessage("Let's discuss Python programming."),
                                    new AIMessage(
                                            """
                                    Python is great! But have you considered adopting a pet python snake? \
                                    They're fascinating creatures."""),
                                    new HumanMessage("No, I mean the programming language."),
                                    new AIMessage(
                                            """
                                    Oh right! Speaking of languages, did you know French is \
                                    the language of love? Très romantique!"""),
                                    new HumanMessage("Please focus on Python coding."),
                                    new AIMessage(
                                            """
                                    Coding is nice. But coding music is also fun! \
                                    Have you tried playing guitar?""")))
                    .referenceTopics(List.of("Python programming", "software development", "coding"))
                    .build();

            final TopicAdherenceMetric.TopicAdherenceConfig config = TopicAdherenceMetric.TopicAdherenceConfig.builder()
                    .mode(TopicAdherenceMetric.Mode.PRECISION)
                    .build();

            final Double score = topicAdherenceMetric.singleTurnScore(config, sample);

            log.info("Reference topics: Python programming, software development, coding");
            log.info("Precision Score: {}", score);

            assertNotNull(score);
            assertTrue(score <= 0.6, "Low precision expected with many off-topic discussions. Received: " + score);
        }
    }

    @Nested
    @DisplayName("RECALL Mode Tests")
    class RecallModeTests {

        @Test
        @DisplayName("High recall - all reference topics covered")
        void testHighRecallAllTopicsCovered() {
            log.info("=== High Recall Test ===");

            final Sample sample = Sample.builder()
                    .userInputMessages(
                            List.of(
                                    new HumanMessage("I want to plan a healthy lifestyle."),
                                    new AIMessage(
                                            """
                                    Great goal! A healthy lifestyle involves three key areas: \
                                    nutrition, exercise, and mental wellness."""),
                                    new HumanMessage("Tell me more about each."),
                                    new AIMessage(
                                            """
                                    For nutrition, focus on whole foods, vegetables, and lean proteins. \
                                    For exercise, aim for 30 minutes of activity daily - walking, swimming, or gym. \
                                    For mental wellness, practice meditation, get enough sleep, \
                                    and maintain social connections.""")))
                    .referenceTopics(List.of("nutrition", "exercise", "mental health"))
                    .build();

            final TopicAdherenceMetric.TopicAdherenceConfig config = TopicAdherenceMetric.TopicAdherenceConfig.builder()
                    .mode(TopicAdherenceMetric.Mode.RECALL)
                    .build();

            final Double score = topicAdherenceMetric.singleTurnScore(config, sample);

            log.info("Reference topics: nutrition, exercise, mental health");
            log.info("Recall Score: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.5, "High recall expected when all reference topics are covered. Received: " + score);
        }

        @Test
        @DisplayName("Low recall - few reference topics covered")
        void testLowRecallFewTopicsCovered() {
            log.info("=== Low Recall Test ===");

            final Sample sample = Sample.builder()
                    .userInputMessages(
                            List.of(
                                    new HumanMessage("Explain the components of a modern web application."),
                                    new AIMessage("A modern web application uses JavaScript for interactivity."),
                                    new HumanMessage("What about other components?"),
                                    new AIMessage(
                                            """
                                    JavaScript frameworks like React are very popular. \
                                    Many companies use React for their frontend.""")))
                    .referenceTopics(List.of(
                            "frontend development",
                            "backend development",
                            "database systems",
                            "API design",
                            "security",
                            "deployment"))
                    .build();

            final TopicAdherenceMetric.TopicAdherenceConfig config = TopicAdherenceMetric.TopicAdherenceConfig.builder()
                    .mode(TopicAdherenceMetric.Mode.RECALL)
                    .build();

            final Double score = topicAdherenceMetric.singleTurnScore(config, sample);

            log.info("Reference topics: frontend, backend, database, API, security, deployment");
            log.info("Recall Score: {}", score);

            assertNotNull(score);
            assertTrue(score <= 0.5, "Low recall expected when few reference topics are covered. Received: " + score);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Single reference topic fully covered")
        void testSingleTopicFullyCovered() {
            log.info("=== Single Topic Fully Covered Test ===");

            final Sample sample = Sample.builder()
                    .userInputMessages(
                            List.of(
                                    new HumanMessage("How do I make a proper cup of tea?"),
                                    new AIMessage(
                                            """
                                    To make perfect tea, start by boiling fresh water. \
                                    Warm your teapot, add one teaspoon of loose leaf tea per cup, \
                                    pour hot water and steep for 3-5 minutes."""),
                                    new HumanMessage("What temperature should the water be?"),
                                    new AIMessage(
                                            """
                                    For black tea, use water just off the boil (95-100°C). \
                                    Green tea prefers cooler water around 80°C to avoid bitterness.""")))
                    .referenceTopics(List.of("tea preparation"))
                    .build();

            final TopicAdherenceMetric.TopicAdherenceConfig config = TopicAdherenceMetric.TopicAdherenceConfig.builder()
                    .mode(TopicAdherenceMetric.Mode.F1)
                    .build();

            final Double score = topicAdherenceMetric.singleTurnScore(config, sample);

            log.info("Reference topics: tea preparation");
            log.info("Score: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.5, "Single focused topic should have high adherence. Received: " + score);
        }

        @Test
        @DisplayName("Async scoring works correctly")
        void testAsyncScoring() {
            log.info("=== Async Scoring Test ===");

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("What's the weather like?"),
                            new AIMessage("It's currently sunny with a temperature of 72°F (22°C).")))
                    .referenceTopics(List.of("weather"))
                    .build();

            final TopicAdherenceMetric.TopicAdherenceConfig config = TopicAdherenceMetric.TopicAdherenceConfig.builder()
                    .mode(TopicAdherenceMetric.Mode.F1)
                    .build();

            final Double score =
                    topicAdherenceMetric.singleTurnScoreAsync(config, sample).join();

            log.info("Async Score: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.5, "Async scoring should work identically to sync. Received: " + score);
        }
    }

    @Nested
    @DisplayName("Typed Messages API Tests")
    class TypedMessagesApiTests {

        @Test
        @DisplayName("Should evaluate topic adherence with tool messages")
        void shouldEvaluateWithToolMessages() {
            log.info("=== Topic Adherence with Tool Messages Test ===");

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("Help me book a restaurant"),
                            new AIMessage(
                                    "Let me search...",
                                    List.of(new ToolCall("search_restaurants", Map.of("type", "Italian")))),
                            new ToolMessage("Found 10 Italian restaurants"),
                            new AIMessage("I found 10 Italian restaurants near you. Would you like to book one?")))
                    .referenceTopics(List.of("restaurant booking", "Italian food"))
                    .build();

            final TopicAdherenceMetric.TopicAdherenceConfig config = TopicAdherenceMetric.TopicAdherenceConfig.builder()
                    .mode(TopicAdherenceMetric.Mode.F1)
                    .build();

            final Double score = topicAdherenceMetric.multiTurnScore(config, sample);

            log.info("Topic adherence with tool calls");
            log.info("Score: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.0 && score <= 1.0, "Score should be between 0 and 1");
        }

        @Test
        @DisplayName("Should detect topic drift even with tool calls")
        void shouldDetectTopicDriftWithToolCalls() {
            log.info("=== Topic Drift with Tool Calls Test ===");

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("I need help with my taxes"),
                            new AIMessage(
                                    "Let me look up tax information.",
                                    List.of(new ToolCall("search_tax_info", Map.of("year", "2024")))),
                            new ToolMessage("Tax filing deadline is April 15, 2024"),
                            new AIMessage(
                                    """
                                    By the way, have you considered cryptocurrency investments? \
                                    Let me search for crypto prices.""",
                                    List.of(new ToolCall("get_crypto_prices", Map.of("currency", "BTC")))),
                            new ToolMessage("Bitcoin: $45,000"),
                            new AIMessage(
                                    """
                                    Bitcoin is trading at $45,000. Also, the weather is nice today! \
                                    Let me check the forecast.""",
                                    List.of(new ToolCall("get_weather", Map.of("location", "current")))),
                            new ToolMessage("Sunny, 75°F"),
                            new AIMessage("It's sunny and 75°F. Great day for a walk!")))
                    .referenceTopics(List.of("tax preparation", "tax filing", "IRS"))
                    .build();

            final TopicAdherenceMetric.TopicAdherenceConfig config = TopicAdherenceMetric.TopicAdherenceConfig.builder()
                    .mode(TopicAdherenceMetric.Mode.F1)
                    .build();

            final Double score = topicAdherenceMetric.multiTurnScore(config, sample);

            log.info("Topic drift with irrelevant tool calls");
            log.info("Score: {}", score);

            assertNotNull(score);
            assertTrue(score <= 0.6, "Topic drift should result in lower score. Received: " + score);
        }

        @Test
        @DisplayName("Should evaluate multi-step conversation staying on topic")
        void shouldEvaluateMultiStepOnTopic() {
            log.info("=== Multi-Step Conversation On Topic Test ===");

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("I want to learn Python programming"),
                            new AIMessage(
                                    "Great choice! Let me find resources for you.",
                                    List.of(new ToolCall(
                                            "search_courses", Map.of("topic", "Python", "level", "beginner")))),
                            new ToolMessage("Found 15 Python courses"),
                            new AIMessage("I found 15 beginner Python courses. Would you like recommendations?"),
                            new HumanMessage("Yes, what's the best one?"),
                            new AIMessage(
                                    "Let me check ratings.",
                                    List.of(new ToolCall("get_course_details", Map.of("course_id", "PY101")))),
                            new ToolMessage("Python for Beginners - 4.8/5 stars, 50,000 students"),
                            new AIMessage(
                                    """
                                    'Python for Beginners' has excellent reviews. It covers variables, \
                                    loops, functions, and basic data structures."""),
                            new HumanMessage("Perfect, how do I enroll?"),
                            new AIMessage(
                                    "Enrolling you now.",
                                    List.of(new ToolCall(
                                            "enroll_course", Map.of("course_id", "PY101", "user_id", "12345")))),
                            new ToolMessage("Enrollment successful"),
                            new AIMessage("You're enrolled! You can start learning Python right away.")))
                    .referenceTopics(List.of("Python programming", "online learning", "coding education"))
                    .build();

            final TopicAdherenceMetric.TopicAdherenceConfig config = TopicAdherenceMetric.TopicAdherenceConfig.builder()
                    .mode(TopicAdherenceMetric.Mode.F1)
                    .build();

            final Double score = topicAdherenceMetric.multiTurnScore(config, sample);

            log.info("Multi-step conversation staying on topic");
            log.info("Score: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.5, "On-topic multi-step conversation should have high score. Received: " + score);
        }

        @Test
        @DisplayName("Should evaluate with PRECISION mode and typed messages")
        void shouldEvaluatePrecisionModeWithTypedMessages() {
            log.info("=== PRECISION Mode with Typed Messages Test ===");

            final Sample sample = Sample.builder()
                    .userInputMessages(
                            List.of(
                                    new HumanMessage("Explain machine learning"),
                                    new AIMessage(
                                            "Let me search for ML information.",
                                            List.of(new ToolCall("search_docs", Map.of("topic", "machine learning")))),
                                    new ToolMessage("Found ML documentation"),
                                    new AIMessage(
                                            """
                                    Machine learning is a subset of AI that enables systems to learn from data. \
                                    It includes supervised, unsupervised, and reinforcement learning algorithms.""")))
                    .referenceTopics(List.of("machine learning", "artificial intelligence", "algorithms"))
                    .build();

            final TopicAdherenceMetric.TopicAdherenceConfig config = TopicAdherenceMetric.TopicAdherenceConfig.builder()
                    .mode(TopicAdherenceMetric.Mode.PRECISION)
                    .build();

            final Double score = topicAdherenceMetric.multiTurnScore(config, sample);

            log.info("PRECISION mode with typed messages");
            log.info("Score: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.0 && score <= 1.0, "Score should be between 0 and 1");
        }

        @Test
        @DisplayName("Should evaluate with RECALL mode and typed messages")
        void shouldEvaluateRecallModeWithTypedMessages() {
            log.info("=== RECALL Mode with Typed Messages Test ===");

            final Sample sample = Sample.builder()
                    .userInputMessages(
                            List.of(
                                    new HumanMessage("Tell me about healthy eating"),
                                    new AIMessage(
                                            "Searching nutrition info...",
                                            List.of(new ToolCall(
                                                    "search_nutrition", Map.of("category", "healthy eating")))),
                                    new ToolMessage("Found nutrition guidelines"),
                                    new AIMessage(
                                            """
                                    Healthy eating includes eating plenty of fruits and vegetables. \
                                    It's also important to stay hydrated and get regular exercise.""")))
                    .referenceTopics(List.of("nutrition", "vegetables", "fruits", "hydration", "exercise", "vitamins"))
                    .build();

            final TopicAdherenceMetric.TopicAdherenceConfig config = TopicAdherenceMetric.TopicAdherenceConfig.builder()
                    .mode(TopicAdherenceMetric.Mode.RECALL)
                    .build();

            final Double score = topicAdherenceMetric.multiTurnScore(config, sample);

            log.info("RECALL mode with typed messages (partial topic coverage)");
            log.info("Score: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.0 && score <= 1.0, "Score should be between 0 and 1");
        }
    }
}
