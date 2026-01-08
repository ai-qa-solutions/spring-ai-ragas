package ai.qa.solutions.embedding;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;

/**
 * Store for pre-configured {@link EmbeddingModel} instances for different models.
 * <p>
 * Provides thread-safe access to vectorization models by ID, as well as
 * to the default model. Used for managing multiple embedding models
 * in Spring AI applications.
 *
 * @see EmbeddingModelFactory
 */
@Slf4j
public class EmbeddingModelStore {

    /**
     * Thread-safe map of models indexed by model ID.
     */
    private final Map<String, EmbeddingModel> models = new ConcurrentHashMap<>();

    /**
     * Default EmbeddingModel configured through standard Spring AI auto-configuration.
     */
    private final EmbeddingModel defaultModel;

    /**
     * Creates a new store with the given models and default model.
     *
     * @param models map of models where key is model ID, value is EmbeddingModel
     * @param defaultModel default model to use when model is not specified
     */
    public EmbeddingModelStore(final Map<String, EmbeddingModel> models, final EmbeddingModel defaultModel) {
        this.models.putAll(models);
        this.defaultModel = defaultModel;
        log.info("EmbeddingModelStore initialized with {} models + default", models.size());
    }

    /**
     * Gets EmbeddingModel for the specified model.
     *
     * @param modelId unique model identifier
     * @return configured EmbeddingModel for this model
     * @throws IllegalArgumentException if model with this ID is not found
     */
    public EmbeddingModel get(final String modelId) {
        final EmbeddingModel model = models.get(modelId);
        if (model == null) {
            throw new IllegalArgumentException(
                    "Embedding model not found: " + modelId + ". Available: " + models.keySet());
        }
        return model;
    }

    /**
     * Gets the default EmbeddingModel.
     * <p>
     * The default model is created from the standard Spring AI auto-configuration
     * and is not linked to a specific model from the list.
     *
     * @return default EmbeddingModel
     */
    public EmbeddingModel getDefault() {
        return defaultModel;
    }

    /**
     * Gets an immutable list of all registered EmbeddingModel instances.
     * <p>
     * The default model is not included in the result.
     *
     * @return list of all EmbeddingModel instances from configuration
     */
    public List<EmbeddingModel> getAll() {
        return List.copyOf(models.values());
    }

    /**
     * Gets an immutable list of identifiers for all registered models.
     *
     * @return list of model IDs
     */
    public List<String> getModelIds() {
        return List.copyOf(models.keySet());
    }

    /**
     * Checks whether a model with the specified ID is registered.
     *
     * @param modelId model identifier to check
     * @return {@code true} if the model exists, {@code false} otherwise
     */
    public boolean contains(final String modelId) {
        return models.containsKey(modelId);
    }

    /**
     * Returns the number of registered models.
     * <p>
     * The default model is not counted.
     *
     * @return number of models
     */
    public int size() {
        return models.size();
    }
}
