# Retrieval Metrics

Retrieval metrics evaluate the quality of RAG (Retrieval-Augmented Generation) systems. They measure how well retrieved
contexts support answer generation, detect hallucinations, and assess retrieval ranking quality.

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
    # Chat models for multi-model evaluation
    chat-models:
      default-options:
        temperature: 0.0
        max-tokens: 1000
        top-p: 1.0
      list:
        - { id: anthropic/claude-4.5-sonnet }
        - { id: google/gemini-2.5-flash }
        - { id: openai/gpt-4o-mini }
        - { id: deepseek/deepseek-v3.2 }
    # Embedding models for ResponseRelevancy
    embedding-models:
      default-options:
        dimensions: 1024
      list:
        - id: openai/text-embedding-3-large
          options: { dimensions: 3072 }
        - id: qwen/qwen3-embedding-8b
  threads:
    virtual:
      enabled: true
```

---

## ContextEntityRecall

ContextEntityRecall measures the recall of entities present in both reference and retrieved contexts relative to
entities in the reference alone. This metric is particularly useful for fact-based applications like tourism help desks
or historical QA systems.

### How It Works

1. **Entity Extraction**: Extracts named entities from the reference answer using LLM
2. **Context Analysis**: Extracts entities from all retrieved contexts
3. **Recall Calculation**: Computes the intersection between reference and context entities
4. **Score Computation**: Returns ratio of covered entities to total reference entities

```java
// From ContextEntityRecallMetric.java - entity recall calculation
class Example {
    double calculateEntityRecall(Set<String> referenceEntities, Set<String> contextEntities) {
        Set<String> commonEntities = new HashSet<>(referenceEntities);
        commonEntities.retainAll(contextEntities);
        return (double) commonEntities.size() / referenceEntities.size();
    }
}
```

**Entity Types Detected:**

- Person names (Albert Einstein, Napoleon)
- Place names (Paris, Eiffel Tower, France)
- Organizations (UNESCO, European Union)
- Dates and times (1889, July 16, 1969)
- Events (World War II, Apollo 11 mission)
- Numbers and measurements (21,196 kilometers, 50,000 spectators)

### Example

```java
package ai.qa.solutions.metrics.retrieval.en;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.retrieval.ContextEntityRecallMetric;
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
@SpringBootTest(classes = ContextEntityRecallTest.TestConfiguration.class)
class ContextEntityRecallTest {

    @Configuration
    public static class TestConfiguration {}

    @Autowired
    private ContextEntityRecallMetric contextEntityRecallMetric;

    @Test
    @DisplayName("ContextEntityRecall: High entity coverage")
    void testHighEntityCoverage() {
        Sample sample = Sample.builder()
                .reference("The Eiffel Tower is located in Paris, France. "
                        + "It was completed in 1889 for the World's Fair.")
                .retrievedContexts(List.of(
                        "The Eiffel Tower, located in Paris, France, is one of the most iconic landmarks.",
                        "Completed in 1889, it was constructed for the 1889 World's Fair.",
                        "Millions of visitors are attracted to it each year."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        Double score = contextEntityRecallMetric.singleTurnScore(config, sample);

        log.info("Entity Recall Score: {}", score);
        assertTrue(score >= 0.7, "Expected high score for good entity coverage");
    }

    @Test
    @DisplayName("ContextEntityRecall: Poor entity coverage")
    void testPoorEntityCoverage() {
        Sample sample = Sample.builder()
                .reference("Albert Einstein was born in Ulm, Germany on March 14, 1879. "
                        + "He won the Nobel Prize in Physics in 1921.")
                .retrievedContexts(List.of(
                        "Physics is a fundamental science that studies matter and energy.",
                        "Scientists have made many important discoveries throughout history."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        Double score = contextEntityRecallMetric.singleTurnScore(config, sample);

        log.info("Entity Recall Score: {}", score);
        assertTrue(score <= 0.3, "Expected low score for poor entity coverage");
    }
}
```

### Configuration

| Parameter |     Type     | Required | Default |            Description            |
|-----------|--------------|----------|---------|-----------------------------------|
| `models`  | List<String> | No       | all     | Specific model IDs for evaluation |

### When to Use

- Tourism and travel systems (ensuring location/date coverage)
- Historical QA systems (verifying people, dates, events coverage)
- Knowledge base evaluation (assessing factual completeness)
- News and information retrieval (comprehensive entity coverage)

---

## ContextPrecision

ContextPrecision evaluates the retriever's ability to rank relevant chunks higher in the retrieved context list. It
calculates Average Precision (AP) which rewards relevant contexts appearing earlier in the ranking.

### How It Works

1. **Relevance Assessment**: Each retrieved context chunk is evaluated for relevance against reference or response
2. **Precision@k Calculation**: For each position k, calculates precision considering all items up to position k
3. **Average Precision**: Computes weighted average of precision values at relevant positions
4. **Final Score**: Returns AP score between 0.0 and 1.0

```java
// From ContextPrecisionMetric.java - Average Precision calculation
class Example {
    Double calculateContextPrecision(List<Boolean> relevanceScores) {
        long totalRelevant = relevanceScores.stream().filter(r -> r).count();
        if (totalRelevant == 0) return 0.0;

        double sum = IntStream.range(0, relevanceScores.size())
                .filter(k -> relevanceScores.get(k))
                .mapToDouble(k -> {
                    long relevantUpToK = relevanceScores.subList(0, k + 1).stream()
                            .mapToInt(relevant -> relevant ? 1 : 0).sum();
                    return (double) relevantUpToK / (k + 1);
                })
                .sum();

        return sum / totalRelevant;
    }
}
```

### Example

```java
package ai.qa.solutions.metrics.retrieval.en;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.retrieval.ContextPrecisionMetric;
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
@SpringBootTest(classes = ContextPrecisionTest.TestConfiguration.class)
class ContextPrecisionTest {

    @Configuration
    public static class TestConfiguration {}

    @Autowired
    private ContextPrecisionMetric contextPrecisionMetric;

    @Test
    @DisplayName("ContextPrecision: Reference-based evaluation")
    void testReferenceBased() {
        Sample sample = Sample.builder()
                .userInput("What is photosynthesis?")
                .reference("Photosynthesis is the process by which plants use sunlight, "
                        + "carbon dioxide, and water to produce glucose and oxygen.")
                .retrievedContexts(List.of(
                        "Photosynthesis is a biological process where plants convert light energy.",
                        "The process involves: 6CO2 + 6H2O + light → C6H12O6 + 6O2.",
                        "Plants are autotrophs that produce their own food."))
                .build();

        ContextPrecisionMetric.ContextPrecisionConfig config =
                ContextPrecisionMetric.ContextPrecisionConfig.builder()
                        .evaluationStrategy(ContextPrecisionMetric.EvaluationStrategy.REFERENCE_BASED)
                        .build();

        Double score = contextPrecisionMetric.singleTurnScore(config, sample);

        log.info("Context Precision Score: {}", score);
        assertTrue(score >= 0.8, "Expected high score for relevant contexts");
    }

    @Test
    @DisplayName("ContextPrecision: Poor ordering with irrelevant context first")
    void testPoorOrdering() {
        Sample sample = Sample.builder()
                .userInput("What is quantum computing?")
                .response("Quantum computing uses quantum mechanical phenomena.")
                .retrievedContexts(List.of(
                        "The price of groceries has increased this year.",  // Irrelevant first
                        "Quantum computers use quantum bits (qubits).",
                        "Quantum computing leverages superposition and entanglement."))
                .build();

        ContextPrecisionMetric.ContextPrecisionConfig config =
                ContextPrecisionMetric.ContextPrecisionConfig.builder()
                        .evaluationStrategy(ContextPrecisionMetric.EvaluationStrategy.RESPONSE_BASED)
                        .build();

        Double score = contextPrecisionMetric.singleTurnScore(config, sample);

        log.info("Context Precision Score: {}", score);
        assertTrue(score <= 0.7, "Expected lower score when irrelevant context is first");
    }
}
```

### Configuration

|      Parameter       |        Type        | Required |   Default   |            Description            |
|----------------------|--------------------|----------|-------------|-----------------------------------|
| `evaluationStrategy` | EvaluationStrategy | No       | Auto-detect | REFERENCE_BASED or RESPONSE_BASED |
| `models`             | List<String>       | No       | all         | Specific model IDs for evaluation |

**Evaluation Strategies:**

- **REFERENCE_BASED**: Uses reference answer as gold standard (preferred when available)
- **RESPONSE_BASED**: Uses AI response for relevance evaluation
- **Auto-detect**: Chooses REFERENCE_BASED if reference is available, otherwise RESPONSE_BASED

### When to Use

- Retrieval system optimization (measuring ranking quality)
- Search relevance evaluation (assessing document prioritization)
- RAG system tuning (optimizing retrieval parameters)
- Comparing different retrieval strategies

---

## ContextRecall

ContextRecall measures how many statements in the reference answer can be attributed to the retrieved contexts. This
metric evaluates the completeness of retrieved information.

### How It Works

1. **Statement Decomposition**: Breaks reference answer into individual sentences
2. **Attribution Analysis**: Evaluates each statement against retrieved contexts
3. **Support Classification**: Determines if each statement can be attributed (1) or not (0)
4. **Recall Calculation**: Returns ratio of attributable statements to total statements

```java
// From ContextRecallMetric.java - recall calculation
class Example {
    Double calculateContextRecall(List<ContextRecallClassification> classifications) {
        long attributedStatements = classifications.stream()
                .mapToInt(ContextRecallClassification::attributed)
                .sum();
        return (double) attributedStatements / classifications.size();
    }
}
```

### Example

```java
package ai.qa.solutions.metrics.retrieval.en;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.retrieval.ContextRecallMetric;
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
@SpringBootTest(classes = ContextRecallTest.TestConfiguration.class)
class ContextRecallTest {

    @Configuration
    public static class TestConfiguration {}

    @Autowired
    private ContextRecallMetric contextRecallMetric;

    @Test
    @DisplayName("ContextRecall: High recall - all statements supported")
    void testHighRecall() {
        Sample sample = Sample.builder()
                .userInput("Tell me about photosynthesis")
                .reference("Photosynthesis converts light energy to chemical energy. "
                        + "It occurs in chloroplasts. The process requires CO₂, water, and sunlight. "
                        + "Oxygen is released as a byproduct.")
                .retrievedContexts(List.of(
                        "Photosynthesis is a process where plants convert sunlight into chemical energy.",
                        "Chloroplasts are organelles in plant cells where photosynthesis occurs.",
                        "During photosynthesis, oxygen is produced as a waste product.",
                        "The process requires carbon dioxide, water, and light energy."))
                .build();

        ContextRecallMetric.ContextRecallConfig config =
                ContextRecallMetric.ContextRecallConfig.builder().build();

        Double score = contextRecallMetric.singleTurnScore(config, sample);

        log.info("Context Recall Score: {}", score);
        assertTrue(score >= 0.9, "Expected high score when all statements are supported");
    }
}
```

### Configuration

| Parameter |     Type     | Required | Default |            Description            |
|-----------|--------------|----------|---------|-----------------------------------|
| `models`  | List<String> | No       | all     | Specific model IDs for evaluation |

### When to Use

- Comprehensive information retrieval (ensuring all needed info is accessible)
- Knowledge base completeness verification
- RAG system evaluation (measuring information coverage quality)
- Retrieval gap analysis (identifying missing information)

---

## Faithfulness

Faithfulness measures factual consistency between the generated response and retrieved contexts. It identifies
hallucinations and ensures responses are grounded in provided information.

### How It Works

1. **Statement Generation**: Decomposes the response into atomic statements without pronouns
2. **Faithfulness Evaluation**: Checks each statement against retrieved contexts
3. **Verdict Assignment**: For each statement, verdict is 1 (can be inferred) or 0 (cannot be inferred)
4. **Score Computation**: Returns ratio of faithful statements to total statements

```java
// From FaithfulnessMetric.java - faithfulness calculation
class Example {
    Double calculateFaithfulness(VerdictsResponse verdicts) {
        long faithfulStatements = verdicts.verdicts().stream()
                .filter(v -> v.verdict() != null && v.verdict() == 1)
                .count();
        return (double) faithfulStatements / verdicts.verdicts().size();
    }
}
```

### Example

```java
package ai.qa.solutions.metrics.retrieval.en;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.retrieval.FaithfulnessMetric;
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
@SpringBootTest(classes = FaithfulnessTest.TestConfiguration.class)
class FaithfulnessTest {

    @Configuration
    public static class TestConfiguration {}

    @Autowired
    private FaithfulnessMetric faithfulnessMetric;

    @Test
    @DisplayName("Faithfulness: Perfect faithfulness - all claims supported")
    void testPerfectFaithfulness() {
        Sample sample = Sample.builder()
                .userInput("When was the first Super Bowl?")
                .response("The first Super Bowl was held on January 15, 1967.")
                .retrievedContexts(List.of(
                        "The first Super Bowl was held on January 15, 1967, "
                                + "at the Los Angeles Memorial Coliseum."))
                .build();

        Double score = faithfulnessMetric.singleTurnScore(sample);

        log.info("Faithfulness Score: {}", score);
        assertTrue(score >= 0.9, "Expected high score for fully supported answer");
    }

    @Test
    @DisplayName("Faithfulness: Low faithfulness - hallucinated information")
    void testHallucination() {
        Sample sample = Sample.builder()
                .userInput("What courses is John taking?")
                .response("John is taking Data Structures, Algorithms, and Artificial Intelligence. "
                        + "He also has a part-time job at the university library.")
                .retrievedContexts(List.of(
                        "John is a student at XYZ University pursuing a Computer Science degree. "
                                + "He is enrolled in Data Structures, Algorithms, and Database Management. "
                                + "John often stays late in the library to work on his projects."))
                .build();

        Double score = faithfulnessMetric.singleTurnScore(sample);

        log.info("Faithfulness Score: {}", score);
        assertTrue(score <= 0.7, "Expected low score due to hallucinated AI course and job claim");
    }
}
```

### Configuration

| Parameter |     Type     | Required | Default |            Description            |
|-----------|--------------|----------|---------|-----------------------------------|
| `models`  | List<String> | No       | all     | Specific model IDs for evaluation |

### When to Use

- Hallucination detection (identifying unsupported claims)
- RAG system validation (ensuring responses are grounded)
- Quality assurance (maintaining factual accuracy)
- Medical/legal applications (critical domains requiring precision)

---

## NoiseSensitivity

NoiseSensitivity measures how often a system makes errors by providing incorrect responses when utilizing either
relevant or irrelevant retrieved documents. **Lower scores indicate better performance.**

### How It Works

1. **Statement Decomposition**: Breaks both reference and response into atomic statements
2. **Ground Truth Evaluation**: Evaluates response statements against reference
3. **Context Relevance Classification**: Determines which contexts are relevant based on reference
4. **Error Attribution**: Identifies incorrect statements attributable to relevant or irrelevant contexts
5. **Sensitivity Calculation**: Computes proportion of context-influenced errors

```java
// From NoiseSensitivityMetric.java - sensitivity calculation for RELEVANT mode
class Example {
    Double calculateNoiseSensitivity(boolean[] incorrect, boolean[] relevantFaithful, int numStatements) {
        int count = 0;
        for (int i = 0; i < numStatements; i++) {
            if (relevantFaithful[i] && incorrect[i]) {
                count++;
            }
        }
        return (double) count / numStatements;
    }
}
```

### Example

```java
package ai.qa.solutions.metrics.retrieval.en;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.retrieval.NoiseSensitivityMetric;
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
@SpringBootTest(classes = NoiseSensitivityTest.TestConfiguration.class)
class NoiseSensitivityTest {

    @Configuration
    public static class TestConfiguration {}

    @Autowired
    private NoiseSensitivityMetric noiseSensitivityMetric;

    @Test
    @DisplayName("NoiseSensitivity: Low sensitivity - good robustness")
    void testLowSensitivity() {
        Sample sample = Sample.builder()
                .userInput("What causes earthquakes?")
                .response("Earthquakes are caused by tectonic plate movement.")
                .reference("Earthquakes are caused by movement of tectonic plates. "
                        + "Plates suddenly shift and release energy.")
                .retrievedContexts(List.of(
                        "Tectonic plates are large sections of Earth's crust that move slowly.",
                        "When tectonic plates collide, they can cause earthquakes.",
                        "Seismic waves are energy released during earthquakes.",
                        "Best time to visit Japan is during cherry blossom season."))  // Irrelevant
                .build();

        NoiseSensitivityMetric.NoiseSensitivityConfig config =
                NoiseSensitivityMetric.NoiseSensitivityConfig.builder()
                        .mode(NoiseSensitivityMetric.NoiseSensitivityMode.RELEVANT)
                        .build();

        Double score = noiseSensitivityMetric.singleTurnScore(config, sample);

        log.info("Noise Sensitivity Score: {}", score);
        assertTrue(score <= 0.3, "Expected low sensitivity score (good robustness)");
    }
}
```

### Configuration

| Parameter |         Type         | Required | Default  |              Description               |
|-----------|----------------------|----------|----------|----------------------------------------|
| `mode`    | NoiseSensitivityMode | No       | RELEVANT | RELEVANT or IRRELEVANT evaluation mode |
| `models`  | List<String>         | No       | all      | Specific model IDs for evaluation      |

**Evaluation Modes:**

- **RELEVANT**: Measures errors attributable to relevant retrieved contexts
- **IRRELEVANT**: Measures errors attributable to irrelevant retrieved contexts

### Result Interpretation

**Lower scores are better:**

- **0.0-0.1**: Excellent robustness
- **0.1-0.3**: Good robustness
- **0.3-0.5**: Moderate robustness
- **0.5-1.0**: Poor robustness, highly sensitive to noise

### When to Use

- Robustness testing (evaluating system behavior with noisy retrieval)
- Error analysis (understanding how irrelevant contexts affect quality)
- System optimization (improving response generation despite noisy inputs)
- Production monitoring (detecting degradation due to retrieval issues)

---

## ResponseRelevancy

ResponseRelevancy measures how relevant a response is to the user's input. It detects incomplete answers, off-topic
responses, and noncommittal (evasive) statements.

### How It Works

1. **Question Generation**: LLM generates N artificial questions that the response could be answering
2. **Noncommittal Detection**: Each generated question includes a flag for noncommittal answers
3. **Noncommittal Check**: If all questions indicate noncommittal, returns 0.0 immediately
4. **Embedding Computation**: Gets vector representations of original question and generated questions
5. **Similarity Calculation**: Computes cosine similarity between original and each generated question
6. **Aggregation**: Returns mean of all similarity scores

```java
// From ResponseRelevancyMetric.java - cosine similarity calculation
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

**Key Insight:**

- If response is relevant, generated questions will be semantically similar to original question
- If response is off-topic or incomplete, generated questions will differ from original
- Noncommittal answers ("I don't know") automatically receive 0.0 score

### Example

```java
package ai.qa.solutions.metrics.retrieval.en;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.retrieval.ResponseRelevancyMetric;
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
@SpringBootTest(classes = ResponseRelevancyTest.TestConfiguration.class)
class ResponseRelevancyTest {

    @Configuration
    public static class TestConfiguration {}

    @Autowired
    private ResponseRelevancyMetric responseRelevancyMetric;

    @Test
    @DisplayName("ResponseRelevancy: Complete relevant answer")
    void testCompleteAnswer() {
        Sample sample = Sample.builder()
                .userInput("Where is France located and what is its capital?")
                .response("France is located in Western Europe, and its capital is Paris.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Response Relevancy Score: {}", score);
        assertTrue(score >= 0.85, "Expected high score for complete answer");
    }

    @Test
    @DisplayName("ResponseRelevancy: Incomplete answer")
    void testIncompleteAnswer() {
        Sample sample = Sample.builder()
                .userInput("Where is France located and what is its capital?")
                .response("France is located in Western Europe.")  // Missing capital info
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Response Relevancy Score: {}", score);
        assertTrue(score >= 0.5 && score <= 0.8, "Expected moderate score for incomplete answer");
    }

    @Test
    @DisplayName("ResponseRelevancy: Noncommittal answer")
    void testNoncommittalAnswer() {
        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("I don't know what the capital of France is.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Response Relevancy Score: {}", score);
        assertTrue(score < 0.1, "Expected zero score for noncommittal answer");
    }
}
```

### Configuration

|      Parameter      |     Type     | Required | Default |                 Description                 |
|---------------------|--------------|----------|---------|---------------------------------------------|
| `numberOfQuestions` | int          | No       | 3       | Number of questions to generate from answer |
| `models`            | List<String> | No       | all     | Specific model IDs for evaluation           |

### When to Use

- Chatbot quality evaluation (ensuring responses answer user questions)
- Question-answering systems (measuring answer relevance)
- Dialogue systems (evaluating multi-turn conversation quality)
- Virtual assistants (quality control for responses)

---

## Choosing the Right Metric

|            Use Case            | Recommended Metric  |                    Why                    |
|--------------------------------|---------------------|-------------------------------------------|
| Entity-focused applications    | ContextEntityRecall | Measures coverage of factual entities     |
| Retrieval ranking optimization | ContextPrecision    | Evaluates ranking quality of contexts     |
| Information completeness       | ContextRecall       | Measures support for reference statements |
| Hallucination detection        | Faithfulness        | Identifies unsupported claims             |
| System robustness testing      | NoiseSensitivity    | Measures sensitivity to noisy inputs      |
| Response relevance evaluation  | ResponseRelevancy   | Measures answer-question alignment        |

---

## Sample Schema

All retrieval metrics use the `Sample` class for input:

```java
class Example {
    void createSample() {
        Sample sample = Sample.builder()
                .userInput("User's question")                        // Required for most metrics
                .response("AI-generated response")                   // Required for Faithfulness, NoiseSensitivity
                .reference("Ground truth answer")                    // Required for ContextRecall, NoiseSensitivity
                .retrievedContexts(List.of("context1", "context2"))  // Required for all retrieval metrics
                .build();
    }
}
```

|        Field        |     Type     |                     Required By                      |
|---------------------|--------------|------------------------------------------------------|
| `userInput`         | String       | ContextPrecision, ContextRecall, ResponseRelevancy   |
| `response`          | String       | Faithfulness, NoiseSensitivity, ResponseRelevancy    |
| `reference`         | String       | ContextEntityRecall, ContextRecall, NoiseSensitivity |
| `retrievedContexts` | List<String> | All retrieval metrics                                |

