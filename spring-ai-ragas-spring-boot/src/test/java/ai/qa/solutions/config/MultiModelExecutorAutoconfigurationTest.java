package ai.qa.solutions.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import ai.qa.solutions.chatclient.ChatClientStore;
import ai.qa.solutions.embedding.EmbeddingModelStore;
import ai.qa.solutions.execution.MultiModelExecutor;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;

@DisplayName("MultiModelExecutorAutoconfiguration Tests")
class MultiModelExecutorAutoconfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MultiModelExecutorAutoconfiguration.class));

    @Nested
    @DisplayName("Conditional Activation")
    class ConditionalActivation {

        @Test
        @DisplayName("Should not create bean when property is disabled")
        void shouldNotCreateBeanWhenDisabled() {
            contextRunner
                    .withPropertyValues("spring.ai.ragas.metrics.enabled=false")
                    .withUserConfiguration(FullDependenciesConfig.class)
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(MultiModelExecutor.class);
                    });
        }

        @Test
        @DisplayName("Should create bean when property is enabled")
        void shouldCreateBeanWhenEnabled() {
            contextRunner
                    .withPropertyValues("spring.ai.ragas.metrics.enabled=true")
                    .withUserConfiguration(FullDependenciesConfig.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(MultiModelExecutor.class);
                    });
        }

        @Test
        @DisplayName("Should create bean by default (matchIfMissing=true)")
        void shouldCreateBeanByDefault() {
            contextRunner.withUserConfiguration(FullDependenciesConfig.class).run(context -> {
                assertThat(context).hasSingleBean(MultiModelExecutor.class);
            });
        }
    }

    @Nested
    @DisplayName("MultiModelExecutor Creation")
    class MultiModelExecutorCreation {

        @Test
        @DisplayName("Should create executor with all dependencies")
        void shouldCreateWithAllDependencies() {
            contextRunner.withUserConfiguration(FullDependenciesConfig.class).run(context -> {
                assertThat(context).hasSingleBean(MultiModelExecutor.class);
                MultiModelExecutor executor = context.getBean(MultiModelExecutor.class);
                assertThat(executor).isNotNull();
                assertThat(executor.getModelIds()).containsExactlyInAnyOrder("model-1", "model-2");
                assertThat(executor.getEmbeddingModelIds()).containsExactlyInAnyOrder("embed-1");
            });
        }

        @Test
        @DisplayName("Should create executor without EmbeddingModelStore (optional)")
        void shouldCreateWithoutEmbeddingModelStore() {
            contextRunner.withUserConfiguration(NullEmbeddingStoreConfig.class).run(context -> {
                assertThat(context).hasSingleBean(MultiModelExecutor.class);
                MultiModelExecutor executor = context.getBean(MultiModelExecutor.class);
                assertThat(executor).isNotNull();
                assertThat(executor.getEmbeddingModelIds()).isEmpty();
            });
        }

        @Test
        @DisplayName("Should create dedicated ragasMetricExecutor and ragasHttpExecutor")
        void shouldCreateRagasExecutors() {
            contextRunner.withUserConfiguration(FullDependenciesConfig.class).run(context -> {
                assertThat(context).hasSingleBean(MultiModelExecutor.class);
                assertThat(context).hasBean("ragasMetricExecutor");
                assertThat(context).hasBean("ragasHttpExecutor");
                AsyncTaskExecutor metricExecutor = context.getBean("ragasMetricExecutor", AsyncTaskExecutor.class);
                AsyncTaskExecutor httpExecutor = context.getBean("ragasHttpExecutor", AsyncTaskExecutor.class);
                assertThat(metricExecutor).isNotNull();
                assertThat(httpExecutor).isNotNull();
                assertThat(metricExecutor).isNotSameAs(httpExecutor);
            });
        }
    }

    @Nested
    @DisplayName("Bean Dependencies")
    class BeanDependencies {

        @Test
        @DisplayName("Should fail without ChatClientStore")
        void shouldFailWithoutChatClientStore() {
            contextRunner.withUserConfiguration(NoChatClientStoreConfig.class).run(context -> {
                assertThat(context).hasFailed();
            });
        }

        @Test
        @DisplayName("Should create executor even without external AsyncTaskExecutor (creates own)")
        void shouldCreateWithoutExternalTaskExecutor() {
            contextRunner.withUserConfiguration(NoTaskExecutorConfig.class).run(context -> {
                // Autoconfiguration creates its own ragasMetricExecutor and ragasHttpExecutor
                assertThat(context).hasSingleBean(MultiModelExecutor.class);
                assertThat(context).hasBean("ragasMetricExecutor");
                assertThat(context).hasBean("ragasHttpExecutor");
            });
        }
    }

    @Configuration
    static class FullDependenciesConfig {
        @Bean
        ChatClientStore chatClientStore() {
            ChatClient client1 = mock(ChatClient.class);
            ChatClient client2 = mock(ChatClient.class);
            return new ChatClientStore(Map.of("model-1", client1, "model-2", client2), client1);
        }

        @Bean
        EmbeddingModelStore embeddingModelStore() {
            EmbeddingModel model = mock(EmbeddingModel.class);
            return new EmbeddingModelStore(Map.of("embed-1", model), model);
        }
    }

    @Configuration
    static class NullEmbeddingStoreConfig {
        @Bean
        ChatClientStore chatClientStore() {
            ChatClient client = mock(ChatClient.class);
            return new ChatClientStore(Map.of("model-1", client), client);
        }
    }

    @Configuration
    static class NoChatClientStoreConfig {
        @Bean
        EmbeddingModelStore embeddingModelStore() {
            EmbeddingModel model = mock(EmbeddingModel.class);
            return new EmbeddingModelStore(Map.of("embed-1", model), model);
        }
    }

    @Configuration
    static class NoTaskExecutorConfig {
        @Bean
        ChatClientStore chatClientStore() {
            ChatClient client = mock(ChatClient.class);
            return new ChatClientStore(Map.of("model-1", client), client);
        }

        @Bean
        EmbeddingModelStore embeddingModelStore() {
            EmbeddingModel model = mock(EmbeddingModel.class);
            return new EmbeddingModelStore(Map.of("embed-1", model), model);
        }
    }
}
