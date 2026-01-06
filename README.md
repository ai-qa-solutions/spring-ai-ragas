# Spring AI RAGAS - LLM Agent Evaluation for Java

[![en](https://img.shields.io/badge/lang-en-blue.svg)](https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/README.md)
[![ru](https://img.shields.io/badge/lang-ru-blue.svg)](https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/README.ru.md)

A Java library for evaluating and testing AI agents based on Large Language Models (LLM), inspired by the Python
[RAGAS](https://docs.ragas.io/en/stable/concepts/metrics/available_metrics/general_purpose/)
framework. Built on Spring Boot and Spring AI SDK for easy integration with the Java ecosystem.

## Why This Library?

Modern AI agents require objective and automated quality assessment. Manual testing is time-consuming and highly
subjective. Spring AI RAGAS solves these problems:

- **Objective Evaluation**: LLM-based metrics for automated testing
- **Spring-native**: Native integration with Spring Boot ecosystem
- **Asynchronous**: CompletableFuture for parallel evaluations
- **Multi-Model**: Run evaluations across multiple LLMs with aggregation strategies
- **Multilingual**: Support for Russian and English languages
- **Extensible**: Easy to create custom metrics

## Supported Metrics

### General Purpose Metrics

|                                      Metric                                      |                  Description                  |
|----------------------------------------------------------------------------------|-----------------------------------------------|
| [AspectCritic](docs/en/general_purpose_metrics_en.md#aspectcritic)               | Binary evaluation based on predefined aspects |
| [SimpleCriteriaScore](docs/en/general_purpose_metrics_en.md#simplecriteriascore) | Quantitative evaluation on continuous scale   |
| [RubricsScore](docs/en/general_purpose_metrics_en.md#rubricsscore)               | Detailed evaluation based on explicit rubrics |

Full documentation: [General Purpose Metrics Guide](docs/en/general_purpose_metrics_en.md)

### Retrieval Metrics

|                                   Metric                                   |                 Description                 |
|----------------------------------------------------------------------------|---------------------------------------------|
| [ContextEntityRecall](docs/en/retrieval_metrics_en.md#contextentityrecall) | Entity coverage in retrieved contexts       |
| [ContextPrecision](docs/en/retrieval_metrics_en.md#contextprecision)       | Precision of retrieved context ranking      |
| [ContextRecall](docs/en/retrieval_metrics_en.md#contextrecall)             | Completeness of retrieved information       |
| [Faithfulness](docs/en/retrieval_metrics_en.md#faithfulness)               | Factual consistency with retrieved contexts |
| [NoiseSensitivity](docs/en/retrieval_metrics_en.md#noisesensitivity)       | Robustness to irrelevant contexts           |
| [ResponseRelevancy](docs/en/retrieval_metrics_en.md#responserelevancy)     | Response semantic relevance to user input   |

Full documentation: [Retrieval Metrics Guide](docs/en/retrieval_metrics_en.md)

## Quick Start

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
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
    <version>1.1.0-M2</version>
    <scope>test</scope>
</dependency>
```

#### Gradle

```groovy
testImplementation 'io.github.ai-qa-solutions:spring-ai-ragas-spring-boot-starter:1.0.0'
testImplementation 'org.springframework.ai:spring-ai-starter-model-openai:1.1.0-M2'
```

### Configuration

application.yaml

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
    # Multi-model evaluation configuration
    chat-models:
      default-options:
        temperature: 0.0
        max-tokens: 1000
      list:
        - { id: anthropic/claude-4.5-sonnet }
        - { id: google/gemini-2.5-flash }
        - { id: openai/gpt-4o-mini }
```

## Usage Example

```java
@SpringBootTest
class MetricsTest {

    @Autowired
    private AspectCriticMetric aspectCritic;

    @Autowired
    private SimpleCriteriaScoreMetric simpleCriteria;

    @Autowired
    private RubricsScoreMetric rubrics;

    @Test
    void evaluateResponse() {
        Sample sample = Sample.builder()
                .userInput("What is artificial intelligence?")
                .response("AI is a field of computer science that creates systems "
                        + "capable of performing tasks requiring human intelligence.")
                .build();

        // Binary evaluation (AspectCritic)
        var aspectConfig = AspectCriticMetric.AspectCriticConfig.builder()
                .definition("Does the response accurately explain AI?")
                .build();
        Double aspectScore = aspectCritic.singleTurnScore(aspectConfig, sample);
        // Result: 1.0 (yes) or 0.0 (no)

        // Continuous scale evaluation (SimpleCriteriaScore)
        var criteriaConfig = SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                .definition("Rate explanation quality from 1 to 5")
                .build();
        Double criteriaScore = simpleCriteria.singleTurnScore(criteriaConfig, sample);
        // Result: 0.0-1.0 (normalized score)

        // Rubric-based evaluation (RubricsScore)
        var rubricsConfig = RubricsScoreMetric.RubricsConfig.builder()
                .rubric("score1_description", "No relevant information")
                .rubric("score3_description", "Basic definition provided")
                .rubric("score5_description", "Comprehensive explanation with examples")
                .build();
        Double rubricsScore = rubrics.singleTurnScore(rubricsConfig, sample);
        // Result: 0.0-1.0 (normalized score)
    }
}
```

### Content Safety Filtering

```java
var config = AspectCriticMetric.AspectCriticConfig.builder()
        .definition("Does the response contain harmful information?")
        .strictness(5)
        .build();
Double score = aspectCritic.singleTurnScore(config, sample);
// score == 0.0: safe content, score == 1.0: harmful content
```

### RAG System Evaluation

```java
@Autowired
private FaithfulnessMetric faithfulness;

@Autowired
private ContextPrecisionMetric contextPrecision;

@Test
void evaluateRAG() {
    Sample sample = Sample.builder()
            .userInput("When was the first Super Bowl?")
            .response("The first Super Bowl was held on January 15, 1967.")
            .retrievedContexts(List.of(
                    "The first Super Bowl was held on January 15, 1967.",
                    "The game was played at the Los Angeles Memorial Coliseum."))
            .build();

    Double faithfulnessScore = faithfulness.singleTurnScore(sample);
    // Measures if response is grounded in retrieved contexts

    var precisionConfig = ContextPrecisionMetric.ContextPrecisionConfig.builder()
            .evaluationStrategy(ContextPrecisionMetric.EvaluationStrategy.RESPONSE_BASED)
            .build();
    Double precisionScore = contextPrecision.singleTurnScore(precisionConfig, sample);
    // Measures retrieval ranking quality
}
```

## Architecture

```
spring-ai-ragas/
├── spring-ai-ragas-metrics/              # Core metrics library
│   └── ai.qa.solutions/
│       ├── metric/                       # Base metric classes
│       │   ├── Metric                    # Metric interface
│       │   └── AbstractMultiModelMetric  # Base class for multi-model metrics
│       └── metrics/
│           ├── general/                  # General purpose metrics
│           │   ├── AspectCriticMetric
│           │   ├── SimpleCriteriaScoreMetric
│           │   └── RubricsScoreMetric
│           └── retrieval/                # RAG evaluation metrics
│               ├── ContextEntityRecallMetric
│               ├── ContextPrecisionMetric
│               ├── ContextRecallMetric
│               ├── FaithfulnessMetric
│               ├── NoiseSensitivityMetric
│               └── ResponseRelevancyMetric
│
├── spring-ai-ragas-multi-model/          # Multi-model execution
│   └── ai.qa.solutions/
│       ├── chatclient/                   # ChatClient factory
│       ├── embedding/                    # EmbeddingModel factory
│       ├── execution/                    # Multi-model execution engine
│       │   ├── MultiModelExecutor        # Parallel model execution
│       │   └── ScoreAggregator           # Score aggregation strategies
│       └── sample/                       # Sample DTO classes
│
├── spring-ai-ragas-spring-boot/          # Spring Boot autoconfiguration
│   └── config/                           # Auto-configuration classes
│
└── spring-ai-ragas-spring-boot-starter/  # Spring Boot starter
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [RAGAS](https://github.com/explodinggradients/ragas) - Original Python framework
- [Spring AI](https://spring.io/projects/spring-ai) - LLM integration foundation

