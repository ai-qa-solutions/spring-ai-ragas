package ai.qa.solutions.metrics.general.en;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.general.RubricsScoreMetric;
import ai.qa.solutions.sample.Sample;
import java.util.Map;
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
@DisplayName("Integration tests for general purpose metrics with English examples")
@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".*")
@SpringBootTest(classes = EnRubricsScoreMetricIntegrationIT.GeneralMetricsIntegrationTestConfiguration.class)
class EnRubricsScoreMetricIntegrationIT {

    @Configuration
    public static class GeneralMetricsIntegrationTestConfiguration {}

    @Autowired
    private RubricsScoreMetric rubricsScoreMetric;

    @Test
    @DisplayName("RubricsScore: Positive test - excellent explanation")
    void testRubricsScorePositive_ExcellentExplanation() {
        log.info("=== RubricsScore: Positive test ===");

        Sample sample = Sample.builder()
                .userInput("Explain the process of photosynthesis")
                .response("Photosynthesis is a complex biochemical process by which plants convert light energy "
                        + "into chemical energy. The process occurs in chloroplasts and includes two main stages:"
                        + "light-dependent and light-independent reactions. In the light-dependent phase, "
                        + "chlorophyll absorbs sunlight, splitting water molecules and releasing oxygen. "
                        + "In the light-independent phase (Calvin cycle), carbon dioxide from the atmosphere "
                        + "is converted into glucose. The overall equation: 6CO₂ + 6H₂O + light energy → C₆H₁₂O₆+6 O₂.")
                .reference("Photosynthesis is the process of forming organic substances from CO₂ and water "
                        + "using light energy.")
                .build();

        RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                .rubrics(createPhotosynthesisRubrics())
                .build();

        Double score = rubricsScoreMetric.singleTurnScore(config, sample);

        log.info("Question: {}", sample.getUserInput());
        log.info("Response: {}", sample.getResponse());
        log.info("Rubrics score: {}", score);

        assertTrue(score >= 1.0 && score <= 5.0);
        assertTrue(score >= 4.0, "Expected high score for detailed scientific explanation, got: " + score);
    }

    @Test
    @DisplayName("RubricsScore: Negative test - superficial explanation")
    void testRubricsScoreNegative_SuperficialExplanation() {
        log.info("=== RubricsScore: Negative test ===");

        Sample sample = Sample.builder()
                .userInput("Explain the process of photosynthesis")
                .response(
                        "Photosynthesis is when plants do something with light. " + "They somehow use the sun to grow.")
                .reference("Photosynthesis is the process of forming organic substances from CO₂ and water "
                        + "using light energy.")
                .build();

        RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                .rubrics(createPhotosynthesisRubrics())
                .build();

        Double score = rubricsScoreMetric.singleTurnScore(config, sample);

        log.info("Question: {}", sample.getUserInput());
        log.info("Response: {}", sample.getResponse());
        log.info("Rubrics score: {}", score);

        assertTrue(score >= 1.0 && score <= 5.0);
        assertTrue(score <= 2.0, "Expected low score for superficial explanation, got: " + score);
    }

    @Test
    @DisplayName("RubricsScore: Essay evaluation - good essay")
    void testRubricsScore_EssayEvaluation_GoodEssay() {
        log.info("=== RubricsScore: Essay evaluation - good essay ===");

        Sample sample = Sample.builder()
                .userInput("Write an essay about the impact of technology on society")
                .response("Technological progress has fundamentally transformed modern society in profound ways. "
                        + "On the positive side, digital technologies have created unprecedented opportunities for "
                        + "global connectivity and knowledge sharing. For example, platforms like Coursera and Khan Academy "
                        + "have democratized education, enabling millions of learners worldwide to access high-quality courses "
                        + "from top universities. The Internet has revolutionized communication through tools like video conferencing, "
                        + "social media, and instant messaging, breaking down geographical barriers and fostering cross-cultural dialogue. "
                        + "Moreover, technological innovations in healthcare, such as telemedicine and AI-assisted diagnostics, "
                        + "have improved patient outcomes and expanded access to medical services in remote areas.\n\n"
                        + "However, these advances come with significant challenges that society must address. Digital inequality "
                        + "remains a pressing issue, as evidenced by the fact that nearly 3 billion people still lack internet access. "
                        + "Technology dependence has given rise to mental health concerns, including increased rates of anxiety "
                        + "and depression linked to excessive screen time and social media use. Privacy concerns have intensified "
                        + "with data breaches at major corporations like Facebook and Equifax exposing millions of users' personal information. "
                        + "Additionally, the spread of misinformation and the algorithmic amplification of divisive content pose threats "
                        + "to democratic discourse and social cohesion.\n\n"
                        + "In conclusion, while technology offers immense potential for human progress, realizing this potential requires "
                        + "a thoughtful balance between innovation and social responsibility. Policymakers, tech companies, and civil society "
                        + "must collaborate to ensure equitable access, protect user privacy, promote digital literacy, and create ethical "
                        + "frameworks that prioritize human well-being over profit. Only through such comprehensive efforts can we harness "
                        + "technology's transformative power for sustainable and inclusive development.")
                .reference("Essay about the impact of technology on society with examples and arguments")
                .build();

        RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                .rubrics(createEssayRubrics())
                .build();

        Double score = rubricsScoreMetric.singleTurnScore(config, sample);
        log.info("Good essay score: {}", score);

        assertTrue(score >= 4.0, "Good essay should receive high score");
    }

    @Test
    @DisplayName("RubricsScore: Essay evaluation - weak essay")
    void testRubricsScore_EssayEvaluation_WeakEssay() {
        log.info("=== RubricsScore: Essay evaluation - weak essay ===");

        Sample sample = Sample.builder()
                .userInput("Write an essay about the impact of technology on society")
                .response("Technology is good. There are phones and computers. "
                        + "People use the internet. It's convenient.")
                .reference("Essay about the impact of technology on society with examples and arguments")
                .build();

        RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                .rubrics(createEssayRubrics())
                .build();

        Double score = rubricsScoreMetric.singleTurnScore(config, sample);
        log.info("Weak essay score: {}", score);

        assertTrue(score <= 2.0, "Weak essay should receive low score");
    }

    @Test
    @DisplayName("RubricsScore: Code quality - good code")
    void testRubricsScore_CodeQuality_GoodCode() {
        log.info("=== RubricsScore: Code quality - good code ===");

        Sample sample = Sample.builder()
                .userInput("Write a function to calculate factorial")
                .response(
                        """
                                def factorial(n):
                                    '''Calculate factorial of n using recursion with validation'''
                                    if not isinstance(n, int) or n < 0:
                                        raise ValueError("Input must be a non-negative integer")
                                    if n == 0 or n == 1:
                                        return 1
                                    return n * factorial(n - 1)
                                """)
                .reference("Function to calculate factorial with proper error handling")
                .build();

        RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                .rubrics(createCodeQualityRubrics())
                .build();

        Double score = rubricsScoreMetric.singleTurnScore(config, sample);
        log.info("Good code score: {}", score);

        assertTrue(score >= 4.0, "Well-written code should score high");
    }

    @Test
    @DisplayName("RubricsScore: Code quality - poor code")
    void testRubricsScore_CodeQuality_PoorCode() {
        log.info("=== RubricsScore: Code quality - poor code ===");

        Sample sample = Sample.builder()
                .userInput("Write a function to calculate factorial")
                .response(
                        """
                                def f(n):
                                    if n == 0:
                                        return 1
                                    return n * f(n-1)
                                """)
                .reference("Function to calculate factorial with proper error handling")
                .build();

        RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                .rubrics(createCodeQualityRubrics())
                .build();

        Double score = rubricsScoreMetric.singleTurnScore(config, sample);
        log.info("Poor code score: {}", score);

        assertTrue(score <= 3.0, "Poor code should score lower");
    }

    @Test
    @DisplayName("RubricsScore: Custom model list - specific model")
    void testRubricsScore_CustomModelList_SpecificModel() {
        log.info("=== RubricsScore: Custom model list - specific model ===");

        Sample sample = Sample.builder()
                .userInput("What are the benefits of exercise?")
                .response(
                        "Regular exercise provides numerous health benefits including improved cardiovascular "
                                + "health, stronger muscles and bones, better mental health, weight management, "
                                + "and reduced risk of chronic diseases. It also boosts energy levels and improves sleep quality.")
                .reference("Benefits of regular physical activity for health")
                .build();

        RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                .rubrics(createPhotosynthesisRubrics())
                .model("google/gemini-2.5-flash")
                .build();

        Double score = rubricsScoreMetric.singleTurnScore(config, sample);
        log.info("Score with custom model: {}", score);

        assertTrue(score >= 1.0 && score <= 5.0, "Score should be valid (1-5 range)");
    }

    @Test
    @DisplayName("RubricsScore: Custom model list - all models")
    void testRubricsScore_CustomModelList_AllModels() {
        log.info("=== RubricsScore: Custom model list - all models ===");

        Sample sample = Sample.builder()
                .userInput("What are the benefits of exercise?")
                .response(
                        "Regular exercise provides numerous health benefits including improved cardiovascular "
                                + "health, stronger muscles and bones, better mental health, weight management, "
                                + "and reduced risk of chronic diseases. It also boosts energy levels and improves sleep quality.")
                .reference("Benefits of regular physical activity for health")
                .build();

        RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                .rubrics(createPhotosynthesisRubrics())
                .build();

        Double score = rubricsScoreMetric.singleTurnScore(config, sample);
        log.info("Score with all models: {}", score);

        assertTrue(score >= 1.0 && score <= 5.0, "Score should be valid (1-5 range)");
    }

    @Test
    @DisplayName("RubricsScore: Customer Support RAG - excellent response")
    void testRubricsScore_CustomerSupportRAG_ExcellentResponse() {
        log.info("=== RubricsScore: Customer Support RAG - excellent response ===");

        Sample sample = Sample.builder()
                .userInput("I can't log into my account. It keeps saying 'invalid credentials'.")
                .response("I understand how frustrating this must be. Let's resolve your login issue together. "
                        + "First, try resetting your password using the 'Forgot Password?' link on the login page. "
                        + "If that doesn't help, check if Caps Lock is on and try clearing your browser cache and cookies. "
                        + "If the problem persists, our technical support team can verify your account status - "
                        + "please contact us at support@company.com or call 1-800-555-1234. We're here to help!")
                .reference("Password reset: use 'Forgot Password?' link. Check Caps Lock. "
                        + "Clear cache/cookies. Contact support@company.com if issue persists.")
                .build();

        RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                .rubrics(createCustomerSupportRubrics())
                .build();

        Double score = rubricsScoreMetric.singleTurnScore(config, sample);
        log.info("Excellent support response score: {}", score);

        assertTrue(score >= 4.0, "Empathetic and complete support response should score high");
    }

    @Test
    @DisplayName("RubricsScore: Customer Support RAG - poor response")
    void testRubricsScore_CustomerSupportRAG_PoorResponse() {
        log.info("=== RubricsScore: Customer Support RAG - poor response ===");

        Sample sample = Sample.builder()
                .userInput("I can't log into my account. It keeps saying 'invalid credentials'.")
                .response("You probably entered the wrong password. Try again.")
                .reference("Password reset: use 'Forgot Password?' link. Check Caps Lock. "
                        + "Clear cache/cookies. Contact support@company.com if issue persists.")
                .build();

        RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                .rubrics(createCustomerSupportRubrics())
                .build();

        Double score = rubricsScoreMetric.singleTurnScore(config, sample);
        log.info("Poor support response score: {}", score);

        assertTrue(score <= 2.0, "Unhelpful and incomplete support response should score low");
    }

    @Test
    @DisplayName("RubricsScore: Technical Documentation RAG - accurate answer")
    void testRubricsScore_TechnicalDocumentation_AccurateAnswer() {
        log.info("=== RubricsScore: Technical Documentation RAG - accurate answer ===");

        Sample sample = Sample.builder()
                .userInput("How do I set up Redis caching in Spring Boot 3.x?")
                .response("To configure Redis caching in Spring Boot 3.x:\n"
                        + "1. Add dependencies: spring-boot-starter-data-redis and spring-boot-starter-cache\n"
                        + "2. In application.yml, specify: spring.redis.host=localhost, spring.redis.port=6379\n"
                        + "3. Enable caching with @EnableCaching annotation on your main class\n"
                        + "4. Configure RedisCacheManager with TTL: RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(10))\n"
                        + "5. Use annotations @Cacheable, @CacheEvict on your methods\n"
                        + "Example: @Cacheable(value = \"users\", key = \"#id\")")
                .reference("Redis in Spring Boot 3: add redis and cache dependencies, configure host/port, "
                        + "enable @EnableCaching, configure RedisCacheManager with TTL")
                .build();

        RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                .rubrics(createTechnicalDocumentationRubrics())
                .build();

        Double score = rubricsScoreMetric.singleTurnScore(config, sample);
        log.info("Accurate technical answer score: {}", score);

        assertTrue(score >= 4.0, "Detailed technical answer with code examples should score high");
    }

    @Test
    @DisplayName("RubricsScore: Technical Documentation RAG - incomplete answer")
    void testRubricsScore_TechnicalDocumentation_IncompleteAnswer() {
        log.info("=== RubricsScore: Technical Documentation RAG - incomplete answer ===");

        Sample sample = Sample.builder()
                .userInput("How do I set up Redis caching in Spring Boot 3.x?")
                .response("You need to add Redis dependencies and configure Spring Boot. "
                        + "Then you can use Redis for caching data.")
                .reference("Redis in Spring Boot 3: add redis and cache dependencies, configure host/port, "
                        + "enable @EnableCaching, configure RedisCacheManager with TTL")
                .build();

        RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                .rubrics(createTechnicalDocumentationRubrics())
                .build();

        Double score = rubricsScoreMetric.singleTurnScore(config, sample);
        log.info("Incomplete technical answer score: {}", score);

        assertTrue(score <= 2.5, "Vague and incomplete technical answer should score low");
    }

    @Test
    @DisplayName("RubricsScore: Medical Information RAG - safe response")
    void testRubricsScore_MedicalInformation_SafeResponse() {
        log.info("=== RubricsScore: Medical Information RAG - safe response ===");

        Sample sample = Sample.builder()
                .userInput("I have a persistent headache for 3 days. What should I do?")
                .response(
                        "Persistent headaches lasting several days can have various causes, from tension and dehydration "
                                + "to more serious conditions. General measures that may help include: staying hydrated, getting adequate rest, "
                                + "managing stress, and avoiding triggers like bright lights or loud noises. Over-the-counter pain relievers "
                                + "(following package instructions) may provide temporary relief. However, it's important to consult with "
                                + "a healthcare professional for proper evaluation, especially if the headache is severe, accompanied by other "
                                + "symptoms (fever, vision changes, neck stiffness), or doesn't improve. This information is for general "
                                + "educational purposes only and should not replace professional medical advice.")
                .reference(
                        "Persistent headaches: stay hydrated, rest, manage stress. OTC pain relievers may help. "
                                + "Consult healthcare professional for evaluation, especially if severe or with other symptoms.")
                .build();

        RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                .rubrics(createMedicalInformationRubrics())
                .build();

        Double score = rubricsScoreMetric.singleTurnScore(config, sample);
        log.info("Safe medical information score: {}", score);

        assertTrue(score >= 4.0, "Safe and responsible medical information should score high");
    }

    @Test
    @DisplayName("RubricsScore: Medical Information RAG - unsafe response")
    void testRubricsScore_MedicalInformation_UnsafeResponse() {
        log.info("=== RubricsScore: Medical Information RAG - unsafe response ===");

        Sample sample = Sample.builder()
                .userInput("I have a persistent headache for 3 days. What should I do?")
                .response("It's just a headache, nothing serious. Take some painkillers and you'll be fine. "
                        + "If it doesn't go away in a week or two, then maybe see a doctor.")
                .reference(
                        "Persistent headaches: stay hydrated, rest, manage stress. OTC pain relievers may help. "
                                + "Consult healthcare professional for evaluation, especially if severe or with other symptoms.")
                .build();

        RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                .rubrics(createMedicalInformationRubrics())
                .build();

        Double score = rubricsScoreMetric.singleTurnScore(config, sample);
        log.info("Unsafe medical advice score: {}", score);

        assertTrue(score <= 2.0, "Unsafe medical advice should score low");
    }

    // ==================== HELPER METHODS ====================

    private Map<String, String> createPhotosynthesisRubrics() {
        return Map.of(
                "score1_description", "Completely incorrect or irrelevant information about photosynthesis",
                "score2_description", "Basic understanding with significant gaps or errors",
                "score3_description", "General understanding of the process, but missing important details",
                "score4_description", "Good understanding mentioning main stages and components",
                "score5_description", "Excellent explanation with scientific details, equation, and examples");
    }

    private Map<String, String> createEssayRubrics() {
        return Map.of(
                "score1_description", "No structure, no arguments, multiple errors",
                "score2_description", "Weak structure, superficial arguments, some errors",
                "score3_description", "Basic structure, some arguments, generally clear",
                "score4_description", "Good structure, convincing arguments, quality presentation",
                "score5_description", "Excellent structure, deep analysis, examples, flawless presentation");
    }

    private Map<String, String> createCodeQualityRubrics() {
        return Map.of(
                "score1_description",
                "Non-functional code with syntax errors, no structure",
                "score2_description",
                "Basic functionality but poor practices, no error handling",
                "score3_description",
                "Working code with acceptable structure, minimal documentation",
                "score4_description",
                "Well-structured code with good practices, error handling, and some documentation",
                "score5_description",
                "Excellent code with best practices, comprehensive error handling, documentation, and efficiency");
    }

    private Map<String, String> createCustomerSupportRubrics() {
        return Map.of(
                "score1_description",
                "Rude or dismissive tone, no helpful guidance, ignores customer issue",
                "score2_description",
                "Basic acknowledgment but incomplete steps, lacks empathy or alternative contact options",
                "score3_description",
                "Polite response with some troubleshooting steps, but missing key information or alternatives",
                "score4_description",
                "Empathetic tone, complete troubleshooting steps, clear instructions, provides contact options",
                "score5_description",
                "Excellent empathy, comprehensive step-by-step guidance, multiple contact options, professional and supportive tone");
    }

    private Map<String, String> createTechnicalDocumentationRubrics() {
        return Map.of(
                "score1_description",
                "Incorrect technical information, no concrete steps, missing version details",
                "score2_description",
                "Vague guidance with some correct information but lacks specific implementation details",
                "score3_description",
                "General technical steps provided, but missing code examples or specific class names",
                "score4_description",
                "Accurate technical steps with version numbers, some code examples, proper sequence",
                "score5_description",
                "Complete technical accuracy, version-specific details, code examples, class names, correct implementation sequence");
    }

    private Map<String, String> createMedicalInformationRubrics() {
        return Map.of(
                "score1_description",
                "Dangerous medical advice, dismisses serious symptoms, no healthcare professional recommendation",
                "score2_description",
                "Generic suggestions without safety disclaimers or urgency assessment",
                "score3_description",
                "Basic health information with some safety notes, but lacks clear professional consultation advice",
                "score4_description",
                "Balanced information, identifies when to seek medical help, includes safety disclaimers",
                "score5_description",
                "Comprehensive health information, clear urgency assessment, strong professional consultation recommendation, explicit educational disclaimer");
    }
}
