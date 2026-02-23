package ai.qa.solutions.metric.explanation;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Score explanation for AnswerCorrectnessMetric.
 * <p>
 * Answer Correctness combines factual correctness (default 75%) and semantic similarity (25%)
 * to provide a comprehensive evaluation of response quality.
 * <p>
 * This composite metric captures both:
 * <ul>
 *   <li>Whether specific facts are correct (via NLI verification)</li>
 *   <li>Whether the overall meaning is preserved (via embedding similarity)</li>
 * </ul>
 */
@Getter
public class AnswerCorrectnessExplanation extends AbstractScoreExplanation {

    private static final String METRIC_TYPE = "answer-correctness";

    private final String response;
    private final String reference;
    private final double factualScore;
    private final double semanticScore;
    private final double factualWeight;
    private final double semanticWeight;

    @Builder
    public AnswerCorrectnessExplanation(
            final Double score,
            final String language,
            final String response,
            final String reference,
            final double factualScore,
            final double semanticScore,
            final double factualWeight,
            final double semanticWeight) {
        super(score, language);
        this.response = response != null ? response : "";
        this.reference = reference != null ? reference : "";
        this.factualScore = factualScore;
        this.semanticScore = semanticScore;
        this.factualWeight = factualWeight > 0 ? factualWeight : 0.75;
        this.semanticWeight = semanticWeight > 0 ? semanticWeight : 0.25;

        buildSteps();
        buildInterpretation();
    }

    @Override
    public String getMetricType() {
        return METRIC_TYPE;
    }

    @Override
    public String getSimpleDescription() {
        return messages.get("answerCorrectness.description");
    }

    private void buildSteps() {
        // Step 1: Input texts
        steps.add(StepExplanation.builder()
                .stepName("InputTexts")
                .stepNumber(1)
                .title(messages.get("answerCorrectness.step1.title"))
                .description(messages.get("answerCorrectness.step1.desc"))
                .inputData(String.format(
                        "%s: %s\n%s: %s",
                        messages.get("common.response"),
                        truncate(response, 200),
                        messages.get("common.reference"),
                        truncate(reference, 200)))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 2: Compute factual correctness
        steps.add(StepExplanation.builder()
                .stepName("ComputeFactualCorrectness")
                .stepNumber(2)
                .title(messages.get("answerCorrectness.step2.title"))
                .description(messages.get("answerCorrectness.step2.desc"))
                .outputSummary(String.format(
                        "%s: %s (weight: %.0f%%)",
                        messages.get("answerCorrectness.factualScore"),
                        formatPercent(factualScore),
                        factualWeight * 100))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 3: Compute semantic similarity
        steps.add(StepExplanation.builder()
                .stepName("ComputeSemanticSimilarity")
                .stepNumber(3)
                .title(messages.get("answerCorrectness.step3.title"))
                .description(messages.get("answerCorrectness.step3.desc"))
                .outputSummary(String.format(
                        "%s: %s (weight: %.0f%%)",
                        messages.get("answerCorrectness.semanticScore"),
                        formatPercent(semanticScore),
                        semanticWeight * 100))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 4: Combine scores
        steps.add(StepExplanation.builder()
                .stepName("CombineScores")
                .stepNumber(4)
                .title(messages.get("answerCorrectness.step4.title"))
                .description(messages.get("answerCorrectness.step4.desc"))
                .outputSummary(formatPercent(score))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());
    }

    private void buildInterpretation() {
        final String formula = String.format(
                "%s = %.2f × %s + %.2f × %s",
                messages.get("common.score"),
                factualWeight,
                messages.get("answerCorrectness.factual"),
                semanticWeight,
                messages.get("answerCorrectness.semantic"));

        interpretation = ScoreInterpretation.builder()
                .formula(formula)
                .calculation(String.format(
                        "%.2f × %.4f + %.2f × %.4f = %s",
                        factualWeight, factualScore, semanticWeight, semanticScore, formatPercent(score)))
                .score(score)
                .scorePercent(formatPercent(score))
                .level(getLevelName(score))
                .meaning(getMeaning())
                .scaleLevels(createAnswerCorrectnessScale())
                .currentLevelIndex(getCurrentLevelIndex(score))
                .build();
    }

    private String getMeaning() {
        if (score == null) {
            return messages.get("common.scoreNotCalculated");
        }
        if (score >= 0.9) {
            return messages.get("answerCorrectness.meaning.excellent");
        }
        if (score >= 0.7) {
            return messages.get("answerCorrectness.meaning.good");
        }
        if (score >= 0.5) {
            return messages.get("answerCorrectness.meaning.moderate");
        }
        return messages.get("answerCorrectness.meaning.poor");
    }

    private List<ScoreInterpretation.ScaleLevel> createAnswerCorrectnessScale() {
        return List.of(
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.excellent"))
                        .range("90-100%")
                        .description(messages.get("answerCorrectness.scale.excellent"))
                        .current(score != null && score >= 0.9)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.good"))
                        .range("70-90%")
                        .description(messages.get("answerCorrectness.scale.good"))
                        .current(score != null && score >= 0.7 && score < 0.9)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.moderate"))
                        .range("50-70%")
                        .description(messages.get("answerCorrectness.scale.moderate"))
                        .current(score != null && score >= 0.5 && score < 0.7)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.poor"))
                        .range("0-50%")
                        .description(messages.get("answerCorrectness.scale.poor"))
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
