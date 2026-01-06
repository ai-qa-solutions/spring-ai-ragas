package ai.qa.solutions.metric;

import ai.qa.solutions.sample.Sample;
import java.util.concurrent.CompletableFuture;

/**
 * Base interface for all metrics
 */
public interface Metric<T extends Metric.MetricConfiguration> {
    default String getName() {
        return getClass().getSimpleName();
    }

    Double singleTurnScore(T metricConfiguration, Sample sample);

    CompletableFuture<Double> singleTurnScoreAsync(T metricConfiguration, Sample sample);

    interface MetricConfiguration {}
}
