package ai.qa.solutions.config.detector;

import ai.qa.solutions.properties.MultiProviderProperties.ExternalStarterConfig;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
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
 * <p>Supports explicit model configuration via {@link ExternalStarterConfig}:</p>
 * <pre>{@code
 * spring:
 *   ai:
 *     ragas:
 *       providers:
 *         external-starters:
 *           gigachat:
 *             chat-models:
 *               - GigaChat-2-Max
 *               - GigaChat-2-Pro
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
     * @param allChatModels    map of bean name to ChatModel from Spring context
     * @param externalStarters configuration for external starters (may be null)
     * @return map of model ID to list of ChatClients
     */
    public Map<String, List<ChatClient>> detect(
            final Map<String, ChatModel> allChatModels, final Map<String, ExternalStarterConfig> externalStarters) {
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

            final String starterName = resolveStarterName(chatModel);
            final ExternalStarterConfig config = findConfig(starterName, externalStarters);

            if (config != null && !config.isEnabled()) {
                log.debug("Skipping disabled external starter: {}", starterName);
                continue;
            }

            if (config != null && !config.getChatModels().isEmpty()) {
                // Explicit model list configured - create ChatClient for each model
                for (final String modelId : config.getChatModels()) {
                    final ChatClient client = ChatClient.builder(chatModel)
                            .defaultOptions(ChatOptions.builder().model(modelId).build())
                            .build();
                    result.computeIfAbsent(modelId, k -> new ArrayList<>()).add(client);

                    log.info(
                            "Registered external ChatModel: {} -> {} (type: {})",
                            starterName,
                            modelId,
                            chatModel.getClass().getSimpleName());
                }
            } else {
                // Auto-detect model ID from bean
                final String modelId = resolveModelId(beanName, chatModel);
                final ChatClient client = ChatClient.builder(chatModel).build();
                result.computeIfAbsent(modelId, k -> new ArrayList<>()).add(client);

                log.info(
                        "Detected external ChatModel: {} (type: {}, modelId: {})",
                        beanName,
                        chatModel.getClass().getSimpleName(),
                        modelId);
            }
        }

        log.info("Detected {} external ChatModel entries", result.size());
        return result;
    }

    /**
     * Backward compatible method without external starters config.
     *
     * @param allChatModels map of bean name to ChatModel from Spring context
     * @return map of model ID to list of ChatClients
     */
    public Map<String, List<ChatClient>> detect(final Map<String, ChatModel> allChatModels) {
        return detect(allChatModels, null);
    }

    /**
     * Resolves starter name from ChatModel class.
     *
     * @param chatModel the chat model
     * @return starter name (e.g., "gigachat", "anthropic")
     */
    private String resolveStarterName(final ChatModel chatModel) {
        final String className = chatModel.getClass().getSimpleName().toLowerCase();
        if (className.contains("gigachat")) {
            return "gigachat";
        }
        if (className.contains("anthropic") || className.contains("claude")) {
            return "anthropic";
        }
        if (className.contains("ollama")) {
            return "ollama";
        }
        if (className.contains("vertex") || className.contains("gemini")) {
            return "vertex";
        }
        return className.replace("chatmodel", "").replace("model", "");
    }

    /**
     * Finds configuration for starter by name (case-insensitive).
     */
    private ExternalStarterConfig findConfig(
            final String starterName, final Map<String, ExternalStarterConfig> externalStarters) {
        if (externalStarters == null || externalStarters.isEmpty()) {
            return null;
        }
        // Try exact match first
        if (externalStarters.containsKey(starterName)) {
            return externalStarters.get(starterName);
        }
        // Try case-insensitive match
        for (final Map.Entry<String, ExternalStarterConfig> entry : externalStarters.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(starterName)) {
                return entry.getValue();
            }
        }
        return null;
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
