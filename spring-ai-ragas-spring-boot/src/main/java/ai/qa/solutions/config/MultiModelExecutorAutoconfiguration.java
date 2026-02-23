package ai.qa.solutions.config;

import ai.qa.solutions.chatclient.ChatClientStore;
import ai.qa.solutions.embedding.EmbeddingModelStore;
import ai.qa.solutions.execution.MultiModelExecutor;
import ai.qa.solutions.execution.ratelimit.ProviderRateLimiterRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Spring Boot autoconfiguration for the multimodel execution framework.
 * <p>
 * This configuration automatically sets up the {@link MultiModelExecutor} bean
 * when Spring AI's {@link ChatClient} is present on the classpath.
 * <p>
 * The autoconfiguration runs after {@link MultiProviderAutoConfiguration} to ensure
 * that stores are properly initialized before creating the executor.
 * <p>
 * <b>Note:</b> The executor is now stateless - all listeners should be registered
 * at the metric level.
 * <p>
 * <b>Conditional Activation:</b> This configuration is only active when
 * {@code org.springframework.ai.chat.client.ChatClient} is available on the classpath.
 */
@ConditionalOnClass(ChatClient.class)
@AutoConfiguration(after = MultiProviderAutoConfiguration.class)
@ConditionalOnProperty(
        prefix = "spring.ai.ragas.metrics",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class MultiModelExecutorAutoconfiguration {

    /**
     * Creates a task executor for RAGAS metric-level operations.
     * <p>
     * This executor handles the outer async layer - metric evaluation tasks.
     * It is separate from HTTP executor to prevent deadlocks when metrics
     * wait for HTTP responses while holding threads from this pool.
     * <p>
     * Configure this pool based on how many metrics you want to evaluate in parallel.
     *
     * @return a configured task executor for metric operations
     */
    @Bean(name = "ragasMetricExecutor")
    public AsyncTaskExecutor ragasMetricExecutor() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("ragas-metric-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * Creates a task executor for RAGAS HTTP/LLM operations.
     * <p>
     * This executor handles HTTP requests to LLM APIs. It is separate from
     * the metric executor to prevent deadlocks - metric tasks can safely
     * wait on HTTP tasks without blocking the HTTP pool.
     * <p>
     * Configure this pool based on how many concurrent LLM API requests
     * your infrastructure can handle.
     *
     * @return a configured task executor for HTTP operations
     */
    @Bean(name = "ragasHttpExecutor")
    public AsyncTaskExecutor ragasHttpExecutor() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(64);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("ragas-http-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.initialize();
        return executor;
    }

    /**
     * Creates the main {@link MultiModelExecutor} bean.
     * <p>
     * This bean is responsible for executing LLM and embedding calls across multiple AI models.
     * It uses two separate executors to prevent deadlocks:
     * <ul>
     *   <li>{@code ragasMetricExecutor} - for metric-level async operations (runAsync)</li>
     *   <li>{@code ragasHttpExecutor} - for HTTP/LLM API calls</li>
     * </ul>
     * <p>
     * When a {@link ProviderRateLimiterRegistry} bean is available, it is injected into the executor
     * to enforce per-provider rate limiting on all LLM and embedding API calls.
     *
     * @param chatClientStore       store of configured AI model clients
     * @param embeddingModelStore   store of configured embedding models (optional)
     * @param ragasMetricExecutor   executor for metric-level async operations
     * @param ragasHttpExecutor     executor for HTTP/LLM API calls
     * @param rateLimiterRegistry   per-provider rate limiter registry (optional, no rate limiting if absent)
     * @return a configured multi-model executor
     */
    @Bean
    public MultiModelExecutor multiModelExecutor(
            final ChatClientStore chatClientStore,
            @Autowired(required = false) final EmbeddingModelStore embeddingModelStore,
            final AsyncTaskExecutor ragasMetricExecutor,
            final AsyncTaskExecutor ragasHttpExecutor,
            @Autowired(required = false) final ProviderRateLimiterRegistry rateLimiterRegistry) {
        return new MultiModelExecutor(
                chatClientStore, embeddingModelStore, ragasMetricExecutor, ragasHttpExecutor, rateLimiterRegistry);
    }
}
