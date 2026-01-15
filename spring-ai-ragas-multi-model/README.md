# Spring AI Multi-Model Support

<!-- TOC -->
* [Spring AI Multi-Model Support](#spring-ai-multi-model-support)
  * [Features](#features)
  * [Installation](#installation)
  * [Quick Start](#quick-start)
    * [1. Configure Your Models](#1-configure-your-models)
    * [2. Use in Your Code](#2-use-in-your-code)
  * [Configuration Reference](#configuration-reference)
    * [Provider Configuration](#provider-configuration)
  * [Advanced Usage](#advanced-usage)
    * [Working with OpenRouter](#working-with-openrouter-multiple-ai-providers)
    * [Multiple Providers Configuration](#multiple-providers-configuration)
    * [Real-World Example: OpenRouter + cloud.ru](#real-world-example-openrouter--cloudru)
    * [Error Handling](#error-handling)
  * [API Reference](#api-reference)
    * [ChatClientStore](#chatclientstore)
    * [EmbeddingModelStore](#embeddingmodelstore)
  * [Multi-Model Execution API](#multi-model-execution-api)
  * [How It Works](#how-it-works)
  * [Examples](#examples)
  * [Complete Working Example](#complete-working-example)

<!-- TOC -->

-------------

A Spring Boot autoconfiguration library that enables seamless management of multiple AI chat models and embedding
models in Spring AI applications.

This module provides thread-safe stores for managing multiple pre-configured model instances with individual settings.

## Features

- **Autoconfiguration** - Zero-code setup for multiple chat and embedding models
- **Multi-Model Support** - Manage multiple AI models with different configurations simultaneously
- **Model-Specific Options** - Configure individual settings (temperature, max tokens, dimensions) per model

## Installation

Add the dependency to your `pom.xml`

```xml

<dependency>
    <groupId>io.github.ai-qa-solutions</groupId>
    <artifactId>spring-ai-ragas-multi-model</artifactId>
    <version>LATEST</version>
</dependency>
```

Or for Gradle

```gradle
implementation 'io.github.ai-qa-solutions:spring-ai-ragas-multi-model:1.0.0'
```

## Quick Start

### 1. Configure Your Models

Add to your `application.yml`

```yaml
spring:
  ai:
    # Base Spring AI configuration (using OpenRouter as example)
    openai:
      base-url: https://openrouter.ai/api
      api-key: ${OPENROUTER_API_KEY}
      chat:
        options:
          model: google/gemini-2.5-flash
          temperature: 0.0
      embedding:
        base-url: https://openrouter.ai/api
        api-key: ${OPENROUTER_API_KEY}
        options:
          model: openai/text-embedding-3-small
          dimensions: 1024

    ragas:
      # Multi-provider configuration
      providers:
        auto-detect-beans: false
        openai-compatible:
          - name: openrouter
            base-url: https://openrouter.ai/api
            api-key: ${OPENROUTER_API_KEY}
            # Chat models - multiple providers via OpenRouter
            chat-models:
              - { id: openai/gpt-4o }
              - { id: openai/gpt-4o-mini }
              - { id: anthropic/claude-3.5-sonnet }
              - { id: anthropic/claude-3-haiku }
              - { id: google/gemini-2.5-flash }
              - { id: google/gemini-1.5-pro }
            # Embedding models
            embedding-models:
              - { id: openai/text-embedding-3-small, dimensions: 1024 }
              - { id: openai/text-embedding-3-large, dimensions: 3072 }
              - { id: qwen/qwen3-embedding-8b, dimensions: 1024 }
              - { id: google/gemini-embedding-001, dimensions: 768 }
        default-provider:
          enabled: false
        default-options:
          temperature: 0.0
          max-tokens: 1000
          top-p: 1.0
        embedding-default-options:
          dimensions: 1024
```

### 2. Use in Your Code

#### Chat Models

```java

@Service
public class ChatService {

    private final ChatClientStore chatClientStore;

    public ChatService(ChatClientStore chatClientStore) {
        this.chatClientStore = chatClientStore;
    }

    public String chat(String message, String modelId) {
        // Get specific model
        ChatClient client = chatClientStore.get(modelId);
        return client.prompt()
                .user(message)
                .call()
                .content();
    }

    public String chatWithDefault(String message) {
        // Use default model
        return chatClientStore.getDefault()
                .prompt()
                .user(message)
                .call()
                .content();
    }

    public List<String> getAllModelIds() {
        // List all available models
        return chatClientStore.getModelIds();
    }
}
```

#### Embedding Models

```java

@Service
public class EmbeddingService {

    private final EmbeddingModelStore embeddingModelStore;

    public EmbeddingService(EmbeddingModelStore embeddingModelStore) {
        this.embeddingModelStore = embeddingModelStore;
    }

    public float[] embed(String text, String modelId) {
        // Get specific embedding model
        EmbeddingModel model = embeddingModelStore.get(modelId);
        return model.embed(text);
    }

    public void embedWithAllModels(String text) {
        // Iterate over all configured models
        for (EmbeddingModel model : embeddingModelStore.getAll()) {
            float[] embedding = model.embed(text);
            // Process embedding...
        }
    }

    public boolean isModelAvailable(String modelId) {
        return embeddingModelStore.contains(modelId);
    }
}
```

## Configuration Reference

### Provider Configuration

|                                   Property                                    |  Type   | Default |                               Description                               |
|-------------------------------------------------------------------------------|---------|---------|-------------------------------------------------------------------------|
| `spring.ai.ragas.providers.auto-detect-beans`                                 | Boolean | true    | Auto-detect external ChatModel beans (GigaChat, Anthropic native, etc.) |
| `spring.ai.ragas.providers.openai-compatible[].name`                          | String  | -       | Provider name for identification                                        |
| `spring.ai.ragas.providers.openai-compatible[].base-url`                      | String  | -       | **Required**. Base URL of the OpenAI-compatible API                     |
| `spring.ai.ragas.providers.openai-compatible[].api-key`                       | String  | -       | **Required**. API key for authentication                                |
| `spring.ai.ragas.providers.openai-compatible[].chat-models[].id`              | String  | -       | **Required**. Unique model identifier                                   |
| `spring.ai.ragas.providers.openai-compatible[].embedding-models[].id`         | String  | -       | **Required**. Unique embedding model identifier                         |
| `spring.ai.ragas.providers.openai-compatible[].embedding-models[].dimensions` | Integer | -       | Optional. Vector dimensions for this model                              |
| `spring.ai.ragas.providers.default-provider.enabled`                          | Boolean | true    | Enable default OpenAI provider via Spring AI autoconfiguration          |
| `spring.ai.ragas.providers.default-options.temperature`                       | Double  | 0.0     | Default temperature for all models                                      |
| `spring.ai.ragas.providers.default-options.max-tokens`                        | Integer | 1000    | Default maximum tokens in response                                      |
| `spring.ai.ragas.providers.default-options.top-p`                             | Double  | 1.0     | Default nucleus sampling parameter                                      |
| `spring.ai.ragas.providers.embedding-default-options.dimensions`              | Integer | 1024    | Default vector dimensions for all embedding models                      |

## Advanced Usage

### Working with OpenRouter (Multiple AI Providers)

OpenRouter provides unified access to multiple AI providers. Complete configuration example

```yaml
spring:
  ai:
    retry:
      on-http-codes: [ 429 ]
      on-client-errors: true
      backoff:
        initial-interval: 2000ms
        max-interval: 30000ms
        multiplier: 2
    openai:
      base-url: https://openrouter.ai/api
      api-key: ${OPENROUTER_API_KEY}
      chat:
        options:
          model: google/gemini-2.5-flash
          temperature: 0.0
      embedding:
        base-url: https://openrouter.ai/api
        api-key: ${OPENROUTER_API_KEY}
        options:
          model: openai/text-embedding-3-small
          dimensions: 1024

    ragas:
      providers:
        auto-detect-beans: false
        openai-compatible:
          - name: openrouter
            base-url: https://openrouter.ai/api
            api-key: ${OPENROUTER_API_KEY}
            chat-models:
              # Premium tier models
              - { id: anthropic/claude-3.5-sonnet }
              - { id: openai/gpt-4o }
              - { id: google/gemini-1.5-pro }
              # Efficient tier models
              - { id: google/gemini-2.5-flash }
              - { id: anthropic/claude-3-haiku }
              - { id: openai/gpt-4o-mini }
              - { id: deepseek/deepseek-chat }
              # Open-source models
              - { id: meta-llama/llama-3.3-70b-instruct }
              - { id: qwen/qwen-2.5-72b-instruct }
            embedding-models:
              - { id: openai/text-embedding-3-large, dimensions: 3072 }
              - { id: openai/text-embedding-3-small, dimensions: 1024 }
        default-provider:
          enabled: false
        default-options:
          temperature: 0.0
          max-tokens: 1000
          top-p: 1.0
        embedding-default-options:
          dimensions: 1024
```

### Multiple Providers Configuration

You can configure multiple providers (e.g., separate premium and efficient tiers):

```yaml
spring:
  ai:
    ragas:
      providers:
        openai-compatible:
          # Premium tier provider
          - name: openrouter-premium
            base-url: https://openrouter.ai/api
            api-key: ${OPENROUTER_API_KEY}
            chat-models:
              - { id: anthropic/claude-3.5-sonnet }
              - { id: openai/gpt-4o }
            embedding-models:
              - { id: openai/text-embedding-3-large, dimensions: 3072 }
          # Efficient tier provider
          - name: openrouter-efficient
            base-url: https://openrouter.ai/api
            api-key: ${OPENROUTER_API_KEY}
            chat-models:
              - { id: google/gemini-2.5-flash }
              - { id: openai/gpt-4o-mini }
              - { id: deepseek/deepseek-chat }
            embedding-models:
              - { id: openai/text-embedding-3-small, dimensions: 1024 }
        default-provider:
          enabled: false
        default-options:
          temperature: 0.0
          max-tokens: 1000
```

### Real-World Example: OpenRouter + cloud.ru

A complete working example combining two different API providers - OpenRouter for global models and cloud.ru Evolution for Russian-hosted models:

#### application.yml

```yaml
spring:
  ai:
    retry:
      on-http-codes: [ 429 ]
      on-client-errors: true
      backoff:
        initial-interval: 2000ms
        max-interval: 30000ms
        multiplier: 2
    openai:
      base-url: https://openrouter.ai/api
      api-key: ${OPENROUTER_API_KEY}
      chat:
        options:
          model: google/gemini-2.0-flash-001
          temperature: 0.0
      embedding:
        base-url: https://openrouter.ai/api
        api-key: ${OPENROUTER_API_KEY}
        options:
          model: openai/text-embedding-3-small
          dimensions: 1024

    ragas:
      providers:
        auto-detect-beans: false
        openai-compatible:
          # Provider 1: OpenRouter - global models
          - name: openrouter
            base-url: https://openrouter.ai/api
            api-key: ${OPENROUTER_API_KEY}
            chat-models:
              - { id: anthropic/claude-3.5-sonnet }
              - { id: openai/gpt-4o }
              - { id: google/gemini-2.0-flash-exp }
              - { id: meta-llama/llama-3.3-70b-instruct }
            embedding-models:
              - { id: openai/text-embedding-3-large, dimensions: 3072 }
              - { id: openai/text-embedding-3-small, dimensions: 1536 }
          # Provider 2: cloud.ru Evolution - Russian-hosted models
          - name: cloudru
            base-url: https://foundation-models.api.cloud.ru
            api-key: ${CLOUD_RU_API_KEY}
            chat-models:
              - { id: Qwen/Qwen3-235B-A22B-Instruct-2507 }
              - { id: openai/gpt-oss-120b }
              - { id: t-tech/T-pro-it-2.0 }
        default-provider:
          enabled: false
        default-options:
          temperature: 0.0
          max-tokens: 1000
          top-p: 1.0
        embedding-default-options:
          dimensions: 1024
```

#### Supported OpenAI-Compatible Providers

|      Provider      |                 Base URL                 |            Notes            |
|--------------------|------------------------------------------|-----------------------------|
| OpenRouter         | `https://openrouter.ai/api`              | Access to 200+ models       |
| cloud.ru Evolution | `https://foundation-models.api.cloud.ru` | Russian-hosted              |
| Groq               | `https://api.groq.com/openai`            | Fast inference              |
| Together AI        | `https://api.together.xyz`               | Open-source models          |
| Fireworks AI       | `https://api.fireworks.ai/inference`     | Fast open-source models     |
| Azure OpenAI       | `https://{resource}.openai.azure.com`    | Enterprise Azure deployment |
| Ollama             | `http://localhost:11434`                 | Local models                |

### Error Handling

```java

@Service
public class SafeChatService {

    private final ChatClientStore chatClientStore;

    public String chat(String message, String modelId) {
        try {
            ChatClient client = chatClientStore.get(modelId);
            return client.prompt().user(message).call().content();
        } catch (IllegalArgumentException e) {
            // Model not found - use default
            return chatClientStore.getDefault()
                    .prompt()
                    .user(message)
                    .call()
                    .content();
        }
    }
}
```

## API Reference

### ChatClientStore

Thread-safe store for managing multiple ChatClient instances.

|               Method               |                                Description                                 |
|------------------------------------|----------------------------------------------------------------------------|
| `ChatClient get(String modelId)`   | Get ChatClient by model ID. Throws `IllegalArgumentException` if not found |
| `ChatClient getDefault()`          | Get the default ChatClient configured by Spring AI                         |
| `List<ChatClient> getAll()`        | Get all registered ChatClient instances                                    |
| `List<String> getModelIds()`       | Get list of all registered model IDs                                       |
| `boolean contains(String modelId)` | Check if model ID exists                                                   |
| `int size()`                       | Get number of registered models                                            |

### EmbeddingModelStore

Thread-safe store for managing multiple EmbeddingModel instances.

|                Method                |                                  Description                                   |
|--------------------------------------|--------------------------------------------------------------------------------|
| `EmbeddingModel get(String modelId)` | Get EmbeddingModel by model ID. Throws `IllegalArgumentException` if not found |
| `EmbeddingModel getDefault()`        | Get the default EmbeddingModel configured by Spring AI                         |
| `List<EmbeddingModel> getAll()`      | Get all registered EmbeddingModel instances                                    |
| `List<String> getModelIds()`         | Get list of all registered model IDs                                           |
| `boolean contains(String modelId)`   | Check if model ID exists                                                       |
| `int size()`                         | Get number of registered models                                                |

## Multi-Model Execution API

The Multi-Model Execution API provides a powerful framework for executing AI metrics across multiple models in parallel,
aggregating results, and monitoring execution through listeners.

### Key Features

- **Parallel Execution** - Execute requests across all configured models simultaneously
- **Score Aggregation** - Combine multiple model scores using various strategies (AVERAGE, MEDIAN, MIN, MAX, CONSENSUS)
- **Execution Listeners** - Monitor and react to execution events (before, after, aggregation)
- **Comprehensive Results** - Get detailed statistics including success rates, durations, and score distributions
- **Visual Charts** - Automatic ASCII chart rendering for score comparison

### Core Components

#### MultiModelExecutor

The main executor class that manages parallel execution across multiple models.

```java

@Service
public class MetricService {

    private final MultiModelExecutor executor;

    public MetricService(MultiModelExecutor executor) {
        this.executor = executor;
    }

    public Double evaluateMetric(String prompt) {
        return executor.execute(
                ExecutionRequest.<Response>builder()
                        .metricName("AspectCritic")
                        .prompt(prompt)
                        .responseType(Response.class)
                        .scoreExtractor(Response::getScore)
                        .build()
        ).join();
    }

    record Response(double score, String reasoning) {
    }
}
```

#### Score Aggregators

Built-in aggregation strategies for combining scores from multiple models:

```java

@Service
public class AggregationService {

    private final MultiModelExecutor executor;

    public void demonstrateAggregators(ExecutionRequest<?> request) {
        // Use AVERAGE (default)
        Double avgScore = executor.execute(request).join();

        // Use MEDIAN for robustness against outliers
        Double medianScore = executor.execute(request, ScoreAggregator.MEDIAN).join();

        // Use MIN for conservative estimates
        Double minScore = executor.execute(request, ScoreAggregator.MIN).join();

        // Use MAX for optimistic estimates
        Double maxScore = executor.execute(request, ScoreAggregator.MAX).join();

        // Use CONSENSUS - requires models to agree within tolerance
        Double consensusScore = executor.execute(
                request,
                ScoreAggregator.consensus(0.1)  // tolerance: 0.1
        ).join();
    }
}
```

#### Custom Aggregators

Create custom aggregation logic:

```java

@Service
public class CustomAggregatorService {

    private final MultiModelExecutor executor;

    public Double executeWithCustomAggregator(ExecutionRequest<?> request) {
        ScoreAggregator weightedAverage = new ScoreAggregator() {
            @Override
            public double aggregate(List<Double> scores) {
                // Weighted average: first model has 50% weight, others split remaining 50%
                double firstScore = scores.get(0) * 0.5;
                double otherAvg = scores.stream()
                        .skip(1)
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0) * 0.5;
                return firstScore + otherAvg;
            }

            @Override
            public String getName() {
                return "WEIGHTED_AVERAGE";
            }
        };

        return executor.execute(request, weightedAverage).join();
    }
}
```

### Execution Listeners

Monitor execution lifecycle with listeners:

#### Built-in LoggingExecutionListener

Automatically registered by default. Logs execution events and renders ASCII charts:

```
[AspectCritic] Aggregation complete: score=0.82, strategy=AVERAGE, success=3/3, duration=1250ms (min=0.75, max=0.89, avg=0.82)

[AspectCritic] Scores by model (n=3, min=0.75 [gpt-4o-mini], max=0.89 [claude-3.5-sonnet], avg=0.82, aggregated=0.82, strategy=AVERAGE)
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│0.75                                                                                     0.89 │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│gpt-4o-mini       │█████████████████████████████████████████████████████              0.75   │
│claude-3-haiku    │█████████████████████████████████████████████████████████████      0.80   │
│claude-3.5-sonnet │████████████████████████████████████████████████████████████████████ 0.89 │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

#### Custom Listeners

Create custom listeners for metrics collection, caching, or external integrations:

```java

@Component
public class MetricsCollectionListener implements ModelExecutionListener {

    private final MeterRegistry meterRegistry;

    @Override
    public void afterExecution(ModelExecutionResult result) {
        // Record execution duration per model
        Timer.builder("ai.model.execution.duration")
                .tag("model", result.getContext().getModelId())
                .tag("metric", result.getContext().getMetricName())
                .tag("success", String.valueOf(result.isSuccess()))
                .register(meterRegistry)
                .record(result.getDuration());
    }

    @Override
    public void afterAggregation(AggregatedExecutionResult result) {
        // Record aggregated scores
        Gauge.builder("ai.metric.score", result, AggregatedExecutionResult::getAggregatedScore)
                .tag("metric", result.getMetricName())
                .tag("strategy", result.getAggregationStrategy())
                .register(meterRegistry);

        // Record success rate
        Gauge.builder("ai.metric.success.rate", result, AggregatedExecutionResult::getSuccessRate)
                .tag("metric", result.getMetricName())
                .register(meterRegistry);
    }

    @Override
    public int getOrder() {
        return 100; // Run after logging listener
    }
}
```

```java

@Component
public class CachingListener implements ModelExecutionListener {

    private final Cache<String, Double> cache;

    @Override
    public void afterAggregation(AggregatedExecutionResult result) {
        // Cache aggregated scores
        String cacheKey = generateCacheKey(result);
        cache.put(cacheKey, result.getAggregatedScore());
    }

    private String generateCacheKey(AggregatedExecutionResult result) {
        return result.getMetricName() + ":" +
                result.getAggregationStrategy() + ":" +
                result.getResults().get(0).getContext().getPrompt().hashCode();
    }
}
```

### Advanced Usage Examples

#### Parallel Execution with Multiple Metrics

```java

@Service
public class EvaluationService {

    private final MultiModelExecutor executor;

    public Map<String, Double> evaluateAllMetrics(String answer, String context) {
        // Execute multiple metrics in parallel
        CompletableFuture<Double> faithfulness = executor.execute(
                createRequest("Faithfulness", buildPrompt("faithfulness", answer, context))
        );

        CompletableFuture<Double> relevancy = executor.execute(
                createRequest("AnswerRelevancy", buildPrompt("relevancy", answer, context))
        );

        CompletableFuture<Double> correctness = executor.execute(
                createRequest("Correctness", buildPrompt("correctness", answer, context))
        );

        // Wait for all to complete
        CompletableFuture.allOf(faithfulness, relevancy, correctness).join();

        return Map.of(
                "faithfulness", faithfulness.join(),
                "relevancy", relevancy.join(),
                "correctness", correctness.join()
        );
    }

    private ExecutionRequest<MetricResponse> createRequest(String metricName, String prompt) {
        return ExecutionRequest.<MetricResponse>builder()
                .metricName(metricName)
                .prompt(prompt)
                .responseType(MetricResponse.class)
                .scoreExtractor(MetricResponse::score)
                .metadata(Map.of("timestamp", Instant.now()))
                .build();
    }

    record MetricResponse(double score, String reasoning) {
    }
}
```

#### Using Different Aggregators per Metric

```java

@Service
public class AdaptiveMetricService {

    private final MultiModelExecutor executor;

    public EvaluationResults evaluate(String text) {
        // Use CONSENSUS for critical metrics (require agreement)
        Double faithfulness = executor.execute(
                createRequest("Faithfulness", text),
                ScoreAggregator.consensus(0.05)  // Strict tolerance
        ).join();

        // Use MEDIAN for robustness against outliers
        Double relevancy = executor.execute(
                createRequest("Relevancy", text),
                ScoreAggregator.MEDIAN
        ).join();

        // Use MIN for conservative estimates
        Double safety = executor.execute(
                createRequest("Safety", text),
                ScoreAggregator.MIN  // Most conservative
        ).join();

        return new EvaluationResults(faithfulness, relevancy, safety);
    }

    record EvaluationResults(double faithfulness, double relevancy, double safety) {
    }
}
```

#### Accessing Detailed Results

```java

@Service
public class DetailedAnalysisService {

    private final MultiModelExecutor executor;

    public DetailedMetricResult analyzeWithDetails(String prompt) {
        // Create custom listener to capture detailed results
        final AtomicReference<AggregatedExecutionResult> detailedResult = new AtomicReference<>();

        ModelExecutionListener captureListener = new ModelExecutionListener() {
            @Override
            public void afterAggregation(AggregatedExecutionResult result) {
                detailedResult.set(result);
            }
        };

        executor.addListener(captureListener);

        try {
            Double score = executor.execute(createRequest(prompt)).join();
            AggregatedExecutionResult result = detailedResult.get();

            return new DetailedMetricResult(
                    score,
                    result.getSuccessRate(),
                    result.getTotalDuration(),
                    result.getScoreStatistics().orElse(null),
                    result.getSuccessfulResults().stream()
                            .collect(Collectors.toMap(
                                    r -> r.getContext().getModelId(),
                                    r -> r.getScore().orElse(0.0)
                            ))
            );
        } finally {
            executor.removeListener(captureListener);
        }
    }

    record DetailedMetricResult(
            double aggregatedScore,
            double successRate,
            Duration totalDuration,
            DoubleSummaryStatistics scoreStats,
            Map<String, Double> scoresByModel
    ) {
    }
}
```

### Execution Result Classes

#### AggregatedExecutionResult

Contains comprehensive statistics from multi-model execution:

```java

@Service
public class ResultAnalysisService {

    public void analyzeAggregatedResult(AggregatedExecutionResult result) {
        // Basic properties
        String metricName = result.getMetricName();
        Double aggregatedScore = result.getAggregatedScore();
        String strategy = result.getAggregationStrategy();
        Instant completedAt = result.getCompletedAt();

        // Filter results
        List<ModelExecutionResult> successful = result.getSuccessfulResults();
        List<ModelExecutionResult> failed = result.getFailedResults();

        // Statistics
        double successRate = result.getSuccessRate();  // 0.0 to 1.0
        Duration totalDuration = result.getTotalDuration();  // Max of all executions

        // Score statistics (min, max, average, count)
        Optional<DoubleSummaryStatistics> stats = result.getScoreStatistics();
        if (stats.isPresent()) {
            double min = stats.get().getMin();
            double max = stats.get().getMax();
            double avg = stats.get().getAverage();
            long count = stats.get().getCount();
        }
    }
}
```

#### ModelExecutionResult

Individual model execution result:

```java

@Service
public class ModelResultAnalysisService {

    public void analyzeModelResult(ModelExecutionResult result) {
        // Execution context
        ModelExecutionContext context = result.getContext();
        String modelId = context.getModelId();
        String metricName = context.getMetricName();
        String prompt = context.getPrompt();

        // Result data
        Optional<Double> score = result.getScore();
        Object rawResponse = result.getRawResponse();
        Throwable error = result.getError();  // If failed

        // Status
        boolean success = result.isSuccess();
        Duration duration = result.getDuration();
        Instant completedAt = result.getCompletedAt();
    }
}
```

### Listener Priority

Control listener execution order using `getOrder()`:

```java

@Component
public class HighPriorityListener implements ModelExecutionListener {
    @Override
    public int getOrder() {
        return -100;  // Runs before default listeners
    }
}

@Component
public class LowPriorityListener implements ModelExecutionListener {
    @Override
    public int getOrder() {
        return 100;  // Runs after default listeners
    }
}

// Built-in LoggingExecutionListener has order = Integer.MIN_VALUE (highest priority)
```

### Error Handling

The executor handles failures gracefully:

```java

@Service
public class ErrorHandlingService {

    private final MultiModelExecutor executor;

    public void handleExecutionErrors(ExecutionRequest<?> request) {
        // Partial failures - aggregates successful results
        try {
            Double score = executor.execute(request).join();
            // Even if some models fail, you get aggregated score from successful ones
        } catch (CompletionException e) {
            if (e.getCause() instanceof IllegalStateException) {
                // All models failed
                log.error("All models failed for metric", e);
            }
        }

        // No models configured
        try {
            Double score = executor.execute(request).join();
        } catch (CompletionException e) {
            if (e.getMessage().contains("No models configured")) {
                // Handle no models case
            }
        }
    }
}
```

### Thread Safety

All execution classes are thread-safe and designed for concurrent use:

- `MultiModelExecutor` - Thread-safe, can execute multiple requests concurrently
- Listeners use `CopyOnWriteArrayList` - Can be added/removed during execution
- Result objects - Immutable value objects

```java

@Service
public class ConcurrentExecutionService {

    private final MultiModelExecutor executor;

    public List<Double> executeConcurrently(List<String> prompts) {
        // Safe concurrent execution
        List<CompletableFuture<Double>> futures = prompts.stream()
                .map(prompt -> executor.execute(createRequest(prompt)))
                .toList();

        // Wait for all
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Collect results
        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    private ExecutionRequest<?> createRequest(String prompt) {
        // Implementation here
        return null;
    }
}
```

## How It Works

### Multi-Provider Architecture

The autoconfiguration supports three layers of providers that work together:

1. **Layer 1: External ChatModel beans** - Auto-detected GigaChat, Anthropic native clients, etc.
2. **Layer 2: OpenAI-compatible providers** - Groq, cloud.ru, Azure OpenAI, OpenRouter via `mutate()` pattern
3. **Layer 3: Default OpenAI models** - Backward compatible with standard Spring AI configuration

### Chat Models

1. **Auto-Configuration**: `MultiProviderAutoConfiguration` detects configured providers
2. **Model Creation**: For each model in `openai-compatible[].chat-models`, a separate `ChatClient` is created with:
   - **Independent Configuration**: Uses `mutate()` pattern for isolated API connections per provider
   - Model-specific ID
   - Individual or default options (temperature, max tokens, top-p)
3. **Store Initialization**: All clients are registered in `ChatClientStore` for thread-safe access

> **Important**: Each `ChatClient` has its own independent configuration. The mutate pattern creates new API instances
> for each provider to prevent configuration sharing between different providers.

### Embedding Models

1. **Auto-Configuration**: `MultiProviderAutoConfiguration` creates embedding models from configured providers
2. **Model Creation**: For each model in `openai-compatible[].embedding-models`:
   - Creates provider-specific `OpenAiApi` via `mutate()` pattern
   - Creates `OpenAiEmbeddingModel` with model-specific dimensions
3. **Store Initialization**: All models are registered in `EmbeddingModelStore`

## Examples

### Use Case: Multi-Model Chat Comparison

```java

@RestController
public class ModelComparisonController {

    private final ChatClientStore chatClientStore;

    @PostMapping("/compare")
    public Map<String, String> compareModels(@RequestParam String prompt) {
        // Compare responses from different models
        List<String> modelIds = List.of(
                "google/gemini-2.5-flash",
                "anthropic/claude-3-haiku",
                "openai/gpt-4o-mini"
        );

        Map<String, String> results = new HashMap<>();
        for (String modelId : modelIds) {
            String response = chatClientStore.get(modelId)
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();
            results.put(modelId, response);
        }

        return results;
    }

    @GetMapping("/available-models")
    public List<String> getAvailableModels() {
        return chatClientStore.getModelIds();
    }
}
```

### Use Case: RAG with Multiple Embedding Models

```java

@Service
public class RAGService {

    private final EmbeddingModelStore embeddingModelStore;
    private final ChatClientStore chatClientStore;

    public String answerQuestion(String question) {
        // Example documents
        List<String> documents = List.of(
                "Spring AI is a framework for AI applications in Java",
                "Embeddings convert text to numerical vectors",
                "RAG combines retrieval with generation"
        );

        // Step 1: Create embeddings for documents
        EmbeddingModel embeddingModel = embeddingModelStore.get("qwen/qwen3-embedding-8b");
        List<float[]> docEmbeddings = embeddingModel.embed(documents);

        // Step 2: Embed the query
        float[] queryEmbedding = embeddingModel.embed(question);

        // Step 3: Find most relevant document (simplified)
        String relevantDoc = findMostSimilar(queryEmbedding, docEmbeddings, documents);

        // Step 4: Generate answer using chat model
        ChatClient chatClient = chatClientStore.get("anthropic/claude-3.5-sonnet");
        return chatClient.prompt()
                .user(String.format(
                        "Context: %s\n\nQuestion: %s\n\nAnswer based on the context:",
                        relevantDoc, question
                ))
                .call()
                .content();
    }

    private String findMostSimilar(float[] query, List<float[]> embeddings,
                                   List<String> documents) {
        int bestIdx = 0;
        double bestScore = -1.0;

        for (int i = 0; i < embeddings.size(); i++) {
            double score = cosineSimilarity(query, embeddings.get(i));
            if (score > bestScore) {
                bestScore = score;
                bestIdx = i;
            }
        }

        return documents.get(bestIdx);
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dotProduct = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
```

## Complete Working Example

Here's a full working example based on the integration tests

### application.yml

```yaml
spring:
  ai:
    retry:
      on-http-codes: [ 429 ]
      on-client-errors: true
      backoff:
        initial-interval: 2000ms
        max-interval: 30000ms
        multiplier: 2
    openai:
      base-url: https://openrouter.ai/api
      api-key: ${OPENROUTER_API_KEY}
      chat:
        options:
          model: google/gemini-2.5-flash
          temperature: 0.0
      embedding:
        base-url: https://openrouter.ai/api
        api-key: ${OPENROUTER_API_KEY}
        options:
          model: openai/text-embedding-3-small
          dimensions: 1024

    ragas:
      # Multi-provider configuration
      providers:
        auto-detect-beans: false
        openai-compatible:
          - name: openrouter
            base-url: https://openrouter.ai/api
            api-key: ${OPENROUTER_API_KEY}
            chat-models:
              # Premium tier
              - { id: anthropic/claude-3.5-sonnet }
              - { id: openai/gpt-4o }
              - { id: google/gemini-1.5-pro }
              # Efficient tier
              - { id: google/gemini-2.5-flash }
              - { id: anthropic/claude-3-haiku }
              - { id: openai/gpt-4o-mini }
              - { id: deepseek/deepseek-chat }
              # Open-source
              - { id: meta-llama/llama-3.3-70b-instruct }
              - { id: qwen/qwen-2.5-72b-instruct }
            embedding-models:
              - { id: openai/text-embedding-3-large, dimensions: 3072 }
              - { id: qwen/qwen3-embedding-8b, dimensions: 1024 }
              - { id: google/gemini-embedding-001, dimensions: 768 }
              - { id: baai/bge-m3, dimensions: 1024 }
        default-provider:
          enabled: false
        default-options:
          temperature: 0.0
          max-tokens: 1000
          top-p: 1.0
        embedding-default-options:
          dimensions: 1024
```

### Spring Boot Application

```java

@SpringBootApplication
public class MultiModelApplication {

    public static void main(String[] args) {
        SpringApplication.run(MultiModelApplication.class, args);
    }
}
```

### Service Example

```java

@Service
@Slf4j
public class AIService {

    private final ChatClientStore chatClientStore;
    private final EmbeddingModelStore embeddingModelStore;

    public AIService(ChatClientStore chatClientStore,
                     EmbeddingModelStore embeddingModelStore) {
        this.chatClientStore = chatClientStore;
        this.embeddingModelStore = embeddingModelStore;
    }

    @PostConstruct
    public void init() {
        log.info("Available chat models: {}", chatClientStore.getModelIds());
        log.info("Available embedding models: {}", embeddingModelStore.getModelIds());
    }

    public String chat(String message) {
        return chatClientStore.getDefault()
                .prompt()
                .user(message)
                .call()
                .content();
    }

    public String chatWithModel(String message, String modelId) {
        return chatClientStore.get(modelId)
                .prompt()
                .user(message)
                .call()
                .content();
    }

    public float[] embed(String text) {
        return embeddingModelStore.getDefault().embed(text);
    }

    public float[] embedWithModel(String text, String modelId) {
        return embeddingModelStore.get(modelId).embed(text);
    }
}
```

