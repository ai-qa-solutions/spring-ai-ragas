package ai.qa.solutions.execution;

import ai.qa.solutions.chatclient.ChatClientAutoConfiguration;
import ai.qa.solutions.chatclient.ChatClientStore;
import ai.qa.solutions.execution.listeners.LoggingExecutionListener;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.AsyncTaskExecutor;

/**
 * Spring Boot autoconfiguration for the multimodel execution framework.
 * <p>
 * This configuration automatically sets up the {@link MultiModelExecutor} bean
 * when Spring AI's {@link ChatClient} is present on the classpath. It also registers
 * a default {@link LoggingExecutionListener} for monitoring execution events.
 * <p>
 * The autoconfiguration runs after {@link ChatClientAutoConfiguration} to ensure
 * that the {@link ChatClientStore} is properly initialized before creating the executor.
 * <p>
 * <b>Conditional Activation:</b> This configuration is only active when
 * {@code org.springframework.ai.chat.client.ChatClient} is available on the classpath.
 */
@Slf4j
@ConditionalOnClass(ChatClient.class)
@AutoConfiguration(after = ChatClientAutoConfiguration.class)
public class MultiModelExecutorAutoconfiguration {

    /**
     * Creates and configures the main {@link MultiModelExecutor} bean.
     * <p>
     * This bean is responsible for executing metrics across multiple AI models in parallel.
     * All {@link ModelExecutionListener} beans found in the application context are
     * automatically registered with the executor.
     *
     * @param chatClientStore store of configured AI model clients
     * @param taskExecutor    executor for parallel async operations
     * @param listeners       all execution listeners available in the context
     * @return a configured multi-model executor
     */
    @Bean
    public MultiModelExecutor multiModelExecutor(
            final ChatClientStore chatClientStore,
            final AsyncTaskExecutor taskExecutor,
            final List<ModelExecutionListener> listeners) {
        final MultiModelExecutor executor = new MultiModelExecutor(chatClientStore, taskExecutor);
        listeners.forEach(executor::addListener);
        return executor;
    }

    /**
     * Creates the default logging listener for execution monitoring.
     * <p>
     * This listener logs execution events at various levels and renders ASCII charts
     * showing score distributions across models. It runs with highest priority
     * (lowest order value) to ensure logging happens first.
     *
     * @return a logging execution listener
     */
    @Bean
    public LoggingExecutionListener loggingExecutionListener() {
        return new LoggingExecutionListener();
    }
}
