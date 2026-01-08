package ai.qa.solutions.allure.explanation;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Score explanation for FaithfulnessMetric.
 * <p>
 * Faithfulness measures whether the AI response is factually consistent
 * with the provided context. It breaks down the response into statements
 * and verifies each against the context.
 */
@Getter
public class FaithfulnessExplanation extends AbstractScoreExplanation {

    private static final String METRIC_TYPE = "faithfulness";

    private final String aiResponse;
    private final List<String> statements;
    private final List<StatementVerdict> verdicts;
    private final int faithfulCount;
    private final int totalCount;

    @Builder
    public FaithfulnessExplanation(
            final Double score,
            final String language,
            final String aiResponse,
            final List<String> statements,
            final List<StatementVerdict> verdicts) {
        super(score, language);
        this.aiResponse = aiResponse != null ? aiResponse : "";
        this.statements = statements != null ? statements : List.of();
        this.verdicts = verdicts != null ? verdicts : List.of();
        this.faithfulCount = (int) this.verdicts.stream().filter(v -> v.passed).count();
        this.totalCount = this.verdicts.size();
        buildSteps();
        buildInterpretation();
    }

    @Override
    public String getMetricType() {
        return METRIC_TYPE;
    }

    @Override
    public String getSimpleDescription() {
        return messages.get("faithfulness.description");
    }

    private void buildSteps() {
        // Step 1: Extract statements
        steps.add(StepExplanation.builder()
                .stepName("ExtractStatements")
                .stepNumber(1)
                .title(messages.get("faithfulness.step1.title"))
                .description(messages.get("faithfulness.step1.desc"))
                .inputData(aiResponse)
                .outputSummary(messages.get("faithfulness.step1.output", statements.size()))
                .items(statements.stream()
                        .map((s) -> ExplanationItem.builder()
                                .content(s)
                                .index(statements.indexOf(s) + 1)
                                .build())
                        .toList())
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 2: Verify statements
        steps.add(StepExplanation.builder()
                .stepName("VerifyStatements")
                .stepNumber(2)
                .title(messages.get("faithfulness.step2.title"))
                .description(messages.get("faithfulness.step2.desc"))
                .outputSummary(messages.get("faithfulness.step2.output", faithfulCount, totalCount))
                .items(verdicts.stream()
                        .map((v) -> ExplanationItem.builder()
                                .content(v.statement)
                                .passed(v.passed)
                                .verdict(
                                        v.passed
                                                ? messages.get("verdict.faithful")
                                                : messages.get("verdict.unfaithful"))
                                .reason(v.reason)
                                .index(verdicts.indexOf(v) + 1)
                                .build())
                        .toList())
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 3: Calculate score
        steps.add(StepExplanation.builder()
                .stepName("ComputeScore")
                .stepNumber(3)
                .title(messages.get("faithfulness.step3.title"))
                .description(messages.get("faithfulness.step3.desc"))
                .outputSummary(formatPercent(score))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());
    }

    private void buildInterpretation() {
        final String formula = messages.get("faithfulness.formula");

        // Score is aggregated across models, so show simplified calculation
        final String calculation = formatPercent(score);

        final String meaning = getMeaning();

        interpretation = ScoreInterpretation.builder()
                .formula(formula)
                .calculation(calculation)
                .numerator(faithfulCount)
                .denominator(totalCount)
                .score(score)
                .scorePercent(formatPercent(score))
                .level(getLevelName(score))
                .meaning(meaning)
                .scaleLevels(createFaithfulnessScale())
                .currentLevelIndex(getCurrentLevelIndex(score))
                .build();
    }

    private String getMeaning() {
        if (score == null) {
            return messages.get("common.scoreNotCalculated");
        }
        if (score >= 0.9) {
            return messages.get("faithfulness.meaning.excellent");
        }
        if (score >= 0.7) {
            return messages.get("faithfulness.meaning.good");
        }
        if (score >= 0.5) {
            return messages.get("faithfulness.meaning.moderate");
        }
        return messages.get("faithfulness.meaning.poor");
    }

    private List<ScoreInterpretation.ScaleLevel> createFaithfulnessScale() {
        return List.of(
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.excellent"))
                        .range("90-100%")
                        .description(messages.get("faithfulness.scale.excellent"))
                        .current(score != null && score >= 0.9)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.good"))
                        .range("70-90%")
                        .description(messages.get("faithfulness.scale.good"))
                        .current(score != null && score >= 0.7 && score < 0.9)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.moderate"))
                        .range("50-70%")
                        .description(messages.get("faithfulness.scale.moderate"))
                        .current(score != null && score >= 0.5 && score < 0.7)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.poor"))
                        .range("0-50%")
                        .description(messages.get("faithfulness.scale.poor"))
                        .current(score != null && score < 0.5)
                        .build());
    }

    /**
     * Single statement verdict for faithfulness.
     */
    @Builder
    @Getter
    public static class StatementVerdict {
        private final String statement;
        private final boolean passed;
        private final String reason;
    }
}
