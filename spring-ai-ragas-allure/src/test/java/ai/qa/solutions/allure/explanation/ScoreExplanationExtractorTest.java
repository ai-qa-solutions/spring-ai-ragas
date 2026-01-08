package ai.qa.solutions.allure.explanation;

import static org.assertj.core.api.Assertions.assertThat;

import ai.qa.solutions.allure.model.ModelExecutionData;
import ai.qa.solutions.allure.model.StepExecutionData;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ScoreExplanationExtractor")
class ScoreExplanationExtractorTest {

    private final ScoreExplanationExtractor extractor = new ScoreExplanationExtractor();

    @Nested
    @DisplayName("extract")
    class Extract {

        @Test
        @DisplayName("should return empty for null metric name")
        void shouldReturnEmptyForNullMetricName() {
            final var result = extractor.extract(null, List.of(), 0.5, "en");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty for null steps")
        void shouldReturnEmptyForNullSteps() {
            final var result = extractor.extract("Faithfulness", null, 0.5, "en");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty for empty steps")
        void shouldReturnEmptyForEmptySteps() {
            final var result = extractor.extract("Faithfulness", List.of(), 0.5, "en");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty for unknown metric")
        void shouldReturnEmptyForUnknownMetric() {
            final var step = StepExecutionData.builder()
                    .stepName("Test")
                    .modelResults(List.of())
                    .build();

            final var result = extractor.extract("UnknownMetric", List.of(step), 0.5, "en");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("extractFaithfulness")
    class ExtractFaithfulness {

        @Test
        @DisplayName("should extract faithfulness explanation")
        void shouldExtractFaithfulness() {
            final var statementsJson =
                    "{\"statements\": [\"The tower was built in 1889\", \"The tower is 300m tall\"]}";
            final var verdictsJson =
                    "{\"verdicts\": [{\"statement\": \"The tower was built in 1889\", \"verdict\": 1, \"reason\": \"Found\"}, {\"statement\": \"The tower is 300m tall\", \"verdict\": 0, \"reason\": \"Not found\"}]}";

            final var statementsStep = StepExecutionData.builder()
                    .stepName("GenerateStatements")
                    .modelResults(List.of(ModelExecutionData.builder()
                            .modelId("gpt-4")
                            .success(true)
                            .resultJson(statementsJson)
                            .build()))
                    .build();

            final var verdictsStep = StepExecutionData.builder()
                    .stepName("EvaluateFaithfulness")
                    .modelResults(List.of(ModelExecutionData.builder()
                            .modelId("gpt-4")
                            .success(true)
                            .resultJson(verdictsJson)
                            .build()))
                    .build();

            final var result = extractor.extract("Faithfulness", List.of(statementsStep, verdictsStep), 0.5, "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(FaithfulnessExplanation.class);
            final var explanation = (FaithfulnessExplanation) result.get();
            assertThat(explanation.getStatements()).hasSize(2);
            assertThat(explanation.getVerdicts()).hasSize(2);
        }

        @Test
        @DisplayName("should return empty when no verdicts")
        void shouldReturnEmptyWhenNoVerdicts() {
            final var step = StepExecutionData.builder()
                    .stepName("GenerateStatements")
                    .modelResults(List.of())
                    .build();

            final var result = extractor.extract("Faithfulness", List.of(step), 0.5, "en");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("extractNoiseSensitivity")
    class ExtractNoiseSensitivity {

        @Test
        @DisplayName("should extract noise sensitivity explanation")
        void shouldExtractNoiseSensitivity() {
            final var refStep = StepExecutionData.builder()
                    .stepName("ExtractReferenceStatements")
                    .modelResults(List.of(ModelExecutionData.builder()
                            .modelId("gpt-4")
                            .success(true)
                            .resultJson("{\"statements\": [\"Statement 1\", \"Statement 2\"]}")
                            .build()))
                    .build();

            final var respStep = StepExecutionData.builder()
                    .stepName("ExtractResponseStatements")
                    .modelResults(List.of(ModelExecutionData.builder()
                            .modelId("gpt-4")
                            .success(true)
                            .resultJson("{\"statements\": [\"Statement 1\", \"Statement 2\"]}")
                            .build()))
                    .build();

            final var result = extractor.extract("NoiseSensitivityMetric", List.of(refStep, respStep), 0.0, "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(NoiseSensitivityExplanation.class);
        }
    }

    @Nested
    @DisplayName("extractAspectCritic")
    class ExtractAspectCritic {

        @Test
        @DisplayName("should extract aspect critic explanation")
        void shouldExtractAspectCritic() {
            final var step = StepExecutionData.builder()
                    .stepName("EvaluateAspect")
                    .modelResults(List.of(ModelExecutionData.builder()
                            .modelId("gpt-4")
                            .success(true)
                            .resultJson("{\"verdict\": true, \"reason\": \"Criteria met\"}")
                            .build()))
                    .build();

            final var result = extractor.extract("AspectCritic", List.of(step), 1.0, "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(AspectCriticExplanation.class);
            final var explanation = (AspectCriticExplanation) result.get();
            assertThat(explanation.isPassed()).isTrue();
            assertThat(explanation.getReasoning()).isEqualTo("Criteria met");
        }
    }

    @Nested
    @DisplayName("extractContextPrecision")
    class ExtractContextPrecision {

        @Test
        @DisplayName("should extract context precision explanation")
        void shouldExtractContextPrecision() {
            final var step = StepExecutionData.builder()
                    .stepName("EvaluateContext")
                    .modelResults(List.of(ModelExecutionData.builder()
                            .modelId("gpt-4")
                            .success(true)
                            .resultJson("{\"verdict\": true, \"reason\": \"Relevant\"}")
                            .build()))
                    .build();

            final var result = extractor.extract("ContextPrecision", List.of(step), 1.0, "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(ContextPrecisionExplanation.class);
        }
    }

    @Nested
    @DisplayName("extractContextRecall")
    class ExtractContextRecall {

        @Test
        @DisplayName("should extract context recall explanation")
        void shouldExtractContextRecall() {
            final var step = StepExecutionData.builder()
                    .stepName("ClassifyStatements")
                    .modelResults(List.of(ModelExecutionData.builder()
                            .modelId("gpt-4")
                            .success(true)
                            .resultJson(
                                    "{\"classifications\": [{\"statement\": \"S1\", \"attributed\": 1, \"reason\": \"Found\"}]}")
                            .build()))
                    .build();

            final var result = extractor.extract("ContextRecall", List.of(step), 1.0, "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(ContextRecallExplanation.class);
        }
    }

    @Nested
    @DisplayName("extractContextEntityRecall")
    class ExtractContextEntityRecall {

        @Test
        @DisplayName("should extract context entity recall explanation")
        void shouldExtractContextEntityRecall() {
            final var refStep = StepExecutionData.builder()
                    .stepName("ExtractReferenceEntities")
                    .modelResults(List.of(ModelExecutionData.builder()
                            .modelId("gpt-4")
                            .success(true)
                            .resultJson("{\"entities\": [\"Entity1\", \"Entity2\"]}")
                            .build()))
                    .build();

            final var ctxStep = StepExecutionData.builder()
                    .stepName("ExtractContextEntities")
                    .modelResults(List.of(ModelExecutionData.builder()
                            .modelId("gpt-4")
                            .success(true)
                            .resultJson("{\"entities\": [\"Entity1\"]}")
                            .build()))
                    .build();

            final var result = extractor.extract("ContextEntityRecall", List.of(refStep, ctxStep), 0.5, "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(ContextEntityRecallExplanation.class);
        }
    }

    @Nested
    @DisplayName("extractResponseRelevancy")
    class ExtractResponseRelevancy {

        @Test
        @DisplayName("should extract response relevancy explanation")
        void shouldExtractResponseRelevancy() {
            final var step = StepExecutionData.builder()
                    .stepName("GenerateQuestions")
                    .request("Question: What is the tower?")
                    .modelResults(List.of(ModelExecutionData.builder()
                            .modelId("gpt-4")
                            .success(true)
                            .resultJson("{\"questions\": [\"What is the tower?\", \"Where is the tower?\"]}")
                            .build()))
                    .build();

            final var result = extractor.extract("ResponseRelevancy", List.of(step), 0.9, "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(ResponseRelevancyExplanation.class);
        }
    }

    @Nested
    @DisplayName("extractSimpleCriteria")
    class ExtractSimpleCriteria {

        @Test
        @DisplayName("should extract simple criteria explanation")
        void shouldExtractSimpleCriteria() {
            final var step = StepExecutionData.builder()
                    .stepName("EvaluateCriteria")
                    .modelResults(List.of(ModelExecutionData.builder()
                            .modelId("gpt-4")
                            .success(true)
                            .resultJson("{\"score\": 4, \"reason\": \"Good quality\"}")
                            .build()))
                    .build();

            final var result = extractor.extract("SimpleCriteria", List.of(step), 0.75, "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(SimpleCriteriaExplanation.class);
        }
    }

    @Nested
    @DisplayName("extractRubricsScore")
    class ExtractRubricsScore {

        @Test
        @DisplayName("should extract rubrics score explanation")
        void shouldExtractRubricsScore() {
            final var step = StepExecutionData.builder()
                    .stepName("EvaluateRubric")
                    .modelResults(List.of(ModelExecutionData.builder()
                            .modelId("gpt-4")
                            .success(true)
                            .resultJson("{\"score\": 4, \"reason\": \"Good quality\"}")
                            .build()))
                    .build();

            final var result = extractor.extract("RubricsScore", List.of(step), 0.8, "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(RubricsScoreExplanation.class);
        }
    }

    @Nested
    @DisplayName("handleInvalidJson")
    class HandleInvalidJson {

        @Test
        @DisplayName("should handle invalid JSON gracefully")
        void shouldHandleInvalidJson() {
            final var step = StepExecutionData.builder()
                    .stepName("EvaluateFaithfulness")
                    .modelResults(List.of(ModelExecutionData.builder()
                            .modelId("gpt-4")
                            .success(true)
                            .resultJson("not valid json")
                            .build()))
                    .build();

            final var result = extractor.extract("Faithfulness", List.of(step), 0.5, "en");

            // Should not throw, should return empty or explanation without extracted data
            assertThat(result).isEmpty();
        }
    }
}
