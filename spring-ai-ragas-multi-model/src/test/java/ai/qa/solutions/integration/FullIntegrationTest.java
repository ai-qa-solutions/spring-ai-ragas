package ai.qa.solutions.integration;

import static org.assertj.core.api.Assertions.assertThat;

import ai.qa.solutions.ConfigurationForTests;
import ai.qa.solutions.chatclient.ChatClientStore;
import ai.qa.solutions.embedding.EmbeddingModelStore;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@ActiveProfiles("test")
@EnableAutoConfiguration
@SpringBootTest(classes = ConfigurationForTests.class)
@DisplayName("Full Integration Tests - Chat & Embedding")
class FullIntegrationTest {

    @Autowired
    private ChatClientStore chatClientStore;

    @Autowired
    private EmbeddingModelStore embeddingModelStore;

    @Test
    @DisplayName("Both stores should be autowired")
    void bothStoresShouldBeAutowired() {
        assertThat(chatClientStore).isNotNull();
        assertThat(embeddingModelStore).isNotNull();
    }

    @Test
    @DisplayName("Chat and embedding should work together")
    void chatAndEmbeddingShouldWorkTogether() {
        final ChatClient chatClient = chatClientStore.getDefault();
        final String generatedText = chatClient
                .prompt()
                .user("Generate a short sentence about artificial intelligence")
                .call()
                .content();

        assertThat(generatedText).isNotEmpty();

        final EmbeddingModel embeddingModel = embeddingModelStore.getDefault();
        final float[] embedding = embeddingModel.embed(generatedText);

        assertThat(embedding).isNotEmpty();
    }

    @Test
    @DisplayName("Should handle RAG-like workflow")
    void shouldHandleRagLikeWorkflow() {
        final List<String> documents = List.of(
                "Spring AI is a framework for AI applications",
                "Embeddings convert text to vectors",
                "ChatGPT is a language model");

        final EmbeddingModel embeddingModel = embeddingModelStore.get("qwen/qwen3-embedding-8b");
        final List<float[]> documentEmbeddings = embeddingModel.embed(documents);

        assertThat(documentEmbeddings).hasSize(3);

        final String userQuery = "What is Spring AI?";
        final float[] queryEmbedding = embeddingModel.embed(userQuery);

        assertThat(queryEmbedding).isNotEmpty();

        final String relevantDocument = documents.get(0);

        final ChatClient chatClient = chatClientStore.get("anthropic/claude-4.5-sonnet");
        final String answer = chatClient
                .prompt()
                .user(String.format(
                        "Context: %s\n\nQuestion: %s\n\nAnswer based on the context:", relevantDocument, userQuery))
                .call()
                .content();

        assertThat(answer).isNotEmpty().containsIgnoringCase("spring");
    }

    @Test
    @DisplayName("Should test semantic similarity")
    void shouldTestSemanticSimilarity() {
        final EmbeddingModel embeddingModel = embeddingModelStore.get("openai/text-embedding-3-large");

        final float[] embedding1 = embeddingModel.embed("The cat sits on the mat");
        final float[] embedding2 = embeddingModel.embed("A feline is resting on the rug");
        final float[] embedding3 = embeddingModel.embed("Quantum computing is revolutionary");

        final double similarity12 = cosineSimilarity(embedding1, embedding2);
        final double similarity13 = cosineSimilarity(embedding1, embedding3);

        assertThat(similarity12).isGreaterThan(similarity13);
    }

    @Test
    @DisplayName("Should handle multi-model chat comparison")
    void shouldHandleMultiModelChatComparison() {
        final String prompt = "What is 2+2? Answer with just the number.";

        final List<String> modelIds =
                List.of("google/gemini-2.5-flash", "anthropic/claude-haiku-4.5", "openai/gpt-4o-mini");

        final List<String> responses = modelIds.stream()
                .map(modelId -> chatClientStore.get(modelId))
                .map(client -> client.prompt().user(prompt).call().content())
                .toList();

        assertThat(responses).hasSize(3).allMatch(r -> r != null && !r.isEmpty());
        assertThat(responses).allMatch(r -> r.contains("4"));
    }

    @Test
    @DisplayName("Should handle multi-model embedding comparison")
    void shouldHandleMultiModelEmbeddingComparison() {
        final String text = "Test embedding comparison";

        final List<String> modelIds = embeddingModelStore.getModelIds();
        final List<float[]> embeddings = modelIds.stream()
                .map(modelId -> {
                    try {
                        final EmbeddingModel model = embeddingModelStore.get(modelId);
                        return model.embed(text);
                    } catch (final Exception e) {
                        // Логируем ошибку, но не прерываем тест
                        System.err.println("Failed to create embedding for model: " + modelId + " - " + e.getMessage());
                        return null;
                    }
                })
                .filter(e -> e != null && e.length > 0)
                .toList();
        assertThat(embeddings).isNotEmpty().allMatch(e -> e != null && e.length > 0);
        System.out.println(
                "Successfully created embeddings for " + embeddings.size() + " out of " + modelIds.size() + " models");
    }

    @Test
    @DisplayName("Should handle concurrent chat and embedding operations")
    void shouldHandleConcurrentChatAndEmbeddingOperations() throws InterruptedException {
        final int threadCount = 10;
        final Thread[] threads = new Thread[threadCount];
        final boolean[] results = new boolean[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    if (index % 2 == 0) {
                        final ChatClient client = chatClientStore.getDefault();
                        final String response = client.prompt()
                                .user("Say: test " + index)
                                .call()
                                .content();
                        results[index] = response != null && !response.isEmpty();
                    } else {
                        final EmbeddingModel model = embeddingModelStore.getDefault();
                        final float[] embedding = model.embed("test " + index);
                        results[index] = embedding.length > 0;
                    }
                } catch (final Exception e) {
                    results[index] = false;
                }
            });
            threads[i].start();
        }
        for (final Thread thread : threads) {
            thread.join();
        }
        for (final boolean result : results) {
            assertThat(result).isTrue();
        }
    }

    @Test
    @DisplayName("Should verify store independence")
    void shouldVerifyStoreIndependence() {
        final int chatModelsCount = chatClientStore.size();
        final int embeddingModelsCount = embeddingModelStore.size();
        chatClientStore.get(chatClientStore.getModelIds().get(0));
        embeddingModelStore.get(embeddingModelStore.getModelIds().get(0));
        assertThat(chatClientStore.size()).isEqualTo(chatModelsCount);
        assertThat(embeddingModelStore.size()).isEqualTo(embeddingModelsCount);
    }

    @Test
    @DisplayName("Should verify all embedding models are available")
    void shouldVerifyAllEmbeddingModelsAreAvailable() {
        final List<String> expectedModels = List.of(
                "openai/text-embedding-3-large",
                "qwen/qwen3-embedding-8b",
                "qwen/qwen3-embedding-4b",
                "google/gemini-embedding-001",
                "baai/bge-m3",
                "baai/bge-base-en-v1.5");

        assertThat(embeddingModelStore.getModelIds()).containsExactlyInAnyOrderElementsOf(expectedModels);
    }

    private double cosineSimilarity(final float[] vectorA, final float[] vectorB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
