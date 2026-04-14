package ai.qa.solutions.metrics.response;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.util.Assert;

/**
 * Utility for splitting text into token-limited chunks suitable for embedding models.
 * Uses sentence-boundary splitting and a simple character-based token estimation heuristic.
 */
final class TextChunker {

    /** Default ratio of characters to tokens used by the token estimation heuristic. */
    private static final double CHARS_PER_TOKEN = 3.0;

    /** Regex matching sentence boundaries: end punctuation followed by whitespace, or newline. */
    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("(?<=[.!?])\\s+|\\n");

    private TextChunker() {}

    /**
     * Splits text into chunks that each fit within the specified token limit.
     *
     * @param text the text to split
     * @param maxTokensPerChunk maximum tokens per chunk
     * @return list of text chunks, never empty
     * @throws IllegalArgumentException if text is null
     */
    static List<String> splitIntoChunks(final String text, final int maxTokensPerChunk) {
        return splitIntoChunks(text, maxTokensPerChunk, CHARS_PER_TOKEN);
    }

    /**
     * Splits text into chunks that each fit within the specified token limit, using the
     * provided characters-per-token ratio.
     *
     * @param text the text to split
     * @param maxTokensPerChunk maximum tokens per chunk
     * @param charsPerToken ratio of characters to tokens used for estimation
     * @return list of text chunks, never empty
     * @throws IllegalArgumentException if text is null
     */
    static List<String> splitIntoChunks(final String text, final int maxTokensPerChunk, final double charsPerToken) {
        Assert.notNull(text, "text must not be null");

        if (text.isEmpty()) {
            return List.of("");
        }

        if (estimateTokens(text, charsPerToken) <= maxTokensPerChunk) {
            return List.of(text);
        }

        final int maxChars = (int) Math.ceil(maxTokensPerChunk * charsPerToken);
        final String[] sentences = SENTENCE_BOUNDARY.split(text);
        final List<String> chunks = new ArrayList<>();
        final StringBuilder currentChunk = new StringBuilder();

        for (final String sentence : sentences) {
            processSentence(sentence, maxTokensPerChunk, charsPerToken, maxChars, chunks, currentChunk);
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString());
        }

        return chunks;
    }

    /**
     * Processes a single sentence: either hard-splits it if oversized, or appends it to the
     * current chunk, flushing the chunk when the token limit would be exceeded.
     */
    private static void processSentence(
            final String sentence,
            final int maxTokensPerChunk,
            final double charsPerToken,
            final int maxChars,
            final List<String> chunks,
            final StringBuilder currentChunk) {
        if (sentence.isEmpty()) {
            return;
        }

        if (estimateTokens(sentence, charsPerToken) > maxTokensPerChunk) {
            flushCurrentChunk(chunks, currentChunk);
            chunks.addAll(hardSplitOversizedSentence(sentence, maxChars));
            return;
        }

        appendSentenceToChunk(sentence, maxTokensPerChunk, charsPerToken, chunks, currentChunk);
    }

    /**
     * Appends a sentence to the current chunk, flushing the chunk first if adding the sentence
     * would exceed the token limit.
     */
    private static void appendSentenceToChunk(
            final String sentence,
            final int maxTokensPerChunk,
            final double charsPerToken,
            final List<String> chunks,
            final StringBuilder currentChunk) {
        final String candidateAddition = currentChunk.isEmpty() ? sentence : " " + sentence;
        final boolean wouldExceed =
                estimateTokens(currentChunk.toString() + candidateAddition, charsPerToken) > maxTokensPerChunk;

        if (wouldExceed && !currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString());
            currentChunk.setLength(0);
            currentChunk.append(sentence);
            return;
        }

        if (!currentChunk.isEmpty()) {
            currentChunk.append(' ');
        }
        currentChunk.append(sentence);
    }

    /** Flushes the current chunk buffer into the chunks list if non-empty. */
    private static void flushCurrentChunk(final List<String> chunks, final StringBuilder currentChunk) {
        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString());
            currentChunk.setLength(0);
        }
    }

    /**
     * Hard-splits a sentence that exceeds the token limit into character-boundary slices
     * of at most {@code maxChars} characters.
     */
    private static List<String> hardSplitOversizedSentence(final String sentence, final int maxChars) {
        final List<String> parts = new ArrayList<>();
        for (int i = 0; i < sentence.length(); i += maxChars) {
            final String part = sentence.substring(i, Math.min(i + maxChars, sentence.length()));
            if (!part.isEmpty()) {
                parts.add(part);
            }
        }
        return parts;
    }

    /**
     * Estimates the number of tokens in the given text using a character-based heuristic.
     *
     * @param text the text to estimate
     * @return estimated token count, 0 for empty text
     */
    static int estimateTokens(final String text) {
        return estimateTokens(text, CHARS_PER_TOKEN);
    }

    /**
     * Estimates the number of tokens in the given text using the supplied characters-per-token ratio.
     *
     * @param text the text to estimate
     * @param charsPerToken ratio of characters to tokens used for estimation
     * @return estimated token count, 0 for empty text
     */
    private static int estimateTokens(final String text, final double charsPerToken) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return (int) Math.ceil(text.length() / charsPerToken);
    }

    /**
     * Truncates text to fit within the specified token limit.
     *
     * @param text the text to truncate
     * @param maxTokens maximum number of tokens
     * @return the original text if it fits, otherwise truncated
     */
    static String truncateToTokenLimit(final String text, final int maxTokens) {
        return truncateToTokenLimit(text, maxTokens, CHARS_PER_TOKEN);
    }

    /**
     * Truncates text to fit within the specified token limit using the supplied characters-per-token ratio.
     *
     * @param text the text to truncate
     * @param maxTokens maximum number of tokens
     * @param charsPerToken ratio of characters to tokens used for estimation
     * @return the original text if it fits, otherwise truncated
     */
    static String truncateToTokenLimit(final String text, final int maxTokens, final double charsPerToken) {
        if (text == null || estimateTokens(text, charsPerToken) <= maxTokens) {
            return text;
        }
        final int maxChars = (int) Math.ceil(maxTokens * charsPerToken);
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
        Assert.notEmpty(embeddings, "embeddings must not be null or empty");

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
