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

    /** Тип метрики, используется в сериализации и идентификации explanation. */
    private static final String METRIC_TYPE = "semantic-similarity";

    /** Текст ответа AI системы. */
    private final String response;

    /** Эталонный текст для сравнения. */
    private final String reference;

    /** Результаты от каждой embedding-модели. */
    private final List<ModelSimilarityResult> modelResults;

    /** Порог для бинарной классификации pass/fail (nullable). */
    private final Double threshold;

    /** Флаг, было ли применено чанкование. */
    private final boolean chunkingApplied;

    /** Количество чанков для response. */
    private final int responseChunkCount;

    /** Количество чанков для reference. */
    private final int referenceChunkCount;

    /** Название стратегии обработки длинных текстов (CHUNK / TRUNCATE / FAIL_FAST). */
    private final String longTextStrategy;

    /** Создаёт explanation для SemanticSimilarityMetric со всеми параметрами расчёта. */
    @Builder
    public SemanticSimilarityExplanation(
            final Double score,
            final String language,
            final String response,
            final String reference,
            final List<ModelSimilarityResult> modelResults,
            final Double threshold,
            final boolean chunkingApplied,
            final int responseChunkCount,
            final int referenceChunkCount,
            final String longTextStrategy) {
        super(score, language);
        this.response = response != null ? response : "";
        this.reference = reference != null ? reference : "";
        this.modelResults = modelResults != null ? modelResults : List.of();
        this.threshold = threshold;
        this.chunkingApplied = chunkingApplied;
        this.responseChunkCount = responseChunkCount;
        this.referenceChunkCount = referenceChunkCount;
        this.longTextStrategy = longTextStrategy != null ? longTextStrategy : "";
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
        int stepNumber = 1;

        // Step 1: Show response and reference
        steps.add(StepExplanation.builder()
                .stepName("InputTexts")
                .stepNumber(stepNumber++)
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

        // Optional chunking step: show chunking info when applied
        if (chunkingApplied) {
            final String chunkingDescription = messages.get("semanticSimilarity.chunking_applied");
            final String chunkingOutput = String.format(
                    "%s\n%s",
                    messages.get("semanticSimilarity.chunking_strategy", longTextStrategy),
                    messages.get("semanticSimilarity.chunk_count", responseChunkCount, referenceChunkCount));

            steps.add(StepExplanation.builder()
                    .stepName("TextChunking")
                    .stepNumber(stepNumber++)
                    .title(messages.get("semanticSimilarity.chunking_applied"))
                    .description(chunkingDescription)
                    .outputSummary(chunkingOutput)
                    .hasModelDisagreement(false)
                    .agreementPercent(100.0)
                    .build());
        }

        // Compute embeddings step
        steps.add(StepExplanation.builder()
                .stepName("ComputeEmbeddings")
                .stepNumber(stepNumber++)
                .title(messages.get("semanticSimilarity.step2.title"))
                .description(messages.get("semanticSimilarity.step2.desc"))
                .outputSummary(messages.get("semanticSimilarity.step2.output", modelResults.size()))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Calculate cosine similarity - show per-model results
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
                .stepNumber(stepNumber)
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
        /** Идентификатор embedding-модели. */
        private final String modelId;

        /** Значение косинусного сходства, полученное от модели. */
        private final double similarity;
    }
}
