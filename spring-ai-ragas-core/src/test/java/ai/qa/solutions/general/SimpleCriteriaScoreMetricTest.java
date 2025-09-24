package ai.qa.solutions.general;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import ai.qa.solutions.llm.LLMEvaluationService;
import ai.qa.solutions.metrics.general.SimpleCriteriaScoreMetric;
import ai.qa.solutions.sample.SingleTurnSample;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SimpleCriteriaScoreMetricTest {

    @Mock
    private LLMEvaluationService llmService;

    private SimpleCriteriaScoreMetric simpleCriteriaScoreMetric;
    private SingleTurnSample testSample;

    @BeforeEach
    void setUp() {
        simpleCriteriaScoreMetric = new SimpleCriteriaScoreMetric();
        simpleCriteriaScoreMetric.setLlmService(llmService);
        simpleCriteriaScoreMetric.setDefinition("Score 0 to 5 by similarity");

        testSample = SingleTurnSample.builder()
                .userInput("Where is the Eiffel Tower located?")
                .response("The Eiffel Tower is located in Paris.")
                .reference("The Eiffel Tower is in Paris, France.")
                .build();
    }

    @Test
    void testSimpleCriteriaScoring_HighSimilarity() {
        when(llmService.evaluate(anyString()))
                .thenReturn("{\"score\": 4.5, \"reasoning\": \"Very similar responses\"}");
        when(llmService.parseJsonScore(anyString())).thenReturn(4.5);

        Double score = simpleCriteriaScoreMetric.singleTurnScore(testSample);

        assertEquals(4.5, score);
        assertEquals("simple_criteria_score", simpleCriteriaScoreMetric.getName());
    }

    @Test
    void testSimpleCriteriaScoring_LowSimilarity() {
        // Create dissimilar sample
        SingleTurnSample dissimilarSample = SingleTurnSample.builder()
                .userInput("Where is the Eiffel Tower located?")
                .response("The Eiffel Tower is located in Paris.")
                .reference("The Eiffel Tower is located in Egypt")
                .build();

        when(llmService.evaluate(anyString()))
                .thenReturn("{\"score\": 1.0, \"reasoning\": \"Responses are contradictory\"}");
        when(llmService.parseJsonScore(anyString())).thenReturn(1.0);

        Double score = simpleCriteriaScoreMetric.singleTurnScore(dissimilarSample);

        assertEquals(1.0, score);
    }

    @Test
    void testScoreRangeConfiguration() {
        simpleCriteriaScoreMetric.setScoreRange(0.0, 10.0);

        when(llmService.evaluate(anyString())).thenReturn("{\"score\": 8.5, \"reasoning\": \"High quality response\"}");
        when(llmService.parseJsonScore(anyString())).thenReturn(8.5);

        Double score = simpleCriteriaScoreMetric.singleTurnScore(testSample);

        assertEquals(8.5, score);
    }
}
