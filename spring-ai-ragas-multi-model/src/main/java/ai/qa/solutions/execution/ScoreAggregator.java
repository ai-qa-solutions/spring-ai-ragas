package ai.qa.solutions.execution;

import java.util.List;

/**
 * Strategy interface for aggregating scores from multiple model executions.
 * <p>
 * Implementations define how individual model scores are combined
 * into a single final score.
 */
@FunctionalInterface
public interface ScoreAggregator {

    /**
     * Aggregates multiple scores into a single result.
     *
     * @param scores list of successful scores (never empty)
     * @return aggregated score
     */
    double aggregate(List<Double> scores);

    /**
     * @return human-readable name of this aggregation strategy
     */
    default String getName() {
        return getClass().getSimpleName();
    }

    // ========== Built-in aggregators ==========

    /** Averages all scores */
    ScoreAggregator AVERAGE = new ScoreAggregator() {
        @Override
        public double aggregate(List<Double> scores) {
            return scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        }

        @Override
        public String getName() {
            return "AVERAGE";
        }
    };

    /** Returns the median score */
    ScoreAggregator MEDIAN = new ScoreAggregator() {
        @Override
        public double aggregate(final List<Double> scores) {
            if (scores.isEmpty()) {
                return 0.0;
            }
            final var sorted = scores.stream().sorted().toList();
            final int size = sorted.size();
            if (size % 2 == 0) {
                return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
            }
            return sorted.get(size / 2);
        }

        @Override
        public String getName() {
            return "MEDIAN";
        }
    };

    /** Returns the minimum score (most conservative) */
    ScoreAggregator MIN = new ScoreAggregator() {
        @Override
        public double aggregate(List<Double> scores) {
            return scores.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        }

        @Override
        public String getName() {
            return "MIN";
        }
    };

    /** Returns the maximum score (most optimistic) */
    ScoreAggregator MAX = new ScoreAggregator() {
        @Override
        public double aggregate(List<Double> scores) {
            return scores.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        }

        @Override
        public String getName() {
            return "MAX";
        }
    };

    /**
     * Majority voting for binary verdicts.
     * Returns 1.0 if more than half of the scores are >= 0.5, otherwise returns 0.0.
     * Used for AspectCritic-style metrics where verdicts are binary (true/false).
     */
    ScoreAggregator MAJORITY_VOTING = new ScoreAggregator() {
        @Override
        public double aggregate(List<Double> scores) {
            if (scores.isEmpty()) {
                return 0.0;
            }
            long trueVotes = scores.stream().filter(s -> s >= 0.5).count();
            return trueVotes > scores.size() / 2.0 ? 1.0 : 0.0;
        }

        @Override
        public String getName() {
            return "MAJORITY_VOTING";
        }
    };

    /** Requires consensus: returns score only if all models agree (within tolerance) */
    static ScoreAggregator consensus(final double tolerance) {
        return new ScoreAggregator() {
            @Override
            public double aggregate(final List<Double> scores) {
                final double min =
                        scores.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
                final double max =
                        scores.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
                if (max - min <= tolerance) {
                    return AVERAGE.aggregate(scores);
                }
                throw new IllegalStateException(
                        "No consensus: scores range from " + min + " to " + max + " (tolerance: " + tolerance + ")");
            }

            @Override
            public String getName() {
                return "CONSENSUS(tolerance=" + tolerance + ")";
            }
        };
    }
}
