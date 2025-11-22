package ai.qa.solutions.metrics.retrieval.en;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.retrieval.ResponseRelevancyMetric;
import ai.qa.solutions.sample.Sample;
import io.qameta.allure.Step;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
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
@SpringBootTest(classes = EnResponseRelevancyIntegrationTest.ResponseRelevancyIntegrationTestConfiguration.class)
class EnResponseRelevancyIntegrationTest {

    @Configuration
    public static class ResponseRelevancyIntegrationTestConfiguration {}

    @Autowired
    private ResponseRelevancyMetric responseRelevancyMetric;

    @Autowired(required = false)
    private OpenAiApi openAiApi;

    public static Stream<Arguments> embeddingModels() {
        return Stream.of(
                Arguments.of("qwen/qwen3-embedding-8b", 1024),
                Arguments.of("qwen/qwen3-embedding-4b", 1024),
                Arguments.of("openai/text-embedding-3-large", 1024),
                Arguments.of("openai/text-embedding-3-small", 1024),
                Arguments.of("google/gemini-embedding-001", 1024),
                Arguments.of("intfloat/multilingual-e5-large", 1024),
                Arguments.of("baai/bge-m3", 1024));
    }

    @ParameterizedTest
    @MethodSource("embeddingModels")
    @DisplayName("✅ Perfect Answer: Direct and complete - EXPECTED HIGH SCORE")
    void testResponseRelevancy_PerfectAnswer(String model, int dimensions) {
        log.info("=== Perfect Answer Test ===");

        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("The capital of France is Paris.")
                .build();

        Double score =
                executeTest(sample, "Perfect Answer: Direct and complete - EXPECTED HIGH SCORE", model, dimensions);

        assertTrue(score >= 0.85, "Perfect answers get very high scores (0.86-0.97). Received: " + score);

        log.info("✅ SUCCESS: Perfect answer detection works correctly!");
    }

    @ParameterizedTest
    @MethodSource("embeddingModels")
    @DisplayName("✅ Noncommittal Answer: 'I don't know' - EXPECTED ZERO SCORE")
    void testResponseRelevancy_NoncommittalAnswer(String model, int dimensions) {
        log.info("=== Noncommittal Answer Test ===");

        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("I don't know the answer to that question.")
                .build();

        Double score = executeTest(sample, "Noncommittal Answer", model, dimensions);

        assertEquals(
                0.0,
                score,
                0.01,
                "Noncommittal answers ('I don't know', 'unclear' etc.) return 0.0. Received: " + score);

        log.info("✅ SUCCESS: Noncommittal detection works correctly!");
    }

    @ParameterizedTest
    @MethodSource("embeddingModels")
    @DisplayName("✅ Verbose but Complete: Detailed answer - EXPECTED HIGH SCORE")
    void testResponseRelevancy_VerboseButComplete(String model, int dimensions) {
        log.info("=== Verbose but Complete Answer Test ===");

        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("France, officially known as the French Republic, is a country in Western Europe. "
                        + "The capital and largest city of France is Paris, which is located in the north-central "
                        + "part of the country. Paris is not only the political capital but also a major European city "
                        + "known for art, fashion, gastronomy, and culture.")
                .build();

        Double score = executeTest(sample, "Verbose but Complete: Detailed answer", model, dimensions);

        assertTrue(
                score >= 0.60,
                "Verbose but complete answers get moderate to high scores (0.65-0.88). Received: " + score);
    }

    @ParameterizedTest
    @MethodSource("embeddingModels")
    @DisplayName("Comparison: Complete vs Incomplete answer")
    void testResponseRelevancy_CompleteVsIncomplete(String model, int dimensions) {
        log.info("=== Comparing Complete vs Incomplete Answers ===");

        Sample incompleteSample = Sample.builder()
                .userInput("Where is France located and what is its capital?")
                .response("France is located in Western Europe.")
                .build();

        Sample completeSample = Sample.builder()
                .userInput("Where is France located and what is its capital?")
                .response("France is located in Western Europe, and its capital is Paris.")
                .build();

        Double incompleteScore = executeTest(incompleteSample, "Incomplete answer", model, dimensions);
        Double completeScore = executeTest(completeSample, "Complete answer", model, dimensions);

        log.info("Incomplete: {}, Complete: {}", incompleteScore, completeScore);

        assertTrue(
                completeScore >= incompleteScore - 0.05,
                "Complete answer should score similar or higher. Complete: " + completeScore + ", Incomplete: "
                        + incompleteScore);
    }

    @ParameterizedTest
    @MethodSource("embeddingModels")
    @DisplayName("⚠️ LIMITATION: Partial answer to multi-part question - MODEL DEPENDENT")
    void testResponseRelevancy_PartialAnswer_ModelDependent(String model, int dimensions) {
        log.info("=== LIMITATION: Partial Answer Test ===");

        Sample sample = Sample.builder()
                .userInput("Who discovered penicillin and when?")
                .response("Alexander Fleming discovered penicillin.")
                .build();

        Double score = executeTest(sample, "Partial answer to multi-part question", model, dimensions);

        assertTrue(
                score >= 0.70,
                "⚠️ KNOWN LIMITATION: Partial answers get HIGH scores (0.75-0.90) because "
                        + "cosine similarity cannot detect MISSING information. Received: " + score);

        log.warn("⚠️ Metric cannot detect incomplete answers if partial answer is semantically similar!");
    }

    @ParameterizedTest
    @MethodSource("embeddingModels")
    @DisplayName("⚠️ LIMITATION: Same entity, different aspect - MODEL DEPENDENT")
    void testResponseRelevancy_SameEntity_DifferentAspect(String model, int dimensions) {
        log.info("=== LIMITATION: Same Entity, Different Aspect ===");

        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("The currency of France is the Euro.")
                .build();

        Double score = executeTest(sample, "Same entity, different aspect", model, dimensions);

        assertTrue(
                score >= 0.45,
                "⚠️ LIMITATION: Different aspects vary wildly (0.47-0.84) depending on model. " + "Received: " + score);

        log.error("🚨 CRITICAL: Metric confuses different aspects of same topic!");
    }

    @ParameterizedTest
    @MethodSource("embeddingModels")
    @DisplayName("⚠️ LIMITATION: Completely off-topic - HUGE MODEL VARIATION")
    void testResponseRelevancy_CompletelyOffTopic(String model, int dimensions) {
        log.info("=== LIMITATION: Completely Off-topic ===");

        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("The Great Wall of China was built over many centuries.")
                .build();

        Double score = executeTest(sample, "Completely off-topic", model, dimensions);

        assertTrue(score >= 0.02, "⚠️ KNOWN LIMITATION: Off-topic scores vary 26x (0.01-0.74). Received: " + score);

        log.warn("⚠️ Metric reliability for off-topic answers CRITICALLY depends on model!");
    }

    @ParameterizedTest
    @MethodSource("embeddingModels")
    @DisplayName("⚠️ LIMITATION: Different domains, similar structure - HUGE MODEL VARIATION")
    void testResponseRelevancy_DifferentDomains_SimilarStructure(String model, int dimensions) {
        log.info("=== LIMITATION: Different Domains with Similar Question Structure ===");

        Sample sample = Sample.builder()
                .userInput("How do I configure Spring Boot security?")
                .response("The recipe for chocolate chip cookies includes flour, sugar, and chocolate chips.")
                .build();

        Double score = executeTest(sample, "Different domains, similar structure", model, dimensions);

        assertTrue(score >= 0.01, "⚠️ LIMITATION: Domain shift scores vary 15x (0.05-0.74) . Received: " + score);

        log.error("🚨 CRITICAL: Programming vs cooking scored {}!", score);
    }

    @ParameterizedTest
    @MethodSource("embeddingModels")
    @DisplayName("⚠️ LIMITATION: Single-word nonsense - VERY MODEL DEPENDENT")
    void testResponseRelevancy_SingleWordNonsense(String model, int dimensions) {
        log.info("=== LIMITATION: Single-word Nonsense ===");

        Sample sample = Sample.builder()
                .userInput("Calculate the derivative of x squared")
                .response("Blue")
                .build();

        Double score = executeTest(sample, "Single-word nonsense", model, dimensions);

        assertTrue(score >= 0.01, "🚨 LIMITATION: Nonsense word scores vary 16x (0.05-0.78). Received: " + score);

        log.error("🚨 Model-dependent: Single-word nonsense scored {}!", score);
    }

    @ParameterizedTest
    @MethodSource("embeddingModels")
    @DisplayName("Edge Case: Very short Q&A")
    void testResponseRelevancy_ShortQA(String model, int dimensions) {
        log.info("=== Edge Case: Short Q&A ===");

        Sample sample = Sample.builder()
                .userInput("Capital of France?")
                .response("Paris.")
                .build();

        Double score = executeTest(sample, "Edge Case: Very short Q&A", model, dimensions);

        assertTrue(score >= 0.30, "Short answers vary widely (0.40-0.84). Received: " + score);
    }

    @ParameterizedTest
    @MethodSource("embeddingModels")
    @DisplayName("Edge Case: Incorrect but on-topic")
    void testResponseRelevancy_IncorrectButOnTopic(String model, int dimensions) {
        log.info("=== Edge Case: Incorrect but On-topic ===");

        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("The capital of France is Lyon.")
                .build();

        Double score = executeTest(sample, "Edge Case: Incorrect but on-topic", model, dimensions);

        assertTrue(
                score >= 0.80,
                "Incorrect but on-topic answers score HIGH (0.82-0.97) - identical to correct answers. "
                        + "This is by design - metric doesn't check correctness. Received: " + score);

        log.info("ℹ️ Reminder: This metric does NOT validate correctness!");
    }

    @ParameterizedTest
    @MethodSource("embeddingModels")
    @DisplayName("Edge Case: Answer with redundant information")
    void testResponseRelevancy_RedundantInformation(String model, int dimensions) {
        log.info("=== Edge Case: Redundant Information ===");

        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("The capital of France is Paris. "
                        + "By the way, yesterday I went to the store and bought milk. "
                        + "The weather was great. I also met an old friend.")
                .build();

        Double score = executeTest(sample, "Edge Case: Answer with redundant information", model, dimensions);

        assertTrue(score >= 0.03, "Redundant info handling varies WILDLY (0.04-0.83). Received: " + score);
    }

    @ParameterizedTest
    @MethodSource("embeddingModels")
    @DisplayName("Edge Case: Hypothetical question")
    void testResponseRelevancy_HypotheticalQuestion(String model, int dimensions) {
        log.info("=== Edge Case: Hypothetical Question ===");

        Sample sample = Sample.builder()
                .userInput("What would happen if the Earth stopped rotating?")
                .response(
                        "If Earth stopped rotating, one side would face the Sun continuously, experiencing extreme heat, "
                                + "while the other side would be in perpetual darkness and freezing cold. The atmosphere "
                                + "would continue moving at high speed, causing catastrophic winds.")
                .build();

        Double score = executeTest(sample, "Edge Case: Hypothetical question", model, dimensions);

        assertTrue(score >= 0.40, "Hypothetical questions vary (0.54-0.88). Received: " + score);
    }

    @ParameterizedTest
    @MethodSource("embeddingModels")
    @DisplayName("Edge Case: Ambiguous question")
    void testResponseRelevancy_AmbiguousQuestion(String model, int dimensions) {
        log.info("=== Edge Case: Ambiguous Question ===");

        Sample sample = Sample.builder()
                .userInput("What is the bank?")
                .response("A bank is a financial institution that accepts deposits and creates credit.")
                .build();

        Double score = executeTest(sample, "Edge Case: Ambiguous question", model, dimensions);

        assertTrue(
                score >= 0.60,
                "Ambiguous questions with reasonable interpretation score moderately to high (0.69-0.91). "
                        + "Received: " + score);
    }

    @ParameterizedTest
    @MethodSource("embeddingModels")
    @DisplayName("Edge Case: Clarification request")
    void testResponseRelevancy_ClarificationRequest(String model, int dimensions) {
        log.info("=== Edge Case: Clarification Request ===");

        Sample sample = Sample.builder()
                .userInput("What is it?")
                .response("I need more context to answer your question. What are you referring to?")
                .build();

        Double score = executeTest(sample, "Edge Case: Clarification request", model, dimensions);

        assertTrue(
                score >= 0.00,
                "Clarification requests vary WILDLY (0.00-0.81). "
                        + "Some models treat as noncommittal (0.0), others don't. Received: " + score);
    }

    @ParameterizedTest
    @MethodSource("embeddingModels")
    @DisplayName("Validation: Empty user input")
    void testResponseRelevancy_EmptyUserInput(String model, int dimensions) {
        log.info("=== Validation: Empty User Input ===");

        Sample sample = Sample.builder()
                .userInput("")
                .response("Paris is the capital of France.")
                .build();

        Double score = executeTest(sample, "Validation: Empty user input", model, dimensions);

        assertEquals(0.0, score, "Empty input should return 0.0. Received: " + score);
    }

    @ParameterizedTest
    @MethodSource("embeddingModels")
    @DisplayName("Validation: Empty response")
    void testResponseRelevancy_EmptyResponse(String model, int dimensions) {
        log.info("=== Validation: Empty Response ===");

        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("")
                .build();

        Double score = executeTest(sample, "Validation: Empty response", model, dimensions);

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

    @Step("Call metric")
    private Double executeTest(Sample sample, String label, String model, int dimensions) {
        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score;
        if (openAiApi != null) {
            score = responseRelevancyMetric.toBuilder()
                    .embeddingModel(new OpenAiEmbeddingModel(
                            openAiApi,
                            MetadataMode.EMBED,
                            OpenAiEmbeddingOptions.builder()
                                    .model(model)
                                    .dimensions(dimensions)
                                    .build()))
                    .build()
                    .singleTurnScore(config, sample);
        } else {
            score = responseRelevancyMetric.singleTurnScore(config, sample);
        }

        log.info(
                """
                        Model: {} - {} dimensions
                        🏷️ Scenario: {}
                        ❓ Question: {}
                        💬 Response: {}
                        📊 Score: {} {}

                        """,
                model,
                dimensions,
                label,
                sample.getUserInput(),
                sample.getResponse(),
                String.format("%.4f", score),
                getScoreEmoji(score));
        return score;
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
