package ai.qa.solutions.metrics.retrieval.en;

import static org.junit.jupiter.api.Assertions.*;

import ai.qa.solutions.metrics.retrieval.ResponseRelevancyMetric;
import ai.qa.solutions.sample.Sample;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

@Slf4j
@EnableAutoConfiguration
@SuppressWarnings("LoggingSimilarMessage")
@Disabled("OpenRouter does not support embedder models")
@DisplayName("Response Relevancy Metric Integration Tests with English Examples")
@SpringBootTest(classes = EnResponseRelevancyIntegrationTest.ResponseRelevancyIntegrationTestConfiguration.class)
class EnResponseRelevancyIntegrationTest {

    @Configuration
    public static class ResponseRelevancyIntegrationTestConfiguration {}

    @Autowired
    private ResponseRelevancyMetric responseRelevancyMetric;

    // ==================== CORE FUNCTIONALITY TESTS ====================

    @Test
    @DisplayName("Response Relevancy: Complete answer receives higher score than incomplete")
    void testResponseRelevancy_CompleteVsIncomplete() {
        log.info("=== Comparing complete and incomplete answers ===");

        Sample incompleteSample = Sample.builder()
                .userInput("Where is France located and what is its capital?")
                .response("France is located in Western Europe.")
                .build();

        Sample completeSample = Sample.builder()
                .userInput("Where is France located and what is its capital?")
                .response("France is located in Western Europe, and its capital is Paris.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double incompleteScore = responseRelevancyMetric.singleTurnScore(config, incompleteSample);
        Double completeScore = responseRelevancyMetric.singleTurnScore(config, completeSample);

        log.info("Incomplete answer score: {}", incompleteScore);
        log.info("Complete answer score: {}", completeScore);

        assertNotNull(incompleteScore);
        assertNotNull(completeScore);
        assertTrue(incompleteScore >= 0.0 && incompleteScore <= 1.0);
        assertTrue(completeScore >= 0.0 && completeScore <= 1.0);

        assertTrue(
                completeScore > incompleteScore,
                "Complete answer should receive a higher score than incomplete. Complete: " + completeScore
                        + ", Incomplete: " + incompleteScore);
    }

    // ==================== NONCOMMITTAL ANSWERS TESTS ====================

    @Test
    @DisplayName("Noncommittal: 'I don't know' answer receives 0.0 score")
    void testResponseRelevancy_Noncommittal_IDontKnow() {
        log.info("=== Noncommittal Test - 'I don't know' ===");

        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("I don't know what the capital of France is.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Question: {}", sample.getUserInput());
        log.info("Answer (noncommittal): {}", sample.getResponse());
        log.info("Response Relevancy Score (noncommittal answer): {}", score);

        assertNotNull(score);
        assertEquals(
                0.0, score, 0.01, "Noncommittal answer 'I don't know' should receive 0.0 score, received: " + score);
    }

    @Test
    @DisplayName("Noncommittal: 'Not sure' answer receives 0.0 score")
    void testResponseRelevancy_Noncommittal_NotSure() {
        log.info("=== Noncommittal Test - 'Not sure' ===");

        Sample sample = Sample.builder()
                .userInput("When was the light bulb invented?")
                .response("I'm not sure when the light bulb was invented.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Question: {}", sample.getUserInput());
        log.info("Answer (noncommittal): {}", sample.getResponse());
        log.info("Response Relevancy Score (noncommittal answer): {}", score);

        assertNotNull(score);
        assertEquals(0.0, score, 0.01, "Noncommittal answer 'Not sure' should receive 0.0 score, received: " + score);
    }

    @Test
    @DisplayName("Noncommittal: 'I don't have information' answer receives 0.0 score")
    void testResponseRelevancy_Noncommittal_NoInformation() {
        log.info("=== Noncommittal Test - 'I don't have information' ===");

        Sample sample = Sample.builder()
                .userInput("What is the innovative feature of the smartphone invented in 2023?")
                .response(
                        "I don't know about the innovative feature of the smartphone invented in 2023, as I don't have information after 2022.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Question: {}", sample.getUserInput());
        log.info("Answer (noncommittal - no information): {}", sample.getResponse());
        log.info("Response Relevancy Score (noncommittal answer): {}", score);

        assertNotNull(score);
        assertEquals(
                0.0, score, 0.01, "Noncommittal answer 'no information' should receive 0.0 score, received: " + score);
    }

    @Test
    @DisplayName("Noncommittal vs Committal: Score comparison")
    void testResponseRelevancy_NoncommittalVsCommittal() {
        log.info("=== Noncommittal vs Committal Comparison ===");

        Sample noncommittalSample = Sample.builder()
                .userInput("Where was Albert Einstein born?")
                .response("I'm not sure where Albert Einstein was born.")
                .build();

        Sample committalSample = Sample.builder()
                .userInput("Where was Albert Einstein born?")
                .response("Albert Einstein was born in Germany.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double noncommittalScore = responseRelevancyMetric.singleTurnScore(config, noncommittalSample);
        Double committalScore = responseRelevancyMetric.singleTurnScore(config, committalSample);

        log.info("Noncommittal answer score: {}", noncommittalScore);
        log.info("Normal answer score: {}", committalScore);

        assertNotNull(noncommittalScore);
        assertNotNull(committalScore);

        assertEquals(0.0, noncommittalScore, 0.01, "Noncommittal answer should receive 0.0");
        assertTrue(committalScore >= 0.85, "Normal answer should receive a high score");
        assertTrue(
                committalScore > noncommittalScore,
                "Normal answer should receive significantly higher score than noncommittal");
    }

    // ==================== COMPLEX SCENARIOS ====================

    @Test
    @DisplayName("Complex Scenario: Scientific Explanation")
    void testResponseRelevancy_ScientificExplanation() {
        log.info("=== Complex Scenario Test - Scientific Explanation ===");

        Sample sample = Sample.builder()
                .userInput("What is photosynthesis?")
                .response("Photosynthesis is the process by which green plants and some other organisms use sunlight "
                        + "to synthesize foods with the help of chlorophyll. During this process, plants convert "
                        + "carbon dioxide and water into glucose and oxygen.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Question: {}", sample.getUserInput());
        log.info("Answer: {}", sample.getResponse());
        log.info("Response Relevancy Score: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.8, "Expected high score for relevant scientific answer, received: " + score);
    }

    @Test
    @DisplayName("Complex Scenario: Historical Event")
    void testResponseRelevancy_HistoricalEvent() {
        log.info("=== Complex Scenario Test - Historical Event ===");

        Sample sample = Sample.builder()
                .userInput("When did World War II end?")
                .response("World War II ended in 1945, with Germany surrendering in May and Japan in September.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Question: {}", sample.getUserInput());
        log.info("Answer: {}", sample.getResponse());
        log.info("Response Relevancy Score: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.8, "Expected high score for relevant historical answer, received: " + score);
    }

    @Test
    @DisplayName("Complex Scenario: Mathematical Explanation")
    void testResponseRelevancy_MathematicalExplanation() {
        log.info("=== Complex Scenario Test - Mathematical Explanation ===");

        Sample sample = Sample.builder()
                .userInput("What is the Pythagorean theorem?")
                .response(
                        "The Pythagorean theorem is a fundamental principle in geometry that states in a right-angled "
                                + "triangle, the square of the length of the hypotenuse (the side opposite the right angle) "
                                + "is equal to the sum of squares of the lengths of the other two sides. This is expressed "
                                + "as a² + b² = c².")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Question: {}", sample.getUserInput());
        log.info("Answer: {}", sample.getResponse());
        log.info("Response Relevancy Score: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.8, "Expected high score for relevant mathematical answer, received: " + score);
    }

    @Test
    @DisplayName("Complex Scenario: Technological Concept")
    void testResponseRelevancy_TechnologicalConcept() {
        log.info("=== Complex Scenario Test - Technological Concept ===");

        Sample sample = Sample.builder()
                .userInput("What is artificial intelligence?")
                .response("Artificial intelligence (AI) is a branch of computer science that deals with creating "
                        + "intelligent machines capable of performing tasks that typically require human intelligence. "
                        + "This includes learning, reasoning, problem-solving, perception, and language understanding.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Question: {}", sample.getUserInput());
        log.info("Answer: {}", sample.getResponse());
        log.info("Response Relevancy Score: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.8, "Expected high score for relevant technological answer, received: " + score);
    }

    // ==================== MULTILINGUAL CONTEXT TESTS ====================

    @Test
    @DisplayName("Multilingual Context: Question about a specific country")
    void testResponseRelevancy_MultilingualContext_Country() {
        log.info("=== Multilingual Context Test - Country Question ===");

        Sample sample = Sample.builder()
                .userInput("What is the capital of Japan?")
                .response("The capital of Japan is Tokyo, which is also the country's largest city.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Question: {}", sample.getUserInput());
        log.info("Answer: {}", sample.getResponse());
        log.info("Response Relevancy Score: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.8, "Expected high score for relevant answer, received: " + score);
    }

    @Test
    @DisplayName("Multilingual Context: Cultural question")
    void testResponseRelevancy_MultilingualContext_Culture() {
        log.info("=== Multilingual Context Test - Cultural Question ===");

        Sample sample = Sample.builder()
                .userInput("What is the traditional Japanese tea ceremony called?")
                .response(
                        "The traditional Japanese tea ceremony is called 'chanoyu' or 'sado'. It is a cultural activity "
                                + "involving the ceremonial preparation and presentation of matcha, powdered green tea.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Question: {}", sample.getUserInput());
        log.info("Answer: {}", sample.getResponse());
        log.info("Response Relevancy Score: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.8, "Expected high score for relevant cultural answer, received: " + score);
    }

    // ==================== EDGE CASES AND SPECIAL SCENARIOS ====================

    @Test
    @DisplayName("Edge Case: Very short question and answer")
    void testResponseRelevancy_EdgeCase_ShortQA() {
        log.info("=== Edge Case Test - Short Q&A ===");

        Sample sample = Sample.builder()
                .userInput("Capital of France?")
                .response("Paris.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Question: {}", sample.getUserInput());
        log.info("Answer: {}", sample.getResponse());
        log.info("Response Relevancy Score: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.7, "Expected high score for concise relevant answer, received: " + score);
    }

    @Test
    @DisplayName("Edge Case: Multi-part question with partial answer")
    void testResponseRelevancy_EdgeCase_MultiPartPartial() {
        log.info("=== Edge Case Test - Multi-part Question, Partial Answer ===");

        Sample sample = Sample.builder()
                .userInput("Who discovered penicillin and when?")
                .response("Alexander Fleming discovered penicillin.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Question: {}", sample.getUserInput());
        log.info("Answer (partial): {}", sample.getResponse());
        log.info("Response Relevancy Score: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(
                score >= 0.4 && score <= 0.8,
                "Expected moderate score for partially complete answer, received: " + score);
    }

    @Test
    @DisplayName("Edge Case: Question with negation")
    void testResponseRelevancy_EdgeCase_Negation() {
        log.info("=== Edge Case Test - Question with Negation ===");

        Sample sample = Sample.builder()
                .userInput("What is NOT the capital of France?")
                .response("London, Berlin, Madrid, and Rome are not the capital of France. The capital is Paris.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Question: {}", sample.getUserInput());
        log.info("Answer: {}", sample.getResponse());
        log.info("Response Relevancy Score: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(
                score >= 0.6, "Expected reasonable score for relevant answer to negation question, received: " + score);
    }

    @Test
    @DisplayName("Edge Case: Hypothetical question")
    void testResponseRelevancy_EdgeCase_Hypothetical() {
        log.info("=== Edge Case Test - Hypothetical Question ===");

        Sample sample = Sample.builder()
                .userInput("What would happen if the Earth stopped rotating?")
                .response(
                        "If Earth stopped rotating, one side would face the Sun continuously, experiencing extreme heat, "
                                + "while the other side would be in perpetual darkness and freezing cold. The atmosphere "
                                + "would continue moving at high speed, causing catastrophic winds.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Question: {}", sample.getUserInput());
        log.info("Answer: {}", sample.getResponse());
        log.info("Response Relevancy Score: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.7, "Expected high score for relevant hypothetical answer, received: " + score);
    }

    // ==================== IRRELEVANT AND MISLEADING ANSWERS ====================

    @Test
    @DisplayName("Irrelevant Answer: Completely off-topic response")
    void testResponseRelevancy_IrrelevantAnswer_OffTopic() {
        log.info("=== Irrelevant Answer Test - Completely Off-topic ===");

        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("The Great Wall of China was built over many centuries.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Question: {}", sample.getUserInput());
        log.info("Answer (off-topic): {}", sample.getResponse());
        log.info("Response Relevancy Score: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score <= 0.3, "Expected low score for completely off-topic answer, received: " + score);
    }

    @Test
    @DisplayName("Irrelevant Answer: Answer to different question")
    void testResponseRelevancy_IrrelevantAnswer_DifferentQuestion() {
        log.info("=== Irrelevant Answer Test - Answer to Different Question ===");

        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("The currency of France is the Euro.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Question: {}", sample.getUserInput());
        log.info("Answer (different question): {}", sample.getResponse());
        log.info("Response Relevancy Score: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score <= 0.5, "Expected low score for answer to different question, received: " + score);
    }

    @Test
    @DisplayName("Misleading Answer: Incorrect but on-topic information")
    void testResponseRelevancy_MisleadingAnswer_Incorrect() {
        log.info("=== Misleading Answer Test - Incorrect Information ===");

        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("The capital of France is Lyon.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Question: {}", sample.getUserInput());
        log.info("Answer (incorrect): {}", sample.getResponse());
        log.info("Response Relevancy Score: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        // Note: Response Relevancy measures relevance, not correctness
        // An incorrect but on-topic answer should still score reasonably high
        assertTrue(
                score >= 0.6, "Expected moderate-to-high score for on-topic but incorrect answer, received: " + score);
    }

    // ==================== AMBIGUITY AND CLARIFICATION ====================

    @Test
    @DisplayName("Ambiguity: Ambiguous question with reasonable answer")
    void testResponseRelevancy_Ambiguity_ReasonableAnswer() {
        log.info("=== Ambiguity Test - Reasonable Answer to Ambiguous Question ===");

        Sample sample = Sample.builder()
                .userInput("What is the bank?")
                .response(
                        "A bank is a financial institution that accepts deposits from the public and creates credit. "
                                + "Banks provide various financial services including loans, savings accounts, and payment services.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Question (ambiguous): {}", sample.getUserInput());
        log.info("Answer: {}", sample.getResponse());
        log.info("Response Relevancy Score: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.7, "Expected high score for reasonable interpretation, received: " + score);
    }

    @Test
    @DisplayName("Clarification: Answer requesting clarification")
    void testResponseRelevancy_Clarification_Request() {
        log.info("=== Clarification Test - Request for Clarification ===");

        Sample sample = Sample.builder()
                .userInput("What is it?")
                .response("I need more context to answer your question. What are you referring to?")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Question (vague): {}", sample.getUserInput());
        log.info("Answer (clarification request): {}", sample.getResponse());
        log.info("Response Relevancy Score: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        // A clarification request is a valid response to a vague question
        assertTrue(score >= 0.4, "Expected moderate score for clarification request, received: " + score);
    }

    // ==================== RELEVANCY SPECTRUM TESTS ====================

    @Test
    @DisplayName("Relevancy Spectrum: Perfect relevance")
    void testResponseRelevancy_RelevancySpectrum_Perfect() {
        log.info("=== Relevancy Spectrum Test - Perfect Relevance ===");

        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("The capital of France is Paris.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Question: {}", sample.getUserInput());
        log.info("Answer: {}", sample.getResponse());
        log.info("Response Relevancy Score: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.7, "Expected high score for perfectly relevant answer, received: " + score);
    }

    @Test
    @DisplayName("Response Relevancy: High relevance - answer on related topic")
    void testResponseRelevancy_HighRelevance_RelatedTopic() {
        log.info("=== Response Relevancy Test - Relevant Answer on Related Topic ===");

        // Answer about France's location is relevant to the question about capital,
        // as both topics are about French geography
        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("France is located in Western Europe, and its capital is Paris.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Question: {}", sample.getUserInput());
        log.info("Answer: {}", sample.getResponse());
        log.info("Response Relevancy Score: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.7, "Expected high score for relevant answer, received: " + score);
    }

    @Test
    @DisplayName("Response Relevancy: Zero relevance - noncommittal answer")
    void testResponseRelevancy_ZeroRelevance_NoncommittalAnswer() {
        log.info("=== Response Relevancy Test - Zero Relevance (Noncommittal Answer) ===");

        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("I don't know the answer to this question.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Question: {}", sample.getUserInput());
        log.info("Answer (noncommittal): {}", sample.getResponse());
        log.info("Response Relevancy Score: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertEquals(0.0, score, 0.01, "Expected zero score for noncommittal answer, received: " + score);
    }

    @Test
    @DisplayName("Response Relevancy: High relevance - detailed answer")
    void testResponseRelevancy_HighRelevance_DetailedAnswer() {
        log.info("=== Response Relevancy Test - Detailed Relevant Answer ===");

        Sample sample = Sample.builder()
                .userInput("Tell me about the capital of France")
                .response("Paris is the capital and largest city of France. "
                        + "It is located on the Seine River in the northern part of the country. "
                        + "Paris is known for its landmarks such as the Eiffel Tower, "
                        + "the Louvre, and Notre-Dame Cathedral.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Question: {}", sample.getUserInput());
        log.info("Answer: {}", sample.getResponse());
        log.info("Response Relevancy Score: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.8, "Expected very high score for detailed relevant answer, received: " + score);
    }

    @Test
    @DisplayName("Response Relevancy: Low relevance - redundant information")
    void testResponseRelevancy_LowRelevance_RedundantInformation() {
        log.info("=== Response Relevancy Test - Redundant Irrelevant Information ===");

        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("The capital of France is Paris. "
                        + "By the way, yesterday I went to the store and bought milk. "
                        + "The weather was great, the sun was shining. "
                        + "I also met an old friend.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Question: {}", sample.getUserInput());
        log.info("Answer: {}", sample.getResponse());
        log.info("Response Relevancy Score: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score < 0.8, "Expected reduced score due to redundant irrelevant information, received: " + score);
    }

    @Test
    @DisplayName("Response Relevancy: Check with empty input")
    void testResponseRelevancy_EmptyInput() {
        log.info("=== Response Relevancy Test - Empty Input ===");

        Sample sample = Sample.builder()
                .userInput("")
                .response("Paris is the capital of France.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Response Relevancy Score (empty input): {}", score);

        assertNotNull(score);
        assertEquals(0.0, score, "Expected 0.0 for empty input");
    }

    @Test
    @DisplayName("Response Relevancy: Check with empty response")
    void testResponseRelevancy_EmptyResponse() {
        log.info("=== Response Relevancy Test - Empty Response ===");

        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Response Relevancy Score (empty response): {}", score);

        assertNotNull(score);
        assertEquals(0.0, score, "Expected 0.0 for empty response");
    }

    @Test
    @DisplayName("Response Relevancy: Asynchronous call")
    void testResponseRelevancy_AsyncCall() throws Exception {
        log.info("=== Response Relevancy Test - Asynchronous Call ===");

        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("Paris is the capital of France.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score =
                responseRelevancyMetric.singleTurnScoreAsync(config, sample).get();

        log.info("Response Relevancy Score (async): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.7, "Expected high score for relevant answer");
    }
}
