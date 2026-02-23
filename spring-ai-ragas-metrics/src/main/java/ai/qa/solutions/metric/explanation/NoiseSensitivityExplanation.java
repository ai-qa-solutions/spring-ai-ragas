package ai.qa.solutions.metric.explanation;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Score explanation for NoiseSensitivityMetric.
 * <p>
 * NoiseSensitivity measures whether the AI response was "poisoned" by
 * irrelevant (noisy) contexts. A score of 0 is GOOD (no noise contamination),
 * while higher scores indicate the response used noisy information.
 * <p>
 * This is an inverted metric: 0 = good, 1 = bad.
 */
@Getter
public class NoiseSensitivityExplanation extends AbstractScoreExplanation {

    private static final String METRIC_TYPE = "noise-sensitivity";

    private final String reference;
    private final String aiResponse;
    private final List<String> referenceStatements;
    private final List<String> responseStatements;
    private final List<StatementMatch> matches;
    private final String mode;
    private final int errorCount;
    private final int totalChecks;

    @Builder
    public NoiseSensitivityExplanation(
            final Double score,
            final String language,
            final String reference,
            final String aiResponse,
            final List<String> referenceStatements,
            final List<String> responseStatements,
            final List<StatementMatch> matches,
            final String mode) {
        super(score, language);
        this.reference = reference != null ? reference : "";
        this.aiResponse = aiResponse != null ? aiResponse : "";
        this.referenceStatements = referenceStatements != null ? referenceStatements : List.of();
        this.responseStatements = responseStatements != null ? responseStatements : List.of();
        this.matches = matches != null ? matches : List.of();
        this.mode = mode != null ? mode : "RELEVANT";
        this.errorCount = (int) this.matches.stream().filter(m -> !m.correct).count();
        this.totalChecks = this.matches.size();
        buildSteps();
        buildInterpretation();
    }

    @Override
    public String getMetricType() {
        return METRIC_TYPE;
    }

    @Override
    public String getSimpleDescription() {
        return messages.get("noiseSensitivity.description");
    }

    @Override
    public boolean hasFixedInterpretation() {
        return true;
    }

    private void buildSteps() {
        // Step 1: Extract reference statements
        steps.add(StepExplanation.builder()
                .stepName("ExtractReferenceStatements")
                .stepNumber(1)
                .title(messages.get("noiseSensitivity.step1.title"))
                .description(messages.get("noiseSensitivity.step1.desc"))
                .inputData(reference)
                .outputSummary(messages.get("noiseSensitivity.step1.output"))
                .items(referenceStatements.stream()
                        .map((s) -> ExplanationItem.builder()
                                .content(s)
                                .index(referenceStatements.indexOf(s) + 1)
                                .build())
                        .toList())
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 2: Extract response statements
        steps.add(StepExplanation.builder()
                .stepName("ExtractResponseStatements")
                .stepNumber(2)
                .title(messages.get("noiseSensitivity.step2.title"))
                .description(messages.get("noiseSensitivity.step2.desc"))
                .inputData(aiResponse)
                .outputSummary(messages.get("noiseSensitivity.step2.output"))
                .items(responseStatements.stream()
                        .map((s) -> ExplanationItem.builder()
                                .content(s)
                                .index(responseStatements.indexOf(s) + 1)
                                .build())
                        .toList())
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 3: Match and analyze
        steps.add(StepExplanation.builder()
                .stepName("AnalyzeMatches")
                .stepNumber(3)
                .title(messages.get("noiseSensitivity.step3.title"))
                .description(messages.get("noiseSensitivity.step3.desc"))
                .outputSummary(messages.get("noiseSensitivity.step3.output", errorCount, totalChecks))
                .items(matches.stream()
                        .map((m) -> ExplanationItem.builder()
                                .content(m.statement)
                                .passed(m.correct)
                                .verdict(m.correct ? messages.get("verdict.ok") : messages.get("verdict.error"))
                                .reason(m.analysis)
                                .source(m.contextSource)
                                .index(matches.indexOf(m) + 1)
                                .build())
                        .toList())
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 4: Calculate noise sensitivity
        steps.add(StepExplanation.builder()
                .stepName("ComputeScore")
                .stepNumber(4)
                .title(messages.get("noiseSensitivity.step4.title"))
                .description(messages.get("noiseSensitivity.step4.desc"))
                .outputSummary(formatPercent(score))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());
    }

    private void buildInterpretation() {
        final String formula = messages.get("noiseSensitivity.formula");

        // Score is aggregated across models, so show simplified calculation
        final String calculation = formatPercent(score);

        // NoiseSensitivity: 0 = GOOD, 1 = BAD
        final boolean isGood = score != null && score <= 0.2;

        final String meaning = getMeaning();

        interpretation = ScoreInterpretation.builder()
                .formula(formula)
                .calculation(calculation)
                .numerator(errorCount)
                .denominator(totalChecks)
                .score(score)
                .scorePercent(formatPercent(score))
                .level(getInvertedLevelName(score))
                .isGood(isGood)
                .meaning(meaning)
                .scaleLevels(createNoiseSensitivityScale())
                .currentLevelIndex(getInvertedLevelIndex(score))
                .build();
    }

    private String getMeaning() {
        if (score == null) {
            return messages.get("common.scoreNotCalculated");
        }
        if (score <= 0.1) {
            return messages.get("noiseSensitivity.meaning.excellent");
        }
        if (score <= 0.3) {
            return messages.get("noiseSensitivity.meaning.good");
        }
        if (score <= 0.5) {
            return messages.get("noiseSensitivity.meaning.moderate");
        }
        return messages.get("noiseSensitivity.meaning.poor");
    }

    private String getInvertedLevelName(final Double score) {
        if (score == null) {
            return messages.get("common.unknown");
        }
        // Inverted: lower is better
        if (score <= 0.1) {
            return messages.get("scale.excellent");
        }
        if (score <= 0.3) {
            return messages.get("scale.good");
        }
        if (score <= 0.5) {
            return messages.get("scale.moderate");
        }
        return messages.get("scale.poor");
    }

    private int getInvertedLevelIndex(final Double score) {
        if (score == null) {
            return 3;
        }
        // Inverted: lower is better
        if (score <= 0.1) {
            return 0;
        }
        if (score <= 0.3) {
            return 1;
        }
        if (score <= 0.5) {
            return 2;
        }
        return 3;
    }

    private List<ScoreInterpretation.ScaleLevel> createNoiseSensitivityScale() {
        return List.of(
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.excellent"))
                        .range("0-10%")
                        .description(messages.get("noiseSensitivity.scale.excellent"))
                        .current(score != null && score <= 0.1)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.good"))
                        .range("10-30%")
                        .description(messages.get("noiseSensitivity.scale.good"))
                        .current(score != null && score > 0.1 && score <= 0.3)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.moderate"))
                        .range("30-50%")
                        .description(messages.get("noiseSensitivity.scale.moderate"))
                        .current(score != null && score > 0.3 && score <= 0.5)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.poor"))
                        .range("50-100%")
                        .description(messages.get("noiseSensitivity.scale.poor"))
                        .current(score != null && score > 0.5)
                        .build());
    }

    /**
     * Statement match analysis result.
     */
    @Builder
    @Getter
    public static class StatementMatch {
        private final String statement;
        private final boolean inReference;
        private final boolean correct;
        private final String contextSource;
        private final String analysis;
    }
}
