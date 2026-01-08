package ai.qa.solutions.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.qa.solutions.embedding.EmbeddingModelFactory;
import ai.qa.solutions.embedding.EmbeddingModelStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@DisplayName("EmbeddingModelAutoConfiguration Tests")
class EmbeddingModelAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EmbeddingModelAutoConfiguration.class));

    @Nested
    @DisplayName("Conditional Activation")
    class ConditionalActivation {

        @Test
        @DisplayName("Should not create beans when property is disabled")
        void shouldNotCreateBeansWhenDisabled() {
            contextRunner
                    .withPropertyValues("spring.ai.ragas.metrics.enabled=false")
                    .withUserConfiguration(MinimalEmbeddingModelConfig.class)
                    .run(context -> {
                        // Autoconfigured beans should not be created
                        assertThat(context).doesNotHaveBean(EmbeddingModelStore.class);
                        assertThat(context).doesNotHaveBean(EmbeddingModelFactory.class);
                    });
        }

        @Test
        @DisplayName("Should create beans when property is enabled")
        void shouldCreateBeansWhenEnabled() {
            contextRunner
                    .withPropertyValues("spring.ai.ragas.metrics.enabled=true")
                    .withUserConfiguration(MockEmbeddingModelConfig.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(EmbeddingModelStore.class);
                        assertThat(context).hasSingleBean(EmbeddingModelFactory.class);
                    });
        }

        @Test
        @DisplayName("Should create beans by default (matchIfMissing=true)")
        void shouldCreateBeansByDefault() {
            contextRunner.withUserConfiguration(MockEmbeddingModelConfig.class).run(context -> {
                assertThat(context).hasSingleBean(EmbeddingModelStore.class);
                assertThat(context).hasSingleBean(EmbeddingModelFactory.class);
            });
        }
    }

    @Nested
    @DisplayName("EmbeddingModelStore Creation")
    class EmbeddingModelStoreCreation {

        @Test
        @DisplayName("Should create EmbeddingModelStore with empty model list")
        void shouldCreateWithEmptyModelList() {
            contextRunner.withUserConfiguration(MockEmbeddingModelConfig.class).run(context -> {
                assertThat(context).hasSingleBean(EmbeddingModelStore.class);
                EmbeddingModelStore store = context.getBean(EmbeddingModelStore.class);
                assertThat(store.getModelIds()).isEmpty();
            });
        }

        @Test
        @DisplayName("Should create EmbeddingModelStore with configured models")
        void shouldCreateWithConfiguredModels() {
            contextRunner
                    .withUserConfiguration(MockEmbeddingModelConfig.class)
                    .withPropertyValues(
                            "spring.ai.embedding-models.list[0].id=text-embedding-3-small",
                            "spring.ai.embedding-models.list[1].id=text-embedding-3-large")
                    .run(context -> {
                        assertThat(context).hasSingleBean(EmbeddingModelStore.class);
                        EmbeddingModelStore store = context.getBean(EmbeddingModelStore.class);
                        assertThat(store.getModelIds())
                                .containsExactlyInAnyOrder("text-embedding-3-small", "text-embedding-3-large");
                    });
        }

        @Test
        @DisplayName("Should use default dimensions when model options not specified")
        void shouldUseDefaultDimensions() {
            contextRunner
                    .withUserConfiguration(MockEmbeddingModelConfig.class)
                    .withPropertyValues(
                            "spring.ai.embedding-models.default-options.dimensions=2048",
                            "spring.ai.embedding-models.list[0].id=test-embedding")
                    .run(context -> {
                        assertThat(context).hasSingleBean(EmbeddingModelStore.class);
                        EmbeddingModelStore store = context.getBean(EmbeddingModelStore.class);
                        assertThat(store.getModelIds()).contains("test-embedding");
                    });
        }

        @Test
        @DisplayName("Should use model-specific dimensions when specified")
        void shouldUseModelSpecificDimensions() {
            contextRunner
                    .withUserConfiguration(MockEmbeddingModelConfig.class)
                    .withPropertyValues(
                            "spring.ai.embedding-models.default-options.dimensions=1024",
                            "spring.ai.embedding-models.list[0].id=custom-embedding",
                            "spring.ai.embedding-models.list[0].options.dimensions=3072")
                    .run(context -> {
                        assertThat(context).hasSingleBean(EmbeddingModelStore.class);
                        EmbeddingModelStore store = context.getBean(EmbeddingModelStore.class);
                        assertThat(store.getModelIds()).contains("custom-embedding");
                    });
        }

        @Test
        @DisplayName("Should handle factory errors gracefully")
        void shouldHandleFactoryErrorsGracefully() {
            contextRunner
                    .withUserConfiguration(FailingEmbeddingModelConfig.class)
                    .withPropertyValues(
                            "spring.ai.embedding-models.list[0].id=failing-model",
                            "spring.ai.embedding-models.list[1].id=working-model")
                    .run(context -> {
                        assertThat(context).hasSingleBean(EmbeddingModelStore.class);
                        EmbeddingModelStore store = context.getBean(EmbeddingModelStore.class);
                        // Only working-model should be in the store
                        assertThat(store.getModelIds()).containsExactly("working-model");
                    });
        }
    }

    @Nested
    @DisplayName("EmbeddingModelFactory Bean")
    class EmbeddingModelFactoryBean {

        @Test
        @DisplayName("Should create EmbeddingModelFactory bean")
        void shouldCreateFactoryBean() {
            contextRunner.withUserConfiguration(MockEmbeddingModelConfig.class).run(context -> {
                assertThat(context).hasSingleBean(EmbeddingModelFactory.class);
            });
        }
    }

    @Nested
    @DisplayName("ConditionalOnMissingBean")
    class ConditionalOnMissingBeanTests {

        @Test
        @DisplayName("Should not override existing EmbeddingModelStore bean")
        void shouldNotOverrideExistingStoreBean() {
            contextRunner
                    .withUserConfiguration(MockEmbeddingModelConfig.class, CustomEmbeddingModelStoreConfig.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(EmbeddingModelStore.class);
                        EmbeddingModelStore store = context.getBean(EmbeddingModelStore.class);
                        assertThat(store.getModelIds()).containsExactly("custom-existing");
                    });
        }

        @Test
        @DisplayName("Should not override existing EmbeddingModelFactory bean")
        void shouldNotOverrideExistingFactoryBean() {
            contextRunner
                    .withUserConfiguration(MinimalEmbeddingModelConfig.class, CustomEmbeddingModelFactoryConfig.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(EmbeddingModelFactory.class);
                        EmbeddingModelFactory factory = context.getBean(EmbeddingModelFactory.class);
                        // Custom factory should be used
                        assertThat(factory).isNotNull();
                    });
        }
    }

    @Nested
    @DisplayName("EmbeddingModelProperties")
    class EmbeddingModelPropertiesTests {

        @Test
        @DisplayName("Should have correct default values")
        void shouldHaveDefaultValues() {
            EmbeddingModelAutoConfiguration.EmbeddingModelProperties properties =
                    new EmbeddingModelAutoConfiguration.EmbeddingModelProperties();

            assertThat(properties.getList()).isEmpty();
            assertThat(properties.getDefaultOptions()).isNotNull();
            assertThat(properties.getDefaultOptions().getDimensions()).isEqualTo(1024);
        }

        @Test
        @DisplayName("ModelConfig should store id and options")
        void modelConfigShouldStoreIdAndOptions() {
            EmbeddingModelAutoConfiguration.EmbeddingModelProperties.ModelConfig config =
                    new EmbeddingModelAutoConfiguration.EmbeddingModelProperties.ModelConfig();
            config.setId("test-embedding-id");

            EmbeddingModelAutoConfiguration.EmbeddingModelProperties.ModelOptions options =
                    new EmbeddingModelAutoConfiguration.EmbeddingModelProperties.ModelOptions();
            options.setDimensions(3072);
            config.setOptions(options);

            assertThat(config.getId()).isEqualTo("test-embedding-id");
            assertThat(config.getOptions().getDimensions()).isEqualTo(3072);
        }
    }

    @Configuration
    static class MockEmbeddingModelConfig {
        @Bean
        EmbeddingModel embeddingModel() {
            return mock(EmbeddingModel.class);
        }

        @Bean
        EmbeddingModelFactory embeddingModelFactory() {
            EmbeddingModelFactory factory = mock(EmbeddingModelFactory.class);
            when(factory.create(any(EmbeddingModel.class), anyString(), anyInt()))
                    .thenAnswer(invocation -> mock(EmbeddingModel.class));
            return factory;
        }
    }

    @Configuration
    static class FailingEmbeddingModelConfig {
        @Bean
        EmbeddingModel embeddingModel() {
            return mock(EmbeddingModel.class);
        }

        @Bean
        EmbeddingModelFactory embeddingModelFactory() {
            EmbeddingModelFactory factory = mock(EmbeddingModelFactory.class);
            when(factory.create(any(EmbeddingModel.class), anyString(), anyInt()))
                    .thenAnswer(invocation -> {
                        String modelId = invocation.getArgument(1);
                        if ("failing-model".equals(modelId)) {
                            throw new RuntimeException("Factory error for failing-model");
                        }
                        return mock(EmbeddingModel.class);
                    });
            return factory;
        }
    }

    @Configuration
    static class CustomEmbeddingModelStoreConfig {
        @Bean
        EmbeddingModelStore embeddingModelStore() {
            EmbeddingModel mockModel = mock(EmbeddingModel.class);
            return new EmbeddingModelStore(java.util.Map.of("custom-existing", mockModel), mockModel);
        }
    }

    @Configuration
    static class MinimalEmbeddingModelConfig {
        @Bean
        EmbeddingModel embeddingModel() {
            return mock(EmbeddingModel.class);
        }
    }

    @Configuration
    static class CustomEmbeddingModelFactoryConfig {
        @Bean
        EmbeddingModelFactory embeddingModelFactory() {
            return new EmbeddingModelFactory();
        }
    }
}
