# Spring AI RAGAS - LLM Agent Evaluation for Java 🎯

[![en](https://img.shields.io/badge/lang-en-blue.svg)](https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/README.md)
[![ru](https://img.shields.io/badge/lang-ru-blue.svg)](https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/README.ru.md)

A Java library for evaluating and testing AI agents based on Large Language Models (LLM), inspired by the Python
[RAGAS](https://docs.ragas.io/en/stable/concepts/metrics/available_metrics/general_purpose/)
framework. Built on Spring Boot and Spring AI SDK for easy integration with the Java ecosystem.

## 🎯 Why This Library?

Modern AI agents require objective and automated quality assessment. Manual testing is time-consuming and highly
subjective. Spring AI RAGAS solves these problems:

- **🔍 Objective Evaluation**: LLM-based metrics for automated testing
- **🚀 Spring-native**: Native integration with Spring Boot ecosystem
- **⚡ Asynchronous**: CompletableFuture for parallel evaluations
- **🌍 Multilingual**: Support for Russian and English languages
- **🛠️ Extensible**: Easy to create custom metrics

## 🔄 Supported Metrics

### General Purpose Metrics

- **[AspectCritic](docs/en/general_purpose_metrics_en.md#aspectcritic)** - Binary evaluation based on predefined aspects
- **[SimpleCriteriaScore](docs/en/general_purpose_metrics_en.md#simplecriteriascore)** - Quantitative evaluation based on simple criteria
- **[RubricsScore](docs/en/general_purpose_metrics_en.md#rubricsscore)** - Detailed evaluation based on rubrics

> 📖 **Detailed Documentation**: [General Purpose Metrics Guide](docs/en/general_purpose_metrics_en.md)

### RAG-Specific Metrics - *Under Development*

- **Faithfulness** - Factual accuracy of responses
- **ContextRelevance** - Relevance of context
- **AnswerRelevance** - Relevance of answers
- **ContextRecall** - Completeness of retrieved context

## 🚀 Quick Start

### Prerequisites

- Java 17+
- Spring Boot 3.x
- Access to LLM (OpenAI, Azure OpenAI, Anthropic, etc. via Spring AI)

### Installation

#### Maven

```xml
<dependency>
    <groupId>io.github.ai-qa-solutions</groupId>
    <artifactId>spring-ai-ragas-spring-boot-starter</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
<!-- Add any required starters from spring-ai ecosystem -->
<dependency>
    <groupId>chat.giga</groupId>
    <artifactId>spring-ai-starter-model-gigachat</artifactId>
    <version>1.0.5</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
    <version>1.1.0-M2</version>
    <scope>test</scope>
</dependency>
```

#### Gradle

```groovy
implementation 'io.github.ai-qa-solutions:spring-ai-ragas-spring-boot-starter:1.0.0'
implementation 'chat.giga:spring-ai-starter-model-gigachat:1.0.5'
implementation 'org.springframework.ai:spring-ai-starter-model-openai:1.1.0-M2'
```

### Configuration

application.yaml

```yaml
spring:
  ai:
    retry: # Recommended to configure retries for large test volumes
      on-http-codes: [ 429 ]
      on-client-errors: true
      backoff:
        initial-interval: 2000ms
        max-interval: 30000ms
        multiplier: 2
    model: # Choose API starter for work
      chat: gigachat
    gigachat: # GigaChat API connection parameters
      auth:
        unsafe-ssl: true
        scope: gigachat_api_pers
        bearer:
          client-id: ${SPRING_AI_GIGACHAT_CLIENT_ID}
          client-secret: ${SPRING_AI_GIGACHAT_CLIENT_SECRET}
      chat:
        options:
          model: GigaChat-2-Max
    openai: # OpenRouter connection parameters
      base-url: https://openrouter.ai/api
      api-key: ${OPENROUTER_API_KEY}
      chat:
        options:
          model: qwen/qwen3-235b-a22b:free
```

## 📡 5-Minute Quick Start

### Basic Usage Example

```java
@SpringBootTest
class MetricsQuickStartTest {
    
    @Autowired
    private AspectCriticMetric aspectCritic;
    
    @Autowired 
    private SimpleCriteriaScoreMetric simpleCriteria;
    
    @Autowired
    private RubricsScoreMetric rubrics;
    
    @Test
    void quickEvaluationExample() {
        // Create test data
        Sample sample = Sample.builder()
            .userInput("What is artificial intelligence?")
            .response("AI is a field of computer science that creates systems " +
                    "capable of performing tasks requiring human intelligence.")
            .build();
        
        // 1. Binary safety check (AspectCritic)
        var safetyConfig = AspectCriticMetric.AspectCriticConfig.builder()
            .definition("Does the response contain accurate information?")
            .build();
        
        Double safetyScore = aspectCritic.singleTurnScore(safetyConfig, sample);
        // Result: 1.0 (accurate) or 0.0 (inaccurate)
        
        // 2. Quality assessment (SimpleCriteriaScore)  
        var qualityConfig = SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
            .definition("Rate explanation quality from 1 to 5")
            .minScore(1.0)
            .maxScore(5.0)
            .build();
        
        Double qualityScore = simpleCriteria.singleTurnScore(qualityConfig, sample);
        // Result: 1.0-5.0 (quality level)
        
        // 3. Detailed rubric evaluation (RubricsScore)
        var rubricsConfig = RubricsScoreMetric.RubricsConfig.builder()
            .rubric("score1_description", "No relevant information")
            .rubric("score3_description", "Basic definition provided")  
            .rubric("score5_description", "Comprehensive explanation with examples")
            .build();
        
        Double detailedScore = rubrics.singleTurnScore(rubricsConfig, sample);
        // Result: 1.0-5.0 (based on rubric criteria)
        
        System.out.println("Safety: " + safetyScore);    // 1.0
        System.out.println("Quality: " + qualityScore);  // 4.2
        System.out.println("Detailed: " + detailedScore);// 4.0
    }
    
    @Test
    void parallelEvaluationExample() {
        Sample sample = Sample.builder()
            .userInput("Explain photosynthesis")
            .response("Photosynthesis is how plants convert light into energy...")
            .build();
        
        // Run all metrics in parallel
        CompletableFuture<Double> safety = aspectCritic.singleTurnScoreAsync(safetyConfig, sample);
        CompletableFuture<Double> quality = simpleCriteria.singleTurnScoreAsync(qualityConfig, sample);
        CompletableFuture<Double> detailed = rubrics.singleTurnScoreAsync(rubricsConfig, sample);
        
        // Wait for all results
        CompletableFuture.allOf(safety, quality, detailed).join();
        
        System.out.println("Results: " + safety.join() + ", " + 
                          quality.join() + ", " + detailed.join());
    }
}
```

### Common Use Cases

**Content Safety Filtering:**

```java
var config = AspectCriticMetric.AspectCriticConfig.builder()
    .definition("Does the response contain harmful information?")
    .strictness(5) // Very strict
    .build();
Double score = aspectCritic.singleTurnScore(config, sample);
// Use score == 0.0 to allow content, == 1.0 to block
```

**Response Quality Ranking:**

```java
var config = SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
    .definition("Rate answer helpfulness from 1 to 10")
    .minScore(1.0).maxScore(10.0)
    .build();
Double score = simpleCriteria.singleTurnScore(config, sample);
// Use score for ranking: higher = better
```

**Detailed Assessment:**

```java
var config = RubricsScoreMetric.RubricsConfig.builder()
    .rubric("score1_description", "Incorrect or missing information")
    .rubric("score2_description", "Basic understanding, some gaps")
    .rubric("score3_description", "Good understanding, minor issues") 
    .rubric("score4_description", "Very good explanation")
    .rubric("score5_description", "Excellent, comprehensive answer")
    .build();
Double score = rubrics.singleTurnScore(config, sample);
// Provides detailed feedback based on rubric levels
```

## 🏗️ Architecture

### Core Components

```
spring-ai-ragas-core
└── ai.qa.solutions/
   ├── sample/           # DTO data samples (Sample, MultiTurnSample)
   ├── metric/           # Base metric interfaces  
   └── metrics/          # Evaluators and results
        ├── general/          # General metrics (AspectCritic, SimpleCriteria, Rubrics)
        └── retrieval/        # RAG-specific metrics

spring-ai-ragas-autoconfiguration
└── config/               # Spring Boot configuration

spring-ai-ragas-spring-boot-starter 
                          # Spring Boot starter
```

## 🗺️ Roadmap

### v1.0.0 ✅

- [x] AspectCriticMetric
- [x] SimpleCriteriaScore
- [x] RubricsScore
- [x] Spring Boot auto-configuration
- [ ] Asynchronous support

### v1.1.0 🔄

- [ ] RAG-specific metrics (Faithfulness, ContextRelevance)

## 🤝 Contributing

We welcome contributions from the community! See [CONTRIBUTING.md](CONTRIBUTING.md) for details.

### Developer Quick Start

```bash
git clone https://github.com/ai-qa-solutions/spring-ai-ragas.git
cd spring-ai-ragas
mvn clean install
```

### Running Tests

```bash
# Set environment variables
export SPRING_AI_GIGACHAT_CLIENT_ID=your_client_id
export SPRING_AI_GIGACHAT_CLIENT_SECRET=your_client_secret

# Or use OpenAI/OpenRouter
export OPENROUTER_API_KEY=your_api_key

# Run tests
mvn test
```

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [RAGAS](https://github.com/explodinggradients/ragas) - Idea and examples
- [Spring AI](https://spring.io/projects/spring-ai) - LLM integration foundation

