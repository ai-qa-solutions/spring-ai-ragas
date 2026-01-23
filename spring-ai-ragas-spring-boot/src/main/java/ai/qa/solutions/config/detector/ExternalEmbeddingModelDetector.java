package ai.qa.solutions.config.detector;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;

/**
 * Detector for external EmbeddingModel beans from third-party starters.
 * <p>
 * Automatically detects EmbeddingModel beans in the Spring context that are NOT
 * from the standard OpenAI starter. This includes:
 * <ul>
 *   <li>GigaChat (chat.giga.springai:spring-ai-starter-model-gigachat)</li>
 *   <li>Vertex AI / Gemini</li>
 *   <li>Mistral AI</li>
 *   <li>Ollama</li>
 *   <li>Any other EmbeddingModel implementation</li>
 * </ul>
 *
 * <p>Each detected EmbeddingModel is registered in the EmbeddingModelStore
 * using the model's default name or class-derived name.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * ExternalEmbeddingModelDetector detector = new ExternalEmbeddingModelDetector();
 * Map<String, EmbeddingModel> models = detector.detect(allEmbeddingModelBeans);
 * // Returns: {"Embeddings" -> GigaChatEmbeddingModel, "nomic-embed-text" -> OllamaEmbeddingModel}
 * }</pre>
 */
@Slf4j
public class ExternalEmbeddingModelDetector {

    /**
     * Detects external EmbeddingModel beans.
     * <p>
     * OpenAiEmbeddingModel beans are skipped as they are handled separately
     * via the mutate() pattern in the OpenAI-compatible provider configuration.
     *
     * @param allEmbeddingModels map of bean name to EmbeddingModel from Spring context
     * @return map of model ID to EmbeddingModel
     */
    public Map<String, EmbeddingModel> detect(final Map<String, EmbeddingModel> allEmbeddingModels) {
        final Map<String, EmbeddingModel> result = new LinkedHashMap<>();

        if (allEmbeddingModels == null || allEmbeddingModels.isEmpty()) {
            log.debug("No EmbeddingModel beans found in context");
            return result;
        }

        for (final Map.Entry<String, EmbeddingModel> entry : allEmbeddingModels.entrySet()) {
            final String beanName = entry.getKey();
            final EmbeddingModel embeddingModel = entry.getValue();

            if (shouldSkipModel(embeddingModel)) {
                log.debug("Skipping OpenAI EmbeddingModel bean: {}", beanName);
                continue;
            }

            final String modelId = resolveModelId(beanName, embeddingModel);

            if (result.containsKey(modelId)) {
                log.warn(
                        "Duplicate model ID '{}' detected. Bean '{}' will override previous model.", modelId, beanName);
            }

            result.put(modelId, embeddingModel);

            log.info(
                    "Detected external EmbeddingModel: {} (type: {}, modelId: {})",
                    beanName,
                    embeddingModel.getClass().getSimpleName(),
                    modelId);
        }

        log.info("Detected {} external EmbeddingModel beans", result.size());
        return result;
    }

    /**
     * Checks if the model should be skipped (e.g., OpenAI models).
     *
     * @param embeddingModel the embedding model to check
     * @return true if should be skipped
     */
    private boolean shouldSkipModel(final EmbeddingModel embeddingModel) {
        return embeddingModel instanceof OpenAiEmbeddingModel;
    }

    /**
     * Resolves the model ID from bean name or EmbeddingModel properties.
     * <p>
     * Resolution order:
     * <ol>
     *   <li>Extract from EmbeddingModel's options if available</li>
     *   <li>Derive from class name (e.g., GigaChatEmbeddingModel -> GigaChat)</li>
     *   <li>Use bean name as fallback</li>
     * </ol>
     *
     * @param beanName       Spring bean name
     * @param embeddingModel the embedding model instance
     * @return resolved model ID
     */
    private String resolveModelId(final String beanName, final EmbeddingModel embeddingModel) {
        final String modelFromOptions = extractModelFromOptions(embeddingModel);
        if (modelFromOptions != null && !modelFromOptions.isEmpty()) {
            return modelFromOptions;
        }

        final String className = embeddingModel.getClass().getSimpleName();
        if (className.endsWith("EmbeddingModel")) {
            final String derived = className.replace("EmbeddingModel", "");
            if (!derived.isEmpty()) {
                return derived;
            }
        }
        if (className.endsWith("Model")) {
            final String derived = className.replace("Model", "");
            if (!derived.isEmpty()) {
                return derived;
            }
        }

        return beanName;
    }

    /**
     * Attempts to extract model ID from EmbeddingModel's options.
     * <p>
     * Uses reflection to call getOptions() method if available.
     *
     * @param embeddingModel the embedding model
     * @return model ID or null if not available
     */
    private String extractModelFromOptions(final EmbeddingModel embeddingModel) {
        try {
            final var method = embeddingModel.getClass().getMethod("getOptions");
            final var options = method.invoke(embeddingModel);
            if (options != null) {
                final var getModelMethod = options.getClass().getMethod("getModel");
                final var model = getModelMethod.invoke(options);
                if (model != null) {
                    return model.toString();
                }
            }
        } catch (final Exception e) {
            log.debug("Could not extract model from options: {}", e.getMessage());
        }
        return null;
    }
}
