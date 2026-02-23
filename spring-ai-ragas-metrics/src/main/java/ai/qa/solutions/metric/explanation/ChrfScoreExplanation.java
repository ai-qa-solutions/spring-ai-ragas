package ai.qa.solutions.metric.explanation;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Score explanation for ChrfScoreMetric.
 * <p>
 * chrF (Character n-gram F-score) measures character-level overlap between
 * response and reference. chrF++ also includes word n-grams.
 * This is a non-LLM metric.
 */
@Getter
public class ChrfScoreExplanation extends AbstractScoreExplanation {

    private static final String METRIC_TYPE = "chrf-score";

    private final String response;
    private final String reference;
    private final int charNgramOrder;
    private final int wordNgramOrder;
    private final double beta;

    @Builder
    public ChrfScoreExplanation(
            final Double score,
            final String language,
            final String response,
            final String reference,
            final int charNgramOrder,
            final int wordNgramOrder,
            final double beta) {
        super(score, language);
        this.response = response != null ? response : "";
        this.reference = reference != null ? reference : "";
        this.charNgramOrder = charNgramOrder > 0 ? charNgramOrder : 6;
        this.wordNgramOrder = wordNgramOrder;
        this.beta = beta > 0 ? beta : 2.0;
        buildSteps();
        buildInterpretation();
    }

    @Override
    public String getMetricType() {
        return METRIC_TYPE;
    }

    @Override
    public String getSimpleDescription() {
        return messages.get("chrfScore.description");
    }

    private void buildSteps() {
        // Step 1: Show response and reference
        steps.add(StepExplanation.builder()
                .stepName("InputTexts")
                .stepNumber(1)
                .title(messages.get("chrfScore.step1.title"))
                .description(messages.get("chrfScore.step1.desc"))
                .inputData(String.format(
                        "%s: %s\n%s: %s",
                        messages.get("common.response"),
                        truncate(response, 200),
                        messages.get("common.reference"),
                        truncate(reference, 200)))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 2: Configuration
        final String metricVariant = wordNgramOrder > 0 ? "chrF++" : "chrF";
        steps.add(StepExplanation.builder()
                .stepName("Configuration")
                .stepNumber(2)
                .title(messages.get("chrfScore.step2.title"))
                .description(messages.get("chrfScore.step2.desc"))
                .outputSummary(
                        messages.get("chrfScore.step2.output", metricVariant, charNgramOrder, wordNgramOrder, beta))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 3: Compute character n-gram overlap
        steps.add(StepExplanation.builder()
                .stepName("ComputeCharNgrams")
                .stepNumber(3)
                .title(messages.get("chrfScore.step3.title"))
                .description(messages.get("chrfScore.step3.desc", charNgramOrder))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 4: Compute word n-gram overlap (if chrF++)
        if (wordNgramOrder > 0) {
            steps.add(StepExplanation.builder()
                    .stepName("ComputeWordNgrams")
                    .stepNumber(4)
                    .title(messages.get("chrfScore.step4.title"))
                    .description(messages.get("chrfScore.step4.desc", wordNgramOrder))
                    .hasModelDisagreement(false)
                    .agreementPercent(100.0)
                    .build());
        }

        // Step 5: Compute final score
        final int finalStepNumber = wordNgramOrder > 0 ? 5 : 4;
        steps.add(StepExplanation.builder()
                .stepName("ComputeChrfScore")
                .stepNumber(finalStepNumber)
                .title(messages.get("chrfScore.step5.title"))
                .description(messages.get("chrfScore.step5.desc", beta))
                .outputSummary(formatPercent(score))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());
    }

    private void buildInterpretation() {
        final String formula = messages.get("chrfScore.formula", beta);

        interpretation = ScoreInterpretation.builder()
                .formula(formula)
                .calculation(formatPercent(score))
                .score(score)
                .scorePercent(formatPercent(score))
                .level(getLevelName(score))
                .meaning(getMeaning())
                .scaleLevels(createChrfScale())
                .currentLevelIndex(getCurrentLevelIndex(score))
                .build();
    }

    private String getMeaning() {
        if (score == null) {
            return messages.get("common.scoreNotCalculated");
        }
        if (score >= 0.9) {
            return messages.get("chrfScore.meaning.excellent");
        }
        if (score >= 0.7) {
            return messages.get("chrfScore.meaning.good");
        }
        if (score >= 0.5) {
            return messages.get("chrfScore.meaning.moderate");
        }
        return messages.get("chrfScore.meaning.poor");
    }

    private List<ScoreInterpretation.ScaleLevel> createChrfScale() {
        return List.of(
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.excellent"))
                        .range("90-100%")
                        .description(messages.get("chrfScore.scale.excellent"))
                        .current(score != null && score >= 0.9)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.good"))
                        .range("70-90%")
                        .description(messages.get("chrfScore.scale.good"))
                        .current(score != null && score >= 0.7 && score < 0.9)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.moderate"))
                        .range("50-70%")
                        .description(messages.get("chrfScore.scale.moderate"))
                        .current(score != null && score >= 0.5 && score < 0.7)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.poor"))
                        .range("0-50%")
                        .description(messages.get("chrfScore.scale.poor"))
                        .current(score != null && score < 0.5)
                        .build());
    }

    private String truncate(final String text, final int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
