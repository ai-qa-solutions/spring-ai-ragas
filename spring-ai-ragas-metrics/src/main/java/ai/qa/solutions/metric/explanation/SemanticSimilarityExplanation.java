package ai.qa.solutions.metric.explanation;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Score explanation for SemanticSimilarityMetric.
 * <p>
 * SemanticSimilarity measures how semantically similar the AI response is
 * to the reference answer using embedding-based cosine similarity.
 * This metric does NOT use LLM calls, only embedding models.
 */
@Getter
public class SemanticSimilarityExplanation extends AbstractScoreExplanation {

    private static final String METRIC_TYPE = "semantic-similarity";

    private final String response;
    private final String reference;
    private final List<ModelSimilarityResult> modelResults;
    private final Double threshold;

    @Builder
    public SemanticSimilarityExplanation(
            final Double score,
            final String language,
            final String response,
            final String reference,
            final List<ModelSimilarityResult> modelResults,
            final Double threshold) {
        super(score, language);
        this.response = response != null ? response : "";
        this.reference = reference != null ? reference : "";
        this.modelResults = modelResults != null ? modelResults : List.of();
        this.threshold = threshold;
        buildSteps();
        buildInterpretation();
    }

    @Override
    public String getMetricType() {
        return METRIC_TYPE;
    }

    @Override
    public String getSimpleDescription() {
        return messages.get("semanticSimilarity.description");
    }

    private void buildSteps() {
        // Step 1: Show response and reference
        steps.add(StepExplanation.builder()
                .stepName("InputTexts")
                .stepNumber(1)
                .title(messages.get("semanticSimilarity.step1.title"))
                .description(messages.get("semanticSimilarity.step1.desc"))
                .inputData(String.format(
                        "%s: %s\n%s: %s",
                        messages.get("semanticSimilarity.response"),
                        truncate(response, 200),
                        messages.get("semanticSimilarity.reference"),
                        truncate(reference, 200)))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 2: Compute embeddings
        steps.add(StepExplanation.builder()
                .stepName("ComputeEmbeddings")
                .stepNumber(2)
                .title(messages.get("semanticSimilarity.step2.title"))
                .description(messages.get("semanticSimilarity.step2.desc"))
                .outputSummary(messages.get("semanticSimilarity.step2.output", modelResults.size()))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 3: Calculate cosine similarity - show per-model results
        final List<ModelStepResult> stepModelResults = modelResults.stream()
                .map(m -> ModelStepResult.builder()
                        .modelId(m.modelId)
                        .success(true)
                        .numericResult(m.similarity)
                        .build())
                .toList();

        final String outputSummary;
        if (threshold != null && threshold > 0) {
            outputSummary = messages.get("semanticSimilarity.step3.outputThreshold", formatPercent(score), threshold);
        } else {
            outputSummary = formatPercent(score);
        }

        steps.add(StepExplanation.builder()
                .stepName("ComputeCosineSimilarity")
                .stepNumber(3)
                .title(messages.get("semanticSimilarity.step3.title"))
                .description(messages.get("semanticSimilarity.step3.desc"))
                .outputSummary(outputSummary)
                .modelResults(stepModelResults)
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());
    }

    private void buildInterpretation() {
        final String formula = messages.get("semanticSimilarity.formula");

        interpretation = ScoreInterpretation.builder()
                .formula(formula)
                .calculation(formatPercent(score))
                .score(score)
                .scorePercent(formatPercent(score))
                .level(getLevelName(score))
                .meaning(getMeaning())
                .scaleLevels(createSimilarityScale())
                .currentLevelIndex(getCurrentLevelIndex(score))
                .build();
    }

    private String getMeaning() {
        if (score == null) {
            return messages.get("common.scoreNotCalculated");
        }
        if (threshold != null && threshold > 0) {
            // Binary result with threshold
            if (score >= 1.0) {
                return messages.get("semanticSimilarity.meaning.passThreshold", threshold);
            }
            return messages.get("semanticSimilarity.meaning.failThreshold", threshold);
        }
        // Continuous score
        if (score >= 0.9) {
            return messages.get("semanticSimilarity.meaning.excellent");
        }
        if (score >= 0.7) {
            return messages.get("semanticSimilarity.meaning.good");
        }
        if (score >= 0.5) {
            return messages.get("semanticSimilarity.meaning.moderate");
        }
        return messages.get("semanticSimilarity.meaning.poor");
    }

    private List<ScoreInterpretation.ScaleLevel> createSimilarityScale() {
        return List.of(
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.excellent"))
                        .range("90-100%")
                        .description(messages.get("semanticSimilarity.scale.excellent"))
                        .current(score != null && score >= 0.9)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.good"))
                        .range("70-90%")
                        .description(messages.get("semanticSimilarity.scale.good"))
                        .current(score != null && score >= 0.7 && score < 0.9)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.moderate"))
                        .range("50-70%")
                        .description(messages.get("semanticSimilarity.scale.moderate"))
                        .current(score != null && score >= 0.5 && score < 0.7)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.poor"))
                        .range("0-50%")
                        .description(messages.get("semanticSimilarity.scale.poor"))
                        .current(score != null && score < 0.5)
                        .build());
    }

    private String truncate(final String text, final int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    /**
     * Similarity result from a single embedding model.
     */
    @Builder
    @Getter
    public static class ModelSimilarityResult {
        private final String modelId;
        private final double similarity;
    }
}
