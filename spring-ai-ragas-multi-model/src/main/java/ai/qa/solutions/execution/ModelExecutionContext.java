package ai.qa.solutions.execution;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * Immutable context for a single model execution.
 * <p>
 * This class encapsulates all information about an execution request and its environment,
 * including the model being invoked, the metric being evaluated, and the prompt to be sent.
 * It serves as a comprehensive snapshot of the execution state and can be passed to
 * listeners for monitoring and logging purposes.
 * <p>
 * The context is immutable to ensure thread safety when executing models in parallel,
 * though it provides a {@link #withMetadata(String, Object)} method to create modified
 * copies with additional metadata.
 */
@Value
@Builder(toBuilder = true)
public class ModelExecutionContext {

    /**
     * Unique identifier for this execution, used for tracing and correlation.
     * <p>
     * Automatically generated as a random UUID if not explicitly provided.
     * This ID can be used to track the execution through logs and listeners.
     */
    @Builder.Default
    String executionId = UUID.randomUUID().toString();

    /**
     * Identifier of the model being executed (e.g., "gpt-4", "claude-3-opus").
     * <p>
     * This ID corresponds to the model name registered in the {@code ChatClientStore}.
     */
    String modelId;

    /**
     * Name of the metric being evaluated (e.g., "AspectCritic", "Faithfulness").
     * <p>
     * Used for logging, monitoring, and grouping results by metric type.
     */
    String metricName;

    /**
     * The rendered prompt that will be sent to the model.
     * <p>
     * This is the final prompt string after all template processing and variable
     * substitution has been applied.
     */
    String prompt;

    /**
     * Timestamp when the execution started.
     * <p>
     * Defaults to the current time when the context is created. Used to calculate
     * execution duration in the corresponding {@link ModelExecutionResult}.
     */
    @Builder.Default
    Instant startedAt = Instant.now();

    /**
     * Additional metadata for extensions and custom use cases.
     * <p>
     * This map can store arbitrary data such as sample inputs, configuration parameters,
     * or any other contextual information needed by listeners or custom processing logic.
     * The map is immutable; use {@link #withMetadata(String, Object)} to add entries.
     */
    @Builder.Default
    Map<String, Object> metadata = Map.of();

    /**
     * Creates a new context with an additional metadata entry.
     * <p>
     * This method returns a new instance with the specified key-value pair added to
     * the metadata map, leaving the original context unchanged.
     *
     * @param key   the metadata key
     * @param value the metadata value
     * @return a new context instance with the updated metadata
     */
    public ModelExecutionContext withMetadata(final String key, final Object value) {
        var newMetadata = new HashMap<>(this.metadata);
        newMetadata.put(key, value);
        return this.toBuilder().metadata(Map.copyOf(newMetadata)).build();
    }
}
