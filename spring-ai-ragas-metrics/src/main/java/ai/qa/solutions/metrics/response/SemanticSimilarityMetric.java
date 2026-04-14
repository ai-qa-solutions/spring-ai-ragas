package ai.qa.solutions.metrics.response;

import ai.qa.solutions.execution.ModelResult;
import ai.qa.solutions.execution.MultiModelExecutor;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationContext;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationResult;
import ai.qa.solutions.execution.listener.dto.ModelExclusionEvent;
import ai.qa.solutions.execution.listener.dto.StepResults;
import ai.qa.solutions.execution.listener.dto.StepType;
import ai.qa.solutions.metric.AbstractMultiModelMetric;
import ai.qa.solutions.metric.metadata.SemanticSimilarityMetadata;
import ai.qa.solutions.sample.Sample;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;

/**
 * Semantic Similarity Metric - measures the semantic similarity between response and reference.
 * <p>
 * This metric uses embedding models to compute vector representations of the response and reference
 * texts, then calculates the cosine similarity between them. It does not require any LLM calls,
 * making it fast and cost-effective for large-scale evaluations.
 * <p>
 * <strong>Algorithm:</strong>
 * <ol>
 *   <li>Compute embeddings for both response and reference texts</li>
 *   <li>Calculate cosine similarity between the embedding vectors</li>
 *   <li>Optionally apply threshold for binary pass/fail classification</li>
 * </ol>
 * <p>
 * <strong>Score interpretation:</strong>
 * <ul>
 *   <li>1.0 - Semantically identical (vectors point in same direction)</li>
 *   <li>0.8-1.0 - High similarity, very close in meaning</li>
 *   <li>0.5-0.8 - Moderate similarity, related but different phrasing</li>
 *   <li>0.0-0.5 - Low similarity, different meanings</li>
 *   <li>-1.0 to 0.0 - Negative similarity (rare in practice for text embeddings)</li>
 * </ul>
 * <p>
 * Based on the Sentence Transformers paper: <a href="https://arxiv.org/pdf/2108.06130.pdf">SAS</a>
 *
 * @author Artem Simeshin
 * @see Sample
 * @see SemanticSimilarityConfig
 * @since 1.0
 */
@Slf4j
public class SemanticSimilarityMetric
        extends AbstractMultiModelMetric<SemanticSimilarityMetric.SemanticSimilarityConfig> {

    /** Имя первого шага — вычисление эмбеддингов. */
    private static final String STEP_COMPUTE_EMBEDDINGS = "ComputeEmbeddings";

    /** Имя второго шага — вычисление косинусной близости. */
    private static final String STEP_COMPUTE_COSINE_SIMILARITY = "ComputeCosineSimilarity";

    /** Общее количество шагов evaluation pipeline. */
    private static final int TOTAL_STEPS = 2;

    /** Индекс шага ComputeEmbeddings. */
    private static final int STEP_INDEX_EMBEDDINGS = 0;

    /** Индекс шага ComputeCosineSimilarity. */
    private static final int STEP_INDEX_COSINE = 1;

    /** Метка запроса для шага эмбеддингов в аллюр-отчёте. */
    private static final String EMBEDDINGS_REQUEST_LABEL = "response + reference";

    /** Метка запроса для шага вычисления косинусной близости. */
    private static final String COMPUTE_REQUEST_LABEL = "compute";

    /**
     * Создаёт метрику с указанным multi-model исполнителем.
     *
     * @param executor исполнитель, предоставляющий доступ к embedding-моделям
     */
    @Builder(toBuilder = true)
    protected SemanticSimilarityMetric(final MultiModelExecutor executor) {
        super(executor);
    }

    /**
     * Convenience method for single-turn scoring with default configuration.
     *
     * @param sample the sample to evaluate
     * @return the semantic similarity score
     */
    public Double singleTurnScore(final Sample sample) {
        return singleTurnScore(SemanticSimilarityConfig.builder().build(), sample);
    }

    /**
     * Convenience method for async single-turn scoring with default configuration.
     *
     * @param sample the sample to evaluate
     * @return future with the semantic similarity score
     */
    public CompletableFuture<Double> singleTurnScoreAsync(final Sample sample) {
        return singleTurnScoreAsync(SemanticSimilarityConfig.builder().build(), sample);
    }

    @Override
    public Double singleTurnScore(final SemanticSimilarityConfig config, final Sample sample) {
        return singleTurnScoreAsync(config, sample).join();
    }

    @Override
    public CompletableFuture<Double> singleTurnScoreAsync(final SemanticSimilarityConfig config, final Sample sample) {

        // Validate required inputs
        final String response = sample.getResponse();
        if (response == null || response.isBlank()) {
            log.warn("No response provided for Semantic Similarity evaluation");
            return CompletableFuture.completedFuture(0.0);
        }

        final String reference = sample.getReference();
        if (reference == null || reference.isBlank()) {
            log.warn("No reference provided for Semantic Similarity evaluation");
            return CompletableFuture.completedFuture(0.0);
        }

        final Instant startTime = Instant.now();
        final List<String> embeddingModelIds = executor.getEmbeddingModelIds();

        if (embeddingModelIds.isEmpty()) {
            log.error("No embedding models available for Semantic Similarity evaluation");
            return CompletableFuture.completedFuture(0.0);
        }

        // Create evaluation-specific notifier for thread-safe parallel execution
        final EvaluationNotifier notifier = createEvaluationNotifier();

        // Notify listeners before evaluation
        notifier.beforeMetricEvaluation(MetricEvaluationContext.builder()
                .metricName(getName())
                .sample(sample)
                .config(config)
                .modelIds(List.of())
                .embeddingModelIds(embeddingModelIds)
                .totalSteps(TOTAL_STEPS)
                .build());

        return executor.runAsync(() -> {
            log.debug("Computing semantic similarity evaluation with explicit flow");

            final List<StepResults> accumulatedSteps = new ArrayList<>();
            final List<ModelExclusionEvent> accumulatedExclusions = new ArrayList<>();
            final List<String> excludedModels = new ArrayList<>();

            // ========== Step 1: Compute embeddings ==========
            final EmbeddingTexts texts = prepareTextsForStrategy(config, response, reference);

            final List<ModelResult<List<float[]>>> embeddingResults =
                    executor.executeEmbeddingsAsync(texts.textsToEmbed()).join();

            final List<ModelResult<?>> step1LlmResults = new ArrayList<>();
            final Map<String, EmbeddingsResult> step1Successful = processEmbeddingResults(
                    embeddingResults, texts, step1LlmResults, accumulatedExclusions, excludedModels);

            final List<ModelResult<?>> embeddingModelResultsList = new ArrayList<>(embeddingResults);
            accumulatedSteps.add(StepResults.builder()
                    .stepName(STEP_COMPUTE_EMBEDDINGS)
                    .stepIndex(STEP_INDEX_EMBEDDINGS)
                    .totalSteps(TOTAL_STEPS)
                    .stepType(StepType.EMBEDDING)
                    .request(EMBEDDINGS_REQUEST_LABEL)
                    .results(step1LlmResults)
                    .embeddingModelResults(embeddingModelResultsList)
                    .build());

            if (step1Successful.isEmpty()) {
                throw new IllegalStateException(
                        "All embedding models failed at step ComputeEmbeddings for metric: " + getName());
            }

            // ========== Step 2: Compute cosine similarity ==========
            final Map<String, Double> modelScores =
                    computeAllSimilarities(step1Successful, config, accumulatedExclusions, excludedModels);

            final List<ModelResult<?>> step2Results = new ArrayList<>();
            for (final Map.Entry<String, Double> e : modelScores.entrySet()) {
                step2Results.add(ModelResult.success(e.getKey(), e.getValue(), Duration.ZERO, COMPUTE_REQUEST_LABEL));
            }
            accumulatedSteps.add(StepResults.builder()
                    .stepName(STEP_COMPUTE_COSINE_SIMILARITY)
                    .stepIndex(STEP_INDEX_COSINE)
                    .totalSteps(TOTAL_STEPS)
                    .stepType(StepType.COMPUTE)
                    .results(step2Results)
                    .build());

            if (modelScores.isEmpty()) {
                throw new IllegalStateException("All models failed to compute similarity for metric: " + getName());
            }

            final double aggregatedScore = aggregate(modelScores);
            final Duration duration = Duration.between(startTime, Instant.now());

            notifier.afterMetricEvaluation(MetricEvaluationResult.builder()
                    .metricName(getName())
                    .sample(sample)
                    .config(config)
                    .embeddingModelIds(embeddingModelIds)
                    .aggregatedScore(aggregatedScore)
                    .modelScores(modelScores)
                    .excludedModels(excludedModels)
                    .totalDuration(duration)
                    .steps(accumulatedSteps)
                    .exclusions(accumulatedExclusions)
                    .metadata(new SemanticSimilarityMetadata(
                            new HashMap<>(modelScores),
                            config.getThreshold(),
                            texts.chunkingApplied(),
                            texts.responseChunkCount(),
                            texts.referenceChunkCount(),
                            config.getLongTextStrategy().name()))
                    .build());

            return aggregatedScore;
        });
    }

    /**
     * Готовит список текстов для embedding-вызова согласно выбранной стратегии
     * обработки длинных текстов.
     */
    private EmbeddingTexts prepareTextsForStrategy(
            final SemanticSimilarityConfig config, final String response, final String reference) {
        final SemanticSimilarityConfig.LongTextStrategy strategy = config.getLongTextStrategy();
        final int maxTokens = config.getMaxTokensPerChunk();
        final double charsPerToken = config.getCharsPerToken();
        return switch (strategy) {
            case CHUNK -> prepareChunked(response, reference, maxTokens, charsPerToken);
            case TRUNCATE -> prepareTruncated(response, reference, maxTokens, charsPerToken);
            case FAIL_FAST -> prepareAsIs(response, reference);
        };
    }

    /**
     * Разбивает оба текста на чанки через {@link TextChunker}, объединяет их в один список
     * и фиксирует метаданные чанкинга.
     */
    private EmbeddingTexts prepareChunked(
            final String response, final String reference, final int maxTokens, final double charsPerToken) {
        final List<String> responseChunks = TextChunker.splitIntoChunks(response, maxTokens, charsPerToken);
        final List<String> referenceChunks = TextChunker.splitIntoChunks(reference, maxTokens, charsPerToken);

        final int responseChunkCount = responseChunks.size();
        final int referenceChunkCount = referenceChunks.size();
        final boolean chunkingApplied = responseChunkCount > 1 || referenceChunkCount > 1;

        final List<String> allChunks = new ArrayList<>(responseChunkCount + referenceChunkCount);
        allChunks.addAll(responseChunks);
        allChunks.addAll(referenceChunks);

        return new EmbeddingTexts(allChunks, responseChunkCount, referenceChunkCount, chunkingApplied);
    }

    /**
     * Усекает оба текста до лимита токенов через {@link TextChunker#truncateToTokenLimit}.
     */
    private EmbeddingTexts prepareTruncated(
            final String response, final String reference, final int maxTokens, final double charsPerToken) {
        final String processedResponse = TextChunker.truncateToTokenLimit(response, maxTokens, charsPerToken);
        final String processedReference = TextChunker.truncateToTokenLimit(reference, maxTokens, charsPerToken);
        return new EmbeddingTexts(List.of(processedResponse, processedReference), 1, 1, false);
    }

    /**
     * Передаёт тексты в embedding-модель как есть — стратегия {@code FAIL_FAST}.
     */
    private EmbeddingTexts prepareAsIs(final String response, final String reference) {
        return new EmbeddingTexts(List.of(response, reference), 1, 1, false);
    }

    /**
     * Обрабатывает результаты вызова embedding-моделей: для каждой модели либо собирает
     * успешный {@link EmbeddingsResult}, либо добавляет запись об исключении модели.
     */
    private Map<String, EmbeddingsResult> processEmbeddingResults(
            final List<ModelResult<List<float[]>>> embeddingResults,
            final EmbeddingTexts texts,
            final List<ModelResult<?>> step1LlmResults,
            final List<ModelExclusionEvent> accumulatedExclusions,
            final List<String> excludedModels) {
        final Map<String, EmbeddingsResult> step1Successful = new HashMap<>();
        for (final ModelResult<List<float[]>> result : embeddingResults) {
            final Optional<EmbeddingsResult> maybeEmbResult = tryBuildEmbeddingsResult(result, texts);
            if (maybeEmbResult.isPresent()) {
                final EmbeddingsResult embResult = maybeEmbResult.get();
                step1Successful.put(result.modelId(), embResult);
                step1LlmResults.add(
                        ModelResult.success(result.modelId(), embResult, result.duration(), result.request()));
            } else {
                excludedModels.add(result.modelId());
                accumulatedExclusions.add(buildEmbeddingExclusion(result));
            }
        }
        return step1Successful;
    }

    /**
     * Пытается собрать {@link EmbeddingsResult} из ответа одной embedding-модели.
     * Возвращает пустой {@link Optional}, если модель упала или вернула недостаточно векторов.
     */
    private Optional<EmbeddingsResult> tryBuildEmbeddingsResult(
            final ModelResult<List<float[]>> result, final EmbeddingTexts texts) {
        if (!result.isSuccess()) {
            log.warn(
                    "Embedding model {} failed: {}",
                    result.modelId(),
                    result.error() != null ? result.error().getMessage() : "unknown error");
            return Optional.empty();
        }

        final List<float[]> embeddings = result.result();
        final int expectedMinEmbeddings = texts.textsToEmbed().size();
        if (embeddings == null || embeddings.size() < expectedMinEmbeddings) {
            log.warn("Insufficient embeddings returned from model {}", result.modelId());
            return Optional.empty();
        }

        final double[] responseEmbedding;
        final double[] referenceEmbedding;
        if (texts.chunkingApplied()) {
            final int responseChunkCount = texts.responseChunkCount();
            responseEmbedding = aggregateChunkEmbeddings(embeddings, 0, responseChunkCount);
            referenceEmbedding = aggregateChunkEmbeddings(embeddings, responseChunkCount, embeddings.size());
        } else {
            responseEmbedding = convertToDoubleArray(embeddings.get(0));
            referenceEmbedding = convertToDoubleArray(embeddings.get(1));
        }
        return Optional.of(new EmbeddingsResult(responseEmbedding, referenceEmbedding));
    }

    /**
     * Строит событие исключения модели из шага ComputeEmbeddings — используется
     * как для {@code !isSuccess()}, так и для случая «недостаточно эмбеддингов».
     */
    private ModelExclusionEvent buildEmbeddingExclusion(final ModelResult<List<float[]>> result) {
        final Throwable cause =
                result.isSuccess() ? new IllegalStateException("Insufficient embeddings returned") : result.error();
        return ModelExclusionEvent.builder()
                .modelId(result.modelId())
                .failedStepName(STEP_COMPUTE_EMBEDDINGS)
                .failedStepIndex(STEP_INDEX_EMBEDDINGS)
                .cause(cause)
                .build();
    }

    /**
     * Усредняет подмассив чанк-эмбеддингов {@code [fromIndex; toIndex)} в один double-вектор
     * через {@link TextChunker#averageEmbeddings}.
     */
    private double[] aggregateChunkEmbeddings(final List<float[]> embeddings, final int fromIndex, final int toIndex) {
        final List<double[]> doubleVectors = new ArrayList<>(toIndex - fromIndex);
        for (int i = fromIndex; i < toIndex; i++) {
            doubleVectors.add(convertToDoubleArray(embeddings.get(i)));
        }
        return TextChunker.averageEmbeddings(doubleVectors);
    }

    /**
     * Вычисляет косинусную близость для каждой embedding-модели, применяет threshold
     * и собирает события исключений для моделей, упавших на вычислении.
     */
    private Map<String, Double> computeAllSimilarities(
            final Map<String, EmbeddingsResult> step1Successful,
            final SemanticSimilarityConfig config,
            final List<ModelExclusionEvent> accumulatedExclusions,
            final List<String> excludedModels) {
        final Map<String, Double> modelScores = new HashMap<>();
        for (final Map.Entry<String, EmbeddingsResult> entry : step1Successful.entrySet()) {
            computeSingleSimilarity(entry, config, accumulatedExclusions, excludedModels)
                    .ifPresent(score -> modelScores.put(entry.getKey(), score));
        }
        return modelScores;
    }

    /**
     * Вычисляет косинусную близость для одной модели и применяет threshold.
     * Возвращает пустой {@link Optional}, если вычисление упало; в этом случае
     * также дописывает запись об исключении модели.
     */
    private Optional<Double> computeSingleSimilarity(
            final Map.Entry<String, EmbeddingsResult> entry,
            final SemanticSimilarityConfig config,
            final List<ModelExclusionEvent> accumulatedExclusions,
            final List<String> excludedModels) {
        final String modelId = entry.getKey();
        final EmbeddingsResult embResult = entry.getValue();
        try {
            final double similarity = cosineSimilarity(embResult.responseEmbedding(), embResult.referenceEmbedding());
            final double finalScore = applyThreshold(similarity, config.getThreshold());
            log.debug("Semantic similarity for model {}: {} (raw: {})", modelId, finalScore, similarity);
            return Optional.of(finalScore);
        } catch (final Exception e) {
            log.warn("Failed to calculate similarity for model {}: {}", modelId, e.getMessage());
            excludedModels.add(modelId);
            accumulatedExclusions.add(ModelExclusionEvent.builder()
                    .modelId(modelId)
                    .failedStepName(STEP_COMPUTE_COSINE_SIMILARITY)
                    .failedStepIndex(STEP_INDEX_COSINE)
                    .cause(e)
                    .build());
            return Optional.empty();
        }
    }

    /**
     * Применяет бинарный threshold к сырому similarity, если он задан и положителен.
     */
    private double applyThreshold(final double similarity, final Double threshold) {
        if (threshold == null || threshold <= 0) {
            return similarity;
        }
        return similarity >= threshold ? 1.0 : 0.0;
    }

    /**
     * Converts float array to double array for more precise mathematical operations.
     */
    private double[] convertToDoubleArray(final float[] floatArray) {
        final double[] doubleArray = new double[floatArray.length];
        for (int i = 0; i < floatArray.length; i++) {
            doubleArray[i] = floatArray[i];
        }
        return doubleArray;
    }

    /**
     * Calculates cosine similarity between two embedding vectors.
     * <p>
     * The vectors are L2-normalized before computing the dot product,
     * following the standard practice for semantic similarity.
     *
     * @param vectorA First embedding vector
     * @param vectorB Second embedding vector
     * @return Cosine similarity score in range [-1, 1], typically [0, 1] for text embeddings
     * @throws IllegalArgumentException if vectors have different dimensions
     */
    private double cosineSimilarity(final double[] vectorA, final double[] vectorB) {
        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException(
                    "Vectors must have same length: " + vectorA.length + " vs " + vectorB.length);
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        normA = Math.sqrt(normA);
        normB = Math.sqrt(normB);

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (normA * normB);
    }

    /**
     * Result DTO for embeddings computation step.
     *
     * @param responseEmbedding  Embedding vector for the response
     * @param referenceEmbedding Embedding vector for the reference
     */
    public record EmbeddingsResult(double[] responseEmbedding, double[] referenceEmbedding) {}

    /** Тексты, подготовленные под выбранную стратегию для embedding-вызова. */
    private record EmbeddingTexts(
            List<String> textsToEmbed, int responseChunkCount, int referenceChunkCount, boolean chunkingApplied) {}

    /**
     * Configuration class for Semantic Similarity metric parameters.
     */
    @Data
    @Builder
    public static class SemanticSimilarityConfig implements MetricConfiguration {

        /**
         * Strategy for handling texts that exceed the embedding model's token limit.
         */
        public enum LongTextStrategy {
            /** Split into chunks, embed each, average vectors. */
            CHUNK,
            /** Truncate to token limit. */
            TRUNCATE,
            /** Legacy behavior — pass texts as-is; embedding model rejects if over token limit. */
            FAIL_FAST
        }

        /**
         * Optional threshold for binary pass/fail classification.
         * If set, scores above threshold return 1.0, below return 0.0.
         * If null or 0, returns the raw cosine similarity score.
         */
        @Builder.Default
        private Double threshold = null;

        /**
         * List of embedding model IDs to use for multi-model execution.
         * If empty, all available embedding models from executor will be used.
         */
        @Singular
        private List<String> models;

        /** Языковая метка (ISO-639) для будущей i18n-поддержки в объяснениях. */
        @Builder.Default
        private String language = "en";

        /**
         * Strategy for handling long texts that may exceed the embedding model's token limit.
         * Default is CHUNK — split into chunks, embed each, average vectors.
         */
        @Builder.Default
        private LongTextStrategy longTextStrategy = LongTextStrategy.CHUNK;

        /**
         * Maximum tokens per chunk when using CHUNK or TRUNCATE strategy.
         * Default is 512, matching common embedding model limits.
         */
        @Builder.Default
        private int maxTokensPerChunk = 512;

        /**
         * Эвристика chars-per-token для оценки длины текста перед эмбеддингом.
         * Default 3.0 (английский). Для русского рекомендуется 2.5,
         * для длинных английских текстов можно 3.5. Меньшее значение → больше чанков.
         */
        @Builder.Default
        private double charsPerToken = 3.0;

        /**
         * Creates a default configuration instance.
         *
         * @return Default configuration without threshold
         */
        public static SemanticSimilarityConfig defaultConfig() {
            return SemanticSimilarityConfig.builder().build();
        }
    }
}
