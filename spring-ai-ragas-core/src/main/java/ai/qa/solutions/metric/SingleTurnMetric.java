package ai.qa.solutions.metric;

import ai.qa.solutions.sample.Sample;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for single-turn metrics
 */
public interface SingleTurnMetric extends Metric {
    Double singleTurnScore(Sample sample);

    CompletableFuture<Double> singleTurnScoreAsync(Sample sample);

    @Override
    default Double score(Sample sample) {
        return singleTurnScore(sample);
    }

    @Override
    default CompletableFuture<Double> scoreAsync(Sample sample) {
        return singleTurnScoreAsync(sample);
    }
}
