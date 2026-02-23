# Response Metrics

Response metrics evaluate the quality of AI-generated responses by comparing them against reference answers.
These metrics use a combination of embedding-based similarity and NLI-based factual verification.

## Overview

|       Metric       |                  Approach                  |
|--------------------|--------------------------------------------|
| SemanticSimilarity | Embedding cosine similarity (no LLM calls) |
| FactualCorrectness | Claim decomposition + NLI verification     |
| AnswerCorrectness  | Combines factual and semantic scores       |

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
              - { id: anthropic/claude-3.5-sonnet }
              - { id: google/gemini-2.5-flash }
              - { id: openai/gpt-4o-mini }
            embedding-models:
              - { id: openai/text-embedding-3-large, dimensions: 3072 }
              - { id: qwen/qwen3-embedding-8b, dimensions: 1024 }
        default-provider:
          enabled: false
        default-options:
          temperature: 0.0
          max-tokens: 1000
        embedding-default-options:
          dimensions: 1024
  threads:
    virtual:
      enabled: true
```

---

## SemanticSimilarity

> **Reference:** [Sentence-BERT: Sentence Embeddings using Siamese BERT-Networks](https://arxiv.org/pdf/1908.10084.pdf)

SemanticSimilarity measures the semantic closeness between response and reference using embedding vectors.
This is a fast, cost-effective metric that doesn't require LLM calls.

### How It Works

1. **Embedding Computation**: Generate vector embeddings for both response and reference
2. **Cosine Similarity**: Calculate the cosine of the angle between vectors
3. **Optional Threshold**: Convert to binary pass/fail based on threshold

```java
// From SemanticSimilarityMetric.java - cosine similarity calculation
class Example {
    double cosineSimilarity(double[] vectorA, double[] vectorB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
```

### Score Interpretation

| Score Range |               Interpretation               |
|-------------|--------------------------------------------|
| 0.9 - 1.0   | Semantically identical                     |
| 0.8 - 0.9   | Very high similarity, nearly same meaning  |
| 0.5 - 0.8   | Moderate similarity, related but different |
| 0.0 - 0.5   | Low similarity, different meanings         |

### Example

```java
package ai.qa.solutions.metrics.response.en;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.response.SemanticSimilarityMetric;
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
@SpringBootTest(classes = SemanticSimilarityTest.TestConfiguration.class)
class SemanticSimilarityTest {

    @Configuration
    public static class TestConfiguration {
    }

    @Autowired
    private SemanticSimilarityMetric semanticSimilarityMetric;

    @Test
    @DisplayName("SemanticSimilarity: High similarity paraphrases")
    void testHighSimilarityParaphrases() {
        Sample sample = Sample.builder()
                .response("Machine learning is a subset of artificial intelligence.")
                .reference("ML is a branch of AI that enables systems to learn from data.")
                .build();

        SemanticSimilarityMetric.SemanticSimilarityConfig config =
                SemanticSimilarityMetric.SemanticSimilarityConfig.defaultConfig();

        Double score = semanticSimilarityMetric.singleTurnScore(config, sample);

        log.info("Semantic Similarity Score: {}", score);
        assertTrue(score >= 0.7, "Expected high similarity for paraphrases");
    }

    @Test
    @DisplayName("SemanticSimilarity: Low similarity different topics")
    void testLowSimilarityDifferentTopics() {
        Sample sample = Sample.builder()
                .response("The weather today is sunny and warm.")
                .reference("Quantum computing uses qubits for calculations.")
                .build();

        SemanticSimilarityMetric.SemanticSimilarityConfig config =
                SemanticSimilarityMetric.SemanticSimilarityConfig.defaultConfig();

        Double score = semanticSimilarityMetric.singleTurnScore(config, sample);

        log.info("Semantic Similarity Score: {}", score);
        assertTrue(score <= 0.5, "Expected low similarity for different topics");
    }

    @Test
    @DisplayName("SemanticSimilarity: Threshold-based classification")
    void testThresholdBasedClassification() {
        Sample sample = Sample.builder()
                .response("Python is a programming language used for web development.")
                .reference("Python is a versatile programming language popular for web applications.")
                .build();

        SemanticSimilarityMetric.SemanticSimilarityConfig config =
                SemanticSimilarityMetric.SemanticSimilarityConfig.builder()
                        .threshold(0.8)  // Binary: 1.0 if >= 0.8, else 0.0
                        .build();

        Double score = semanticSimilarityMetric.singleTurnScore(config, sample);

        log.info("Semantic Similarity Score (threshold=0.8): {}", score);
        assertTrue(score == 0.0 || score == 1.0, "Expected binary score with threshold");
    }
}
```

### Configuration

|  Parameter  |     Type     | Required | Default |                  Description                  |
|-------------|--------------|----------|---------|-----------------------------------------------|
| `threshold` | Double       | No       | null    | If set, returns 1.0 or 0.0 based on threshold |
| `models`    | List<String> | No       | all     | Embedding model IDs to use                    |
| `language`  | String       | No       | `"en"`  | Language for explanations (`"en"`, `"ru"`)    |

### When to Use

- Fast similarity scoring at scale
- Paraphrase detection
- Answer equivalence checking
- Cost-effective evaluation (no LLM calls)

---

## FactualCorrectness

> **RAGAS Reference:** [Documentation](https://docs.ragas.io/en/stable/concepts/metrics/available_metrics/factual_correctness/) | [Python Source](https://github.com/explodinggradients/ragas/blob/main/src/ragas/metrics/_factual_correctness.py)

FactualCorrectness evaluates factual accuracy by decomposing texts into atomic claims and verifying them using
Natural Language Inference (NLI).

### How It Works

1. **Claim Decomposition**: Break response and reference into atomic claims
2. **NLI Verification (Precision)**: Verify response claims against reference
3. **NLI Verification (Recall)**: Verify reference claims against response
4. **Score Computation**: Calculate precision, recall, or F1

```java
// From FactualCorrectnessMetric.java - factual correctness flow
class Example {
    double computeFactualCorrectness(String response, String reference) {
        // Step 1: Decompose into claims
        List<String> responseClaims = llm.decomposeClaims(response);
        List<String> referenceClaims = llm.decomposeClaims(reference);

        // Step 2: Verify response claims against reference (precision)
        double precision = verifyClaimsNLI(responseClaims, reference);

        // Step 3: Verify reference claims against response (recall)
        double recall = verifyClaimsNLI(referenceClaims, response);

        // Step 4: Compute F1
        return 2 * precision * recall / (precision + recall);
    }
}
```

**NLI Verdicts:**

- **SUPPORTED**: Claim can be inferred from context
- **CONTRADICTED**: Claim is contradicted by context
- **NEUTRAL**: Cannot be verified (not enough information)

### Example

```java
package ai.qa.solutions.metrics.response.en;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.response.FactualCorrectnessMetric;
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
@SpringBootTest(classes = FactualCorrectnessTest.TestConfiguration.class)
class FactualCorrectnessTest {

    @Configuration
    public static class TestConfiguration {
    }

    @Autowired
    private FactualCorrectnessMetric factualCorrectnessMetric;

    @Test
    @DisplayName("FactualCorrectness: High factual accuracy")
    void testHighFactualAccuracy() {
        Sample sample = Sample.builder()
                .response("Albert Einstein was a German-born physicist. "
                        + "He developed the theory of relativity. "
                        + "He won the Nobel Prize in Physics in 1921.")
                .reference("Albert Einstein was born in Germany in 1879. "
                        + "He is famous for developing the theory of relativity. "
                        + "Einstein received the Nobel Prize in Physics in 1921.")
                .build();

        FactualCorrectnessMetric.FactualCorrectnessConfig config =
                FactualCorrectnessMetric.FactualCorrectnessConfig.builder()
                        .mode(FactualCorrectnessMetric.Mode.F1)
                        .build();

        Double score = factualCorrectnessMetric.singleTurnScore(config, sample);

        log.info("Factual Correctness Score: {}", score);
        assertTrue(score >= 0.8, "Expected high score for factually accurate response");
    }

    @Test
    @DisplayName("FactualCorrectness: Factual errors detected")
    void testFactualErrorsDetected() {
        Sample sample = Sample.builder()
                .response("Einstein was born in France. "
                        + "He invented the telephone. "
                        + "He won the Nobel Prize in Chemistry.")
                .reference("Albert Einstein was born in Germany. "
                        + "He developed the theory of relativity. "
                        + "He won the Nobel Prize in Physics.")
                .build();

        FactualCorrectnessMetric.FactualCorrectnessConfig config =
                FactualCorrectnessMetric.FactualCorrectnessConfig.builder()
                        .mode(FactualCorrectnessMetric.Mode.F1)
                        .build();

        Double score = factualCorrectnessMetric.singleTurnScore(config, sample);

        log.info("Factual Correctness Score: {}", score);
        assertTrue(score <= 0.3, "Expected low score for factually incorrect response");
    }

    @Test
    @DisplayName("FactualCorrectness: Precision mode")
    void testPrecisionMode() {
        Sample sample = Sample.builder()
                .response("Paris is the capital of France.")  // Correct but incomplete
                .reference("Paris is the capital of France. It has a population of 2 million.")
                .build();

        FactualCorrectnessMetric.FactualCorrectnessConfig config =
                FactualCorrectnessMetric.FactualCorrectnessConfig.builder()
                        .mode(FactualCorrectnessMetric.Mode.PRECISION)
                        .build();

        Double score = factualCorrectnessMetric.singleTurnScore(config, sample);

        log.info("Factual Correctness Precision: {}", score);
        assertTrue(score >= 0.9, "Expected high precision for correct but incomplete response");
    }

    @Test
    @DisplayName("FactualCorrectness: Recall mode")
    void testRecallMode() {
        Sample sample = Sample.builder()
                .response("Paris is the capital of France.")  // Missing population info
                .reference("Paris is the capital of France. It has a population of 2 million.")
                .build();

        FactualCorrectnessMetric.FactualCorrectnessConfig config =
                FactualCorrectnessMetric.FactualCorrectnessConfig.builder()
                        .mode(FactualCorrectnessMetric.Mode.RECALL)
                        .build();

        Double score = factualCorrectnessMetric.singleTurnScore(config, sample);

        log.info("Factual Correctness Recall: {}", score);
        assertTrue(score >= 0.4 && score <= 0.7, "Expected moderate recall for incomplete response");
    }
}
```

### Configuration

| Parameter  |     Type     | Required | Default |                Description                 |
|------------|--------------|----------|---------|--------------------------------------------|
| `mode`     | Mode         | No       | F1      | F1, PRECISION, or RECALL scoring mode      |
| `models`   | List<String> | No       | all     | Model IDs for claim decomposition and NLI  |
| `language` | String       | No       | `"en"`  | Language for explanations (`"en"`, `"ru"`) |

**Scoring Modes:**

- **F1**: Harmonic mean of precision and recall (balanced)
- **PRECISION**: Focus on correctness of response claims
- **RECALL**: Focus on coverage of reference claims

### When to Use

- Detailed factual verification
- Detecting specific factual errors
- Understanding precision vs. recall trade-offs
- High-stakes accuracy evaluation

---

## AnswerCorrectness

> **RAGAS Reference:** [Documentation](https://docs.ragas.io/en/stable/concepts/metrics/available_metrics/answer_correctness/) | [Python Source](https://github.com/explodinggradients/ragas/blob/main/src/ragas/metrics/_answer_correctness.py)

AnswerCorrectness combines FactualCorrectness and SemanticSimilarity for comprehensive response evaluation.

### How It Works

1. **Factual Correctness**: Decompose claims + NLI verification
2. **Semantic Similarity**: Embedding cosine similarity
3. **Weighted Combination**: `factualWeight * factual + semanticWeight * semantic`

Default weights: 75% factual, 25% semantic

```java
// From AnswerCorrectnessMetric.java - combined score
class Example {
    double computeAnswerCorrectness(Sample sample, double factualWeight, double semanticWeight) {
        // Step 1: Factual correctness (NLI-based)
        double factualScore = factualCorrectnessMetric.singleTurnScore(sample);

        // Step 2: Semantic similarity (embedding-based)
        double semanticScore = semanticSimilarityMetric.singleTurnScore(sample);

        // Step 3: Weighted combination
        return factualWeight * factualScore + semanticWeight * semanticScore;
    }
}
```

### Example

```java
package ai.qa.solutions.metrics.response.en;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.response.AnswerCorrectnessMetric;
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
@SpringBootTest(classes = AnswerCorrectnessTest.TestConfiguration.class)
class AnswerCorrectnessTest {

    @Configuration
    public static class TestConfiguration {
    }

    @Autowired
    private AnswerCorrectnessMetric answerCorrectnessMetric;

    @Test
    @DisplayName("AnswerCorrectness: High correctness")
    void testHighCorrectness() {
        Sample sample = Sample.builder()
                .response("The Great Wall of China is over 13,000 miles long "
                        + "and was built to protect against invasions.")
                .reference("The Great Wall of China stretches over 13,000 miles. "
                        + "It was constructed as a defense against invaders.")
                .build();

        AnswerCorrectnessMetric.AnswerCorrectnessConfig config =
                AnswerCorrectnessMetric.AnswerCorrectnessConfig.defaultConfig();

        Double score = answerCorrectnessMetric.singleTurnScore(config, sample);

        log.info("Answer Correctness Score: {}", score);
        assertTrue(score >= 0.8, "Expected high correctness score");
    }

    @Test
    @DisplayName("AnswerCorrectness: Equal weights configuration")
    void testEqualWeights() {
        Sample sample = Sample.builder()
                .response("Python is a programming language.")
                .reference("Python is a high-level, interpreted programming language.")
                .build();

        AnswerCorrectnessMetric.AnswerCorrectnessConfig config =
                AnswerCorrectnessMetric.AnswerCorrectnessConfig.equalWeights();  // 50/50

        Double score = answerCorrectnessMetric.singleTurnScore(config, sample);

        log.info("Answer Correctness Score (equal weights): {}", score);
        assertTrue(score >= 0.5 && score <= 0.9, "Expected moderate-high score");
    }

    @Test
    @DisplayName("AnswerCorrectness: Factual-focused configuration")
    void testFactualFocused() {
        Sample sample = Sample.builder()
                .response("Water boils at 100°C. Ice melts at 0°C.")
                .reference("At standard pressure, water boils at 100 degrees Celsius "
                        + "and ice melts at 0 degrees Celsius.")
                .build();

        AnswerCorrectnessMetric.AnswerCorrectnessConfig config =
                AnswerCorrectnessMetric.AnswerCorrectnessConfig.factualFocused();  // 90/10

        Double score = answerCorrectnessMetric.singleTurnScore(config, sample);

        log.info("Answer Correctness Score (factual-focused): {}", score);
        assertTrue(score >= 0.8, "Expected high score with factual focus");
    }

    @Test
    @DisplayName("AnswerCorrectness: Custom weights")
    void testCustomWeights() {
        Sample sample = Sample.builder()
                .response("Machine learning enables computers to learn from data.")
                .reference("ML allows systems to automatically improve from experience.")
                .build();

        AnswerCorrectnessMetric.AnswerCorrectnessConfig config =
                AnswerCorrectnessMetric.AnswerCorrectnessConfig.builder()
                        .factualWeight(0.6)
                        .semanticWeight(0.4)
                        .build();

        Double score = answerCorrectnessMetric.singleTurnScore(config, sample);

        log.info("Answer Correctness Score (60/40 weights): {}", score);
        assertTrue(score >= 0.0 && score <= 1.0, "Score should be normalized");
    }
}
```

### Configuration

|    Parameter     |     Type     | Required | Default |                Description                 |
|------------------|--------------|----------|---------|--------------------------------------------|
| `factualWeight`  | double       | No       | 0.75    | Weight for factual correctness component   |
| `semanticWeight` | double       | No       | 0.25    | Weight for semantic similarity component   |
| `models`         | List<String> | No       | all     | Model IDs for factual correctness          |
| `language`       | String       | No       | `"en"`  | Language for explanations (`"en"`, `"ru"`) |

**Preset Configurations:**

```java
class Example {
    void configurations() {
        // Default: 75% factual, 25% semantic
        AnswerCorrectnessConfig.defaultConfig();

        // Equal: 50% factual, 50% semantic
        AnswerCorrectnessConfig.equalWeights();

        // Factual focus: 90% factual, 10% semantic
        AnswerCorrectnessConfig.factualFocused();

        // Semantic focus: 10% factual, 90% semantic
        AnswerCorrectnessConfig.semanticFocused();
    }
}
```

### When to Use

- Comprehensive response quality evaluation
- Balanced factual and semantic assessment
- General-purpose answer grading
- Default choice when unsure which metric to use

---

## Choosing the Right Metric

|          Need          |       Metric       |              Why              |
|------------------------|--------------------|-------------------------------|
| Fast similarity check  | SemanticSimilarity | No LLM calls, just embeddings |
| Detailed fact checking | FactualCorrectness | NLI-based claim verification  |
| Balanced evaluation    | AnswerCorrectness  | Combines both approaches      |
| High-volume evaluation | SemanticSimilarity | Most cost-effective           |
| Critical accuracy      | FactualCorrectness | Identifies specific errors    |
| General purpose        | AnswerCorrectness  | Best default choice           |

---

## Sample Schema

All response metrics require `response` and `reference`:

```java
class Example {
    void createSample() {
        Sample sample = Sample.builder()
                .response("AI-generated response to evaluate")
                .reference("Ground truth reference answer")
                .build();
    }
}
```

|    Field    |  Type  | Required |          Description           |
|-------------|--------|----------|--------------------------------|
| `response`  | String | Yes      | Generated response to evaluate |
| `reference` | String | Yes      | Ground truth reference         |

---

## Rich Evaluation API

All response metrics support `singleTurnEvaluate()` returning `EvaluationResult` with score, explanation, per-model details, and metadata:

```java
import ai.qa.solutions.metric.EvaluationResult;

// Instead of Double score = metric.singleTurnScore(config, sample);
EvaluationResult result = answerCorrectnessMetric.singleTurnEvaluate(config, sample);

log.info("Score: {}", result.getScore());
log.info("Explanation: {}", result.getExplanation().getSimpleDescription());
log.info("Model scores: {}", result.getModelScores());
log.info("Duration: {}ms", result.getTotalDuration().toMillis());

// Async variant
CompletableFuture<EvaluationResult> future =
        answerCorrectnessMetric.singleTurnEvaluateAsync(config, sample);

// Russian language explanations
AnswerCorrectnessMetric.AnswerCorrectnessConfig ruConfig =
        AnswerCorrectnessMetric.AnswerCorrectnessConfig.builder()
                .language("ru")
                .build();
EvaluationResult ruResult = answerCorrectnessMetric.singleTurnEvaluate(ruConfig, sample);
```

