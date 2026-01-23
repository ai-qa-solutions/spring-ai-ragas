package ai.qa.solutions.metrics.nvidia.en;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.nvidia.AnswerAccuracyMetric;
import ai.qa.solutions.sample.Sample;
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
 * Integration tests for Answer Accuracy Metric (NVIDIA-style) - English Language.
 * <p>
 * Tests the evaluation of response accuracy against reference answers.
 * <p>
 * Key characteristics:
 * - Uses 0-2 scoring scale normalized to 0-1
 * - 0: Incorrect, 1: Partially correct, 2: Fully correct
 * - Supports dual-judge mode for higher reliability
 */
@Slf4j
@EnableAutoConfiguration
@SuppressWarnings("LoggingSimilarMessage")
@DisplayName("Answer Accuracy Metric - English Language Validation")
@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".*")
@SpringBootTest(classes = EnAnswerAccuracyIntegrationIT.AnswerAccuracyIntegrationTestConfiguration.class)
class EnAnswerAccuracyIntegrationIT {

    @Configuration
    public static class AnswerAccuracyIntegrationTestConfiguration {}

    @Autowired
    private AnswerAccuracyMetric answerAccuracyMetric;

    @Nested
    @DisplayName("Accuracy Evaluation Tests")
    class AccuracyEvaluationTests {

        @Test
        @DisplayName("Fully correct response - EXPECTED HIGH SCORE")
        void testFullyCorrectResponse() {
            log.info("=== Fully Correct Response Test ===");

            final Sample sample = Sample.builder()
                    .response("Paris is the capital of France.")
                    .reference("Paris is the capital of France.")
                    .build();

            final AnswerAccuracyMetric.AnswerAccuracyConfig config = AnswerAccuracyMetric.AnswerAccuracyConfig.builder()
                    .useDualJudge(false)
                    .build();

            final Double score = answerAccuracyMetric.singleTurnScore(config, sample);

            log.info("Response: Paris is the capital of France.");
            log.info("Reference: Paris is the capital of France.");
            log.info("Score: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.7, "Fully correct response should have high score. Received: " + score);
        }

        @Test
        @DisplayName("Partially correct response - EXPECTED MODERATE SCORE")
        void testPartiallyCorrectResponse() {
            log.info("=== Partially Correct Response Test ===");

            final Sample sample = Sample.builder()
                    .response("Paris is a major city in France, known for the Eiffel Tower.")
                    .reference("Paris is the capital and largest city of France, with a population of 2.1 million.")
                    .build();

            final AnswerAccuracyMetric.AnswerAccuracyConfig config = AnswerAccuracyMetric.AnswerAccuracyConfig.builder()
                    .useDualJudge(false)
                    .build();

            final Double score = answerAccuracyMetric.singleTurnScore(config, sample);

            log.info("Response: Paris is a major city in France...");
            log.info("Reference: Paris is the capital and largest city of France...");
            log.info("Score: {}", score);

            assertNotNull(score);
            assertTrue(
                    score >= 0.2 && score <= 0.9,
                    "Partially correct response should have moderate score. Received: " + score);
        }

        @Test
        @DisplayName("Incorrect response - EXPECTED LOW SCORE")
        void testIncorrectResponse() {
            log.info("=== Incorrect Response Test ===");

            final Sample sample = Sample.builder()
                    .response("Berlin is the capital of France.")
                    .reference("Paris is the capital of France.")
                    .build();

            final AnswerAccuracyMetric.AnswerAccuracyConfig config = AnswerAccuracyMetric.AnswerAccuracyConfig.builder()
                    .useDualJudge(false)
                    .build();

            final Double score = answerAccuracyMetric.singleTurnScore(config, sample);

            log.info("Response: Berlin is the capital of France.");
            log.info("Reference: Paris is the capital of France.");
            log.info("Score: {}", score);

            assertNotNull(score);
            assertTrue(score <= 0.4, "Incorrect response should have low score. Received: " + score);
        }
    }

    @Nested
    @DisplayName("Dual Judge Mode Tests")
    class DualJudgeModeTests {

        @Test
        @DisplayName("Dual judge mode evaluation")
        void testDualJudgeMode() {
            log.info("=== Dual Judge Mode Test ===");

            final Sample sample = Sample.builder()
                    .response("The Eiffel Tower is a famous landmark in Paris, France.")
                    .reference("The Eiffel Tower is an iron lattice tower located on the Champ de Mars in Paris.")
                    .build();

            final AnswerAccuracyMetric.AnswerAccuracyConfig config = AnswerAccuracyMetric.AnswerAccuracyConfig.builder()
                    .useDualJudge(true)
                    .build();

            final Double score = answerAccuracyMetric.singleTurnScore(config, sample);

            log.info("Dual Judge Mode enabled");
            log.info("Score: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.3, "Dual judge should evaluate correctly. Received: " + score);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Technical response with technical reference")
        void testTechnicalContent() {
            log.info("=== Technical Content Test ===");

            final Sample sample = Sample.builder()
                    .response("TCP uses a three-way handshake: SYN, SYN-ACK, ACK.")
                    .reference(
                            "The TCP three-way handshake consists of SYN, SYN-ACK, and ACK packets to establish a connection.")
                    .build();

            final AnswerAccuracyMetric.AnswerAccuracyConfig config = AnswerAccuracyMetric.AnswerAccuracyConfig.builder()
                    .useDualJudge(false)
                    .build();

            final Double score = answerAccuracyMetric.singleTurnScore(config, sample);

            log.info("Response: TCP uses a three-way handshake...");
            log.info("Score: {}", score);

            assertNotNull(score);
            assertTrue(
                    score >= 0.6, "Semantically equivalent technical response should score well. Received: " + score);
        }

        @Test
        @DisplayName("Async scoring works correctly")
        void testAsyncScoring() {
            log.info("=== Async Scoring Test ===");

            final Sample sample = Sample.builder()
                    .response("Water boils at 100 degrees Celsius.")
                    .reference("The boiling point of water is 100°C at sea level.")
                    .build();

            final AnswerAccuracyMetric.AnswerAccuracyConfig config = AnswerAccuracyMetric.AnswerAccuracyConfig.builder()
                    .useDualJudge(false)
                    .build();

            final Double score =
                    answerAccuracyMetric.singleTurnScoreAsync(config, sample).join();

            log.info("Async Score: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.6, "Async scoring should work correctly. Received: " + score);
        }
    }
}
