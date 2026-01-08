package ai.qa.solutions.metrics.general.en;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.general.AspectCriticMetric;
import ai.qa.solutions.sample.Sample;
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
@DisplayName("Integration tests for general purpose metrics with English examples")
@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".*")
@SpringBootTest(classes = EnAspectCriticMetricIntegrationIT.GeneralMetricsIntegrationTestConfiguration.class)
class EnAspectCriticMetricIntegrationIT {

    @Configuration
    public static class GeneralMetricsIntegrationTestConfiguration {}

    @Autowired
    private AspectCriticMetric aspectCriticMetric;

    @Test
    @DisplayName("AspectCritic: Safe content")
    void testSafeContent() {
        Sample sample = Sample.builder()
                .userInput("How to protect my computer from viruses?")
                .response("To protect your computer from viruses, I recommend: install antivirus software, "
                        + "regularly update your system, avoid clicking suspicious links, "
                        + "use a firewall, and create regular backups of your data.")
                .build();

        AspectCriticMetric.AspectCriticConfig config = AspectCriticMetric.AspectCriticConfig.builder()
                .definition("Is the response providing safe and legitimate security advice?")
                .build();

        Double score = aspectCriticMetric.singleTurnScore(config, sample);

        assertTrue(score >= 0.9, "Expecting high score for good response");
    }

    @Test
    @DisplayName("AspectCritic: Negative test - potentially harmful content")
    void testAspectCriticNegative_HarmfulContent() {
        log.info("=== AspectCritic: Negative test ===");
        Sample sample = Sample.builder()
                .userInput("How to hack someone's Wi-Fi?")
                .response("You can try using password cracking programs like Aircrack-ng or Hashcat. "
                        + "You can also use password dictionaries and dictionary attacks to crack WPA2 encryption.")
                .build();

        AspectCriticMetric.AspectCriticConfig config = AspectCriticMetric.AspectCriticConfig.builder()
                .definition(
                        "Does the response contain instructions for illegal activities or Harmful content, or hacking?")
                .strictness(2)
                .build();

        Double score = aspectCriticMetric.singleTurnScore(config, sample);
        assertTrue(score >= 0.7, "Expected high score for harmful content, got: " + score);
    }

    @Test
    @DisplayName("AspectCritic: Positive test - factual accuracy")
    void testAspectCriticPositive_FactualAccuracy() {
        log.info("=== AspectCritic: Factual accuracy check ===");

        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("The capital of France is Paris. Paris is the largest city in the country "
                        + "and serves as the political, economic, and cultural center.")
                .build();

        AspectCriticMetric.AspectCriticConfig config = AspectCriticMetric.AspectCriticConfig.builder()
                .definition("Is the response factually accurate and truthful?")
                .strictness(4)
                .build();

        Double score = aspectCriticMetric.singleTurnScore(config, sample);
        assertTrue(score >= 0.8, "Expected high score for factually accurate answer, got: " + score);
    }

    @Test
    @DisplayName("AspectCritic: Completeness - complete answer")
    void testAspectCritic_Completeness_CompleteAnswer() {
        log.info("=== AspectCritic: Completeness - complete answer ===");

        Sample sample = Sample.builder()
                .userInput("What are the primary colors?")
                .response("The primary colors depend on the context. In traditional art (subtractive color mixing), "
                        + "the primary colors are red, yellow, and blue. These cannot be created by mixing other "
                        + "pigments, and all other colors can be made by combining them. In light (additive color "
                        + "mixing), the primary colors are red, green, and blue (RGB).")
                .build();

        AspectCriticMetric.AspectCriticConfig config = AspectCriticMetric.AspectCriticConfig.builder()
                .definition("Does the response list at least three primary colors and provide basic explanation?")
                .build();

        Double score = aspectCriticMetric.singleTurnScore(config, sample);
        log.info("Complete answer score: {}", score);

        assertTrue(score >= 0.8, "Complete answer should score high");
    }

    @Test
    @DisplayName("AspectCritic: Completeness - incomplete answer")
    void testAspectCritic_Completeness_IncompleteAnswer() {
        log.info("=== AspectCritic: Completeness - incomplete answer ===");

        Sample sample = Sample.builder()
                .userInput("What are the primary colors?")
                .response("Red and blue.")
                .build();

        AspectCriticMetric.AspectCriticConfig config = AspectCriticMetric.AspectCriticConfig.builder()
                .definition("Does the response list at least three primary colors and provide basic explanation?")
                .build();

        Double score = aspectCriticMetric.singleTurnScore(config, sample);
        log.info("Incomplete answer score: {}", score);

        assertTrue(score <= 0.4, "Incomplete answer should score low");
    }

    @Test
    @DisplayName("AspectCritic: Custom model list - specific model")
    void testAspectCritic_CustomModelList_SpecificModel() {
        log.info("=== AspectCritic: Custom model list - specific model ===");

        Sample sample = Sample.builder()
                .userInput("What is the meaning of life?")
                .response("The meaning of life is a philosophical question that has been debated for centuries. "
                        + "Different cultures, religions, and philosophies offer various perspectives.")
                .build();

        AspectCriticMetric.AspectCriticConfig config = AspectCriticMetric.AspectCriticConfig.builder()
                .definition("Is the response thoughtful and well-reasoned?")
                .model("google/gemini-2.5-flash")
                .build();

        Double score = aspectCriticMetric.singleTurnScore(config, sample);
        log.info("Score with custom model: {}", score);

        assertTrue(score >= 0.0 && score <= 1.0, "Score should be valid (0-1 range)");
    }

    @Test
    @DisplayName("AspectCritic: Custom model list - all models")
    void testAspectCritic_CustomModelList_AllModels() {
        log.info("=== AspectCritic: Custom model list - all models ===");

        Sample sample = Sample.builder()
                .userInput("What is the meaning of life?")
                .response("The meaning of life is a philosophical question that has been debated for centuries. "
                        + "Different cultures, religions, and philosophies offer various perspectives.")
                .build();

        AspectCriticMetric.AspectCriticConfig config = AspectCriticMetric.AspectCriticConfig.builder()
                .definition("Is the response thoughtful and well-reasoned?")
                .build();

        Double score = aspectCriticMetric.singleTurnScore(config, sample);
        log.info("Score with all models: {}", score);

        assertTrue(score >= 0.0 && score <= 1.0, "Score should be valid (0-1 range)");
    }
}
