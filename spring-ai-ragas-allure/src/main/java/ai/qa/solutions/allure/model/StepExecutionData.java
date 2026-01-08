package ai.qa.solutions.allure.model;

import ai.qa.solutions.execution.listener.dto.StepResults;
import ai.qa.solutions.execution.listener.dto.StepType;
import java.time.Duration;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Data container for a single step execution within a metric evaluation.
 * <p>
 * Contains step metadata, the request/prompt (for LLM steps), and
 * all model execution results.
 */
@Value
@Builder
public class StepExecutionData {

    /**
     * Name of the step (e.g., "GenerateStatements", "EvaluateFaithfulness").
     */
    String stepName;

    /**
     * Zero-based index of this step in the evaluation.
     */
    int stepIndex;

    /**
     * Total number of steps in the evaluation.
     */
    int totalSteps;

    /**
     * Type of step execution (LLM, EMBEDDING, COMPUTE).
     */
    StepType stepType;

    /**
     * The request/prompt sent to models (null for COMPUTE steps).
     */
    String request;

    /**
     * Total duration of this step (max across parallel model executions).
     */
    Duration duration;

    /**
     * Results from all LLM models that executed this step.
     */
    @Builder.Default
    List<ModelExecutionData> modelResults = List.of();

    /**
     * Results from embedding models (for EMBEDDING steps).
     */
    @Builder.Default
    List<ModelExecutionData> embeddingResults = List.of();

    /**
     * Number of successful model executions.
     */
    int successCount;

    /**
     * Number of failed model executions.
     */
    int failureCount;

    /**
     * Creates StepExecutionData from StepResults.
     *
     * @param results the step results to convert
     * @return converted StepExecutionData
     */
    public static StepExecutionData from(final StepResults results) {
        final List<ModelExecutionData> modelData =
                results.getResults().stream().map(ModelExecutionData::from).toList();

        final List<ModelExecutionData> embeddingData = results.getEmbeddingModelResults() != null
                ? results.getEmbeddingModelResults().stream()
                        .map(ModelExecutionData::from)
                        .toList()
                : List.of();

        return StepExecutionData.builder()
                .stepName(results.getStepName())
                .stepIndex(results.getStepIndex())
                .totalSteps(results.getTotalSteps())
                .stepType(results.getStepType())
                .request(results.getRequest())
                .duration(results.getTotalDuration())
                .modelResults(modelData)
                .embeddingResults(embeddingData)
                .successCount(results.getSuccessCount())
                .failureCount(results.getFailCount())
                .build();
    }

    /**
     * Gets duration in milliseconds.
     *
     * @return duration in ms, or 0 if duration is null
     */
    public long getDurationMs() {
        return duration != null ? duration.toMillis() : 0;
    }

    /**
     * Checks if this is an LLM step.
     *
     * @return true if step type is LLM
     */
    public boolean isLlmStep() {
        return stepType == StepType.LLM;
    }

    /**
     * Checks if this is an embedding step.
     *
     * @return true if step type is EMBEDDING
     */
    public boolean isEmbeddingStep() {
        return stepType == StepType.EMBEDDING;
    }

    /**
     * Checks if this is a compute step.
     *
     * @return true if step type is COMPUTE
     */
    public boolean isComputeStep() {
        return stepType == StepType.COMPUTE;
    }

    /**
     * Gets the step number (1-based) for display.
     *
     * @return step number starting from 1
     */
    public int getStepNumber() {
        return stepIndex + 1;
    }

    /**
     * Checks if the step has any failures.
     *
     * @return true if at least one model failed
     */
    public boolean hasFailures() {
        return failureCount > 0;
    }

    /**
     * Checks if the step has embedding results.
     *
     * @return true if embedding results are available
     */
    public boolean hasEmbeddingResults() {
        return !embeddingResults.isEmpty();
    }
}
