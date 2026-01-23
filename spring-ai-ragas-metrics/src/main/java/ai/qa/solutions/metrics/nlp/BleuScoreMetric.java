package ai.qa.solutions.metrics.nlp;

import ai.qa.solutions.metric.Metric;
import ai.qa.solutions.sample.Sample;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * BLEU (Bilingual Evaluation Understudy) Score Metric.
 * <p>
 * Evaluates the quality of machine-generated text by comparing n-gram precision
 * with a reference text. This is a non-LLM metric that computes scores algorithmically.
 * <p>
 * The BLEU score ranges from 0 to 1, where:
 * <ul>
 *   <li>0 - No n-gram overlap with reference</li>
 *   <li>1 - Perfect match with reference</li>
 * </ul>
 * <p>
 * Key features:
 * <ul>
 *   <li>Computes modified n-gram precision for n=1 to maxNgram</li>
 *   <li>Applies brevity penalty for short responses</li>
 *   <li>Supports optional smoothing for better handling of short texts</li>
 * </ul>
 * <p>
 * Required sample fields:
 * <ul>
 *   <li>{@code response} - The generated text to evaluate</li>
 *   <li>{@code reference} - The reference (ground truth) text</li>
 * </ul>
 */
@Slf4j
public class BleuScoreMetric implements Metric<BleuScoreMetric.BleuScoreConfig> {

    /**
     * Computes the BLEU score for a single sample.
     *
     * @param config the metric configuration
     * @param sample the sample containing response and reference
     * @return the BLEU score (0-1), or null if input is invalid
     */
    @Override
    public Double singleTurnScore(final BleuScoreConfig config, final Sample sample) {
        // Validate input
        if (sample.getResponse() == null || sample.getResponse().isEmpty()) {
            log.warn("No response provided for BLEU score evaluation");
            return null;
        }

        if (sample.getReference() == null || sample.getReference().isEmpty()) {
            log.warn("No reference provided for BLEU score evaluation");
            return null;
        }

        final List<String> responseTokens = tokenize(sample.getResponse());
        final List<String> referenceTokens = tokenize(sample.getReference());

        if (responseTokens.isEmpty() || referenceTokens.isEmpty()) {
            log.warn("Empty tokens after tokenization");
            return 0.0;
        }

        final int maxNgram = config.getMaxNgram();
        final boolean smoothing = config.isSmoothing();

        // Compute modified n-gram precisions
        final List<Double> precisions = new ArrayList<>();
        for (int n = 1; n <= maxNgram; n++) {
            final double precision = computeModifiedPrecision(responseTokens, referenceTokens, n, smoothing);
            precisions.add(precision);
        }

        // Check if all precisions are zero (would result in BLEU = 0)
        final boolean allZero = precisions.stream().allMatch(p -> p <= 0.0);
        if (allZero) {
            return 0.0;
        }

        // Compute brevity penalty
        final double brevityPenalty = computeBrevityPenalty(responseTokens.size(), referenceTokens.size());

        // Compute BLEU score as geometric mean of precisions × brevity penalty
        double logSum = 0.0;
        int validCount = 0;
        for (final double precision : precisions) {
            if (precision > 0) {
                logSum += Math.log(precision);
                validCount++;
            }
        }

        if (validCount == 0) {
            return 0.0;
        }

        final double geometricMean = Math.exp(logSum / maxNgram);
        final double bleuScore = brevityPenalty * geometricMean;

        return Math.min(1.0, Math.max(0.0, bleuScore));
    }

    /**
     * Asynchronously computes the BLEU score for a single sample.
     * <p>
     * Since this is a non-LLM computational metric, it executes synchronously
     * and wraps the result in a completed future.
     *
     * @param config the metric configuration
     * @param sample the sample containing response and reference
     * @return a CompletableFuture containing the BLEU score
     */
    @Override
    public CompletableFuture<Double> singleTurnScoreAsync(final BleuScoreConfig config, final Sample sample) {
        return CompletableFuture.completedFuture(singleTurnScore(config, sample));
    }

    @Override
    public String getName() {
        return "BleuScoreMetric";
    }

    /**
     * Tokenizes text into words.
     * Uses simple whitespace and punctuation-based tokenization.
     *
     * @param text the text to tokenize
     * @return list of tokens (lowercase)
     */
    private List<String> tokenize(final String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        // Simple tokenization: split on whitespace and punctuation, convert to lowercase
        final String normalized = text.toLowerCase().replaceAll("[^a-zа-яё0-9\\s]", " ");
        return Arrays.stream(normalized.split("\\s+")).filter(s -> !s.isEmpty()).toList();
    }

    /**
     * Computes modified n-gram precision with clipping.
     * Each n-gram in the response is counted at most as many times as it appears in the reference.
     *
     * @param response the response tokens
     * @param reference the reference tokens
     * @param n the n-gram size
     * @param smoothing whether to apply smoothing (+1 to counts)
     * @return the modified precision
     */
    private double computeModifiedPrecision(
            final List<String> response, final List<String> reference, final int n, final boolean smoothing) {

        if (response.size() < n) {
            return smoothing ? 1.0 / (response.size() + 1) : 0.0;
        }

        // Count n-grams in response
        final Map<String, Integer> responseNgrams = extractNgrams(response, n);
        // Count n-grams in reference
        final Map<String, Integer> referenceNgrams = extractNgrams(reference, n);

        // Compute clipped counts
        int clippedCount = 0;
        int totalCount = 0;

        for (final Map.Entry<String, Integer> entry : responseNgrams.entrySet()) {
            final String ngram = entry.getKey();
            final int responseCount = entry.getValue();
            final int referenceCount = referenceNgrams.getOrDefault(ngram, 0);
            final int clipped = Math.min(responseCount, referenceCount);

            clippedCount += clipped;
            totalCount += responseCount;
        }

        if (smoothing) {
            // Add-1 smoothing
            return (clippedCount + 1.0) / (totalCount + 1.0);
        }

        return totalCount > 0 ? (double) clippedCount / totalCount : 0.0;
    }

    /**
     * Extracts n-grams from a list of tokens.
     *
     * @param tokens the tokens
     * @param n the n-gram size
     * @return map of n-gram to count
     */
    private Map<String, Integer> extractNgrams(final List<String> tokens, final int n) {
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
     * Computes the brevity penalty.
     * Penalizes responses shorter than the reference.
     *
     * @param responseLength the length of response in tokens
     * @param referenceLength the length of reference in tokens
     * @return the brevity penalty (0-1)
     */
    private double computeBrevityPenalty(final int responseLength, final int referenceLength) {
        if (responseLength >= referenceLength) {
            return 1.0;
        }
        return Math.exp(1.0 - (double) referenceLength / responseLength);
    }

    /**
     * Configuration for BleuScoreMetric.
     */
    @Data
    @Builder
    public static class BleuScoreConfig implements Metric.MetricConfiguration {

        /**
         * Maximum n-gram size to consider.
         * Standard BLEU uses n=4.
         */
        @Builder.Default
        private int maxNgram = 4;

        /**
         * Whether to apply smoothing.
         * Smoothing helps with short texts that may have zero n-gram matches.
         */
        @Builder.Default
        private boolean smoothing = true;
    }
}
