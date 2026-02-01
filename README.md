# Spring AI RAGAS - LLM Agent Evaluation for Java

[![Maven Central Version](https://img.shields.io/maven-central/v/io.github.ai-qa-solutions/spring-ai-ragas-spring-boot-starter?color=green)](https://central.sonatype.com/artifact/io.github.ai-qa-solutions/spring-ai-ragas-spring-boot-starter)
[![MIT License](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
[![Build Status](https://github.com/ai-qa-solutions/spring-ai-ragas/actions/workflows/maven-build.yml/badge.svg)](https://github.com/ai-qa-solutions/spring-ai-ragas/actions/workflows/maven-build.yml)

[![en](https://img.shields.io/badge/lang-en-blue.svg)](https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/README.md)
[![ru](https://img.shields.io/badge/lang-ru-blue.svg)](https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/README.ru.md)

A Java library for evaluating and testing AI agents based on Large Language Models (LLM), inspired by the
[Python RAGAS framework](https://docs.ragas.io/en/stable/).
Built on Spring Boot and [Spring AI](https://docs.spring.io/spring-ai/reference/index.html) SDK for easy integration with the Java ecosystem.

## Why This Library?

Modern AI agents require objective and automated quality assessment. Manual testing is time-consuming and highly
subjective. Spring AI RAGAS solves these problems:

- **Objective Evaluation**: LLM-based metrics for automated testing
- **Spring-native**: Native integration with Spring Boot ecosystem
- **Asynchronous**: CompletableFuture for parallel evaluations
- **Multi-Model**: Run evaluations across multiple LLMs with aggregation strategies
- **Multilingual**: Support for Russian and English languages
- **Extensible**: Easy to create custom metrics

## Use Cases

Comprehensive quality analysis of agent systems in two scenarios:

- **With References**: POC evaluation, automated tests, synthetic monitoring
- **Without References**: Sampling and analysis of live production traffic

## Version Compatibility

| Spring AI RAGAS | RAGAS (Python) | Spring Boot | Spring AI |
|-----------------|----------------|-------------|-----------|
| 0.3.0           | 0.3.x          | 3.5.x       | 1.1.x     |

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

### Agent Metrics

|                               Metric                               |               Description                |
|--------------------------------------------------------------------|------------------------------------------|
| [AgentGoalAccuracy](docs/en/agent_metrics_en.md#agentgoalaccuracy) | Whether agent achieved its intended goal |
| [ToolCallAccuracy](docs/en/agent_metrics_en.md#toolcallaccuracy)   | Correctness of tool/function calls       |
| [TopicAdherence](docs/en/agent_metrics_en.md#topicadherence)       | Staying on topic during conversation     |

Full documentation: [Agent Metrics Guide](docs/en/agent_metrics_en.md)

### Response Metrics

|                                 Metric                                  |                     Description                      |
|-------------------------------------------------------------------------|------------------------------------------------------|
| [AnswerCorrectness](docs/en/response_metrics_en.md#answercorrectness)   | Overall answer correctness                           |
| [FactualCorrectness](docs/en/response_metrics_en.md#factualcorrectness) | Factual accuracy of statements                       |
| [SemanticSimilarity](docs/en/response_metrics_en.md#semanticsimilarity) | Embedding-based similarity (requires EmbeddingModel) |

Full documentation: [Response Metrics Guide](docs/en/response_metrics_en.md)

### NVIDIA Metrics

|                                  Metric                                   |          Description          |
|---------------------------------------------------------------------------|-------------------------------|
| [AnswerAccuracy](docs/en/nvidia_metrics_en.md#answeraccuracy)             | NVIDIA-style answer accuracy  |
| [ContextRelevance](docs/en/nvidia_metrics_en.md#contextrelevance)         | Context relevance scoring     |
| [ResponseGroundedness](docs/en/nvidia_metrics_en.md#responsegroundedness) | Response grounding in context |

Full documentation: [NVIDIA Metrics Guide](docs/en/nvidia_metrics_en.md)

### NLP Metrics (Non-LLM)

These metrics compute text similarity directly without LLM calls:

|                             Metric                             |                       Description                       |
|----------------------------------------------------------------|---------------------------------------------------------|
| [BleuScore](docs/en/nlp_metrics_en.md#bleuscore)               | BLEU score for translation quality                      |
| [RougeScore](docs/en/nlp_metrics_en.md#rougescore)             | ROUGE score (ROUGE-1, ROUGE-2, ROUGE-L)                 |
| [ChrfScore](docs/en/nlp_metrics_en.md#chrfscore)               | Character n-gram F-score (chrF/chrF++)                  |
| [StringSimilarity](docs/en/nlp_metrics_en.md#stringsimilarity) | Edit distance metrics (Levenshtein, Jaro, Jaro-Winkler) |

Full documentation: [NLP Metrics Guide](docs/en/nlp_metrics_en.md)

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
    <version>0.3.0</version>
    <scope>test</scope>
</dependency>
<!-- Add any required starters from spring-ai ecosystem -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
    <version>1.1.2</version>
    <scope>test</scope>
</dependency>
```

#### Gradle

```groovy
testImplementation 'io.github.ai-qa-solutions:spring-ai-ragas-spring-boot-starter:0.3.0'
testImplementation 'org.springframework.ai:spring-ai-starter-model-openai:1.1.2'
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
    ragas:
      # Multi-model evaluation configuration
      providers:
        auto-detect-beans: false
        openai-compatible:
          - name: openrouter
            base-url: https://openrouter.ai/api
            api-key: ${OPENROUTER_API_KEY}
            chat-models:
              - { id: anthropic/claude-3.5-sonnet }
              - { id: google/gemini-2.5-flash }
              - { id: openai/gpt-4o-mini }
        default-provider:
          enabled: false
        default-options:
          temperature: 0.0
          max-tokens: 1000
```

## Usage Example

### Pipeline 1: With References (Testing/Monitoring)

Comprehensive evaluation for POC, automated tests, and synthetic monitoring.
Uses ground truth data to validate agent behavior (~8 LLM calls).

```java
import static org.assertj.core.api.Assertions.assertThat;

import ai.qa.solutions.metrics.agent.*;
import ai.qa.solutions.metrics.general.*;
import ai.qa.solutions.metrics.retrieval.*;
import ai.qa.solutions.sample.Sample;
import ai.qa.solutions.sample.message.*;
import java.util.List;
import java.util.Map;

@Slf4j
@SpringBootTest
@EnableAutoConfiguration
class AgentEvaluationWithReferencesTest {

    @Configuration
    static class TestConfig {}

    @Autowired private ToolCallAccuracyMetric toolCallAccuracy;
    @Autowired private AgentGoalAccuracyMetric goalAccuracy;
    @Autowired private TopicAdherenceMetric topicAdherence;
    @Autowired private AspectCriticMetric aspectCritic;
    @Autowired private RubricsScoreMetric rubricsScore;
    @Autowired private ContextRecallMetric contextRecall;

    @Test
    @DisplayName("Customer support: undelivered order → refund")
    void evaluateCustomerSupport() {
        // Customer support scenario: undelivered order → refund
        Sample sample = Sample.builder()
            .userInput("Order 12345 not delivered, waiting 2 weeks already!")
            .response("Done! Your refund of $55 has been initiated. " +
                "Funds will arrive in 3-5 business days.")
            .userInputMessages(List.of(
                new HumanMessage("Order 12345 not delivered, waiting 2 weeks already!"),
                new AIMessage("Hello! I apologize for the inconvenience. " +
                    "Let me check your order status right away.",
                    List.of(new ToolCall("get_order_status", Map.of("order_id", "12345")))),
                new ToolMessage("{\"status\": \"LOST\", \"shipped\": \"2024-01-10\"}"),
                new AIMessage("I found the issue - your package was lost in transit. " +
                    "I'm processing a full refund of $55 for you now.",
                    List.of(new ToolCall("process_refund", Map.of("order_id", "12345", "amount", 5500)))),
                new ToolMessage("{\"refund_id\": \"RF-789\", \"status\": \"INITIATED\"}"),
                new AIMessage("Done! Your refund of $55 has been initiated. " +
                    "Funds will arrive in 3-5 business days.", List.of())))
            // Ground truth
            .reference("Resolve customer issue with undelivered order via refund")
            .referenceToolCalls(List.of(
                new Sample.ToolCall("get_order_status", Map.of("order_id", "12345")),
                new Sample.ToolCall("process_refund", Map.of("order_id", "12345", "amount", 5500))))
            .referenceTopics(List.of("order status", "refund", "delivery"))
            // RAG contexts from telemetry
            .retrievedContexts(List.of(
                "Order 12345: shipped Jan 10, marked LOST on Jan 20. Order total: $55.",
                "Refund policy: full refund for lost orders within 30 days."))
            .build();

        // 1. Tool calls accuracy (0 LLM calls)
        ToolCallAccuracyMetric.ToolCallAccuracyConfig toolConfig =
            ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder().build();
        Double toolScore = toolCallAccuracy.multiTurnScore(toolConfig, sample);

        // 2. Goal achievement (1 LLM call)
        AgentGoalAccuracyMetric.AgentGoalAccuracyConfig goalConfig =
            AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                .mode(AgentGoalAccuracyMetric.Mode.WITH_REFERENCE).build();
        Double goalScore = goalAccuracy.multiTurnScore(goalConfig, sample);

        // 3. Safety guard rail (1 LLM call) - 1.0 = safe, 0.0 = unsafe
        AspectCriticMetric.AspectCriticConfig safetyConfig = AspectCriticMetric.AspectCriticConfig.builder()
            .definition("Response is safe, professional, and free from harmful content").build();
        Double safetyScore = aspectCritic.singleTurnScore(safetyConfig, sample);

        // 4. Response completeness rubric (1 LLM call)
        RubricsScoreMetric.RubricsConfig rubricsConfig = RubricsScoreMetric.RubricsConfig.builder()
            .rubric("score1_description", "No greeting, problem not addressed")
            .rubric("score3_description", "Greeted, diagnosed issue, offered solution")
            .rubric("score5_description", "Greeted, apologized, diagnosed, solved, confirmed, offered follow-up")
            .build();
        Double completenessScore = rubricsScore.singleTurnScore(rubricsConfig, sample);

        // 5. Topic adherence (2 LLM calls)
        TopicAdherenceMetric.TopicAdherenceConfig topicConfig =
            TopicAdherenceMetric.TopicAdherenceConfig.builder().build();
        Double topicScore = topicAdherence.multiTurnScore(topicConfig, sample);

        // 6. Helpfulness check (1 LLM call)
        AspectCriticMetric.AspectCriticConfig helpfulConfig = AspectCriticMetric.AspectCriticConfig.builder()
            .definition("Response directly addresses user's problem and provides a clear solution").build();
        Double helpfulScore = aspectCritic.singleTurnScore(helpfulConfig, sample);

        // 7. Context recall (1 LLM call)
        ContextRecallMetric.ContextRecallConfig recallConfig =
            ContextRecallMetric.ContextRecallConfig.builder().build();
        Double recallScore = contextRecall.singleTurnScore(recallConfig, sample);

        // Assertions for CI/CD - RAGAS principle: 0 = bad, 1 = good
        assertThat(toolScore).as("Tool calls accuracy").isGreaterThanOrEqualTo(0.9);
        assertThat(goalScore).as("Goal achieved").isEqualTo(1.0);
        assertThat(safetyScore).as("Safe content").isEqualTo(1.0);
        assertThat(completenessScore).as("Response completeness").isGreaterThanOrEqualTo(0.6);
        assertThat(topicScore).as("Stayed on topic").isGreaterThanOrEqualTo(0.5);
        assertThat(helpfulScore).as("Helpful response").isGreaterThanOrEqualTo(0.7);
    }
}
```

### Pipeline 2: Without References (Production Sampling)

Evaluation for live traffic analysis without ground truth (~6 LLM calls + embeddings).

```java
import static org.assertj.core.api.Assertions.assertThat;

import ai.qa.solutions.metrics.agent.*;
import ai.qa.solutions.metrics.general.*;
import ai.qa.solutions.metrics.retrieval.*;
import ai.qa.solutions.sample.Sample;
import ai.qa.solutions.sample.message.*;
import java.util.List;
import java.util.Map;

@Slf4j
@SpringBootTest
@EnableAutoConfiguration
class ProductionSamplingTest {

    @Configuration
    static class TestConfig {}

    @Autowired private AspectCriticMetric aspectCritic;
    @Autowired private RubricsScoreMetric rubricsScore;
    @Autowired private ResponseRelevancyMetric responseRelevancy;
    @Autowired private AgentGoalAccuracyMetric goalAccuracy;

    @Test
    @DisplayName("Production sample: customer support conversation")
    void evaluateProductionSample() {
        // From production telemetry - no ground truth available
        Sample sample = Sample.builder()
            .userInput("Order 12345 not delivered, waiting 2 weeks!")
            .response("Refund processed! Funds will arrive in 3-5 days. Anything else?")
            .userInputMessages(List.of(
                new HumanMessage("Order 12345 not delivered, waiting 2 weeks!"),
                new AIMessage("Hello! I apologize for the delay. Checking your order now.",
                    List.of(new ToolCall("get_order_status", Map.of("order_id", "12345")))),
                new ToolMessage("{\"status\": \"LOST\"}"),
                new AIMessage("I see your package was lost. Processing a full refund for you.",
                    List.of(new ToolCall("process_refund", Map.of("order_id", "12345", "amount", 5500)))),
                new ToolMessage("{\"status\": \"REFUND_INITIATED\"}"),
                new AIMessage("Refund processed! Funds will arrive in 3-5 days. Anything else?", List.of())))
            // Only RAG contexts from traces - NO reference fields
            .retrievedContexts(List.of(
                "Order 12345: shipped Jan 10, status LOST. Order total: $55.",
                "Refund policy: full refund within 30 days."))
            .build();

        // 1. Safety guard rail (1 LLM call) - 1.0 = safe, 0.0 = unsafe
        AspectCriticMetric.AspectCriticConfig safetyConfig = AspectCriticMetric.AspectCriticConfig.builder()
            .definition("Response is safe, professional, and appropriate").build();
        Double safetyScore = aspectCritic.singleTurnScore(safetyConfig, sample);

        // 2. Response relevancy screening (1 LLM + embeddings)
        Double relevancy = responseRelevancy.singleTurnScore(sample);

        // 3. Goal achievement - infers goal from conversation (2 LLM calls)
        AgentGoalAccuracyMetric.AgentGoalAccuracyConfig goalConfig =
            AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                .mode(AgentGoalAccuracyMetric.Mode.WITHOUT_REFERENCE).build();
        Double goalScore = goalAccuracy.multiTurnScore(goalConfig, sample);

        // 4. Helpfulness check (1 LLM call)
        AspectCriticMetric.AspectCriticConfig helpfulConfig = AspectCriticMetric.AspectCriticConfig.builder()
            .definition("Response directly addresses user's problem and provides a clear solution").build();
        Double helpfulScore = aspectCritic.singleTurnScore(helpfulConfig, sample);

        // 5. Politeness/professionalism rubric (1 LLM call)
        RubricsScoreMetric.RubricsConfig toneConfig = RubricsScoreMetric.RubricsConfig.builder()
            .rubric("score1_description", "Rude, dismissive, unprofessional")
            .rubric("score3_description", "Neutral, functional")
            .rubric("score5_description", "Polite, empathetic, professional")
            .build();
        Double toneScore = rubricsScore.singleTurnScore(toneConfig, sample);

        // Production monitoring alerts - RAGAS principle: 0 = bad, 1 = good
        if (safetyScore < 1.0) log.error("ALERT: Potentially unsafe content!");
        if (relevancy < 0.5) log.warn("Low relevancy - off-topic response");
        if (goalScore < 0.7) log.warn("Goal not achieved");

        // Assertions - more lenient for production sampling
        assertThat(safetyScore).as("Safe content").isEqualTo(1.0);
        assertThat(relevancy).as("Response relevancy").isGreaterThanOrEqualTo(0.2);
        assertThat(goalScore).as("Goal achieved").isGreaterThanOrEqualTo(0.5);
        assertThat(helpfulScore).as("Helpful response").isGreaterThanOrEqualTo(0.7);
        assertThat(toneScore).as("Professional tone").isGreaterThanOrEqualTo(0.5);
    }
}
```

## Allure Reports (Optional)

For rich HTML reports with score explanations and execution timelines, add the Allure integration module.
Requires pre-configured AspectJ and Allure in your project.
See [pom.xml](pom.xml) for a complete setup example.

```xml
<dependency>
    <groupId>io.github.ai-qa-solutions</groupId>
    <artifactId>spring-ai-ragas-allure</artifactId>
    <version>0.3.0</version>
    <scope>test</scope>
</dependency>
```

Report examples:

**General Purpose:**
[AspectCritic](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/AspectCriticMetric_en.html) |
[SimpleCriteriaScore](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/SimpleCriteriaScoreMetric_en.html) |
[RubricsScore](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/RubricsScoreMetric_en.html)

**Retrieval:**
[Faithfulness](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/FaithfulnessMetric_en.html) |
[ContextPrecision](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/ContextPrecisionMetric_en.html) |
[ContextRecall](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/ContextRecallMetric_en.html) |
[ContextEntityRecall](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/ContextEntityRecallMetric_en.html) |
[NoiseSensitivity](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/NoiseSensitivityMetric_en.html) |
[ResponseRelevancy](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/ResponseRelevancyMetric_en.html)

**Agent:**
[AgentGoalAccuracy](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/AgentGoalAccuracyMetric_en.html) |
[ToolCallAccuracy](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/ToolCallAccuracyMetric_en.html) |
[TopicAdherence](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/TopicAdherenceMetric_en.html)

**Response:**
[AnswerCorrectness](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/AnswerCorrectnessMetric_en.html) |
[FactualCorrectness](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/FactualCorrectnessMetric_en.html) |
[SemanticSimilarity](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/SemanticSimilarityMetric_en.html)

**NVIDIA:**
[AnswerAccuracy](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/AnswerAccuracyMetric_en.html) |
[ContextRelevance](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/ContextRelevanceMetric_en.html) |
[ResponseGroundedness](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/ResponseGroundednessMetric_en.html)

**NLP:**
[BleuScore](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/BleuScoreMetric_en.html) |
[RougeScore](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/RougeScoreMetric_en.html) |
[ChrfScore](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/ChrfScoreMetric_en.html) |
[StringSimilarity](https://htmlpreview.github.io/?https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/docs/attachments/StringSimilarityMetric_en.html)

Full documentation: [Allure Integration Guide](spring-ai-ragas-allure/README.md)

## Architecture

```
spring-ai-ragas/
├── spring-ai-ragas-metrics/              # Core metrics library (20+ metrics)
│   └── ai.qa.solutions/
│       ├── metric/                       # Base metric classes
│       │   ├── Metric                    # Metric interface
│       │   └── AbstractMultiModelMetric  # Base class for multi-model metrics
│       └── metrics/
│           ├── general/                  # General purpose metrics
│           ├── retrieval/                # RAG evaluation metrics
│           ├── agent/                    # Agent evaluation metrics
│           ├── response/                 # Response quality metrics
│           ├── nvidia/                   # NVIDIA-style metrics
│           └── nlp/                      # Non-LLM text similarity metrics
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
├── spring-ai-ragas-allure/               # Allure reporting integration
│   └── ai.qa.solutions.allure/
│       ├── listener/                     # AllureMetricExecutionListener
│       ├── nlp/                          # AllureNlpMetricHelper
│       ├── explanation/                  # Score explanation classes
│       ├── methodology/                  # Metric methodology docs (en/ru)
│       └── template/                     # Freemarker report templates
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

