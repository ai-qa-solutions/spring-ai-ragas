package ai.qa.solutions.config;

import ai.qa.solutions.embedding.EmbeddingModelFactory;
import ai.qa.solutions.embedding.EmbeddingModelStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Autoconfiguration for managing multiple embedding models.
 * <p>
 * Creates an {@link EmbeddingModelStore} that contains a set of pre-configured
 * {@link EmbeddingModel} instances for different vectorization models, as well as a default model.
 *
 * @see EmbeddingModelStore
 * @see EmbeddingModelFactory
 * @see EmbeddingModelProperties
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(EmbeddingModel.class)
@ConditionalOnProperty(
        prefix = "spring.ai.ragas.metrics",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@EnableConfigurationProperties(EmbeddingModelAutoConfiguration.EmbeddingModelProperties.class)
public class EmbeddingModelAutoConfiguration {

    /**
     * Creates an EmbeddingModel store with pre-configured models.
     * <p>
     * For each model in {@code properties.list}, a separate {@link EmbeddingModel} is created
     * via {@link EmbeddingModelFactory} with individual options (dimensions).
     * Errors creating individual models are logged but do not interrupt initialization.
     *
     * @param defaultEmbeddingModel default model configured by Spring AI
     * @param embeddingModelFactory factory for creating models with custom options
     * @param properties model configuration from application.properties/yml
     * @return initialized EmbeddingModelStore
     */
    @Bean
    @ConditionalOnMissingBean
    public EmbeddingModelStore embeddingModelStore(
            final EmbeddingModel defaultEmbeddingModel,
            final EmbeddingModelFactory embeddingModelFactory,
            final EmbeddingModelProperties properties) {

        log.info(
                "Initializing EmbeddingModelStore with {} models",
                properties.getList().size());

        final Map<String, EmbeddingModel> models = new HashMap<>();
        for (final EmbeddingModelProperties.ModelConfig modelConfig : properties.getList()) {
            try {
                final EmbeddingModelProperties.ModelOptions options =
                        modelConfig.getOptions() != null ? modelConfig.getOptions() : properties.getDefaultOptions();
                final EmbeddingModel model = embeddingModelFactory.create(
                        defaultEmbeddingModel, modelConfig.getId(), options.getDimensions());
                models.put(modelConfig.getId(), model);
                log.debug("Created EmbeddingModel for model: {}", modelConfig.getId());
            } catch (final Exception e) {
                log.error("Failed to create EmbeddingModel for model: {}", modelConfig.getId(), e);
            }
        }

        return new EmbeddingModelStore(models, defaultEmbeddingModel);
    }

    /**
     * Creates a factory for constructing EmbeddingModel with custom options.
     *
     * @return new instance of {@link EmbeddingModelFactory}
     */
    @Bean
    @ConditionalOnMissingBean
    public EmbeddingModelFactory embeddingModelFactory() {
        return new EmbeddingModelFactory();
    }

    /**
     * Properties for configuring multiple embedding models.
     * <p>
     * Bound to the {@code spring.ai.embedding-models} prefix in configuration files.
     * Allows defining a list of models with individual options or shared
     * default settings.
     */
    @Data
    @ConfigurationProperties(prefix = "spring.ai.embedding-models")
    public static class EmbeddingModelProperties {

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
         * Configuration for an individual embedding model.
         */
        @Data
        public static class ModelConfig {
            /**
             * Unique model identifier (e.g., "text-embedding-3-small", "text-embedding-ada-002").
             * Required field. Used as a key in {@link EmbeddingModelStore}.
             */
            private String id;

            /**
             * Individual options for this model.
             * If not specified, {@code defaultOptions} from the parent configuration are used.
             */
            private ModelOptions options;
        }

        /**
         * Options for configuring embedding model behavior.
         */
        @Data
        public static class ModelOptions {
            /**
             * Dimensionality of the vector representation (number of dimensions).
             * Defaults to {@code 1024}.
             */
            private Integer dimensions = 1024;
        }
    }
}
