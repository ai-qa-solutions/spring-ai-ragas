package ai.qa.solutions.embedding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

/**
 * Factory for creating {@link EmbeddingModel} instances with custom options.
 * <p>
 * Creates wrappers around a base model, overriding options (model ID, dimensions)
 * without the need to create new API connections.
 *
 * @see DelegatingEmbeddingModel
 */
@Slf4j
public class EmbeddingModelFactory {

    /**
     * Creates an EmbeddingModel with overridden options for a specific model.
     *
     * @param baseModel base model for delegating calls
     * @param modelId model ID to use in requests
     * @param dimensions vector dimensionality (can be null to use base model's default)
     * @return delegating EmbeddingModel with overridden options
     */
    public EmbeddingModel create(final EmbeddingModel baseModel, final String modelId, final Integer dimensions) {
        return new DelegatingEmbeddingModel(baseModel, modelId, dimensions);
    }

    /**
     * Delegating implementation of {@link EmbeddingModel} that overrides model ID and dimensions.
     * <p>
     * All calls are forwarded to the base model, but with modified options in the request.
     * Allows using a single API connection to work with different models.
     */
    private static class DelegatingEmbeddingModel implements EmbeddingModel {

        /**
         * Base model to which all calls are delegated.
         */
        private final EmbeddingModel delegate;

        /**
         * Model ID to use in requests.
         */
        private final String modelId;

        /**
         * Dimensionality of the vector representation.
         */
        private final Integer dimensions;

        /**
         * Creates a new delegating EmbeddingModel.
         *
         * @param delegate base model
         * @param modelId model ID to substitute in requests
         * @param dimensions vector dimensionality
         */
        public DelegatingEmbeddingModel(final EmbeddingModel delegate, final String modelId, final Integer dimensions) {
            this.delegate = delegate;
            this.modelId = modelId;
            this.dimensions = dimensions;
        }

        /**
         * Executes a vectorization request with overridden model options.
         * <p>
         * Creates a new request with substituted model ID and dimensions,
         * merging them with the original options from the request.
         *
         * @param request original vectorization request
         * @return response from the base model
         */
        @Override
        public EmbeddingResponse call(final EmbeddingRequest request) {
            final EmbeddingOptions originalOptions = request.getOptions();
            final EmbeddingOptions newOptions = EmbeddingOptions.builder()
                    .model(modelId)
                    .dimensions(dimensions)
                    .build();

            final EmbeddingOptions mergedOptions =
                    originalOptions != null ? mergeOptions(originalOptions, newOptions) : newOptions;

            final EmbeddingRequest newRequest = new EmbeddingRequest(request.getInstructions(), mergedOptions);

            return delegate.call(newRequest);
        }

        /**
         * Vectorizes a document by delegating the call to the base model.
         *
         * @param document document to vectorize
         * @return vector representation of the document
         */
        @Override
        public float[] embed(final Document document) {
            return delegate.embed(document);
        }

        /**
         * Returns the dimensionality of the vectors.
         * <p>
         * If dimensions are set in the constructor, returns them,
         * otherwise queries the base model.
         *
         * @return dimensionality of the vector representation
         */
        @Override
        public int dimensions() {
            return dimensions != null ? dimensions : delegate.dimensions();
        }

        /**
         * Merges original options with overriding ones.
         * <p>
         * Priority is given to values from {@code override}.
         *
         * @param original original options from the request
         * @param override overriding options
         * @return merged options
         */
        private EmbeddingOptions mergeOptions(final EmbeddingOptions original, final EmbeddingOptions override) {
            return EmbeddingOptions.builder()
                    .model(override.getModel() != null ? override.getModel() : original.getModel())
                    .dimensions(override.getDimensions() != null ? override.getDimensions() : original.getDimensions())
                    .build();
        }
    }
}
