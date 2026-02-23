package ai.qa.solutions.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ai.qa.solutions.chatclient.ChatClientStore;
import ai.qa.solutions.embedding.EmbeddingModelStore;
import ai.qa.solutions.execution.ratelimit.ProviderRateLimiterRegistry;
import ai.qa.solutions.execution.ratelimit.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

@DisplayName("MultiModelExecutor Rate Limiting Tests")
@ExtendWith(MockitoExtension.class)
class MultiModelExecutorRateLimitTest {

    @Mock
    private ChatClientStore chatClientStore;

    @Mock
    private ProviderRateLimiterRegistry rateLimiterRegistry;

    private AsyncTaskExecutor taskExecutor;

    @BeforeEach
    void setUp() {
        taskExecutor = new SimpleAsyncTaskExecutor();
    }

    @Nested
    @DisplayName("LLM Rate Limiting")
    class LlmRateLimiting {

        @Test
        @DisplayName("Should execute without rate limiter when registry is null")
        void shouldExecuteWithoutRateLimiterWhenRegistryIsNull() {
            // Given - 4-arg constructor (no registry)
            final var executor = new MultiModelExecutor(chatClientStore, null, taskExecutor, taskExecutor);
            final ChatClient mockClient = createMockClientWithScore(0.8);
            when(chatClientStore.get("model-1")).thenReturn(mockClient);

            // When
            final ModelResult<TestResponse> result =
                    executor.executeLlmOnModel("model-1", "test prompt", TestResponse.class);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result().score()).isEqualTo(0.8);
        }

        @Test
        @DisplayName("Should acquire rate limit before LLM call")
        void shouldAcquireRateLimitBeforeLlmCall() {
            // Given
            final var executor =
                    new MultiModelExecutor(chatClientStore, null, taskExecutor, taskExecutor, rateLimiterRegistry);
            final ChatClient mockClient = createMockClientWithScore(0.9);
            when(chatClientStore.get("model-1")).thenReturn(mockClient);
            doNothing().when(rateLimiterRegistry).acquire("model-1");

            // When
            final ModelResult<TestResponse> result =
                    executor.executeLlmOnModel("model-1", "test prompt", TestResponse.class);

            // Then
            assertThat(result.isSuccess()).isTrue();
            verify(rateLimiterRegistry).acquire("model-1");
        }

        @Test
        @DisplayName("Should propagate rate limit exceeded as model failure")
        void shouldPropagateRateLimitExceededAsModelFailure() {
            // Given
            final var executor =
                    new MultiModelExecutor(chatClientStore, null, taskExecutor, taskExecutor, rateLimiterRegistry);
            doThrow(new RateLimitExceededException("model-1", "provider-1", "Rate limit exceeded"))
                    .when(rateLimiterRegistry)
                    .acquire("model-1");

            // When
            final ModelResult<TestResponse> result =
                    executor.executeLlmOnModel("model-1", "test prompt", TestResponse.class);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.error()).isInstanceOf(RateLimitExceededException.class);
            assertThat(result.error().getMessage()).contains("Rate limit exceeded");
        }

        @Test
        @DisplayName("Should not acquire rate limit when registry is not set")
        void shouldNotAcquireRateLimitWhenRegistryIsNotSet() {
            // Given - 4-arg constructor (null registry)
            final var executor = new MultiModelExecutor(chatClientStore, null, taskExecutor, taskExecutor);
            final ChatClient mockClient = createMockClientWithScore(0.7);
            when(chatClientStore.get("model-1")).thenReturn(mockClient);

            // When
            final ModelResult<TestResponse> result =
                    executor.executeLlmOnModel("model-1", "test prompt", TestResponse.class);

            // Then - no NullPointerException, no interaction with any registry
            assertThat(result.isSuccess()).isTrue();
            verifyNoInteractions(rateLimiterRegistry);
        }
    }

    @Nested
    @DisplayName("Embedding Rate Limiting")
    class EmbeddingRateLimiting {

        @Mock
        private EmbeddingModelStore embeddingModelStore;

        @Test
        @DisplayName("Should acquire rate limit before embedding call")
        void shouldAcquireRateLimitBeforeEmbeddingCall() {
            // Given
            final var executor = new MultiModelExecutor(
                    chatClientStore, embeddingModelStore, taskExecutor, taskExecutor, rateLimiterRegistry);
            final EmbeddingModel mockModel = mock(EmbeddingModel.class);
            when(embeddingModelStore.get("embed-1")).thenReturn(mockModel);
            when(mockModel.embed("test text")).thenReturn(new float[] {0.1f, 0.2f});
            doNothing().when(rateLimiterRegistry).acquire("embed-1");

            // When
            final ModelResult<float[]> result = executor.executeEmbeddingOnModel("embed-1", "test text");

            // Then
            assertThat(result.isSuccess()).isTrue();
            verify(rateLimiterRegistry).acquire("embed-1");
        }
    }

    // ========== Helper Methods ==========

    private ChatClient createMockClientWithScore(final double score) {
        final ChatClient client = mock(ChatClient.class);
        final ChatClient.CallResponseSpec callSpec = mock(ChatClient.CallResponseSpec.class);
        final ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);

        lenient().when(client.prompt(any(String.class))).thenReturn(requestSpec);
        lenient().when(requestSpec.call()).thenReturn(callSpec);
        lenient().when(callSpec.entity(TestResponse.class)).thenReturn(new TestResponse(score));

        return client;
    }

    record TestResponse(double score) {}
}
