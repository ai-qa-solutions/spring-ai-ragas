package ai.qa.solutions.metric.explanation;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Score explanation for BleuScoreMetric.
 * <p>
 * BLEU (Bilingual Evaluation Understudy) measures n-gram overlap between
 * the response and reference texts. This is a non-LLM metric.
 */
@Getter
public class BleuScoreExplanation extends AbstractScoreExplanation {

    private static final String METRIC_TYPE = "bleu-score";

    private final String response;
    private final String reference;
    private final int maxNgram;
    private final boolean smoothing;

    @Builder
    public BleuScoreExplanation(
            final Double score,
            final String language,
            final String response,
            final String reference,
            final int maxNgram,
            final boolean smoothing) {
        super(score, language);
        this.response = response != null ? response : "";
        this.reference = reference != null ? reference : "";
        this.maxNgram = maxNgram > 0 ? maxNgram : 4;
        this.smoothing = smoothing;
        buildSteps();
        buildInterpretation();
    }

    @Override
    public String getMetricType() {
        return METRIC_TYPE;
    }

    @Override
    public String getSimpleDescription() {
        return messages.get("bleuScore.description");
    }

    private void buildSteps() {
        // Step 1: Show response and reference
        steps.add(StepExplanation.builder()
                .stepName("InputTexts")
                .stepNumber(1)
                .title(messages.get("bleuScore.step1.title"))
                .description(messages.get("bleuScore.step1.desc"))
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
        steps.add(StepExplanation.builder()
                .stepName("Configuration")
                .stepNumber(2)
                .title(messages.get("bleuScore.step2.title"))
                .description(messages.get("bleuScore.step2.desc"))
                .outputSummary(messages.get("bleuScore.step2.output", maxNgram, smoothing))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 3: Compute n-gram precision
        steps.add(StepExplanation.builder()
                .stepName("ComputeNgramPrecision")
                .stepNumber(3)
                .title(messages.get("bleuScore.step3.title"))
                .description(messages.get("bleuScore.step3.desc", maxNgram))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 4: Compute BLEU score
        steps.add(StepExplanation.builder()
                .stepName("ComputeBleuScore")
                .stepNumber(4)
                .title(messages.get("bleuScore.step4.title"))
                .description(messages.get("bleuScore.step4.desc"))
                .outputSummary(formatPercent(score))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());
    }

    private void buildInterpretation() {
        final String formula = messages.get("bleuScore.formula");

        interpretation = ScoreInterpretation.builder()
                .formula(formula)
                .calculation(formatPercent(score))
                .score(score)
                .scorePercent(formatPercent(score))
                .level(getLevelName(score))
                .meaning(getMeaning())
                .scaleLevels(createBleuScale())
                .currentLevelIndex(getCurrentLevelIndex(score))
                .build();
    }

    private String getMeaning() {
        if (score == null) {
            return messages.get("common.scoreNotCalculated");
        }
        if (score >= 0.9) {
            return messages.get("bleuScore.meaning.excellent");
        }
        if (score >= 0.7) {
            return messages.get("bleuScore.meaning.good");
        }
        if (score >= 0.5) {
            return messages.get("bleuScore.meaning.moderate");
        }
        return messages.get("bleuScore.meaning.poor");
    }

    private List<ScoreInterpretation.ScaleLevel> createBleuScale() {
        return List.of(
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.excellent"))
                        .range("90-100%")
                        .description(messages.get("bleuScore.scale.excellent"))
                        .current(score != null && score >= 0.9)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.good"))
                        .range("70-90%")
                        .description(messages.get("bleuScore.scale.good"))
                        .current(score != null && score >= 0.7 && score < 0.9)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.moderate"))
                        .range("50-70%")
                        .description(messages.get("bleuScore.scale.moderate"))
                        .current(score != null && score >= 0.5 && score < 0.7)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.poor"))
                        .range("0-50%")
                        .description(messages.get("bleuScore.scale.poor"))
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
