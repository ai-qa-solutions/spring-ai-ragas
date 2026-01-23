package ai.qa.solutions.metrics.agent.en;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.agent.TopicAdherenceMetric;
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
                    .messages(List.of(
                            new Sample.Message("user", "I'd like to book a flight to Paris for next week."),
                            new Sample.Message(
                                    "assistant",
                                    "I'd be happy to help you book a flight to Paris. What dates "
                                            + "are you looking at, and which airport would you prefer to fly from?"),
                            new Sample.Message(
                                    "user", "I'll fly from New York, departing Monday and returning Friday."),
                            new Sample.Message(
                                    "assistant",
                                    "I found several flight options for you. There's a direct Air France "
                                            + "flight departing at 7 PM for $850 round trip. Would you like me to book that?"),
                            new Sample.Message("user", "Yes, please book that flight for me."),
                            new Sample.Message(
                                    "assistant",
                                    "Your flight has been booked! Confirmation number: AF12345. "
                                            + "You'll receive an email with your e-ticket shortly.")))
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
                    .messages(List.of(
                            new Sample.Message("user", "I want to learn about cooking Italian pasta."),
                            new Sample.Message(
                                    "assistant",
                                    "Great choice! Italian pasta is delicious. By the way, "
                                            + "have you tried the new cryptocurrency Bitcoin?"),
                            new Sample.Message("user", "No, I'm interested in pasta, not crypto."),
                            new Sample.Message(
                                    "assistant",
                                    "Sure, but speaking of investments, the stock market "
                                            + "has been quite volatile lately. You should consider investing."),
                            new Sample.Message("user", "Can we please focus on cooking?"),
                            new Sample.Message(
                                    "assistant",
                                    "Of course! Also, have you heard about the latest football "
                                            + "match? It was quite exciting.")))
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
                    .messages(List.of(
                            new Sample.Message("user", "My laptop won't turn on after the latest Windows update."),
                            new Sample.Message(
                                    "assistant",
                                    "I understand that's frustrating. Let's troubleshoot this together. "
                                            + "Can you tell me if any lights appear when you press the power button?"),
                            new Sample.Message("user", "Yes, the power light blinks briefly then nothing."),
                            new Sample.Message(
                                    "assistant",
                                    "That indicates the laptop is receiving power but failing to boot. "
                                            + "Let's try a hard reset: hold the power button for 15 seconds, then release."),
                            new Sample.Message("user", "I did that and now it's showing the Windows recovery screen."),
                            new Sample.Message(
                                    "assistant",
                                    "Perfect! The recovery screen means we can fix this. "
                                            + "Select 'Startup Repair' to automatically fix boot issues.")))
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
                    .messages(List.of(
                            new Sample.Message("user", "Tell me about machine learning algorithms."),
                            new Sample.Message(
                                    "assistant",
                                    "Machine learning algorithms can be categorized into three types: "
                                            + "supervised learning, unsupervised learning, and reinforcement learning."),
                            new Sample.Message("user", "What's the difference between them?"),
                            new Sample.Message(
                                    "assistant",
                                    "Supervised learning uses labeled data to make predictions. "
                                            + "Unsupervised learning finds patterns in unlabeled data. "
                                            + "Reinforcement learning learns through trial and error rewards.")))
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
                    .messages(List.of(
                            new Sample.Message("user", "Let's discuss Python programming."),
                            new Sample.Message(
                                    "assistant",
                                    "Python is great! But have you considered adopting a pet python snake? "
                                            + "They're fascinating creatures."),
                            new Sample.Message("user", "No, I mean the programming language."),
                            new Sample.Message(
                                    "assistant",
                                    "Oh right! Speaking of languages, did you know French is "
                                            + "the language of love? Très romantique!"),
                            new Sample.Message("user", "Please focus on Python coding."),
                            new Sample.Message(
                                    "assistant",
                                    "Coding is nice. But coding music is also fun! "
                                            + "Have you tried playing guitar?")))
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
                    .messages(List.of(
                            new Sample.Message("user", "I want to plan a healthy lifestyle."),
                            new Sample.Message(
                                    "assistant",
                                    "Great goal! A healthy lifestyle involves three key areas: "
                                            + "nutrition, exercise, and mental wellness."),
                            new Sample.Message("user", "Tell me more about each."),
                            new Sample.Message(
                                    "assistant",
                                    "For nutrition, focus on whole foods, vegetables, and lean proteins. "
                                            + "For exercise, aim for 30 minutes of activity daily - walking, swimming, or gym. "
                                            + "For mental wellness, practice meditation, get enough sleep, "
                                            + "and maintain social connections.")))
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
                    .messages(List.of(
                            new Sample.Message("user", "Explain the components of a modern web application."),
                            new Sample.Message(
                                    "assistant", "A modern web application uses JavaScript for interactivity."),
                            new Sample.Message("user", "What about other components?"),
                            new Sample.Message(
                                    "assistant",
                                    "JavaScript frameworks like React are very popular. "
                                            + "Many companies use React for their frontend.")))
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
                    .messages(List.of(
                            new Sample.Message("user", "How do I make a proper cup of tea?"),
                            new Sample.Message(
                                    "assistant",
                                    "To make perfect tea, start by boiling fresh water. "
                                            + "Warm your teapot, add one teaspoon of loose leaf tea per cup, "
                                            + "pour hot water and steep for 3-5 minutes."),
                            new Sample.Message("user", "What temperature should the water be?"),
                            new Sample.Message(
                                    "assistant",
                                    "For black tea, use water just off the boil (95-100°C). "
                                            + "Green tea prefers cooler water around 80°C to avoid bitterness.")))
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
                    .messages(List.of(
                            new Sample.Message("user", "What's the weather like?"),
                            new Sample.Message("assistant", "It's currently sunny with a temperature of 72°F (22°C).")))
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
}
