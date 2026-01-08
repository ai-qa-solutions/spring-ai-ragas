package ai.qa.solutions.execution;

import ai.qa.solutions.chatclient.ChatClientStore;
import ai.qa.solutions.embedding.EmbeddingModelStore;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.springframework.core.task.AsyncTaskExecutor;

/**
 * Stub implementation of MultiModelExecutor for testing metrics without real LLM calls.
 * <p>
 * Allows configuring expected responses for each response type and model.
 * Returns predefined responses instead of making actual LLM/embedding calls.
 */
public class StubMultiModelExecutor extends MultiModelExecutor {

    private final List<String> modelIds;
    private final List<String> embeddingModelIds;

    // Response providers by response type class
    private final Map<Class<?>, Function<String, ?>> responseProviders = new HashMap<>();

    // Embedding providers
    private Function<String, float[]> embeddingProvider;
    private Function<List<String>, List<float[]>> embeddingsProvider;

    // Error simulation
    private final Map<String, Exception> modelErrors = new HashMap<>();

    public StubMultiModelExecutor(List<String> modelIds) {
        this(modelIds, List.of());
    }

    public StubMultiModelExecutor(List<String> modelIds, List<String> embeddingModelIds) {
        super(
                createMinimalChatClientStore(modelIds),
                createMinimalEmbeddingModelStore(embeddingModelIds),
                createSyncTaskExecutor());
        this.modelIds = new ArrayList<>(modelIds);
        this.embeddingModelIds = new ArrayList<>(embeddingModelIds);
    }

    private static ChatClientStore createMinimalChatClientStore(List<String> modelIds) {
        // Create a minimal store - actual clients won't be used
        Map<String, org.springframework.ai.chat.client.ChatClient> clients = new HashMap<>();
        org.springframework.ai.chat.client.ChatClient defaultClient = null;
        for (String modelId : modelIds) {
            org.springframework.ai.chat.client.ChatClient mockClient =
                    org.mockito.Mockito.mock(org.springframework.ai.chat.client.ChatClient.class);
            clients.put(modelId, mockClient);
            if (defaultClient == null) {
                defaultClient = mockClient;
            }
        }
        if (defaultClient == null) {
            defaultClient = org.mockito.Mockito.mock(org.springframework.ai.chat.client.ChatClient.class);
        }
        return new ChatClientStore(clients, defaultClient);
    }

    private static EmbeddingModelStore createMinimalEmbeddingModelStore(List<String> embeddingModelIds) {
        if (embeddingModelIds.isEmpty()) {
            return null;
        }
        Map<String, org.springframework.ai.embedding.EmbeddingModel> models = new HashMap<>();
        org.springframework.ai.embedding.EmbeddingModel defaultModel = null;
        for (String modelId : embeddingModelIds) {
            org.springframework.ai.embedding.EmbeddingModel mockModel =
                    org.mockito.Mockito.mock(org.springframework.ai.embedding.EmbeddingModel.class);
            models.put(modelId, mockModel);
            if (defaultModel == null) {
                defaultModel = mockModel;
            }
        }
        return new EmbeddingModelStore(models, defaultModel);
    }

    private static AsyncTaskExecutor createSyncTaskExecutor() {
        // Synchronous executor for predictable test behavior
        return new AsyncTaskExecutor() {
            @Override
            public void execute(Runnable task) {
                task.run();
            }

            @Override
            public <T> CompletableFuture<T> submitCompletable(java.util.concurrent.Callable<T> task) {
                try {
                    return CompletableFuture.completedFuture(task.call());
                } catch (Exception e) {
                    return CompletableFuture.failedFuture(e);
                }
            }
        };
    }

    // ============ Configuration Methods ============

    /**
     * Registers a fixed response for a specific response type (ignores prompt).
     */
    public <R> StubMultiModelExecutor withResponse(Class<R> responseType, R fixedResponse) {
        responseProviders.put(responseType, (Function<String, R>) prompt -> fixedResponse);
        return this;
    }

    /**
     * Registers a response provider for a specific response type.
     * The provider receives the prompt and returns the response.
     */
    public <R> StubMultiModelExecutor withResponseProvider(Class<R> responseType, Function<String, R> provider) {
        responseProviders.put(responseType, provider);
        return this;
    }

    /**
     * Registers an embedding provider for single text embedding.
     */
    public StubMultiModelExecutor withEmbedding(Function<String, float[]> provider) {
        this.embeddingProvider = provider;
        return this;
    }

    /**
     * Registers an embeddings provider for multiple texts.
     */
    public StubMultiModelExecutor withEmbeddings(Function<List<String>, List<float[]>> provider) {
        this.embeddingsProvider = provider;
        return this;
    }

    /**
     * Simulates an error for a specific model.
     */
    public StubMultiModelExecutor withModelError(String modelId, Exception error) {
        modelErrors.put(modelId, error);
        return this;
    }

    // ============ Overridden Methods ============

    @Override
    public List<String> getModelIds() {
        return modelIds;
    }

    @Override
    public List<String> getEmbeddingModelIds() {
        return embeddingModelIds;
    }

    @Override
    public <R> CompletableFuture<ModelResult<R>> executeLlmOnModelAsync(
            String modelId, String prompt, Class<R> responseType) {
        return CompletableFuture.supplyAsync(() -> {
            // Check for simulated error
            if (modelErrors.containsKey(modelId)) {
                return ModelResult.failure(modelId, Duration.ZERO, prompt, modelErrors.get(modelId));
            }

            // Get response from provider
            @SuppressWarnings("unchecked")
            Function<String, R> provider = (Function<String, R>) responseProviders.get(responseType);
            if (provider == null) {
                throw new IllegalStateException(
                        "No response provider configured for type: " + responseType.getSimpleName());
            }

            R response = provider.apply(prompt);
            return ModelResult.success(modelId, response, Duration.ofMillis(100), prompt);
        });
    }

    @Override
    public CompletableFuture<ModelResult<float[]>> executeEmbeddingOnModelAsync(String modelId, String text) {
        return CompletableFuture.supplyAsync(() -> {
            if (modelErrors.containsKey(modelId)) {
                return ModelResult.failure(modelId, Duration.ZERO, text, modelErrors.get(modelId));
            }

            if (embeddingProvider == null) {
                throw new IllegalStateException("No embedding provider configured");
            }

            float[] embedding = embeddingProvider.apply(text);
            return ModelResult.success(modelId, embedding, Duration.ofMillis(50), text);
        });
    }

    @Override
    public CompletableFuture<ModelResult<List<float[]>>> executeEmbeddingsOnModelAsync(
            String modelId, List<String> texts) {
        return CompletableFuture.supplyAsync(() -> {
            if (modelErrors.containsKey(modelId)) {
                return ModelResult.failure(modelId, Duration.ZERO, String.join(", ", texts), modelErrors.get(modelId));
            }

            if (embeddingsProvider != null) {
                List<float[]> embeddings = embeddingsProvider.apply(texts);
                return ModelResult.success(modelId, embeddings, Duration.ofMillis(50), String.join(", ", texts));
            }

            if (embeddingProvider != null) {
                List<float[]> embeddings = new ArrayList<>();
                for (String text : texts) {
                    embeddings.add(embeddingProvider.apply(text));
                }
                return ModelResult.success(modelId, embeddings, Duration.ofMillis(50), String.join(", ", texts));
            }

            throw new IllegalStateException("No embedding provider configured");
        });
    }
}
