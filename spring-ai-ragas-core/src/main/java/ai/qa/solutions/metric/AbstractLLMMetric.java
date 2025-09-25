package ai.qa.solutions.metric;

import ai.qa.solutions.sample.SingleTurnSample;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract base class for LLM-based metrics
 */
public abstract class AbstractLLMMetric extends AbstractMetric implements SingleTurnMetric {
    protected String promptTemplate;

    protected AbstractLLMMetric(String name, MetricOutputType outputType, Set<String> requiredColumns) {
        super(name, MetricType.SINGLE_TURN, outputType, requiredColumns);
    }

    @Override
    public CompletableFuture<Double> singleTurnScoreAsync(SingleTurnSample sample) {
        return CompletableFuture.supplyAsync(() -> singleTurnScore(sample));
    }

    protected abstract String buildPrompt(SingleTurnSample sample);
}
