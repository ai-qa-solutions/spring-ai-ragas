package ai.qa.solutions.properties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for multi-provider ChatModel and EmbeddingModel support.
 * <p>
 * Supports three layers of providers:
 * <ul>
 *   <li>Layer 1: Auto-detected external ChatModel beans (GigaChat, Anthropic, etc.)</li>
 *   <li>Layer 2: OpenAI-compatible endpoints via mutate() (Groq, cloud.ru, Azure, OpenRouter)</li>
 *   <li>Layer 3: Default OpenAI models (backward compatible)</li>
 * </ul>
 *
 * <p>Example YAML configuration:</p>
 * <pre>{@code
 * spring:
 *   ai:
 *     ragas:
 *       providers:
 *         auto-detect-beans: true
 *         openai-compatible:
 *           - name: openrouter-premium
 *             base-url: https://openrouter.ai/api
 *             api-key: ${OPENROUTER_API_KEY}
 *             chat-models:
 *               - { id: anthropic/claude-3.5-sonnet }
 *               - { id: openai/gpt-4o }
 *             embedding-models:
 *               - { id: openai/text-embedding-3-large, dimensions: 3072 }
 *           - name: openrouter-efficient
 *             base-url: https://openrouter.ai/api
 *             api-key: ${OPENROUTER_API_KEY}
 *             chat-models:
 *               - { id: openai/gpt-4o-mini }
 *             embedding-models:
 *               - { id: openai/text-embedding-3-small, dimensions: 1536 }
 *         default-provider:
 *           enabled: false
 *           models: []
 *         default-options:
 *           temperature: 0.0
 *           max-tokens: 1000
 *         embedding-default-options:
 *           dimensions: 1024
 * }</pre>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "spring.ai.ragas.providers")
public class MultiProviderProperties {

    /**
     * Whether to auto-detect external ChatModel beans (GigaChat, Anthropic, etc.).
     * When enabled, any ChatModel bean in the context will be wrapped in a ChatClient
     * and added to the ChatClientStore.
     */
    private boolean autoDetectBeans = true;

    /**
     * OpenAI-compatible API endpoints configuration.
     * Use this to configure providers like Groq, cloud.ru Evolution, Azure OpenAI,
     * OpenRouter, or any other OpenAI-compatible API.
     */
    private List<OpenAiCompatibleProvider> openaiCompatible = new ArrayList<>();

    /**
     * Default OpenAI provider configuration.
     * This provides backward compatibility with existing configurations.
     */
    private DefaultProvider defaultProvider = new DefaultProvider();

    /**
     * Global default options applied to all chat models when individual options are not specified.
     */
    private DefaultOptions defaultOptions = new DefaultOptions();

    /**
     * Global default options applied to all embedding models when individual options are not specified.
     */
    private EmbeddingDefaultOptions embeddingDefaultOptions = new EmbeddingDefaultOptions();

    /**
     * Configuration for external Spring AI starters (GigaChat, Anthropic, Ollama, etc.).
     * Allows overriding model IDs, enabling/disabling specific starters.
     * Key is the starter name (e.g., "gigachat", "anthropic", "ollama").
     */
    private Map<String, ExternalStarterConfig> externalStarters = new HashMap<>();

    /**
     * Configuration for an external Spring AI starter.
     */
    @Getter
    @Setter
    public static class ExternalStarterConfig {

        /**
         * Whether this starter is enabled. Default is true.
         */
        private boolean enabled = true;

        /**
         * List of chat model IDs to register from this starter.
         * Each model ID will create a separate ChatClient entry.
         * If empty, the model ID will be auto-detected from the ChatModel bean.
         */
        private List<String> chatModels = new ArrayList<>();

        /**
         * List of embedding model IDs to register from this starter.
         * If empty, the model ID will be auto-detected from the EmbeddingModel bean.
         */
        private List<String> embeddingModels = new ArrayList<>();

        /**
         * Whether to include the embedding model from this starter.
         * Default is true.
         */
        private boolean includeEmbedding = true;
    }

    /**
     * Configuration for an OpenAI-compatible API provider.
     */
    @Getter
    @Setter
    public static class OpenAiCompatibleProvider {

        /**
         * Provider name for identification (e.g., "groq", "cloudru", "azure", "openrouter").
         */
        private String name;

        /**
         * Base URL of the OpenAI-compatible API.
         * Examples: "https://api.groq.com/openai", "https://openrouter.ai/api"
         */
        private String baseUrl;

        /**
         * API key for authentication with this provider.
         */
        private String apiKey;

        /**
         * List of chat models available through this provider.
         */
        private List<ModelConfig> chatModels = new ArrayList<>();

        /**
         * List of embedding models available through this provider.
         */
        private List<EmbeddingModelConfig> embeddingModels = new ArrayList<>();
    }

    /**
     * Configuration for the default OpenAI provider.
     * Uses standard Spring AI OpenAI configuration (spring.ai.openai.*).
     */
    @Getter
    @Setter
    public static class DefaultProvider {

        /**
         * Whether to enable default OpenAI provider.
         * When enabled, uses ChatClient.Builder from Spring AI autoconfiguration.
         */
        private boolean enabled = true;

        /**
         * List of OpenAI models to configure.
         */
        private List<ModelConfig> models = new ArrayList<>();
    }

    /**
     * Configuration for a single chat model.
     */
    @Getter
    @Setter
    public static class ModelConfig {

        /**
         * Model identifier (e.g., "gpt-4", "llama3-70b-8192", "anthropic/claude-3.5-sonnet").
         */
        private String id;

        /**
         * Optional model-specific options that override default options.
         */
        private ModelOptions options;
    }

    /**
     * Configuration for a single embedding model.
     */
    @Getter
    @Setter
    public static class EmbeddingModelConfig {

        /**
         * Model identifier (e.g., "openai/text-embedding-3-large").
         */
        private String id;

        /**
         * Vector dimensions for this embedding model.
         */
        private Integer dimensions;
    }

    /**
     * Model-specific options for chat models.
     */
    @Getter
    @Setter
    public static class ModelOptions {

        /**
         * Generation temperature (0.0 - deterministic, higher - more creative).
         * If null, uses value from defaultOptions.
         */
        private Double temperature;

        /**
         * Maximum number of tokens in the model response.
         * If null, uses value from defaultOptions.
         */
        private Integer maxTokens;

        /**
         * Nucleus sampling parameter - probability threshold for token selection.
         * If null, uses value from defaultOptions.
         */
        private Double topP;
    }

    /**
     * Default options applied to all chat models when individual options are not specified.
     */
    @Getter
    @Setter
    public static class DefaultOptions {

        /**
         * Default generation temperature.
         */
        private Double temperature = 0.0;

        /**
         * Default maximum number of tokens.
         */
        private Integer maxTokens = 1000;

        /**
         * Default nucleus sampling parameter.
         */
        private Double topP = 1.0;
    }

    /**
     * Default options applied to all embedding models when individual options are not specified.
     */
    @Getter
    @Setter
    public static class EmbeddingDefaultOptions {

        /**
         * Default vector dimensions.
         */
        private Integer dimensions = 1024;
    }
}
