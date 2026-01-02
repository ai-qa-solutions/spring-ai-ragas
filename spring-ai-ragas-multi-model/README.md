# Spring AI Multi-Model Support

<!-- TOC -->
* [Spring AI Multi-Model Support](#spring-ai-multi-model-support)
  * [Features](#features)
  * [Installation](#installation)
  * [Quick Start](#quick-start)
    * [1. Configure Your Models](#1-configure-your-models)
    * [2. Use in Your Code](#2-use-in-your-code)
      * [Chat Models](#chat-models)
      * [Embedding Models](#embedding-models)
  * [Configuration Reference](#configuration-reference)
    * [Chat Models Configuration](#chat-models-configuration)
    * [Embedding Models Configuration](#embedding-models-configuration)
  * [Advanced Usage](#advanced-usage)
    * [Working with OpenRouter (Multiple AI Providers)](#working-with-openrouter-multiple-ai-providers)
    * [Custom Options Per Model](#custom-options-per-model)
    * [Error Handling](#error-handling)
  * [API Reference](#api-reference)
    * [ChatClientStore](#chatclientstore)
    * [EmbeddingModelStore](#embeddingmodelstore)
  * [How It Works](#how-it-works)
    * [Chat Models](#chat-models-1)
    * [Embedding Models](#embedding-models-1)
  * [Examples](#examples)
    * [Use Case: Multi-Model Chat Comparison](#use-case-multi-model-chat-comparison)
    * [Use Case: RAG with Multiple Embedding Models](#use-case-rag-with-multiple-embedding-models)
  * [Complete Working Example](#complete-working-example)
    * [application.yml](#applicationyml)
    * [Spring Boot Application](#spring-boot-application)
    * [Service Example](#service-example)

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
      base-url: https://openrouter.ai/api/v1
      api-key: ${OPENROUTER_API_KEY}
      chat:
        options:
          model: google/gemini-2.5-flash
          temperature: 0.0
      embedding:
        base-url: https://openrouter.ai/api/v1
        api-key: ${OPENROUTER_API_KEY}
        options:
          model: openai/text-embedding-3-small
          dimensions: 1024
          
    # Configure multiple chat models
    chat-models:
      default-options:
        temperature: 0.0
        max-tokens: 1000
        top-p: 1.0
      list:
        # OpenAI models via OpenRouter
        - id: openai/gpt-4o
        - id: openai/gpt-4o-mini
        # Anthropic models
        - id: anthropic/claude-3.5-sonnet
        - id: anthropic/claude-3-haiku
          options:
            temperature: 0.7
            max-tokens: 2000
        # Google models
        - id: google/gemini-2.5-flash
        - id: google/gemini-1.5-pro
            
    # Configure multiple embedding models
    embedding-models:
      default-options:
        dimensions: 1024
      list:
        # OpenAI embeddings
        - id: openai/text-embedding-3-small
        - id: openai/text-embedding-3-large
          options:
            dimensions: 3072
        # Qwen embeddings
        - id: qwen/qwen3-embedding-8b
        - id: qwen/qwen3-embedding-4b
        # Google embedding
        - id: google/gemini-embedding-001
          options:
            dimensions: 768
        # BAAI embeddings
        - id: baai/bge-m3
        - id: baai/bge-base-en-v1.5
          options:
            dimensions: 768
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

### Chat Models Configuration

|                      Property                       |  Type   | Default |                                   Description                                    |
|-----------------------------------------------------|---------|---------|----------------------------------------------------------------------------------|
| `spring.ai.chat-models.default-options.temperature` | Double  | 0.0     | Default temperature for all models (0.0 = deterministic, higher = more creative) |
| `spring.ai.chat-models.default-options.max-tokens`  | Integer | 1000    | Default maximum tokens in response                                               |
| `spring.ai.chat-models.default-options.top-p`       | Double  | 1.0     | Default nucleus sampling parameter                                               |
| `spring.ai.chat-models.list[].id`                   | String  | -       | **Required**. Unique model identifier                                            |
| `spring.ai.chat-models.list[].options`              | Object  | -       | Optional. Model-specific options override defaults                               |

### Embedding Models Configuration

|                        Property                         |  Type   | Default |               Description                |
|---------------------------------------------------------|---------|---------|------------------------------------------|
| `spring.ai.embedding-models.default-options.dimensions` | Integer | 1024    | Default vector dimensions for all models |
| `spring.ai.embedding-models.list[].id`                  | String  | -       | **Required**. Unique model identifier    |
| `spring.ai.embedding-models.list[].options.dimensions`  | Integer | -       | Optional. Model-specific dimensions      |

## Advanced Usage

### Working with OpenRouter (Multiple AI Providers)

OpenRouter provides unified access to multiple AI providers. Complete configuration example

```yaml
spring:
  ai:
    retry:
      on-http-codes: [429]
      on-client-errors: true
      backoff:
        initial-interval: 2000ms
        max-interval: 30000ms
        multiplier: 2
    openai:
      base-url: https://openrouter.ai/api/v1
      api-key: ${OPENROUTER_API_KEY}
      chat:
        options:
          model: google/gemini-2.5-flash
          temperature: 0.0
      embedding:
        base-url: https://openrouter.ai/api/v1
        api-key: ${OPENROUTER_API_KEY}
        options:
          model: openai/text-embedding-3-small
          dimensions: 1024
      
    chat-models:
      default-options:
        temperature: 0.0
        max-tokens: 1000
        top-p: 1.0
      list:
        # Premium tier models
        - id: anthropic/claude-3.5-sonnet
        - id: openai/gpt-4o
        - id: google/gemini-1.5-pro
        
        # Efficient tier models
        - id: google/gemini-2.5-flash
        - id: anthropic/claude-3-haiku
        - id: openai/gpt-4o-mini
        - id: deepseek/deepseek-chat
        
        # Open-source models
        - id: meta-llama/llama-3.3-70b-instruct
        - id: qwen/qwen-2.5-72b-instruct
```

### Custom Options Per Model

```yaml
spring:
  ai:
    chat-models:
      default-options:
        temperature: 0.0
        max-tokens: 1000
      list:
        # Use default options - for precise tasks
        - id: openai/gpt-4o-mini
        
        # Override for more creative responses
        - id: anthropic/claude-3.5-sonnet
          options:
            temperature: 0.7
            max-tokens: 4000
            top-p: 0.95
            
        # Creative writing model - high temperature
        - id: google/gemini-1.5-pro
          options:
            temperature: 0.9
            max-tokens: 8000
            
        # Code generation - deterministic
        - id: deepseek/deepseek-chat
          options:
            temperature: 0.0
            max-tokens: 4000
```

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

## How It Works

### Chat Models

1. **Auto-Configuration**: `ChatClientAutoConfiguration` detects Spring AI's `ChatClient.Builder` and your YAML
   configuration
2. **Model Creation**: For each model in `spring.ai.chat-models.list`, a separate `ChatClient` is created with:
   - Model-specific ID
   - Individual or default options (temperature, max tokens, top-p)
   - SimpleLoggerAdvisor for logging
3. **Store Initialization**: All clients are registered in `ChatClientStore` for thread-safe access

### Embedding Models

1. **Auto-Configuration**: `EmbeddingModelAutoConfiguration` detects the default `EmbeddingModel` bean
2. **Factory Pattern**: `EmbeddingModelFactory` creates delegating wrappers that:
   - Use a single API connection
   - Override model ID and dimensions per request
   - Merge options at runtime
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
      on-http-codes: [429]
      on-client-errors: true
      backoff:
        initial-interval: 2000ms
        max-interval: 30000ms
        multiplier: 2
    openai:
      base-url: https://openrouter.ai/api/v1
      api-key: ${OPENROUTER_API_KEY}
      chat:
        options:
          model: google/gemini-2.5-flash
          temperature: 0.0
      embedding:
        base-url: https://openrouter.ai/api/v1
        api-key: ${OPENROUTER_API_KEY}
        options:
          model: openai/text-embedding-3-small
          dimensions: 1024
    
    # Configure multiple chat models
    chat-models:
      default-options:
        temperature: 0.0
        max-tokens: 1000
        top-p: 1.0
      list:
        # Base tier
        - id: anthropic/claude-3.5-sonnet
        - id: openai/gpt-4o
        - id: google/gemini-1.5-pro
        # Efficient tier
        - id: google/gemini-2.5-flash
        - id: anthropic/claude-3-haiku
        - id: openai/gpt-4o-mini
        - id: deepseek/deepseek-chat
        # Open-source
        - id: meta-llama/llama-3.3-70b-instruct
        - id: qwen/qwen-2.5-72b-instruct
    
    # Configure multiple embedding models
    embedding-models:
      default-options:
        dimensions: 1024
      list:
        # OpenAI embeddings
        - id: openai/text-embedding-3-large
          options:
            dimensions: 3072
        # Qwen embeddings
        - id: qwen/qwen3-embedding-8b
        - id: qwen/qwen3-embedding-4b
        # Google embedding
        - id: google/gemini-embedding-001
          options:
            dimensions: 768
        # BAAI embeddings
        - id: baai/bge-m3
        - id: baai/bge-base-en-v1.5
          options:
            dimensions: 768
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

