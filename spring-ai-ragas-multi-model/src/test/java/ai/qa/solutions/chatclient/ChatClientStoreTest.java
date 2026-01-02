package ai.qa.solutions.chatclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

@DisplayName("ChatClientStore Unit Tests")
class ChatClientStoreTest {

    private ChatClient defaultClient;
    private ChatClient client1;
    private ChatClient client2;
    private ChatClient client3;
    private ChatClientStore store;

    @BeforeEach
    void setUp() {
        defaultClient = mock(ChatClient.class);
        client1 = mock(ChatClient.class);
        client2 = mock(ChatClient.class);
        client3 = mock(ChatClient.class);

        final Map<String, ChatClient> clients = new HashMap<>();
        clients.put("model-1", client1);
        clients.put("model-2", client2);
        clients.put("model-3", client3);

        store = new ChatClientStore(clients, defaultClient);
    }

    @Test
    @DisplayName("Should return default client")
    void shouldReturnDefaultClient() {
        assertThat(store.getDefault()).isEqualTo(defaultClient);
    }

    @Test
    @DisplayName("Should return client by ID")
    void shouldReturnClientById() {
        assertThat(store.get("model-1")).isEqualTo(client1);
        assertThat(store.get("model-2")).isEqualTo(client2);
        assertThat(store.get("model-3")).isEqualTo(client3);
    }

    @Test
    @DisplayName("Should throw exception for non-existent model")
    void shouldThrowExceptionForNonExistentModel() {
        assertThatThrownBy(() -> store.get("non-existent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Chat model not found: non-existent")
                .hasMessageContaining("Available: ");
    }

    @Test
    @DisplayName("Should return all clients")
    void shouldReturnAllClients() {
        final List<ChatClient> allClients = store.getAll();

        assertThat(allClients)
                .hasSize(3)
                .containsExactlyInAnyOrder(client1, client2, client3)
                .doesNotContain(defaultClient);
    }

    @Test
    @DisplayName("Should return all model IDs")
    void shouldReturnAllModelIds() {
        final List<String> modelIds = store.getModelIds();

        assertThat(modelIds).hasSize(3).containsExactlyInAnyOrder("model-1", "model-2", "model-3");
    }

    @Test
    @DisplayName("Should check if model exists")
    void shouldCheckIfModelExists() {
        assertThat(store.contains("model-1")).isTrue();
        assertThat(store.contains("model-2")).isTrue();
        assertThat(store.contains("model-3")).isTrue();
        assertThat(store.contains("non-existent")).isFalse();
    }

    @Test
    @DisplayName("Should return correct size")
    void shouldReturnCorrectSize() {
        assertThat(store.size()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should handle empty store")
    void shouldHandleEmptyStore() {
        final ChatClientStore emptyStore = new ChatClientStore(new HashMap<>(), defaultClient);

        assertThat(emptyStore.size()).isZero();
        assertThat(emptyStore.getAll()).isEmpty();
        assertThat(emptyStore.getModelIds()).isEmpty();
        assertThat(emptyStore.getDefault()).isEqualTo(defaultClient);
    }

    @Test
    @DisplayName("Should return immutable list of clients")
    void shouldReturnImmutableListOfClients() {
        final List<ChatClient> clients = store.getAll();

        assertThatThrownBy(() -> clients.add(mock(ChatClient.class))).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Should return immutable list of model IDs")
    void shouldReturnImmutableListOfModelIds() {
        final List<String> modelIds = store.getModelIds();

        assertThatThrownBy(() -> modelIds.add("new-model")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Should be thread-safe for concurrent access")
    void shouldBeThreadSafeForConcurrentAccess() throws InterruptedException {
        final int threadCount = 10;
        final Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    store.get("model-1");
                    store.contains("model-2");
                    store.getAll();
                    store.getModelIds();
                }
            });
            threads[i].start();
        }
        for (final Thread thread : threads) {
            thread.join();
        }
        assertThat(store.size()).isEqualTo(3);
    }
}
