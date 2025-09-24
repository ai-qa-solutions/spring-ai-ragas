package ai.qa.solutions.metric;

import ai.qa.solutions.llm.LLMEvaluationService;
import ai.qa.solutions.sample.SingleTurnSample;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract base class for LLM-based metrics
 */
public abstract class AbstractLLMMetric extends AbstractMetric implements SingleTurnMetric {
    protected LLMEvaluationService llmService;
    protected String promptTemplate;

    protected AbstractLLMMetric(String name, MetricOutputType outputType, Set<String> requiredColumns) {
        super(name, MetricType.SINGLE_TURN, outputType, requiredColumns);
    }

    public void setLlmService(LLMEvaluationService llmService) {
        this.llmService = llmService;
    }

    @Override
    public Double singleTurnScore(SingleTurnSample sample) {
        validateSample(sample);
        String prompt = buildPrompt(sample);
        String llmResponse = llmService.evaluate(prompt);
        return parseScore(llmResponse);
    }

    @Override
    public CompletableFuture<Double> singleTurnScoreAsync(SingleTurnSample sample) {
        return CompletableFuture.supplyAsync(() -> singleTurnScore(sample));
    }

    protected abstract String buildPrompt(SingleTurnSample sample);

    protected abstract Double parseScore(String llmResponse);
}
