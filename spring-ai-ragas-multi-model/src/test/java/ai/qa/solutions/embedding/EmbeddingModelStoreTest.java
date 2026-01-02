package ai.qa.solutions.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;

@DisplayName("EmbeddingModelStore Unit Tests")
class EmbeddingModelStoreTest {

    private EmbeddingModel defaultModel;
    private EmbeddingModel model1;
    private EmbeddingModel model2;
    private EmbeddingModel model3;
    private EmbeddingModelStore store;

    @BeforeEach
    void setUp() {
        defaultModel = mock(EmbeddingModel.class);
        model1 = mock(EmbeddingModel.class);
        model2 = mock(EmbeddingModel.class);
        model3 = mock(EmbeddingModel.class);

        final Map<String, EmbeddingModel> models = new HashMap<>();
        models.put("embedding-1", model1);
        models.put("embedding-2", model2);
        models.put("embedding-3", model3);

        store = new EmbeddingModelStore(models, defaultModel);
    }

    @Test
    @DisplayName("Should return default model")
    void shouldReturnDefaultModel() {
        assertThat(store.getDefault()).isEqualTo(defaultModel);
    }

    @Test
    @DisplayName("Should return model by ID")
    void shouldReturnModelById() {
        assertThat(store.get("embedding-1")).isEqualTo(model1);
        assertThat(store.get("embedding-2")).isEqualTo(model2);
        assertThat(store.get("embedding-3")).isEqualTo(model3);
    }

    @Test
    @DisplayName("Should throw exception for non-existent model")
    void shouldThrowExceptionForNonExistentModel() {
        assertThatThrownBy(() -> store.get("non-existent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Embedding model not found: non-existent")
                .hasMessageContaining("Available: ");
    }

    @Test
    @DisplayName("Should return all models")
    void shouldReturnAllModels() {
        final List<EmbeddingModel> allModels = store.getAll();

        assertThat(allModels)
                .hasSize(3)
                .containsExactlyInAnyOrder(model1, model2, model3)
                .doesNotContain(defaultModel);
    }

    @Test
    @DisplayName("Should return all model IDs")
    void shouldReturnAllModelIds() {
        final List<String> modelIds = store.getModelIds();

        assertThat(modelIds).hasSize(3).containsExactlyInAnyOrder("embedding-1", "embedding-2", "embedding-3");
    }

    @Test
    @DisplayName("Should check if model exists")
    void shouldCheckIfModelExists() {
        assertThat(store.contains("embedding-1")).isTrue();
        assertThat(store.contains("embedding-2")).isTrue();
        assertThat(store.contains("embedding-3")).isTrue();
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
        final EmbeddingModelStore emptyStore = new EmbeddingModelStore(new HashMap<>(), defaultModel);

        assertThat(emptyStore.size()).isZero();
        assertThat(emptyStore.getAll()).isEmpty();
        assertThat(emptyStore.getModelIds()).isEmpty();
        assertThat(emptyStore.getDefault()).isEqualTo(defaultModel);
    }

    @Test
    @DisplayName("Should return immutable list of models")
    void shouldReturnImmutableListOfModels() {
        final List<EmbeddingModel> models = store.getAll();

        assertThatThrownBy(() -> models.add(mock(EmbeddingModel.class)))
                .isInstanceOf(UnsupportedOperationException.class);
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
                    store.get("embedding-1");
                    store.contains("embedding-2");
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
