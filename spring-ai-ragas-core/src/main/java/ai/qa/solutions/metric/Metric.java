package ai.qa.solutions.metric;

import ai.qa.solutions.sample.Sample;
import java.util.concurrent.CompletableFuture;

/**
 * Base interface for all metrics
 */
public interface Metric {
    String getName();

    Double score(Sample sample);

    CompletableFuture<Double> scoreAsync(Sample sample);
}
