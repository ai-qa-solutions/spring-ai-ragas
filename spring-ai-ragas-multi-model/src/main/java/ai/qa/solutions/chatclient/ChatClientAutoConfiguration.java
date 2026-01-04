package ai.qa.solutions.chatclient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Autoconfiguration for managing multiple chat models.
 * <p>
 * Creates a {@link ChatClientStore} that contains a set of pre-configured
 * {@link ChatClient} instances for different models, as well as a default client.
 *
 * @see ChatClientStore
 * @see ChatModelProperties
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(ChatClient.class)
@EnableConfigurationProperties(ChatClientAutoConfiguration.ChatModelProperties.class)
public class ChatClientAutoConfiguration {

    /**
     * Creates a ChatClient store with pre-configured clients for all models.
     * <p>
     * For each model in {@code properties.list}, a separate {@link ChatClient} is created
     * with individual options (temperature, maxTokens, topP). Also creates a default client
     * based on the Spring AI base builder.
     *
     * @param chatClientBuilder base ChatClient builder configured by Spring AI
     * @param properties model configuration from application.properties/yml
     * @return initialized ChatClientStore
     */
    @Bean
    @ConditionalOnMissingBean
    public ChatClientStore chatClientStore(
            final ChatClient.Builder chatClientBuilder, final ChatModelProperties properties) {

        log.info(
                "Initializing ChatClientStore with {} models",
                properties.getList().size());

        final ChatClient defaultClient =
                chatClientBuilder.defaultAdvisors(new SimpleLoggerAdvisor()).build();
        log.debug("Created default ChatClient from existing builder configuration");

        final Map<String, ChatClient> clients = new HashMap<>();
        for (final ChatModelProperties.ModelConfig modelConfig : properties.getList()) {
            final ChatClient client = createClient(chatClientBuilder, modelConfig, properties);
            clients.put(modelConfig.getId(), client);
            log.debug("Created ChatClient for model: {}", modelConfig.getId());
        }

        return new ChatClientStore(clients, defaultClient);
    }

    /**
     * Creates a separate ChatClient for a specific model with its options.
     * <p>
     * If individual options are not specified for the model, {@code defaultOptions}
     * from the general configuration are used.
     * <p>
     * IMPORTANT: The builder is cloned before configuration to ensure each ChatClient
     * has its own independent configuration. Without cloning, all clients would share
     * the same mutable builder state, causing all models to use the last configured options.
     *
     * @param builder base ChatClient builder
     * @param modelConfig specific model configuration
     * @param properties general configuration for all models
     * @return configured ChatClient for the model
     */
    private ChatClient createClient(
            final ChatClient.Builder builder,
            final ChatModelProperties.ModelConfig modelConfig,
            final ChatModelProperties properties) {

        final ChatModelProperties.ModelOptions options =
                modelConfig.getOptions() != null ? modelConfig.getOptions() : properties.getDefaultOptions();

        // Clone the builder to avoid sharing mutable state between different ChatClients
        return builder.clone()
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultOptions(ChatOptions.builder()
                        .model(modelConfig.getId())
                        .temperature(options.getTemperature())
                        .maxTokens(options.getMaxTokens())
                        .topP(options.getTopP())
                        .build())
                .build();
    }

    /**
     * Properties for configuring multiple chat models.
     * <p>
     * Bound to the {@code spring.ai.chat-models} prefix in configuration files.
     * Allows defining a list of models with individual options or shared
     * default settings.
     */
    @Data
    @ConfigurationProperties(prefix = "spring.ai.chat-models")
    public static class ChatModelProperties {

        /**
         * Default options applied to all models in the list
         * if individual options are not specified for a model.
         */
        private ModelOptions defaultOptions = new ModelOptions();

        /**
         * List of model configurations. Each model has a unique ID
         * and can have its own options or use defaultOptions.
         */
        private List<ModelConfig> list = new ArrayList<>();

        /**
         * Configuration for an individual chat model.
         */
        @Data
        public static class ModelConfig {
            /**
             * Unique model identifier (e.g., "gpt-4", "claude-3-opus").
             * Required field. Used as a key in {@link ChatClientStore}.
             */
            private String id;

            /**
             * Individual options for this model.
             * If not specified, {@code defaultOptions} from the parent configuration are used.
             */
            private ModelOptions options;
        }

        /**
         * Options for configuring chat model behavior.
         */
        @Data
        public static class ModelOptions {
            /**
             * Generation temperature (0.0 - deterministic, higher - more creative).
             * Defaults to {@code 0.0}.
             */
            private Double temperature = 0.0;

            /**
             * Maximum number of tokens in the model response.
             * Defaults to {@code 1000}.
             */
            private Integer maxTokens = 1000;

            /**
             * Nucleus sampling parameter - probability threshold for token selection.
             * Defaults to {@code 1.0} (all tokens are considered).
             */
            private Double topP = 1.0;
        }
    }
}
