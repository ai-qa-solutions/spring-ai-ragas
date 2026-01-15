package ai.qa.solutions.config.detector;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;

/**
 * Detector for external ChatModel beans from third-party starters.
 * <p>
 * Automatically detects ChatModel beans in the Spring context that are NOT
 * from the standard OpenAI starter. This includes:
 * <ul>
 *   <li>GigaChat (chat.giga.springai:spring-ai-starter-model-gigachat)</li>
 *   <li>Anthropic (spring-ai-starter-model-anthropic)</li>
 *   <li>Google Vertex AI / Gemini</li>
 *   <li>Any other ChatModel implementation</li>
 * </ul>
 *
 * <p>Each detected ChatModel is wrapped in a ChatClient and registered
 * in the ChatClientStore using the model's default name.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * ExternalChatModelDetector detector = new ExternalChatModelDetector();
 * Map<String, List<ChatClient>> clients = detector.detect(allChatModelBeans);
 * // Returns: {"GigaChat" -> [ChatClient], "claude-3-opus" -> [ChatClient]}
 * }</pre>
 */
@Slf4j
public class ExternalChatModelDetector {

    /**
     * Detects external ChatModel beans and wraps them in ChatClients.
     * <p>
     * OpenAiChatModel beans are skipped as they are handled separately
     * via the mutate() pattern in {@link ai.qa.solutions.config.factory.OpenAiCompatibleModelFactory}.
     *
     * @param allChatModels map of bean name to ChatModel from Spring context
     * @return map of model ID to list of ChatClients
     */
    public Map<String, List<ChatClient>> detect(final Map<String, ChatModel> allChatModels) {
        final Map<String, List<ChatClient>> result = new LinkedHashMap<>();

        if (allChatModels == null || allChatModels.isEmpty()) {
            log.debug("No ChatModel beans found in context");
            return result;
        }

        for (final Map.Entry<String, ChatModel> entry : allChatModels.entrySet()) {
            final String beanName = entry.getKey();
            final ChatModel chatModel = entry.getValue();

            if (shouldSkipModel(chatModel)) {
                log.debug("Skipping OpenAI ChatModel bean: {}", beanName);
                continue;
            }

            final String modelId = resolveModelId(beanName, chatModel);
            final ChatClient client = ChatClient.builder(chatModel).build();

            result.computeIfAbsent(modelId, k -> new ArrayList<>()).add(client);

            log.info(
                    "Detected external ChatModel: {} (type: {}, modelId: {})",
                    beanName,
                    chatModel.getClass().getSimpleName(),
                    modelId);
        }

        log.info("Detected {} external ChatModel beans", result.size());
        return result;
    }

    /**
     * Checks if the model should be skipped (e.g., OpenAI models).
     *
     * @param chatModel the chat model to check
     * @return true if should be skipped
     */
    private boolean shouldSkipModel(final ChatModel chatModel) {
        return chatModel instanceof OpenAiChatModel;
    }

    /**
     * Resolves the model ID from bean name or ChatModel properties.
     * <p>
     * Resolution order:
     * <ol>
     *   <li>Extract from ChatModel's default options if available</li>
     *   <li>Derive from class name (e.g., GigaChatModel -> GigaChat)</li>
     *   <li>Use bean name as fallback</li>
     * </ol>
     *
     * @param beanName  Spring bean name
     * @param chatModel the chat model instance
     * @return resolved model ID
     */
    private String resolveModelId(final String beanName, final ChatModel chatModel) {
        final String modelFromOptions = extractModelFromOptions(chatModel);
        if (modelFromOptions != null && !modelFromOptions.isEmpty()) {
            return modelFromOptions;
        }

        final String className = chatModel.getClass().getSimpleName();
        if (className.endsWith("ChatModel")) {
            final String derived = className.replace("ChatModel", "");
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
     * Attempts to extract model ID from ChatModel's default options.
     *
     * @param chatModel the chat model
     * @return model ID or null if not available
     */
    private String extractModelFromOptions(final ChatModel chatModel) {
        try {
            final var options = chatModel.getDefaultOptions();
            if (options != null && options.getModel() != null) {
                return options.getModel();
            }
        } catch (Exception e) {
            log.debug("Could not extract model from options: {}", e.getMessage());
        }
        return null;
    }
}
