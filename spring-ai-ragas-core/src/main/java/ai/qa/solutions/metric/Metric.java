package ai.qa.solutions.metric;

import ai.qa.solutions.sample.EvaluationSample;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Base interface for all metrics
 */
public interface Metric {
    String getName();

    MetricType getType();

    MetricOutputType getOutputType();

    Set<String> getRequiredColumns();

    Double score(EvaluationSample sample);

    CompletableFuture<Double> scoreAsync(EvaluationSample sample);
}
