package ai.qa.solutions.metric.explanation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Explanation Models")
class ExplanationModelsTest {

    @Nested
    @DisplayName("NoiseSensitivityExplanation")
    class NoiseSensitivityExplanationTest {

        @Test
        @DisplayName("should build with all fields")
        void shouldBuildWithAllFields() {
            final var matches = List.of(NoiseSensitivityExplanation.StatementMatch.builder()
                    .statement("Test statement")
                    .inReference(true)
                    .correct(true)
                    .contextSource("Context 1")
                    .analysis("Match found")
                    .build());

            final var explanation = NoiseSensitivityExplanation.builder()
                    .score(0.0)
                    .language("en")
                    .referenceStatements(List.of("Ref 1"))
                    .responseStatements(List.of("Resp 1"))
                    .matches(matches)
                    .mode("RELEVANT")
                    .build();

            assertThat(explanation.getMetricType()).isEqualTo("noise-sensitivity");
            assertThat(explanation.hasFixedInterpretation()).isTrue();
            assertThat(explanation.getErrorCount()).isZero();
        }

        @Test
        @DisplayName("should return correct simple description")
        void shouldReturnSimpleDescription() {
            final var explanation = NoiseSensitivityExplanation.builder()
                    .score(0.0)
                    .language("en")
                    .build();

            assertThat(explanation.getSimpleDescription()).contains("irrelevant");
            assertThat(explanation.getSimpleDescription()).containsIgnoringCase("lower");
        }

        @Test
        @DisplayName("should return Russian description")
        void shouldReturnRussianDescription() {
            final var explanation = NoiseSensitivityExplanation.builder()
                    .score(0.0)
                    .language("ru")
                    .build();

            assertThat(explanation.getSimpleDescription()).contains("нерелевантная");
        }

        @Test
        @DisplayName("should generate correct interpretation for low score")
        void shouldGenerateCorrectInterpretation() {
            final var explanation = NoiseSensitivityExplanation.builder()
                    .score(0.05)
                    .language("en")
                    .build();

            assertThat(explanation.getInterpretation().getLevel()).isEqualTo("Excellent");
            assertThat(explanation.getInterpretation().getIsGood()).isTrue();
        }

        @Test
        @DisplayName("should generate correct interpretation for high score (bad)")
        void shouldIndicatePoorForHighScore() {
            final var explanation = NoiseSensitivityExplanation.builder()
                    .score(0.7)
                    .language("en")
                    .build();

            assertThat(explanation.getInterpretation().getLevel()).isEqualTo("Poor");
            assertThat(explanation.getInterpretation().getIsGood()).isFalse();
        }

        @Test
        @DisplayName("should indicate good for score 0.1-0.3")
        void shouldIndicateGoodForLowScore() {
            final var explanation = NoiseSensitivityExplanation.builder()
                    .score(0.2)
                    .language("en")
                    .build();

            assertThat(explanation.getInterpretation().getLevel()).isEqualTo("Good");
            assertThat(explanation.getInterpretation().getIsGood()).isTrue();
        }

        @Test
        @DisplayName("should indicate moderate for score 0.3-0.5")
        void shouldIndicateModerateForMidScore() {
            final var explanation = NoiseSensitivityExplanation.builder()
                    .score(0.4)
                    .language("en")
                    .build();

            assertThat(explanation.getInterpretation().getLevel()).isEqualTo("Moderate");
            assertThat(explanation.getInterpretation().getIsGood()).isFalse();
        }

        @Test
        @DisplayName("should handle null score")
        void shouldHandleNullScore() {
            final var explanation = NoiseSensitivityExplanation.builder()
                    .score(null)
                    .language("en")
                    .build();

            assertThat(explanation.getInterpretation().getMeaning()).contains("not calculated");
        }

        @Test
        @DisplayName("should create Russian scale levels")
        void shouldCreateRussianScaleLevels() {
            final var explanation = NoiseSensitivityExplanation.builder()
                    .score(0.05)
                    .language("ru")
                    .build();

            assertThat(explanation.getInterpretation().getScaleLevels()).hasSize(4);
            assertThat(explanation.getInterpretation().getLevel()).contains("Отлично");
        }

        @Test
        @DisplayName("should generate Russian steps")
        void shouldGenerateRussianSteps() {
            final var explanation = NoiseSensitivityExplanation.builder()
                    .score(0.0)
                    .language("ru")
                    .build();

            assertThat(explanation.getSteps().get(0).getTitle()).contains("Разбиение");
        }

        @Test
        @DisplayName("should handle null statements")
        void shouldHandleNullStatements() {
            final var explanation = NoiseSensitivityExplanation.builder()
                    .score(0.0)
                    .language("en")
                    .referenceStatements(null)
                    .responseStatements(null)
                    .build();

            assertThat(explanation.getReferenceStatements()).isEmpty();
            assertThat(explanation.getResponseStatements()).isEmpty();
        }

        @Test
        @DisplayName("should count errors correctly")
        void shouldCountErrorsCorrectly() {
            final var matches = List.of(
                    NoiseSensitivityExplanation.StatementMatch.builder()
                            .statement("S1")
                            .correct(true)
                            .build(),
                    NoiseSensitivityExplanation.StatementMatch.builder()
                            .statement("S2")
                            .correct(false)
                            .build());

            final var explanation = NoiseSensitivityExplanation.builder()
                    .score(0.5)
                    .language("en")
                    .matches(matches)
                    .build();

            assertThat(explanation.getErrorCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("AspectCriticExplanation")
    class AspectCriticExplanationTest {

        @Test
        @DisplayName("should build with all fields")
        void shouldBuildWithAllFields() {
            final var explanation = AspectCriticExplanation.builder()
                    .score(1.0)
                    .language("en")
                    .aspectName("Conciseness")
                    .aspectDefinition("Response should be brief")
                    .passed(true)
                    .reasoning("Response is concise")
                    .build();

            assertThat(explanation.getMetricType()).isEqualTo("aspect-critic");
            assertThat(explanation.hasFixedInterpretation()).isTrue();
            assertThat(explanation.getAspectName()).isEqualTo("Conciseness");
            assertThat(explanation.isPassed()).isTrue();
        }

        @Test
        @DisplayName("should use default aspect name when null")
        void shouldUseDefaultAspectName() {
            final var explanation = AspectCriticExplanation.builder()
                    .score(1.0)
                    .language("en")
                    .aspectName(null)
                    .build();

            assertThat(explanation.getAspectName()).isEqualTo("Custom Aspect");
        }

        @Test
        @DisplayName("should generate PASS interpretation")
        void shouldGeneratePassInterpretation() {
            final var explanation = AspectCriticExplanation.builder()
                    .score(1.0)
                    .language("en")
                    .passed(true)
                    .aspectName("Test")
                    .build();

            assertThat(explanation.getInterpretation().getLevel()).isEqualTo("PASS");
            assertThat(explanation.getInterpretation().getIsGood()).isTrue();
        }

        @Test
        @DisplayName("should generate FAIL interpretation")
        void shouldGenerateFailInterpretation() {
            final var explanation = AspectCriticExplanation.builder()
                    .score(0.0)
                    .language("en")
                    .passed(false)
                    .aspectName("Test")
                    .build();

            assertThat(explanation.getInterpretation().getLevel()).isEqualTo("FAIL");
            assertThat(explanation.getInterpretation().getIsGood()).isFalse();
        }

        @Test
        @DisplayName("should return Russian description")
        void shouldReturnRussianDescription() {
            final var explanation = AspectCriticExplanation.builder()
                    .score(1.0)
                    .language("ru")
                    .passed(true)
                    .build();

            assertThat(explanation.getSimpleDescription()).contains("соответствует");
        }

        @Test
        @DisplayName("should generate Russian steps")
        void shouldGenerateRussianSteps() {
            final var explanation =
                    AspectCriticExplanation.builder().score(1.0).language("ru").build();

            assertThat(explanation.getSteps().get(0).getTitle()).contains("аспект");
        }

        @Test
        @DisplayName("should create Russian interpretation for pass")
        void shouldCreateRussianInterpretationForPass() {
            final var explanation = AspectCriticExplanation.builder()
                    .score(1.0)
                    .language("ru")
                    .passed(true)
                    .aspectName("Тест")
                    .build();

            assertThat(explanation.getInterpretation().getMeaning()).contains("ВЫПОЛНЕН");
        }

        @Test
        @DisplayName("should create Russian interpretation for fail")
        void shouldCreateRussianInterpretationForFail() {
            final var explanation = AspectCriticExplanation.builder()
                    .score(0.0)
                    .language("ru")
                    .passed(false)
                    .aspectName("Тест")
                    .build();

            assertThat(explanation.getInterpretation().getMeaning()).contains("НЕ ВЫПОЛНЕН");
        }
    }

    @Nested
    @DisplayName("ContextPrecisionExplanation")
    class ContextPrecisionExplanationTest {

        @Test
        @DisplayName("should build with contexts")
        void shouldBuildWithContexts() {
            final var contexts = List.of(
                    ContextPrecisionExplanation.ContextRelevance.builder()
                            .position(1)
                            .contextText("Context 1")
                            .relevant(true)
                            .reason("Relevant")
                            .build(),
                    ContextPrecisionExplanation.ContextRelevance.builder()
                            .position(2)
                            .contextText("Context 2")
                            .relevant(false)
                            .reason("Not relevant")
                            .build());

            final var explanation = ContextPrecisionExplanation.builder()
                    .score(0.75)
                    .language("en")
                    .contexts(contexts)
                    .precisionAtK(List.of(1.0, 0.5))
                    .build();

            assertThat(explanation.getMetricType()).isEqualTo("context-precision");
            assertThat(explanation.getContexts()).hasSize(2);
            assertThat(explanation.getRelevantCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should return Russian description")
        void shouldReturnRussianDescription() {
            final var explanation = ContextPrecisionExplanation.builder()
                    .score(0.5)
                    .language("ru")
                    .build();

            assertThat(explanation.getSimpleDescription()).contains("Релевантные");
        }

        @Test
        @DisplayName("should indicate excellent for high score")
        void shouldIndicateExcellentForHighScore() {
            final var explanation = ContextPrecisionExplanation.builder()
                    .score(0.95)
                    .language("en")
                    .build();

            assertThat(explanation.getInterpretation().getLevel()).isEqualTo("Excellent");
        }

        @Test
        @DisplayName("should indicate good for score 0.7-0.9")
        void shouldIndicateGoodForMediumScore() {
            final var explanation = ContextPrecisionExplanation.builder()
                    .score(0.8)
                    .language("en")
                    .build();

            assertThat(explanation.getInterpretation().getLevel()).isEqualTo("Good");
        }

        @Test
        @DisplayName("should indicate moderate for score 0.5-0.7")
        void shouldIndicateModerateForScore() {
            final var explanation = ContextPrecisionExplanation.builder()
                    .score(0.6)
                    .language("en")
                    .build();

            assertThat(explanation.getInterpretation().getLevel()).isEqualTo("Moderate");
        }

        @Test
        @DisplayName("should indicate poor for low score")
        void shouldIndicatePoorForLowScore() {
            final var explanation = ContextPrecisionExplanation.builder()
                    .score(0.3)
                    .language("en")
                    .build();

            assertThat(explanation.getInterpretation().getLevel()).isEqualTo("Poor");
        }

        @Test
        @DisplayName("should handle null score")
        void shouldHandleNullScore() {
            final var explanation = ContextPrecisionExplanation.builder()
                    .score(null)
                    .language("en")
                    .build();

            assertThat(explanation.getInterpretation().getMeaning()).contains("not calculated");
        }

        @Test
        @DisplayName("should create Russian scale levels")
        void shouldCreateRussianScaleLevels() {
            final var explanation = ContextPrecisionExplanation.builder()
                    .score(0.85)
                    .language("ru")
                    .build();

            assertThat(explanation.getInterpretation().getScaleLevels()).hasSize(4);
            assertThat(explanation.getInterpretation().getLevel()).contains("Хорошо");
        }

        @Test
        @DisplayName("should generate Russian steps")
        void shouldGenerateRussianSteps() {
            final var explanation = ContextPrecisionExplanation.builder()
                    .score(0.9)
                    .language("ru")
                    .build();

            assertThat(explanation.getSteps().get(0).getTitle()).contains("релевантности");
        }
    }

    @Nested
    @DisplayName("ContextRecallExplanation")
    class ContextRecallExplanationTest {

        @Test
        @DisplayName("should build with classifications")
        void shouldBuildWithClassifications() {
            final var classifications = List.of(
                    ContextRecallExplanation.ReferenceClassification.builder()
                            .statement("Statement 1")
                            .found(true)
                            .reason("Found in context")
                            .build(),
                    ContextRecallExplanation.ReferenceClassification.builder()
                            .statement("Statement 2")
                            .found(false)
                            .reason("Not found")
                            .build());

            final var explanation = ContextRecallExplanation.builder()
                    .score(0.5)
                    .language("en")
                    .classifications(classifications)
                    .build();

            assertThat(explanation.getMetricType()).isEqualTo("context-recall");
            assertThat(explanation.getFoundCount()).isEqualTo(1);
            assertThat(explanation.getTotalCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should return Russian description")
        void shouldReturnRussianDescription() {
            final var explanation =
                    ContextRecallExplanation.builder().score(0.5).language("ru").build();

            assertThat(explanation.getSimpleDescription()).contains("эталонного ответа");
        }

        @Test
        @DisplayName("should indicate excellent for high score")
        void shouldIndicateExcellentForHighScore() {
            final var explanation = ContextRecallExplanation.builder()
                    .score(0.95)
                    .language("en")
                    .build();

            assertThat(explanation.getInterpretation().getLevel()).isEqualTo("Excellent");
        }

        @Test
        @DisplayName("should indicate good for score 0.7-0.9")
        void shouldIndicateGoodForMediumScore() {
            final var explanation =
                    ContextRecallExplanation.builder().score(0.8).language("en").build();

            assertThat(explanation.getInterpretation().getLevel()).isEqualTo("Good");
        }

        @Test
        @DisplayName("should indicate moderate for score 0.5-0.7")
        void shouldIndicateModerateForScore() {
            final var explanation =
                    ContextRecallExplanation.builder().score(0.6).language("en").build();

            assertThat(explanation.getInterpretation().getLevel()).isEqualTo("Moderate");
        }

        @Test
        @DisplayName("should indicate poor for low score")
        void shouldIndicatePoorForLowScore() {
            final var explanation =
                    ContextRecallExplanation.builder().score(0.3).language("en").build();

            assertThat(explanation.getInterpretation().getLevel()).isEqualTo("Poor");
        }

        @Test
        @DisplayName("should handle null score")
        void shouldHandleNullScore() {
            final var explanation = ContextRecallExplanation.builder()
                    .score(null)
                    .language("en")
                    .build();

            assertThat(explanation.getInterpretation().getMeaning()).contains("not calculated");
        }

        @Test
        @DisplayName("should handle empty classifications")
        void shouldHandleEmptyClassifications() {
            final var explanation = ContextRecallExplanation.builder()
                    .score(0.0)
                    .language("en")
                    .classifications(List.of())
                    .build();

            assertThat(explanation.getTotalCount()).isZero();
            assertThat(explanation.getInterpretation().getCalculation()).isEqualTo("N/A");
        }

        @Test
        @DisplayName("should create Russian scale levels")
        void shouldCreateRussianScaleLevels() {
            final var explanation = ContextRecallExplanation.builder()
                    .score(0.85)
                    .language("ru")
                    .build();

            assertThat(explanation.getInterpretation().getScaleLevels()).hasSize(4);
            assertThat(explanation.getInterpretation().getLevel()).contains("Хорошо");
        }

        @Test
        @DisplayName("should return Russian meaning for excellent score")
        void shouldReturnRussianMeaningForExcellentScore() {
            final var explanation = ContextRecallExplanation.builder()
                    .score(0.95)
                    .language("ru")
                    .build();

            assertThat(explanation.getInterpretation().getMeaning()).contains("Отлично");
        }

        @Test
        @DisplayName("should return Russian meaning for poor score")
        void shouldReturnRussianMeaningForPoorScore() {
            final var explanation =
                    ContextRecallExplanation.builder().score(0.3).language("ru").build();

            assertThat(explanation.getInterpretation().getMeaning()).contains("Плохо");
        }
    }

    @Nested
    @DisplayName("ContextEntityRecallExplanation")
    class ContextEntityRecallExplanationTest {

        @Test
        @DisplayName("should build with entities")
        void shouldBuildWithEntities() {
            final var explanation = ContextEntityRecallExplanation.builder()
                    .score(0.67)
                    .language("en")
                    .referenceEntities(List.of("Entity1", "Entity2", "Entity3"))
                    .contextEntities(List.of("Entity1", "Entity2"))
                    .foundEntities(List.of("Entity1", "Entity2"))
                    .missingEntities(List.of("Entity3"))
                    .build();

            assertThat(explanation.getMetricType()).isEqualTo("context-entity-recall");
            assertThat(explanation.getFoundEntities()).hasSize(2);
            assertThat(explanation.getMissingEntities()).hasSize(1);
        }

        @Test
        @DisplayName("should return Russian description")
        void shouldReturnRussianDescription() {
            final var explanation = ContextEntityRecallExplanation.builder()
                    .score(0.5)
                    .language("ru")
                    .build();

            assertThat(explanation.getSimpleDescription()).contains("важные сущности");
        }

        @Test
        @DisplayName("should indicate excellent for high score")
        void shouldIndicateExcellentForHighScore() {
            final var explanation = ContextEntityRecallExplanation.builder()
                    .score(0.95)
                    .language("en")
                    .build();

            assertThat(explanation.getInterpretation().getLevel()).isEqualTo("Excellent");
        }

        @Test
        @DisplayName("should indicate good for score 0.7-0.9")
        void shouldIndicateGoodForMediumScore() {
            final var explanation = ContextEntityRecallExplanation.builder()
                    .score(0.8)
                    .language("en")
                    .build();

            assertThat(explanation.getInterpretation().getLevel()).isEqualTo("Good");
        }

        @Test
        @DisplayName("should indicate moderate for score 0.5-0.7")
        void shouldIndicateModerateForScore() {
            final var explanation = ContextEntityRecallExplanation.builder()
                    .score(0.6)
                    .language("en")
                    .build();

            assertThat(explanation.getInterpretation().getLevel()).isEqualTo("Moderate");
        }

        @Test
        @DisplayName("should indicate poor for low score")
        void shouldIndicatePoorForLowScore() {
            final var explanation = ContextEntityRecallExplanation.builder()
                    .score(0.3)
                    .language("en")
                    .build();

            assertThat(explanation.getInterpretation().getLevel()).isEqualTo("Poor");
        }

        @Test
        @DisplayName("should handle null score")
        void shouldHandleNullScore() {
            final var explanation = ContextEntityRecallExplanation.builder()
                    .score(null)
                    .language("en")
                    .build();

            assertThat(explanation.getInterpretation().getMeaning()).contains("not calculated");
        }

        @Test
        @DisplayName("should create Russian scale levels")
        void shouldCreateRussianScaleLevels() {
            final var explanation = ContextEntityRecallExplanation.builder()
                    .score(0.85)
                    .language("ru")
                    .build();

            assertThat(explanation.getInterpretation().getScaleLevels()).hasSize(4);
            assertThat(explanation.getInterpretation().getLevel()).contains("Хорошо");
        }

        @Test
        @DisplayName("should generate Russian steps")
        void shouldGenerateRussianSteps() {
            final var explanation = ContextEntityRecallExplanation.builder()
                    .score(0.9)
                    .language("ru")
                    .build();

            assertThat(explanation.getSteps().get(0).getTitle()).contains("сущностей");
        }

        @Test
        @DisplayName("should handle null reference entities")
        void shouldHandleNullReferenceEntities() {
            final var explanation = ContextEntityRecallExplanation.builder()
                    .score(0.5)
                    .language("en")
                    .referenceEntities(null)
                    .build();

            assertThat(explanation.getReferenceEntities()).isEmpty();
        }

        @Test
        @DisplayName("should handle null context entities")
        void shouldHandleNullContextEntities() {
            final var explanation = ContextEntityRecallExplanation.builder()
                    .score(0.5)
                    .language("en")
                    .contextEntities(null)
                    .build();

            assertThat(explanation.getContextEntities()).isEmpty();
        }
    }

    @Nested
    @DisplayName("ResponseRelevancyExplanation")
    class ResponseRelevancyExplanationTest {

        @Test
        @DisplayName("should build with questions")
        void shouldBuildWithQuestions() {
            final var questions = List.of(
                    ResponseRelevancyExplanation.GeneratedQuestion.builder()
                            .question("Q1")
                            .similarity(0.9)
                            .build(),
                    ResponseRelevancyExplanation.GeneratedQuestion.builder()
                            .question("Q2")
                            .similarity(0.8)
                            .build());

            final var explanation = ResponseRelevancyExplanation.builder()
                    .score(0.85)
                    .language("en")
                    .originalQuestion("Original Q")
                    .generatedQuestions(questions)
                    .build();

            assertThat(explanation.getMetricType()).isEqualTo("response-relevancy");
            assertThat(explanation.getGeneratedQuestions()).hasSize(2);
            assertThat(explanation.getAverageSimilarity()).isEqualTo(0.85);
        }

        @Test
        @DisplayName("should return Russian description")
        void shouldReturnRussianDescription() {
            final var explanation = ResponseRelevancyExplanation.builder()
                    .score(0.5)
                    .language("ru")
                    .build();

            assertThat(explanation.getSimpleDescription()).contains("отвечает на вопрос пользователя");
        }

        @Test
        @DisplayName("should handle null original question")
        void shouldHandleNullOriginalQuestion() {
            final var explanation = ResponseRelevancyExplanation.builder()
                    .score(0.5)
                    .language("en")
                    .originalQuestion(null)
                    .build();

            assertThat(explanation.getOriginalQuestion()).isEmpty();
        }

        @Test
        @DisplayName("should handle null generated questions")
        void shouldHandleNullGeneratedQuestions() {
            final var explanation = ResponseRelevancyExplanation.builder()
                    .score(0.5)
                    .language("en")
                    .generatedQuestions(null)
                    .build();

            assertThat(explanation.getGeneratedQuestions()).isEmpty();
        }

        @Test
        @DisplayName("should indicate excellent for high score")
        void shouldIndicateExcellentForHighScore() {
            final var explanation = ResponseRelevancyExplanation.builder()
                    .score(0.95)
                    .language("en")
                    .build();

            assertThat(explanation.getInterpretation().getLevel()).isEqualTo("Excellent");
            assertThat(explanation.getInterpretation().getMeaning()).contains("directly answers");
        }

        @Test
        @DisplayName("should indicate good for score 0.7-0.9")
        void shouldIndicateGoodForMediumScore() {
            final var explanation = ResponseRelevancyExplanation.builder()
                    .score(0.8)
                    .language("en")
                    .build();

            assertThat(explanation.getInterpretation().getLevel()).isEqualTo("Good");
            assertThat(explanation.getInterpretation().getMeaning()).contains("mostly relevant");
        }

        @Test
        @DisplayName("should indicate moderate for score 0.5-0.7")
        void shouldIndicateModerateForScore() {
            final var explanation = ResponseRelevancyExplanation.builder()
                    .score(0.6)
                    .language("en")
                    .build();

            assertThat(explanation.getInterpretation().getLevel()).isEqualTo("Moderate");
            assertThat(explanation.getInterpretation().getMeaning()).contains("partially");
        }

        @Test
        @DisplayName("should indicate poor for low score")
        void shouldIndicatePoorForLowScore() {
            final var explanation = ResponseRelevancyExplanation.builder()
                    .score(0.3)
                    .language("en")
                    .build();

            assertThat(explanation.getInterpretation().getLevel()).isEqualTo("Poor");
            assertThat(explanation.getInterpretation().getMeaning()).contains("off-topic");
        }

        @Test
        @DisplayName("should handle null score")
        void shouldHandleNullScore() {
            final var explanation = ResponseRelevancyExplanation.builder()
                    .score(null)
                    .language("en")
                    .build();

            assertThat(explanation.getInterpretation().getMeaning()).contains("not calculated");
        }

        @Test
        @DisplayName("should create Russian scale levels")
        void shouldCreateRussianScaleLevels() {
            final var explanation = ResponseRelevancyExplanation.builder()
                    .score(0.85)
                    .language("ru")
                    .build();

            assertThat(explanation.getInterpretation().getScaleLevels()).hasSize(4);
            assertThat(explanation.getInterpretation().getLevel()).contains("Хорошо");
        }

        @Test
        @DisplayName("should return Russian meaning for excellent score")
        void shouldReturnRussianMeaningForExcellentScore() {
            final var explanation = ResponseRelevancyExplanation.builder()
                    .score(0.95)
                    .language("ru")
                    .build();

            assertThat(explanation.getInterpretation().getMeaning()).contains("Отлично");
        }

        @Test
        @DisplayName("should return Russian meaning for poor score")
        void shouldReturnRussianMeaningForPoorScore() {
            final var explanation = ResponseRelevancyExplanation.builder()
                    .score(0.3)
                    .language("ru")
                    .build();

            assertThat(explanation.getInterpretation().getMeaning()).contains("Плохо");
        }

        @Test
        @DisplayName("should generate steps with items for questions")
        void shouldGenerateStepsWithItems() {
            final var questions = List.of(ResponseRelevancyExplanation.GeneratedQuestion.builder()
                    .question("Q1")
                    .similarity(0.9)
                    .build());

            final var explanation = ResponseRelevancyExplanation.builder()
                    .score(0.9)
                    .language("en")
                    .originalQuestion("Test?")
                    .generatedQuestions(questions)
                    .build();

            assertThat(explanation.getSteps()).hasSize(4);
            assertThat(explanation.getSteps().get(1).getItems()).hasSize(1);
        }

        @Test
        @DisplayName("should generate Russian steps")
        void shouldGenerateRussianSteps() {
            final var explanation = ResponseRelevancyExplanation.builder()
                    .score(0.9)
                    .language("ru")
                    .build();

            assertThat(explanation.getSteps().get(0).getTitle()).contains("Исходный");
        }
    }

    @Nested
    @DisplayName("SimpleCriteriaExplanation")
    class SimpleCriteriaExplanationTest {

        @Test
        @DisplayName("should build with score range")
        void shouldBuildWithScoreRange() {
            final var explanation = SimpleCriteriaExplanation.builder()
                    .score(0.75)
                    .language("en")
                    .criteriaName("Quality")
                    .criteriaDefinition("Evaluate quality")
                    .rawScore(4)
                    .minScore(1)
                    .maxScore(5)
                    .reasoning("Good quality")
                    .build();

            assertThat(explanation.getMetricType()).isEqualTo("simple-criteria");
            assertThat(explanation.getRawScore()).isEqualTo(4);
            assertThat(explanation.getInterpretation().getIsGood()).isNull();
        }

        @Test
        @DisplayName("should return Russian description")
        void shouldReturnRussianDescription() {
            final var explanation = SimpleCriteriaExplanation.builder()
                    .score(0.5)
                    .language("ru")
                    .build();

            assertThat(explanation.getSimpleDescription()).contains("пользовательскому критерию");
        }

        @Test
        @DisplayName("should use default criteria name when null")
        void shouldUseDefaultCriteriaName() {
            final var explanation = SimpleCriteriaExplanation.builder()
                    .score(0.5)
                    .language("en")
                    .criteriaName(null)
                    .build();

            assertThat(explanation.getCriteriaName()).isEqualTo("Custom Criteria");
        }

        @Test
        @DisplayName("should generate Russian steps")
        void shouldGenerateRussianSteps() {
            final var explanation = SimpleCriteriaExplanation.builder()
                    .score(0.9)
                    .language("ru")
                    .build();

            assertThat(explanation.getSteps().get(0).getTitle()).contains("критерий");
        }
    }

    @Nested
    @DisplayName("RubricsScoreExplanation")
    class RubricsScoreExplanationTest {

        @Test
        @DisplayName("should build with rubric levels")
        void shouldBuildWithRubricLevels() {
            final var levels = List.of(
                    RubricsScoreExplanation.RubricLevel.builder()
                            .level(5)
                            .description("Excellent")
                            .build(),
                    RubricsScoreExplanation.RubricLevel.builder()
                            .level(4)
                            .description("Good")
                            .build());

            final var explanation = RubricsScoreExplanation.builder()
                    .score(0.8)
                    .language("en")
                    .rubricLevels(levels)
                    .selectedLevel(4)
                    .reasoning("Good performance")
                    .build();

            assertThat(explanation.getMetricType()).isEqualTo("rubrics-score");
            assertThat(explanation.getSelectedLevel()).isEqualTo(4);
            assertThat(explanation.getRubricLevels()).hasSize(2);
        }

        @Test
        @DisplayName("should return Russian description")
        void shouldReturnRussianDescription() {
            final var explanation =
                    RubricsScoreExplanation.builder().score(0.5).language("ru").build();

            assertThat(explanation.getSimpleDescription()).contains("рубрик");
        }

        @Test
        @DisplayName("should generate Russian steps")
        void shouldGenerateRussianSteps() {
            final var explanation =
                    RubricsScoreExplanation.builder().score(0.9).language("ru").build();

            assertThat(explanation.getSteps().get(0).getTitle()).contains("рубрика");
        }

        @Test
        @DisplayName("should handle null rubric levels")
        void shouldHandleNullRubricLevels() {
            final var explanation = RubricsScoreExplanation.builder()
                    .score(0.5)
                    .language("en")
                    .rubricLevels(null)
                    .build();

            assertThat(explanation.getRubricLevels()).isEmpty();
        }
    }

    @Nested
    @DisplayName("AbstractScoreExplanation")
    class AbstractScoreExplanationTest {

        @Test
        @DisplayName("should format percent correctly")
        void shouldFormatPercent() {
            final var explanation = FaithfulnessExplanation.builder()
                    .score(0.6789)
                    .language("en")
                    .build();

            assertThat(explanation.getInterpretation().getScorePercent()).isEqualTo("67.89%");
        }

        @Test
        @DisplayName("should detect Russian language")
        void shouldDetectRussianLanguage() {
            final var explanation =
                    FaithfulnessExplanation.builder().score(0.5).language("ru").build();

            assertThat(explanation.getSimpleDescription()).contains("AI что-то от себя");
        }

        @Test
        @DisplayName("should use default language when null")
        void shouldUseDefaultLanguageWhenNull() {
            final var explanation =
                    FaithfulnessExplanation.builder().score(0.5).language(null).build();

            // Default is "en", so should return English
            assertThat(explanation.getSimpleDescription()).contains("make up anything");
        }

        @Test
        @DisplayName("should format percent as N/A when null")
        void shouldFormatPercentAsNaWhenNull() {
            final var explanation =
                    FaithfulnessExplanation.builder().score(null).language("en").build();

            assertThat(explanation.getInterpretation().getScorePercent()).isEqualTo("N/A");
        }

        @Test
        @DisplayName("should return unknown level for null score")
        void shouldReturnUnknownLevelForNullScore() {
            final var explanation =
                    FaithfulnessExplanation.builder().score(null).language("en").build();

            assertThat(explanation.getInterpretation().getLevel()).isEqualTo("Unknown");
        }

        @Test
        @DisplayName("should return Russian unknown level for null score")
        void shouldReturnRussianUnknownLevelForNullScore() {
            final var explanation =
                    FaithfulnessExplanation.builder().score(null).language("ru").build();

            assertThat(explanation.getInterpretation().getLevel()).isEqualTo("Неизвестно");
        }

        @Test
        @DisplayName("should return last level index for null score")
        void shouldReturnLastLevelIndexForNullScore() {
            final var explanation =
                    FaithfulnessExplanation.builder().score(null).language("en").build();

            assertThat(explanation.getInterpretation().getCurrentLevelIndex()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("ExplanationItem")
    class ExplanationItemTest {

        @Test
        @DisplayName("should build with all fields")
        void shouldBuildWithAllFields() {
            final var item = ExplanationItem.builder()
                    .content("Test content")
                    .passed(true)
                    .verdict("PASS")
                    .reason("Test reason")
                    .source("Context 1")
                    .index(1)
                    .numericValue(0.95)
                    .build();

            assertThat(item.getContent()).isEqualTo("Test content");
            assertThat(item.getPassed()).isTrue();
            assertThat(item.getVerdict()).isEqualTo("PASS");
            assertThat(item.getReason()).isEqualTo("Test reason");
            assertThat(item.getSource()).isEqualTo("Context 1");
            assertThat(item.getIndex()).isEqualTo(1);
            assertThat(item.getNumericValue()).isEqualTo(0.95);
        }

        @Test
        @DisplayName("should return passed status class")
        void shouldReturnPassedStatusClass() {
            final var item =
                    ExplanationItem.builder().content("Test").passed(true).build();

            assertThat(item.getStatusClass()).isEqualTo("passed");
            assertThat(item.getStatusIcon()).isEqualTo("\u2713");
        }

        @Test
        @DisplayName("should return failed status class")
        void shouldReturnFailedStatusClass() {
            final var item =
                    ExplanationItem.builder().content("Test").passed(false).build();

            assertThat(item.getStatusClass()).isEqualTo("failed");
            assertThat(item.getStatusIcon()).isEqualTo("\u2717");
        }

        @Test
        @DisplayName("should return neutral status class when passed is null")
        void shouldReturnNeutralStatusClass() {
            final var item =
                    ExplanationItem.builder().content("Test").passed(null).build();

            assertThat(item.getStatusClass()).isEqualTo("neutral");
            assertThat(item.getStatusIcon()).isEqualTo("-");
        }
    }

    @Nested
    @DisplayName("StepExplanation")
    class StepExplanationTest {

        @Test
        @DisplayName("should build with all fields")
        void shouldBuildWithAllFields() {
            final var items =
                    List.of(ExplanationItem.builder().content("Item 1").index(1).build());

            final var modelResults = List.of(ModelStepResult.builder()
                    .modelId("gpt-4")
                    .success(true)
                    .verdict(true)
                    .reasoning("result")
                    .build());

            final var step = StepExplanation.builder()
                    .stepNumber(1)
                    .stepName("TestStep")
                    .title("Test Step Title")
                    .description("Test description")
                    .outputSummary("Output summary")
                    .items(items)
                    .modelResults(modelResults)
                    .hasModelDisagreement(false)
                    .agreementPercent(100.0)
                    .build();

            assertThat(step.getStepNumber()).isEqualTo(1);
            assertThat(step.getStepName()).isEqualTo("TestStep");
            assertThat(step.getTitle()).isEqualTo("Test Step Title");
            assertThat(step.getItems()).hasSize(1);
            assertThat(step.getModelResults()).hasSize(1);
            assertThat(step.isHasModelDisagreement()).isFalse();
        }
    }

    @Nested
    @DisplayName("ModelStepResult")
    class ModelStepResultTest {

        @Test
        @DisplayName("should build with all fields")
        void shouldBuildWithAllFields() {
            final var result = ModelStepResult.builder()
                    .modelId("gpt-4")
                    .success(true)
                    .verdict(true)
                    .numericResult(0.95)
                    .reasoning("test reasoning")
                    .build();

            assertThat(result.getModelId()).isEqualTo("gpt-4");
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getVerdict()).isTrue();
            assertThat(result.getNumericResult()).isEqualTo(0.95);
            assertThat(result.getReasoning()).isEqualTo("test reasoning");
        }

        @Test
        @DisplayName("should return AGREE display status when verdict is true")
        void shouldReturnAgreeDisplayStatus() {
            final var result = ModelStepResult.builder()
                    .modelId("gpt-4")
                    .success(true)
                    .verdict(true)
                    .build();

            assertThat(result.getDisplayStatus()).isEqualTo("AGREE");
            assertThat(result.getIcon()).isEqualTo("\u2713");
        }

        @Test
        @DisplayName("should return DISAGREE display status when verdict is false")
        void shouldReturnDisagreeDisplayStatus() {
            final var result = ModelStepResult.builder()
                    .modelId("gpt-4")
                    .success(true)
                    .verdict(false)
                    .build();

            assertThat(result.getDisplayStatus()).isEqualTo("DISAGREE");
            assertThat(result.getIcon()).isEqualTo("\u2717");
        }

        @Test
        @DisplayName("should return ERROR display status when not successful")
        void shouldReturnErrorDisplayStatus() {
            final var result = ModelStepResult.builder()
                    .modelId("gpt-4")
                    .success(false)
                    .errorMessage("Failed")
                    .build();

            assertThat(result.getDisplayStatus()).isEqualTo("ERROR");
            assertThat(result.getIcon()).isEqualTo("\u26a0");
        }

        @Test
        @DisplayName("should return OK display status when success but no verdict")
        void shouldReturnOkDisplayStatus() {
            final var result = ModelStepResult.builder()
                    .modelId("gpt-4")
                    .success(true)
                    .verdict(null)
                    .build();

            assertThat(result.getDisplayStatus()).isEqualTo("OK");
            assertThat(result.getIcon()).isEqualTo("\u2022");
        }
    }

    @Nested
    @DisplayName("ScoreInterpretation")
    class ScoreInterpretationTest {

        @Test
        @DisplayName("should build with all fields")
        void shouldBuildWithAllFields() {
            final var scaleLevels = List.of(
                    ScoreInterpretation.ScaleLevel.builder()
                            .name("Excellent")
                            .range("90-100%")
                            .description("Very good")
                            .current(true)
                            .build(),
                    ScoreInterpretation.ScaleLevel.builder()
                            .name("Good")
                            .range("70-90%")
                            .description("Good")
                            .current(false)
                            .build());

            final var interpretation = ScoreInterpretation.builder()
                    .formula("a / b")
                    .calculation("3 / 4 = 0.75")
                    .numerator(3)
                    .denominator(4)
                    .score(0.75)
                    .scorePercent("75.00%")
                    .level("Good")
                    .isGood(true)
                    .meaning("Good performance")
                    .scaleLevels(scaleLevels)
                    .currentLevelIndex(1)
                    .build();

            assertThat(interpretation.getFormula()).isEqualTo("a / b");
            assertThat(interpretation.getNumerator()).isEqualTo(3);
            assertThat(interpretation.getDenominator()).isEqualTo(4);
            assertThat(interpretation.getScore()).isEqualTo(0.75);
            assertThat(interpretation.getIsGood()).isTrue();
            assertThat(interpretation.getScaleLevels()).hasSize(2);
        }
    }
}
