package ai.qa.solutions.metrics.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TextChunker Tests")
class TextChunkerTest {

    @Nested
    @DisplayName("splitIntoChunks")
    class SplitIntoChunksTests {

        @Test
        @DisplayName("Should return single chunk for short text")
        void shouldReturnSingleChunkForShortText() {
            final String text = "This is a short text.";
            final List<String> chunks = TextChunker.splitIntoChunks(text, 512);

            assertThat(chunks).hasSize(1);
            assertThat(chunks.get(0)).isEqualTo(text);
        }

        @Test
        @DisplayName("Should split by sentence boundaries")
        void shouldSplitBySentenceBoundaries() {
            // Each sentence is roughly 10-15 tokens (30-45 chars), so with a limit of 20 tokens (60 chars)
            // they should be split across multiple chunks
            final String text = "First sentence here. Second sentence here. Third sentence here. Fourth sentence here.";
            final int maxTokens = 20; // ~60 chars per chunk

            final List<String> chunks = TextChunker.splitIntoChunks(text, maxTokens);

            assertThat(chunks).hasSizeGreaterThan(1);
            // Each chunk should respect the token limit
            for (final String chunk : chunks) {
                assertThat(TextChunker.estimateTokens(chunk)).isLessThanOrEqualTo(maxTokens);
            }
        }

        @Test
        @DisplayName("Should handle sentence longer than limit")
        void shouldHandleSentenceLongerThanLimit() {
            // Create a single long sentence with no boundary
            final String longSentence = "a".repeat(300); // 100 tokens at 3 chars/token
            final int maxTokens = 30; // 90 chars per chunk

            final List<String> chunks = TextChunker.splitIntoChunks(longSentence, maxTokens);

            assertThat(chunks).hasSizeGreaterThan(1);
            // Verify hard-split: each chunk should be at most maxTokens * 3 characters
            for (final String chunk : chunks) {
                assertThat(chunk.length()).isLessThanOrEqualTo(maxTokens * 3);
            }
            // Reconstruct original
            final String reconstructed = String.join("", chunks);
            assertThat(reconstructed).isEqualTo(longSentence);
        }

        @Test
        @DisplayName("Should handle empty text")
        void shouldHandleEmptyText() {
            final List<String> chunks = TextChunker.splitIntoChunks("", 512);

            assertThat(chunks).hasSize(1);
            assertThat(chunks.get(0)).isEmpty();
        }

        @Test
        @DisplayName("Should handle null text")
        void shouldHandleNullText() {
            assertThatThrownBy(() -> TextChunker.splitIntoChunks(null, 512))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("text must not be null");
        }

        @Test
        @DisplayName("Should handle text with no sentence boundaries")
        void shouldHandleTextWithNoSentenceBoundaries() {
            // A long string without period, exclamation, question mark, or newline
            final String text = "word ".repeat(200).trim(); // ~334 chars = ~112 tokens
            final int maxTokens = 40; // 120 chars per chunk

            final List<String> chunks = TextChunker.splitIntoChunks(text, maxTokens);

            assertThat(chunks).hasSizeGreaterThan(1);
            // Hard-split should produce chunks within limit
            for (final String chunk : chunks) {
                assertThat(chunk.length()).isLessThanOrEqualTo(maxTokens * 3);
            }
        }

        @Test
        @DisplayName("Should respect maxTokensPerChunk")
        void shouldRespectMaxTokensPerChunk() {
            final String text = "First sentence. Second sentence. Third sentence. Fourth sentence. "
                    + "Fifth sentence. Sixth sentence. Seventh sentence. Eighth sentence.";
            final int maxTokens = 15;

            final List<String> chunks = TextChunker.splitIntoChunks(text, maxTokens);

            for (final String chunk : chunks) {
                assertThat(TextChunker.estimateTokens(chunk))
                        .as("Chunk '%s' should not exceed %d tokens", chunk, maxTokens)
                        .isLessThanOrEqualTo(maxTokens);
            }
        }

        @Test
        @DisplayName("Should handle Russian text")
        void shouldHandleRussianText() {
            final String russianText = "Первое предложение на русском языке. Второе предложение тоже на русском. "
                    + "Третье предложение здесь. Четвёртое предложение для теста.";
            final int maxTokens = 25; // ~75 chars

            final List<String> chunks = TextChunker.splitIntoChunks(russianText, maxTokens);

            assertThat(chunks).hasSizeGreaterThan(1);
            for (final String chunk : chunks) {
                assertThat(TextChunker.estimateTokens(chunk)).isLessThanOrEqualTo(maxTokens);
            }
        }

        @Test
        @DisplayName("splitIntoChunks with custom charsPerToken should produce more chunks for Cyrillic")
        void splitIntoChunks_withCustomCharsPerToken_producesMoreChunksForCyrillic() {
            // given
            final String cyrillicText = "Это предложение на русском языке. ".repeat(18); // ~612 chars
            final int maxTokens = 100;

            // when
            final List<String> withDefault = TextChunker.splitIntoChunks(cyrillicText, maxTokens);
            final List<String> withCustom = TextChunker.splitIntoChunks(cyrillicText, maxTokens, 2.0);

            // then
            assertThat(withCustom.size()).isGreaterThan(withDefault.size());
        }

        @Test
        @DisplayName("splitIntoChunks with custom charsPerToken should respect the token limit")
        void splitIntoChunks_withCustomCharsPerToken_respectsLimit() {
            // given
            final String text = "First sentence here. Second sentence here. Third sentence here. "
                    + "Fourth sentence here. Fifth sentence here. Sixth sentence here.";
            final int maxTokens = 50;
            final double charsPerToken = 2.0;

            // when
            final List<String> chunks = TextChunker.splitIntoChunks(text, maxTokens, charsPerToken);

            // then
            assertThat(chunks).isNotEmpty();
            for (final String chunk : chunks) {
                // With charsPerToken=2.0, each token = 2 chars, so maxChars = 100
                assertThat(chunk.length())
                        .as("Chunk '%s' should respect token limit with charsPerToken=%s", chunk, charsPerToken)
                        .isLessThanOrEqualTo((int) Math.ceil(maxTokens * charsPerToken));
            }
        }
    }

    @Nested
    @DisplayName("estimateTokens")
    class EstimateTokensTests {

        @Test
        @DisplayName("Should estimate tokens correctly using chars/3 heuristic")
        void shouldEstimateTokensCorrectly() {
            // 9 chars / 3.0 = 3.0 tokens -> ceil = 3
            assertThat(TextChunker.estimateTokens("123456789")).isEqualTo(3);

            // 10 chars / 3.0 = 3.33 tokens -> ceil = 4
            assertThat(TextChunker.estimateTokens("1234567890")).isEqualTo(4);

            // 3 chars / 3.0 = 1.0 -> ceil = 1
            assertThat(TextChunker.estimateTokens("abc")).isEqualTo(1);

            // 1 char / 3.0 = 0.33 -> ceil = 1
            assertThat(TextChunker.estimateTokens("a")).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return 0 for empty text")
        void shouldReturn0ForEmptyText() {
            assertThat(TextChunker.estimateTokens("")).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return 0 for null text")
        void shouldReturn0ForNullText() {
            assertThat(TextChunker.estimateTokens(null)).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("truncateToTokenLimit")
    class TruncateTests {

        @Test
        @DisplayName("Should truncate to token limit")
        void shouldTruncateToTokenLimit() {
            final String longText = "a".repeat(300); // 100 tokens
            final int maxTokens = 10; // 30 chars

            final String truncated = TextChunker.truncateToTokenLimit(longText, maxTokens);

            assertThat(truncated).hasSize(30);
            assertThat(TextChunker.estimateTokens(truncated)).isLessThanOrEqualTo(maxTokens);
        }

        @Test
        @DisplayName("Should return original when within limit")
        void shouldReturnOriginalWhenWithinLimit() {
            final String shortText = "Short text.";
            final int maxTokens = 512;

            final String result = TextChunker.truncateToTokenLimit(shortText, maxTokens);

            assertThat(result).isSameAs(shortText);
        }

        @Test
        @DisplayName("Should return null for null input")
        void shouldReturnNullForNullInput() {
            final String result = TextChunker.truncateToTokenLimit(null, 512);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("truncateToTokenLimit with custom charsPerToken should truncate shorter")
        void truncateToTokenLimit_withCustomCharsPerToken_truncatesShorter() {
            // given
            final String longText = "a".repeat(300);
            final int maxTokens = 10;

            // when
            final String withDefault = TextChunker.truncateToTokenLimit(longText, maxTokens);
            final String withCustom = TextChunker.truncateToTokenLimit(longText, maxTokens, 2.0);

            // then
            assertThat(withCustom.length()).isLessThanOrEqualTo(withDefault.length());
            assertThat(withDefault).hasSize(30);
            assertThat(withCustom).hasSize(20);
        }
    }

    @Nested
    @DisplayName("averageEmbeddings")
    class AverageEmbeddingsTests {

        @Test
        @DisplayName("Should average embeddings correctly")
        void shouldAverageEmbeddingsCorrectly() {
            final double[] emb1 = {1.0, 2.0, 3.0};
            final double[] emb2 = {3.0, 4.0, 5.0};

            final double[] averaged = TextChunker.averageEmbeddings(List.of(emb1, emb2));

            assertThat(averaged).hasSize(3);
            assertThat(averaged[0]).isCloseTo(2.0, within(0.001));
            assertThat(averaged[1]).isCloseTo(3.0, within(0.001));
            assertThat(averaged[2]).isCloseTo(4.0, within(0.001));
        }

        @Test
        @DisplayName("Should return single embedding as-is")
        void shouldReturnSingleEmbeddingAsIs() {
            final double[] emb = {1.0, 2.0, 3.0};

            final double[] result = TextChunker.averageEmbeddings(List.of(emb));

            assertThat(result).isSameAs(emb);
        }

        @Test
        @DisplayName("Should throw for empty list")
        void shouldThrowForEmptyList() {
            assertThatThrownBy(() -> TextChunker.averageEmbeddings(List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be null or empty");
        }

        @Test
        @DisplayName("Should throw for null list")
        void shouldThrowForNullList() {
            assertThatThrownBy(() -> TextChunker.averageEmbeddings(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be null or empty");
        }

        @Test
        @DisplayName("Should throw for mismatched dimensions")
        void shouldThrowForMismatchedDimensions() {
            final double[] emb1 = {1.0, 2.0};
            final double[] emb2 = {1.0, 2.0, 3.0};

            assertThatThrownBy(() -> TextChunker.averageEmbeddings(List.of(emb1, emb2)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("same dimension");
        }
    }
}
