package ai.qa.solutions.chatclient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

/**
 * Store for pre-configured {@link ChatClient} instances for different models.
 * <p>
 * Provides thread-safe access to clients by model ID, as well as
 * to the default client. Used for managing multiple chat models
 * in Spring AI applications.
 *
 * @see ChatClientAutoConfiguration
 */
@Slf4j
public class ChatClientStore {

    /**
     * Thread-safe map of clients indexed by model ID.
     */
    private final Map<String, ChatClient> clients = new ConcurrentHashMap<>();

    /**
     * Default ChatClient configured through standard Spring AI auto-configuration.
     */
    private final ChatClient defaultClient;

    /**
     * Creates a new store with the given clients and default client.
     *
     * @param clients map of clients where key is model ID, value is ChatClient
     * @param defaultClient default client to use when model is not specified
     */
    public ChatClientStore(final Map<String, ChatClient> clients, final ChatClient defaultClient) {
        this.clients.putAll(clients);
        this.defaultClient = defaultClient;
        log.info("ChatClientStore initialized with {} models + default", clients.size());
    }

    /**
     * Gets ChatClient for the specified model.
     *
     * @param modelId unique model identifier
     * @return configured ChatClient for this model
     * @throws IllegalArgumentException if model with this ID is not found
     */
    public ChatClient get(final String modelId) {
        final ChatClient client = clients.get(modelId);
        if (client == null) {
            throw new IllegalArgumentException("Chat model not found: " + modelId + ". Available: " + clients.keySet());
        }
        return client;
    }

    /**
     * Gets the default ChatClient.
     * <p>
     * The default client is created from the standard Spring AI builder and is not linked
     * to a specific model from the list.
     *
     * @return default ChatClient
     */
    public ChatClient getDefault() {
        return defaultClient;
    }

    /**
     * Gets an immutable list of all registered ChatClient instances.
     * <p>
     * The default client is not included in the result.
     *
     * @return list of all ChatClient instances from configuration
     */
    public List<ChatClient> getAll() {
        return List.copyOf(clients.values());
    }

    /**
     * Gets an immutable list of identifiers for all registered models.
     *
     * @return list of model IDs
     */
    public List<String> getModelIds() {
        return List.copyOf(clients.keySet());
    }

    /**
     * Checks whether a model with the specified ID is registered.
     *
     * @param modelId model identifier to check
     * @return {@code true} if the model exists, {@code false} otherwise
     */
    public boolean contains(final String modelId) {
        return clients.containsKey(modelId);
    }

    /**
     * Returns the number of registered models.
     * <p>
     * The default client is not counted.
     *
     * @return number of models
     */
    public int size() {
        return clients.size();
    }
}
