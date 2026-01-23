package ai.qa.solutions.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.qa.solutions.chatclient.ChatClientStore;
import ai.qa.solutions.embedding.EmbeddingModelStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@DisplayName("MultiProviderAutoConfiguration Tests")
class MultiProviderAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MultiProviderAutoConfiguration.class));

    @Nested
    @DisplayName("Conditional Activation")
    class ConditionalActivation {

        @Test
        @DisplayName("Should not create beans when property is disabled")
        void shouldNotCreateBeansWhenDisabled() {
            contextRunner
                    .withPropertyValues("spring.ai.ragas.metrics.enabled=false")
                    .withUserConfiguration(MockConfig.class)
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(ChatClientStore.class);
                        assertThat(context).doesNotHaveBean(EmbeddingModelStore.class);
                    });
        }

        @Test
        @DisplayName("Should create beans when property is enabled")
        void shouldCreateBeansWhenEnabled() {
            contextRunner
                    .withPropertyValues("spring.ai.ragas.metrics.enabled=true")
                    .withUserConfiguration(MockConfig.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(ChatClientStore.class);
                        assertThat(context).hasSingleBean(EmbeddingModelStore.class);
                    });
        }

        @Test
        @DisplayName("Should create beans by default (matchIfMissing=true)")
        void shouldCreateBeansByDefault() {
            contextRunner.withUserConfiguration(MockConfig.class).run(context -> {
                assertThat(context).hasSingleBean(ChatClientStore.class);
                assertThat(context).hasSingleBean(EmbeddingModelStore.class);
            });
        }
    }

    @Nested
    @DisplayName("ChatClientStore Creation")
    class ChatClientStoreCreation {

        @Test
        @DisplayName("Should create ChatClientStore with empty providers")
        void shouldCreateWithEmptyProviders() {
            contextRunner
                    .withPropertyValues("spring.ai.ragas.providers.auto-detect-beans=false")
                    .withUserConfiguration(MockConfig.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(ChatClientStore.class);
                        final ChatClientStore store = context.getBean(ChatClientStore.class);
                        assertThat(store.getModelIds()).isEmpty();
                    });
        }

        @Test
        @DisplayName("Should work with SimpleLoggerAdvisor when available")
        void shouldWorkWithLoggerAdvisor() {
            contextRunner
                    .withUserConfiguration(MockConfig.class, LoggerAdvisorConfig.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(ChatClientStore.class);
                        assertThat(context).hasSingleBean(SimpleLoggerAdvisor.class);
                    });
        }
    }

    @Nested
    @DisplayName("EmbeddingModelStore Creation")
    class EmbeddingModelStoreCreation {

        @Test
        @DisplayName("Should create EmbeddingModelStore with empty providers")
        void shouldCreateWithEmptyProviders() {
            contextRunner
                    .withPropertyValues("spring.ai.ragas.providers.auto-detect-beans=false")
                    .withUserConfiguration(MockConfig.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(EmbeddingModelStore.class);
                        final EmbeddingModelStore store = context.getBean(EmbeddingModelStore.class);
                        assertThat(store.getModelIds()).isEmpty();
                    });
        }
    }

    @Nested
    @DisplayName("ConditionalOnMissingBean")
    class ConditionalOnMissingBeanTests {

        @Test
        @DisplayName("Should not override existing ChatClientStore bean")
        void shouldNotOverrideChatClientStoreBean() {
            contextRunner
                    .withUserConfiguration(MockConfig.class, CustomChatClientStoreConfig.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(ChatClientStore.class);
                        final ChatClientStore store = context.getBean(ChatClientStore.class);
                        assertThat(store.getModelIds()).containsExactly("custom-existing");
                    });
        }

        @Test
        @DisplayName("Should not override existing EmbeddingModelStore bean")
        void shouldNotOverrideEmbeddingModelStoreBean() {
            contextRunner
                    .withUserConfiguration(MockConfig.class, CustomEmbeddingModelStoreConfig.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(EmbeddingModelStore.class);
                        final EmbeddingModelStore store = context.getBean(EmbeddingModelStore.class);
                        assertThat(store.getModelIds()).containsExactly("custom-embedding");
                    });
        }
    }

    @Configuration
    static class MockConfig {
        @Bean
        ChatClient.Builder chatClientBuilder() {
            final ChatClient mockClient = mock(ChatClient.class);
            final ChatClient.Builder builder = mock(ChatClient.Builder.class);
            when(builder.build()).thenReturn(mockClient);
            when(builder.clone()).thenReturn(builder);
            when(builder.defaultAdvisors(org.mockito.ArgumentMatchers.any(SimpleLoggerAdvisor.class)))
                    .thenReturn(builder);
            when(builder.defaultOptions(org.mockito.ArgumentMatchers.any())).thenReturn(builder);
            return builder;
        }

        @Bean
        EmbeddingModel embeddingModel() {
            return mock(EmbeddingModel.class);
        }
    }

    @Configuration
    static class LoggerAdvisorConfig {
        @Bean
        SimpleLoggerAdvisor simpleLoggerAdvisor() {
            return new SimpleLoggerAdvisor();
        }
    }

    @Configuration
    static class CustomChatClientStoreConfig {
        @Bean
        ChatClientStore chatClientStore() {
            final ChatClient mockClient = mock(ChatClient.class);
            return new ChatClientStore(java.util.Map.of("custom-existing", mockClient), mockClient);
        }
    }

    @Configuration
    static class CustomEmbeddingModelStoreConfig {
        @Bean
        EmbeddingModelStore embeddingModelStore() {
            final EmbeddingModel mockModel = mock(EmbeddingModel.class);
            return new EmbeddingModelStore(java.util.Map.of("custom-embedding", mockModel), mockModel);
        }
    }
}
