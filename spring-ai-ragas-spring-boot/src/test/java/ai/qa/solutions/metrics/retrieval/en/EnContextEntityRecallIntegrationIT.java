package ai.qa.solutions.metrics.retrieval.en;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.retrieval.ContextEntityRecallMetric;
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
@DisplayName("Context Entity Recall Metric Integration Tests - English")
@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".*")
@SpringBootTest(classes = EnContextEntityRecallIntegrationIT.ContextEntityRecallIntegrationTestConfiguration.class)
class EnContextEntityRecallIntegrationIT {

    @Configuration
    public static class ContextEntityRecallIntegrationTestConfiguration {}

    @Autowired
    private ContextEntityRecallMetric contextEntityRecallMetric;

    // ==================== BASIC FUNCTIONALITY TESTS ====================

    @Test
    @DisplayName("Context Entity Recall: High entity coverage - most entities found")
    void testContextEntityRecall_HighCoverage() {
        log.info("=== Testing Context Entity Recall - High Coverage ===");

        Sample sample = Sample.builder()
                .reference(
                        "The Eiffel Tower is located in Paris, France. It was completed in 1889 for the World's Fair.")
                .retrievedContexts(List.of(
                        "The Eiffel Tower, located in Paris, France, is one of the most iconic landmarks globally.",
                        "Completed in 1889, it was constructed in time for the 1889 World's Fair.",
                        "Millions of visitors are attracted to it each year for its breathtaking views of the city."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        Double score = contextEntityRecallMetric.singleTurnScore(config, sample);

        log.info("Reference: {}", sample.getReference());
        log.info("Context Entity Recall Score (high coverage): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.7, "Expected high score for good entity coverage, got: " + score);
    }

    @Test
    @DisplayName("Context Entity Recall: Partial entity coverage - some entities missing")
    void testContextEntityRecall_PartialCoverage() {
        log.info("=== Testing Context Entity Recall - Partial Coverage ===");

        Sample sample = Sample.builder()
                .reference(
                        "The Taj Mahal is located in Agra, India. It was built by Shah Jahan in 1631 for his wife Mumtaz Mahal.")
                .retrievedContexts(List.of(
                        "The Taj Mahal is a beautiful monument in India.",
                        "It was constructed by the Mughal emperor Shah Jahan.",
                        "The structure is made of white marble and attracts millions of tourists."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        Double score = contextEntityRecallMetric.singleTurnScore(config, sample);

        log.info("Reference: {}", sample.getReference());
        log.info("Context Entity Recall Score (partial coverage): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.3 && score <= 0.8, "Expected moderate score for partial entity coverage, got: " + score);
    }

    @Test
    @DisplayName("Context Entity Recall: Poor entity coverage - most entities missing")
    void testContextEntityRecall_PoorCoverage() {
        log.info("=== Testing Context Entity Recall - Poor Coverage ===");

        Sample sample = Sample.builder()
                .reference(
                        "Albert Einstein was born in Ulm, Germany on March 14, 1879. He won the Nobel Prize in Physics in 1921.")
                .retrievedContexts(List.of(
                        "Physics is a fundamental science that studies matter and energy.",
                        "Scientists have made many important discoveries throughout history.",
                        "Awards and prizes recognize outstanding achievements in various fields."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        Double score = contextEntityRecallMetric.singleTurnScore(config, sample);

        log.info("Reference: {}", sample.getReference());
        log.info("Context Entity Recall Score (poor coverage): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score <= 0.3, "Expected low score for poor entity coverage, got: " + score);
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Edge case: Empty retrieved contexts")
    void testContextEntityRecall_EmptyContexts() {
        log.info("=== Testing Edge Case - Empty Retrieved Contexts ===");

        Sample sample = Sample.builder()
                .reference("The Statue of Liberty is located in New York Harbor.")
                .retrievedContexts(List.of())
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        Double score = contextEntityRecallMetric.singleTurnScore(config, sample);

        log.info("Score for empty contexts: {}", score);

        assertNotNull(score);
        assertEquals(0.0, score, "Expected 0.0 for empty retrieved contexts, got: " + score);
    }

    @Test
    @DisplayName("Edge case: Empty reference")
    void testContextEntityRecall_EmptyReference() {
        log.info("=== Testing Edge Case - Empty Reference ===");

        Sample sample = Sample.builder()
                .reference("")
                .retrievedContexts(List.of("Paris is the capital of France.", "The Eiffel Tower is a famous landmark."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        Double score = contextEntityRecallMetric.singleTurnScore(config, sample);

        log.info("Score for empty reference: {}", score);

        assertNotNull(score);
        assertEquals(0.0, score, "Expected 0.0 for empty reference, got: " + score);
    }

    @Test
    @DisplayName("Edge case: No entities in reference")
    void testContextEntityRecall_NoEntitiesInReference() {
        log.info("=== Testing Edge Case - No Entities in Reference ===");

        Sample sample = Sample.builder()
                .reference("This is very important and interesting.")
                .retrievedContexts(List.of(
                        "The Eiffel Tower is located in Paris, France.",
                        "Napoleon Bonaparte was a French military leader."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        Double score = contextEntityRecallMetric.singleTurnScore(config, sample);

        log.info("Score for reference with no entities: {}", score);

        assertNotNull(score);
        assertEquals(0.0, score, "Expected 0.0 when no entities in reference, got: " + score);
    }

    @Test
    @DisplayName("Edge case: Single entity in reference")
    void testContextEntityRecall_SingleEntity() {
        log.info("=== Testing Edge Case - Single Entity ===");

        Sample sample = Sample.builder()
                .reference("Paris is beautiful.")
                .retrievedContexts(List.of(
                        "Paris is the capital of France and known for its culture.",
                        "The city attracts millions of tourists every year."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        Double score = contextEntityRecallMetric.singleTurnScore(config, sample);

        log.info("Score for single entity: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.8, "Expected high score for single covered entity, got: " + score);
    }

    // ==================== COMPLEX SCENARIOS ====================

    @Test
    @DisplayName("Complex scenario: Historical entities and dates")
    void testContextEntityRecall_HistoricalEntities() {
        log.info("=== Testing Complex Scenario - Historical Entities ===");

        Sample sample = Sample.builder()
                .reference(
                        "World War II lasted from 1939 to 1945. Adolf Hitler led Nazi Germany while Winston Churchill was the British Prime Minister.")
                .retrievedContexts(List.of(
                        "World War II was a global conflict that lasted from 1939 to 1945.",
                        "Adolf Hitler was the leader of Nazi Germany during the war.",
                        "Winston Churchill served as the Prime Minister of the United Kingdom.",
                        "The war involved many countries and resulted in significant casualties."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        Double score = contextEntityRecallMetric.singleTurnScore(config, sample);

        log.info("Reference: {}", sample.getReference());
        log.info("Context Entity Recall Score (historical entities): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.7, "Expected high score for well-covered historical entities, got: " + score);
    }

    @Test
    @DisplayName("Complex scenario: Geographic entities")
    void testContextEntityRecall_GeographicEntities() {
        log.info("=== Testing Complex Scenario - Geographic Entities ===");

        Sample sample = Sample.builder()
                .reference(
                        "The Amazon River flows through Brazil, Peru, and Colombia. It is approximately 6,400 kilometers long.")
                .retrievedContexts(List.of(
                        "The Amazon River is the longest river in South America.",
                        "It flows through several countries including Brazil and Peru.",
                        "The river is approximately 6,400 kilometers in length.",
                        "The Amazon basin covers a vast area of tropical rainforest."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        Double score = contextEntityRecallMetric.singleTurnScore(config, sample);

        log.info("Reference: {}", sample.getReference());
        log.info("Context Entity Recall Score (geographic entities): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.6, "Expected good score for geographic entities, got: " + score);
    }

    @Test
    @DisplayName("Complex scenario: Scientific entities and measurements")
    void testContextEntityRecall_ScientificEntities() {
        log.info("=== Testing Complex Scenario - Scientific Entities ===");

        Sample sample = Sample.builder()
                .reference(
                        "The speed of light is 299,792,458 meters per second. Albert Einstein discovered this in his theory of relativity in 1905.")
                .retrievedContexts(List.of(
                        "The speed of light in vacuum is approximately 299,792,458 meters per second.",
                        "Albert Einstein developed the theory of relativity.",
                        "Einstein published his special theory of relativity in 1905.",
                        "This discovery revolutionized our understanding of space and time."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        Double score = contextEntityRecallMetric.singleTurnScore(config, sample);

        log.info("Reference: {}", sample.getReference());
        log.info("Context Entity Recall Score (scientific entities): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.7, "Expected high score for well-covered scientific entities, got: " + score);
    }

    @Test
    @DisplayName("Complex scenario: Mixed relevant and irrelevant contexts")
    void testContextEntityRecall_MixedContexts() {
        log.info("=== Testing Complex Scenario - Mixed Contexts ===");

        Sample sample = Sample.builder()
                .reference(
                        "The Apollo 11 mission launched on July 16, 1969. Neil Armstrong and Buzz Aldrin landed on the Moon.")
                .retrievedContexts(List.of(
                        "The Apollo 11 mission was a historic space mission.",
                        "Neil Armstrong was the first person to walk on the Moon.",
                        "The weather today is sunny and pleasant.",
                        "Buzz Aldrin accompanied Armstrong on the lunar surface.",
                        "Many people enjoy watching movies about space exploration.",
                        "The mission launched on July 16, 1969 from Kennedy Space Center."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        Double score = contextEntityRecallMetric.singleTurnScore(config, sample);

        log.info("Reference: {}", sample.getReference());
        log.info("Context Entity Recall Score (mixed contexts): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.7, "Expected good score despite irrelevant contexts, got: " + score);
    }

    // ==================== ASYNC TESTS ====================

    @Test
    @DisplayName("Async evaluation test")
    void testContextEntityRecall_AsyncEvaluation() throws Exception {
        log.info("=== Testing Async Evaluation ===");

        Sample sample = Sample.builder()
                .reference(
                        "The Great Wall of China stretches over 21,196 kilometers. It was built during the Ming Dynasty.")
                .retrievedContexts(List.of(
                        "The Great Wall of China is one of the most famous landmarks in the world.",
                        "It stretches over 21,196 kilometers across northern China.",
                        "Much of the wall that exists today was built during the Ming Dynasty.",
                        "It was constructed to protect against invasions from the north."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        long startTime = System.currentTimeMillis();
        CompletableFuture<Double> asyncScore = contextEntityRecallMetric.singleTurnScoreAsync(config, sample);
        Double score = asyncScore.get();
        long endTime = System.currentTimeMillis();

        log.info("Async execution time: {} ms", (endTime - startTime));
        log.info("Async score: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.65, "Expected high score for well-covered entities, got: " + score);
    }

    @Test
    @DisplayName("Parallel evaluation with multiple samples")
    void testContextEntityRecall_ParallelEvaluation() {
        log.info("=== Testing Parallel Evaluation ===");

        Sample sample1 = Sample.builder()
                .reference(
                        "The Louvre Museum is located in Paris, France. It houses the Mona Lisa painted by Leonardo da Vinci.")
                .retrievedContexts(List.of(
                        "The Louvre Museum is one of the world's largest art museums.",
                        "It is located in Paris, the capital of France.",
                        "The museum houses the famous Mona Lisa painting.",
                        "Leonardo da Vinci created this masterpiece in the early 16th century."))
                .build();

        Sample sample2 = Sample.builder()
                .reference(
                        "Mount Everest is located in the Himalayas between Nepal and Tibet. It stands 8,848 meters tall.")
                .retrievedContexts(List.of(
                        "Mount Everest is the highest mountain in the world.",
                        "It is located in the Himalayas on the border between Nepal and Tibet.",
                        "The mountain reaches a height of 8,848 meters above sea level.",
                        "Many climbers attempt to reach its summit each year."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        long startTime = System.currentTimeMillis();

        CompletableFuture<Double> score1Future = contextEntityRecallMetric.singleTurnScoreAsync(config, sample1);
        CompletableFuture<Double> score2Future = contextEntityRecallMetric.singleTurnScoreAsync(config, sample2);

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(score1Future, score2Future);
        allFutures.join();
        long endTime = System.currentTimeMillis();

        Double score1 = score1Future.join();
        Double score2 = score2Future.join();

        log.info("Parallel execution time: {} ms", (endTime - startTime));
        log.info("Sample 1 Context Entity Recall: {}", score1);
        log.info("Sample 2 Context Entity Recall: {}", score2);

        assertNotNull(score1);
        assertNotNull(score2);
        assertTrue(score1 >= 0.0 && score1 <= 1.0);
        assertTrue(score2 >= 0.0 && score2 <= 1.0);
        assertTrue(score1 >= 0.6, "Expected high score for Louvre sample");
        assertTrue(score2 >= 0.6, "Expected high score for Everest sample");
    }

    // ==================== ENTITY COVERAGE ASSESSMENT ====================

    @Test
    @DisplayName("Entity coverage: Tourism use case")
    void testContextEntityRecall_TourismUseCase() {
        log.info("=== Testing Entity Coverage - Tourism Use Case ===");

        Sample sample = Sample.builder()
                .reference(
                        "Visit the Colosseum in Rome, Italy. It was built by Emperor Vespasian in AD 70 and completed by Titus in AD 80.")
                .retrievedContexts(List.of(
                        "The Colosseum is an ancient amphitheater in Rome, the capital of Italy.",
                        "Construction of the Colosseum began under Emperor Vespasian around AD 70.",
                        "The structure was completed during the reign of his son Titus in AD 80.",
                        "It could accommodate between 50,000 and 80,000 spectators for gladiatorial games."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        Double score = contextEntityRecallMetric.singleTurnScore(config, sample);

        log.info("Reference: {}", sample.getReference());
        log.info("Context Entity Recall Score (tourism use case): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.7, "Expected high score for tourism entities coverage, got: " + score);
    }

    @Test
    @DisplayName("Entity coverage: Historical QA use case")
    void testContextEntityRecall_HistoricalQAUseCase() {
        log.info("=== Testing Entity Coverage - Historical QA Use Case ===");

        Sample sample = Sample.builder()
                .reference(
                        "The American Civil War lasted from 1861 to 1865. Abraham Lincoln was President during this period. The war ended with the assassination of Lincoln in April 1865.")
                .retrievedContexts(List.of(
                        "The American Civil War was fought from 1861 to 1865.",
                        "Abraham Lincoln served as the 16th President of the United States.",
                        "Lincoln was assassinated in April 1865, shortly after the war ended.",
                        "The conflict was primarily about slavery and states' rights."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        Double score = contextEntityRecallMetric.singleTurnScore(config, sample);

        log.info("Reference: {}", sample.getReference());
        log.info("Context Entity Recall Score (historical QA): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.7, "Expected high score for historical entities coverage, got: " + score);
    }

    @Test
    @DisplayName("Entity coverage: Different entity types")
    void testContextEntityRecall_DifferentEntityTypes() {
        log.info("=== Testing Entity Coverage - Different Entity Types ===");

        Sample sample = Sample.builder()
                .reference(
                        "NASA launched the Hubble Space Telescope on April 24, 1990. It orbits Earth at 547 kilometers altitude and has captured over 1.5 million observations.")
                .retrievedContexts(List.of(
                        "NASA, the United States space agency, launched the Hubble Space Telescope.",
                        "The launch took place on April 24, 1990 aboard the Space Shuttle Discovery.",
                        "Hubble orbits Earth at an altitude of approximately 547 kilometers.",
                        "The telescope has made over 1.5 million scientific observations since its deployment."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        Double score = contextEntityRecallMetric.singleTurnScore(config, sample);

        log.info("Reference: {}", sample.getReference());
        log.info("Context Entity Recall Score (different entity types): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.7, "Expected high score for diverse entity types coverage, got: " + score);
    }

    // ==================== RETRIEVAL MECHANISM COMPARISON ====================

    @Test
    @DisplayName("Retrieval comparison: High entity coverage")
    void testContextEntityRecall_RetrievalComparison_HighCoverage() {
        log.info("=== Testing Retrieval Comparison - High Coverage ===");

        String reference =
                "The Renaissance began in Florence, Italy in the 14th century. Leonardo da Vinci and Michelangelo were famous Renaissance artists.";

        // High entity coverage retrieval
        Sample highCoverageSample = Sample.builder()
                .reference(reference)
                .retrievedContexts(List.of(
                        "The Renaissance was a cultural movement that began in Florence, Italy.",
                        "The movement started in the 14th century and spread throughout Europe.",
                        "Leonardo da Vinci was a polymath and artist of the Renaissance period.",
                        "Michelangelo was another renowned Renaissance artist known for his sculptures and paintings."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        Double highScore = contextEntityRecallMetric.singleTurnScore(config, highCoverageSample);

        log.info("High coverage retrieval score: {}", highScore);

        assertNotNull(highScore);
        assertTrue(highScore >= 0.0 && highScore <= 1.0);
        assertTrue(highScore >= 0.7, "Expected high score for good entity coverage retrieval");
    }

    @Test
    @DisplayName("Retrieval comparison: Low entity coverage")
    void testContextEntityRecall_RetrievalComparison_LowCoverage() {
        log.info("=== Testing Retrieval Comparison - Low Coverage ===");

        String reference =
                "The Renaissance began in Florence, Italy in the 14th century. Leonardo da Vinci and Michelangelo were famous Renaissance artists.";

        // Low entity coverage retrieval
        Sample lowCoverageSample = Sample.builder()
                .reference(reference)
                .retrievedContexts(List.of(
                        "The Renaissance was an important cultural period.",
                        "Many artists created beautiful works during this time.",
                        "Art and culture flourished in European cities."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        Double lowScore = contextEntityRecallMetric.singleTurnScore(config, lowCoverageSample);

        log.info("Low coverage retrieval score: {}", lowScore);

        assertNotNull(lowScore);
        assertTrue(lowScore >= 0.0 && lowScore <= 1.0);
        assertTrue(lowScore <= 0.3, "Expected low score for poor entity coverage retrieval");
    }

    @Test
    @DisplayName("Case sensitivity: Entity matching variations")
    void testContextEntityRecall_CaseSensitivity() {
        log.info("=== Testing Case Sensitivity - Entity Matching ===");

        Sample sample = Sample.builder()
                .reference("Albert Einstein lived in Princeton, New Jersey.")
                .retrievedContexts(List.of(
                        "albert einstein was a famous physicist.",
                        "He lived in princeton, new jersey for many years.",
                        "PRINCETON is located in NEW JERSEY state."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        Double score = contextEntityRecallMetric.singleTurnScore(config, sample);

        log.info("Reference: {}", sample.getReference());
        log.info("Context Entity Recall Score (case variations): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.7, "Expected high score with case-insensitive matching, got: " + score);
    }
}
