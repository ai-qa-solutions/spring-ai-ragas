package ai.qa.solutions.metric;

import ai.qa.solutions.sample.EvaluationSample;
import java.util.concurrent.CompletableFuture;

/**
 * Base interface for all metrics
 */
public interface Metric {
    String getName();

    Double score(EvaluationSample sample);

    CompletableFuture<Double> scoreAsync(EvaluationSample sample);
}
