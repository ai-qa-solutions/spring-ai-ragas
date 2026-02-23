package ai.qa.solutions.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.qa.solutions.chatclient.ChatClientStore;
import ai.qa.solutions.embedding.EmbeddingModelStore;
import ai.qa.solutions.execution.MultiModelExecutor;
import ai.qa.solutions.execution.ratelimit.ProviderRateLimiterRegistry;
import java.util.Map;
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

@DisplayName("RateLimitAutoconfiguration Tests")
class RateLimitAutoconfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    MultiProviderAutoConfiguration.class, MultiModelExecutorAutoconfiguration.class));

    @Nested
    @DisplayName("Registry Creation")
    class RegistryCreation {

        @Test
        @DisplayName("Should create registry with OpenAI-compatible provider rate limit")
        void shouldCreateRegistryWithOpenAiCompatibleProviderRateLimit() {
            contextRunner
                    .withUserConfiguration(MockConfig.class)
                    .withPropertyValues(
                            "spring.ai.ragas.providers.auto-detect-beans=false",
                            "spring.ai.ragas.providers.openai-compatible[0].name=openrouter",
                            "spring.ai.ragas.providers.openai-compatible[0].base-url=https://openrouter.ai/api",
                            "spring.ai.ragas.providers.openai-compatible[0].api-key=test-key",
                            "spring.ai.ragas.providers.openai-compatible[0].rate-limit.rps=5",
                            "spring.ai.ragas.providers.openai-compatible[0].rate-limit.strategy=WAIT",
                            "spring.ai.ragas.providers.openai-compatible[0].chat-models[0].id=claude-3.5-sonnet")
                    .run(context -> {
                        assertThat(context).hasSingleBean(ProviderRateLimiterRegistry.class);
                    });
        }

        @Test
        @DisplayName("Should apply global defaults when provider has no rate limit")
        void shouldApplyGlobalDefaultsWhenProviderHasNoRateLimit() {
            contextRunner
                    .withUserConfiguration(MockConfig.class)
                    .withPropertyValues(
                            "spring.ai.ragas.providers.auto-detect-beans=false",
                            "spring.ai.ragas.providers.rate-limit.default-rps=10",
                            "spring.ai.ragas.providers.rate-limit.default-strategy=WAIT",
                            "spring.ai.ragas.providers.default-provider.enabled=true",
                            "spring.ai.ragas.providers.default-provider.models[0].id=gpt-4o-mini")
                    .run(context -> {
                        // Global default-rps=10 should cause a registry bean to be created
                        // since the default provider has models but no explicit rate-limit
                        assertThat(context).hasSingleBean(ProviderRateLimiterRegistry.class);
                    });
        }

        @Test
        @DisplayName("Should not create registry when no rate limit configured")
        void shouldNotCreateRegistryWhenNoRateLimitConfigured() {
            contextRunner
                    .withUserConfiguration(MockConfig.class)
                    .withPropertyValues("spring.ai.ragas.providers.auto-detect-beans=false")
                    .run(context -> {
                        // No rate limit properties at all - registry bean returns null
                        // (Spring registers the @Bean definition but the instance is null)
                        final var registry = context.getBeanProvider(ProviderRateLimiterRegistry.class)
                                .getIfAvailable();
                        assertThat(registry).isNull();
                    });
        }
    }

    @Nested
    @DisplayName("Executor Integration")
    class ExecutorIntegration {

        @Test
        @DisplayName("Should inject registry into MultiModelExecutor")
        void shouldInjectRegistryIntoMultiModelExecutor() {
            contextRunner
                    .withUserConfiguration(FullDependenciesConfig.class)
                    .withPropertyValues(
                            "spring.ai.ragas.providers.auto-detect-beans=false",
                            "spring.ai.ragas.providers.rate-limit.default-rps=10",
                            "spring.ai.ragas.providers.default-provider.enabled=true",
                            "spring.ai.ragas.providers.default-provider.models[0].id=gpt-4o-mini")
                    .run(context -> {
                        assertThat(context).hasSingleBean(MultiModelExecutor.class);
                        assertThat(context).hasSingleBean(ProviderRateLimiterRegistry.class);
                        // Executor bean exists and was created successfully with registry
                        final MultiModelExecutor executor = context.getBean(MultiModelExecutor.class);
                        assertThat(executor).isNotNull();
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
    static class FullDependenciesConfig {
        @Bean
        ChatClientStore chatClientStore() {
            final ChatClient client = mock(ChatClient.class);
            return new ChatClientStore(Map.of("gpt-4o-mini", client), client);
        }

        @Bean
        EmbeddingModelStore embeddingModelStore() {
            final EmbeddingModel model = mock(EmbeddingModel.class);
            return new EmbeddingModelStore(Map.of("embed-1", model), model);
        }
    }
}
