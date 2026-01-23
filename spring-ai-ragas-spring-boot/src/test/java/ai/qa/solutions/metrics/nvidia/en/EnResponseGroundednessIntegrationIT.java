package ai.qa.solutions.metrics.nvidia.en;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.nvidia.ResponseGroundednessMetric;
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
 * Integration tests for Response Groundedness Metric (NVIDIA-style) - English Language.
 * <p>
 * Tests the evaluation of whether the response is grounded in the retrieved contexts.
 * <p>
 * Key characteristics:
 * - Uses 0-2 scoring scale normalized to 0-1
 * - 0: Not grounded, 1: Partially grounded, 2: Fully grounded
 * - Supports heuristic shortcuts for exact matches
 */
@Slf4j
@EnableAutoConfiguration
@SuppressWarnings("LoggingSimilarMessage")
@DisplayName("Response Groundedness Metric - English Language Validation")
@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".*")
@SpringBootTest(classes = EnResponseGroundednessIntegrationIT.ResponseGroundednessIntegrationTestConfiguration.class)
class EnResponseGroundednessIntegrationIT {

    @Configuration
    public static class ResponseGroundednessIntegrationTestConfiguration {}

    @Autowired
    private ResponseGroundednessMetric responseGroundednessMetric;

    @Nested
    @DisplayName("Groundedness Evaluation Tests")
    class GroundednessEvaluationTests {

        @Test
        @DisplayName("Fully grounded response - EXPECTED HIGH SCORE")
        void testFullyGroundedResponse() {
            log.info("=== Fully Grounded Response Test ===");

            final Sample sample = Sample.builder()
                    .response("Paris is the capital of France.")
                    .retrievedContexts(List.of(
                            "France is a country in Western Europe. Paris is the capital and largest city of France. "
                                    + "The country has a population of about 67 million people."))
                    .build();

            final ResponseGroundednessMetric.ResponseGroundednessConfig config =
                    ResponseGroundednessMetric.ResponseGroundednessConfig.builder()
                            .useHeuristicShortcuts(false)
                            .build();

            final Double score = responseGroundednessMetric.singleTurnScore(config, sample);

            log.info("Response: Paris is the capital of France.");
            log.info("Score: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.7, "Fully grounded response should have high score. Received: " + score);
        }

        @Test
        @DisplayName("Partially grounded response - EXPECTED MODERATE SCORE")
        void testPartiallyGroundedResponse() {
            log.info("=== Partially Grounded Response Test ===");

            final Sample sample = Sample.builder()
                    .response(
                            "Paris is the beautiful capital of France with amazing restaurants and romantic atmosphere.")
                    .retrievedContexts(List.of("Paris is the capital of France. It is located along the Seine River."))
                    .build();

            final ResponseGroundednessMetric.ResponseGroundednessConfig config =
                    ResponseGroundednessMetric.ResponseGroundednessConfig.builder()
                            .useHeuristicShortcuts(false)
                            .build();

            final Double score = responseGroundednessMetric.singleTurnScore(config, sample);

            log.info("Response: Paris is the beautiful capital of France with amazing restaurants...");
            log.info("Score: {}", score);

            assertNotNull(score);
            assertTrue(
                    score >= 0.2 && score <= 0.8,
                    "Partially grounded response should have moderate score. Received: " + score);
        }

        @Test
        @DisplayName("Ungrounded response - EXPECTED LOW SCORE")
        void testUngroundedResponse() {
            log.info("=== Ungrounded Response Test ===");

            final Sample sample = Sample.builder()
                    .response("Berlin is the capital of Germany and has a population of 3.6 million.")
                    .retrievedContexts(List.of("Paris is the capital of France. It is known for the Eiffel Tower."))
                    .build();

            final ResponseGroundednessMetric.ResponseGroundednessConfig config =
                    ResponseGroundednessMetric.ResponseGroundednessConfig.builder()
                            .useHeuristicShortcuts(false)
                            .build();

            final Double score = responseGroundednessMetric.singleTurnScore(config, sample);

            log.info("Response: Berlin is the capital of Germany...");
            log.info("Score: {}", score);

            assertNotNull(score);
            assertTrue(score <= 0.4, "Ungrounded response should have low score. Received: " + score);
        }
    }

    @Nested
    @DisplayName("Heuristic Shortcuts Tests")
    class HeuristicShortcutsTests {

        @Test
        @DisplayName("Exact match response - EXPECTED PERFECT SCORE")
        void testExactMatchResponse() {
            log.info("=== Exact Match Response Test ===");

            final Sample sample = Sample.builder()
                    .response("Paris is the capital of France")
                    .retrievedContexts(List.of("Paris is the capital of France"))
                    .build();

            final ResponseGroundednessMetric.ResponseGroundednessConfig config =
                    ResponseGroundednessMetric.ResponseGroundednessConfig.builder()
                            .useHeuristicShortcuts(true)
                            .build();

            final Double score = responseGroundednessMetric.singleTurnScore(config, sample);

            log.info("Response matches context exactly");
            log.info("Score: {}", score);

            assertNotNull(score);
            assertTrue(score == 1.0, "Exact match should return 1.0. Received: " + score);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Technical response with technical context")
        void testTechnicalContent() {
            log.info("=== Technical Content Test ===");

            final Sample sample = Sample.builder()
                    .response("TCP uses a three-way handshake: SYN, SYN-ACK, ACK to establish a connection.")
                    .retrievedContexts(
                            List.of("The TCP three-way handshake is a method used to establish a connection. "
                                    + "It involves three steps: First, the client sends a SYN packet. "
                                    + "Second, the server responds with SYN-ACK. Third, the client sends ACK."))
                    .build();

            final ResponseGroundednessMetric.ResponseGroundednessConfig config =
                    ResponseGroundednessMetric.ResponseGroundednessConfig.builder()
                            .useHeuristicShortcuts(false)
                            .build();

            final Double score = responseGroundednessMetric.singleTurnScore(config, sample);

            log.info("Response: TCP uses a three-way handshake...");
            log.info("Score: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.6, "Technical response grounded in context should score well. Received: " + score);
        }

        @Test
        @DisplayName("Async scoring works correctly")
        void testAsyncScoring() {
            log.info("=== Async Scoring Test ===");

            final Sample sample = Sample.builder()
                    .response("The Eiffel Tower is in Paris")
                    .retrievedContexts(List.of("The Eiffel Tower is a famous landmark located in Paris, France."))
                    .build();

            final ResponseGroundednessMetric.ResponseGroundednessConfig config =
                    ResponseGroundednessMetric.ResponseGroundednessConfig.builder()
                            .useHeuristicShortcuts(false)
                            .build();

            final Double score = responseGroundednessMetric
                    .singleTurnScoreAsync(config, sample)
                    .join();

            log.info("Async Score: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.6, "Async scoring should work correctly. Received: " + score);
        }
    }
}
