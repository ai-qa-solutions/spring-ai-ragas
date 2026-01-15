package ai.qa.solutions.config;

import ai.qa.solutions.chatclient.ChatClientStore;
import ai.qa.solutions.config.detector.ExternalChatModelDetector;
import ai.qa.solutions.config.factory.OpenAiCompatibleModelFactory;
import ai.qa.solutions.embedding.EmbeddingModelStore;
import ai.qa.solutions.properties.MultiProviderProperties;
import ai.qa.solutions.properties.MultiProviderProperties.DefaultOptions;
import ai.qa.solutions.properties.MultiProviderProperties.EmbeddingModelConfig;
import ai.qa.solutions.properties.MultiProviderProperties.ModelConfig;
import ai.qa.solutions.properties.MultiProviderProperties.OpenAiCompatibleProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Autoconfiguration for multi-provider ChatClientStore and EmbeddingModelStore.
 * <p>
 * Supports three layers of providers that can work together or separately:
 * <ul>
 *   <li><b>Layer 1:</b> External ChatModel beans (GigaChat, Anthropic, etc.)</li>
 *   <li><b>Layer 2:</b> OpenAI-compatible endpoints via mutate() (Groq, cloud.ru, Azure, OpenRouter)</li>
 *   <li><b>Layer 3:</b> Default OpenAI models (backward compatible)</li>
 * </ul>
 *
 * <p>Configuration prefix: {@code spring.ai.ragas.providers}</p>
 *
 * @see MultiProviderProperties
 * @see ChatClientStore
 * @see EmbeddingModelStore
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(ChatClient.class)
@ConditionalOnProperty(
        prefix = "spring.ai.ragas.metrics",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@EnableConfigurationProperties(MultiProviderProperties.class)
public class MultiProviderAutoConfiguration {

    private final ExternalChatModelDetector externalDetector = new ExternalChatModelDetector();
    private final OpenAiCompatibleModelFactory openAiCompatibleFactory = new OpenAiCompatibleModelFactory();

    /**
     * Creates a ChatClientStore with models from all configured providers.
     */
    @Bean
    @ConditionalOnMissingBean
    public ChatClientStore chatClientStore(
            final ObjectProvider<Map<String, ChatModel>> chatModelsProvider,
            final ObjectProvider<OpenAiApi> openAiApiProvider,
            final ObjectProvider<OpenAiChatModel> openAiChatModelProvider,
            final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
            final MultiProviderProperties properties,
            @Autowired(required = false) final SimpleLoggerAdvisor simpleLoggerAdvisor) {

        final Map<String, List<ChatClient>> allClients = new LinkedHashMap<>();

        // Layer 1: External ChatModel beans (GigaChat, Anthropic, etc.)
        if (properties.isAutoDetectBeans()) {
            final Map<String, ChatModel> chatModels = chatModelsProvider.getIfAvailable();
            if (chatModels != null && !chatModels.isEmpty()) {
                log.info("Layer 1: Detecting external ChatModel beans...");
                final Map<String, List<ChatClient>> externalClients = externalDetector.detect(chatModels);
                mergeClients(allClients, externalClients);
            }
        }

        // Layer 2: OpenAI-compatible endpoints via mutate()
        final OpenAiApi openAiApi = openAiApiProvider.getIfAvailable();
        final OpenAiChatModel openAiChatModel = openAiChatModelProvider.getIfAvailable();
        if (openAiApi != null
                && openAiChatModel != null
                && !properties.getOpenaiCompatible().isEmpty()) {
            log.info("Layer 2: Creating OpenAI-compatible chat models via mutate()...");
            final Map<String, List<ChatClient>> openAiCompatibleClients = openAiCompatibleFactory.createModels(
                    openAiApi, openAiChatModel, properties.getOpenaiCompatible(), properties.getDefaultOptions());
            mergeClients(allClients, openAiCompatibleClients);
        }

        // Layer 3: Default OpenAI models (backward compatible)
        final ChatClient.Builder chatClientBuilder = chatClientBuilderProvider.getIfAvailable();
        if (chatClientBuilder != null
                && properties.getDefaultProvider().isEnabled()
                && !properties.getDefaultProvider().getModels().isEmpty()) {
            log.info("Layer 3: Creating default OpenAI chat models...");
            final Map<String, List<ChatClient>> defaultClients = createDefaultChatModels(
                    chatClientBuilder, properties.getDefaultProvider().getModels(), properties.getDefaultOptions());
            mergeClients(allClients, defaultClients);
        }

        // Resolve default client
        final ChatClient defaultClient = resolveDefaultClient(allClients, chatClientBuilder, simpleLoggerAdvisor);

        log.info(
                "ChatClientStore initialized with {} model IDs ({} total clients)",
                allClients.size(),
                countTotalClients(allClients));

        return new ChatClientStore(allClients, defaultClient, true);
    }

    /**
     * Creates an EmbeddingModelStore with models from all configured providers.
     */
    @Bean
    @ConditionalOnMissingBean
    public EmbeddingModelStore embeddingModelStore(
            final ObjectProvider<OpenAiApi> openAiApiProvider,
            final ObjectProvider<EmbeddingModel> defaultEmbeddingModelProvider,
            final MultiProviderProperties properties) {

        final Map<String, EmbeddingModel> allModels = new HashMap<>();
        final OpenAiApi openAiApi = openAiApiProvider.getIfAvailable();
        final EmbeddingModel defaultEmbeddingModel = defaultEmbeddingModelProvider.getIfAvailable();

        // Create embedding models from OpenAI-compatible providers
        if (openAiApi != null) {
            for (final OpenAiCompatibleProvider provider : properties.getOpenaiCompatible()) {
                if (provider.getBaseUrl() == null || provider.getApiKey() == null) {
                    continue;
                }

                if (provider.getEmbeddingModels() == null
                        || provider.getEmbeddingModels().isEmpty()) {
                    continue;
                }

                log.info(
                        "Creating EmbeddingModels for provider '{}' with {} models",
                        provider.getName(),
                        provider.getEmbeddingModels().size());

                final OpenAiApi providerApi = openAiApi
                        .mutate()
                        .baseUrl(provider.getBaseUrl())
                        .apiKey(provider.getApiKey())
                        .build();

                for (final EmbeddingModelConfig modelConfig : provider.getEmbeddingModels()) {
                    try {
                        final Integer dimensions = modelConfig.getDimensions() != null
                                ? modelConfig.getDimensions()
                                : properties.getEmbeddingDefaultOptions().getDimensions();

                        final OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                                .model(modelConfig.getId())
                                .dimensions(dimensions)
                                .build();

                        final OpenAiEmbeddingModel providerModel =
                                new OpenAiEmbeddingModel(providerApi, MetadataMode.EMBED, options);

                        allModels.put(modelConfig.getId(), providerModel);
                        log.debug(
                                "Created EmbeddingModel for model '{}' from provider '{}'",
                                modelConfig.getId(),
                                provider.getName());
                    } catch (final Exception e) {
                        log.error(
                                "Failed to create EmbeddingModel for model '{}': {}",
                                modelConfig.getId(),
                                e.getMessage());
                    }
                }
            }
        }

        log.info("EmbeddingModelStore initialized with {} models", allModels.size());
        return new EmbeddingModelStore(allModels, defaultEmbeddingModel);
    }

    /**
     * Creates ChatClients for default OpenAI models using ChatClient.Builder.
     */
    private Map<String, List<ChatClient>> createDefaultChatModels(
            final ChatClient.Builder builder, final List<ModelConfig> models, final DefaultOptions defaultOptions) {

        final Map<String, List<ChatClient>> result = new LinkedHashMap<>();

        for (final ModelConfig modelConfig : models) {
            final ChatClient client = builder.clone()
                    .defaultAdvisors(new SimpleLoggerAdvisor())
                    .defaultOptions(ChatOptions.builder()
                            .model(modelConfig.getId())
                            .temperature(resolveTemperature(modelConfig, defaultOptions))
                            .maxTokens(resolveMaxTokens(modelConfig, defaultOptions))
                            .topP(resolveTopP(modelConfig, defaultOptions))
                            .build())
                    .build();

            result.computeIfAbsent(modelConfig.getId(), k -> new ArrayList<>()).add(client);
            log.debug("Created default ChatClient for model: {}", modelConfig.getId());
        }

        return result;
    }

    /**
     * Resolves the default client for the store.
     */
    private ChatClient resolveDefaultClient(
            final Map<String, List<ChatClient>> allClients,
            final ChatClient.Builder chatClientBuilder,
            final SimpleLoggerAdvisor simpleLoggerAdvisor) {

        if (!allClients.isEmpty()) {
            final List<ChatClient> firstClients = allClients.values().iterator().next();
            if (!firstClients.isEmpty()) {
                return firstClients.get(0);
            }
        }

        if (chatClientBuilder != null) {
            final ChatClient.Builder defaultBuilder = simpleLoggerAdvisor != null
                    ? chatClientBuilder.defaultAdvisors(simpleLoggerAdvisor)
                    : chatClientBuilder;
            return defaultBuilder.build();
        }

        log.warn("No default ChatClient available - store will be incomplete");
        return null;
    }

    /**
     * Merges source clients into target map, supporting multiple clients per model ID.
     */
    private void mergeClients(final Map<String, List<ChatClient>> target, final Map<String, List<ChatClient>> source) {
        for (final Map.Entry<String, List<ChatClient>> entry : source.entrySet()) {
            target.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).addAll(entry.getValue());
        }
    }

    private Double resolveTemperature(final ModelConfig modelConfig, final DefaultOptions defaultOptions) {
        if (modelConfig.getOptions() != null && modelConfig.getOptions().getTemperature() != null) {
            return modelConfig.getOptions().getTemperature();
        }
        return defaultOptions.getTemperature();
    }

    private Integer resolveMaxTokens(final ModelConfig modelConfig, final DefaultOptions defaultOptions) {
        if (modelConfig.getOptions() != null && modelConfig.getOptions().getMaxTokens() != null) {
            return modelConfig.getOptions().getMaxTokens();
        }
        return defaultOptions.getMaxTokens();
    }

    private Double resolveTopP(final ModelConfig modelConfig, final DefaultOptions defaultOptions) {
        if (modelConfig.getOptions() != null && modelConfig.getOptions().getTopP() != null) {
            return modelConfig.getOptions().getTopP();
        }
        return defaultOptions.getTopP();
    }

    private int countTotalClients(final Map<String, List<ChatClient>> clients) {
        return clients.values().stream().mapToInt(List::size).sum();
    }
}
