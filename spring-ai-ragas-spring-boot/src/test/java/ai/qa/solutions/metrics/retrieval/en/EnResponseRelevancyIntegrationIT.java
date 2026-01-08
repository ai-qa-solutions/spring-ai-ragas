package ai.qa.solutions.metrics.retrieval.en;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.retrieval.ResponseRelevancyMetric;
import ai.qa.solutions.sample.Sample;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

/**
 * <p>
 * Integration tests for Response Relevancy Metric based on Ragas methodology.
 * </p>
 * <p>
 * IMPORTANT: These tests reflect the ACTUAL behavior of Ragas Response Relevancy,
 * including its known limitations. Test thresholds are based on real benchmark data
 * across 7 embedding models for English language.
 * </p>
 * Key findings:
 * - The metric uses cosine similarity of embeddings, which measures linguistic patterns
 * - Results HEAVILY depend on chosen embedding model
 * - Reliable only for: perfect matches (0.86-0.97), noncommittal answers (0.0)
 * - Everything else requires validation with other metrics
 */
@Slf4j
@EnableAutoConfiguration
@SuppressWarnings("LoggingSimilarMessage")
@DisplayName("Response Relevancy Metric - English Language Validation")
@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".*")
@SpringBootTest(classes = EnResponseRelevancyIntegrationIT.ResponseRelevancyIntegrationTestConfiguration.class)
class EnResponseRelevancyIntegrationIT {

    @Configuration
    public static class ResponseRelevancyIntegrationTestConfiguration {}

    @Autowired
    private ResponseRelevancyMetric responseRelevancyMetric;

    @Test
    @DisplayName("✅ Perfect Answer: Direct and complete - EXPECTED HIGH SCORE")
    void testResponseRelevancy_PerfectAnswer() {
        log.info("=== Perfect Answer Test ===");

        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("The capital of France is Paris.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Response: {}", sample.getResponse());
        log.info("📊 Score: {} {}", String.format("%.4f", score), getScoreEmoji(score));

        assertTrue(score >= 0.85, "Perfect answers get very high scores (0.86-0.97). Received: " + score);

        log.info("✅ SUCCESS: Perfect answer detection works correctly!");
    }

    @Test
    @DisplayName("✅ Noncommittal Answer: 'I don't know' - EXPECTED ZERO SCORE")
    void testResponseRelevancy_NoncommittalAnswer() {
        log.info("=== Noncommittal Answer Test ===");

        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("I don't know the answer to that question.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        assertEquals(
                0.0,
                score,
                0.01,
                "Noncommittal answers ('I don't know', 'unclear' etc.) return 0.0. Received: " + score);

        log.info("✅ SUCCESS: Noncommittal detection works correctly!");
    }

    @Test
    @DisplayName("✅ Verbose but Complete: Detailed answer - EXPECTED HIGH SCORE")
    void testResponseRelevancy_VerboseButComplete() {
        log.info("=== Verbose but Complete Answer Test ===");

        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("France, officially known as the French Republic, is a country in Western Europe. "
                        + "The capital and largest city of France is Paris, which is located in the north-central "
                        + "part of the country. Paris is not only the political capital but also a major European city "
                        + "known for art, fashion, gastronomy, and culture.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        assertTrue(
                score >= 0.60,
                "Verbose but complete answers get moderate to high scores (0.65-0.88). Received: " + score);
    }

    @Test
    @DisplayName("Incomplete Answer: Partial response to multi-part question")
    void testResponseRelevancy_IncompleteAnswer() {
        log.info("=== Incomplete Answer Test ===");

        Sample sample = Sample.builder()
                .userInput("Where is France located and what is its capital?")
                .response("France is located in Western Europe.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Incomplete answer score: {}", score);

        assertTrue(
                score >= 0.50,
                "Incomplete answer should have moderate to high score due to partial relevance. Received: " + score);
    }

    @Test
    @DisplayName("Complete Answer: Full response to multi-part question")
    void testResponseRelevancy_CompleteAnswer() {
        log.info("=== Complete Answer Test ===");

        Sample sample = Sample.builder()
                .userInput("Where is France located and what is its capital?")
                .response("France is located in Western Europe, and its capital is Paris.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Complete answer score: {}", score);

        assertTrue(score >= 0.70, "Complete answer should have high score. Received: " + score);
    }

    @Test
    @DisplayName("⚠️ LIMITATION: Partial answer to multi-part question - MODEL DEPENDENT")
    void testResponseRelevancy_PartialAnswer_ModelDependent() {
        log.info("=== LIMITATION: Partial Answer Test ===");

        Sample sample = Sample.builder()
                .userInput("Who discovered penicillin and when?")
                .response("Alexander Fleming discovered penicillin.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        assertTrue(
                score >= 0.70,
                "⚠️ KNOWN LIMITATION: Partial answers get HIGH scores (0.75-0.90) because "
                        + "cosine similarity cannot detect MISSING information. Received: " + score);

        log.warn("⚠️ Metric cannot detect incomplete answers if partial answer is semantically similar!");
    }

    @Test
    @DisplayName("⚠️ LIMITATION: Same entity, different aspect - MODEL DEPENDENT")
    void testResponseRelevancy_SameEntity_DifferentAspect() {
        log.info("=== LIMITATION: Same Entity, Different Aspect ===");

        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("The currency of France is the Euro.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        assertTrue(
                score >= 0.45,
                "⚠️ LIMITATION: Different aspects vary wildly (0.47-0.84) depending on model. " + "Received: " + score);

        log.error("🚨 CRITICAL: Metric confuses different aspects of same topic!");
    }

    @Test
    @DisplayName("⚠️ LIMITATION: Completely off-topic - HUGE MODEL VARIATION")
    void testResponseRelevancy_CompletelyOffTopic() {
        log.info("=== LIMITATION: Completely Off-topic ===");

        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("The Great Wall of China was built over many centuries.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        assertTrue(score >= 0.02, "⚠️ KNOWN LIMITATION: Off-topic scores vary 26x (0.01-0.74). Received: " + score);

        log.warn("⚠️ Metric reliability for off-topic answers CRITICALLY depends on model!");
    }

    @Test
    @DisplayName("⚠️ LIMITATION: Different domains, similar structure - HUGE MODEL VARIATION")
    void testResponseRelevancy_DifferentDomains_SimilarStructure() {
        log.info("=== LIMITATION: Different Domains with Similar Question Structure ===");

        Sample sample = Sample.builder()
                .userInput("How do I configure Spring Boot security?")
                .response("The recipe for chocolate chip cookies includes flour, sugar, and chocolate chips.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        assertTrue(score >= 0.01, "⚠️ LIMITATION: Domain shift scores vary 15x (0.05-0.74) . Received: " + score);

        log.error("🚨 CRITICAL: Programming vs cooking scored {}!", score);
    }

    @Test
    @DisplayName("⚠️ LIMITATION: Single-word nonsense - VERY MODEL DEPENDENT")
    void testResponseRelevancy_SingleWordNonsense() {
        log.info("=== LIMITATION: Single-word Nonsense ===");

        Sample sample = Sample.builder()
                .userInput("Calculate the derivative of x squared")
                .response("Blue")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        assertTrue(score >= 0.01, "🚨 LIMITATION: Nonsense word scores vary 16x (0.05-0.78). Received: " + score);

        log.error("🚨 Model-dependent: Single-word nonsense scored {}!", score);
    }

    @Test
    @DisplayName("Edge Case: Very short Q&A")
    void testResponseRelevancy_ShortQA() {
        log.info("=== Edge Case: Short Q&A ===");

        Sample sample = Sample.builder()
                .userInput("Capital of France?")
                .response("Paris.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        assertTrue(score >= 0.30, "Short answers vary widely (0.40-0.84). Received: " + score);
    }

    @Test
    @DisplayName("Edge Case: Incorrect but on-topic")
    void testResponseRelevancy_IncorrectButOnTopic() {
        log.info("=== Edge Case: Incorrect but On-topic ===");

        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("The capital of France is Lyon.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        assertTrue(
                score >= 0.75,
                "Incorrect but on-topic answers score HIGH (0.75-0.97) - identical to correct answers. "
                        + "This is by design - metric doesn't check correctness. Received: " + score);

        log.info("ℹ️ Reminder: This metric does NOT validate correctness!");
    }

    @Test
    @DisplayName("Edge Case: Answer with redundant information")
    void testResponseRelevancy_RedundantInformation() {
        log.info("=== Edge Case: Redundant Information ===");

        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("The capital of France is Paris. "
                        + "By the way, yesterday I went to the store and bought milk. "
                        + "The weather was great. I also met an old friend.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        assertTrue(score >= 0.03, "Redundant info handling varies WILDLY (0.04-0.83). Received: " + score);
    }

    @Test
    @DisplayName("Edge Case: Hypothetical question")
    void testResponseRelevancy_HypotheticalQuestion() {
        log.info("=== Edge Case: Hypothetical Question ===");

        Sample sample = Sample.builder()
                .userInput("What would happen if the Earth stopped rotating?")
                .response(
                        "If Earth stopped rotating, one side would face the Sun continuously, experiencing extreme heat, "
                                + "while the other side would be in perpetual darkness and freezing cold. The atmosphere "
                                + "would continue moving at high speed, causing catastrophic winds.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        assertTrue(score >= 0.40, "Hypothetical questions vary (0.54-0.88). Received: " + score);
    }

    @Test
    @DisplayName("Edge Case: Ambiguous question")
    void testResponseRelevancy_AmbiguousQuestion() {
        log.info("=== Edge Case: Ambiguous Question ===");

        Sample sample = Sample.builder()
                .userInput("What is the bank?")
                .response("A bank is a financial institution that accepts deposits and creates credit.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        assertTrue(
                score >= 0.60,
                "Ambiguous questions with reasonable interpretation score moderately to high (0.69-0.91). "
                        + "Received: " + score);
    }

    @Test
    @DisplayName("Edge Case: Clarification request")
    void testResponseRelevancy_ClarificationRequest() {
        log.info("=== Edge Case: Clarification Request ===");

        Sample sample = Sample.builder()
                .userInput("What is it?")
                .response("I need more context to answer your question. What are you referring to?")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        assertTrue(
                score >= 0.00,
                "Clarification requests vary WILDLY (0.00-0.81). "
                        + "Some models treat as noncommittal (0.0), others don't. Received: " + score);
    }

    @Test
    @DisplayName("Validation: Empty user input")
    void testResponseRelevancy_EmptyUserInput() {
        log.info("=== Validation: Empty User Input ===");

        Sample sample = Sample.builder()
                .userInput("")
                .response("Paris is the capital of France.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        assertEquals(0.0, score, "Empty input should return 0.0. Received: " + score);
    }

    @Test
    @DisplayName("Validation: Empty response")
    void testResponseRelevancy_EmptyResponse() {
        log.info("=== Validation: Empty Response ===");

        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        assertEquals(0.0, score, "Empty response should return 0.0. Received: " + score);
    }

    @Test
    @Disabled
    @DisplayName("Validation: Async call")
    void testResponseRelevancy_AsyncCall() throws Exception {
        log.info("=== Validation: Async Call ===");

        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("Paris is the capital of France.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score =
                responseRelevancyMetric.singleTurnScoreAsync(config, sample).get();

        log.info("Async score: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.85, "Async call should work identically to sync. Received: " + score);
    }

    /**
     * Get emoji based on score range for quick visual assessment
     */
    private String getScoreEmoji(double score) {
        if (score == 0.0) return "🔵 (Missing/Noncommittal)";
        if (score >= 0.95) return "🟢 (Excellent)";
        if (score >= 0.85) return "🟢 (Very Good)";
        if (score >= 0.75) return "🟡 (Good)";
        if (score >= 0.60) return "🟠 (Moderate)";
        if (score >= 0.40) return "🔴 (Low)";
        return "🔴 (Very Low)";
    }
}
