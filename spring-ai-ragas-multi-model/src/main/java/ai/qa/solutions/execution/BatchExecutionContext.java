package ai.qa.solutions.execution;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

/**
 * Context for a batch execution across multiple models.
 * <p>
 * This class encapsulates information about a multi-model execution request
 * before individual model executions begin. It provides a snapshot of the
 * execution parameters that will be used for all models in the batch.
 * <p>
 * This context is passed to listeners via {@link ModelExecutionListener#beforeAllExecutions(BatchExecutionContext)}
 * to allow logging, monitoring, and preparation before the parallel execution starts.
 */
@Value
@Builder(toBuilder = true)
public class BatchExecutionContext {

    /**
     * Name of the metric being evaluated (e.g., "AspectCritic", "Faithfulness").
     * <p>
     * Used for logging, monitoring, and grouping results by metric type.
     */
    String metricName;

    /**
     * The rendered prompt that will be sent to all models.
     * <p>
     * This is the final prompt string after all template processing and variable
     * substitution has been applied.
     */
    String prompt;

    /**
     * List of model IDs that will be executed.
     * <p>
     * This is the final list of models after considering custom model lists
     * from the request and available models in the ChatClientStore.
     */
    List<String> modelIds;

    /**
     * Additional metadata for extensions and custom use cases.
     * <p>
     * This map can store arbitrary data such as sample inputs, configuration parameters,
     * or any other contextual information needed by listeners.
     */
    @Builder.Default
    Map<String, Object> metadata = Map.of();
}
