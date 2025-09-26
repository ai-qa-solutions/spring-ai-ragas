package ai.qa.solutions.metric;

import ai.qa.solutions.sample.EvaluationSample;
import ai.qa.solutions.sample.Sample;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for single-turn metrics
 */
public interface SingleTurnMetric extends Metric {
    Double singleTurnScore(Sample sample);

    CompletableFuture<Double> singleTurnScoreAsync(Sample sample);

    @Override
    default Double score(EvaluationSample sample) {
        if (!(sample instanceof Sample)) {
            throw new IllegalArgumentException(
                    "Expected SingleTurnSample but got " + sample.getClass().getSimpleName());
        }
        return singleTurnScore((Sample) sample);
    }

    @Override
    default CompletableFuture<Double> scoreAsync(EvaluationSample sample) {
        if (!(sample instanceof Sample)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException(
                    "Expected SingleTurnSample but got " + sample.getClass().getSimpleName()));
        }
        return singleTurnScoreAsync((Sample) sample);
    }
}
