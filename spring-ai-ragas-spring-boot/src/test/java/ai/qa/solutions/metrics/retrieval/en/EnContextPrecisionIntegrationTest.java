package ai.qa.solutions.metrics.retrieval.en;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.retrieval.ContextPrecisionMetric;
import ai.qa.solutions.sample.Sample;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

@Slf4j
@EnableAutoConfiguration
@DisplayName("Context Precision Metric Integration Tests - English")
@SpringBootTest(classes = EnContextPrecisionIntegrationTest.ContextPrecisionIntegrationTestConfiguration.class)
class EnContextPrecisionIntegrationTest {

    @Configuration
    public static class ContextPrecisionIntegrationTestConfiguration {}

    @Autowired
    private ContextPrecisionMetric contextPrecisionMetric;

    // ==================== AUTO-DETECTION TESTS ====================

    @Test
    @DisplayName("Context Precision: Auto-detection with response (no reference)")
    void testContextPrecision_AutoDetectResponse() {
        log.info("=== Testing Context Precision - Auto-detect Response-based ===");

        Sample sample = Sample.builder()
                .userInput("Where is the Eiffel Tower located?")
                .response("The Eiffel Tower is located in Paris, France.")
                .retrievedContexts(List.of(
                        "The Eiffel Tower is located in Paris, the capital city of France.",
                        "Paris is known for its iconic landmarks including the Eiffel Tower.",
                        "The weather today is sunny and warm."))
                .build();

        ContextPrecisionMetric.ContextPrecisionConfig config = ContextPrecisionMetric.ContextPrecisionConfig.builder()
                // No evaluation strategy specified - will auto-detect
                .build();

        Double score = contextPrecisionMetric.singleTurnScore(config, sample);

        log.info("Query: {}", sample.getUserInput());
        log.info("Response: {}", sample.getResponse());
        log.info("Context Precision Score (auto-detected response-based): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.5, "Expected reasonable score for mixed relevant contexts, got: " + score);
    }

    @Test
    @DisplayName("Context Precision: Auto-detection with reference (preferred)")
    void testContextPrecision_AutoDetectReference() {
        log.info("=== Testing Context Precision - Auto-detect Reference-based (preferred) ===");

        Sample sample = Sample.builder()
                .userInput("What is photosynthesis?")
                .response("Photosynthesis is a process used by plants.")
                .reference(
                        "Photosynthesis is the process by which plants use sunlight, carbon dioxide, and water to produce glucose and oxygen.")
                .retrievedContexts(List.of(
                        "Photosynthesis is a biological process where plants convert light energy into chemical energy using chlorophyll.",
                        "The process involves the reaction: 6CO2 + 6H2O + light energy → C6H12O6 + 6O2.",
                        "Plants are autotrophs that can produce their own food through photosynthesis."))
                .build();

        ContextPrecisionMetric.ContextPrecisionConfig config = ContextPrecisionMetric.ContextPrecisionConfig.builder()
                // No evaluation strategy specified - will auto-detect reference-based
                .build();

        Double score = contextPrecisionMetric.singleTurnScore(config, sample);

        log.info("Query: {}", sample.getUserInput());
        log.info("Reference: {}", sample.getReference());
        log.info("Context Precision Score (auto-detected reference-based): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.8, "Expected high score for relevant contexts with reference, got: " + score);
    }

    // ==================== EXPLICIT STRATEGY TESTS ====================

    @Test
    @DisplayName("Context Precision: Explicit reference-based strategy")
    void testContextPrecision_ExplicitReference() {
        log.info("=== Testing Context Precision - Explicit Reference-based ===");

        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("Paris is a city.")
                .reference("The capital of France is Paris.")
                .retrievedContexts(List.of(
                        "Paris is the capital and largest city of France.",
                        "France is a country located in Western Europe.",
                        "Basketball is played with two teams of five players."))
                .build();

        ContextPrecisionMetric.ContextPrecisionConfig config = ContextPrecisionMetric.ContextPrecisionConfig.builder()
                .evaluationStrategy(ContextPrecisionMetric.EvaluationStrategy.REFERENCE_BASED)
                .build();

        Double score = contextPrecisionMetric.singleTurnScore(config, sample);

        log.info("Query: {}", sample.getUserInput());
        log.info("Context Precision Score (explicit reference-based): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.5, "Expected reasonable score with mixed contexts, got: " + score);
    }

    @Test
    @DisplayName("Context Precision: Explicit response-based strategy")
    void testContextPrecision_ExplicitResponse() {
        log.info("=== Testing Context Precision - Explicit Response-based ===");

        Sample sample = Sample.builder()
                .userInput("How does machine learning work?")
                .response("Machine learning uses algorithms to learn patterns from data and make predictions.")
                .reference(
                        "Machine learning is a subset of AI that enables computers to learn without explicit programming.")
                .retrievedContexts(List.of(
                        "Machine learning algorithms learn from training data to make predictions.",
                        "Neural networks are a popular ML technique inspired by the human brain.",
                        "The weather forecast shows rain tomorrow with 80% probability."))
                .build();

        ContextPrecisionMetric.ContextPrecisionConfig config = ContextPrecisionMetric.ContextPrecisionConfig.builder()
                .evaluationStrategy(ContextPrecisionMetric.EvaluationStrategy.RESPONSE_BASED)
                .build();

        Double score = contextPrecisionMetric.singleTurnScore(config, sample);

        log.info("Query: {}", sample.getUserInput());
        log.info("Context Precision Score (explicit response-based): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
    }

    // ==================== QUALITY TESTS ====================

    @Test
    @DisplayName("Context Precision: Perfect ordering with all relevant contexts")
    void testContextPrecision_PerfectOrdering() {
        log.info("=== Testing Context Precision - Perfect Ordering ===");

        Sample sample = Sample.builder()
                .userInput("What is artificial intelligence?")
                .reference(
                        "Artificial intelligence is the simulation of human intelligence processes by machines, especially computer systems.")
                .retrievedContexts(List.of(
                        "Artificial intelligence (AI) refers to the simulation of human intelligence in machines.",
                        "AI systems are designed to think like humans and mimic their actions.",
                        "Machine learning is a subset of artificial intelligence.",
                        "AI applications include expert systems, natural language processing, and machine vision."))
                .build();

        ContextPrecisionMetric.ContextPrecisionConfig config = ContextPrecisionMetric.ContextPrecisionConfig.builder()
                .evaluationStrategy(ContextPrecisionMetric.EvaluationStrategy.REFERENCE_BASED)
                .build();

        Double score = contextPrecisionMetric.singleTurnScore(config, sample);

        log.info("Query: {}", sample.getUserInput());
        log.info("Context Precision Score (perfect ordering): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.75, "Expected very high score for perfectly ordered relevant contexts, got: " + score);
    }

    @Test
    @DisplayName("Context Precision: Poor ordering with irrelevant context first")
    void testContextPrecision_PoorOrdering() {
        log.info("=== Testing Context Precision - Poor Ordering ===");

        Sample sample = Sample.builder()
                .userInput("What is quantum computing?")
                .response("Quantum computing uses quantum mechanical phenomena to process information.")
                .retrievedContexts(List.of(
                        "The price of groceries has increased significantly this year.",
                        "Quantum computers use quantum bits (qubits) instead of classical bits.",
                        "Quantum computing leverages quantum superposition and entanglement.",
                        "Traditional computers use binary digits for computation."))
                .build();

        ContextPrecisionMetric.ContextPrecisionConfig config = ContextPrecisionMetric.ContextPrecisionConfig.builder()
                .evaluationStrategy(ContextPrecisionMetric.EvaluationStrategy.RESPONSE_BASED)
                .build();

        Double score = contextPrecisionMetric.singleTurnScore(config, sample);

        log.info("Query: {}", sample.getUserInput());
        log.info("Context Precision Score (poor ordering): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score <= 0.7, "Expected lower score when irrelevant context is first, got: " + score);
    }

    @Test
    @DisplayName("Context Precision: Mixed relevance - realistic scenario")
    void testContextPrecision_MixedRelevance() {
        log.info("=== Testing Context Precision - Mixed Relevance ===");

        Sample sample = Sample.builder()
                .userInput("Explain the greenhouse effect")
                .reference(
                        "The greenhouse effect is a natural process where certain gases in Earth's atmosphere trap heat from the sun, warming the planet.")
                .retrievedContexts(List.of(
                        "Greenhouse gases like CO2 and methane trap heat in Earth's atmosphere.",
                        "The greenhouse effect is essential for life on Earth as it keeps the planet warm.",
                        "Popular tourist destinations include tropical beaches and mountain resorts.",
                        "Without the greenhouse effect, Earth would be too cold to support most life.",
                        "Climate change is linked to an enhanced greenhouse effect from human activities."))
                .build();

        ContextPrecisionMetric.ContextPrecisionConfig config = ContextPrecisionMetric.ContextPrecisionConfig.builder()
                .evaluationStrategy(ContextPrecisionMetric.EvaluationStrategy.REFERENCE_BASED)
                .build();

        Double score = contextPrecisionMetric.singleTurnScore(config, sample);

        log.info("Query: {}", sample.getUserInput());
        log.info("Context Precision Score (mixed relevance): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        // Most contexts are relevant, so should be reasonably high
        assertTrue(score >= 0.4, "Expected decent score for mostly relevant contexts, got: " + score);
    }

    // ==================== ASYNC TESTS ====================

    @Test
    @DisplayName("Async evaluation test")
    void testAsyncEvaluation() throws Exception {
        log.info("=== Testing Async Evaluation ===");

        Sample sample = Sample.builder()
                .userInput("What is blockchain technology?")
                .response(
                        "Blockchain is a distributed ledger technology that maintains a continuously growing list of records.")
                .retrievedContexts(List.of(
                        "Blockchain technology enables secure and transparent transactions without intermediaries.",
                        "Each block in a blockchain contains a cryptographic hash of the previous block.",
                        "Cryptocurrencies like Bitcoin are built on blockchain technology."))
                .build();

        ContextPrecisionMetric.ContextPrecisionConfig config = ContextPrecisionMetric.ContextPrecisionConfig.builder()
                .evaluationStrategy(ContextPrecisionMetric.EvaluationStrategy.RESPONSE_BASED)
                .build();

        long startTime = System.currentTimeMillis();
        CompletableFuture<Double> asyncScore = contextPrecisionMetric.singleTurnScoreAsync(config, sample);
        Double score = asyncScore.get();
        long endTime = System.currentTimeMillis();

        log.info("Async execution time: {} ms", (endTime - startTime));
        log.info("Async score: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
    }

    @Test
    @DisplayName("Parallel evaluation: Reference vs Response strategies")
    void testParallelEvaluationComparison() {
        log.info("=== Testing Parallel Evaluation - Reference vs Response Strategies ===");

        Sample sample = Sample.builder()
                .userInput("Explain climate change")
                .response("Climate change refers to long-term shifts in global temperatures and weather patterns.")
                .reference(
                        "Climate change is the long-term warming of the planet primarily due to human activities that increase greenhouse gas concentrations in the atmosphere.")
                .retrievedContexts(List.of(
                        "Human activities like burning fossil fuels contribute to global warming.",
                        "Climate change impacts include rising sea levels and extreme weather events.",
                        "Popular vacation destinations include beaches and mountains.",
                        "Greenhouse gases trap heat in the Earth's atmosphere causing global warming."))
                .build();

        // Create configs for both strategies
        ContextPrecisionMetric.ContextPrecisionConfig referenceConfig =
                ContextPrecisionMetric.ContextPrecisionConfig.builder()
                        .evaluationStrategy(ContextPrecisionMetric.EvaluationStrategy.REFERENCE_BASED)
                        .build();

        ContextPrecisionMetric.ContextPrecisionConfig responseConfig =
                ContextPrecisionMetric.ContextPrecisionConfig.builder()
                        .evaluationStrategy(ContextPrecisionMetric.EvaluationStrategy.RESPONSE_BASED)
                        .build();

        long startTime = System.currentTimeMillis();

        // Parallel execution
        CompletableFuture<Double> referenceFuture =
                contextPrecisionMetric.singleTurnScoreAsync(referenceConfig, sample);
        CompletableFuture<Double> responseFuture = contextPrecisionMetric.singleTurnScoreAsync(responseConfig, sample);

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(referenceFuture, responseFuture);
        allFutures.join();
        long endTime = System.currentTimeMillis();

        Double referenceScore = referenceFuture.join();
        Double responseScore = responseFuture.join();

        log.info("Parallel execution time: {} ms", (endTime - startTime));
        log.info("Reference-based Context Precision: {}", referenceScore);
        log.info("Response-based Context Precision: {}", responseScore);

        assertNotNull(referenceScore);
        assertNotNull(responseScore);

        assertTrue(referenceScore >= 0.0 && referenceScore <= 1.0);
        assertTrue(responseScore >= 0.0 && responseScore <= 1.0);
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Edge case: Empty retrieved contexts")
    void testEmptyRetrievedContexts() {
        log.info("=== Testing Edge Case - Empty Retrieved Contexts ===");

        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("The capital of France is Paris.")
                .retrievedContexts(List.of())
                .build();

        ContextPrecisionMetric.ContextPrecisionConfig config =
                ContextPrecisionMetric.ContextPrecisionConfig.builder().build();

        Double score = contextPrecisionMetric.singleTurnScore(config, sample);

        log.info("Score for empty contexts: {}", score);

        assertNotNull(score);
        assertTrue(score == 0.0, "Expected 0.0 for empty retrieved contexts, got: " + score);
    }

    @Test
    @DisplayName("Edge case: Single relevant context")
    void testSingleRelevantContext() {
        log.info("=== Testing Edge Case - Single Relevant Context ===");

        Sample sample = Sample.builder()
                .userInput("What is the capital of Italy?")
                .response("The capital of Italy is Rome.")
                .retrievedContexts(List.of("Rome is the capital and largest city of Italy."))
                .build();

        ContextPrecisionMetric.ContextPrecisionConfig config =
                ContextPrecisionMetric.ContextPrecisionConfig.builder().build();

        Double score = contextPrecisionMetric.singleTurnScore(config, sample);

        log.info("Score for single relevant context: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.8, "Expected high score for single relevant context, got: " + score);
    }

    @Test
    @DisplayName("Edge case: All irrelevant contexts")
    void testAllIrrelevantContexts() {
        log.info("=== Testing Edge Case - All Irrelevant Contexts ===");

        Sample sample = Sample.builder()
                .userInput("What is machine learning?")
                .response("Machine learning is a subset of artificial intelligence.")
                .retrievedContexts(List.of(
                        "The weather today is sunny and warm.",
                        "Pizza is a popular Italian dish.",
                        "Basketball is played with two teams of five players."))
                .build();

        ContextPrecisionMetric.ContextPrecisionConfig config =
                ContextPrecisionMetric.ContextPrecisionConfig.builder().build();

        Double score = contextPrecisionMetric.singleTurnScore(config, sample);

        log.info("Score for all irrelevant contexts: {}", score);

        assertNotNull(score);
        assertTrue(score <= 0.2, "Expected low score for all irrelevant contexts, got: " + score);
    }

    @Test
    @DisplayName("Edge case: Reference-based fallback to response-based")
    void testReferenceFallbackToResponse() {
        log.info("=== Testing Edge Case - Reference-based Fallback ===");

        Sample sample = Sample.builder()
                .userInput("What is renewable energy?")
                .response("Renewable energy comes from natural sources that replenish themselves.")
                .reference("") // Empty reference
                .retrievedContexts(List.of(
                        "Solar and wind power are examples of renewable energy sources.",
                        "Renewable energy helps reduce greenhouse gas emissions."))
                .build();

        // Request reference-based but will fall back to response-based due to empty reference
        ContextPrecisionMetric.ContextPrecisionConfig config = ContextPrecisionMetric.ContextPrecisionConfig.builder()
                .evaluationStrategy(ContextPrecisionMetric.EvaluationStrategy.REFERENCE_BASED)
                .build();

        Double score = contextPrecisionMetric.singleTurnScore(config, sample);

        log.info("Score for reference fallback: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        // Should still get a reasonable score using response-based evaluation
        assertTrue(score >= 0.5, "Expected reasonable score after fallback to response-based, got: " + score);
    }

    @Test
    @DisplayName("Strategy comparison: Reference-based strategy")
    void testStrategyComparison_ReferenceBased() {
        log.info("=== Testing Strategy Comparison - Reference-based ===");

        Sample sample = Sample.builder()
                .userInput("What causes earthquakes?")
                .response("Earthquakes are caused by the movement of tectonic plates.")
                .reference(
                        "Earthquakes occur when tectonic plates in the Earth's crust suddenly shift and release energy, creating seismic waves.")
                .retrievedContexts(List.of(
                        "Earthquakes are caused by the sudden movement of tectonic plates.",
                        "Tectonic plate movement occurs when plates shift along fault lines.",
                        "The release of energy from moving tectonic plates creates seismic waves.",
                        "Popular tourist destinations include tropical beaches."))
                .build();

        ContextPrecisionMetric.ContextPrecisionConfig config = ContextPrecisionMetric.ContextPrecisionConfig.builder()
                .evaluationStrategy(ContextPrecisionMetric.EvaluationStrategy.REFERENCE_BASED)
                .build();

        Double referenceScore = contextPrecisionMetric.singleTurnScore(config, sample);

        log.info("Reference-based score: {}", referenceScore);

        assertNotNull(referenceScore);
        assertTrue(referenceScore >= 0.0 && referenceScore <= 1.0);
        assertTrue(referenceScore >= 0.5, "Expected reasonable reference-based score");
    }

    @Test
    @DisplayName("Strategy comparison: Response-based strategy")
    void testStrategyComparison_ResponseBased() {
        log.info("=== Testing Strategy Comparison - Response-based ===");

        Sample sample = Sample.builder()
                .userInput("What causes earthquakes?")
                .response("Earthquakes are caused by the movement of tectonic plates.")
                .reference(
                        "Earthquakes occur when tectonic plates in the Earth's crust suddenly shift and release energy, creating seismic waves.")
                .retrievedContexts(List.of(
                        "Earthquakes are caused by the sudden movement of tectonic plates.",
                        "Tectonic plate movement occurs when plates shift along fault lines.",
                        "The release of energy from moving tectonic plates creates seismic waves.",
                        "Popular tourist destinations include tropical beaches."))
                .build();

        ContextPrecisionMetric.ContextPrecisionConfig config = ContextPrecisionMetric.ContextPrecisionConfig.builder()
                .evaluationStrategy(ContextPrecisionMetric.EvaluationStrategy.RESPONSE_BASED)
                .build();

        Double responseScore = contextPrecisionMetric.singleTurnScore(config, sample);

        log.info("Response-based score: {}", responseScore);

        assertNotNull(responseScore);
        assertTrue(responseScore >= 0.0 && responseScore <= 1.0);
        assertTrue(responseScore >= 0.5, "Expected reasonable response-based score");
    }
}
