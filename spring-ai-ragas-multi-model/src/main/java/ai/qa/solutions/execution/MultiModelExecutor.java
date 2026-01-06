package ai.qa.solutions.execution;

import ai.qa.solutions.chatclient.ChatClientStore;
import ai.qa.solutions.embedding.EmbeddingModelStore;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.lang.Nullable;

/**
 * Stateless executor for making LLM and embedding calls across multiple models.
 * <p>
 * This executor:
 * <ul>
 *   <li>Does NOT manage any listeners (stateless)</li>
 *   <li>Does NOT aggregate scores (metrics do this)</li>
 *   <li>Does NOT track chain state (metrics do this)</li>
 *   <li>Simply executes calls on configured models in parallel and returns results</li>
 * </ul>
 * <p>
 * All execution methods return {@link ModelResult} objects containing:
 * <ul>
 *   <li>Model ID</li>
 *   <li>Result (if successful)</li>
 *   <li>Duration</li>
 *   <li>Request that was sent</li>
 *   <li>Error (if failed)</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Execute LLM call on all models
 * List<ModelResult<Response>> results = executor.executeLlm(prompt, Response.class);
 *
 * for (ModelResult<Response> result : results) {
 *     if (result.isSuccess()) {
 *         double score = result.result().getScore();
 *         log.info("Model {} scored {}", result.modelId(), score);
 *     }
 * }
 *
 * // Execute on specific model
 * ModelResult<Response> singleResult = executor.executeLlmOnModel("gpt-4", prompt, Response.class);
 * }</pre>
 *
 * @author Artem Simeshin
 * @see ModelResult
 */
@Slf4j
public class MultiModelExecutor {

    private final ChatClientStore chatClientStore;

    @Nullable
    private final EmbeddingModelStore embeddingModelStore;

    private final AsyncTaskExecutor taskExecutor;

    /**
     * Creates a new executor without embedding support.
     *
     * @param chatClientStore store of configured AI model clients
     * @param taskExecutor    executor for parallel async operations
     */
    public MultiModelExecutor(final ChatClientStore chatClientStore, final AsyncTaskExecutor taskExecutor) {
        this(chatClientStore, null, taskExecutor);
    }

    /**
     * Creates a new executor with embedding support.
     *
     * @param chatClientStore     store of configured AI model clients
     * @param embeddingModelStore store of configured embedding models (nullable)
     * @param taskExecutor        executor for parallel async operations
     */
    public MultiModelExecutor(
            final ChatClientStore chatClientStore,
            @Nullable final EmbeddingModelStore embeddingModelStore,
            final AsyncTaskExecutor taskExecutor) {
        this.chatClientStore = Objects.requireNonNull(chatClientStore, "chatClientStore");
        this.embeddingModelStore = embeddingModelStore;
        this.taskExecutor = Objects.requireNonNull(taskExecutor, "taskExecutor");
    }

    // ============ LLM Operations - All Models ============

    /**
     * Executes LLM call on ALL configured models in parallel.
     *
     * @param prompt       the prompt to send
     * @param responseType the expected response type
     * @param <R>          the response type
     * @return list of results from all models
     */
    public <R> List<ModelResult<R>> executeLlm(final String prompt, final Class<R> responseType) {
        return executeLlm(chatClientStore.getModelIds(), prompt, responseType);
    }

    /**
     * Executes LLM call on specified models in parallel.
     *
     * @param modelIds     list of model IDs to execute on
     * @param prompt       the prompt to send
     * @param responseType the expected response type
     * @param <R>          the response type
     * @return list of results from specified models
     */
    public <R> List<ModelResult<R>> executeLlm(
            final List<String> modelIds, final String prompt, final Class<R> responseType) {
        return executeLlmAsync(modelIds, prompt, responseType).join();
    }

    /**
     * Executes LLM call on ALL configured models in parallel (async).
     *
     * @param prompt       the prompt to send
     * @param responseType the expected response type
     * @param <R>          the response type
     * @return future with list of results from all models
     */
    public <R> CompletableFuture<List<ModelResult<R>>> executeLlmAsync(
            final String prompt, final Class<R> responseType) {
        return executeLlmAsync(chatClientStore.getModelIds(), prompt, responseType);
    }

    /**
     * Executes LLM call on specified models in parallel (async).
     *
     * @param modelIds     list of model IDs to execute on
     * @param prompt       the prompt to send
     * @param responseType the expected response type
     * @param <R>          the response type
     * @return future with list of results from specified models
     */
    public <R> CompletableFuture<List<ModelResult<R>>> executeLlmAsync(
            final List<String> modelIds, final String prompt, final Class<R> responseType) {
        if (modelIds == null || modelIds.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        final List<CompletableFuture<ModelResult<R>>> futures = modelIds.stream()
                .map(modelId -> executeLlmOnModelAsync(modelId, prompt, responseType))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream().map(CompletableFuture::join).toList());
    }

    // ============ LLM Operations - Single Model ============

    /**
     * Executes LLM call on a SPECIFIC model.
     *
     * @param modelId      the model ID to execute on
     * @param prompt       the prompt to send
     * @param responseType the expected response type
     * @param <R>          the response type
     * @return result from the specified model
     */
    public <R> ModelResult<R> executeLlmOnModel(
            final String modelId, final String prompt, final Class<R> responseType) {
        return executeLlmOnModelAsync(modelId, prompt, responseType).join();
    }

    /**
     * Executes LLM call on a SPECIFIC model (async).
     *
     * @param modelId      the model ID to execute on
     * @param prompt       the prompt to send
     * @param responseType the expected response type
     * @param <R>          the response type
     * @return future with result from the specified model
     */
    public <R> CompletableFuture<ModelResult<R>> executeLlmOnModelAsync(
            final String modelId, final String prompt, final Class<R> responseType) {
        return taskExecutor.submitCompletable(() -> {
            final Instant start = Instant.now();
            try {
                final ChatClient client = chatClientStore.get(modelId);
                final R response = client.prompt(prompt).call().entity(responseType);
                final Duration duration = Duration.between(start, Instant.now());
                return ModelResult.success(modelId, response, duration, prompt);
            } catch (Exception e) {
                final Duration duration = Duration.between(start, Instant.now());
                log.warn("Model {} failed: {}", modelId, e.getMessage());
                return ModelResult.failure(modelId, duration, prompt, e);
            }
        });
    }

    // ============ Embedding Operations - All Models ============

    /**
     * Executes embedding on ALL configured embedding models in parallel.
     *
     * @param text the text to embed
     * @return list of results from all embedding models
     */
    public List<ModelResult<float[]>> executeEmbedding(final String text) {
        return executeEmbeddingAsync(text).join();
    }

    /**
     * Executes embedding on ALL configured embedding models in parallel (async).
     *
     * @param text the text to embed
     * @return future with list of results from all embedding models
     */
    public CompletableFuture<List<ModelResult<float[]>>> executeEmbeddingAsync(final String text) {
        if (embeddingModelStore == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        final List<String> modelIds = embeddingModelStore.getModelIds();
        final List<CompletableFuture<ModelResult<float[]>>> futures = modelIds.stream()
                .map(modelId -> executeEmbeddingOnModelAsync(modelId, text))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream().map(CompletableFuture::join).toList());
    }

    /**
     * Executes embeddings for multiple texts on ALL configured models.
     *
     * @param texts the texts to embed
     * @return list of results from all embedding models
     */
    public List<ModelResult<List<float[]>>> executeEmbeddings(final List<String> texts) {
        return executeEmbeddingsAsync(texts).join();
    }

    /**
     * Executes embeddings for multiple texts on ALL configured models (async).
     *
     * @param texts the texts to embed
     * @return future with list of results from all embedding models
     */
    public CompletableFuture<List<ModelResult<List<float[]>>>> executeEmbeddingsAsync(final List<String> texts) {
        if (embeddingModelStore == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        final List<String> modelIds = embeddingModelStore.getModelIds();
        final List<CompletableFuture<ModelResult<List<float[]>>>> futures = modelIds.stream()
                .map(modelId -> executeEmbeddingsOnModelAsync(modelId, texts))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream().map(CompletableFuture::join).toList());
    }

    // ============ Embedding Operations - Single Model ============

    /**
     * Executes embedding on a SPECIFIC model.
     *
     * @param modelId the model ID to execute on
     * @param text    the text to embed
     * @return result from the specified model
     */
    public ModelResult<float[]> executeEmbeddingOnModel(final String modelId, final String text) {
        return executeEmbeddingOnModelAsync(modelId, text).join();
    }

    /**
     * Executes embedding on a SPECIFIC model (async).
     *
     * @param modelId the model ID to execute on
     * @param text    the text to embed
     * @return future with result from the specified model
     */
    public CompletableFuture<ModelResult<float[]>> executeEmbeddingOnModelAsync(
            final String modelId, final String text) {
        return taskExecutor.submitCompletable(() -> {
            final Instant start = Instant.now();
            try {
                if (embeddingModelStore == null) {
                    throw new IllegalStateException("EmbeddingModelStore not configured");
                }
                final EmbeddingModel embeddingModel = embeddingModelStore.get(modelId);
                final float[] embedding = embeddingModel.embed(text);
                final Duration duration = Duration.between(start, Instant.now());
                return ModelResult.success(modelId, embedding, duration, text);
            } catch (Exception e) {
                final Duration duration = Duration.between(start, Instant.now());
                log.warn("Embedding model {} failed: {}", modelId, e.getMessage());
                return ModelResult.failure(modelId, duration, text, e);
            }
        });
    }

    /**
     * Executes embeddings for multiple texts on a SPECIFIC model.
     *
     * @param modelId the model ID to execute on
     * @param texts   the texts to embed
     * @return result from the specified model
     */
    public ModelResult<List<float[]>> executeEmbeddingsOnModel(final String modelId, final List<String> texts) {
        return executeEmbeddingsOnModelAsync(modelId, texts).join();
    }

    /**
     * Executes embeddings for multiple texts on a SPECIFIC model (async).
     *
     * @param modelId the model ID to execute on
     * @param texts   the texts to embed
     * @return future with result from the specified model
     */
    public CompletableFuture<ModelResult<List<float[]>>> executeEmbeddingsOnModelAsync(
            final String modelId, final List<String> texts) {
        return taskExecutor.submitCompletable(() -> {
            final Instant start = Instant.now();
            final String request = String.join(", ", texts);
            try {
                if (embeddingModelStore == null) {
                    throw new IllegalStateException("EmbeddingModelStore not configured");
                }
                final EmbeddingModel embeddingModel = embeddingModelStore.get(modelId);
                final List<float[]> embeddings = new ArrayList<>();
                for (final String text : texts) {
                    embeddings.add(embeddingModel.embed(text));
                }
                final Duration duration = Duration.between(start, Instant.now());
                return ModelResult.success(modelId, embeddings, duration, request);
            } catch (Exception e) {
                final Duration duration = Duration.between(start, Instant.now());
                log.warn("Embedding model {} failed: {}", modelId, e.getMessage());
                return ModelResult.failure(modelId, duration, request, e);
            }
        });
    }

    // ============ Utility Methods ============

    /**
     * Gets all configured LLM model IDs.
     *
     * @return list of model IDs
     */
    public List<String> getModelIds() {
        return chatClientStore.getModelIds();
    }

    /**
     * Gets all configured embedding model IDs.
     *
     * @return list of embedding model IDs, or empty list if not configured
     */
    public List<String> getEmbeddingModelIds() {
        return embeddingModelStore != null ? embeddingModelStore.getModelIds() : List.of();
    }
}
