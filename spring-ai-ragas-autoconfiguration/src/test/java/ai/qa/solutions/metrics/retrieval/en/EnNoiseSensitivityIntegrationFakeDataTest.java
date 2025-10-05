package ai.qa.solutions.metrics.retrieval.en;

import static org.junit.jupiter.api.Assertions.*;

import ai.qa.solutions.metrics.retrieval.NoiseSensitivityMetric;
import ai.qa.solutions.sample.Sample;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

@Slf4j
@EnableAutoConfiguration
@SuppressWarnings("LoggingSimilarMessage")
@DisplayName("Noise Sensitivity Metric Integration Tests - English")
@SpringBootTest(classes = EnNoiseSensitivityIntegrationFakeDataTest.EnNoiseSensitivityTestConfiguration.class)
class EnNoiseSensitivityIntegrationFakeDataTest {

    @Configuration
    public static class EnNoiseSensitivityTestConfiguration {}

    @Autowired
    private NoiseSensitivityMetric noiseSensitivityMetric;

    @Test
    @DisplayName("RELEVANT mode: Perfect response - should have 0 noise")
    void testRelevantMode_PerfectResponse() {
        log.info("=== Testing RELEVANT Mode: Perfect Response ===");

        Sample sample = Sample.builder()
                .userInput("Tell me about the Zenith Tower.")
                .response(
                        "The Zenith Tower was built in 1889. It is located in Novara, Helvetia. It was designed by Marcus Chen.")
                .reference(
                        "The Zenith Tower was built in 1889. It is located in Novara, Helvetia. It was designed by Marcus Chen.")
                .retrievedContexts(List.of(
                        "The Zenith Tower was constructed in 1889 for the World Exposition.",
                        "The tower is located in Novara, Helvetia.",
                        "Marcus Chen was the architect who designed the tower."))
                .build();

        NoiseSensitivityMetric.NoiseSensitivityConfig config = NoiseSensitivityMetric.NoiseSensitivityConfig.builder()
                .mode(NoiseSensitivityMetric.NoiseSensitivityMode.RELEVANT)
                .build();

        Double score = noiseSensitivityMetric.singleTurnScore(config, sample);

        log.info("Score: {}", score);
        assertNotNull(score);
        assertEquals(0.0, score, 0.01, "Perfect response should have 0 noise");
    }

    @Test
    @DisplayName("RELEVANT mode: Error NOT from relevant context - should have 0 noise")
    void testRelevantMode_ErrorNotFromRelevantContext() {
        log.info("=== Testing RELEVANT Mode: Error Not From Relevant Context ===");

        Sample sample = Sample.builder()
                .userInput("Tell me about the Zenith Tower.")
                .response(
                        "The Zenith Tower was built in 1889. It is located in Aldoria, Concordia. It was designed by Marcus Chen.")
                .reference(
                        "The Zenith Tower was built in 1889. It is located in Novara, Helvetia. It was designed by Marcus Chen.")
                .retrievedContexts(List.of(
                        "The Zenith Tower was constructed in 1889 for the World Exposition.",
                        "The tower is located in Novara, Helvetia.",
                        "Marcus Chen was the architect who designed the tower."))
                .build();

        NoiseSensitivityMetric.NoiseSensitivityConfig config = NoiseSensitivityMetric.NoiseSensitivityConfig.builder()
                .mode(NoiseSensitivityMetric.NoiseSensitivityMode.RELEVANT)
                .build();

        Double score = noiseSensitivityMetric.singleTurnScore(config, sample);

        log.info("Score: {}", score);
        assertNotNull(score);
        assertEquals(0.0, score, 0.01, "Error (Aldoria) is NOT supported by any relevant context, so noise = 0");
    }

    @Test
    @DisplayName("RELEVANT mode: Error IS from relevant context - should have HIGH noise")
    void testRelevantMode_ErrorFromRelevantContext() {
        log.info("=== Testing RELEVANT Mode: Error From Relevant Context ===");

        Sample sample = Sample.builder()
                .userInput("Tell me about the Zenith Tower.")
                .response(
                        "The Zenith Tower was built in 1889. It is located in Novara, Concordia. It was designed by Marcus Chen.")
                .reference(
                        "The Zenith Tower was built in 1889. It is located in Novara, Helvetia. It was designed by Marcus Chen.")
                .retrievedContexts(List.of(
                        "The Zenith Tower was constructed in 1889 for the World Exposition.",
                        // RELEVANT (supports "built in 1889") AND misleading (says Concordia)
                        "The Zenith Tower was built in 1889 and is located in Novara, Concordia.",
                        "Marcus Chen was the architect who designed the tower."))
                .build();

        NoiseSensitivityMetric.NoiseSensitivityConfig config = NoiseSensitivityMetric.NoiseSensitivityConfig.builder()
                .mode(NoiseSensitivityMetric.NoiseSensitivityMode.RELEVANT)
                .build();

        Double score = noiseSensitivityMetric.singleTurnScore(config, sample);

        log.info("Score: {}", score);
        assertNotNull(score);
        assertTrue(
                score >= 0.2 && score <= 0.5,
                "Error 'Concordia' IS supported by relevant context, expected 0.2-0.5, got: " + score);
    }

    @Test
    @DisplayName("IRRELEVANT mode: Error from irrelevant context - should have HIGH noise")
    void testIrrelevantMode_ErrorFromIrrelevantContext() {
        log.info("=== Testing IRRELEVANT Mode: Error From Irrelevant Context ===");

        Sample sample = Sample.builder()
                .userInput("Tell me about the Zenith Tower.")
                .response(
                        "The Zenith Tower was built in 1889. It is located in Novara, Concordia. It was designed by Marcus Chen.")
                .reference(
                        "The Zenith Tower was built in 1889. It is located in Novara, Helvetia. It was designed by Marcus Chen.")
                .retrievedContexts(List.of(
                        "The Zenith Tower was constructed in 1889 for the World Exposition.",
                        // IRRELEVANT (doesn't support any reference facts) but misleading
                        "The tower is located in Novara, the capital of Concordia.",
                        "Marcus Chen was the architect who designed the tower."))
                .build();

        NoiseSensitivityMetric.NoiseSensitivityConfig config = NoiseSensitivityMetric.NoiseSensitivityConfig.builder()
                .mode(NoiseSensitivityMetric.NoiseSensitivityMode.IRRELEVANT)
                .build();

        Double score = noiseSensitivityMetric.singleTurnScore(config, sample);

        log.info("Score: {}", score);
        assertNotNull(score);
        assertTrue(
                score >= 0.2 && score <= 0.5,
                "Error from irrelevant misleading context, expected 0.2-0.5, got: " + score);
    }

    @Test
    @DisplayName("IRRELEVANT mode: Perfect response ignores irrelevant contexts - should have 0 noise")
    void testIrrelevantMode_IgnoresIrrelevantContexts() {
        log.info("=== Testing IRRELEVANT Mode: Ignores Irrelevant ===");

        Sample sample = Sample.builder()
                .userInput("Tell me about the Zenith Tower.")
                .response(
                        "The Zenith Tower was built in 1889. It is located in Novara, Helvetia. It was designed by Marcus Chen.")
                .reference(
                        "The Zenith Tower was built in 1889. It is located in Novara, Helvetia. It was designed by Marcus Chen.")
                .retrievedContexts(List.of(
                        "The Zenith Tower was constructed in 1889 for the World Exposition.",
                        "The tower is located in Novara, the capital of Concordia.", // Irrelevant + wrong
                        "Concordian architecture is known for its precision.", // Irrelevant
                        "Marcus Chen was the architect who designed the tower."))
                .build();

        NoiseSensitivityMetric.NoiseSensitivityConfig config = NoiseSensitivityMetric.NoiseSensitivityConfig.builder()
                .mode(NoiseSensitivityMetric.NoiseSensitivityMode.IRRELEVANT)
                .build();

        Double score = noiseSensitivityMetric.singleTurnScore(config, sample);

        log.info("Score: {}", score);
        assertNotNull(score);
        assertEquals(0.0, score, 0.01, "Response doesn't use irrelevant contexts, so noise = 0");
    }

    @Test
    @DisplayName("Edge case: No contexts - should return 0")
    void testNoContexts() {
        Sample sample = Sample.builder()
                .userInput("Tell me about the Zenith Tower.")
                .response("The Zenith Tower was built in 1889.")
                .reference("The Zenith Tower was built in 1889.")
                .retrievedContexts(List.of())
                .build();

        NoiseSensitivityMetric.NoiseSensitivityConfig config = NoiseSensitivityMetric.NoiseSensitivityConfig.builder()
                .mode(NoiseSensitivityMetric.NoiseSensitivityMode.RELEVANT)
                .build();

        Double score = noiseSensitivityMetric.singleTurnScore(config, sample);
        assertEquals(0.0, score);
    }

    @Test
    @DisplayName("Edge case: Empty response - should return 0")
    void testEmptyResponse() {
        Sample sample = Sample.builder()
                .userInput("Tell me about the Zenith Tower.")
                .response("")
                .reference("The Zenith Tower was built in 1889.")
                .retrievedContexts(List.of("Some context"))
                .build();

        NoiseSensitivityMetric.NoiseSensitivityConfig config = NoiseSensitivityMetric.NoiseSensitivityConfig.builder()
                .mode(NoiseSensitivityMetric.NoiseSensitivityMode.RELEVANT)
                .build();

        Double score = noiseSensitivityMetric.singleTurnScore(config, sample);
        assertEquals(0.0, score);
    }
}
