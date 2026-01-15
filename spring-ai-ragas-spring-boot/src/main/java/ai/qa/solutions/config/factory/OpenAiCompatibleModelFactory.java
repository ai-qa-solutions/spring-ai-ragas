package ai.qa.solutions.config.factory;

import ai.qa.solutions.properties.MultiProviderProperties;
import ai.qa.solutions.properties.MultiProviderProperties.DefaultOptions;
import ai.qa.solutions.properties.MultiProviderProperties.ModelConfig;
import ai.qa.solutions.properties.MultiProviderProperties.OpenAiCompatibleProvider;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

/**
 * Factory for creating ChatClients from OpenAI-compatible API providers.
 * <p>
 * Uses the mutate() pattern to create new OpenAiApi and OpenAiChatModel instances
 * with different base URLs and API keys, enabling support for providers like:
 * <ul>
 *   <li>Groq (https://api.groq.com/openai)</li>
 *   <li>cloud.ru Evolution (https://api.cloud.ru/v1)</li>
 *   <li>Azure OpenAI</li>
 *   <li>Any other OpenAI-compatible API</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * OpenAiCompatibleModelFactory factory = new OpenAiCompatibleModelFactory();
 * Map<String, List<ChatClient>> clients = factory.createModels(
 *     baseApi, baseModel, providers, defaultOptions);
 * }</pre>
 */
@Slf4j
public class OpenAiCompatibleModelFactory {

    /**
     * Creates ChatClients for all configured OpenAI-compatible providers.
     *
     * @param baseApi        base OpenAiApi to mutate
     * @param baseModel      base OpenAiChatModel to mutate
     * @param providers      list of OpenAI-compatible provider configurations
     * @param defaultOptions default options for models
     * @return map of model ID to list of ChatClients (supports multiple providers per model)
     */
    public Map<String, List<ChatClient>> createModels(
            final OpenAiApi baseApi,
            final OpenAiChatModel baseModel,
            final List<OpenAiCompatibleProvider> providers,
            final DefaultOptions defaultOptions) {

        final Map<String, List<ChatClient>> result = new LinkedHashMap<>();

        for (final OpenAiCompatibleProvider provider : providers) {
            if (provider.getBaseUrl() == null || provider.getApiKey() == null) {
                log.warn("Skipping OpenAI-compatible provider '{}': missing baseUrl or apiKey", provider.getName());
                continue;
            }

            log.info(
                    "Creating ChatClients for OpenAI-compatible provider '{}' with {} models",
                    provider.getName(),
                    provider.getChatModels().size());

            final OpenAiApi providerApi = createProviderApi(baseApi, provider);

            for (final ModelConfig modelConfig : provider.getChatModels()) {
                final ChatClient client = createClientForModel(baseModel, providerApi, modelConfig, defaultOptions);
                result.computeIfAbsent(modelConfig.getId(), k -> new ArrayList<>())
                        .add(client);

                log.debug(
                        "Created ChatClient for model '{}' from provider '{}'",
                        modelConfig.getId(),
                        provider.getName());
            }
        }

        return result;
    }

    /**
     * Creates a provider-specific OpenAiApi using the mutate() pattern.
     *
     * @param baseApi  base API to mutate
     * @param provider provider configuration
     * @return new OpenAiApi configured for this provider
     */
    private OpenAiApi createProviderApi(final OpenAiApi baseApi, final OpenAiCompatibleProvider provider) {
        return baseApi.mutate()
                .baseUrl(provider.getBaseUrl())
                .apiKey(provider.getApiKey())
                .build();
    }

    /**
     * Creates a ChatClient for a specific model using the mutate() pattern.
     *
     * @param baseModel      base model to mutate
     * @param providerApi    provider-specific API
     * @param modelConfig    model configuration
     * @param defaultOptions default options
     * @return configured ChatClient
     */
    private ChatClient createClientForModel(
            final OpenAiChatModel baseModel,
            final OpenAiApi providerApi,
            final ModelConfig modelConfig,
            final DefaultOptions defaultOptions) {

        final OpenAiChatOptions options = buildOptions(modelConfig, defaultOptions);

        final OpenAiChatModel providerModel = baseModel
                .mutate()
                .openAiApi(providerApi)
                .defaultOptions(options)
                .build();

        return ChatClient.builder(providerModel).build();
    }

    /**
     * Builds OpenAiChatOptions from model config and default options.
     *
     * @param modelConfig    model-specific configuration
     * @param defaultOptions default options
     * @return configured OpenAiChatOptions
     */
    private OpenAiChatOptions buildOptions(final ModelConfig modelConfig, final DefaultOptions defaultOptions) {
        final MultiProviderProperties.ModelOptions configOptions = modelConfig.getOptions();

        return OpenAiChatOptions.builder()
                .model(modelConfig.getId())
                .temperature(resolveTemperature(configOptions, defaultOptions))
                .maxTokens(resolveMaxTokens(configOptions, defaultOptions))
                .topP(resolveTopP(configOptions, defaultOptions))
                .build();
    }

    private Double resolveTemperature(
            final MultiProviderProperties.ModelOptions configOptions, final DefaultOptions defaultOptions) {
        if (configOptions != null && configOptions.getTemperature() != null) {
            return configOptions.getTemperature();
        }
        return defaultOptions.getTemperature();
    }

    private Integer resolveMaxTokens(
            final MultiProviderProperties.ModelOptions configOptions, final DefaultOptions defaultOptions) {
        if (configOptions != null && configOptions.getMaxTokens() != null) {
            return configOptions.getMaxTokens();
        }
        return defaultOptions.getMaxTokens();
    }

    private Double resolveTopP(
            final MultiProviderProperties.ModelOptions configOptions, final DefaultOptions defaultOptions) {
        if (configOptions != null && configOptions.getTopP() != null) {
            return configOptions.getTopP();
        }
        return defaultOptions.getTopP();
    }
}
