package ai.qa.solutions.metrics.nlp;

import ai.qa.solutions.execution.listener.dto.MetricEvaluationContext;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationResult;
import ai.qa.solutions.metric.AbstractMetric;
import ai.qa.solutions.metric.Metric;
import ai.qa.solutions.metric.metadata.StringSimilarityMetadata;
import ai.qa.solutions.sample.Sample;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.HammingDistance;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;

/**
 * String Similarity Metric using various distance algorithms.
 * <p>
 * Provides multiple string distance measures:
 * <ul>
 *   <li>LEVENSHTEIN - Edit distance (insertions, deletions, substitutions)</li>
 *   <li>HAMMING - Substitution-only distance (requires equal length strings)</li>
 *   <li>JARO - Similarity based on matching characters</li>
 *   <li>JARO_WINKLER - Jaro with prefix bonus</li>
 * </ul>
 * <p>
 * Returns scores in the range [0, 1], where:
 * <ul>
 *   <li>0 - Completely different strings</li>
 *   <li>1 - Identical strings</li>
 * </ul>
 * <p>
 * Required sample fields:
 * <ul>
 *   <li>{@code response} - The generated text to evaluate</li>
 *   <li>{@code reference} - The reference (ground truth) text</li>
 * </ul>
 */
@Slf4j
public class StringSimilarityMetric extends AbstractMetric<StringSimilarityMetric.StringSimilarityConfig> {

    private final LevenshteinDistance levenshteinDistance = new LevenshteinDistance();
    private final HammingDistance hammingDistance = new HammingDistance();
    private final JaroWinklerSimilarity jaroWinklerSimilarity = new JaroWinklerSimilarity();

    /**
     * Computes the string similarity score for a single sample.
     *
     * @param config the metric configuration
     * @param sample the sample containing response and reference
     * @return the similarity score (0-1), or null if input is invalid
     */
    @Override
    public Double singleTurnScore(final StringSimilarityConfig config, final Sample sample) {
        final EvaluationNotifier notifier = createEvaluationNotifier();
        notifier.beforeMetricEvaluation(MetricEvaluationContext.builder()
                .metricName(getName())
                .sample(sample)
                .config(config)
                .modelIds(List.of())
                .totalSteps(0)
                .build());

        Double score = null;
        try {
            // Validate input
            if (sample.getResponse() == null || sample.getResponse().isEmpty()) {
                log.warn("No response provided for String Similarity evaluation");
                return null;
            }

            if (sample.getReference() == null || sample.getReference().isEmpty()) {
                log.warn("No reference provided for String Similarity evaluation");
                return null;
            }

            final String response = config.isCaseSensitive()
                    ? sample.getResponse()
                    : sample.getResponse().toLowerCase();
            final String reference = config.isCaseSensitive()
                    ? sample.getReference()
                    : sample.getReference().toLowerCase();

            final DistanceMeasure measure = config.getDistanceMeasure();

            score = switch (measure) {
                case LEVENSHTEIN -> computeLevenshteinSimilarity(response, reference);
                case HAMMING -> computeHammingSimilarity(response, reference);
                case JARO -> computeJaroSimilarity(response, reference);
                case JARO_WINKLER -> computeJaroWinklerSimilarity(response, reference);};
            return score;
        } finally {
            notifier.afterMetricEvaluation(MetricEvaluationResult.builder()
                    .metricName(getName())
                    .sample(sample)
                    .config(config)
                    .modelIds(List.of())
                    .aggregatedScore(score)
                    .metadata(new StringSimilarityMetadata(
                            config.getDistanceMeasure().name(), config.isCaseSensitive()))
                    .build());
        }
    }

    /**
     * Asynchronously computes the string similarity score for a single sample.
     * <p>
     * Since this is a non-LLM computational metric, it executes synchronously
     * and wraps the result in a completed future.
     *
     * @param config the metric configuration
     * @param sample the sample containing response and reference
     * @return a CompletableFuture containing the similarity score
     */
    @Override
    public CompletableFuture<Double> singleTurnScoreAsync(final StringSimilarityConfig config, final Sample sample) {
        return CompletableFuture.completedFuture(singleTurnScore(config, sample));
    }

    @Override
    public String getName() {
        return "StringSimilarityMetric";
    }

    /**
     * Computes Levenshtein similarity as 1 - normalized distance.
     *
     * @param s1 first string
     * @param s2 second string
     * @return similarity score (0-1)
     */
    private double computeLevenshteinSimilarity(final String s1, final String s2) {
        if (s1.equals(s2)) {
            return 1.0;
        }

        final int distance = levenshteinDistance.apply(s1, s2);
        final int maxLength = Math.max(s1.length(), s2.length());

        if (maxLength == 0) {
            return 1.0; // Both empty = identical
        }

        return 1.0 - ((double) distance / maxLength);
    }

    /**
     * Computes Hamming similarity as 1 - normalized distance.
     * Requires equal length strings; pads shorter string with spaces.
     *
     * @param s1 first string
     * @param s2 second string
     * @return similarity score (0-1)
     */
    private double computeHammingSimilarity(final String s1, final String s2) {
        if (s1.equals(s2)) {
            return 1.0;
        }

        // Pad strings to equal length
        final int maxLen = Math.max(s1.length(), s2.length());
        final String padded1 = padRight(s1, maxLen);
        final String padded2 = padRight(s2, maxLen);

        final int distance = hammingDistance.apply(padded1, padded2);

        if (maxLen == 0) {
            return 1.0;
        }

        return 1.0 - ((double) distance / maxLen);
    }

    /**
     * Computes Jaro similarity.
     * Uses only matching characters without prefix bonus.
     *
     * @param s1 first string
     * @param s2 second string
     * @return similarity score (0-1)
     */
    private double computeJaroSimilarity(final String s1, final String s2) {
        if (s1.equals(s2)) {
            return 1.0;
        }

        // Use Jaro-Winkler with prefix weight 0 to get pure Jaro
        // Actually, JaroWinklerSimilarity returns Jaro-Winkler, so we'll compute Jaro manually
        return computeJaroInternal(s1, s2);
    }

    /**
     * Computes Jaro-Winkler similarity.
     * Jaro similarity with prefix bonus.
     *
     * @param s1 first string
     * @param s2 second string
     * @return similarity score (0-1)
     */
    private double computeJaroWinklerSimilarity(final String s1, final String s2) {
        if (s1.equals(s2)) {
            return 1.0;
        }

        return jaroWinklerSimilarity.apply(s1, s2);
    }

    /**
     * Internal Jaro similarity calculation.
     *
     * @param s1 first string
     * @param s2 second string
     * @return Jaro similarity (0-1)
     */
    private double computeJaroInternal(final String s1, final String s2) {
        if (s1.isEmpty() && s2.isEmpty()) {
            return 1.0;
        }

        if (s1.isEmpty() || s2.isEmpty()) {
            return 0.0;
        }

        final int matchWindow = Math.max(s1.length(), s2.length()) / 2 - 1;
        final boolean[] s1Matches = new boolean[s1.length()];
        final boolean[] s2Matches = new boolean[s2.length()];

        int matches = 0;
        int transpositions = 0;

        // Find matches
        for (int i = 0; i < s1.length(); i++) {
            final int start = Math.max(0, i - matchWindow);
            final int end = Math.min(i + matchWindow + 1, s2.length());

            for (int j = start; j < end; j++) {
                if (s2Matches[j] || s1.charAt(i) != s2.charAt(j)) {
                    continue;
                }
                s1Matches[i] = true;
                s2Matches[j] = true;
                matches++;
                break;
            }
        }

        if (matches == 0) {
            return 0.0;
        }

        // Count transpositions
        int k = 0;
        for (int i = 0; i < s1.length(); i++) {
            if (!s1Matches[i]) {
                continue;
            }
            while (!s2Matches[k]) {
                k++;
            }
            if (s1.charAt(i) != s2.charAt(k)) {
                transpositions++;
            }
            k++;
        }

        return ((double) matches / s1.length()
                        + (double) matches / s2.length()
                        + (double) (matches - transpositions / 2) / matches)
                / 3.0;
    }

    /**
     * Pads a string to the right with spaces.
     *
     * @param s string to pad
     * @param length target length
     * @return padded string
     */
    private String padRight(final String s, final int length) {
        if (s.length() >= length) {
            return s;
        }
        final StringBuilder sb = new StringBuilder(s);
        while (sb.length() < length) {
            sb.append(' ');
        }
        return sb.toString();
    }

    /**
     * Available distance measures.
     */
    public enum DistanceMeasure {
        /** Edit distance (insertions, deletions, substitutions) */
        LEVENSHTEIN,
        /** Substitution-only distance */
        HAMMING,
        /** Character matching similarity */
        JARO,
        /** Jaro with prefix bonus */
        JARO_WINKLER
    }

    /**
     * Configuration for StringSimilarityMetric.
     */
    @Data
    @Builder
    public static class StringSimilarityConfig implements Metric.MetricConfiguration {

        /**
         * Distance measure to use.
         * Default is JARO_WINKLER.
         */
        @Builder.Default
        private DistanceMeasure distanceMeasure = DistanceMeasure.JARO_WINKLER;

        /**
         * Whether comparison should be case sensitive.
         * Default is false (case insensitive).
         */
        @Builder.Default
        private boolean caseSensitive = false;
    }
}
