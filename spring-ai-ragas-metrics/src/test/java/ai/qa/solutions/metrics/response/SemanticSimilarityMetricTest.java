package ai.qa.solutions.metrics.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;

import ai.qa.solutions.chatclient.ChatClientStore;
import ai.qa.solutions.execution.MultiModelExecutor;
import ai.qa.solutions.execution.StubMultiModelExecutor;
import ai.qa.solutions.execution.listener.MetricExecutionListener;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationContext;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationResult;
import ai.qa.solutions.metric.metadata.SemanticSimilarityMetadata;
import ai.qa.solutions.sample.Sample;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

@Epic("Metrics")
@Feature("SemanticSimilarity")
@DisplayName("SemanticSimilarityMetric Tests")
class SemanticSimilarityMetricTest {

    private MultiModelExecutor executor;

    @BeforeEach
    void setUp() {
        ChatClient mockClient = mock(ChatClient.class);
        ChatClientStore store = new ChatClientStore(Map.of("model-1", mockClient), mockClient);
        executor = new MultiModelExecutor(store, null, new SimpleAsyncTaskExecutor());
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("Should create metric with executor")
        void shouldCreateWithExecutor() {
            SemanticSimilarityMetric metric =
                    SemanticSimilarityMetric.builder().executor(executor).build();

            assertThat(metric).isNotNull();
            assertThat(metric.getName()).isEqualTo("SemanticSimilarityMetric");
        }

        @Test
        @DisplayName("toBuilder should preserve settings")
        void toBuilderShouldPreserveSettings() {
            SemanticSimilarityMetric original =
                    SemanticSimilarityMetric.builder().executor(executor).build();

            SemanticSimilarityMetric copy = original.toBuilder().build();

            assertThat(copy).isNotSameAs(original);
            assertThat(copy.getName()).isEqualTo(original.getName());
        }
    }

    @Nested
    @DisplayName("EmbeddingsResult Record")
    class EmbeddingsResultTests {

        @Test
        @DisplayName("Should store embeddings")
        void shouldStoreEmbeddings() {
            double[] responseEmbedding = {0.1, 0.2, 0.3};
            double[] referenceEmbedding = {0.4, 0.5, 0.6};

            SemanticSimilarityMetric.EmbeddingsResult result =
                    new SemanticSimilarityMetric.EmbeddingsResult(responseEmbedding, referenceEmbedding);

            assertThat(result.responseEmbedding()).isEqualTo(responseEmbedding);
            assertThat(result.referenceEmbedding()).isEqualTo(referenceEmbedding);
        }

        @Test
        @DisplayName("Should handle null embeddings")
        void shouldHandleNullEmbeddings() {
            SemanticSimilarityMetric.EmbeddingsResult result =
                    new SemanticSimilarityMetric.EmbeddingsResult(null, null);

            assertThat(result.responseEmbedding()).isNull();
            assertThat(result.referenceEmbedding()).isNull();
        }
    }

    @Nested
    @DisplayName("SemanticSimilarityConfig")
    class ConfigTests {

        @Test
        @DisplayName("Should have default threshold of null")
        void shouldHaveDefaultThresholdNull() {
            SemanticSimilarityMetric.SemanticSimilarityConfig config =
                    SemanticSimilarityMetric.SemanticSimilarityConfig.builder().build();

            assertThat(config.getThreshold()).isNull();
        }

        @Test
        @DisplayName("Should allow custom threshold")
        void shouldAllowCustomThreshold() {
            SemanticSimilarityMetric.SemanticSimilarityConfig config =
                    SemanticSimilarityMetric.SemanticSimilarityConfig.builder()
                            .threshold(0.8)
                            .build();

            assertThat(config.getThreshold()).isEqualTo(0.8);
        }

        @Test
        @DisplayName("Should start with empty models list")
        void shouldStartWithEmptyModelsList() {
            SemanticSimilarityMetric.SemanticSimilarityConfig config =
                    SemanticSimilarityMetric.SemanticSimilarityConfig.builder().build();

            assertThat(config.getModels()).isEmpty();
        }

        @Test
        @DisplayName("Should allow specifying models")
        void shouldAllowSpecifyingModels() {
            SemanticSimilarityMetric.SemanticSimilarityConfig config =
                    SemanticSimilarityMetric.SemanticSimilarityConfig.builder()
                            .model("embedding-1")
                            .model("embedding-2")
                            .build();

            assertThat(config.getModels()).containsExactly("embedding-1", "embedding-2");
        }

        @Test
        @DisplayName("defaultConfig should create valid default config")
        void defaultConfigShouldCreateValidConfig() {
            SemanticSimilarityMetric.SemanticSimilarityConfig config =
                    SemanticSimilarityMetric.SemanticSimilarityConfig.defaultConfig();

            assertThat(config).isNotNull();
            assertThat(config.getThreshold()).isNull();
        }

        @Test
        @DisplayName("Should have default charsPerToken of 3.0")
        void shouldHaveDefaultCharsPerToken3() {
            // given + when
            final SemanticSimilarityMetric.SemanticSimilarityConfig config =
                    SemanticSimilarityMetric.SemanticSimilarityConfig.builder().build();

            // then
            assertThat(config.getCharsPerToken()).isEqualTo(3.0);
        }

        @Test
        @DisplayName("Should allow custom charsPerToken")
        void shouldAllowCustomCharsPerToken() {
            // given + when
            final SemanticSimilarityMetric.SemanticSimilarityConfig config =
                    SemanticSimilarityMetric.SemanticSimilarityConfig.builder()
                            .charsPerToken(2.5)
                            .build();

            // then
            assertThat(config.getCharsPerToken()).isEqualTo(2.5);
        }
    }

    @Nested
    @DisplayName("Scoring")
    class ScoringTests {

        @Test
        @DisplayName("Should return 0.0 when response is missing")
        void shouldReturn0WhenNoResponse() {
            StubMultiModelExecutor stubExecutor =
                    new StubMultiModelExecutor(List.of("model-1"), List.of("embedding-1"));

            SemanticSimilarityMetric metric =
                    SemanticSimilarityMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder().reference("Reference text").build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 0.0 when reference is missing")
        void shouldReturn0WhenNoReference() {
            StubMultiModelExecutor stubExecutor =
                    new StubMultiModelExecutor(List.of("model-1"), List.of("embedding-1"));

            SemanticSimilarityMetric metric =
                    SemanticSimilarityMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder().response("Response text").build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 0.0 when no embedding models available")
        void shouldReturn0WhenNoEmbeddingModels() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"), List.of());

            SemanticSimilarityMetric metric =
                    SemanticSimilarityMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .response("Response text")
                    .reference("Reference text")
                    .build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 1.0 for identical embeddings")
        void shouldReturn1ForIdenticalEmbeddings() {
            float[] embedding = {0.1f, 0.2f, 0.3f, 0.4f, 0.5f};

            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"), List.of("embedding-1"))
                    .withEmbeddings(texts -> {
                        // Return identical embeddings for all texts
                        return texts.stream().map(t -> embedding).toList();
                    });

            SemanticSimilarityMetric metric =
                    SemanticSimilarityMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .response("Paris is the capital of France.")
                    .reference("Paris is the capital of France.")
                    .build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isCloseTo(1.0, within(0.01));
        }

        @Test
        @DisplayName("Should return 0.0 for orthogonal embeddings")
        void shouldReturn0ForOrthogonalEmbeddings() {
            // Orthogonal vectors: [1, 0, 0] and [0, 1, 0]
            float[] responseEmb = {1.0f, 0.0f, 0.0f};
            float[] referenceEmb = {0.0f, 1.0f, 0.0f};

            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"), List.of("embedding-1"))
                    .withEmbeddings(texts -> List.of(responseEmb, referenceEmb));

            SemanticSimilarityMetric metric =
                    SemanticSimilarityMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .response("Response text")
                    .reference("Reference text")
                    .build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isCloseTo(0.0, within(0.01));
        }

        @Test
        @DisplayName("Should return intermediate score for partially similar embeddings")
        void shouldReturnIntermediateScoreForPartialSimilarity() {
            // Vectors with ~0.866 cosine similarity
            float[] responseEmb = {1.0f, 0.0f, 0.0f};
            float[] referenceEmb = {0.866f, 0.5f, 0.0f};

            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"), List.of("embedding-1"))
                    .withEmbeddings(texts -> List.of(responseEmb, referenceEmb));

            SemanticSimilarityMetric metric =
                    SemanticSimilarityMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .response("Response text")
                    .reference("Reference text")
                    .build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isBetween(0.8, 0.9);
        }

        @Test
        @DisplayName("Should apply threshold when configured")
        void shouldApplyThresholdWhenConfigured() {
            // Vectors with ~0.7 cosine similarity
            float[] responseEmb = {1.0f, 0.0f};
            float[] referenceEmb = {0.7f, 0.714f}; // normalized, cosine sim ~0.7

            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"), List.of("embedding-1"))
                    .withEmbeddings(texts -> List.of(responseEmb, referenceEmb));

            SemanticSimilarityMetric metric =
                    SemanticSimilarityMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .response("Response text")
                    .reference("Reference text")
                    .build();

            // With threshold 0.8, should return 0.0 (below threshold)
            SemanticSimilarityMetric.SemanticSimilarityConfig configHigh =
                    SemanticSimilarityMetric.SemanticSimilarityConfig.builder()
                            .threshold(0.8)
                            .build();

            Double scoreWithHighThreshold = metric.singleTurnScore(configHigh, sample);
            assertThat(scoreWithHighThreshold).isEqualTo(0.0);

            // With threshold 0.5, should return 1.0 (above threshold)
            SemanticSimilarityMetric.SemanticSimilarityConfig configLow =
                    SemanticSimilarityMetric.SemanticSimilarityConfig.builder()
                            .threshold(0.5)
                            .build();

            Double scoreWithLowThreshold = metric.singleTurnScore(configLow, sample);
            assertThat(scoreWithLowThreshold).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should throw when all embedding models fail")
        void shouldThrowWhenAllModelsFail() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"), List.of("embedding-1"))
                    .withModelError("embedding-1", new RuntimeException("Embedding model failed"));

            SemanticSimilarityMetric metric =
                    SemanticSimilarityMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .response("Response text")
                    .reference("Reference text")
                    .build();

            assertThatThrownBy(() -> metric.singleTurnScore(sample))
                    .hasCauseInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("All embedding models failed");
        }

        @Test
        @DisplayName("Should work with async scoring")
        void shouldWorkWithAsyncScoring() {
            float[] embedding = {0.1f, 0.2f, 0.3f};

            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"), List.of("embedding-1"))
                    .withEmbeddings(texts -> texts.stream().map(t -> embedding).toList());

            SemanticSimilarityMetric metric =
                    SemanticSimilarityMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .response("Response text")
                    .reference("Reference text")
                    .build();

            Double score = metric.singleTurnScoreAsync(sample).join();

            assertThat(score).isCloseTo(1.0, within(0.01));
        }

        @Test
        @DisplayName("Should work with multiple embedding models and aggregate scores")
        void shouldWorkWithMultipleEmbeddingModels() {
            // Different models return slightly different embeddings
            float[] embedding1 = {1.0f, 0.0f, 0.0f};
            float[] embedding2 = {0.9f, 0.436f, 0.0f}; // ~0.9 cosine sim with [1,0,0]

            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(
                            List.of("model-1"), List.of("embedding-1", "embedding-2"))
                    .withEmbeddings(texts -> List.of(embedding1, embedding2));

            SemanticSimilarityMetric metric =
                    SemanticSimilarityMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .response("Response text")
                    .reference("Reference text")
                    .build();

            Double score = metric.singleTurnScore(sample);

            // Aggregated score from multiple models
            assertThat(score).isBetween(0.0, 1.0);
        }

        @Test
        @DisplayName("Should handle empty strings as single space")
        void shouldHandleEmptyStrings() {
            float[] embedding = {0.1f, 0.2f, 0.3f};

            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"), List.of("embedding-1"))
                    .withEmbeddings(texts -> texts.stream().map(t -> embedding).toList());

            SemanticSimilarityMetric metric =
                    SemanticSimilarityMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .response("  ") // only whitespace
                    .reference("Reference text")
                    .build();

            // Should return 0.0 for whitespace-only response
            Double score = metric.singleTurnScore(sample);
            assertThat(score).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Listener Notifications")
    class ListenerTests {

        @Test
        @DisplayName("Should notify listeners during evaluation")
        void shouldNotifyListenersDuringEvaluation() {
            float[] embedding = {0.1f, 0.2f, 0.3f};

            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"), List.of("embedding-1"))
                    .withEmbeddings(texts -> texts.stream().map(t -> embedding).toList());

            RecordingListener listener = new RecordingListener();

            SemanticSimilarityMetric metric = SemanticSimilarityMetric.builder()
                    .executor(stubExecutor)
                    .build()
                    .withListeners(List.of(listener));

            Sample sample = Sample.builder()
                    .response("Response text")
                    .reference("Reference text")
                    .build();

            metric.singleTurnScore(sample);

            assertThat(listener.beforeMetricCalled).isTrue();
            assertThat(listener.afterMetricCalled).isTrue();
        }

        static class RecordingListener implements MetricExecutionListener {
            boolean beforeMetricCalled = false;
            boolean afterMetricCalled = false;

            @Override
            public void beforeMetricEvaluation(MetricEvaluationContext context) {
                beforeMetricCalled = true;
            }

            @Override
            public void afterMetricEvaluation(MetricEvaluationResult result) {
                afterMetricCalled = true;
            }

            @Override
            public MetricExecutionListener forEvaluation() {
                return this;
            }
        }
    }

    @Nested
    @DisplayName("Chunking")
    class ChunkingTests {

        @Test
        @DisplayName("Should use CHUNK strategy by default")
        void shouldUseChunkStrategyByDefault() {
            final SemanticSimilarityMetric.SemanticSimilarityConfig config =
                    SemanticSimilarityMetric.SemanticSimilarityConfig.defaultConfig();

            assertThat(config.getLongTextStrategy())
                    .isEqualTo(SemanticSimilarityMetric.SemanticSimilarityConfig.LongTextStrategy.CHUNK);
        }

        @Test
        @DisplayName("Should have maxTokensPerChunk default of 512")
        void shouldHaveMaxTokensPerChunkDefault512() {
            final SemanticSimilarityMetric.SemanticSimilarityConfig config =
                    SemanticSimilarityMetric.SemanticSimilarityConfig.defaultConfig();

            assertThat(config.getMaxTokensPerChunk()).isEqualTo(512);
        }

        @Test
        @DisplayName("Should include chunking info in metadata when long text triggers chunking")
        void shouldIncludeChunkingInfoInMetadata() {
            // Create text long enough to trigger chunking (> 512 tokens = > 1536 chars)
            final String longResponse = "This is a sentence about response. ".repeat(60); // ~2100 chars = ~700 tokens
            final String longReference = "This is a sentence about reference. ".repeat(60);

            final float[] embedding = {0.1f, 0.2f, 0.3f};

            final StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(
                            List.of("model-1"), List.of("embedding-1"))
                    .withEmbeddings(texts -> texts.stream().map(t -> embedding).toList());

            final AtomicReference<MetricEvaluationResult> capturedResult = new AtomicReference<>();

            final MetricExecutionListener capturingListener = new MetricExecutionListener() {
                @Override
                public void beforeMetricEvaluation(final MetricEvaluationContext context) {}

                @Override
                public void afterMetricEvaluation(final MetricEvaluationResult result) {
                    capturedResult.set(result);
                }

                @Override
                public MetricExecutionListener forEvaluation() {
                    return this;
                }
            };

            final SemanticSimilarityMetric metric = SemanticSimilarityMetric.builder()
                    .executor(stubExecutor)
                    .build()
                    .withListeners(List.of(capturingListener));

            final Sample sample = Sample.builder()
                    .response(longResponse)
                    .reference(longReference)
                    .build();

            metric.singleTurnScore(sample);

            assertThat(capturedResult.get()).isNotNull();
            final Object metadata = capturedResult.get().getMetadata();
            assertThat(metadata).isInstanceOf(SemanticSimilarityMetadata.class);

            final SemanticSimilarityMetadata semMetadata = (SemanticSimilarityMetadata) metadata;
            assertThat(semMetadata.chunkingApplied()).isTrue();
            assertThat(semMetadata.responseChunkCount()).isGreaterThan(1);
            assertThat(semMetadata.referenceChunkCount()).isGreaterThan(1);
            assertThat(semMetadata.longTextStrategy()).isEqualTo("CHUNK");
        }

        @Test
        @Story("Стратегия TRUNCATE")
        @Description("Длинный текст при стратегии TRUNCATE усекается до лимита и флаг chunkingApplied=false")
        void truncateStrategy_longText_truncatesAndChunkingAppliedFalse() {
            // given
            final String longResponse = "This is a sentence about response. ".repeat(60); // ~2100 chars
            final String longReference = "This is a sentence about reference. ".repeat(60);

            final float[] embedding = {0.1f, 0.2f, 0.3f};
            final List<List<String>> capturedTexts = new ArrayList<>();

            final StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(
                            List.of("model-1"), List.of("embedding-1"))
                    .withEmbeddings(texts -> {
                        capturedTexts.add(new ArrayList<>(texts));
                        return texts.stream().map(t -> embedding).toList();
                    });

            final AtomicReference<MetricEvaluationResult> capturedResult = new AtomicReference<>();
            final MetricExecutionListener capturingListener = new MetricExecutionListener() {
                @Override
                public void beforeMetricEvaluation(final MetricEvaluationContext context) {}

                @Override
                public void afterMetricEvaluation(final MetricEvaluationResult result) {
                    capturedResult.set(result);
                }

                @Override
                public MetricExecutionListener forEvaluation() {
                    return this;
                }
            };

            final SemanticSimilarityMetric metric = SemanticSimilarityMetric.builder()
                    .executor(stubExecutor)
                    .build()
                    .withListeners(List.of(capturingListener));

            final SemanticSimilarityMetric.SemanticSimilarityConfig config =
                    SemanticSimilarityMetric.SemanticSimilarityConfig.builder()
                            .longTextStrategy(
                                    SemanticSimilarityMetric.SemanticSimilarityConfig.LongTextStrategy.TRUNCATE)
                            .maxTokensPerChunk(512)
                            .build();

            final Sample sample = Sample.builder()
                    .response(longResponse)
                    .reference(longReference)
                    .build();

            // when
            metric.singleTurnScore(config, sample);

            // then
            assertThat(capturedTexts).hasSize(1);
            assertThat(capturedTexts.get(0)).hasSize(2);
            assertThat(capturedTexts.get(0).get(0).length()).isLessThan(longResponse.length());
            assertThat(capturedTexts.get(0).get(1).length()).isLessThan(longReference.length());

            assertThat(capturedResult.get()).isNotNull();
            final Object metadata = capturedResult.get().getMetadata();
            assertThat(metadata).isInstanceOf(SemanticSimilarityMetadata.class);
            final SemanticSimilarityMetadata semMetadata = (SemanticSimilarityMetadata) metadata;
            assertThat(semMetadata.chunkingApplied()).isFalse();
            assertThat(semMetadata.responseChunkCount()).isEqualTo(1);
            assertThat(semMetadata.referenceChunkCount()).isEqualTo(1);
            assertThat(semMetadata.longTextStrategy()).isEqualTo("TRUNCATE");
        }

        @Test
        @Story("Стратегия FAIL_FAST")
        @Description("Длинный текст при стратегии FAIL_FAST передаётся как есть без усечения и без чанкинга")
        void failFastStrategy_longText_passesAsIsAndChunkingAppliedFalse() {
            // given
            final String longResponse = "This is a sentence about response. ".repeat(60); // ~2100 chars
            final String longReference = "This is a sentence about reference. ".repeat(60);

            final float[] embedding = {0.1f, 0.2f, 0.3f};
            final List<List<String>> capturedTexts = new ArrayList<>();

            final StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(
                            List.of("model-1"), List.of("embedding-1"))
                    .withEmbeddings(texts -> {
                        capturedTexts.add(new ArrayList<>(texts));
                        return texts.stream().map(t -> embedding).toList();
                    });

            final AtomicReference<MetricEvaluationResult> capturedResult = new AtomicReference<>();
            final MetricExecutionListener capturingListener = new MetricExecutionListener() {
                @Override
                public void beforeMetricEvaluation(final MetricEvaluationContext context) {}

                @Override
                public void afterMetricEvaluation(final MetricEvaluationResult result) {
                    capturedResult.set(result);
                }

                @Override
                public MetricExecutionListener forEvaluation() {
                    return this;
                }
            };

            final SemanticSimilarityMetric metric = SemanticSimilarityMetric.builder()
                    .executor(stubExecutor)
                    .build()
                    .withListeners(List.of(capturingListener));

            final SemanticSimilarityMetric.SemanticSimilarityConfig config =
                    SemanticSimilarityMetric.SemanticSimilarityConfig.builder()
                            .longTextStrategy(
                                    SemanticSimilarityMetric.SemanticSimilarityConfig.LongTextStrategy.FAIL_FAST)
                            .build();

            final Sample sample = Sample.builder()
                    .response(longResponse)
                    .reference(longReference)
                    .build();

            // when
            metric.singleTurnScore(config, sample);

            // then
            assertThat(capturedTexts).hasSize(1);
            assertThat(capturedTexts.get(0)).hasSize(2);
            assertThat(capturedTexts.get(0).get(0)).isEqualTo(longResponse);
            assertThat(capturedTexts.get(0).get(1)).isEqualTo(longReference);

            assertThat(capturedResult.get()).isNotNull();
            final Object metadata = capturedResult.get().getMetadata();
            assertThat(metadata).isInstanceOf(SemanticSimilarityMetadata.class);
            final SemanticSimilarityMetadata semMetadata = (SemanticSimilarityMetadata) metadata;
            assertThat(semMetadata.chunkingApplied()).isFalse();
            assertThat(semMetadata.responseChunkCount()).isEqualTo(1);
            assertThat(semMetadata.referenceChunkCount()).isEqualTo(1);
            assertThat(semMetadata.longTextStrategy()).isEqualTo("FAIL_FAST");
        }

        @Test
        @Story("Ассиметричные тексты: длинный response, короткий reference")
        @Description(
                "При чанкинге ассиметричных текстов длинный response разбивается, короткий reference остаётся одним чанком")
        void chunkStrategy_asymmetricTexts_responseChunkedReferenceShort() {
            // given
            final String longResponse = "Long sentence. ".repeat(200); // ~3000 chars
            final String shortReference = "Short ref.";

            final float[] embedding = {0.1f, 0.2f, 0.3f};
            final List<List<String>> capturedTexts = new ArrayList<>();

            final StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(
                            List.of("model-1"), List.of("embedding-1"))
                    .withEmbeddings(texts -> {
                        capturedTexts.add(new ArrayList<>(texts));
                        return texts.stream().map(t -> embedding).toList();
                    });

            final AtomicReference<MetricEvaluationResult> capturedResult = new AtomicReference<>();
            final MetricExecutionListener capturingListener = new MetricExecutionListener() {
                @Override
                public void beforeMetricEvaluation(final MetricEvaluationContext context) {}

                @Override
                public void afterMetricEvaluation(final MetricEvaluationResult result) {
                    capturedResult.set(result);
                }

                @Override
                public MetricExecutionListener forEvaluation() {
                    return this;
                }
            };

            final SemanticSimilarityMetric metric = SemanticSimilarityMetric.builder()
                    .executor(stubExecutor)
                    .build()
                    .withListeners(List.of(capturingListener));

            final SemanticSimilarityMetric.SemanticSimilarityConfig config =
                    SemanticSimilarityMetric.SemanticSimilarityConfig.builder()
                            .maxTokensPerChunk(50)
                            .build();

            final Sample sample = Sample.builder()
                    .response(longResponse)
                    .reference(shortReference)
                    .build();

            // when
            metric.singleTurnScore(config, sample);

            // then
            assertThat(capturedResult.get()).isNotNull();
            final Object metadata = capturedResult.get().getMetadata();
            assertThat(metadata).isInstanceOf(SemanticSimilarityMetadata.class);
            final SemanticSimilarityMetadata semMetadata = (SemanticSimilarityMetadata) metadata;
            assertThat(semMetadata.responseChunkCount()).isGreaterThan(1);
            assertThat(semMetadata.referenceChunkCount()).isEqualTo(1);
            assertThat(semMetadata.chunkingApplied()).isTrue();
            assertThat(capturedTexts).hasSize(1);
            assertThat(capturedTexts.get(0)).hasSize(semMetadata.responseChunkCount() + 1);
        }

        @Test
        @Story("Ассиметричные тексты: короткий response, длинный reference")
        @Description(
                "При чанкинге ассиметричных текстов длинный reference разбивается, короткий response остаётся одним чанком")
        void chunkStrategy_asymmetricTexts_referenceChunkedResponseShort() {
            // given
            final String shortResponse = "Short resp.";
            final String longReference = "Long sentence. ".repeat(200); // ~3000 chars

            final float[] embedding = {0.1f, 0.2f, 0.3f};
            final List<List<String>> capturedTexts = new ArrayList<>();

            final StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(
                            List.of("model-1"), List.of("embedding-1"))
                    .withEmbeddings(texts -> {
                        capturedTexts.add(new ArrayList<>(texts));
                        return texts.stream().map(t -> embedding).toList();
                    });

            final AtomicReference<MetricEvaluationResult> capturedResult = new AtomicReference<>();
            final MetricExecutionListener capturingListener = new MetricExecutionListener() {
                @Override
                public void beforeMetricEvaluation(final MetricEvaluationContext context) {}

                @Override
                public void afterMetricEvaluation(final MetricEvaluationResult result) {
                    capturedResult.set(result);
                }

                @Override
                public MetricExecutionListener forEvaluation() {
                    return this;
                }
            };

            final SemanticSimilarityMetric metric = SemanticSimilarityMetric.builder()
                    .executor(stubExecutor)
                    .build()
                    .withListeners(List.of(capturingListener));

            final SemanticSimilarityMetric.SemanticSimilarityConfig config =
                    SemanticSimilarityMetric.SemanticSimilarityConfig.builder()
                            .maxTokensPerChunk(50)
                            .build();

            final Sample sample = Sample.builder()
                    .response(shortResponse)
                    .reference(longReference)
                    .build();

            // when
            metric.singleTurnScore(config, sample);

            // then
            assertThat(capturedResult.get()).isNotNull();
            final Object metadata = capturedResult.get().getMetadata();
            assertThat(metadata).isInstanceOf(SemanticSimilarityMetadata.class);
            final SemanticSimilarityMetadata semMetadata = (SemanticSimilarityMetadata) metadata;
            assertThat(semMetadata.responseChunkCount()).isEqualTo(1);
            assertThat(semMetadata.referenceChunkCount()).isGreaterThan(1);
            assertThat(semMetadata.chunkingApplied()).isTrue();
            assertThat(capturedTexts).hasSize(1);
            assertThat(capturedTexts.get(0)).hasSize(1 + semMetadata.referenceChunkCount());
        }

        @Test
        @Story("Кастомный charsPerToken")
        @Description("Для кириллического текста charsPerToken=2.5 даёт больше чанков, чем значение по умолчанию 3.0")
        void chunkStrategy_customCharsPerToken_cyrillicProducesMoreChunks() {
            // given
            // 66 chars * 4 = 264 chars: at default 3.0 → 88 tokens (<=100, one chunk),
            // at 2.5 → 106 tokens (>100, triggers chunking into 2+ chunks, maxChars=250)
            final String cyrillicText =
                    "Это длинное предложение на русском языке для проверки чанкования. ".repeat(4); // ~264 chars

            final float[] embedding = {0.1f, 0.2f, 0.3f};
            final StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(
                            List.of("model-1"), List.of("embedding-1"))
                    .withEmbeddings(texts -> texts.stream().map(t -> embedding).toList());

            final AtomicReference<MetricEvaluationResult> capturedDefault = new AtomicReference<>();
            final AtomicReference<MetricEvaluationResult> capturedCustom = new AtomicReference<>();

            final MetricExecutionListener defaultListener = new MetricExecutionListener() {
                @Override
                public void beforeMetricEvaluation(final MetricEvaluationContext context) {}

                @Override
                public void afterMetricEvaluation(final MetricEvaluationResult result) {
                    capturedDefault.set(result);
                }

                @Override
                public MetricExecutionListener forEvaluation() {
                    return this;
                }
            };
            final MetricExecutionListener customListener = new MetricExecutionListener() {
                @Override
                public void beforeMetricEvaluation(final MetricEvaluationContext context) {}

                @Override
                public void afterMetricEvaluation(final MetricEvaluationResult result) {
                    capturedCustom.set(result);
                }

                @Override
                public MetricExecutionListener forEvaluation() {
                    return this;
                }
            };

            final SemanticSimilarityMetric metricDefault = SemanticSimilarityMetric.builder()
                    .executor(stubExecutor)
                    .build()
                    .withListeners(List.of(defaultListener));
            final SemanticSimilarityMetric metricCustom = SemanticSimilarityMetric.builder()
                    .executor(stubExecutor)
                    .build()
                    .withListeners(List.of(customListener));

            final SemanticSimilarityMetric.SemanticSimilarityConfig defaultConfig =
                    SemanticSimilarityMetric.SemanticSimilarityConfig.builder()
                            .maxTokensPerChunk(100)
                            .build();
            final SemanticSimilarityMetric.SemanticSimilarityConfig customConfig =
                    SemanticSimilarityMetric.SemanticSimilarityConfig.builder()
                            .maxTokensPerChunk(100)
                            .charsPerToken(2.5)
                            .build();

            final Sample sample = Sample.builder()
                    .response(cyrillicText)
                    .reference(cyrillicText)
                    .build();

            // when
            metricDefault.singleTurnScore(defaultConfig, sample);
            metricCustom.singleTurnScore(customConfig, sample);

            // then
            final SemanticSimilarityMetadata defaultMetadata =
                    (SemanticSimilarityMetadata) capturedDefault.get().getMetadata();
            final SemanticSimilarityMetadata customMetadata =
                    (SemanticSimilarityMetadata) capturedCustom.get().getMetadata();

            assertThat(defaultMetadata.responseChunkCount()).isEqualTo(1);
            assertThat(customMetadata.responseChunkCount()).isGreaterThan(1);
            assertThat(customMetadata.responseChunkCount()).isGreaterThan(defaultMetadata.responseChunkCount());
        }
    }
}
