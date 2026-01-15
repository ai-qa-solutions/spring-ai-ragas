package ai.qa.solutions.chatclient;

import java.util.ArrayList;
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
 * <p>
 * Supports multiple ChatClients per model ID, enabling scenarios where
 * the same model is configured in different providers (e.g., gpt-4 in both
 * OpenAI and Azure). When a model ID has multiple ChatClients, all of them
 * will be invoked during evaluation.
 */
@Slf4j
public class ChatClientStore {

    /**
     * Thread-safe map of clients indexed by model ID.
     * Each model ID can have multiple ChatClients (e.g., same model from different providers).
     */
    private final Map<String, List<ChatClient>> clients = new ConcurrentHashMap<>();

    /**
     * Default ChatClient configured through standard Spring AI autoconfiguration.
     */
    private final ChatClient defaultClient;

    /**
     * Creates a new store with the given clients and default client.
     * <p>
     * This constructor supports backward compatibility with single ChatClient per model.
     *
     * @param clients       map of clients where key is model ID, value is ChatClient
     * @param defaultClient default client to use when model is not specified
     */
    public ChatClientStore(final Map<String, ChatClient> clients, final ChatClient defaultClient) {
        for (final Map.Entry<String, ChatClient> entry : clients.entrySet()) {
            this.clients.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(entry.getValue());
        }
        this.defaultClient = defaultClient;
        log.info("ChatClientStore initialized with {} models + default", clients.size());
    }

    /**
     * Creates a new store with multiple ChatClients per model ID.
     *
     * @param clientsMulti  map of clients where key is model ID, value is list of ChatClients
     * @param defaultClient default client to use when model is not specified
     * @param multiProvider flag to distinguish from single-client constructor
     */
    public ChatClientStore(
            final Map<String, List<ChatClient>> clientsMulti,
            final ChatClient defaultClient,
            final boolean multiProvider) {
        for (final Map.Entry<String, List<ChatClient>> entry : clientsMulti.entrySet()) {
            this.clients.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        this.defaultClient = defaultClient;
        final int totalClients =
                clientsMulti.values().stream().mapToInt(List::size).sum();
        log.info(
                "ChatClientStore initialized with {} model IDs ({} total clients) + default",
                clientsMulti.size(),
                totalClients);
    }

    /**
     * Gets the first ChatClient for the specified model.
     * <p>
     * This method maintains backward compatibility. If the model has multiple
     * ChatClients (from different providers), only the first one is returned.
     * Use {@link #getClients(String)} to get all ChatClients for a model.
     *
     * @param modelId unique model identifier
     * @return first configured ChatClient for this model
     * @throws IllegalArgumentException if model with this ID is not found
     */
    public ChatClient get(final String modelId) {
        final List<ChatClient> clientList = clients.get(modelId);
        if (clientList == null || clientList.isEmpty()) {
            throw new IllegalArgumentException("Chat model not found: " + modelId + ". Available: " + clients.keySet());
        }
        return clientList.get(0);
    }

    /**
     * Gets all ChatClients for the specified model.
     * <p>
     * If the same model ID is configured in multiple providers (e.g., gpt-4 in
     * both OpenAI and Azure), this method returns all of them.
     *
     * @param modelId unique model identifier
     * @return list of all ChatClients for this model
     * @throws IllegalArgumentException if model with this ID is not found
     */
    public List<ChatClient> getClients(final String modelId) {
        final List<ChatClient> clientList = clients.get(modelId);
        if (clientList == null || clientList.isEmpty()) {
            throw new IllegalArgumentException("Chat model not found: " + modelId + ". Available: " + clients.keySet());
        }
        return List.copyOf(clientList);
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
     * If models have multiple ChatClients, all of them are included.
     *
     * @return list of all ChatClient instances from configuration
     */
    public List<ChatClient> getAll() {
        return clients.values().stream().flatMap(List::stream).toList();
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
     * Returns the number of registered model IDs.
     * <p>
     * Note: This returns the count of unique model IDs, not the total number
     * of ChatClients. Use {@link #getTotalClientCount()} for the total count.
     *
     * @return number of model IDs
     */
    public int size() {
        return clients.size();
    }

    /**
     * Returns the total number of ChatClients across all models.
     *
     * @return total number of ChatClients
     */
    public int getTotalClientCount() {
        return clients.values().stream().mapToInt(List::size).sum();
    }

    /**
     * Returns the number of ChatClients for a specific model.
     *
     * @param modelId model identifier
     * @return number of ChatClients for this model, or 0 if not found
     */
    public int getClientCount(final String modelId) {
        final List<ChatClient> clientList = clients.get(modelId);
        return clientList != null ? clientList.size() : 0;
    }

    /**
     * Adds a ChatClient for the specified model ID.
     * <p>
     * If the model ID already exists, the new ChatClient is added to the list.
     * This enables configuring the same model from multiple providers.
     *
     * @param modelId model identifier
     * @param client  ChatClient to add
     */
    public void addClient(final String modelId, final ChatClient client) {
        clients.computeIfAbsent(modelId, k -> new ArrayList<>()).add(client);
        log.debug("Added ChatClient for model: {}, total for this model: {}", modelId, getClientCount(modelId));
    }
}
