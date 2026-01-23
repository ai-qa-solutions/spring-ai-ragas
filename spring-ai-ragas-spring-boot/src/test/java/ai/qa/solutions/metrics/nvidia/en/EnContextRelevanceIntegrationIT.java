package ai.qa.solutions.metrics.nvidia.en;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.nvidia.ContextRelevanceMetric;
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
 * Integration tests for Context Relevance Metric (NVIDIA-style) - English Language.
 * <p>
 * Tests the evaluation of whether retrieved contexts are relevant to the user's question.
 * <p>
 * Key characteristics:
 * - Uses 0-2 scoring scale normalized to 0-1
 * - 0: Not relevant, 1: Partially relevant, 2: Fully relevant
 * - Returns averaged score across all contexts
 */
@Slf4j
@EnableAutoConfiguration
@SuppressWarnings("LoggingSimilarMessage")
@DisplayName("Context Relevance Metric - English Language Validation")
@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".*")
@SpringBootTest(classes = EnContextRelevanceIntegrationIT.ContextRelevanceIntegrationTestConfiguration.class)
class EnContextRelevanceIntegrationIT {

    @Configuration
    public static class ContextRelevanceIntegrationTestConfiguration {}

    @Autowired
    private ContextRelevanceMetric contextRelevanceMetric;

    @Nested
    @DisplayName("Relevance Evaluation Tests")
    class RelevanceEvaluationTests {

        @Test
        @DisplayName("Highly relevant context - EXPECTED HIGH SCORE")
        void testHighlyRelevantContext() {
            log.info("=== Highly Relevant Context Test ===");

            final Sample sample = Sample.builder()
                    .userInput("What is machine learning and how does it work?")
                    .retrievedContexts(List.of(
                            "Machine learning is a subset of artificial intelligence that enables systems to automatically "
                                    + "learn and improve from experience without being explicitly programmed. It works by "
                                    + "using algorithms that iteratively learn from data, allowing computers to find hidden "
                                    + "insights without being explicitly programmed where to look. The learning process "
                                    + "begins with observations or data, such as examples, direct experience, or instruction, "
                                    + "in order to look for patterns in data and make better decisions in the future."))
                    .build();

            final ContextRelevanceMetric.ContextRelevanceConfig config =
                    ContextRelevanceMetric.ContextRelevanceConfig.builder().build();

            final Double score = contextRelevanceMetric.singleTurnScore(config, sample);

            log.info("Question: What is machine learning and how does it work?");
            log.info("Score: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.7, "Highly relevant context should have high score. Received: " + score);
        }

        @Test
        @DisplayName("Partially relevant context - EXPECTED MODERATE SCORE")
        void testPartiallyRelevantContext() {
            log.info("=== Partially Relevant Context Test ===");

            final Sample sample = Sample.builder()
                    .userInput("What are the health benefits of green tea?")
                    .retrievedContexts(
                            List.of("Tea is one of the most popular beverages in the world. It comes in many varieties "
                                    + "including black tea, green tea, white tea, and oolong tea. Each type has its own "
                                    + "distinct flavor profile and is produced through different processing methods. "
                                    + "Green tea originated in China and has been consumed for thousands of years."))
                    .build();

            final ContextRelevanceMetric.ContextRelevanceConfig config =
                    ContextRelevanceMetric.ContextRelevanceConfig.builder().build();

            final Double score = contextRelevanceMetric.singleTurnScore(config, sample);

            log.info("Question: What are the health benefits of green tea?");
            log.info("Score: {}", score);

            assertNotNull(score);
            // Context mentions green tea but doesn't discuss health benefits specifically
            // LLM may score this as 0 (not relevant) or 0.5 (partially relevant)
            assertTrue(
                    score <= 0.8, "Partially relevant context should have moderate or low score. Received: " + score);
        }

        @Test
        @DisplayName("Irrelevant context - EXPECTED LOW SCORE")
        void testIrrelevantContext() {
            log.info("=== Irrelevant Context Test ===");

            final Sample sample = Sample.builder()
                    .userInput("How do I bake chocolate chip cookies?")
                    .retrievedContexts(
                            List.of(
                                    "The stock market experienced significant volatility today as investors reacted to "
                                            + "new economic data. Major indices fluctuated throughout the trading session, "
                                            + "with technology stocks leading the gains while energy sector faced pressure. "
                                            + "Analysts are closely watching for the Federal Reserve's upcoming policy decision."))
                    .build();

            final ContextRelevanceMetric.ContextRelevanceConfig config =
                    ContextRelevanceMetric.ContextRelevanceConfig.builder().build();

            final Double score = contextRelevanceMetric.singleTurnScore(config, sample);

            log.info("Question: How do I bake chocolate chip cookies?");
            log.info("Score: {}", score);

            assertNotNull(score);
            assertTrue(score <= 0.4, "Irrelevant context should have low score. Received: " + score);
        }
    }

    @Nested
    @DisplayName("Multiple Contexts Tests")
    class MultipleContextsTests {

        @Test
        @DisplayName("Mixed relevance contexts - EXPECTED MODERATE SCORE")
        void testMixedRelevanceContexts() {
            log.info("=== Mixed Relevance Contexts Test ===");

            final Sample sample = Sample.builder()
                    .userInput("What is Python programming language?")
                    .retrievedContexts(
                            List.of(
                                    "Python is a high-level, interpreted programming language known for its simplicity "
                                            + "and readability. It was created by Guido van Rossum and first released in 1991. "
                                            + "Python supports multiple programming paradigms including procedural, object-oriented, "
                                            + "and functional programming.",
                                    "The weather forecast for tomorrow shows partly cloudy skies with a high of 75°F. "
                                            + "There's a 20% chance of afternoon showers in the western regions.",
                                    "Python syntax emphasizes code readability with the use of significant indentation. "
                                            + "Its design philosophy emphasizes code readability with its use of significant whitespace."))
                    .build();

            final ContextRelevanceMetric.ContextRelevanceConfig config =
                    ContextRelevanceMetric.ContextRelevanceConfig.builder().build();

            final Double score = contextRelevanceMetric.singleTurnScore(config, sample);

            log.info("Question: What is Python programming language?");
            log.info("Score: {}", score);

            assertNotNull(score);
            // 2 relevant + 1 irrelevant contexts should give moderate average
            assertTrue(score >= 0.4 && score <= 0.9, "Mixed contexts should have moderate score. Received: " + score);
        }

        @Test
        @DisplayName("All highly relevant contexts - EXPECTED HIGH SCORE")
        void testAllHighlyRelevantContexts() {
            log.info("=== All Highly Relevant Contexts Test ===");

            final Sample sample = Sample.builder()
                    .userInput("What are the symptoms of vitamin D deficiency?")
                    .retrievedContexts(List.of(
                            "Vitamin D deficiency symptoms include fatigue, bone pain, muscle weakness, and mood changes "
                                    + "including depression. People with low vitamin D may also experience getting sick more "
                                    + "often and have wounds that heal slowly.",
                            "Risk factors for vitamin D deficiency include limited sun exposure, dark skin, obesity, "
                                    + "and age over 65. Certain medical conditions like celiac disease and inflammatory bowel "
                                    + "disease can also affect vitamin D absorption."))
                    .build();

            final ContextRelevanceMetric.ContextRelevanceConfig config =
                    ContextRelevanceMetric.ContextRelevanceConfig.builder().build();

            final Double score = contextRelevanceMetric.singleTurnScore(config, sample);

            log.info("Question: What are the symptoms of vitamin D deficiency?");
            log.info("Score: {}", score);

            assertNotNull(score);
            // First context discusses symptoms directly, second discusses risk factors (related but not symptoms)
            // Average may be moderate rather than high
            assertTrue(score >= 0.4, "Relevant contexts about vitamin D should have decent score. Received: " + score);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Single word question with relevant context")
        void testSingleWordQuestion() {
            log.info("=== Single Word Question Test ===");

            final Sample sample = Sample.builder()
                    .userInput("Photosynthesis?")
                    .retrievedContexts(List.of(
                            "Photosynthesis is the process by which plants convert light energy, usually from the sun, "
                                    + "into chemical energy that can be later released to fuel the plant's activities. "
                                    + "It occurs primarily in the leaves of plants and involves the conversion of carbon "
                                    + "dioxide and water into glucose and oxygen."))
                    .build();

            final ContextRelevanceMetric.ContextRelevanceConfig config =
                    ContextRelevanceMetric.ContextRelevanceConfig.builder().build();

            final Double score = contextRelevanceMetric.singleTurnScore(config, sample);

            log.info("Question: Photosynthesis?");
            log.info("Score: {}", score);

            assertNotNull(score);
            assertTrue(
                    score >= 0.5, "Single word question with relevant context should score well. Received: " + score);
        }

        @Test
        @DisplayName("Technical question with technical context")
        void testTechnicalContent() {
            log.info("=== Technical Content Test ===");

            final Sample sample = Sample.builder()
                    .userInput("How does TCP/IP three-way handshake work?")
                    .retrievedContexts(List.of(
                            "The TCP three-way handshake is the method used by TCP to establish a connection between "
                                    + "a client and server. It involves three steps: First, the client sends a SYN "
                                    + "(synchronize) packet to the server. Second, the server responds with a SYN-ACK "
                                    + "(synchronize-acknowledge) packet. Third, the client sends an ACK (acknowledge) "
                                    + "packet back to the server, establishing the connection."))
                    .build();

            final ContextRelevanceMetric.ContextRelevanceConfig config =
                    ContextRelevanceMetric.ContextRelevanceConfig.builder().build();

            final Double score = contextRelevanceMetric.singleTurnScore(config, sample);

            log.info("Question: How does TCP/IP three-way handshake work?");
            log.info("Score: {}", score);

            assertNotNull(score);
            assertTrue(
                    score >= 0.7,
                    "Technical question with matching technical context should have high score. Received: " + score);
        }

        @Test
        @DisplayName("Async scoring works correctly")
        void testAsyncScoring() {
            log.info("=== Async Scoring Test ===");

            final Sample sample = Sample.builder()
                    .userInput("What is the capital of France?")
                    .retrievedContexts(List.of(
                            "Paris is the capital and most populous city of France. It is located in north-central "
                                    + "France along the Seine River. Paris is known for its museums, architecture, and "
                                    + "cultural significance."))
                    .build();

            final ContextRelevanceMetric.ContextRelevanceConfig config =
                    ContextRelevanceMetric.ContextRelevanceConfig.builder().build();

            final Double score =
                    contextRelevanceMetric.singleTurnScoreAsync(config, sample).join();

            log.info("Async Score: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.6, "Async scoring should work correctly. Received: " + score);
        }
    }
}
