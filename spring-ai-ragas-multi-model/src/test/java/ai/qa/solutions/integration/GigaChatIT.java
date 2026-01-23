package ai.qa.solutions.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration tests for GigaChat external starter.
 *
 * <p>Demonstrates that spring-ai-ragas supports external AI starters
 * beyond standard OpenAI-compatible providers.</p>
 *
 * <p>Configuration notes:</p>
 * <ul>
 *   <li>Scope must be at auth level: spring.ai.gigachat.auth.scope</li>
 *   <li>Use unsafe-ssl=true for testing (not for production)</li>
 *   <li>Embeddings require paid plan (402 Payment Required)</li>
 * </ul>
 */
@EnableAutoConfiguration(
        excludeName = {
            "org.springframework.ai.model.openai.autoconfigure.OpenAiAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration"
        })
@SpringBootTest(classes = GigaChatIT.TestConfig.class)
@TestPropertySource(
        properties = {
            "spring.ai.gigachat.auth.bearer.client-id=${GIGACHAT_API_CLIENT_ID}",
            "spring.ai.gigachat.auth.bearer.client-secret=${GIGACHAT_API_CLIENT_SECRET}",
            "spring.ai.gigachat.auth.scope=${GIGACHAT_API_SCOPE}",
            "spring.ai.gigachat.auth.unsafe-ssl=true",
            "spring.ai.gigachat.chat.options.model=GigaChat-2-Max",
            "spring.ai.gigachat.chat.options.temperature=0.0",
            "spring.ai.gigachat.embedding.options.model=Embeddings"
        })
@DisplayName("GigaChat Integration Tests")
@EnabledIfEnvironmentVariable(named = "GIGACHAT_API_CLIENT_ID", matches = ".*")
class GigaChatIT {

    @Autowired
    private ChatModel gigaChatChatModel;

    @Autowired(required = false)
    private EmbeddingModel gigaChatEmbeddingModel;

    @Test
    @DisplayName("Should call GigaChat-2-Max model")
    void shouldCallGigaChat2Max() {
        System.out.println(
                "GigaChatModel class: " + gigaChatChatModel.getClass().getName());
        System.out.println(
                "Default model: " + gigaChatChatModel.getDefaultOptions().getModel());

        final String prompt = "Сколько будет 2+2? Ответь только числом.";

        final ChatClient client = ChatClient.builder(gigaChatChatModel).build();
        final String response = client.prompt().user(prompt).call().content();

        System.out.println("GigaChat-2-Max response: " + response);
        assertThat(response).isNotEmpty();
    }

    @Test
    @DisplayName("Should test GigaChat embeddings")
    void shouldTestGigaChatEmbeddings() {
        if (gigaChatEmbeddingModel == null) {
            System.out.println("GigaChat EmbeddingModel not available, skipping");
            return;
        }

        System.out.println(
                "EmbeddingModel class: " + gigaChatEmbeddingModel.getClass().getName());

        final float[] embeddings = gigaChatEmbeddingModel.embed("Тестовый текст для эмбеддинга");
        System.out.println("Embedding dimensions: " + embeddings.length);

        assertThat(embeddings).isNotEmpty();
        assertThat(embeddings.length).isGreaterThan(0);
    }

    @SpringBootConfiguration
    static class TestConfig {
        // Empty config - GigaChat autoconfiguration will be loaded automatically
    }
}
