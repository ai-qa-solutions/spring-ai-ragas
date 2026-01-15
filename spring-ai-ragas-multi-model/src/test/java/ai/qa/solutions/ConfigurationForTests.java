package ai.qa.solutions;

import ai.qa.solutions.chatclient.ChatClientStore;
import ai.qa.solutions.embedding.EmbeddingModelStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties({ConfigurationForTests.MultiProviderProperties.class})
public class ConfigurationForTests {

    @Bean
    public ChatClientStore chatClientStore(
            final ObjectProvider<OpenAiApi> openAiApiProvider,
            final ObjectProvider<OpenAiChatModel> openAiChatModelProvider,
            final ChatClient.Builder chatClientBuilder,
            final MultiProviderProperties properties) {

        final Map<String, List<ChatClient>> allClients = new LinkedHashMap<>();
        final OpenAiApi openAiApi = openAiApiProvider.getIfAvailable();
        final OpenAiChatModel openAiChatModel = openAiChatModelProvider.getIfAvailable();

        if (openAiApi != null
                && openAiChatModel != null
                && !properties.getOpenaiCompatible().isEmpty()) {
            for (final OpenAiCompatibleProvider provider : properties.getOpenaiCompatible()) {
                if (provider.getBaseUrl() == null || provider.getApiKey() == null) {
                    log.warn("Skipping provider '{}': missing baseUrl or apiKey", provider.getName());
                    continue;
                }

                log.info(
                        "Creating ChatClients for provider '{}' with {} models",
                        provider.getName(),
                        provider.getChatModels().size());

                final OpenAiApi providerApi = openAiApi
                        .mutate()
                        .baseUrl(provider.getBaseUrl())
                        .apiKey(provider.getApiKey())
                        .build();

                for (final ModelConfig modelConfig : provider.getChatModels()) {
                    final OpenAiChatOptions options = OpenAiChatOptions.builder()
                            .model(modelConfig.getId())
                            .temperature(resolveTemperature(modelConfig, properties.getDefaultOptions()))
                            .maxTokens(resolveMaxTokens(modelConfig, properties.getDefaultOptions()))
                            .topP(resolveTopP(modelConfig, properties.getDefaultOptions()))
                            .build();

                    final OpenAiChatModel providerModel = openAiChatModel
                            .mutate()
                            .openAiApi(providerApi)
                            .defaultOptions(options)
                            .build();

                    final ChatClient client = ChatClient.builder(providerModel)
                            .defaultAdvisors(new SimpleLoggerAdvisor())
                            .build();

                    allClients
                            .computeIfAbsent(modelConfig.getId(), k -> new ArrayList<>())
                            .add(client);
                    log.debug(
                            "Created ChatClient for model '{}' from provider '{}'",
                            modelConfig.getId(),
                            provider.getName());
                }
            }
        } else if (!properties.getOpenaiCompatible().isEmpty()) {
            log.info("OpenAiApi/OpenAiChatModel not available, using ChatClient.Builder fallback");
            for (final OpenAiCompatibleProvider provider : properties.getOpenaiCompatible()) {
                for (final ModelConfig modelConfig : provider.getChatModels()) {
                    final ChatClient client = chatClientBuilder
                            .clone()
                            .defaultAdvisors(new SimpleLoggerAdvisor())
                            .defaultOptions(ChatOptions.builder()
                                    .model(modelConfig.getId())
                                    .temperature(resolveTemperature(modelConfig, properties.getDefaultOptions()))
                                    .maxTokens(resolveMaxTokens(modelConfig, properties.getDefaultOptions()))
                                    .topP(resolveTopP(modelConfig, properties.getDefaultOptions()))
                                    .build())
                            .build();

                    allClients
                            .computeIfAbsent(modelConfig.getId(), k -> new ArrayList<>())
                            .add(client);
                    log.debug("Created ChatClient for model '{}' via builder", modelConfig.getId());
                }
            }
        }

        ChatClient defaultClient;
        if (!allClients.isEmpty()) {
            defaultClient = allClients.values().iterator().next().get(0);
        } else {
            defaultClient =
                    chatClientBuilder.defaultAdvisors(new SimpleLoggerAdvisor()).build();
        }

        log.info(
                "ChatClientStore initialized with {} model IDs ({} total clients)",
                allClients.size(),
                allClients.values().stream().mapToInt(List::size).sum());

        return new ChatClientStore(allClients, defaultClient, true);
    }

    @Bean
    public EmbeddingModelStore embeddingModelStore(
            final ObjectProvider<OpenAiApi> openAiApiProvider,
            final EmbeddingModel defaultEmbeddingModel,
            final MultiProviderProperties properties) {

        final Map<String, EmbeddingModel> models = new HashMap<>();
        final OpenAiApi openAiApi = openAiApiProvider.getIfAvailable();

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

                        models.put(modelConfig.getId(), providerModel);
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

        log.info("EmbeddingModelStore initialized with {} models", models.size());
        return new EmbeddingModelStore(models, defaultEmbeddingModel);
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

    // Properties classes

    @Data
    @ConfigurationProperties(prefix = "spring.ai.ragas.providers")
    public static class MultiProviderProperties {
        private boolean autoDetectBeans = true;
        private List<OpenAiCompatibleProvider> openaiCompatible = new ArrayList<>();
        private DefaultProvider defaultProvider = new DefaultProvider();
        private DefaultOptions defaultOptions = new DefaultOptions();
        private EmbeddingDefaultOptions embeddingDefaultOptions = new EmbeddingDefaultOptions();
    }

    @Data
    public static class OpenAiCompatibleProvider {
        private String name;
        private String baseUrl;
        private String apiKey;
        private List<ModelConfig> chatModels = new ArrayList<>();
        private List<EmbeddingModelConfig> embeddingModels = new ArrayList<>();
    }

    @Data
    public static class DefaultProvider {
        private boolean enabled = true;
        private List<ModelConfig> models = new ArrayList<>();
    }

    @Data
    public static class ModelConfig {
        private String id;
        private ModelOptions options;
    }

    @Data
    public static class ModelOptions {
        private Double temperature;
        private Integer maxTokens;
        private Double topP;
    }

    @Data
    public static class DefaultOptions {
        private Double temperature = 0.0;
        private Integer maxTokens = 1000;
        private Double topP = 1.0;
    }

    @Data
    public static class EmbeddingModelConfig {
        private String id;
        private Integer dimensions;
    }

    @Data
    public static class EmbeddingDefaultOptions {
        private Integer dimensions = 1024;
    }
}
