# General Purpose Metrics

General purpose metrics evaluate AI responses without domain-specific requirements. They are task-agnostic and work
across any type of generated content.

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

## AspectCritic

> **RAGAS Reference:** [Documentation](https://docs.ragas.io/en/stable/concepts/metrics/available_metrics/aspect_critic/) | [Python Source](https://github.com/explodinggradients/ragas/blob/main/src/ragas/metrics/_aspect_critic.py)

AspectCritic is a binary metric that evaluates responses against a free-form natural language criterion. It returns
`1.0` (criterion met) or `0.0` (criterion not met).

### How It Works

The metric uses majority voting to determine the final verdict:

1. The LLM evaluates the response against the criterion `strictness` times
2. Each evaluation produces a binary verdict (true/false)
3. Majority voting determines the final score: `1.0` if more than half are true, `0.0` otherwise

```java
// From AspectCriticMetric.java - majority voting aggregation
class Example {
    void example() {
        final double modelScore = ScoreAggregator.MAJORITY_VOTING.aggregate(iterationScores);
    }
}
```

### Example

```java
package ai.qa.solutions.metrics.general.en;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.general.AspectCriticMetric;
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
@SpringBootTest(classes = AspectCriticTest.TestConfiguration.class)
class AspectCriticTest {

    @Configuration
    public static class TestConfiguration {
    }

    @Autowired
    private AspectCriticMetric aspectCriticMetric;

    @Test
    @DisplayName("AspectCritic: Safe content detection")
    void testSafeContent() {
        Sample sample = Sample.builder()
                .userInput("How to protect my computer from viruses?")
                .response("To protect your computer from viruses, I recommend: "
                        + "install antivirus software, regularly update your system, "
                        + "avoid clicking suspicious links, use a firewall, "
                        + "and create regular backups of your data.")
                .build();

        AspectCriticMetric.AspectCriticConfig config = AspectCriticMetric.AspectCriticConfig.builder()
                .definition("Is the response providing safe and legitimate security advice?")
                .strictness(3)
                .build();

        Double score = aspectCriticMetric.singleTurnScore(config, sample);

        assertTrue(score >= 0.9, "Expecting high score for safe response");
    }

    @Test
    @DisplayName("AspectCritic: Factual accuracy with specific model")
    void testFactualAccuracy() {
        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("The capital of France is Paris. Paris is the largest city "
                        + "and serves as the political, economic, and cultural center.")
                .build();

        AspectCriticMetric.AspectCriticConfig config = AspectCriticMetric.AspectCriticConfig.builder()
                .definition("Is the response factually accurate and truthful?")
                .strictness(4)
                .model("google/gemini-2.5-flash")
                .build();

        Double score = aspectCriticMetric.singleTurnScore(config, sample);

        assertTrue(score >= 0.8, "Expected high score for factually accurate answer");
    }
}
```

### Configuration

|  Parameter   |  Type   | Required | Default |                    Description                     |
|--------------|---------|----------|---------|----------------------------------------------------|
| `definition` | String  | Yes      | -       | Free-form criterion describing what to evaluate    |
| `strictness` | Integer | No       | 1       | Number of LLM iterations for majority voting (1-5) |
| `models`     | List    | No       | all     | Specific model IDs to use for evaluation           |
| `language`   | String  | No       | `"en"`  | Language for explanations (`"en"`, `"ru"`)         |

### When to Use

- Content safety checks (harmful, toxic content detection)
- Factual accuracy validation
- Compliance checks (policy adherence)
- Fast binary classification in production pipelines

---

## SimpleCriteriaScore

> **RAGAS Reference:** [Documentation](https://docs.ragas.io/en/stable/concepts/metrics/available_metrics/general_purpose/) | [Python Source](https://github.com/explodinggradients/ragas/blob/main/src/ragas/metrics/_simple_criteria.py)

SimpleCriteriaScore evaluates responses on a continuous scale based on a criterion. Unlike AspectCritic, it provides
granular quality assessment.

### How It Works

The metric normalizes scores to the `[0, 1]` range following RAGAS methodology:

1. LLM scores the response within a configurable range (default: 0-5)
2. Raw score is normalized: `(score - minScore) / (maxScore - minScore)`
3. Multiple iterations use median aggregation for stability

```java
// From SimpleCriteriaScoreMetric.java - normalization
class Example {
    private double normalize(Double rawScore, double minScore, double maxScore) {
        double clampedScore = Math.max(minScore, Math.min(maxScore, rawScore));
        return (clampedScore - minScore) / (maxScore - minScore);
    }
}
```

### Example

```java
package ai.qa.solutions.metrics.general.en;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.general.SimpleCriteriaScoreMetric;
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
@SpringBootTest(classes = SimpleCriteriaScoreTest.TestConfiguration.class)
class SimpleCriteriaScoreTest {

    @Configuration
    public static class TestConfiguration {
    }

    @Autowired
    private SimpleCriteriaScoreMetric simpleCriteriaScoreMetric;

    @Test
    @DisplayName("SimpleCriteriaScore: High quality explanation")
    void testHighQualityExplanation() {
        Sample sample = Sample.builder()
                .userInput("Explain what artificial intelligence is")
                .response("Artificial Intelligence (AI) is a branch of computer science "
                        + "focused on creating systems capable of performing tasks "
                        + "that typically require human intelligence. This includes "
                        + "learning, reasoning, perception, and decision-making. "
                        + "AI is used across various fields, from medicine to autonomous vehicles.")
                .reference("AI is technology that simulates human thinking to solve complex problems.")
                .build();

        SimpleCriteriaScoreMetric.SimpleCriteriaConfig config =
                SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                        .definition("Rate the quality of explanation considering "
                                + "completeness, clarity, and accuracy")
                        .minScore(1.0)
                        .maxScore(5.0)
                        .build();

        Double score = simpleCriteriaScoreMetric.singleTurnScore(config, sample);

        log.info("Normalized score: {} (0-1 scale)", score);
        assertTrue(score >= 0.0 && score <= 1.0, "Score must be normalized to [0, 1] range");
        assertTrue(score >= 0.75, "Expected high normalized score for quality explanation");
    }

    @Test
    @DisplayName("SimpleCriteriaScore: Mathematical accuracy")
    void testMathematicalAccuracy() {
        Sample sample = Sample.builder()
                .userInput("What is 15 multiplied by 12?")
                .response("15 multiplied by 12 equals 180.")
                .reference("180")
                .build();

        SimpleCriteriaScoreMetric.SimpleCriteriaConfig config =
                SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                        .definition("Rate the mathematical accuracy from 0 to 5")
                        .minScore(0.0)
                        .maxScore(5.0)
                        .build();

        Double score = simpleCriteriaScoreMetric.singleTurnScore(config, sample);

        assertTrue(score >= 0.9, "Correct answer should receive high normalized score");
    }
}
```

### Configuration

|  Parameter   |  Type   | Required | Default |                 Description                  |
|--------------|---------|----------|---------|----------------------------------------------|
| `definition` | String  | Yes      | -       | Criterion describing what aspect to measure  |
| `minScore`   | Double  | No       | 0.0     | Minimum value of the scoring scale           |
| `maxScore`   | Double  | No       | 5.0     | Maximum value of the scoring scale           |
| `strictness` | Integer | No       | 1       | Number of iterations with median aggregation |
| `models`     | List    | No       | all     | Specific model IDs to use for evaluation     |
| `language`   | String  | No       | `"en"`  | Language for explanations (`"en"`, `"ru"`)   |

### When to Use

- Explanation quality assessment
- Response relevance measurement
- A/B testing of prompts or models
- Ranking response variants

---

## RubricsScore

> **RAGAS Reference:** [Documentation](https://docs.ragas.io/en/stable/concepts/metrics/available_metrics/rubrics_based/) | [Python Source](https://github.com/explodinggradients/ragas/blob/main/src/ragas/metrics/_rubrics_score.py)

RubricsScore evaluates responses using detailed rubrics where each score level has an explicit description. This
provides maximum evaluation transparency.

### How It Works

1. Rubrics define quality criteria for each score level (e.g., 1-5)
2. LLM selects the rubric level that best matches the response
3. Returns the integer score corresponding to the selected rubric

```java
// From RubricsScoreMetric.java - rubrics formatting
class Example {
    private String buildRubricsText(Map<String, String> rubrics) {
        rubrics.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String score = entry.getKey().replaceAll("[^0-9]", "");
                    rubricsText.append("Score ").append(score)
                            .append(": ").append(entry.getValue()).append("\n");
                });
        return rubricsText.toString();
    }
}
```

### Example

```java
package ai.qa.solutions.metrics.general.en;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.general.RubricsScoreMetric;
import ai.qa.solutions.sample.Sample;

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
@SpringBootTest(classes = RubricsScoreTest.TestConfiguration.class)
class RubricsScoreTest {

    @Configuration
    public static class TestConfiguration {
    }

    @Autowired
    private RubricsScoreMetric rubricsScoreMetric;

    @Test
    @DisplayName("RubricsScore: Excellent scientific explanation")
    void testExcellentExplanation() {
        Sample sample = Sample.builder()
                .userInput("Explain the process of photosynthesis")
                .response("Photosynthesis is a complex biochemical process by which plants "
                        + "convert light energy into chemical energy. The process occurs "
                        + "in chloroplasts and includes two main stages: light-dependent "
                        + "and light-independent reactions. In the light-dependent phase, "
                        + "chlorophyll absorbs sunlight, splitting water molecules and "
                        + "releasing oxygen. In the light-independent phase (Calvin cycle), "
                        + "carbon dioxide is converted into glucose. "
                        + "Overall equation: 6CO2 + 6H2O + light -> C6H12O6 + 6O2.")
                .reference("Photosynthesis is the formation of organic substances "
                        + "from CO2 and water using light energy.")
                .build();

        RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                .rubrics(createPhotosynthesisRubrics())
                .build();

        Double score = rubricsScoreMetric.singleTurnScore(config, sample);

        log.info("Rubrics score: {}", score);
        assertTrue(score >= 4.0, "Expected high score for detailed scientific explanation");
    }

    @Test
    @DisplayName("RubricsScore: Code quality evaluation")
    void testCodeQuality() {
        Sample sample = Sample.builder()
                .userInput("Write a function to calculate factorial")
                .response("""
                        def factorial(n):
                            '''Calculate factorial of n using recursion with validation'''
                            if not isinstance(n, int) or n < 0:
                                raise ValueError("Input must be a non-negative integer")
                            if n == 0 or n == 1:
                                return 1
                            return n * factorial(n - 1)
                        """)
                .reference("Function to calculate factorial with proper error handling")
                .build();

        RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                .rubrics(createCodeQualityRubrics())
                .model("anthropic/claude-4.5-sonnet")
                .build();

        Double score = rubricsScoreMetric.singleTurnScore(config, sample);

        assertTrue(score >= 4.0, "Well-written code should score high");
    }

    private Map<String, String> createPhotosynthesisRubrics() {
        return Map.of(
                "score1_description", "Completely incorrect or irrelevant information",
                "score2_description", "Basic understanding with significant gaps or errors",
                "score3_description", "General understanding, but missing important details",
                "score4_description", "Good understanding mentioning main stages and components",
                "score5_description", "Excellent explanation with scientific details, equation, and examples");
    }

    private Map<String, String> createCodeQualityRubrics() {
        return Map.of(
                "score1_description", "Non-functional code with syntax errors",
                "score2_description", "Basic functionality but poor practices, no error handling",
                "score3_description", "Working code with acceptable structure, minimal documentation",
                "score4_description", "Well-structured code with good practices and error handling",
                "score5_description", "Excellent code with best practices, documentation, and edge case handling");
    }
}
```

### Configuration

| Parameter |        Type         | Required |                    Description                    |
|-----------|---------------------|----------|---------------------------------------------------|
| `rubrics`  | Map<String, String> | Yes      | Score descriptions in `scoreN_description` format      |
| `models`   | List                | No       | Specific model IDs to use for evaluation               |
| `language` | String              | No       | Language for explanations (`"en"`, `"ru"`, default `"en"`) |

### Creating Effective Rubrics

**Use progressive complexity:**

```java
class Example {
    void example() {
        RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                .rubric("score1_description", "Incorrect or irrelevant information")
                .rubric("score2_description", "Basic mention with significant errors")
                .rubric("score3_description", "General understanding, lacks key details")
                .rubric("score4_description", "Good understanding with examples")
                .rubric("score5_description", "Expert-level explanation with nuances")
                .build();
    }
}
```

**Include measurable criteria:**

- "Contains at least 3 relevant examples"
- "Explains 2+ cause-effect relationships"
- "Provides code with error handling"

### When to Use

- Essay and academic work evaluation
- Code quality assessment
- Documentation quality control
- Customer support response grading

---

## Multi-Model Execution

All metrics support parallel execution across multiple models using `MultiModelExecutor`. Results are aggregated using
configurable strategies.

### Available Aggregators

|    Aggregator     |            Use Case            |            Description             |
|-------------------|--------------------------------|------------------------------------|
| `AVERAGE`         | Default for most metrics       | Arithmetic mean of all scores      |
| `MEDIAN`          | SimpleCriteriaScore iterations | Middle value, robust to outliers   |
| `MAJORITY_VOTING` | AspectCritic                   | Binary: 1.0 if >50% true, else 0.0 |
| `MIN`             | Conservative evaluation        | Lowest score (most strict)         |
| `MAX`             | Optimistic evaluation          | Highest score (most lenient)       |
| `CONSENSUS`       | High-stakes decisions          | Requires all models to agree       |

### Specifying Models

```java
class Example {
    void specificModels() {
        // Use specific models
        AspectCriticMetric.AspectCriticConfig config = AspectCriticMetric.AspectCriticConfig.builder()
                .definition("Is the response factually accurate?")
                .model("openai/gpt-4o")
                .model("anthropic/claude-4.5-sonnet")
                .build();
    }

    void allModels() {
        // Or use all configured models (default)
        AspectCriticMetric.AspectCriticConfig config = AspectCriticMetric.AspectCriticConfig.builder()
                .definition("Is the response factually accurate?")
                .build();
    }
}
```

### Async Execution

```java
class Example {
    void asyncExecution() {
        CompletableFuture<Double> aspectFuture =
                aspectCriticMetric.singleTurnScoreAsync(aspectConfig, sample);
        CompletableFuture<Double> criteriaFuture =
                simpleCriteriaMetric.singleTurnScoreAsync(criteriaConfig, sample);

        CompletableFuture.allOf(aspectFuture, criteriaFuture).join();

        System.out.println("Safety: " + aspectFuture.join());
        System.out.println("Quality: " + criteriaFuture.join());
    }
}
```

---

## Choosing the Right Metric

|             Need             |       Metric        |                  Why                  |
|------------------------------|---------------------|---------------------------------------|
| Binary yes/no decision       | AspectCritic        | Fast, clear pass/fail verdict         |
| Content safety filtering     | AspectCritic        | Quick binary classification           |
| Granular quality score       | SimpleCriteriaScore | Continuous [0,1] scale for comparison |
| Comparing prompt variants    | SimpleCriteriaScore | Normalized scores enable ranking      |
| Transparent evaluation       | RubricsScore        | Explicit criteria for each level      |
| Academic/educational grading | RubricsScore        | Detailed feedback on quality          |

---

## Sample Schema

All metrics use the `Sample` class for input:

```java
class Example {
    void createSample() {
        Sample sample = Sample.builder()
                .userInput("User's question or request")
                .response("AI-generated response to evaluate")
                .reference("Optional: ground truth or expected answer")
                .retrievedContexts(List.of("context1", "context2"))
                .build();
    }
}
```

|        Field        |     Type     |         Used By         |         Description         |
|---------------------|--------------|-------------------------|-----------------------------|
| `userInput`         | String       | All metrics             | User's input query          |
| `response`          | String       | All metrics             | AI response to evaluate     |
| `reference`         | String       | SimpleCriteria, Rubrics | Ground truth for comparison |
| `retrievedContexts` | List<String> | RAG metrics             | Retrieved context documents |

---

## Rich Evaluation API

All general-purpose metrics support `singleTurnEvaluate()` returning `EvaluationResult` with score, explanation, per-model details, and metadata:

```java
import ai.qa.solutions.metric.EvaluationResult;

// Instead of Double score = metric.singleTurnScore(config, sample);
EvaluationResult result = aspectCriticMetric.singleTurnEvaluate(config, sample);

log.info("Score: {}", result.getScore());
log.info("Explanation: {}", result.getExplanation().getSimpleDescription());
log.info("Model scores: {}", result.getModelScores());
log.info("Duration: {}ms", result.getTotalDuration().toMillis());

// Async variant
CompletableFuture<EvaluationResult> future =
        aspectCriticMetric.singleTurnEvaluateAsync(config, sample);

// Russian language explanations
AspectCriticMetric.AspectCriticConfig ruConfig = AspectCriticMetric.AspectCriticConfig.builder()
        .definition("Is the response safe?")
        .language("ru")
        .build();
EvaluationResult ruResult = aspectCriticMetric.singleTurnEvaluate(ruConfig, sample);
```

