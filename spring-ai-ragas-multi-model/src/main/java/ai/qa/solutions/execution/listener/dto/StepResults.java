package ai.qa.solutions.execution.listener.dto;

import ai.qa.solutions.execution.ModelResult;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Value;

/**
 * Results of a step execution across all models.
 * <p>
 * Accumulated by the metric during evaluation and delivered as part of
 * {@link MetricEvaluationResult#getSteps()}.
 * <p>
 * Contains:
 * <ul>
 *   <li>Step metadata (name, index, total steps)</li>
 *   <li>Results from all models that executed the step</li>
 *   <li>Utility methods to filter successful/failed results</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * public void afterMetricEvaluation(MetricEvaluationResult result) {
 *     for (StepResults step : result.getSteps()) {
 *         log.info("Step {} completed: {} successful, {} failed",
 *             step.getStepName(),
 *             step.getSuccessful().size(),
 *             step.getFailed().size());
 *     }
 * }
 * }</pre>
 */
@Value
@Builder
public class StepResults {

    /**
     * The name of the step that completed.
     * <p>
     * Examples: "GenerateStatements", "EvaluateFaithfulness", "ComputeScore"
     */
    String stepName;

    /**
     * The zero-based index of this step in the evaluation chain.
     */
    int stepIndex;

    /**
     * Total number of steps in the evaluation.
     */
    int totalSteps;

    /**
     * Results from all models that executed this step.
     * <p>
     * Contains both successful and failed results.
     */
    @Builder.Default
    List<ModelResult<?>> results = List.of();

    /**
     * The type of step execution.
     * <p>
     * Determines how the step should be displayed in logs and reports.
     */
    StepType stepType;

    /**
     * The request/prompt sent to LLM models (for LLM steps only).
     * <p>
     * This is null for non-LLM steps like COMPUTE or EMBEDDING.
     */
    String request;

    /**
     * Results from embedding models (for EMBEDDING steps only).
     * <p>
     * Contains the raw embedding model results with their model IDs and durations,
     * separate from the main results which are indexed by LLM model IDs.
     * This allows tracking embedding model performance independently.
     */
    @Builder.Default
    List<ModelResult<?>> embeddingModelResults = List.of();

    /**
     * Gets only successful results.
     *
     * @return list of results where {@link ModelResult#isSuccess()} is true
     */
    public List<ModelResult<?>> getSuccessful() {
        return results.stream().filter(ModelResult::isSuccess).toList();
    }

    /**
     * Gets only failed results.
     *
     * @return list of results where {@link ModelResult#isFailure()} is true
     */
    public List<ModelResult<?>> getFailed() {
        return results.stream().filter(ModelResult::isFailure).toList();
    }

    /**
     * Gets the number of successful results.
     *
     * @return count of successful results
     */
    public int getSuccessCount() {
        return (int) results.stream().filter(ModelResult::isSuccess).count();
    }

    /**
     * Gets the number of failed results.
     *
     * @return count of failed results
     */
    public int getFailCount() {
        return (int) results.stream().filter(ModelResult::isFailure).count();
    }

    /**
     * Gets the success rate as a fraction between 0.0 and 1.0.
     *
     * @return success rate, or 0.0 if no results
     */
    public double getSuccessRate() {
        if (results.isEmpty()) {
            return 0.0;
        }
        return (double) getSuccessCount() / results.size();
    }

    /**
     * Gets the total duration of this step (max duration across all models).
     * <p>
     * Since models execute in parallel, the step duration is the maximum
     * of individual model durations.
     *
     * @return the maximum duration, or Duration.ZERO if no results
     */
    public Duration getTotalDuration() {
        return results.stream()
                .map(ModelResult::duration)
                .max(Duration::compareTo)
                .orElse(Duration.ZERO);
    }

    /**
     * Gets results as a map indexed by model ID.
     *
     * @return map from model ID to result
     */
    public Map<String, ModelResult<?>> getResultsByModelId() {
        return results.stream().collect(Collectors.toMap(ModelResult::modelId, r -> r));
    }

    /**
     * Gets the total duration of embedding operations (max duration across embedding models).
     * <p>
     * For EMBEDDING steps, this returns the maximum duration from embedding model results,
     * which represents the actual wall-clock time spent on embeddings.
     *
     * @return the maximum embedding duration, or Duration.ZERO if no embedding results
     */
    public Duration getEmbeddingDuration() {
        if (embeddingModelResults == null || embeddingModelResults.isEmpty()) {
            return Duration.ZERO;
        }
        return embeddingModelResults.stream()
                .map(ModelResult::duration)
                .filter(d -> d != null)
                .max(Duration::compareTo)
                .orElse(Duration.ZERO);
    }

    /**
     * Gets the count of successful embedding model executions.
     *
     * @return count of successful embedding results
     */
    public int getEmbeddingSuccessCount() {
        if (embeddingModelResults == null) {
            return 0;
        }
        return (int)
                embeddingModelResults.stream().filter(ModelResult::isSuccess).count();
    }

    /**
     * Gets the count of failed embedding model executions.
     *
     * @return count of failed embedding results
     */
    public int getEmbeddingFailCount() {
        if (embeddingModelResults == null) {
            return 0;
        }
        return (int)
                embeddingModelResults.stream().filter(ModelResult::isFailure).count();
    }
}
