package ai.qa.solutions.metric;

import ai.qa.solutions.sample.EvaluationSample;
import ai.qa.solutions.sample.SingleTurnSample;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for single-turn metrics
 */
public interface SingleTurnMetric extends Metric {
    Double singleTurnScore(SingleTurnSample sample);

    CompletableFuture<Double> singleTurnScoreAsync(SingleTurnSample sample);

    @Override
    default Double score(EvaluationSample sample) {
        if (!(sample instanceof SingleTurnSample)) {
            throw new IllegalArgumentException(
                    "Expected SingleTurnSample but got " + sample.getClass().getSimpleName());
        }
        return singleTurnScore((SingleTurnSample) sample);
    }

    @Override
    default CompletableFuture<Double> scoreAsync(EvaluationSample sample) {
        if (!(sample instanceof SingleTurnSample)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException(
                    "Expected SingleTurnSample but got " + sample.getClass().getSimpleName()));
        }
        return singleTurnScoreAsync((SingleTurnSample) sample);
    }
}
