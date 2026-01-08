package ai.qa.solutions.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.qa.solutions.chatclient.ChatClientStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@DisplayName("ChatClientStoreAutoConfiguration Tests")
class ChatClientStoreAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ChatClientStoreAutoConfiguration.class));

    @Nested
    @DisplayName("Conditional Activation")
    class ConditionalActivation {

        @Test
        @DisplayName("Should not create beans when property is disabled")
        void shouldNotCreateBeansWhenDisabled() {
            contextRunner
                    .withPropertyValues("spring.ai.ragas.metrics.enabled=false")
                    .withUserConfiguration(MockChatClientConfig.class)
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(ChatClientStore.class);
                    });
        }

        @Test
        @DisplayName("Should create beans when property is enabled")
        void shouldCreateBeansWhenEnabled() {
            contextRunner
                    .withPropertyValues("spring.ai.ragas.metrics.enabled=true")
                    .withUserConfiguration(MockChatClientConfig.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(ChatClientStore.class);
                    });
        }

        @Test
        @DisplayName("Should create beans by default (matchIfMissing=true)")
        void shouldCreateBeansByDefault() {
            contextRunner.withUserConfiguration(MockChatClientConfig.class).run(context -> {
                assertThat(context).hasSingleBean(ChatClientStore.class);
            });
        }
    }

    @Nested
    @DisplayName("ChatClientStore Creation")
    class ChatClientStoreCreation {

        @Test
        @DisplayName("Should create ChatClientStore with empty model list")
        void shouldCreateWithEmptyModelList() {
            contextRunner.withUserConfiguration(MockChatClientConfig.class).run(context -> {
                assertThat(context).hasSingleBean(ChatClientStore.class);
                ChatClientStore store = context.getBean(ChatClientStore.class);
                assertThat(store.getModelIds()).isEmpty();
            });
        }

        @Test
        @DisplayName("Should create ChatClientStore with configured models")
        void shouldCreateWithConfiguredModels() {
            contextRunner
                    .withUserConfiguration(MockChatClientConfig.class)
                    .withPropertyValues(
                            "spring.ai.chat-models.list[0].id=gpt-4", "spring.ai.chat-models.list[1].id=claude-3-opus")
                    .run(context -> {
                        assertThat(context).hasSingleBean(ChatClientStore.class);
                        ChatClientStore store = context.getBean(ChatClientStore.class);
                        assertThat(store.getModelIds()).containsExactlyInAnyOrder("gpt-4", "claude-3-opus");
                    });
        }

        @Test
        @DisplayName("Should use default options when model options not specified")
        void shouldUseDefaultOptions() {
            contextRunner
                    .withUserConfiguration(MockChatClientConfig.class)
                    .withPropertyValues(
                            "spring.ai.chat-models.default-options.temperature=0.5",
                            "spring.ai.chat-models.default-options.max-tokens=2000",
                            "spring.ai.chat-models.default-options.top-p=0.9",
                            "spring.ai.chat-models.list[0].id=test-model")
                    .run(context -> {
                        assertThat(context).hasSingleBean(ChatClientStore.class);
                        ChatClientStore store = context.getBean(ChatClientStore.class);
                        assertThat(store.getModelIds()).contains("test-model");
                    });
        }

        @Test
        @DisplayName("Should use model-specific options when specified")
        void shouldUseModelSpecificOptions() {
            contextRunner
                    .withUserConfiguration(MockChatClientConfig.class)
                    .withPropertyValues(
                            "spring.ai.chat-models.default-options.temperature=0.0",
                            "spring.ai.chat-models.list[0].id=custom-model",
                            "spring.ai.chat-models.list[0].options.temperature=0.8",
                            "spring.ai.chat-models.list[0].options.max-tokens=500")
                    .run(context -> {
                        assertThat(context).hasSingleBean(ChatClientStore.class);
                        ChatClientStore store = context.getBean(ChatClientStore.class);
                        assertThat(store.getModelIds()).contains("custom-model");
                    });
        }

        @Test
        @DisplayName("Should work with SimpleLoggerAdvisor when available")
        void shouldWorkWithLoggerAdvisor() {
            contextRunner
                    .withUserConfiguration(MockChatClientConfig.class, LoggerAdvisorConfig.class)
                    .withPropertyValues("spring.ai.chat-models.list[0].id=logged-model")
                    .run(context -> {
                        assertThat(context).hasSingleBean(ChatClientStore.class);
                        assertThat(context).hasSingleBean(SimpleLoggerAdvisor.class);
                    });
        }

        @Test
        @DisplayName("Should work without SimpleLoggerAdvisor")
        void shouldWorkWithoutLoggerAdvisor() {
            contextRunner
                    .withUserConfiguration(MockChatClientConfig.class)
                    .withPropertyValues("spring.ai.chat-models.list[0].id=no-logger-model")
                    .run(context -> {
                        assertThat(context).hasSingleBean(ChatClientStore.class);
                        assertThat(context).doesNotHaveBean(SimpleLoggerAdvisor.class);
                    });
        }
    }

    @Nested
    @DisplayName("ConditionalOnMissingBean")
    class ConditionalOnMissingBean {

        @Test
        @DisplayName("Should not override existing ChatClientStore bean")
        void shouldNotOverrideExistingBean() {
            contextRunner
                    .withUserConfiguration(MockChatClientConfig.class, CustomChatClientStoreConfig.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(ChatClientStore.class);
                        ChatClientStore store = context.getBean(ChatClientStore.class);
                        assertThat(store.getModelIds()).containsExactly("custom-existing");
                    });
        }
    }

    @Nested
    @DisplayName("ChatModelProperties")
    class ChatModelPropertiesTests {

        @Test
        @DisplayName("Should have correct default values")
        void shouldHaveDefaultValues() {
            ChatClientStoreAutoConfiguration.ChatModelProperties properties =
                    new ChatClientStoreAutoConfiguration.ChatModelProperties();

            assertThat(properties.getList()).isEmpty();
            assertThat(properties.getDefaultOptions()).isNotNull();
            assertThat(properties.getDefaultOptions().getTemperature()).isEqualTo(0.0);
            assertThat(properties.getDefaultOptions().getMaxTokens()).isEqualTo(1000);
            assertThat(properties.getDefaultOptions().getTopP()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("ModelConfig should store id and options")
        void modelConfigShouldStoreIdAndOptions() {
            ChatClientStoreAutoConfiguration.ChatModelProperties.ModelConfig config =
                    new ChatClientStoreAutoConfiguration.ChatModelProperties.ModelConfig();
            config.setId("test-id");

            ChatClientStoreAutoConfiguration.ChatModelProperties.ModelOptions options =
                    new ChatClientStoreAutoConfiguration.ChatModelProperties.ModelOptions();
            options.setTemperature(0.7);
            config.setOptions(options);

            assertThat(config.getId()).isEqualTo("test-id");
            assertThat(config.getOptions().getTemperature()).isEqualTo(0.7);
        }
    }

    @Configuration
    static class MockChatClientConfig {
        @Bean
        ChatClient.Builder chatClientBuilder() {
            ChatClient mockClient = mock(ChatClient.class);
            ChatClient.Builder builder = mock(ChatClient.Builder.class);
            when(builder.build()).thenReturn(mockClient);
            when(builder.clone()).thenReturn(builder);
            when(builder.defaultAdvisors(org.mockito.ArgumentMatchers.any(SimpleLoggerAdvisor.class)))
                    .thenReturn(builder);
            when(builder.defaultOptions(org.mockito.ArgumentMatchers.any())).thenReturn(builder);
            return builder;
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
            ChatClient mockClient = mock(ChatClient.class);
            return new ChatClientStore(java.util.Map.of("custom-existing", mockClient), mockClient);
        }
    }
}
