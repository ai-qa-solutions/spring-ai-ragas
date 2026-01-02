package ai.qa.solutions.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.qa.solutions.chatclient.ChatClientStore;
import ai.qa.solutions.execution.MultiModelExecutor.ExecutionRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
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
        @DisplayName("Should execute on all models and aggregate scores")
        void shouldExecuteOnAllModelsAndAggregateScores() {
            // Given
            setupMockModels(Map.of(
                    "model-1", 0.8,
                    "model-2", 1.0,
                    "model-3", 0.6));

            ExecutionRequest<TestResponse> request = ExecutionRequest.<TestResponse>builder()
                    .metricName("TestMetric")
                    .prompt("test prompt")
                    .responseType(TestResponse.class)
                    .scoreExtractor(TestResponse::score)
                    .build();

            // When
            Double result = executor.execute(request).join();

            // Then
            assertThat(result)
                    .isCloseTo(0.8, org.assertj.core.api.Assertions.within(0.001)); // Average of 0.8, 1.0, 0.6
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

            ExecutionRequest<TestResponse> request = ExecutionRequest.<TestResponse>builder()
                    .metricName("TestMetric")
                    .prompt("test")
                    .responseType(TestResponse.class)
                    .scoreExtractor(TestResponse::score)
                    .build();

            // When
            Double result = executor.execute(request).join();

            // Then
            assertThat(result).isEqualTo(0.8); // Only successful model
        }

        @Test
        @DisplayName("Should fail when all models fail")
        void shouldFailWhenAllModelsFail() {
            // Given
            ChatClient failingClient = createFailingMockClient();
            when(chatClientStore.getModelIds()).thenReturn(List.of("model-1"));
            when(chatClientStore.get("model-1")).thenReturn(failingClient);

            ExecutionRequest<TestResponse> request = ExecutionRequest.<TestResponse>builder()
                    .metricName("TestMetric")
                    .prompt("test")
                    .responseType(TestResponse.class)
                    .scoreExtractor(TestResponse::score)
                    .build();

            // When/Then
            assertThatThrownBy(() -> executor.execute(request).join())
                    .isInstanceOf(CompletionException.class)
                    .hasCauseInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("All 1 models failed");
        }

        @Test
        @DisplayName("Should fail when no models configured")
        void shouldFailWhenNoModelsConfigured() {
            // Given
            when(chatClientStore.getModelIds()).thenReturn(List.of());

            ExecutionRequest<TestResponse> request = ExecutionRequest.<TestResponse>builder()
                    .metricName("TestMetric")
                    .prompt("test")
                    .responseType(TestResponse.class)
                    .scoreExtractor(TestResponse::score)
                    .build();

            // When/Then
            assertThatThrownBy(() -> executor.execute(request).join())
                    .isInstanceOf(CompletionException.class)
                    .hasCauseInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No models configured");
        }
    }

    @Nested
    @DisplayName("Custom Aggregators")
    class CustomAggregators {

        @Test
        @DisplayName("Should use MIN aggregator")
        void shouldUseMinAggregator() {
            // Given
            setupMockModels(Map.of("m1", 0.8, "m2", 0.5, "m3", 0.9));
            ExecutionRequest<TestResponse> request = createTestRequest();

            // When
            Double result = executor.execute(request, ScoreAggregator.MIN).join();

            // Then
            assertThat(result).isEqualTo(0.5);
        }

        @Test
        @DisplayName("Should use MAX aggregator")
        void shouldUseMaxAggregator() {
            // Given
            setupMockModels(Map.of("m1", 0.8, "m2", 0.5, "m3", 0.9));
            ExecutionRequest<TestResponse> request = createTestRequest();

            // When
            Double result = executor.execute(request, ScoreAggregator.MAX).join();

            // Then
            assertThat(result).isEqualTo(0.9);
        }

        @Test
        @DisplayName("Should use MEDIAN aggregator")
        void shouldUseMedianAggregator() {
            // Given
            setupMockModels(Map.of("m1", 0.1, "m2", 0.5, "m3", 0.9));
            ExecutionRequest<TestResponse> request = createTestRequest();

            // When
            Double result = executor.execute(request, ScoreAggregator.MEDIAN).join();

            // Then
            assertThat(result).isEqualTo(0.5);
        }

        @Test
        @DisplayName("Should use consensus aggregator when models agree")
        void shouldUseConsensusAggregatorWhenModelsAgree() {
            // Given
            setupMockModels(Map.of("m1", 0.79, "m2", 0.80, "m3", 0.81));
            ExecutionRequest<TestResponse> request = createTestRequest();

            // When
            Double result =
                    executor.execute(request, ScoreAggregator.consensus(0.1)).join();

            // Then
            assertThat(result).isCloseTo(0.8, org.assertj.core.api.Assertions.within(0.001));
        }

        @Test
        @DisplayName("Should fail consensus when models disagree")
        void shouldFailConsensusWhenModelsDisagree() {
            // Given
            setupMockModels(Map.of("m1", 0.1, "m2", 0.9));
            ExecutionRequest<TestResponse> request = createTestRequest();

            // When/Then
            assertThatThrownBy(() -> executor.execute(request, ScoreAggregator.consensus(0.1))
                            .join())
                    .isInstanceOf(CompletionException.class)
                    .hasCauseInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No consensus");
        }
    }

    @Nested
    @DisplayName("Listeners")
    class Listeners {

        @Test
        @DisplayName("Should notify listeners in order")
        void shouldNotifyListenersInOrder() {
            // Given
            setupMockModels(Map.of("m1", 0.8));
            List<String> callOrder = new ArrayList<>();

            ModelExecutionListener firstListener = new ModelExecutionListener() {
                @Override
                public void beforeExecution(ModelExecutionContext context) {
                    callOrder.add("first-before");
                }

                @Override
                public void afterExecution(ModelExecutionResult result) {
                    callOrder.add("first-after");
                }

                @Override
                public void afterAggregation(AggregatedExecutionResult result) {
                    callOrder.add("first-aggregation");
                }

                @Override
                public int getOrder() {
                    return 1;
                }
            };

            ModelExecutionListener secondListener = new ModelExecutionListener() {
                @Override
                public void beforeExecution(ModelExecutionContext context) {
                    callOrder.add("second-before");
                }

                @Override
                public void afterExecution(ModelExecutionResult result) {
                    callOrder.add("second-after");
                }

                @Override
                public void afterAggregation(AggregatedExecutionResult result) {
                    callOrder.add("second-aggregation");
                }

                @Override
                public int getOrder() {
                    return 2;
                }
            };

            executor.addListener(secondListener).addListener(firstListener);

            // When
            executor.execute(createTestRequest()).join();

            // Then
            assertThat(callOrder)
                    .containsExactly(
                            "first-before",
                            "second-before",
                            "first-after",
                            "second-after",
                            "first-aggregation",
                            "second-aggregation");
        }

        @Test
        @DisplayName("Should provide correct context to listeners")
        void shouldProvideCorrectContextToListeners() {
            // Given
            setupMockModels(Map.of("test-model", 0.7));
            AtomicInteger beforeCalls = new AtomicInteger();
            AtomicInteger afterCalls = new AtomicInteger();

            ModelExecutionListener listener = new ModelExecutionListener() {
                @Override
                public void beforeExecution(ModelExecutionContext context) {
                    beforeCalls.incrementAndGet();
                    assertThat(context.getModelId()).isEqualTo("test-model");
                    assertThat(context.getMetricName()).isEqualTo("TestMetric");
                    assertThat(context.getPrompt()).isEqualTo("test prompt");
                    assertThat(context.getExecutionId()).isNotBlank();
                }

                @Override
                public void afterExecution(ModelExecutionResult result) {
                    afterCalls.incrementAndGet();
                    assertThat(result.isSuccess()).isTrue();
                    assertThat(result.getScore()).contains(0.7);
                    assertThat(result.getDuration()).isNotNull();
                }

                @Override
                public void afterAggregation(AggregatedExecutionResult result) {
                    assertThat(result.getAggregatedScore()).isEqualTo(0.7);
                    assertThat(result.getSuccessRate()).isEqualTo(1.0);
                    assertThat(result.getResults()).hasSize(1);
                }
            };

            executor.addListener(listener);

            // When
            executor.execute(createTestRequest()).join();

            // Then
            assertThat(beforeCalls.get()).isEqualTo(1);
            assertThat(afterCalls.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should handle listener exceptions gracefully")
        void shouldHandleListenerExceptionsGracefully() {
            // Given
            setupMockModels(Map.of("m1", 0.8));

            ModelExecutionListener faultyListener = new ModelExecutionListener() {
                @Override
                public void beforeExecution(ModelExecutionContext context) {
                    throw new RuntimeException("Listener error");
                }
            };

            executor.addListener(faultyListener);

            // When
            Double result = executor.execute(createTestRequest()).join();

            // Then - should still succeed
            assertThat(result).isEqualTo(0.8);
        }

        @Test
        @DisplayName("Should support removing listeners")
        void shouldSupportRemovingListeners() {
            // Given
            setupMockModels(Map.of("m1", 0.8));
            AtomicInteger callCount = new AtomicInteger();

            ModelExecutionListener listener = new ModelExecutionListener() {
                @Override
                public void beforeExecution(ModelExecutionContext context) {
                    callCount.incrementAndGet();
                }
            };

            executor.addListener(listener);
            executor.removeListener(listener);

            // When
            executor.execute(createTestRequest()).join();

            // Then
            assertThat(callCount.get()).isZero();
        }
    }

    // ========== Helper Methods ==========

    private void setupMockModels(Map<String, Double> modelScores) {
        when(chatClientStore.getModelIds()).thenReturn(new ArrayList<>(modelScores.keySet()));
        modelScores.forEach((modelId, score) -> {
            ChatClient client = createMockClientWithScore(score);
            when(chatClientStore.get(modelId)).thenReturn(client);
        });
    }

    private ChatClient createMockClientWithScore(double score) {
        ChatClient client = mock(ChatClient.class);
        ChatClient.CallResponseSpec callSpec = mock(ChatClient.CallResponseSpec.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);

        when(client.prompt(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.entity(TestResponse.class)).thenReturn(new TestResponse(score));

        return client;
    }

    private ChatClient createFailingMockClient() {
        ChatClient client = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);

        when(client.prompt(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("Model error"));

        return client;
    }

    private ExecutionRequest<TestResponse> createTestRequest() {
        return ExecutionRequest.<TestResponse>builder()
                .metricName("TestMetric")
                .prompt("test prompt")
                .responseType(TestResponse.class)
                .scoreExtractor(TestResponse::score)
                .build();
    }

    record TestResponse(double score) {}
}
