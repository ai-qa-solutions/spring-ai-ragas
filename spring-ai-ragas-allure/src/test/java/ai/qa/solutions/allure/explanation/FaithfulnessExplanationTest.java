package ai.qa.solutions.allure.explanation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("FaithfulnessExplanation")
class FaithfulnessExplanationTest {

    @Nested
    @DisplayName("builder")
    class Builder {

        @Test
        @DisplayName("should build with all fields")
        void shouldBuildWithAllFields() {
            final var verdicts = List.of(
                    FaithfulnessExplanation.StatementVerdict.builder()
                            .statement("The tower was built in 1889")
                            .passed(true)
                            .reason("Found in context")
                            .build(),
                    FaithfulnessExplanation.StatementVerdict.builder()
                            .statement("The tower is 300m tall")
                            .passed(false)
                            .reason("Not found in context")
                            .build());

            final var explanation = FaithfulnessExplanation.builder()
                    .score(0.5)
                    .language("en")
                    .statements(List.of("The tower was built in 1889", "The tower is 300m tall"))
                    .verdicts(verdicts)
                    .build();

            assertThat(explanation.getScore()).isEqualTo(0.5);
            assertThat(explanation.getMetricType()).isEqualTo("faithfulness");
            assertThat(explanation.getStatements()).hasSize(2);
            assertThat(explanation.getVerdicts()).hasSize(2);
            assertThat(explanation.getFaithfulCount()).isEqualTo(1);
            assertThat(explanation.getTotalCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should handle null lists")
        void shouldHandleNullLists() {
            final var explanation = FaithfulnessExplanation.builder()
                    .score(0.0)
                    .language("en")
                    .statements(null)
                    .verdicts(null)
                    .build();

            assertThat(explanation.getStatements()).isEmpty();
            assertThat(explanation.getVerdicts()).isEmpty();
            assertThat(explanation.getFaithfulCount()).isZero();
            assertThat(explanation.getTotalCount()).isZero();
        }
    }

    @Nested
    @DisplayName("getSimpleDescription")
    class GetSimpleDescription {

        @Test
        @DisplayName("should return English description")
        void shouldReturnEnglishDescription() {
            final var explanation =
                    FaithfulnessExplanation.builder().score(0.5).language("en").build();

            assertThat(explanation.getSimpleDescription()).contains("AI make up anything");
        }

        @Test
        @DisplayName("should return Russian description")
        void shouldReturnRussianDescription() {
            final var explanation =
                    FaithfulnessExplanation.builder().score(0.5).language("ru").build();

            assertThat(explanation.getSimpleDescription()).contains("AI что-то от себя");
        }
    }

    @Nested
    @DisplayName("steps")
    class Steps {

        @Test
        @DisplayName("should generate steps")
        void shouldGenerateSteps() {
            final var verdicts = List.of(FaithfulnessExplanation.StatementVerdict.builder()
                    .statement("Statement 1")
                    .passed(true)
                    .reason("Reason 1")
                    .build());

            final var explanation = FaithfulnessExplanation.builder()
                    .score(1.0)
                    .language("en")
                    .statements(List.of("Statement 1"))
                    .verdicts(verdicts)
                    .build();

            assertThat(explanation.getSteps()).hasSize(3);
            assertThat(explanation.getSteps().get(0).getStepName()).isEqualTo("ExtractStatements");
            assertThat(explanation.getSteps().get(1).getStepName()).isEqualTo("VerifyStatements");
            assertThat(explanation.getSteps().get(2).getStepName()).isEqualTo("ComputeScore");
        }
    }

    @Nested
    @DisplayName("interpretation")
    class Interpretation {

        @Test
        @DisplayName("should build interpretation with formula")
        void shouldBuildInterpretation() {
            final var verdicts = List.of(
                    FaithfulnessExplanation.StatementVerdict.builder()
                            .statement("S1")
                            .passed(true)
                            .build(),
                    FaithfulnessExplanation.StatementVerdict.builder()
                            .statement("S2")
                            .passed(true)
                            .build());

            final var explanation = FaithfulnessExplanation.builder()
                    .score(1.0)
                    .language("en")
                    .verdicts(verdicts)
                    .build();

            assertThat(explanation.getInterpretation()).isNotNull();
            assertThat(explanation.getInterpretation().getFormula()).contains("verified statements");
            assertThat(explanation.getInterpretation().getScaleLevels()).hasSize(4);
        }

        @Test
        @DisplayName("should indicate excellent level for high score")
        void shouldIndicateExcellent() {
            final var verdicts = List.of(FaithfulnessExplanation.StatementVerdict.builder()
                    .statement("S1")
                    .passed(true)
                    .build());

            final var explanation = FaithfulnessExplanation.builder()
                    .score(0.95)
                    .language("en")
                    .verdicts(verdicts)
                    .build();

            assertThat(explanation.getInterpretation().getLevel()).isEqualTo("Excellent");
            assertThat(explanation.getInterpretation().getMeaning()).contains("Excellent");
        }

        @Test
        @DisplayName("should indicate poor level for low score")
        void shouldIndicatePoor() {
            final var explanation =
                    FaithfulnessExplanation.builder().score(0.3).language("en").build();

            assertThat(explanation.getInterpretation().getLevel()).isEqualTo("Poor");
            assertThat(explanation.getInterpretation().getMeaning()).contains("hallucinations");
        }
    }

    @Nested
    @DisplayName("StatementVerdict")
    class StatementVerdictTest {

        @Test
        @DisplayName("should build verdict with all fields")
        void shouldBuildVerdict() {
            final var verdict = FaithfulnessExplanation.StatementVerdict.builder()
                    .statement("Test statement")
                    .passed(true)
                    .reason("Found in context")
                    .build();

            assertThat(verdict.getStatement()).isEqualTo("Test statement");
            assertThat(verdict.isPassed()).isTrue();
            assertThat(verdict.getReason()).isEqualTo("Found in context");
        }
    }
}
