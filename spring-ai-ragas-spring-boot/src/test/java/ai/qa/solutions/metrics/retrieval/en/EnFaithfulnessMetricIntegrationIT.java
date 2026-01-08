package ai.qa.solutions.metrics.retrieval.en;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.retrieval.FaithfulnessMetric;
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
@DisplayName("Faithfulness Metric Integration Tests - English")
@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".*")
@SpringBootTest(classes = EnFaithfulnessMetricIntegrationIT.FaithfulnessMetricTestConfiguration.class)
class EnFaithfulnessMetricIntegrationIT {

    @Configuration
    public static class FaithfulnessMetricTestConfiguration {}

    @Autowired
    private FaithfulnessMetric faithfulnessMetric;

    // ==================== POSITIVE TESTS - HIGH FAITHFULNESS ====================

    @Test
    @DisplayName("Perfect faithfulness - all claims supported by context")
    void testPerfectFaithfulness() {
        log.info("=== Perfect Faithfulness Test ===");

        Sample sample = Sample.builder()
                .userInput("When was the first Super Bowl?")
                .response("The first Super Bowl was held on January 15, 1967.")
                .retrievedContexts(
                        List.of(
                                "The first Super Bowl was held on January 15, 1967, at the Los Angeles Memorial Coliseum in Los Angeles."))
                .build();

        Double score = faithfulnessMetric.singleTurnScore(sample);

        log.info("Question: {}", sample.getUserInput());
        log.info("Response: {}", sample.getResponse());
        log.info("Context: {}", sample.getRetrievedContexts());
        log.info("Faithfulness Score: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.9, "Expected high faithfulness score for fully supported answer, got: " + score);
    }

    @Test
    @DisplayName("High faithfulness - biographical information")
    void testHighFaithfulness_Biography() {
        log.info("=== High Faithfulness - Biography ===");

        Sample sample = Sample.builder()
                .userInput("Who was Albert Einstein?")
                .response("Albert Einstein was a German-born theoretical physicist. "
                        + "He is recognized as one of the greatest physicists of all time. "
                        + "He developed the theory of relativity.")
                .retrievedContexts(
                        List.of("Albert Einstein (born 14 March 1879) was a German-born theoretical physicist, "
                                + "widely held to be one of the greatest and most influential scientists of all time. "
                                + "He was best known for developing the theory of relativity."))
                .build();

        Double score = faithfulnessMetric.singleTurnScore(sample);

        log.info("Question: {}", sample.getUserInput());
        log.info("Faithfulness Score: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.85, "Expected high faithfulness for well-supported biographical facts, got: " + score);
    }

    // ==================== NEGATIVE TESTS - LOW FAITHFULNESS ====================

    @Test
    @DisplayName("Low faithfulness - incorrect date")
    void testLowFaithfulness_IncorrectDate() {
        log.info("=== Low Faithfulness - Incorrect Date ===");

        Sample sample = Sample.builder()
                .userInput("Where and when was Einstein born?")
                .response("Einstein was born in Germany on March 20, 1879.")
                .retrievedContexts(
                        List.of("Albert Einstein (born 14 March 1879) was a German-born theoretical physicist, "
                                + "widely held to be one of the greatest and most influential scientists of all time."))
                .build();

        Double score = faithfulnessMetric.singleTurnScore(sample);

        log.info("Question: {}", sample.getUserInput());
        log.info("Response: {}", sample.getResponse());
        log.info("Context: {}", sample.getRetrievedContexts());
        log.info("Faithfulness Score: {} (contains incorrect date)", score);

        assertNotNull(score);
        assertTrue(
                score <= 0.6,
                "Expected low faithfulness score due to incorrect date (20th instead of 14th), got: " + score);
    }

    @Test
    @DisplayName("Low faithfulness - hallucinated information")
    void testLowFaithfulness_Hallucination() {
        log.info("=== Low Faithfulness - Hallucination ===");

        Sample sample = Sample.builder()
                .userInput("What courses is John taking?")
                .response("John is taking Data Structures, Algorithms, and Artificial Intelligence courses. "
                        + "He also has a part-time job at the university library.")
                .retrievedContexts(List.of("John is a student at XYZ University pursuing a Computer Science degree. "
                        + "He is enrolled in Data Structures, Algorithms, and Database Management courses. "
                        + "John often stays late in the library to work on his projects."))
                .build();

        Double score = faithfulnessMetric.singleTurnScore(sample);

        log.info("Question: {}", sample.getUserInput());
        log.info("Response: {}", sample.getResponse());
        log.info("Faithfulness Score: {} (contains hallucinations)", score);

        assertNotNull(score);
        assertTrue(
                score <= 0.7, "Expected low faithfulness due to hallucinated course (AI) and job claim, got: " + score);
    }

    @Test
    @DisplayName("Zero faithfulness - completely unrelated response")
    void testZeroFaithfulness_UnrelatedContext() {
        log.info("=== Zero Faithfulness - Unrelated Context ===");

        Sample sample = Sample.builder()
                .userInput("Tell me about Albert Einstein")
                .response("Albert Einstein was a brilliant scientist who won the Nobel Prize in Literature in 1921.")
                .retrievedContexts(List.of("Photosynthesis is a process used by plants, algae, and certain bacteria "
                        + "to convert light energy into chemical energy."))
                .build();

        Double score = faithfulnessMetric.singleTurnScore(sample);

        log.info("Question: {}", sample.getUserInput());
        log.info("Response: {}", sample.getResponse());
        log.info("Context: {}", sample.getRetrievedContexts());
        log.info("Faithfulness Score: {} (unrelated context)", score);

        assertNotNull(score);
        assertTrue(score <= 0.3, "Expected very low faithfulness for unrelated context, got: " + score);
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Multiple contexts - partial support")
    void testMultipleContexts_PartialSupport() {
        log.info("=== Multiple Contexts - Partial Support ===");

        Sample sample = Sample.builder()
                .userInput("What do we know about climate change?")
                .response("Climate change is caused by greenhouse gas emissions from human activities. "
                        + "It leads to rising sea levels and extreme weather events. "
                        + "Scientists predict temperatures will rise by 5 degrees by 2100.")
                .retrievedContexts(List.of(
                        "Climate change refers to long-term shifts in temperatures and weather patterns. "
                                + "The main cause is human activities, especially burning fossil fuels which releases greenhouse gases.",
                        "Rising global temperatures are causing sea levels to rise and increasing the frequency "
                                + "of extreme weather events such as hurricanes and droughts."))
                .build();

        Double score = faithfulnessMetric.singleTurnScore(sample);

        log.info("Question: {}", sample.getUserInput());
        log.info("Faithfulness Score: {} (partial support from multiple contexts)", score);

        assertNotNull(score);
        assertTrue(
                score >= 0.5 && score <= 0.8,
                "Expected medium faithfulness - some claims supported, prediction not in context, got: " + score);
    }

    // ==================== ASYNC TESTS ====================

    @Test
    @DisplayName("Async evaluation test")
    void testAsyncEvaluation() throws Exception {
        log.info("=== Async Faithfulness Evaluation ===");

        Sample sample = Sample.builder()
                .userInput("What is machine learning?")
                .response(
                        "Machine learning is a branch of artificial intelligence that uses data and algorithms to learn.")
                .retrievedContexts(
                        List.of("Machine learning is a branch of artificial intelligence (AI) and computer science "
                                + "which focuses on the use of data and algorithms to imitate the way that humans learn, "
                                + "gradually improving accuracy over time."))
                .build();

        long startTime = System.currentTimeMillis();
        CompletableFuture<Double> asyncScore = faithfulnessMetric.singleTurnScoreAsync(sample);
        Double score = asyncScore.get();
        long endTime = System.currentTimeMillis();

        log.info("Async execution time: {} ms", (endTime - startTime));
        log.info("Faithfulness Score: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.7, "Expected high faithfulness for well-aligned ML definition, got: " + score);
    }

    @Test
    @DisplayName("Parallel evaluation of multiple samples")
    void testParallelEvaluation() {
        log.info("=== Parallel Faithfulness Evaluation ===");

        Sample sample1 = Sample.builder()
                .userInput("What is Python?")
                .response("Python is a high-level programming language.")
                .retrievedContexts(
                        List.of("Python is a high-level, interpreted programming language known for its simplicity."))
                .build();

        Sample sample2 = Sample.builder()
                .userInput("What is Java?")
                .response("Java is an object-oriented programming language.")
                .retrievedContexts(List.of(
                        "Java is a class-based, object-oriented programming language designed for portability."))
                .build();

        Sample sample3 = Sample.builder()
                .userInput("What is JavaScript?")
                .response("JavaScript is a programming language.")
                .retrievedContexts(
                        List.of(
                                "JavaScript is a programming language that is one of the core technologies of the World Wide Web."))
                .build();

        long startTime = System.currentTimeMillis();

        CompletableFuture<Double> future1 = faithfulnessMetric.singleTurnScoreAsync(sample1);
        CompletableFuture<Double> future2 = faithfulnessMetric.singleTurnScoreAsync(sample2);
        CompletableFuture<Double> future3 = faithfulnessMetric.singleTurnScoreAsync(sample3);

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(future1, future2, future3);
        allFutures.join();

        long endTime = System.currentTimeMillis();

        Double score1 = future1.join();
        Double score2 = future2.join();
        Double score3 = future3.join();

        log.info("Parallel execution time: {} ms", (endTime - startTime));
        log.info("Python faithfulness: {}", score1);
        log.info("Java faithfulness: {}", score2);
        log.info("JavaScript faithfulness: {}", score3);

        assertNotNull(score1);
        assertNotNull(score2);
        assertNotNull(score3);

        // All should have high faithfulness
        assertTrue(score1 >= 0.8, "Python definition should be faithful");
        assertTrue(score2 >= 0.8, "Java definition should be faithful");
        assertTrue(score3 >= 0.8, "JavaScript definition should be faithful");
    }
}
