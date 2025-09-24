package ai.qa.solutions.metric;

import ai.qa.solutions.sample.EvaluationSample;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract base class for all metrics
 */
public abstract class AbstractMetric implements Metric {
    protected final String name;
    protected final MetricType type;
    protected final MetricOutputType outputType;
    protected final Set<String> requiredColumns;

    protected AbstractMetric(String name, MetricType type, MetricOutputType outputType, Set<String> requiredColumns) {
        this.name = name;
        this.type = type;
        this.outputType = outputType;
        this.requiredColumns = requiredColumns;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public MetricType getType() {
        return type;
    }

    @Override
    public MetricOutputType getOutputType() {
        return outputType;
    }

    @Override
    public Set<String> getRequiredColumns() {
        return requiredColumns;
    }

    @Override
    public CompletableFuture<Double> scoreAsync(EvaluationSample sample) {
        return CompletableFuture.supplyAsync(() -> score(sample));
    }

    protected void validateSample(EvaluationSample sample) {
        if (sample == null) {
            throw new IllegalArgumentException("Sample cannot be null");
        }
        // Additional validation logic can be added here
    }
}
