package ai.qa.solutions.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.qa.solutions.chatclient.ChatClientStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

@DisplayName("MultiModelExecutor Tests")
@ExtendWith(MockitoExtension.class)
class MultiModelExecutorTest {

    @Mock
    private ChatClientStore chatClientStore;

    private AsyncTaskExecutor taskExecutor;
    private MultiModelExecutor executor;

    @BeforeEach
    void setUp() {
        taskExecutor = new SimpleAsyncTaskExecutor();
        executor = new MultiModelExecutor(chatClientStore, taskExecutor);
    }

    @Nested
    @DisplayName("Basic Execution")
    class BasicExecution {

        @Test
        @DisplayName("Should execute on all models and return results")
        void shouldExecuteOnAllModelsAndReturnResults() {
            // Given
            setupMockModels(Map.of(
                    "model-1", 0.8,
                    "model-2", 1.0,
                    "model-3", 0.6));

            // When
            List<ModelResult<TestResponse>> results = executor.executeLlm("test prompt", TestResponse.class);

            // Then
            assertThat(results).hasSize(3);
            assertThat(results).allMatch(ModelResult::isSuccess);

            Map<String, Double> scores = results.stream()
                    .collect(java.util.stream.Collectors.toMap(
                            ModelResult::modelId, r -> r.result().score()));

            assertThat(scores.get("model-1")).isEqualTo(0.8);
            assertThat(scores.get("model-2")).isEqualTo(1.0);
            assertThat(scores.get("model-3")).isEqualTo(0.6);
        }

        @Test
        @DisplayName("Should handle partial failures gracefully")
        void shouldHandlePartialFailuresGracefully() {
            // Given
            ChatClient successClient = createMockClientWithScore(0.8);
            ChatClient failingClient = createFailingMockClient();

            when(chatClientStore.getModelIds()).thenReturn(List.of("success", "fail"));
            when(chatClientStore.get("success")).thenReturn(successClient);
            when(chatClientStore.get("fail")).thenReturn(failingClient);

            // When
            List<ModelResult<TestResponse>> results = executor.executeLlm("test", TestResponse.class);

            // Then
            assertThat(results).hasSize(2);

            ModelResult<TestResponse> successResult = results.stream()
                    .filter(r -> r.modelId().equals("success"))
                    .findFirst()
                    .orElseThrow();
            ModelResult<TestResponse> failResult = results.stream()
                    .filter(r -> r.modelId().equals("fail"))
                    .findFirst()
                    .orElseThrow();

            assertThat(successResult.isSuccess()).isTrue();
            assertThat(successResult.result().score()).isEqualTo(0.8);

            assertThat(failResult.isFailure()).isTrue();
            assertThat(failResult.error()).isNotNull();
        }

        @Test
        @DisplayName("Should return empty list when no models configured")
        void shouldReturnEmptyListWhenNoModelsConfigured() {
            // Given
            when(chatClientStore.getModelIds()).thenReturn(List.of());

            // When
            List<ModelResult<TestResponse>> results = executor.executeLlm("test", TestResponse.class);

            // Then
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("Should include duration in results")
        void shouldIncludeDurationInResults() {
            // Given
            setupMockModels(Map.of("model-1", 0.8));

            // When
            List<ModelResult<TestResponse>> results = executor.executeLlm("test", TestResponse.class);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).duration()).isNotNull();
            assertThat(results.get(0).duration().toMillis()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("Should include request in results")
        void shouldIncludeRequestInResults() {
            // Given
            setupMockModels(Map.of("model-1", 0.8));

            // When
            List<ModelResult<TestResponse>> results = executor.executeLlm("my test prompt", TestResponse.class);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).request()).isEqualTo("my test prompt");
        }
    }

    @Nested
    @DisplayName("Custom Model List")
    class CustomModelList {

        @Test
        @DisplayName("Should execute only on specified models when custom list provided")
        void shouldExecuteOnlyOnSpecifiedModels() {
            // Given
            setupMockModels(Map.of(
                    "model-1", 0.8,
                    "model-2", 1.0,
                    "model-3", 0.6,
                    "model-4", 0.9));

            // When - only use models 1 and 3
            List<ModelResult<TestResponse>> results =
                    executor.executeLlm(List.of("model-1", "model-3"), "test", TestResponse.class);

            // Then
            assertThat(results).hasSize(2);
            assertThat(results).extracting(ModelResult::modelId).containsExactlyInAnyOrder("model-1", "model-3");
        }

        @Test
        @DisplayName("Should return empty list when custom model list is empty")
        void shouldReturnEmptyListWhenCustomModelListIsEmpty() {
            // Given
            setupMockModels(Map.of(
                    "model-1", 0.8,
                    "model-2", 1.0,
                    "model-3", 0.6));

            // When - explicit empty list means no models to execute on
            List<ModelResult<TestResponse>> results = executor.executeLlm(List.of(), "test", TestResponse.class);

            // Then - empty list = no models = empty result
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("Should execute on single model when only one specified")
        void shouldExecuteOnSingleModelWhenOnlyOneSpecified() {
            // Given
            setupMockModels(Map.of(
                    "model-1", 0.8,
                    "model-2", 1.0,
                    "model-3", 0.6));

            // When
            List<ModelResult<TestResponse>> results =
                    executor.executeLlm(List.of("model-2"), "test", TestResponse.class);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).modelId()).isEqualTo("model-2");
            assertThat(results.get(0).result().score()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("Single Model Execution")
    class SingleModelExecution {

        @Test
        @DisplayName("Should execute on single model and return result")
        void shouldExecuteOnSingleModelAndReturnResult() {
            // Given
            setupMockModels(Map.of("target-model", 0.75));

            // When
            ModelResult<TestResponse> result = executor.executeLlmOnModel("target-model", "prompt", TestResponse.class);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.modelId()).isEqualTo("target-model");
            assertThat(result.result().score()).isEqualTo(0.75);
        }

        @Test
        @DisplayName("Should return failure result when model fails")
        void shouldReturnFailureResultWhenModelFails() {
            // Given
            ChatClient failingClient = createFailingMockClient();
            when(chatClientStore.get("failing-model")).thenReturn(failingClient);

            // When
            ModelResult<TestResponse> result =
                    executor.executeLlmOnModel("failing-model", "prompt", TestResponse.class);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.error()).isNotNull();
            assertThat(result.result()).isNull();
        }
    }

    @Nested
    @DisplayName("Async Execution")
    class AsyncExecution {

        @Test
        @DisplayName("Should execute asynchronously on all models")
        void shouldExecuteAsyncOnAllModels() {
            // Given
            setupMockModels(Map.of("model-1", 0.8, "model-2", 0.9));

            // When
            List<ModelResult<TestResponse>> results =
                    executor.executeLlmAsync("test", TestResponse.class).join();

            // Then
            assertThat(results).hasSize(2);
            assertThat(results).allMatch(ModelResult::isSuccess);
        }

        @Test
        @DisplayName("Should execute asynchronously on single model")
        void shouldExecuteAsyncOnSingleModel() {
            // Given
            setupMockModels(Map.of("model-1", 0.8));

            // When
            ModelResult<TestResponse> result = executor.executeLlmOnModelAsync("model-1", "test", TestResponse.class)
                    .join();

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result().score()).isEqualTo(0.8);
        }
    }

    // ========== Helper Methods ==========

    private void setupMockModels(Map<String, Double> modelScores) {
        lenient().when(chatClientStore.getModelIds()).thenReturn(new ArrayList<>(modelScores.keySet()));
        modelScores.forEach((modelId, score) -> {
            ChatClient client = createMockClientWithScore(score);
            lenient().when(chatClientStore.get(modelId)).thenReturn(client);
        });
    }

    private ChatClient createMockClientWithScore(double score) {
        ChatClient client = mock(ChatClient.class);
        ChatClient.CallResponseSpec callSpec = mock(ChatClient.CallResponseSpec.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);

        lenient().when(client.prompt(any(String.class))).thenReturn(requestSpec);
        lenient().when(requestSpec.call()).thenReturn(callSpec);
        lenient().when(callSpec.entity(TestResponse.class)).thenReturn(new TestResponse(score));

        return client;
    }

    private ChatClient createFailingMockClient() {
        ChatClient client = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);

        when(client.prompt(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("Model error"));

        return client;
    }

    record TestResponse(double score) {}
}
