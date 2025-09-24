package ai.qa.solutions.general;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import ai.qa.solutions.llm.LLMEvaluationService;
import ai.qa.solutions.metrics.general.RubricsScoreMetric;
import ai.qa.solutions.sample.SingleTurnSample;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RubricsScoreMetricTest {

    @Mock
    private LLMEvaluationService llmService;

    private RubricsScoreMetric rubricsScoreMetric;
    private SingleTurnSample testSample;
    private Map<String, String> testRubrics;

    @BeforeEach
    void setUp() {
        rubricsScoreMetric = new RubricsScoreMetric();
        rubricsScoreMetric.setLlmService(llmService);

        testRubrics = Map.of(
                "score1_description",
                        "The response is entirely incorrect and fails to address any aspect of the reference.",
                "score2_description",
                        "The response contains partial accuracy but includes major errors or significant omissions.",
                "score3_description",
                        "The response is mostly accurate but lacks clarity, thoroughness, or minor details.",
                "score4_description",
                        "The response is accurate and clear, with only minor omissions or slight inaccuracies.",
                "score5_description",
                        "The response is completely accurate, clear, and thoroughly addresses the reference without any errors.");

        rubricsScoreMetric.setRubrics(testRubrics);

        testSample = SingleTurnSample.builder()
                .userInput("Explain why the Earth is round")
                .response(
                        "The Earth is round due to gravity pulling matter toward the center, forming a sphere. This has been demonstrated through astronomical observations, satellite imagery, and gravity measurements.")
                .reference(
                        "The Earth is round because of gravitational forces and has been proven through various scientific methods.")
                .build();
    }

    @Test
    void testRubricsScoring_HighQuality() {
        when(llmService.evaluate(anyString()))
                .thenReturn(
                        "{\"score\": 5, \"rubric_level\": \"score5_description\", \"reasoning\": \"Excellent comprehensive response\"}");
        when(llmService.parseJsonScore(anyString())).thenReturn(5.0);

        Double score = rubricsScoreMetric.singleTurnScore(testSample);

        assertEquals(5.0, score);
        assertEquals("rubrics_score", rubricsScoreMetric.getName());
    }

    @Test
    void testRubricsScoring_MediumQuality() {
        when(llmService.evaluate(anyString()))
                .thenReturn(
                        "{\"score\": 3, \"rubric_level\": \"score3_description\", \"reasoning\": \"Good but lacks some detail\"}");
        when(llmService.parseJsonScore(anyString())).thenReturn(3.0);

        Double score = rubricsScoreMetric.singleTurnScore(testSample);

        assertEquals(3.0, score);
    }

    @Test
    void testEmptyRubricsValidation() {
        rubricsScoreMetric.setRubrics(Map.of());

        assertThrows(IllegalStateException.class, () -> rubricsScoreMetric.singleTurnScore(testSample));
    }

    @Test
    void testNullRubricsValidation() {
        rubricsScoreMetric.setRubrics(null);

        assertThrows(IllegalStateException.class, () -> rubricsScoreMetric.singleTurnScore(testSample));
    }
}
