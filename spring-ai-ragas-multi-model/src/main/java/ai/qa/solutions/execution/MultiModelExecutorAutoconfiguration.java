package ai.qa.solutions.execution;

import ai.qa.solutions.chatclient.ChatClientAutoConfiguration;
import ai.qa.solutions.chatclient.ChatClientStore;
import ai.qa.solutions.embedding.EmbeddingModelAutoConfiguration;
import ai.qa.solutions.embedding.EmbeddingModelStore;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.AsyncTaskExecutor;

/**
 * Spring Boot autoconfiguration for the multimodel execution framework.
 * <p>
 * This configuration automatically sets up the {@link MultiModelExecutor} bean
 * when Spring AI's {@link ChatClient} is present on the classpath.
 * <p>
 * The autoconfiguration runs after {@link ChatClientAutoConfiguration} and
 * {@link EmbeddingModelAutoConfiguration} to ensure that both stores are
 * properly initialized before creating the executor.
 * <p>
 * <b>Note:</b> The executor is now stateless - all listeners should be registered
 * at the metric level using {@link ai.qa.solutions.metric.AbstractMultiModelчMetric#addListener}.
 * <p>
 * <b>Conditional Activation:</b> This configuration is only active when
 * {@code org.springframework.ai.chat.client.ChatClient} is available on the classpath.
 */
@ConditionalOnClass(ChatClient.class)
@AutoConfiguration(after = {ChatClientAutoConfiguration.class, EmbeddingModelAutoConfiguration.class})
public class MultiModelExecutorAutoconfiguration {

    /**
     * Creates the main {@link MultiModelExecutor} bean.
     * <p>
     * This bean is responsible for executing LLM and embedding calls across multiple AI models.
     * It is stateless and does not manage listeners - metrics own their listeners directly.
     *
     * @param chatClientStore      store of configured AI model clients
     * @param embeddingModelStore  store of configured embedding models (optional)
     * @param taskExecutor         executor for parallel async operations
     * @return a configured multi-model executor
     */
    @Bean
    public MultiModelExecutor multiModelExecutor(
            final ChatClientStore chatClientStore,
            @Autowired(required = false) final EmbeddingModelStore embeddingModelStore,
            final AsyncTaskExecutor taskExecutor) {
        return new MultiModelExecutor(chatClientStore, embeddingModelStore, taskExecutor);
    }
}
