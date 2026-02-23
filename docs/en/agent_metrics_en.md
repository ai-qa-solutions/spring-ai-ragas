# Agent Metrics

Agent metrics evaluate the quality of AI agents in multi-turn conversations, including goal achievement, tool usage
accuracy, and topic adherence. These metrics are designed for evaluating agentic AI systems that can use tools
and maintain conversations.

## Configuration

### application.yaml

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
              - { id: deepseek/deepseek-v3.2 }
        default-provider:
          enabled: false
        default-options:
          temperature: 0.0
          max-tokens: 1000
  threads:
    virtual:
      enabled: true
```

---

## AgentGoalAccuracy

AgentGoalAccuracy evaluates whether an AI agent successfully achieved its intended goal during a conversation.
It supports two evaluation modes: with and without an explicit reference goal.

### How It Works

The metric supports two modes:

1. **WITH_REFERENCE**: Compares the conversation outcome with a provided reference goal
2. **WITHOUT_REFERENCE**: Infers the goal from the conversation and evaluates if it was achieved

```java
// From AgentGoalAccuracyMetric.java - evaluation flow
class Example {
    // Mode 1: Compare with explicit reference
    void withReference(String conversation, String reference) {
        // LLM compares actual outcome with expected outcome
        // Returns: goalAchieved (true/false) + reasoning
    }

    // Mode 2: Infer goal and evaluate
    void withoutReference(String conversation) {
        // Step 1: LLM infers user's goal from conversation
        // Step 2: LLM evaluates if the goal was achieved
        // Returns: goalAchieved (true/false) + reasoning
    }
}
```

### Example

```java
package ai.qa.solutions.metrics.agent.en;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.agent.AgentGoalAccuracyMetric;
import ai.qa.solutions.sample.Sample;
import ai.qa.solutions.sample.message.*;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

@Slf4j
@EnableAutoConfiguration
@SpringBootTest(classes = AgentGoalAccuracyTest.TestConfiguration.class)
class AgentGoalAccuracyTest {

    @Configuration
    public static class TestConfiguration {
    }

    @Autowired
    private AgentGoalAccuracyMetric agentGoalAccuracyMetric;

    @Test
    @DisplayName("AgentGoalAccuracy: Goal achieved with reference")
    void testGoalAchievedWithReference() {
        Sample sample = Sample.builder()
                .userInputMessages(List.of(
                        new HumanMessage("I need to book a flight to Paris for next Monday"),
                        new AIMessage("I'll help you book a flight to Paris.", List.of(
                                new ToolCall("search_flights", Map.of("destination", "Paris", "date", "next Monday"))
                        )),
                        new ToolMessage("Found: Air France at 10:00 AM for $450"),
                        new AIMessage("I found several flights. The best option is Air France at 10:00 AM for $450. Would you like me to book this?"),
                        new HumanMessage("Yes, please book that one"),
                        new AIMessage("Done! I've booked your Air France flight to Paris for Monday at 10:00 AM. Your confirmation number is AF12345.")
                ))
                .reference("Successfully book a flight to Paris for the user")
                .build();

        AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                        .mode(AgentGoalAccuracyMetric.Mode.WITH_REFERENCE)
                        .build();

        Double score = agentGoalAccuracyMetric.multiTurnScore(config, sample);

        log.info("Goal Accuracy Score: {}", score);
        assertTrue(score >= 0.9, "Expected high score for achieved goal");
    }

    @Test
    @DisplayName("AgentGoalAccuracy: Goal inferred without reference")
    void testGoalInferredWithoutReference() {
        Sample sample = Sample.builder()
                .userInputMessages(List.of(
                        new HumanMessage("What's the weather like in Tokyo?"),
                        new AIMessage("Let me check the weather in Tokyo for you.", List.of(
                                new ToolCall("get_weather", Map.of("city", "Tokyo"))
                        )),
                        new ToolMessage("Tokyo: 22°C, partly cloudy"),
                        new AIMessage("The current weather in Tokyo is 22°C with partly cloudy skies.")
                ))
                .build();

        AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                        .mode(AgentGoalAccuracyMetric.Mode.WITHOUT_REFERENCE)
                        .build();

        Double score = agentGoalAccuracyMetric.multiTurnScore(config, sample);

        log.info("Goal Accuracy Score: {}", score);
        assertTrue(score >= 0.8, "Expected high score when goal is inferred and achieved");
    }
}
```

### Configuration

| Parameter  |     Type     | Required |    Default     |               Description                |
|------------|--------------|----------|----------------|------------------------------------------|
| `mode`     | Mode         | No       | WITH_REFERENCE | WITH_REFERENCE or WITHOUT_REFERENCE      |
| `models`   | List<String> | No       | all            | Specific model IDs to use for evaluation |
| `language` | String       | No       | `"en"`         | Language for explanations (`"en"`, `"ru"`) |

### When to Use

- Evaluating conversational AI agents
- Measuring task completion rate
- Assessing goal-oriented dialogue systems
- Quality assurance for customer service bots

---

## ToolCallAccuracy

ToolCallAccuracy evaluates the accuracy of an agent's tool calls by comparing actual tool invocations against expected
reference calls. This is a non-LLM metric that computes precision, recall, and F1 score algorithmically.

### How It Works

The metric computes precision, recall, and F1 score:

1. **Alignment**: Matches actual tool calls with reference tool calls
2. **Precision**: Correct calls / Total actual calls
3. **Recall**: Correct calls / Total reference calls
4. **F1 Score**: Harmonic mean of precision and recall

```java
// From ToolCallAccuracyMetric.java - score computation
class Example {
    void computeScore() {
        int truePositives = countMatchedCalls();
        int falsePositives = actualCalls.size() - truePositives;
        int falseNegatives = referenceCalls.size() - truePositives;

        double precision = (double) truePositives / actualCalls.size();
        double recall = (double) truePositives / referenceCalls.size();
        double f1 = 2 * precision * recall / (precision + recall);
    }
}
```

**Matching Modes:**

- **STRICT**: Exact matching of tool names and all arguments
- **FLEXIBLE**: Allows partial argument matching based on threshold

### Example

```java
package ai.qa.solutions.metrics.agent.en;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.agent.ToolCallAccuracyMetric;
import ai.qa.solutions.sample.Sample;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

@Slf4j
@EnableAutoConfiguration
@SpringBootTest(classes = ToolCallAccuracyTest.TestConfiguration.class)
class ToolCallAccuracyTest {

    @Configuration
    public static class TestConfiguration {
    }

    @Autowired
    private ToolCallAccuracyMetric toolCallAccuracyMetric;

    @Test
    @DisplayName("ToolCallAccuracy: Perfect tool usage")
    void testPerfectToolUsage() {
        Sample sample = Sample.builder()
                .toolCalls(List.of(
                        new Sample.ToolCall("search_flights",
                                Map.of("destination", "Paris", "date", "2024-01-15")),
                        new Sample.ToolCall("book_flight",
                                Map.of("flight_id", "AF123", "passenger", "John Doe"))
                ))
                .referenceToolCalls(List.of(
                        new Sample.ToolCall("search_flights",
                                Map.of("destination", "Paris", "date", "2024-01-15")),
                        new Sample.ToolCall("book_flight",
                                Map.of("flight_id", "AF123", "passenger", "John Doe"))
                ))
                .build();

        ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                        .mode(ToolCallAccuracyMetric.Mode.STRICT)
                        .build();

        Double score = toolCallAccuracyMetric.singleTurnScore(config, sample);

        log.info("Tool Call Accuracy Score: {}", score);
        assertTrue(score >= 0.99, "Expected perfect score for exact match");
    }

    @Test
    @DisplayName("ToolCallAccuracy: Flexible mode with partial match")
    void testFlexibleMode() {
        Sample sample = Sample.builder()
                .toolCalls(List.of(
                        new Sample.ToolCall("get_weather",
                                Map.of("city", "Tokyo", "units", "celsius"))
                ))
                .referenceToolCalls(List.of(
                        new Sample.ToolCall("get_weather",
                                Map.of("city", "Tokyo", "units", "fahrenheit"))
                ))
                .build();

        ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                        .mode(ToolCallAccuracyMetric.Mode.FLEXIBLE)
                        .argumentMatchThreshold(0.5)
                        .build();

        Double score = toolCallAccuracyMetric.singleTurnScore(config, sample);

        log.info("Tool Call Accuracy Score: {}", score);
        assertTrue(score >= 0.5, "Expected partial score for flexible matching");
    }
}
```

### Configuration

|        Parameter         |     Type     | Required | Default |                   Description                    |
|--------------------------|--------------|----------|---------|--------------------------------------------------|
| `mode`                   | Mode         | No       | STRICT  | STRICT or FLEXIBLE matching mode                 |
| `argumentMatchThreshold` | Double       | No       | 0.8     | Threshold for argument matching in FLEXIBLE mode |
| `models`                 | List<String> | No       | all     | Model IDs (not used - this is a non-LLM metric)  |
| `language`               | String       | No       | `"en"`  | Language for explanations (`"en"`, `"ru"`)       |

### When to Use

- Evaluating function-calling AI agents
- Testing tool selection accuracy
- Validating argument passing correctness
- Benchmarking agent capabilities

---

## TopicAdherence

TopicAdherence evaluates whether conversation topics stay within the expected reference topics. It extracts topics from
the conversation and classifies them against allowed topics.

### How It Works

1. **Topic Extraction**: LLM extracts distinct topics discussed in the conversation
2. **Topic Classification**: Each extracted topic is classified as on-topic or off-topic
3. **Score Computation**: Computes precision, recall, or F1 based on configuration

```java
// From TopicAdherenceMetric.java - score computation
class Example {
    double computeScore(List<TopicClassification> classifications, List<String> referenceTopics, Mode mode) {
        // Precision: what fraction of extracted topics are on topic
        double precision = (double) onTopicCount / classifications.size();

        // Recall: what fraction of reference topics are covered
        double recall = (double) coveredReferenceCount / referenceTopics.size();

        return switch (mode) {
            case PRECISION -> precision;
            case RECALL -> recall;
            case F1 -> 2 * precision * recall / (precision + recall);
        };
    }
}
```

### Example

```java
package ai.qa.solutions.metrics.agent.en;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.agent.TopicAdherenceMetric;
import ai.qa.solutions.sample.Sample;
import ai.qa.solutions.sample.message.*;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

@Slf4j
@EnableAutoConfiguration
@SpringBootTest(classes = TopicAdherenceTest.TestConfiguration.class)
class TopicAdherenceTest {

    @Configuration
    public static class TestConfiguration {
    }

    @Autowired
    private TopicAdherenceMetric topicAdherenceMetric;

    @Test
    @DisplayName("TopicAdherence: Conversation stays on topic")
    void testOnTopicConversation() {
        Sample sample = Sample.builder()
                .userInputMessages(List.of(
                        new HumanMessage("I want to learn about machine learning"),
                        new AIMessage("Machine learning is a subset of AI that enables systems to learn from data."),
                        new HumanMessage("What are the main types?"),
                        new AIMessage("The main types are supervised learning, unsupervised learning, and reinforcement learning.")
                ))
                .referenceTopics(List.of(
                        "machine learning",
                        "artificial intelligence",
                        "supervised learning",
                        "unsupervised learning"
                ))
                .build();

        TopicAdherenceMetric.TopicAdherenceConfig config =
                TopicAdherenceMetric.TopicAdherenceConfig.builder()
                        .mode(TopicAdherenceMetric.Mode.F1)
                        .build();

        Double score = topicAdherenceMetric.multiTurnScore(config, sample);

        log.info("Topic Adherence Score: {}", score);
        assertTrue(score >= 0.7, "Expected high score for on-topic conversation");
    }

    @Test
    @DisplayName("TopicAdherence: Conversation goes off-topic")
    void testOffTopicConversation() {
        Sample sample = Sample.builder()
                .userInputMessages(List.of(
                        new HumanMessage("Tell me about Python programming"),
                        new AIMessage("Python is a programming language. By the way, did you know about the latest football match?"),
                        new HumanMessage("What about football?"),
                        new AIMessage("The World Cup final was amazing! The score was 3-2.")
                ))
                .referenceTopics(List.of(
                        "Python programming",
                        "coding",
                        "software development"
                ))
                .build();

        TopicAdherenceMetric.TopicAdherenceConfig config =
                TopicAdherenceMetric.TopicAdherenceConfig.builder()
                        .mode(TopicAdherenceMetric.Mode.PRECISION)
                        .build();

        Double score = topicAdherenceMetric.multiTurnScore(config, sample);

        log.info("Topic Adherence Score: {}", score);
        assertTrue(score <= 0.6, "Expected lower score when conversation goes off-topic");
    }
}
```

### Configuration

| Parameter  |     Type     | Required | Default |              Description              |
|------------|--------------|----------|---------|---------------------------------------|
| `mode`     | Mode         | No       | F1      | F1, PRECISION, or RECALL scoring mode |
| `models`   | List<String> | No       | all     | Specific model IDs for evaluation     |
| `language` | String       | No       | `"en"`  | Language for explanations (`"en"`, `"ru"`) |

**Scoring Modes:**

- **F1**: Harmonic mean of precision and recall (balanced)
- **PRECISION**: Focus on avoiding off-topic discussions
- **RECALL**: Focus on covering all reference topics

### When to Use

- Customer service bots (ensuring relevant responses)
- Educational chatbots (staying on curriculum)
- Moderated conversations (enforcing topic guidelines)
- Content moderation systems

---

## Choosing the Right Metric

|          Use Case           |      Metric       |                     Why                      |
|-----------------------------|-------------------|----------------------------------------------|
| Task completion evaluation  | AgentGoalAccuracy | Measures if agent achieved user's goal       |
| Function calling validation | ToolCallAccuracy  | Verifies correct tool usage and parameters   |
| Conversation relevance      | TopicAdherence    | Ensures conversation stays on allowed topics |
| End-to-end agent testing    | All three         | Comprehensive agent quality assessment       |

---

## Sample Schema

Agent metrics use the `Sample` class with typed message classes:

### Message Types

|      Type      |      Description      |         Fields         |
|----------------|-----------------------|------------------------|
| `HumanMessage` | User message          | `content`              |
| `AIMessage`    | Assistant response    | `content`, `toolCalls` |
| `ToolMessage`  | Tool execution result | `content`              |
| `ToolCall`     | Tool invocation       | `name`, `arguments`    |

### Example

```java
import ai.qa.solutions.sample.message.*;

Sample sample = Sample.builder()
        // Multi-turn conversation with typed messages
        .userInputMessages(List.of(
                new HumanMessage("Book a flight to NYC"),
                new AIMessage("Searching flights...", List.of(
                        new ToolCall("search_flights", Map.of("destination", "NYC"))
                )),
                new ToolMessage("Found 5 flights"),
                new AIMessage("I found 5 options. Flight UA123 departs at 9am.")
        ))
        // For AgentGoalAccuracy (WITH_REFERENCE mode)
        .reference("Flight booked to NYC")
        // For TopicAdherence
        .referenceTopics(List.of("flight booking", "travel"))
        // For ToolCallAccuracy
        .toolCalls(List.of(
                new Sample.ToolCall("search_flights", Map.of("destination", "NYC"))
        ))
        .referenceToolCalls(List.of(
                new Sample.ToolCall("search_flights", Map.of("destination", "NYC"))
        ))
        .build();

// Using multi-turn API
Double score = agentGoalAccuracy.multiTurnScore(config, sample);
```

### Sample Fields

|        Field         |       Type        |            Required By             |
|----------------------|-------------------|------------------------------------|
| `userInputMessages`  | List<BaseMessage> | AgentGoalAccuracy, TopicAdherence  |
| `reference`          | String            | AgentGoalAccuracy (WITH_REFERENCE) |
| `referenceTopics`    | List<String>      | TopicAdherence                     |
| `toolCalls`          | List<ToolCall>    | ToolCallAccuracy                   |
| `referenceToolCalls` | List<ToolCall>    | ToolCallAccuracy                   |

---

## Rich Evaluation API

All agent metrics support `multiTurnEvaluate()` returning `EvaluationResult` with score, explanation, per-model details, and metadata:

```java
import ai.qa.solutions.metric.EvaluationResult;

// Instead of Double score = metric.multiTurnScore(config, sample);
EvaluationResult result = agentGoalAccuracyMetric.multiTurnEvaluate(config, sample);

log.info("Score: {}", result.getScore());
log.info("Explanation: {}", result.getExplanation().getSimpleDescription());
log.info("Model scores: {}", result.getModelScores());
log.info("Duration: {}ms", result.getTotalDuration().toMillis());

// Async variant
CompletableFuture<EvaluationResult> future =
        agentGoalAccuracyMetric.multiTurnEvaluateAsync(config, sample);

// Russian language explanations
AgentGoalAccuracyMetric.AgentGoalAccuracyConfig ruConfig =
        AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                .mode(AgentGoalAccuracyMetric.Mode.WITH_REFERENCE)
                .language("ru")
                .build();
EvaluationResult ruResult = agentGoalAccuracyMetric.multiTurnEvaluate(ruConfig, sample);
```

