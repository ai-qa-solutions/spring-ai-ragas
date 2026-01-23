package ai.qa.solutions.metrics.nlp;

import ai.qa.solutions.metric.Metric;
import ai.qa.solutions.sample.Sample;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * chrF (Character n-gram F-score) Score Metric.
 * <p>
 * Evaluates text similarity using character-level n-grams, which makes it more robust
 * to morphological variations and typos than word-based metrics.
 * <p>
 * Supports two variants:
 * <ul>
 *   <li>chrF: Character n-grams only (when wordNgramOrder = 0)</li>
 *   <li>chrF++: Character n-grams + word n-grams (when wordNgramOrder > 0)</li>
 * </ul>
 * <p>
 * Returns scores in the range [0, 1], where:
 * <ul>
 *   <li>0 - No n-gram overlap with reference</li>
 *   <li>1 - Complete overlap with reference</li>
 * </ul>
 * <p>
 * Required sample fields:
 * <ul>
 *   <li>{@code response} - The generated text to evaluate</li>
 *   <li>{@code reference} - The reference (ground truth) text</li>
 * </ul>
 */
@Slf4j
public class ChrfScoreMetric implements Metric<ChrfScoreMetric.ChrfScoreConfig> {

    /**
     * Computes the chrF score for a single sample.
     *
     * @param config the metric configuration
     * @param sample the sample containing response and reference
     * @return the chrF score (0-1), or null if input is invalid
     */
    @Override
    public Double singleTurnScore(final ChrfScoreConfig config, final Sample sample) {
        // Validate input
        if (sample.getResponse() == null || sample.getResponse().isEmpty()) {
            log.warn("No response provided for chrF score evaluation");
            return null;
        }

        if (sample.getReference() == null || sample.getReference().isEmpty()) {
            log.warn("No reference provided for chrF score evaluation");
            return null;
        }

        final String response = sample.getResponse().toLowerCase();
        final String reference = sample.getReference().toLowerCase();

        final int charNgramOrder = config.getCharNgramOrder();
        final int wordNgramOrder = config.getWordNgramOrder();
        final double beta = config.getBeta();

        // Compute character n-gram F-score
        double charFScore = 0.0;
        int charNgramCount = 0;
        for (int n = 1; n <= charNgramOrder; n++) {
            final double fscore = computeCharNgramFScore(response, reference, n, beta);
            charFScore += fscore;
            charNgramCount++;
        }
        if (charNgramCount > 0) {
            charFScore /= charNgramCount;
        }

        // If wordNgramOrder > 0, compute word n-gram F-score (chrF++ mode)
        if (wordNgramOrder > 0) {
            final List<String> responseTokens = tokenize(response);
            final List<String> referenceTokens = tokenize(reference);

            double wordFScore = 0.0;
            int wordNgramCount = 0;
            for (int n = 1; n <= wordNgramOrder; n++) {
                final double fscore = computeWordNgramFScore(responseTokens, referenceTokens, n, beta);
                wordFScore += fscore;
                wordNgramCount++;
            }
            if (wordNgramCount > 0) {
                wordFScore /= wordNgramCount;
            }

            // Combine character and word F-scores (equal weight)
            return (charFScore + wordFScore) / 2.0;
        }

        return charFScore;
    }

    /**
     * Asynchronously computes the chrF score for a single sample.
     * <p>
     * Since this is a non-LLM computational metric, it executes synchronously
     * and wraps the result in a completed future.
     *
     * @param config the metric configuration
     * @param sample the sample containing response and reference
     * @return a CompletableFuture containing the chrF score
     */
    @Override
    public CompletableFuture<Double> singleTurnScoreAsync(final ChrfScoreConfig config, final Sample sample) {
        return CompletableFuture.completedFuture(singleTurnScore(config, sample));
    }

    @Override
    public String getName() {
        return "ChrfScoreMetric";
    }

    /**
     * Computes character n-gram F-score with configurable beta.
     *
     * @param response the response text
     * @param reference the reference text
     * @param n the n-gram size
     * @param beta the beta parameter (higher = more weight on recall)
     * @return the F-score for this n-gram size
     */
    private double computeCharNgramFScore(
            final String response, final String reference, final int n, final double beta) {

        final Map<String, Integer> responseNgrams = extractCharNgrams(response, n);
        final Map<String, Integer> referenceNgrams = extractCharNgrams(reference, n);

        if (referenceNgrams.isEmpty() && responseNgrams.isEmpty()) {
            return 1.0; // Both empty = perfect match
        }

        if (referenceNgrams.isEmpty() || responseNgrams.isEmpty()) {
            return 0.0;
        }

        // Count overlapping n-grams
        int overlap = 0;
        for (final Map.Entry<String, Integer> entry : referenceNgrams.entrySet()) {
            final String ngram = entry.getKey();
            final int refCount = entry.getValue();
            final int respCount = responseNgrams.getOrDefault(ngram, 0);
            overlap += Math.min(refCount, respCount);
        }

        final int totalRefNgrams =
                referenceNgrams.values().stream().mapToInt(Integer::intValue).sum();
        final int totalRespNgrams =
                responseNgrams.values().stream().mapToInt(Integer::intValue).sum();

        final double recall = totalRefNgrams > 0 ? (double) overlap / totalRefNgrams : 0.0;
        final double precision = totalRespNgrams > 0 ? (double) overlap / totalRespNgrams : 0.0;

        // Compute F-beta score
        if (precision + recall == 0) {
            return 0.0;
        }

        final double betaSquared = beta * beta;
        return (1 + betaSquared) * precision * recall / (betaSquared * precision + recall);
    }

    /**
     * Computes word n-gram F-score with configurable beta.
     *
     * @param response the response tokens
     * @param reference the reference tokens
     * @param n the n-gram size
     * @param beta the beta parameter
     * @return the F-score for this n-gram size
     */
    private double computeWordNgramFScore(
            final List<String> response, final List<String> reference, final int n, final double beta) {

        final Map<String, Integer> responseNgrams = extractWordNgrams(response, n);
        final Map<String, Integer> referenceNgrams = extractWordNgrams(reference, n);

        if (referenceNgrams.isEmpty() && responseNgrams.isEmpty()) {
            return 1.0;
        }

        if (referenceNgrams.isEmpty() || responseNgrams.isEmpty()) {
            return 0.0;
        }

        // Count overlapping n-grams
        int overlap = 0;
        for (final Map.Entry<String, Integer> entry : referenceNgrams.entrySet()) {
            final String ngram = entry.getKey();
            final int refCount = entry.getValue();
            final int respCount = responseNgrams.getOrDefault(ngram, 0);
            overlap += Math.min(refCount, respCount);
        }

        final int totalRefNgrams =
                referenceNgrams.values().stream().mapToInt(Integer::intValue).sum();
        final int totalRespNgrams =
                responseNgrams.values().stream().mapToInt(Integer::intValue).sum();

        final double recall = totalRefNgrams > 0 ? (double) overlap / totalRefNgrams : 0.0;
        final double precision = totalRespNgrams > 0 ? (double) overlap / totalRespNgrams : 0.0;

        if (precision + recall == 0) {
            return 0.0;
        }

        final double betaSquared = beta * beta;
        return (1 + betaSquared) * precision * recall / (betaSquared * precision + recall);
    }

    /**
     * Extracts character n-grams from a string.
     *
     * @param text the text
     * @param n the n-gram size
     * @return map of n-gram to count
     */
    private Map<String, Integer> extractCharNgrams(final String text, final int n) {
        final Map<String, Integer> ngrams = new HashMap<>();

        if (text.length() < n) {
            return ngrams;
        }

        for (int i = 0; i <= text.length() - n; i++) {
            final String ngram = text.substring(i, i + n);
            ngrams.merge(ngram, 1, Integer::sum);
        }

        return ngrams;
    }

    /**
     * Extracts word n-grams from a list of tokens.
     *
     * @param tokens the tokens
     * @param n the n-gram size
     * @return map of n-gram to count
     */
    private Map<String, Integer> extractWordNgrams(final List<String> tokens, final int n) {
        final Map<String, Integer> ngrams = new HashMap<>();

        if (tokens.size() < n) {
            return ngrams;
        }

        for (int i = 0; i <= tokens.size() - n; i++) {
            final StringBuilder sb = new StringBuilder();
            for (int j = 0; j < n; j++) {
                if (j > 0) {
                    sb.append(" ");
                }
                sb.append(tokens.get(i + j));
            }
            final String ngram = sb.toString();
            ngrams.merge(ngram, 1, Integer::sum);
        }

        return ngrams;
    }

    /**
     * Tokenizes text into words.
     *
     * @param text the text to tokenize
     * @return list of tokens (lowercase)
     */
    private List<String> tokenize(final String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        final String normalized = text.toLowerCase().replaceAll("[^a-zа-яё0-9\\s]", " ");
        return Arrays.stream(normalized.split("\\s+")).filter(s -> !s.isEmpty()).toList();
    }

    /**
     * Configuration for ChrfScoreMetric.
     */
    @Data
    @Builder
    public static class ChrfScoreConfig implements Metric.MetricConfiguration {

        /**
         * Maximum character n-gram order.
         * Default is 6 (chrF standard).
         */
        @Builder.Default
        private int charNgramOrder = 6;

        /**
         * Maximum word n-gram order.
         * 0 = chrF (character only), >0 = chrF++ (character + word).
         */
        @Builder.Default
        private int wordNgramOrder = 0;

        /**
         * Beta parameter for F-score weighting.
         * Higher values give more weight to recall.
         * Default is 2.0 (chrF standard).
         */
        @Builder.Default
        private double beta = 2.0;
    }
}
