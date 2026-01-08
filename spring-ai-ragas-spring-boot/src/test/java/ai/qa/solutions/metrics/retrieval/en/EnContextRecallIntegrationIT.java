package ai.qa.solutions.metrics.retrieval.en;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.retrieval.ContextRecallMetric;
import ai.qa.solutions.sample.Sample;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

@Slf4j
@EnableAutoConfiguration
@SuppressWarnings("LoggingSimilarMessage")
@DisplayName("Context Recall Metric Integration Tests - English")
@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".*")
@SpringBootTest(classes = EnContextRecallIntegrationIT.ContextRecallIntegrationTestConfiguration.class)
class EnContextRecallIntegrationIT {

    @Configuration
    public static class ContextRecallIntegrationTestConfiguration {}

    @Autowired
    private ContextRecallMetric contextRecallMetric;

    // ==================== BASIC FUNCTIONALITY TESTS ====================

    @Test
    @DisplayName("Context Recall: Perfect recall - all information available")
    void testContextRecall_PerfectRecall() {
        log.info("=== Testing Context Recall - Perfect Recall ===");

        Sample sample = Sample.builder()
                .userInput("What can you tell me about Albert Einstein?")
                .reference(
                        "Albert Einstein was born on 14 March 1879. He was a German-born theoretical physicist. He received the 1921 Nobel Prize in Physics.")
                .retrievedContexts(
                        List.of(
                                "Albert Einstein (14 March 1879 - 18 April 1955) was a German-born theoretical physicist, widely held to be one of the greatest and most influential scientists of all time.",
                                "He received the 1921 Nobel Prize in Physics for his services to theoretical physics, and especially for his discovery of the law of the photoelectric effect.",
                                "Best known for developing the theory of relativity, he also made important contributions to quantum mechanics."))
                .build();

        ContextRecallMetric.ContextRecallConfig config =
                ContextRecallMetric.ContextRecallConfig.builder().build();

        Double score = contextRecallMetric.singleTurnScore(config, sample);

        log.info("Question: {}", sample.getUserInput());
        log.info("Reference: {}", sample.getReference());
        log.info("Context Recall Score (perfect recall): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.9, "Expected very high score for perfect recall, got: " + score);
    }

    @Test
    @DisplayName("Context Recall: Partial recall - some information missing")
    void testContextRecall_PartialRecall() {
        log.info("=== Testing Context Recall - Partial Recall ===");

        Sample sample = Sample.builder()
                .userInput("What is the capital of France and what is it famous for?")
                .reference(
                        "The capital of France is Paris. Paris is famous for the Eiffel Tower. The city has a population of over 2 million people.")
                .retrievedContexts(List.of(
                        "Paris is the capital and largest city of France.",
                        "The Eiffel Tower is an iconic landmark located in Paris, France.",
                        "France is a country in Western Europe known for its culture and cuisine."))
                .build();

        ContextRecallMetric.ContextRecallConfig config =
                ContextRecallMetric.ContextRecallConfig.builder().build();

        Double score = contextRecallMetric.singleTurnScore(config, sample);

        log.info("Question: {}", sample.getUserInput());
        log.info("Reference: {}", sample.getReference());
        log.info("Context Recall Score (partial recall): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.6 && score <= 0.8, "Expected moderate score for partial recall, got: " + score);
    }

    @Test
    @DisplayName("Context Recall: Poor recall - most information missing")
    void testContextRecall_PoorRecall() {
        log.info("=== Testing Context Recall - Poor Recall ===");

        Sample sample = Sample.builder()
                .userInput("What is machine learning and how does it work?")
                .reference(
                        "Machine learning is a subset of artificial intelligence. It uses algorithms to learn patterns from data. Neural networks are a popular ML technique. Deep learning is a type of machine learning.")
                .retrievedContexts(List.of(
                        "Artificial intelligence is a broad field in computer science.",
                        "Data analysis is important in modern technology.",
                        "Computers process information using various algorithms."))
                .build();

        ContextRecallMetric.ContextRecallConfig config =
                ContextRecallMetric.ContextRecallConfig.builder().build();

        Double score = contextRecallMetric.singleTurnScore(config, sample);

        log.info("Question: {}", sample.getUserInput());
        log.info("Reference: {}", sample.getReference());
        log.info("Context Recall Score (poor recall): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score <= 0.5, "Expected low score for poor recall, got: " + score);
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Edge case: Empty retrieved contexts")
    void testContextRecall_EmptyContexts() {
        log.info("=== Testing Edge Case - Empty Retrieved Contexts ===");

        Sample sample = Sample.builder()
                .userInput("What is the capital of Italy?")
                .reference("The capital of Italy is Rome.")
                .retrievedContexts(List.of())
                .build();

        ContextRecallMetric.ContextRecallConfig config =
                ContextRecallMetric.ContextRecallConfig.builder().build();

        Double score = contextRecallMetric.singleTurnScore(config, sample);

        log.info("Score for empty contexts: {}", score);

        assertNotNull(score);
        assertEquals(0.0, score, "Expected 0.0 for empty retrieved contexts, got: " + score);
    }

    @Test
    @DisplayName("Edge case: Empty reference")
    void testContextRecall_EmptyReference() {
        log.info("=== Testing Edge Case - Empty Reference ===");

        Sample sample = Sample.builder()
                .userInput("What is quantum computing?")
                .reference("")
                .retrievedContexts(List.of(
                        "Quantum computing uses quantum mechanical phenomena.",
                        "It has potential applications in cryptography and optimization."))
                .build();

        ContextRecallMetric.ContextRecallConfig config =
                ContextRecallMetric.ContextRecallConfig.builder().build();

        Double score = contextRecallMetric.singleTurnScore(config, sample);

        log.info("Score for empty reference: {}", score);

        assertNotNull(score);
        assertEquals(0.0, score, "Expected 0.0 for empty reference, got: " + score);
    }

    @Test
    @DisplayName("Edge case: Single sentence reference")
    void testContextRecall_SingleSentenceReference() {
        log.info("=== Testing Edge Case - Single Sentence Reference ===");

        Sample sample = Sample.builder()
                .userInput("Where is the Statue of Liberty?")
                .reference("The Statue of Liberty is located in New York Harbor.")
                .retrievedContexts(List.of(
                        "The Statue of Liberty is a neoclassical sculpture on Liberty Island in New York Harbor.",
                        "It was a gift from France to the United States in 1886."))
                .build();

        ContextRecallMetric.ContextRecallConfig config =
                ContextRecallMetric.ContextRecallConfig.builder().build();

        Double score = contextRecallMetric.singleTurnScore(config, sample);

        log.info("Score for single sentence reference: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.8, "Expected high score for single supported sentence, got: " + score);
    }

    // ==================== COMPLEX SCENARIOS ====================

    @Test
    @DisplayName("Complex scenario: Scientific information with multiple facts")
    void testContextRecall_ScientificInformation() {
        log.info("=== Testing Complex Scenario - Scientific Information ===");

        Sample sample = Sample.builder()
                .userInput("Explain photosynthesis")
                .reference(
                        "Photosynthesis is the process by which plants convert light energy into chemical energy. It occurs in chloroplasts. The process requires carbon dioxide, water, and sunlight. Oxygen is released as a byproduct.")
                .retrievedContexts(List.of(
                        "Photosynthesis is a biological process where plants use sunlight to convert carbon dioxide and water into glucose.",
                        "Chloroplasts are organelles found in plant cells where photosynthesis takes place.",
                        "During photosynthesis, oxygen is produced as a waste product and released into the atmosphere.",
                        "Light energy is captured by chlorophyll pigments in the chloroplasts."))
                .build();

        ContextRecallMetric.ContextRecallConfig config =
                ContextRecallMetric.ContextRecallConfig.builder().build();

        Double score = contextRecallMetric.singleTurnScore(config, sample);

        log.info("Question: {}", sample.getUserInput());
        log.info("Context Recall Score (scientific information): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.8, "Expected high score for well-supported scientific facts, got: " + score);
    }

    @Test
    @DisplayName("Complex scenario: Historical information with dates")
    void testContextRecall_HistoricalInformation() {
        log.info("=== Testing Complex Scenario - Historical Information ===");

        Sample sample = Sample.builder()
                .userInput("Tell me about World War II")
                .reference(
                        "World War II lasted from 1939 to 1945. It was fought between the Axis and Allied powers. The war ended with the surrender of Germany in May 1945. Japan surrendered in August 1945 after the atomic bombings.")
                .retrievedContexts(List.of(
                        "World War II was a global war that lasted from 1939 to 1945.",
                        "The main belligerents were the Axis powers (Germany, Italy, Japan) and the Allied powers.",
                        "Germany surrendered on May 8, 1945, marking the end of the war in Europe.",
                        "The Pacific War continued until Japan's surrender in August 1945.",
                        "The United States dropped atomic bombs on Hiroshima and Nagasaki in August 1945."))
                .build();

        ContextRecallMetric.ContextRecallConfig config =
                ContextRecallMetric.ContextRecallConfig.builder().build();

        Double score = contextRecallMetric.singleTurnScore(config, sample);

        log.info("Question: {}", sample.getUserInput());
        log.info("Context Recall Score (historical information): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.9, "Expected very high score for well-documented historical facts, got: " + score);
    }

    @Test
    @DisplayName("Complex scenario: Mixed relevant and irrelevant contexts")
    void testContextRecall_MixedContexts() {
        log.info("=== Testing Complex Scenario - Mixed Contexts ===");

        Sample sample = Sample.builder()
                .userInput("What causes climate change?")
                .reference(
                        "Climate change is primarily caused by human activities. Burning fossil fuels releases greenhouse gases. Deforestation reduces carbon absorption. Industrial processes contribute to emissions.")
                .retrievedContexts(
                        List.of(
                                "Human activities such as burning fossil fuels are the primary cause of modern climate change.",
                                "Greenhouse gases like CO2 trap heat in the Earth's atmosphere.",
                                "The weather today is sunny with a high of 75 degrees.",
                                "Deforestation reduces the Earth's capacity to absorb carbon dioxide from the atmosphere.",
                                "Popular vacation destinations include tropical beaches and mountain resorts.",
                                "Industrial activities release various pollutants including greenhouse gases into the atmosphere."))
                .build();

        ContextRecallMetric.ContextRecallConfig config =
                ContextRecallMetric.ContextRecallConfig.builder().build();

        Double score = contextRecallMetric.singleTurnScore(config, sample);

        log.info("Question: {}", sample.getUserInput());
        log.info("Context Recall Score (mixed contexts): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.7, "Expected good score despite irrelevant contexts, got: " + score);
    }

    // ==================== ASYNC TESTS ====================

    @Test
    @DisplayName("Async evaluation test")
    void testContextRecall_AsyncEvaluation() throws Exception {
        log.info("=== Testing Async Evaluation ===");

        Sample sample = Sample.builder()
                .userInput("What is blockchain technology?")
                .reference(
                        "Blockchain is a distributed ledger technology. It uses cryptographic hashing. Transactions are recorded in blocks. Each block is linked to the previous one.")
                .retrievedContexts(List.of(
                        "Blockchain technology is a distributed ledger that maintains a continuously growing list of records.",
                        "Each block contains a cryptographic hash of the previous block, linking them together.",
                        "Transactions are grouped together and recorded in blocks on the blockchain.",
                        "The technology enables secure and transparent record-keeping without a central authority."))
                .build();

        ContextRecallMetric.ContextRecallConfig config =
                ContextRecallMetric.ContextRecallConfig.builder().build();

        long startTime = System.currentTimeMillis();
        CompletableFuture<Double> asyncScore = contextRecallMetric.singleTurnScoreAsync(config, sample);
        Double score = asyncScore.get();
        long endTime = System.currentTimeMillis();

        log.info("Async execution time: {} ms", (endTime - startTime));
        log.info("Async score: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.7, "Expected high score for well-supported blockchain facts, got: " + score);
    }

    @Test
    @DisplayName("Parallel evaluation with multiple samples")
    void testContextRecall_ParallelEvaluation() {
        log.info("=== Testing Parallel Evaluation ===");

        Sample sample1 = Sample.builder()
                .userInput("What is artificial intelligence?")
                .reference(
                        "AI is the simulation of human intelligence in machines. It includes machine learning and natural language processing.")
                .retrievedContexts(
                        List.of(
                                "Artificial intelligence (AI) refers to the simulation of human intelligence in machines.",
                                "Machine learning is a subset of artificial intelligence.",
                                "Natural language processing is an AI technology that helps computers understand human language."))
                .build();

        Sample sample2 = Sample.builder()
                .userInput("Explain renewable energy")
                .reference(
                        "Renewable energy comes from natural sources. Solar power uses sunlight. Wind power uses air movement.")
                .retrievedContexts(List.of(
                        "Renewable energy is energy that comes from natural sources that replenish themselves.",
                        "Solar power harnesses energy from the sun using photovoltaic cells.",
                        "Wind power generates electricity by using wind to turn turbines."))
                .build();

        ContextRecallMetric.ContextRecallConfig config =
                ContextRecallMetric.ContextRecallConfig.builder().build();

        long startTime = System.currentTimeMillis();

        CompletableFuture<Double> score1Future = contextRecallMetric.singleTurnScoreAsync(config, sample1);
        CompletableFuture<Double> score2Future = contextRecallMetric.singleTurnScoreAsync(config, sample2);

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(score1Future, score2Future);
        allFutures.join();
        long endTime = System.currentTimeMillis();

        Double score1 = score1Future.join();
        Double score2 = score2Future.join();

        log.info("Parallel execution time: {} ms", (endTime - startTime));
        log.info("Sample 1 Context Recall: {}", score1);
        log.info("Sample 2 Context Recall: {}", score2);

        assertNotNull(score1);
        assertNotNull(score2);
        assertTrue(score1 >= 0.0 && score1 <= 1.0);
        assertTrue(score2 >= 0.0 && score2 <= 1.0);
        assertTrue(score1 >= 0.7, "Expected high score for AI sample");
        assertTrue(score2 >= 0.7, "Expected high score for renewable energy sample");
    }

    // ==================== QUALITY ASSESSMENT TESTS ====================

    @Test
    @DisplayName("Quality assessment: Brief reference")
    void testContextRecall_BriefReference() {
        log.info("=== Testing Quality Assessment - Brief Reference ===");

        Sample sample = Sample.builder()
                .userInput("What is gravity?")
                .reference("Gravity is a force that attracts objects to each other.")
                .retrievedContexts(List.of(
                        "Gravity is a fundamental force of nature that causes objects with mass to attract each other.",
                        "The strength of gravitational force depends on the mass of objects and distance between them.",
                        "Newton's law of universal gravitation describes how gravity works."))
                .build();

        ContextRecallMetric.ContextRecallConfig config =
                ContextRecallMetric.ContextRecallConfig.builder().build();

        Double score = contextRecallMetric.singleTurnScore(config, sample);

        log.info("Brief reference score: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.7, "Expected high score for brief but supported reference");
    }

    @Test
    @DisplayName("Quality assessment: Detailed reference")
    void testContextRecall_DetailedReference() {
        log.info("=== Testing Quality Assessment - Detailed Reference ===");

        Sample sample = Sample.builder()
                .userInput("What is gravity?")
                .reference(
                        "Gravity is a fundamental force of nature. It causes objects with mass to attract each other. The force depends on mass and distance. Newton described this with his law of universal gravitation. Einstein later explained gravity through general relativity.")
                .retrievedContexts(List.of(
                        "Gravity is a fundamental force of nature that causes objects with mass to attract each other.",
                        "The strength of gravitational force depends on the mass of objects and distance between them.",
                        "Newton's law of universal gravitation describes how gravity works.",
                        "Einstein's theory of general relativity provides a more complete understanding of gravity."))
                .build();

        ContextRecallMetric.ContextRecallConfig config =
                ContextRecallMetric.ContextRecallConfig.builder().build();

        Double score = contextRecallMetric.singleTurnScore(config, sample);

        log.info("Detailed reference score: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.6, "Expected good score for detailed reference");
    }

    @Test
    @DisplayName("Quality assessment: Complete contexts")
    void testContextRecall_CompleteContexts() {
        log.info("=== Testing Quality Assessment - Complete Contexts ===");

        String reference =
                "Water boils at 100 degrees Celsius. This happens at sea level pressure. The boiling point changes with altitude.";

        Sample sample = Sample.builder()
                .userInput("At what temperature does water boil?")
                .reference(reference)
                .retrievedContexts(List.of(
                        "Water boils at 100 degrees Celsius (212 degrees Fahrenheit) at sea level.",
                        "The boiling point of water is affected by atmospheric pressure.",
                        "At higher altitudes, where atmospheric pressure is lower, water boils at lower temperatures."))
                .build();

        ContextRecallMetric.ContextRecallConfig config =
                ContextRecallMetric.ContextRecallConfig.builder().build();

        Double score = contextRecallMetric.singleTurnScore(config, sample);

        log.info("Complete contexts score: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.6, "Expected high score for complete contexts");
    }

    @Test
    @DisplayName("Quality assessment: Incomplete contexts")
    void testContextRecall_IncompleteContexts() {
        log.info("=== Testing Quality Assessment - Incomplete Contexts ===");

        String reference =
                "Water boils at 100 degrees Celsius. This happens at sea level pressure. The boiling point changes with altitude.";

        Sample sample = Sample.builder()
                .userInput("At what temperature does water boil?")
                .reference(reference)
                .retrievedContexts(List.of(
                        "Water is composed of hydrogen and oxygen atoms.",
                        "Boiling is a phase transition from liquid to gas."))
                .build();

        ContextRecallMetric.ContextRecallConfig config =
                ContextRecallMetric.ContextRecallConfig.builder().build();

        Double score = contextRecallMetric.singleTurnScore(config, sample);

        log.info("Incomplete contexts score: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score <= 0.3, "Expected low score for incomplete contexts");
    }

    @Test
    @DisplayName("Context Recall: Custom model list - single model")
    void testContextRecall_CustomModelList_SingleModel() {
        log.info("=== Context Recall: Custom model list - single model test ===");

        Sample sample = Sample.builder()
                .userInput("What is the speed of light?")
                .reference(
                        "The speed of light in vacuum is approximately 299,792,458 meters per second. This is often rounded to 300,000 km/s for simplicity.")
                .retrievedContexts(List.of(
                        "The speed of light in a vacuum is exactly 299,792,458 meters per second.",
                        "Light travels at approximately 300,000 kilometers per second.",
                        "The speed of light is a fundamental constant in physics."))
                .build();

        ContextRecallMetric.ContextRecallConfig config = ContextRecallMetric.ContextRecallConfig.builder()
                .model("google/gemini-2.5-flash") // Only use this model
                .build();

        Double score = contextRecallMetric.singleTurnScore(config, sample);
        log.info("Score with custom model list: {}", score);

        assertTrue(score >= 0.0 && score <= 1.0, "Score should be valid (0-1 range)");
        assertTrue(score >= 0.8, "Expected high score for well-supported facts");
    }

    @Test
    @DisplayName("Context Recall: Custom model list - all models")
    void testContextRecall_CustomModelList_AllModels() {
        log.info("=== Context Recall: Custom model list - all models test ===");

        Sample sample = Sample.builder()
                .userInput("What is the speed of light?")
                .reference(
                        "The speed of light in vacuum is approximately 299,792,458 meters per second. This is often rounded to 300,000 km/s for simplicity.")
                .retrievedContexts(List.of(
                        "The speed of light in a vacuum is exactly 299,792,458 meters per second.",
                        "Light travels at approximately 300,000 kilometers per second.",
                        "The speed of light is a fundamental constant in physics."))
                .build();

        ContextRecallMetric.ContextRecallConfig config =
                ContextRecallMetric.ContextRecallConfig.builder().build();

        Double score = contextRecallMetric.singleTurnScore(config, sample);
        log.info("Score with all models: {}", score);

        assertTrue(score >= 0.0 && score <= 1.0, "Score should be valid (0-1 range)");
        assertTrue(score >= 0.8, "Expected high score for well-supported facts");
    }
}
