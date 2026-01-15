package ai.qa.solutions.embedding;

import java.util.ArrayList;
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
 * <p>
 * Supports multiple EmbeddingModels per model ID, enabling scenarios where
 * the same model is configured in different providers (e.g., text-embedding-3-large
 * in both OpenAI and Azure). When a model ID has multiple EmbeddingModels, all of them
 * can be accessed via {@link #getModels(String)}.
 *
 * @see EmbeddingModelFactory
 */
@Slf4j
public class EmbeddingModelStore {

    /**
     * Thread-safe map of models indexed by model ID.
     * Each model ID can have multiple EmbeddingModels (e.g., same model from different providers).
     */
    private final Map<String, List<EmbeddingModel>> models = new ConcurrentHashMap<>();

    /**
     * Default EmbeddingModel configured through standard Spring AI auto-configuration.
     */
    private final EmbeddingModel defaultModel;

    /**
     * Creates a new store with the given models and default model.
     * <p>
     * This constructor supports backward compatibility with single EmbeddingModel per model.
     *
     * @param models       map of models where key is model ID, value is EmbeddingModel
     * @param defaultModel default model to use when model is not specified
     */
    public EmbeddingModelStore(final Map<String, EmbeddingModel> models, final EmbeddingModel defaultModel) {
        for (final Map.Entry<String, EmbeddingModel> entry : models.entrySet()) {
            this.models.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(entry.getValue());
        }
        this.defaultModel = defaultModel;
        log.info("EmbeddingModelStore initialized with {} models + default", models.size());
    }

    /**
     * Creates a new store with multiple EmbeddingModels per model ID.
     *
     * @param modelsMulti   map of models where key is model ID, value is list of EmbeddingModels
     * @param defaultModel  default model to use when model is not specified
     * @param multiProvider flag to distinguish from single-model constructor
     */
    public EmbeddingModelStore(
            final Map<String, List<EmbeddingModel>> modelsMulti,
            final EmbeddingModel defaultModel,
            final boolean multiProvider) {
        for (final Map.Entry<String, List<EmbeddingModel>> entry : modelsMulti.entrySet()) {
            this.models.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        this.defaultModel = defaultModel;
        final int totalModels =
                modelsMulti.values().stream().mapToInt(List::size).sum();
        log.info(
                "EmbeddingModelStore initialized with {} model IDs ({} total models) + default",
                modelsMulti.size(),
                totalModels);
    }

    /**
     * Gets the first EmbeddingModel for the specified model.
     * <p>
     * This method maintains backward compatibility. If the model has multiple
     * EmbeddingModels (from different providers), only the first one is returned.
     * Use {@link #getModels(String)} to get all EmbeddingModels for a model.
     *
     * @param modelId unique model identifier
     * @return first configured EmbeddingModel for this model
     * @throws IllegalArgumentException if model with this ID is not found
     */
    public EmbeddingModel get(final String modelId) {
        final List<EmbeddingModel> modelList = models.get(modelId);
        if (modelList == null || modelList.isEmpty()) {
            throw new IllegalArgumentException(
                    "Embedding model not found: " + modelId + ". Available: " + models.keySet());
        }
        return modelList.get(0);
    }

    /**
     * Gets all EmbeddingModels for the specified model.
     * <p>
     * If the same model ID is configured in multiple providers, this method returns all of them.
     *
     * @param modelId unique model identifier
     * @return list of all EmbeddingModels for this model
     * @throws IllegalArgumentException if model with this ID is not found
     */
    public List<EmbeddingModel> getModels(final String modelId) {
        final List<EmbeddingModel> modelList = models.get(modelId);
        if (modelList == null || modelList.isEmpty()) {
            throw new IllegalArgumentException(
                    "Embedding model not found: " + modelId + ". Available: " + models.keySet());
        }
        return List.copyOf(modelList);
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
     * If models have multiple EmbeddingModels, all of them are included.
     *
     * @return list of all EmbeddingModel instances from configuration
     */
    public List<EmbeddingModel> getAll() {
        return models.values().stream().flatMap(List::stream).toList();
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
     * Returns the number of registered model IDs.
     * <p>
     * Note: This returns the count of unique model IDs, not the total number
     * of EmbeddingModels. Use {@link #getTotalModelCount()} for the total count.
     *
     * @return number of model IDs
     */
    public int size() {
        return models.size();
    }

    /**
     * Returns the total number of EmbeddingModels across all model IDs.
     *
     * @return total number of EmbeddingModels
     */
    public int getTotalModelCount() {
        return models.values().stream().mapToInt(List::size).sum();
    }

    /**
     * Returns the number of EmbeddingModels for a specific model ID.
     *
     * @param modelId model identifier
     * @return number of EmbeddingModels for this model, or 0 if not found
     */
    public int getModelCount(final String modelId) {
        final List<EmbeddingModel> modelList = models.get(modelId);
        return modelList != null ? modelList.size() : 0;
    }

    /**
     * Adds an EmbeddingModel for the specified model ID.
     * <p>
     * If the model ID already exists, the new EmbeddingModel is added to the list.
     * This enables configuring the same model from multiple providers.
     *
     * @param modelId model identifier
     * @param model   EmbeddingModel to add
     */
    public void addModel(final String modelId, final EmbeddingModel model) {
        models.computeIfAbsent(modelId, k -> new ArrayList<>()).add(model);
        log.debug("Added EmbeddingModel for model: {}, total for this model: {}", modelId, getModelCount(modelId));
    }
}
