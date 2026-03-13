package ai.qa.solutions.metrics.response;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Utility for splitting text into token-limited chunks suitable for embedding models.
 * Uses sentence-boundary splitting and a simple character-based token estimation heuristic.
 */
class TextChunker {

    private static final double CHARS_PER_TOKEN = 3.0;
    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("(?<=[.!?])\\s+|\\n");

    private TextChunker() {}

    /**
     * Splits text into chunks that each fit within the specified token limit.
     *
     * @param text the text to split
     * @param maxTokensPerChunk maximum tokens per chunk
     * @return list of text chunks, never empty
     * @throws NullPointerException if text is null
     */
    static List<String> splitIntoChunks(final String text, final int maxTokensPerChunk) {
        Objects.requireNonNull(text, "text must not be null");

        if (text.isEmpty()) {
            return List.of("");
        }

        if (estimateTokens(text) <= maxTokensPerChunk) {
            return List.of(text);
        }

        final String[] sentences = SENTENCE_BOUNDARY.split(text);
        final List<String> chunks = new ArrayList<>();
        final StringBuilder currentChunk = new StringBuilder();

        for (final String sentence : sentences) {
            if (sentence.isEmpty()) {
                continue;
            }

            if (estimateTokens(sentence) > maxTokensPerChunk) {
                // Flush current chunk before hard-splitting
                if (!currentChunk.isEmpty()) {
                    chunks.add(currentChunk.toString());
                    currentChunk.setLength(0);
                }
                // Hard-split oversized sentence at character boundary
                final int maxChars = maxTokensPerChunk * 3;
                for (int i = 0; i < sentence.length(); i += maxChars) {
                    final String part = sentence.substring(i, Math.min(i + maxChars, sentence.length()));
                    if (!part.isEmpty()) {
                        chunks.add(part);
                    }
                }
                continue;
            }

            final String candidateAddition = currentChunk.isEmpty() ? sentence : " " + sentence;
            if (estimateTokens(currentChunk.toString() + candidateAddition) > maxTokensPerChunk
                    && !currentChunk.isEmpty()) {
                chunks.add(currentChunk.toString());
                currentChunk.setLength(0);
                currentChunk.append(sentence);
            } else {
                if (!currentChunk.isEmpty()) {
                    currentChunk.append(" ");
                }
                currentChunk.append(sentence);
            }
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString());
        }

        return chunks;
    }

    /**
     * Estimates the number of tokens in the given text using a character-based heuristic.
     *
     * @param text the text to estimate
     * @return estimated token count, 0 for empty text
     */
    static int estimateTokens(final String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return (int) Math.ceil(text.length() / CHARS_PER_TOKEN);
    }

    /**
     * Truncates text to fit within the specified token limit.
     *
     * @param text the text to truncate
     * @param maxTokens maximum number of tokens
     * @return the original text if it fits, otherwise truncated
     */
    static String truncateToTokenLimit(final String text, final int maxTokens) {
        if (text == null || estimateTokens(text) <= maxTokens) {
            return text;
        }
        final int maxChars = maxTokens * 3;
        return text.substring(0, Math.min(maxChars, text.length()));
    }

    /**
     * Computes the element-wise average of multiple embedding vectors.
     *
     * @param embeddings list of embedding vectors (all must have the same dimension)
     * @return averaged embedding vector
     * @throws IllegalArgumentException if embeddings is empty or vectors have different dimensions
     */
    static double[] averageEmbeddings(final List<double[]> embeddings) {
        if (embeddings == null || embeddings.isEmpty()) {
            throw new IllegalArgumentException("embeddings must not be null or empty");
        }

        if (embeddings.size() == 1) {
            return embeddings.get(0);
        }

        final int dimension = embeddings.get(0).length;
        final double[] result = new double[dimension];

        for (final double[] embedding : embeddings) {
            if (embedding.length != dimension) {
                throw new IllegalArgumentException("All embedding vectors must have the same dimension. Expected "
                        + dimension + " but got " + embedding.length);
            }
            for (int i = 0; i < dimension; i++) {
                result[i] += embedding[i];
            }
        }

        final int count = embeddings.size();
        for (int i = 0; i < dimension; i++) {
            result[i] /= count;
        }

        return result;
    }
}
