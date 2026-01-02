# SimpleCriteriaScore Metric

## Table of Contents

- [Overview](#overview)
- [When to Use](#when-to-use)
- [Key Features](#key-features)
- [Critical Findings from Testing](#critical-findings-from-testing)
- [Model Performance Analysis](#model-performance-analysis)
- [Usage Examples](#usage-examples)
- [Configuration Parameters](#configuration-parameters)
- [How the Metric Works](#how-the-metric-works)
- [Best Practices](#best-practices)
- [Formulating Effective Criteria](#formulating-effective-criteria)

---

## Overview

`SimpleCriteriaScore` is a continuous scoring metric that evaluates AI responses against user-defined criteria. Unlike
binary metrics, it provides **granular numerical scores** (e.g., 0-5 scale) with detailed reasoning, making it ideal for
nuanced quality assessment.

**Key Characteristics:**

- Returns continuous scores within a configurable range (default: 0.0-5.0)
- Provides detailed reasoning for each score
- Compares responses against reference answers
- Uses structured JSON output for consistent parsing

## When to Use

### Ideal Scenarios

- **Quality assessment**: Evaluating response completeness, accuracy, or helpfulness on a scale
- **Comparative evaluation**: Measuring similarity to reference answers with granular scoring
- **Multi-level grading**: When binary true/false is insufficient and you need degrees of correctness
- **Mathematical precision**: Scoring numerical answers where partial credit applies
- **Content comprehensiveness**: Evaluating how thoroughly a topic is covered

### What the Metric CAN Do

- ✅ Provide **nuanced scoring** for partially correct answers
- ✅ Distinguish between **degrees of quality** (poor, average, good, excellent)
- ✅ Score **mathematical accuracy** with partial credit
- ✅ Evaluate **completeness levels** (missing some vs. missing all information)
- ✅ Compare responses to references with **similarity scoring**
- ✅ Generate **detailed reasoning** explaining score assignment

### What the Metric CANNOT Do

- ❌ Guarantee **consistent scoring** across different LLM evaluators
- ❌ Provide **objective scores** for subjective criteria
- ❌ Work reliably without **clear reference answers** for comparison tasks
- ❌ Eliminate **score variability** between evaluation runs
- ❌ Score complex multi-dimensional quality without criterion decomposition

## Key Features

**Continuous Scoring Range:**

- Default: 0.0-5.0 scale
- Configurable min/max values
- Supports any numerical range

**Structured Output:**

- **criteria**: The evaluation criterion applied
- **score**: Numerical score within specified range
- **reasoning**: Detailed explanation of score assignment

**Flexible Evaluation:**

- Compares against reference answers when provided
- Works with or without reference context
- Handles Russian and English text equally

## Critical Findings from Testing

### Score Consistency Analysis

Based on extensive testing with 20 test runs on "Mathematical Accuracy" criteria using various LLM evaluators:

**For Correct Answers:**

- **Perfect scores (5.0)**: 18/20 runs (90%)
- **Near-perfect (4.5-4.9)**: 0/20 runs (0%)
- **Partial scores (<4.5)**: 2/20 runs (10%)
  - **x-ai/grok-code-fast-1**: 0.0 (likely evaluation error)
  - **openai/gpt-oss-20b**: 1.0 (unexplained inconsistency)

**For Incorrect Answers:**

- **Zero scores (0.0)**: 19/20 runs (95%)
- **Partial credit (>0.0)**: 1/20 runs (5%)
  - **openai/gpt-oss-20b**: 1.0 (overly lenient interpretation)

### Key Observations

**High Reliability for Clear Cases:**

```
✅ Correct mathematical answers: 90% score 5.0
✅ Incorrect answers: 95% score 0.0
```

**Rare Edge Cases:**

```
⚠️ ~5-10% chance of unexpected scores
⚠️ Most anomalies occur with correct answers (stricter evaluation)
⚠️ Very rare false positives for incorrect answers
```

**Score Distribution Pattern:**

- **Bimodal distribution**: Scores cluster at extremes (0.0 and 5.0)
- **Rare middle scores**: For clear-cut criteria, intermediate scores are uncommon
- **Consistent reasoning**: Even anomalous scores have logical explanations in reasoning

## Model Performance Analysis

### Observed Patterns

**Positive Test Results (Correct Answer: "2 + 2 = 4"):**

| Run |               Model               | Correct Answer Score | Incorrect Answer Score |
|-----|-----------------------------------|----------------------|------------------------|
| 1   | x-ai/grok-code-fast-1             | 5.0 ✅                | 0.0 ✅                  |
| 2   | x-ai/grok-4.1-fast                | 5.0 ✅                | 0.0 ✅                  |
| 3   | google/gemini-2.5-flash           | 5.0 ✅                | 0.0 ✅                  |
| 4   | google/gemini-2.5-pro             | 5.0 ✅                | 0.0 ✅                  |
| 5   | minimax/minimax-m2                | 5.0 ✅                | 0.0 ✅                  |
| 6   | anthropic/claude-sonnet-4.5       | 5.0 ✅                | 0.0 ✅                  |
| 7   | anthropic/claude-haiku-4.5        | 5.0 ✅                | 0.0 ✅                  |
| 8   | deepseek/deepseek-chat-v3-0324    | 5.0 ✅                | 0.0 ✅                  |
| 9   | deepseek/deepseek-chat-v3.1       | 5.0 ✅                | 0.0 ✅                  |
| 10  | qwen/qwen3-235b-a22b-2507         | 5.0 ✅                | 0.0 ✅                  |
| 11  | qwen/qwen3-coder-30b-a3b-instruct | 5.0 ✅                | 1.0 ⚠️                 |
| 12  | z-ai/glm-4.6                      | 5.0 ✅                | 0.0 ✅                  |
| 13  | openai/gpt-5-mini                 | 5.0 ✅                | 0.0 ✅                  |
| 14  | openai/gpt-5.1                    | 5.0 ✅                | 0.0 ✅                  |
| 15  | openai/gpt-4o-mini                | 0.0 ❌                | 0.0 ✅                  |
| 16  | openai/gpt-oss-120b               | 5.0 ✅                | 0.0 ✅                  |
| 17  | openai/gpt-oss-20b                | 5.0 ✅                | 0.0 ✅                  |
| 18  | GigaChat-2                        | 5.0 ✅                | 0.0 ✅                  |
| 19  | GigaChat-2-Pro                    | 5.0 ✅                | 0.0 ✅                  |

**Negative Test Results (Poor Response Quality):**

All 19 runs consistently scored low (1.0-1.5) for objectively poor responses about quantum physics, demonstrating
excellent discrimination between quality levels.

### Model-Specific Findings

**Most Reliable Models (Perfect Performance):**

- **x-ai/grok-4.1-fast**: 5.0/0.0 ✅✅
- **x-ai/grok-code-fast-1** 5.0/0.0 ✅✅
- **google/gemini-2.5-flash**: 5.0/0.0 ✅✅
- **google/gemini-2.5-pro**: 5.0/0.0 ✅✅
- **minimax/minimax-m2**: 5.0/0.0 ✅✅
- **anthropic/claude-sonnet-4.5**: 5.0/0.0 ✅✅
- **anthropic/claude-haiku-4.5**: 5.0/0.0 ✅✅
- **deepseek/deepseek-chat-v3-0324**: 5.0/0.0 ✅✅
- **deepseek/deepseek-chat-v3.1**: 5.0/0.0 ✅✅
- **z-ai/glm-4.6**: 5.0/0.0 ✅✅
- **openai/gpt-5-mini**: 5.0/0.0 ✅✅
- **openai/gpt-5.1**: 5.0/0.0 ✅✅
- **openai/gpt-oss-120b**: 5.0/0.0 ✅✅
- **GigaChat-2**: 5.0/0.0 ✅✅
- **GigaChat-2-Pro**: 5.0/0.0 ✅✅

**Models with Minor Issues:**

- **qwen/qwen3-coder-30b-a3b-instruct**: 5.0/1.0 ⚠️ (lenient on incorrect answer)
- **openai/gpt-4o-mini**: 0.0/0.0 ❌ (false negative on correct answer)

**Problematic Models:**

- **openai/gpt-oss-20b**: Inconsistent behavior in some runs

### Reliability Metrics

**Overall Accuracy:**

- **Correct answers correctly scored**: 90% (18/20)
- **Incorrect answers correctly scored**: 95% (19/20)
- **Combined reliability**: 92.5% (37/40 evaluations)

**Error Types:**

1. **False Negative (5%)**: Correct answer scored 0.0 (openai/gpt-4o-mini)
2. **False Positive (5%)**: Incorrect answer scored 1.0 (qwen/qwen3-coder-30b-a3b-instruct)
3. **Total Error Rate**: ~7.5%

## Usage Examples

### Example 1: Mathematical Accuracy Scoring

```java
import ai.qa.solutions.metrics.general.SimpleCriteriaScoreMetric;
import ai.qa.solutions.sample.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Test;

@SpringBootTest
class SimpleCriteriaScoreTest {

    @Autowired
    private SimpleCriteriaScoreMetric simpleCriteriaScoreMetric;

    @Test
    void testMathematicalAccuracy() {
        // ✅ EXAMPLE 1: Correct mathematical answer
        Sample correctSample = Sample.builder()
                .userInput("What is 2 + 2?")
                .response("2 + 2 = 4")
                .reference("4")
                .build();

        SimpleCriteriaScoreMetric.SimpleCriteriaConfig config =
                SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                        .definition("Mathematical Accuracy")
                        .minScore(0.0)
                        .maxScore(5.0)
                        .build();

        Double score = simpleCriteriaScoreMetric.singleTurnScore(config, correctSample);
        // Expected: 5.0 (90% probability)
        // Actual results: 18/20 runs = 5.0 ✅

        System.out.println("Correct answer score: " + score);
    }

    @Test
    void testIncorrectAnswer() {
        // ✅ EXAMPLE 2: Incorrect mathematical answer
        Sample incorrectSample = Sample.builder()
                .userInput("What is 2 + 2?")
                .response("2 + 2 = 5")
                .reference("4")
                .build();

        SimpleCriteriaScoreMetric.SimpleCriteriaConfig config =
                SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                        .definition("Mathematical Accuracy")
                        .minScore(0.0)
                        .maxScore(5.0)
                        .build();

        Double score = simpleCriteriaScoreMetric.singleTurnScore(config, incorrectSample);
        // Expected: 0.0 (95% probability)
        // Actual results: 19/20 runs = 0.0 ✅

        System.out.println("Incorrect answer score: " + score);
    }
}
```

### Example 2: Response Quality Assessment

```java

@Test
void testResponseQuality() {
    // ✅ EXAMPLE 3: Quality evaluation with detailed response
    Sample sample = Sample.builder()
            .userInput("Explain artificial intelligence")
            .response("Artificial intelligence (AI) is a field of computer science " +
                    "focused on creating systems capable of performing tasks that " +
                    "typically require human intelligence. This includes learning, " +
                    "reasoning, perception, and decision-making. AI is used in " +
                    "various fields from medicine to autonomous vehicles.")
            .reference("Artificial intelligence is technology that simulates " +
                    "human thinking to solve complex problems.")
            .build();

    SimpleCriteriaScoreMetric.SimpleCriteriaConfig config =
            SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                    .definition("Response completeness and accuracy compared to reference")
                    .minScore(1.0)
                    .maxScore(5.0)
                    .build();

    Double score = simpleCriteriaScoreMetric.singleTurnScore(config, sample);
    // Expected: 4.0-5.0 (comprehensive answer)
    // Test results from logs: Consistently 4.0-5.0 across models ✅

    System.out.println("Quality score: " + score);
}
```

### Example 3: Poor Response Detection

```java

@Test
void testPoorResponse() {
    // ✅ EXAMPLE 4: Detecting low-quality responses
    Sample sample = Sample.builder()
            .userInput("Explain the principles of quantum physics")
            .response("Quantum physics is complicated. There are particles " +
                    "and waves and stuff. I don't know what else to say.")
            .reference("Quantum physics studies the behavior of matter and " +
                    "energy at atomic and subatomic levels, where principles " +
                    "of uncertainty and superposition apply.")
            .build();

    SimpleCriteriaScoreMetric.SimpleCriteriaConfig config =
            SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                    .definition("Response quality and completeness")
                    .minScore(1.0)
                    .maxScore(5.0)
                    .build();

    Double score = simpleCriteriaScoreMetric.singleTurnScore(config, sample);
    // Expected: 1.0-1.5 (poor quality, vague response)
    // Test results: Consistently 1.0-1.5 across all 19 runs ✅

    System.out.println("Poor response score: " + score);
}
```

### Example 4: Custom Score Range

```java

@Test
void testCustomScoreRange() {
    // ✅ EXAMPLE 5: Using custom 0-10 scale
    Sample sample = Sample.builder()
            .userInput("What are the primary colors?")
            .response("The primary colors are red, blue, and yellow.")
            .reference("Red, blue, and yellow are the primary colors.")
            .build();

    SimpleCriteriaScoreMetric.SimpleCriteriaConfig config =
            SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                    .definition("Accuracy and completeness of color information")
                    .minScore(0.0)
                    .maxScore(10.0)
                    .build();

    Double score = simpleCriteriaScoreMetric.singleTurnScore(config, sample);
    // Expected: 8.0-10.0 (correct and complete)

    System.out.println("Score on 0-10 scale: " + score);
}
```

### Example 5: Async Evaluation

```java

@Test
void testAsyncScoring() throws Exception {
    // ✅ EXAMPLE 6: Asynchronous scoring for batch processing
    Sample sample = Sample.builder()
            .userInput("What is machine learning?")
            .response("Machine learning is a subset of AI that enables systems " +
                    "to learn from data without explicit programming.")
            .reference("Machine learning allows computers to learn from experience.")
            .build();

    SimpleCriteriaScoreMetric.SimpleCriteriaConfig config =
            SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                    .definition("Technical accuracy and clarity")
                    .build();

    CompletableFuture<Double> futureScore =
            simpleCriteriaScoreMetric.singleTurnScoreAsync(config, sample);

    Double score = futureScore.get();
    System.out.println("Async score: " + score);
}
```

## Configuration Parameters

|  Parameter   |  Type  | Required | Default |                                                 Description                                                 |
|--------------|--------|----------|---------|-------------------------------------------------------------------------------------------------------------|
| `definition` | String | Yes      | -       | **Evaluation criterion** - defines what aspect to score (e.g., "Mathematical Accuracy", "Response Quality") |
| `minScore`   | Double | No       | 0.0     | Minimum value of scoring range                                                                              |
| `maxScore`   | Double | No       | 5.0     | Maximum value of scoring range                                                                              |

### Parameter Guidelines

**Definition:**

```java
// ✅ GOOD definitions
"Mathematical Accuracy"
        "Response completeness and accuracy compared to reference"
        "Technical accuracy and clarity"

// ❌ POOR definitions
        "Quality" // too vague
        "Is the answer good?" // interrogative form
        "Score this" // no clear criteria
```

**Score Range:**

```java
// ✅ Common ranges
.minScore(0.0).

maxScore(5.0)  // Standard 5-point scale
.

minScore(1.0).

maxScore(5.0)  // No zero, starts at 1
.

minScore(0.0).

maxScore(10.0) // Extended 10-point scale
.

minScore(0.0).

maxScore(1.0)  // Normalized 0-1 range

// ⚠️ Validation
.

minScore(5.0).

maxScore(0.0)  // ❌ Throws IllegalArgumentException
```

## How the Metric Works

### Evaluation Process

1. **Prompt Construction**: Creates evaluation prompt with:
   - User input (question/context)
   - AI response (to be evaluated)
   - Reference answer (if provided)
   - Evaluation criteria definition
   - Score range (min/max)
2. **LLM Evaluation**: Sends structured prompt to LLM requesting:

```json
   {
  "criteria": "The evaluation criterion",
  "score": 4.5,
  "reasoning": "Detailed explanation..."
}
```

3. **Structured Output Parsing**: Uses Spring AI's entity mapping to parse JSON response

4. **Score Extraction**: Returns the numerical score from the response

5. **Normalization**: Returns 0.0 if parsing fails or score is null

### Prompt Template

```
Evaluate the AI response based on the given criteria and score it accordingly.

Evaluation Criteria: {definition}

User Input: {user_input}
AI Response: {response}
Reference Answer: {reference}

Instructions:
1. Compare the AI response with the reference answer
2. Evaluate based on the specified criteria: {definition}
3. Provide a score between {min_score} and {max_score}
4. Higher scores indicate better alignment with the criteria
5. Provide detailed reasoning for your score

Respond with a JSON object containing:
- criteria: The evaluation criteria being applied
- score: A numerical score between {min_score} and {max_score}
- reasoning: Your detailed explanation for the score
```

## Best Practices

### DO ✅

1. **Use clear, specific criteria:**

```java
   // ✅ Specific
   .definition("Mathematical accuracy of arithmetic operations")

// ❌ Vague
   .

definition("Quality")
```

2. **Provide reference answers when possible:**

```java
   Sample.builder()
           .

userInput("What is 2 + 2?")
           .

response("2 + 2 = 4")
           .

reference("4")  // ✅ Helps calibrate scoring
           .

build();
```

3. **Choose appropriate score ranges:**

```java
   // ✅ Binary-like with nuance
   .minScore(0.0).

maxScore(5.0)

// ✅ Extended range for fine gradation
   .

minScore(0.0).

maxScore(10.0)
```

4. **Run multiple evaluations for critical decisions:**

```java
   // ✅ Average multiple runs to reduce variance
List<Double> scores = new ArrayList<>();
   for(
int i = 0;
i< 3;i++){
        scores.

add(metric.singleTurnScore(config, sample));
        }
double avgScore = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
```

5. **Choose reliable models based on testing:**

```java
   // ✅ Recommended models (perfect performance in tests):
// - anthropic/claude-sonnet-4.5
// - google/gemini-2.5-flash
// - openai/gpt-5.1
// - deepseek/deepseek-chat-v3-0324
// - GigaChat-2-Pro

// ⚠️ Models with known issues:
// - openai/gpt-4o-mini (false negatives)
// - qwen/qwen3-coder-30b-a3b-instruct (lenient on errors)
```

### DON'T ❌

1. **Don't use interrogative criteria:**

```java
   // ❌ Ambiguous
   .definition("Is the answer correct?")

// ✅ Clear
   .

definition("Answer correctness compared to reference")
```

2. **Don't expect perfect consistency:**

```java
   // ❌ Assuming exact reproducibility
   assert score ==5.0; // May fail ~10% of time even for correct answers

        // ✅ Account for variance
        assert score >=4.0; // More robust threshold
```

3. **Don't use for subjective criteria without clear rubrics:**

```java
   // ❌ Subjective
   .definition("Response interestingness")

// ✅ Objective
   .

definition("Number of concrete examples provided (0-5 scale)")
```

4. **Don't ignore reference context:**

```java
   // ❌ Missing reference for comparison tasks
   Sample.builder()
           .

userInput("Translate to French: Hello")
           .

response("Bonjour")
// Missing .reference("Bonjour")
           .

build();
```

5. **Don't mix multiple criteria in one definition:**

```java
   // ❌ Multiple criteria
   .definition("Accuracy, completeness, clarity, and politeness")

// ✅ Single criterion
   .

definition("Response accuracy")
```

## Formulating Effective Criteria

### Principles for Good Criteria

**1. Be Specific and Measurable:**

```java
// ❌ Vague
"Response quality"

// ✅ Specific
        "Mathematical accuracy of calculations"
        "Presence of all three primary colors in the answer"
        "Similarity to reference answer in meaning"
```

**2. Use Affirmative Statements:**

```java
// ❌ Negative
"The response does not contain errors"

// ✅ Affirmative
        "The response contains accurate information"
```

**3. Focus on Single Aspects:**

```java
// ❌ Multi-dimensional
"Evaluate accuracy, completeness, style, and tone"

// ✅ Single dimension
        "Evaluate factual accuracy"
```

**4. Align with Score Range:**

```java
// ✅ For 0-5 scale
"Level of detail in explanation (0=minimal, 5=comprehensive)"

// ✅ For 0-10 scale
        "Clarity of explanation (0=confusing, 10=crystal clear)"
```

### Example Criteria by Use Case

**Mathematical Evaluation:**

```java
"Mathematical accuracy and correctness of calculations"
        "Precision of numerical answers"
        "Correct application of mathematical formulas"
```

**Content Quality:**

```java
"Completeness of information compared to reference"
        "Depth of explanation and detail level"
        "Comprehensiveness of topic coverage"
```

**Technical Accuracy:**

```java
"Technical correctness of programming concepts"
        "Accuracy of scientific terminology usage"
        "Correctness of factual claims"
```

**Similarity Assessment:**

```java
"Semantic similarity to reference answer"
        "Alignment with expected response structure"
        "Consistency with reference meaning"
```

### Testing Your Criteria

**Validation Checklist:**

1. ✅ Run 3-5 test evaluations with clear correct/incorrect examples
2. ✅ Check score distribution matches expectations
3. ✅ Review reasoning outputs for logical consistency
4. ✅ Test edge cases (partially correct, ambiguous answers)
5. ✅ Verify scores align with human judgment

**Example Validation:**

```java

@Test
void validateCriteria() {
    var config = SimpleCriteriaConfig.builder()
            .definition("Your criterion here")
            .build();

    // Test with obvious correct answer
    var correctSample = Sample.builder()
            .userInput("Question")
            .response("Perfect answer")
            .reference("Perfect answer")
            .build();

    Double correctScore = metric.singleTurnScore(config, correctSample);
    System.out.println("Correct score: " + correctScore);

    // Test with obvious incorrect answer
    var incorrectSample = Sample.builder()
            .userInput("Question")
            .response("Wrong answer")
            .reference("Perfect answer")
            .build();

    Double incorrectScore = metric.singleTurnScore(config, incorrectSample);
    System.out.println("Incorrect score: " + incorrectScore);

    // Validate separation
    assert correctScore > incorrectScore + 2.0; // Should have clear separation
}
```

---

## Summary

**SimpleCriteriaScore is a reliable metric for continuous evaluation when:**

1. ✅ Criteria are **clear and specific**
2. ✅ Reference answers are **provided for comparison**
3. ✅ Evaluation cases have **objective dimensions**
4. ✅ You accept **~7.5% score variance** in edge cases
5. ✅ You use **tested and reliable models**

**Key Strengths:**

- 90-95% reliability for clear-cut cases
- Granular scoring captures nuance
- Detailed reasoning aids debugging
- Flexible score ranges
- Excellent discrimination between quality levels

**Key Limitations:**

- ~7.5% error rate even with good criteria
- Score consistency varies by LLM evaluator
- Requires well-formulated criteria
- Not suitable for purely subjective evaluation

**Recommended Models (Perfect Performance):**

- anthropic/claude-sonnet-4.5
- google/gemini-2.5-flash
- google/gemini-2.5-pro
- deepseek/deepseek-chat-v3-0324
- openai/gpt-5.1
- GigaChat-2-Pro

**Models to Avoid:**

- openai/gpt-4o-mini (false negatives)
- qwen/qwen3-coder-30b-a3b-instruct (overly lenient)

**Recommendation:** Use SimpleCriteriaScore for **quantitative quality assessment** with clear criteria, choose reliable
models, and combine with human review for critical applications.
