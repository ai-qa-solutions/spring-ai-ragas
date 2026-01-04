package ai.qa.solutions.execution;

import ai.qa.solutions.chatclient.ChatClientStore;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.task.AsyncTaskExecutor;

/**
 * Executes model calls across multiple models in parallel with listener support.
 * <p>
 * This class provides a clean separation of concerns between:
 * <ul>
 *   <li>Metric logic (what to evaluate)</li>
 *   <li>Execution logic (how to run across models)</li>
 *   <li>Observation logic (listeners for monitoring/publishing)</li>
 * </ul>
 * <p>
 * The executor manages parallel execution across all configured models in the
 * {@link ChatClientStore}, aggregates the results using a specified strategy,
 * and notifies registered listeners at key lifecycle points.
 * <p>
 * <b>Thread Safety:</b> This class is thread-safe. Listeners can be added or
 * removed at any time, even during active executions.
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * executor.execute(
 *     ExecutionRequest.<Response>builder()
 *         .metricName("AspectCritic")
 *         .prompt(renderedPrompt)
 *         .responseType(Response.class)
 *         .scoreExtractor(Response::getScore)
 *         .build()
 * ).thenAccept(result -> System.out.println("Score: " + result));
 * }</pre>
 */
@Slf4j
public class MultiModelExecutor {

    /**
     * Store of configured chat clients for different models.
     */
    private final ChatClientStore chatClientStore;

    /**
     * Executor for running model calls asynchronously in parallel.
     */
    private final AsyncTaskExecutor taskExecutor;

    /**
     * Registered execution listeners, maintained in order by their priority.
     * Thread-safe copy-on-write implementation allows concurrent modifications.
     */
    private final List<ModelExecutionListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Default aggregation strategy to use when none is specified in the request.
     */
    private final ScoreAggregator defaultAggregator;

    /**
     * Creates a new executor with AVERAGE as the default aggregation strategy.
     *
     * @param chatClientStore store of configured AI model clients
     * @param taskExecutor    executor for parallel async operations
     */
    public MultiModelExecutor(final ChatClientStore chatClientStore, final AsyncTaskExecutor taskExecutor) {
        this(chatClientStore, taskExecutor, ScoreAggregator.AVERAGE);
    }

    /**
     * Creates a new executor with a custom default aggregation strategy.
     *
     * @param chatClientStore  store of configured AI model clients
     * @param taskExecutor     executor for parallel async operations
     * @param defaultAggregator default strategy for aggregating scores
     * @throws NullPointerException if any parameter is {@code null}
     */
    public MultiModelExecutor(
            final ChatClientStore chatClientStore,
            final AsyncTaskExecutor taskExecutor,
            final ScoreAggregator defaultAggregator) {
        this.chatClientStore = Objects.requireNonNull(chatClientStore, "chatClientStore");
        this.taskExecutor = Objects.requireNonNull(taskExecutor, "taskExecutor");
        this.defaultAggregator = Objects.requireNonNull(defaultAggregator, "defaultAggregator");
    }

    /**
     * Adds a listener for execution lifecycle events.
     * <p>
     * Listeners are automatically sorted by their {@link ModelExecutionListener#getOrder()}
     * priority, with lower values executing first.
     *
     * @param listener the listener to add
     * @return this executor instance for method chaining
     * @throws NullPointerException if listener is {@code null}
     */
    public MultiModelExecutor addListener(final ModelExecutionListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
        listeners.sort(Comparator.comparingInt(ModelExecutionListener::getOrder));
        return this;
    }

    /**
     * Removes a previously registered listener.
     *
     * @param listener the listener to remove
     * @return this executor instance for method chaining
     */
    public MultiModelExecutor removeListener(final ModelExecutionListener listener) {
        listeners.remove(listener);
        return this;
    }

    /**
     * Executes the request across all configured models and aggregates results.
     * <p>
     * This method uses the default aggregation strategy specified during executor construction.
     * All configured models in the {@link ChatClientStore} are invoked in parallel, and their
     * results are combined into a single score.
     *
     * @param request execution request containing the prompt and score extraction logic
     * @param <R>     the response type expected from the models
     * @return a future that completes with the aggregated score
     * @throws IllegalStateException if no models are configured in the ChatClientStore,
     *                               or if all model executions fail
     */
    public <R> CompletableFuture<Double> execute(final ExecutionRequest<R> request) {
        return execute(request, defaultAggregator);
    }

    /**
     * Executes the request with a custom aggregation strategy.
     * <p>
     * This variant allows overriding the default aggregator for a specific execution,
     * useful when different metrics require different aggregation approaches.
     * <p>
     * If the request contains a custom list of model IDs, only those models will be used.
     * Otherwise, all models from the {@link ChatClientStore} will be invoked.
     *
     * @param request    execution request containing the prompt and score extraction logic
     * @param aggregator score aggregation strategy to use for this execution
     * @param <R>        the response type expected from the models
     * @return a future that completes with the aggregated score
     * @throws IllegalStateException if no models are configured in the ChatClientStore,
     *                               or if all model executions fail
     */
    public <R> CompletableFuture<Double> execute(final ExecutionRequest<R> request, final ScoreAggregator aggregator) {
        // Use custom model list if provided, otherwise use all models from store
        final List<String> modelIds =
                (request.getModelIds() != null && !request.getModelIds().isEmpty())
                        ? request.getModelIds()
                        : chatClientStore.getModelIds();

        if (modelIds.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalStateException("No models configured in ChatClientStore"));
        }

        // Notify listeners before all executions start
        final BatchExecutionContext batchContext = BatchExecutionContext.builder()
                .metricName(request.getMetricName())
                .prompt(request.getPrompt())
                .modelIds(modelIds)
                .metadata(request.getMetadata())
                .build();

        notifyBeforeAllExecutions(batchContext);

        final List<CompletableFuture<ModelExecutionResult>> futures = modelIds.stream()
                .map(modelId -> executeOnModel(modelId, request))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApplyAsync(v -> aggregateResults(request, futures, aggregator), taskExecutor);
    }

    /**
     * Executes the request on a single model asynchronously.
     * <p>
     * This method creates an execution context, notifies listeners before execution,
     * invokes the model, extracts the score, and notifies listeners after completion.
     * Exceptions are caught and converted to failed results.
     *
     * @param modelId the ID of the model to execute on
     * @param request the execution request
     * @param <R>     the response type
     * @return a future that completes with the execution result (success or failure)
     */
    private <R> CompletableFuture<ModelExecutionResult> executeOnModel(
            final String modelId, final ExecutionRequest<R> request) {
        final ModelExecutionContext context = ModelExecutionContext.builder()
                .modelId(modelId)
                .metricName(request.getMetricName())
                .prompt(request.getPrompt())
                .metadata(request.getMetadata())
                .build();

        notifyBeforeExecution(context);

        return taskExecutor
                .submitCompletable(() -> {
                    final ChatClient client = chatClientStore.get(modelId);
                    final R response = client.prompt(request.getPrompt()).call().entity(request.getResponseType());
                    final Double score = request.getScoreExtractor().apply(response);
                    return ModelExecutionResult.success(context, score, response);
                })
                .exceptionally(ex -> ModelExecutionResult.failure(context, ex))
                .whenComplete((result, ex) -> {
                    if (result != null) {
                        notifyAfterExecution(result);
                    }
                });
    }

    /**
     * Aggregates results from all model executions into a single score.
     * <p>
     * This method waits for all futures to complete, extracts successful scores,
     * applies the aggregation strategy, and notifies listeners with the final result.
     *
     * @param request    the original execution request
     * @param futures    list of futures representing individual model executions
     * @param aggregator the aggregation strategy to apply
     * @param <R>        the response type
     * @return the aggregated score
     * @throws IllegalStateException if all model executions failed
     */
    private <R> Double aggregateResults(
            final ExecutionRequest<R> request,
            final List<CompletableFuture<ModelExecutionResult>> futures,
            final ScoreAggregator aggregator) {
        final List<ModelExecutionResult> results =
                futures.stream().map(CompletableFuture::join).toList();

        final List<Double> successfulScores = results.stream()
                .filter(ModelExecutionResult::isSuccess)
                .flatMap(r -> r.getScore().stream())
                .toList();

        if (successfulScores.isEmpty()) {
            throw new IllegalStateException(
                    "All " + results.size() + " models failed for metric: " + request.getMetricName());
        }

        final double aggregatedScore = aggregator.aggregate(successfulScores);

        final AggregatedExecutionResult aggregatedResult = AggregatedExecutionResult.builder()
                .metricName(request.getMetricName())
                .results(results)
                .aggregatedScore(aggregatedScore)
                .aggregationStrategy(aggregator.getName())
                .build();

        notifyAfterAggregation(aggregatedResult);

        return aggregatedScore;
    }

    /**
     * Notifies all registered listeners before all model executions start.
     * <p>
     * Exceptions from listeners are caught and logged to prevent disruption.
     *
     * @param context the batch execution context
     */
    private void notifyBeforeAllExecutions(final BatchExecutionContext context) {
        for (final ModelExecutionListener listener : listeners) {
            try {
                listener.beforeAllExecutions(context);
            } catch (Exception e) {
                log.error(
                        "Listener {} failed in beforeAllExecutions: {}",
                        listener.getClass().getSimpleName(),
                        e.getMessage(),
                        e);
            }
        }
    }

    /**
     * Notifies all registered listeners before a model execution starts.
     * <p>
     * Exceptions from listeners are caught and logged to prevent disruption.
     *
     * @param context the execution context
     */
    private void notifyBeforeExecution(final ModelExecutionContext context) {
        for (final ModelExecutionListener listener : listeners) {
            try {
                listener.beforeExecution(context);
            } catch (Exception e) {
                log.error(
                        "Listener {} failed in beforeExecution: {}",
                        listener.getClass().getSimpleName(),
                        e.getMessage(),
                        e);
            }
        }
    }

    /**
     * Notifies all registered listeners after a model execution completes.
     * <p>
     * Exceptions from listeners are caught and logged to prevent disruption.
     *
     * @param result the execution result (success or failure)
     */
    private void notifyAfterExecution(final ModelExecutionResult result) {
        for (final ModelExecutionListener listener : listeners) {
            try {
                listener.afterExecution(result);
            } catch (Exception e) {
                log.error(
                        "Listener {} failed in afterExecution: {}",
                        listener.getClass().getSimpleName(),
                        e.getMessage(),
                        e);
            }
        }
    }

    /**
     * Notifies all registered listeners after all results are aggregated.
     * <p>
     * Exceptions from listeners are caught and logged to prevent disruption.
     *
     * @param result the aggregated result with statistics
     */
    private void notifyAfterAggregation(final AggregatedExecutionResult result) {
        for (final ModelExecutionListener listener : listeners) {
            try {
                listener.afterAggregation(result);
            } catch (Exception e) {
                log.error(
                        "Listener {} failed in afterAggregation: {}",
                        listener.getClass().getSimpleName(),
                        e.getMessage(),
                        e);
            }
        }
    }

    /**
     * Request object encapsulating all parameters needed for a multi-model execution.
     * <p>
     * This class defines what metric to evaluate, what prompt to send to models,
     * how to parse the response, and how to extract the score from the parsed response.
     * <p>
     * Instances are typically created using the builder pattern:
     * <pre>{@code
     * ExecutionRequest.<MyResponse>builder()
     *     .metricName("MyMetric")
     *     .prompt("Evaluate this: ...")
     *     .responseType(MyResponse.class)
     *     .scoreExtractor(MyResponse::getScore)
     *     .metadata(Map.of("key", "value"))
     *     .build()
     * }</pre>
     *
     * @param <R> the response type expected from the model
     */
    @Value
    @Builder
    public static class ExecutionRequest<R> {

        /**
         * Name of the metric being evaluated.
         * <p>
         * Used for logging, monitoring, and listener notifications.
         */
        String metricName;

        /**
         * The rendered prompt to send to all models.
         * <p>
         * This should be the final prompt string after all template processing.
         */
        String prompt;

        /**
         * The expected response class for structured output parsing.
         * <p>
         * The ChatClient will attempt to deserialize the model's response into this type.
         */
        Class<R> responseType;

        /**
         * Function to extract the numeric score from the parsed response.
         * <p>
         * This function is applied to the successfully parsed response object
         * to obtain the final metric score.
         */
        Function<R, Double> scoreExtractor;

        /**
         * Optional list of specific model IDs to execute on.
         * <p>
         * If provided and not empty, execution will be limited to only these models.
         * If null or empty, all models from the {@link ChatClientStore} will be used.
         * This allows metrics to override the default behavior and execute on a subset of models.
         */
        @lombok.Builder.Default
        List<String> modelIds = List.of();

        /**
         * Additional metadata to be passed to execution listeners.
         * <p>
         * This can include sample data, configuration parameters, or any other
         * contextual information needed by listeners or for debugging.
         */
        @lombok.Builder.Default
        java.util.Map<String, Object> metadata = java.util.Map.of();
    }
}
