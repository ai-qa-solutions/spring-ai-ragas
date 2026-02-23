package ai.qa.solutions.metrics.nlp;

import ai.qa.solutions.execution.listener.dto.MetricEvaluationContext;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationResult;
import ai.qa.solutions.metric.AbstractMetric;
import ai.qa.solutions.metric.Metric;
import ai.qa.solutions.metric.metadata.RougeScoreMetadata;
import ai.qa.solutions.sample.Sample;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * ROUGE (Recall-Oriented Understudy for Gisting Evaluation) Score Metric.
 * <p>
 * Evaluates the quality of machine-generated text by comparing n-gram overlap
 * with a reference text. This is a non-LLM metric that computes scores algorithmically.
 * <p>
 * Supports three variants:
 * <ul>
 *   <li>ROUGE-1: Unigram overlap</li>
 *   <li>ROUGE-2: Bigram overlap</li>
 *   <li>ROUGE-L: Longest Common Subsequence</li>
 * </ul>
 * <p>
 * Returns scores in the range [0, 1], where:
 * <ul>
 *   <li>0 - No overlap with reference</li>
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
public class RougeScoreMetric extends AbstractMetric<RougeScoreMetric.RougeScoreConfig> {

    /**
     * Computes the ROUGE score for a single sample.
     *
     * @param config the metric configuration
     * @param sample the sample containing response and reference
     * @return the ROUGE score (0-1), or null if input is invalid
     */
    @Override
    public Double singleTurnScore(final RougeScoreConfig config, final Sample sample) {
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
                log.warn("No response provided for ROUGE score evaluation");
                return null;
            }

            if (sample.getReference() == null || sample.getReference().isEmpty()) {
                log.warn("No reference provided for ROUGE score evaluation");
                return null;
            }

            final List<String> responseTokens = tokenize(sample.getResponse());
            final List<String> referenceTokens = tokenize(sample.getReference());

            if (responseTokens.isEmpty() || referenceTokens.isEmpty()) {
                log.warn("Empty tokens after tokenization");
                score = 0.0;
                return score;
            }

            final RougeType rougeType = config.getRougeType();
            final Mode mode = config.getMode();

            score = switch (rougeType) {
                case ROUGE_1 -> computeRougeN(responseTokens, referenceTokens, 1, mode);
                case ROUGE_2 -> computeRougeN(responseTokens, referenceTokens, 2, mode);
                case ROUGE_L -> computeRougeL(responseTokens, referenceTokens, mode);};
            return score;
        } finally {
            notifier.afterMetricEvaluation(MetricEvaluationResult.builder()
                    .metricName(getName())
                    .sample(sample)
                    .config(config)
                    .modelIds(List.of())
                    .aggregatedScore(score)
                    .metadata(new RougeScoreMetadata(
                            config.getRougeType().name(), config.getMode().name()))
                    .build());
        }
    }

    /**
     * Asynchronously computes the ROUGE score for a single sample.
     * <p>
     * Since this is a non-LLM computational metric, it executes synchronously
     * and wraps the result in a completed future.
     *
     * @param config the metric configuration
     * @param sample the sample containing response and reference
     * @return a CompletableFuture containing the ROUGE score
     */
    @Override
    public CompletableFuture<Double> singleTurnScoreAsync(final RougeScoreConfig config, final Sample sample) {
        return CompletableFuture.completedFuture(singleTurnScore(config, sample));
    }

    @Override
    public String getName() {
        return "RougeScoreMetric";
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
     * Computes ROUGE-N score (n-gram overlap).
     *
     * @param response the response tokens
     * @param reference the reference tokens
     * @param n the n-gram size
     * @param mode the scoring mode (precision, recall, or f-measure)
     * @return the ROUGE-N score
     */
    private double computeRougeN(
            final List<String> response, final List<String> reference, final int n, final Mode mode) {

        final Map<String, Integer> responseNgrams = extractNgrams(response, n);
        final Map<String, Integer> referenceNgrams = extractNgrams(reference, n);

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

        return switch (mode) {
            case RECALL -> recall;
            case PRECISION -> precision;
            case FMEASURE -> {
                if (precision + recall == 0) {
                    yield 0.0;
                }
                yield 2 * precision * recall / (precision + recall);
            }
        };
    }

    /**
     * Computes ROUGE-L score (Longest Common Subsequence).
     *
     * @param response the response tokens
     * @param reference the reference tokens
     * @param mode the scoring mode (precision, recall, or f-measure)
     * @return the ROUGE-L score
     */
    private double computeRougeL(final List<String> response, final List<String> reference, final Mode mode) {

        if (response.isEmpty() && reference.isEmpty()) {
            return 1.0; // Both empty = perfect match
        }

        if (response.isEmpty() || reference.isEmpty()) {
            return 0.0;
        }

        final int lcsLength = computeLCS(response, reference);

        final double recall = (double) lcsLength / reference.size();
        final double precision = (double) lcsLength / response.size();

        return switch (mode) {
            case RECALL -> recall;
            case PRECISION -> precision;
            case FMEASURE -> {
                if (precision + recall == 0) {
                    yield 0.0;
                }
                yield 2 * precision * recall / (precision + recall);
            }
        };
    }

    /**
     * Computes the length of the Longest Common Subsequence.
     *
     * @param a first sequence
     * @param b second sequence
     * @return length of LCS
     */
    private int computeLCS(final List<String> a, final List<String> b) {
        final int m = a.size();
        final int n = b.size();

        // Use 1D DP array for space efficiency
        final int[] dp = new int[n + 1];

        for (int i = 1; i <= m; i++) {
            int prev = 0;
            for (int j = 1; j <= n; j++) {
                final int temp = dp[j];
                if (a.get(i - 1).equals(b.get(j - 1))) {
                    dp[j] = prev + 1;
                } else {
                    dp[j] = Math.max(dp[j], dp[j - 1]);
                }
                prev = temp;
            }
        }

        return dp[n];
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
     * ROUGE variant types.
     */
    @Getter
    public enum RougeType {
        /** Unigram overlap */
        ROUGE_1(1),
        /** Bigram overlap */
        ROUGE_2(2),
        /** Longest Common Subsequence */
        ROUGE_L(0);

        private final int ngramSize;

        RougeType(final int ngramSize) {
            this.ngramSize = ngramSize;
        }
    }

    /**
     * Scoring mode for ROUGE.
     */
    public enum Mode {
        /** Recall: overlap / reference_length */
        RECALL,
        /** Precision: overlap / response_length */
        PRECISION,
        /** F-measure: harmonic mean of precision and recall */
        FMEASURE
    }

    /**
     * Configuration for RougeScoreMetric.
     */
    @Data
    @Builder
    public static class RougeScoreConfig implements Metric.MetricConfiguration {

        /**
         * ROUGE variant to compute.
         * Default is ROUGE-L.
         */
        @Builder.Default
        private RougeType rougeType = RougeType.ROUGE_L;

        /**
         * Scoring mode.
         * Default is F-measure.
         */
        @Builder.Default
        private Mode mode = Mode.FMEASURE;
    }
}
