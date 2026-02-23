# NVIDIA-Style Metrics

NVIDIA-style metrics provide simplified RAG evaluation using a 0-2 scoring scale that is normalized to 0-1. These metrics
are designed for practical production use with clear, interpretable scoring rubrics.

## Overview

|        Metric        |                        Evaluates                        |
|----------------------|---------------------------------------------------------|
| ContextRelevance     | Whether retrieved contexts are relevant to the question |
| ResponseGroundedness | Whether response is grounded in retrieved contexts      |
| AnswerAccuracy       | Whether response accurately matches reference answer    |

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
        default-provider:
          enabled: false
        default-options:
          temperature: 0.1
          max-tokens: 1000
  threads:
    virtual:
      enabled: true
```

---

## ContextRelevance

ContextRelevance evaluates whether retrieved contexts are relevant to the user's question. Each context chunk is
scored individually, and scores are averaged.

### How It Works

**Scoring Scale (0-2, normalized to 0-1):**

| Score |       Level        |                         Description                         |
|-------|--------------------|-------------------------------------------------------------|
| 0     | Not relevant       | Context does not contain information to answer the question |
| 1     | Partially relevant | Context contains some relevant information                  |
| 2     | Fully relevant     | Context contains comprehensive information to answer        |

```java
// From ContextRelevanceMetric.java - relevance evaluation
class Example {
    void evaluateRelevance(String userInput, List<String> contexts) {
        List<Double> contextScores = new ArrayList<>();

        for (String context : contexts) {
            // LLM evaluates each context against the question
            // Score: 0, 1, or 2 -> normalized to 0.0, 0.5, or 1.0
            double normalizedScore = llmScore / 2.0;
            contextScores.add(normalizedScore);
        }

        // Average across all contexts
        return contextScores.stream().average().orElse(0.0);
    }
}
```

### Example

```java
package ai.qa.solutions.metrics.nvidia.en;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.nvidia.ContextRelevanceMetric;
import ai.qa.solutions.sample.Sample;
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
@SpringBootTest(classes = ContextRelevanceTest.TestConfiguration.class)
class ContextRelevanceTest {

    @Configuration
    public static class TestConfiguration {
    }

    @Autowired
    private ContextRelevanceMetric contextRelevanceMetric;

    @Test
    @DisplayName("ContextRelevance: Highly relevant contexts")
    void testHighlyRelevantContexts() {
        Sample sample = Sample.builder()
                .userInput("What is photosynthesis?")
                .retrievedContexts(List.of(
                        "Photosynthesis is the process by which plants convert light energy into chemical energy.",
                        "During photosynthesis, plants use sunlight, carbon dioxide, and water to produce glucose and oxygen.",
                        "The chloroplast is the organelle where photosynthesis takes place in plant cells."
                ))
                .build();

        ContextRelevanceMetric.ContextRelevanceConfig config =
                ContextRelevanceMetric.ContextRelevanceConfig.builder()
                        .build();

        Double score = contextRelevanceMetric.singleTurnScore(config, sample);

        log.info("Context Relevance Score: {}", score);
        assertTrue(score >= 0.8, "Expected high score for relevant contexts");
    }

    @Test
    @DisplayName("ContextRelevance: Mixed relevance contexts")
    void testMixedRelevanceContexts() {
        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .retrievedContexts(List.of(
                        "Paris is the capital and largest city of France.",
                        "The Eiffel Tower is located in Paris and is a famous landmark.",
                        "The weather in Antarctica is extremely cold with temperatures below -60°C."
                ))
                .build();

        ContextRelevanceMetric.ContextRelevanceConfig config =
                ContextRelevanceMetric.ContextRelevanceConfig.builder()
                        .build();

        Double score = contextRelevanceMetric.singleTurnScore(config, sample);

        log.info("Context Relevance Score: {}", score);
        assertTrue(score >= 0.4 && score <= 0.8, "Expected moderate score for mixed relevance");
    }
}
```

### Configuration

|   Parameter   |     Type     | Required | Default |            Description            |
|---------------|--------------|----------|---------|-----------------------------------|
| `models`      | List<String> | No       | all     | Specific model IDs for evaluation |
| `temperature` | double       | No       | 0.1     | LLM temperature for determinism   |
| `language`    | String       | No       | `"en"`  | Language for explanations (`"en"`, `"ru"`) |

### When to Use

- RAG retrieval quality evaluation
- Search relevance assessment
- Document retrieval optimization
- Context filtering quality

---

## ResponseGroundedness

ResponseGroundedness evaluates whether the AI response is grounded in (supported by) the retrieved contexts.
This helps detect hallucinations where the model generates information not present in the context.

### How It Works

**Scoring Scale (0-2, normalized to 0-1):**

| Score |       Level        |                        Description                        |
|-------|--------------------|-----------------------------------------------------------|
| 0     | Not grounded       | Response contains significant information not in contexts |
| 1     | Partially grounded | Response is partially supported by contexts               |
| 2     | Fully grounded     | Response is completely supported by contexts              |

**Heuristic Shortcuts (when enabled):**

- Empty response → score 0.0
- Response exactly matches context → score 1.0

```java
// From ResponseGroundednessMetric.java - groundedness evaluation
class Example {
    Double evaluateGroundedness(String response, List<String> contexts, boolean useHeuristics) {
        // Heuristic shortcuts
        if (useHeuristics) {
            if (response.isEmpty()) return 0.0;
            if (contextContainsExactResponse(contexts, response)) return 1.0;
        }

        // LLM evaluation
        String combinedContext = String.join("\n\n", contexts);
        // LLM scores groundedness: 0, 1, or 2
        return llmScore / 2.0;
    }
}
```

### Example

```java
package ai.qa.solutions.metrics.nvidia.en;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.nvidia.ResponseGroundednessMetric;
import ai.qa.solutions.sample.Sample;
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
@SpringBootTest(classes = ResponseGroundednessTest.TestConfiguration.class)
class ResponseGroundednessTest {

    @Configuration
    public static class TestConfiguration {
    }

    @Autowired
    private ResponseGroundednessMetric responseGroundednessMetric;

    @Test
    @DisplayName("ResponseGroundedness: Fully grounded response")
    void testFullyGroundedResponse() {
        Sample sample = Sample.builder()
                .response("The Eiffel Tower is located in Paris, France and was completed in 1889.")
                .retrievedContexts(List.of(
                        "The Eiffel Tower is a wrought-iron lattice tower on the Champ de Mars in Paris, France.",
                        "Construction of the Eiffel Tower was completed on March 31, 1889."
                ))
                .build();

        ResponseGroundednessMetric.ResponseGroundednessConfig config =
                ResponseGroundednessMetric.ResponseGroundednessConfig.builder()
                        .useHeuristicShortcuts(true)
                        .build();

        Double score = responseGroundednessMetric.singleTurnScore(config, sample);

        log.info("Groundedness Score: {}", score);
        assertTrue(score >= 0.9, "Expected high score for grounded response");
    }

    @Test
    @DisplayName("ResponseGroundedness: Hallucinated response")
    void testHallucinatedResponse() {
        Sample sample = Sample.builder()
                .response("The Eiffel Tower is the tallest building in the world at 1000 meters and was built by Leonardo da Vinci.")
                .retrievedContexts(List.of(
                        "The Eiffel Tower is a wrought-iron lattice tower in Paris, France.",
                        "The tower is 330 meters tall and was designed by Gustave Eiffel."
                ))
                .build();

        ResponseGroundednessMetric.ResponseGroundednessConfig config =
                ResponseGroundednessMetric.ResponseGroundednessConfig.builder()
                        .useHeuristicShortcuts(true)
                        .build();

        Double score = responseGroundednessMetric.singleTurnScore(config, sample);

        log.info("Groundedness Score: {}", score);
        assertTrue(score <= 0.5, "Expected low score for hallucinated content");
    }

    @Test
    @DisplayName("ResponseGroundedness: Partially grounded response")
    void testPartiallyGroundedResponse() {
        Sample sample = Sample.builder()
                .response("Paris is the capital of France. It has a population of over 12 million people.")
                .retrievedContexts(List.of(
                        "Paris is the capital and most populous city of France."
                        // Note: No specific population number in context
                ))
                .build();

        ResponseGroundednessMetric.ResponseGroundednessConfig config =
                ResponseGroundednessMetric.ResponseGroundednessConfig.builder()
                        .build();

        Double score = responseGroundednessMetric.singleTurnScore(config, sample);

        log.info("Groundedness Score: {}", score);
        assertTrue(score >= 0.3 && score <= 0.7, "Expected moderate score for partial grounding");
    }
}
```

### Configuration

|        Parameter        |     Type     | Required | Default |            Description             |
|-------------------------|--------------|----------|---------|------------------------------------|
| `models`                | List<String> | No       | all     | Specific model IDs for evaluation  |
| `useHeuristicShortcuts` | boolean      | No       | true    | Enable fast path for trivial cases |
| `temperature`           | double       | No       | 0.1     | LLM temperature for determinism    |
| `language`              | String       | No       | `"en"`  | Language for explanations (`"en"`, `"ru"`) |

### When to Use

- Hallucination detection in RAG systems
- Response quality assurance
- Fact-checking generated content
- Production RAG monitoring

---

## AnswerAccuracy

AnswerAccuracy evaluates whether the AI response accurately matches a reference answer. It supports an optional
dual-judge mode for higher reliability.

### How It Works

**Scoring Scale (0-2, normalized to 0-1):**

| Score |       Level       |                        Description                         |
|-------|-------------------|------------------------------------------------------------|
| 0     | Incorrect         | Response is factually wrong or contradicts reference       |
| 1     | Partially correct | Response is partially correct but incomplete or has errors |
| 2     | Fully correct     | Response accurately matches the reference answer           |

**Dual-Judge Mode (optional):**

1. Initial judgment scores the response
2. Confirmation judgment reviews and may adjust the score

```java
// From AnswerAccuracyMetric.java - dual-judge evaluation
class Example {
    Double evaluateAccuracy(String response, String reference, boolean useDualJudge) {
        // Initial judgment
        int initialScore = llm.evaluate(response, reference);

        if (useDualJudge) {
            // Confirmation judgment reviews the initial assessment
            int confirmedScore = llm.confirm(response, reference, initialScore);
            return confirmedScore / 2.0;
        }

        return initialScore / 2.0;
    }
}
```

### Example

```java
package ai.qa.solutions.metrics.nvidia.en;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.nvidia.AnswerAccuracyMetric;
import ai.qa.solutions.sample.Sample;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

@Slf4j
@EnableAutoConfiguration
@SpringBootTest(classes = AnswerAccuracyTest.TestConfiguration.class)
class AnswerAccuracyTest {

    @Configuration
    public static class TestConfiguration {
    }

    @Autowired
    private AnswerAccuracyMetric answerAccuracyMetric;

    @Test
    @DisplayName("AnswerAccuracy: Correct answer")
    void testCorrectAnswer() {
        Sample sample = Sample.builder()
                .response("The capital of France is Paris.")
                .reference("Paris is the capital of France.")
                .build();

        AnswerAccuracyMetric.AnswerAccuracyConfig config =
                AnswerAccuracyMetric.AnswerAccuracyConfig.builder()
                        .useDualJudge(false)
                        .build();

        Double score = answerAccuracyMetric.singleTurnScore(config, sample);

        log.info("Answer Accuracy Score: {}", score);
        assertTrue(score >= 0.9, "Expected high score for correct answer");
    }

    @Test
    @DisplayName("AnswerAccuracy: Incorrect answer")
    void testIncorrectAnswer() {
        Sample sample = Sample.builder()
                .response("The capital of France is London.")
                .reference("Paris is the capital of France.")
                .build();

        AnswerAccuracyMetric.AnswerAccuracyConfig config =
                AnswerAccuracyMetric.AnswerAccuracyConfig.builder()
                        .useDualJudge(false)
                        .build();

        Double score = answerAccuracyMetric.singleTurnScore(config, sample);

        log.info("Answer Accuracy Score: {}", score);
        assertTrue(score <= 0.2, "Expected low score for incorrect answer");
    }

    @Test
    @DisplayName("AnswerAccuracy: Dual-judge mode for higher reliability")
    void testDualJudgeMode() {
        Sample sample = Sample.builder()
                .response("Water boils at 100 degrees Celsius at standard atmospheric pressure.")
                .reference("At standard atmospheric pressure (1 atm), water boils at 100°C or 212°F.")
                .build();

        AnswerAccuracyMetric.AnswerAccuracyConfig config =
                AnswerAccuracyMetric.AnswerAccuracyConfig.builder()
                        .useDualJudge(true)  // Enable confirmation judgment
                        .build();

        Double score = answerAccuracyMetric.singleTurnScore(config, sample);

        log.info("Answer Accuracy Score (dual-judge): {}", score);
        assertTrue(score >= 0.8, "Expected high score for factually correct answer");
    }

    @Test
    @DisplayName("AnswerAccuracy: Partially correct answer")
    void testPartiallyCorrectAnswer() {
        Sample sample = Sample.builder()
                .response("Einstein developed the theory of relativity.")
                .reference("Albert Einstein developed the theory of general relativity in 1915 and the theory of special relativity in 1905.")
                .build();

        AnswerAccuracyMetric.AnswerAccuracyConfig config =
                AnswerAccuracyMetric.AnswerAccuracyConfig.builder()
                        .build();

        Double score = answerAccuracyMetric.singleTurnScore(config, sample);

        log.info("Answer Accuracy Score: {}", score);
        assertTrue(score >= 0.4 && score <= 0.8, "Expected moderate score for partial answer");
    }
}
```

### Configuration

|   Parameter    |     Type     | Required | Default |            Description            |
|----------------|--------------|----------|---------|-----------------------------------|
| `models`       | List<String> | No       | all     | Specific model IDs for evaluation |
| `useDualJudge` | boolean      | No       | false   | Enable confirmation judgment      |
| `temperature`  | double       | No       | 0.1     | LLM temperature for determinism   |
| `language`     | String       | No       | `"en"`  | Language for explanations (`"en"`, `"ru"`) |

### When to Use

- Question-answering system evaluation
- Factual accuracy verification
- Knowledge retrieval testing
- High-stakes answer validation (use dual-judge)

---

## RAG Pipeline Evaluation

These three metrics together provide comprehensive RAG system evaluation:

```
User Question → [Retrieval] → Retrieved Contexts → [Generation] → Response
                     ↓                                   ↓
              ContextRelevance            ResponseGroundedness + AnswerAccuracy
```

### Complete RAG Evaluation Example

```java
class Example {
    void evaluateRAGPipeline(Sample sample) {
        // 1. Evaluate retrieval quality
        Double contextRelevance = contextRelevanceMetric.singleTurnScore(sample);

        // 2. Evaluate response grounding
        Double groundedness = responseGroundednessMetric.singleTurnScore(sample);

        // 3. Evaluate answer accuracy
        Double accuracy = answerAccuracyMetric.singleTurnScore(sample);

        // Combined score (example weighting)
        Double ragScore = 0.3 * contextRelevance + 0.3 * groundedness + 0.4 * accuracy;

        log.info("RAG Pipeline Score: {} (relevance={}, groundedness={}, accuracy={})",
                ragScore, contextRelevance, groundedness, accuracy);
    }
}
```

---

## Choosing the Right Metric

|     Evaluation Goal     |        Metric        |
|-------------------------|----------------------|
| Retrieval quality       | ContextRelevance     |
| Hallucination detection | ResponseGroundedness |
| Answer correctness      | AnswerAccuracy       |
| End-to-end RAG quality  | All three combined   |

---

## Sample Schema

|        Field        |     Type     |          Required By           |
|---------------------|--------------|--------------------------------|
| `userInput`         | String       | ContextRelevance               |
| `retrievedContexts` | List<String> | ContextRelevance, Groundedness |
| `response`          | String       | Groundedness, AnswerAccuracy   |
| `reference`         | String       | AnswerAccuracy                 |

```java
class Example {
    void createSample() {
        Sample sample = Sample.builder()
                .userInput("User's question")
                .retrievedContexts(List.of("context1", "context2"))
                .response("AI-generated response")
                .reference("Ground truth answer")
                .build();
    }
}
```

---

## Rich Evaluation API

All NVIDIA metrics support `singleTurnEvaluate()` returning `EvaluationResult` with score, explanation, per-model details, and metadata:

```java
import ai.qa.solutions.metric.EvaluationResult;

// Instead of Double score = metric.singleTurnScore(config, sample);
EvaluationResult result = answerAccuracyMetric.singleTurnEvaluate(config, sample);

log.info("Score: {}", result.getScore());
log.info("Explanation: {}", result.getExplanation().getSimpleDescription());
log.info("Model scores: {}", result.getModelScores());
log.info("Duration: {}ms", result.getTotalDuration().toMillis());

// Async variant
CompletableFuture<EvaluationResult> future =
        answerAccuracyMetric.singleTurnEvaluateAsync(config, sample);

// Russian language explanations
AnswerAccuracyMetric.AnswerAccuracyConfig ruConfig =
        AnswerAccuracyMetric.AnswerAccuracyConfig.builder()
                .language("ru")
                .build();
EvaluationResult ruResult = answerAccuracyMetric.singleTurnEvaluate(ruConfig, sample);
```

