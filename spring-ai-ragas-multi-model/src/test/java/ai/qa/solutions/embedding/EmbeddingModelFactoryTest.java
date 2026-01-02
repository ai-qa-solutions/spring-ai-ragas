package ai.qa.solutions.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

@DisplayName("EmbeddingModelFactory Unit Tests")
class EmbeddingModelFactoryTest {

    private EmbeddingModelFactory factory;
    private EmbeddingModel baseMock;

    @BeforeEach
    void setUp() {
        factory = new EmbeddingModelFactory();
        baseMock = mock(EmbeddingModel.class);
    }

    @Test
    @DisplayName("Should create model with default options")
    void shouldCreateModelWithDefaultOptions() {
        // Given
        final EmbeddingModelAutoConfiguration.EmbeddingModelProperties properties =
                new EmbeddingModelAutoConfiguration.EmbeddingModelProperties();
        properties.getDefaultOptions().setDimensions(1024);

        final EmbeddingModelAutoConfiguration.EmbeddingModelProperties.ModelConfig config =
                new EmbeddingModelAutoConfiguration.EmbeddingModelProperties.ModelConfig();
        config.setId("test-model");
        config.setOptions(null); // Используем default options

        // When
        final EmbeddingModel model = factory.create(baseMock, config, properties);

        // Then
        assertThat(model).isNotNull();
        assertThat(model.dimensions()).isEqualTo(1024);
    }

    @Test
    @DisplayName("Should create model with custom options")
    void shouldCreateModelWithCustomOptions() {
        // Given
        final EmbeddingModelAutoConfiguration.EmbeddingModelProperties properties =
                new EmbeddingModelAutoConfiguration.EmbeddingModelProperties();
        properties.getDefaultOptions().setDimensions(1024);

        final EmbeddingModelAutoConfiguration.EmbeddingModelProperties.ModelConfig config =
                new EmbeddingModelAutoConfiguration.EmbeddingModelProperties.ModelConfig();
        config.setId("test-model");

        final EmbeddingModelAutoConfiguration.EmbeddingModelProperties.ModelOptions customOptions =
                new EmbeddingModelAutoConfiguration.EmbeddingModelProperties.ModelOptions();
        customOptions.setDimensions(512);
        config.setOptions(customOptions);

        // When
        final EmbeddingModel model = factory.create(baseMock, config, properties);

        // Then
        assertThat(model).isNotNull();
        assertThat(model.dimensions()).isEqualTo(512);
    }

    @Test
    @DisplayName("Should delegate call to base model")
    void shouldDelegateCallToBaseModel() {
        // Given
        final EmbeddingModelAutoConfiguration.EmbeddingModelProperties properties =
                new EmbeddingModelAutoConfiguration.EmbeddingModelProperties();

        final EmbeddingModelAutoConfiguration.EmbeddingModelProperties.ModelConfig config =
                new EmbeddingModelAutoConfiguration.EmbeddingModelProperties.ModelConfig();
        config.setId("test-model");

        final EmbeddingModel model = factory.create(baseMock, config, properties);

        final EmbeddingRequest request = new EmbeddingRequest(
                List.of("test text"), EmbeddingOptions.builder().build());

        final EmbeddingResponse mockResponse =
                new EmbeddingResponse(List.of(new Embedding(new float[] {1.0f, 2.0f}, 0)));
        when(baseMock.call(any(EmbeddingRequest.class))).thenReturn(mockResponse);

        // When
        final EmbeddingResponse response = model.call(request);

        // Then
        assertThat(response).isNotNull();
        verify(baseMock).call(any(EmbeddingRequest.class));
    }

    @Test
    @DisplayName("Should override model ID in request")
    void shouldOverrideModelIdInRequest() {
        // Given
        final EmbeddingModelAutoConfiguration.EmbeddingModelProperties properties =
                new EmbeddingModelAutoConfiguration.EmbeddingModelProperties();

        final EmbeddingModelAutoConfiguration.EmbeddingModelProperties.ModelConfig config =
                new EmbeddingModelAutoConfiguration.EmbeddingModelProperties.ModelConfig();
        config.setId("custom-model-id");

        final EmbeddingModel model = factory.create(baseMock, config, properties);

        final EmbeddingRequest originalRequest = new EmbeddingRequest(
                List.of("test"),
                EmbeddingOptions.builder().model("original-model").build());

        final EmbeddingResponse mockResponse = new EmbeddingResponse(List.of(new Embedding(new float[] {1.0f}, 0)));

        when(baseMock.call(any(EmbeddingRequest.class))).thenAnswer(invocation -> {
            final EmbeddingRequest capturedRequest = invocation.getArgument(0);
            // Проверяем, что modelId был переопределен
            assertThat(capturedRequest.getOptions().getModel()).isEqualTo("custom-model-id");
            return mockResponse;
        });

        // When
        model.call(originalRequest);

        // Then - проверка в when блоке через answer
        verify(baseMock).call(any(EmbeddingRequest.class));
    }

    @Test
    @DisplayName("Should delegate embed(Document) to base model")
    void shouldDelegateEmbedDocumentToBaseModel() {
        // Given
        final EmbeddingModelAutoConfiguration.EmbeddingModelProperties properties =
                new EmbeddingModelAutoConfiguration.EmbeddingModelProperties();

        final EmbeddingModelAutoConfiguration.EmbeddingModelProperties.ModelConfig config =
                new EmbeddingModelAutoConfiguration.EmbeddingModelProperties.ModelConfig();
        config.setId("test-model");

        final EmbeddingModel model = factory.create(baseMock, config, properties);

        final Document document = new Document("test content");
        final float[] expectedEmbedding = new float[] {1.0f, 2.0f, 3.0f};
        when(baseMock.embed(document)).thenReturn(expectedEmbedding);

        // When
        final float[] result = model.embed(document);

        // Then
        assertThat(result).isEqualTo(expectedEmbedding);
        verify(baseMock).embed(document);
    }

    @Test
    @DisplayName("Should handle null options in request")
    void shouldHandleNullOptionsInRequest() {
        // Given
        final EmbeddingModelAutoConfiguration.EmbeddingModelProperties properties =
                new EmbeddingModelAutoConfiguration.EmbeddingModelProperties();
        properties.getDefaultOptions().setDimensions(1024);

        final EmbeddingModelAutoConfiguration.EmbeddingModelProperties.ModelConfig config =
                new EmbeddingModelAutoConfiguration.EmbeddingModelProperties.ModelConfig();
        config.setId("test-model");

        final EmbeddingModel model = factory.create(baseMock, config, properties);

        final EmbeddingRequest requestWithNullOptions = new EmbeddingRequest(List.of("test"), null);

        final EmbeddingResponse mockResponse = new EmbeddingResponse(List.of(new Embedding(new float[] {1.0f}, 0)));
        when(baseMock.call(any(EmbeddingRequest.class))).thenReturn(mockResponse);

        // When
        final EmbeddingResponse response = model.call(requestWithNullOptions);

        // Then
        assertThat(response).isNotNull();
        verify(baseMock).call(any(EmbeddingRequest.class));
    }

    @Test
    @DisplayName("Should return base model dimensions when not specified")
    void shouldReturnBaseModelDimensionsWhenNotSpecified() {
        // Given
        final EmbeddingModelAutoConfiguration.EmbeddingModelProperties properties =
                new EmbeddingModelAutoConfiguration.EmbeddingModelProperties();
        properties.getDefaultOptions().setDimensions(null);

        final EmbeddingModelAutoConfiguration.EmbeddingModelProperties.ModelConfig config =
                new EmbeddingModelAutoConfiguration.EmbeddingModelProperties.ModelConfig();
        config.setId("test-model");

        when(baseMock.dimensions()).thenReturn(768);

        final EmbeddingModel model = factory.create(baseMock, config, properties);

        // When
        final int dimensions = model.dimensions();

        // Then
        assertThat(dimensions).isEqualTo(768);
        verify(baseMock).dimensions();
    }
}
