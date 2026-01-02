package ai.qa.solutions.execution;

import java.time.Duration;
import java.time.Instant;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

/**
 * Aggregated result from multiple model executions.
 * <p>
 * This immutable record encapsulates the outcome of executing a metric across multiple AI models,
 * providing comprehensive statistics for reporting and analysis. It combines individual model
 * execution results into a single aggregated score using a specified aggregation strategy.
 * <p>
 * The class provides utility methods to analyze execution outcomes, including success/failure
 * rates, score statistics, and timing information.
 */
@Value
@Builder
public class AggregatedExecutionResult {

    /**
     * Name of the metric being evaluated (e.g., "AspectCritic", "Faithfulness").
     */
    String metricName;

    /**
     * All individual execution results from each model.
     * <p>
     * This list contains both successful and failed executions, allowing for complete
     * analysis of the execution batch.
     */
    @Singular
    List<ModelExecutionResult> results;

    /**
     * The final aggregated score computed from successful executions.
     * <p>
     * This value is calculated using the specified aggregation strategy (e.g., AVERAGE, MEDIAN)
     * and represents the consolidated metric score across all successful model executions.
     */
    Double aggregatedScore;

    /**
     * Name of the aggregation strategy used to compute the final score.
     * <p>
     * Common values include "AVERAGE", "MEDIAN", "MIN", "MAX", or custom strategy names.
     */
    String aggregationStrategy;

    /**
     * Timestamp when the aggregation was completed.
     * <p>
     * Defaults to the current time when the result object is created.
     */
    @Builder.Default
    Instant completedAt = Instant.now();

    /**
     * Filters and returns only the successful execution results.
     * <p>
     * A result is considered successful if it contains a score and no error.
     *
     * @return list of successful results, may be empty if all executions failed
     */
    public List<ModelExecutionResult> getSuccessfulResults() {
        return results.stream().filter(ModelExecutionResult::isSuccess).toList();
    }

    /**
     * Filters and returns only the failed execution results.
     * <p>
     * A result is considered failed if it contains an error or lacks a score.
     *
     * @return list of failed results, may be empty if all executions succeeded
     */
    public List<ModelExecutionResult> getFailedResults() {
        return results.stream().filter(r -> !r.isSuccess()).toList();
    }

    /**
     * Calculates the success rate of executions.
     *
     * @return success rate as a decimal value between 0.0 and 1.0,
     *         where 1.0 means all executions succeeded and 0.0 means all failed
     */
    public double getSuccessRate() {
        if (results.isEmpty()) {
            return 0.0;
        }
        return (double) getSuccessfulResults().size() / results.size();
    }

    /**
     * Computes statistical summary of scores from successful executions.
     * <p>
     * The statistics include count, sum, min, max, and average of all successful scores.
     *
     * @return optional containing score statistics if at least one execution succeeded,
     *         empty optional if all executions failed
     */
    public Optional<DoubleSummaryStatistics> getScoreStatistics() {
        var successful = getSuccessfulResults();
        if (successful.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(successful.stream()
                .flatMap(r -> r.getScore().stream())
                .mapToDouble(Double::doubleValue)
                .summaryStatistics());
    }

    /**
     * Determines the total execution duration across all models.
     * <p>
     * Since models are executed in parallel, this returns the maximum duration
     * among all individual executions (i.e., the time it took for the slowest model).
     *
     * @return total execution duration, or {@link Duration#ZERO} if no results exist
     */
    public Duration getTotalDuration() {
        return results.stream()
                .map(ModelExecutionResult::getDuration)
                .max(Duration::compareTo)
                .orElse(Duration.ZERO);
    }
}
