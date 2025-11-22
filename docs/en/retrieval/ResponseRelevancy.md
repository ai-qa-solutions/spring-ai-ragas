# ResponseRelevancy

## Table of Contents

- [Overview](#overview)
- [When to Apply](#when-to-apply)
  - [Ideal Scenarios](#ideal-scenarios)
  - [What This Metric CAN Reliably Do](#what-this-metric-can-reliably-do)
  - [What This Metric CANNOT Reliably Do](#what-this-metric-cannot-reliably-do)
- [Critical Limitations](#critical-limitations)
  - [Proven Issues](#proven-issues)
  - [Why This Happens](#why-this-happens)
  - [Score Distribution](#score-distribution)
- [Strong Recommendations](#strong-recommendations)
- [Usage Example](#usage-example)
- [Configuration Parameters](#configuration-parameters)
- [How the Metric Works](#how-the-metric-works)
- [Result Interpretation](#result-interpretation)
  - [Reliable Interpretations](#reliable-interpretations)
  - [Unreliable Interpretations](#unreliable-interpretations)
- [Example Scenarios](#example-scenarios)
- [Technical Details](#technical-details)
  - [Choosing the Embedding Model](#choosing-the-embedding-model)
  - [Model Comparison by Scenarios](#model-comparison-by-scenarios)
- [Troubleshooting](#troubleshooting)
- [References](#references)

---

## Overview

`ResponseRelevancy` measures how relevant a system's response is to the user's input based on
**linguistic similarity of embeddings**. This metric uses LLM-generated questions and cosine
similarity to evaluate response relevance.

⚠️ **IMPORTANT**:
This metric has significant limitations for edge cases. It should be used as a **screening tool only**
before expensive and time-consuming metrics, not for final decision-making. Always combine with other
metrics like Answer Correctness and Faithfulness.

## When to Apply

### Ideal Scenarios

- **Initial quality screening**: Quick first-pass filtering of obviously bad responses
- **Detecting noncommittal answers**: Reliably identifies "I don't know" type responses (score = 0.0)
- **Comparative evaluation**: Comparing relative quality of multiple responses to same question
- **Perfect match verification**: Confirming ideal responses (score 0.90+)

### What This Metric CAN Reliably Do

- ✅ Detect noncommittal/evasive answers → Returns 0.0
- ✅ Identify perfect direct answers → Returns 0.95-0.98 (depending on model)
- ✅ Compare complete vs incomplete answers (relative scoring works on most models)
- ✅ Work without reference answers (reference-free)
- ✅ Support multiple languages

### What This Metric CANNOT Reliably Do

- ❌ Detect partial answers to multi-part questions (scores 0.75-0.97 instead of expected ~0.5)
- ❌ Distinguish different aspects of same topic (e.g., "capital" vs "currency" of France - impossible to distinguish)
- ❌ Identify off-topic answers with similar linguistic patterns (scores 0.43-0.63)
- ❌ Recognize answers from completely different domains (e.g., programming vs cooking - scores 0.32-0.52)
- ❌ Validate factual correctness (incorrect but on-topic answers score identically to correct ones: 0.94-0.98)
- ❌ Reliably handle single-word nonsense (results heavily depend on model)

## Critical Limitations

⚠️ **The metric uses cosine similarity of embeddings, which measures LINGUISTIC PATTERNS,
not semantic relevance or correctness.**

### Proven Issues

**Proven Issues (from testing on different embedding models):**

|               Test Case               | Expected Score |        Actual Score Range        |        Best Model         |
|---------------------------------------|----------------|----------------------------------|---------------------------|
| Partial answer ("who" but not "when") | 0.50-0.70      | 0.75-0.97                        | text-embed-3-large (0.75) |
| Incorrect fact but on-topic           | should differ  | 0.94-0.98 (identical to correct) | -                         |
| Completely off-topic                  | 0.00-0.20      | 0.51-0.63                        | BGE-M3 (0.51)             |
| Different domains (code vs cooking)   | 0.00-0.20      | 0.32-0.52                        | Qwen-8B (0.32)            |
| Redundant information                 | 0.60-0.75      | 0.85-0.96                        | e5-large (0.85)           |

**🔍 Key Findings from Testing:**

1. **Incorrect facts are not detected**: "Capital of France is Lyon" scores 0.94-0.98, identical to correct answer "
   Paris" (0.95-0.98)
2. **Huge variation between models**: For off-topic answers, difference reaches 2x (0.32 vs 0.63)
3. **Some models excel in specific areas**: text-embed-3-small best for off-topic (0.63), Qwen-8B for domain shift (
   0.32)
4. **Short answers penalized unpredictably**: "Paris." scores 0.89-0.97 depending on model

### Why This Happens

- LLM generates "reasonable" questions even from irrelevant content
- Different embedding models have varying sensitivity to linguistic structures
- Cosine similarity cannot distinguish correct from incorrect facts with similar wording
- The metric cannot distinguish meaning from linguistic structure

### Score Distribution

**Score Distribution in Practice (depends on model):**

**For recommended models (text-embed-3-large, BGE-M3, text-embed-3-small):**

- **0.00-0.10**: Only noncommittal answers
- **0.32-0.63**: Off-topic/irrelevant answers
- **0.85-0.98**: Everything else - both good, bad, and incomplete answers

## Strong Recommendations

🚨 **Critical Guidelines:**

1. ❌ **NEVER** use as sole evaluation metric
2. ❌ **NEVER** trust scores above 0.5 for determining answer quality
3. ❌ **NEVER** rely on the metric to determine factual correctness
4. ⚠️ **MUST** test chosen embedding model on your data before production use
5. ✅ **ALWAYS** combine with:
   - **Answer Correctness** (with ground truth) - REQUIRED for fact checking
   - **Faithfulness** - detects hallucinations
   - **Context Precision/Recall** - for RAG pipelines
6. ✅ Use **only** to filter out obviously noncommittal answers (score 0.0)
7. ✅ For agent testing: Implement custom validation logic

## Usage Example

```java
import ai.qa.solutions.metrics.retrieval.ResponseRelevancyMetric;
import ai.qa.solutions.sample.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Test;

@SpringBootTest
class ResponseRelevancyTest {

    @Autowired
    private ResponseRelevancyMetric responseRelevancyMetric;

    @Test
    void testPerfectAnswer() {
        // Example 1: Perfect answer - RELIABLE CASE ✅
        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("The capital of France is Paris.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);
        // Result: 0.95-0.98 (depends on embedding model) ✅ RELIABLE

        System.out.println("Score: " + score);
    }

    @Test
    void testNoncommittalAnswer() {
        // Example 2: Noncommittal answer - RELIABLE CASE ✅
        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("I don't know what the capital of France is.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);
        // Result: 0.0 ✅ RELIABLE

        System.out.println("Score: " + score);
    }

    @Test
    void testPartialAnswer() {
        // Example 3: Partial answer - UNRELIABLE CASE ⚠️
        Sample sample = Sample.builder()
                .userInput("Who discovered penicillin and when?")
                .response("Alexander Fleming discovered penicillin.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);
        // Result: 0.75-0.97 ⚠️ UNRELIABLE - missing "when" but score high!
        // text-embed-3-large: 0.75, BGE-M3: 0.97, e5-large: 0.97
        // The metric CANNOT detect missing information!
        // Solution: Use Answer Correctness with ground truth

        System.out.println("Score: " + score);
    }

    @Test
    void testIncorrectFact() {
        // Example 4: Incorrect fact - CRITICAL ISSUE 🚨
        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("The capital of France is Lyon.") // WRONG!
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);
        // Result: 0.94-0.98 🚨 IDENTICAL TO CORRECT ANSWER!
        // The metric DOES NOT CHECK factual correctness!
        // Solution: MUST use Answer Correctness with ground truth

        System.out.println("Score: " + score);
    }

    @Test
    void testCompleteVsIncomplete() {
        // Example 5: Complete vs Incomplete - RELATIVELY RELIABLE comparison ✅
        Sample completeSample = Sample.builder()
                .userInput("Where is France located and what is its capital?")
                .response("France is located in Western Europe, and its capital is Paris.")
                .build();

        Sample incompleteSample = Sample.builder()
                .userInput("Where is France located and what is its capital?")
                .response("France is located in Western Europe.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double completeScore = responseRelevancyMetric.singleTurnScore(config, completeSample);
        Double incompleteScore = responseRelevancyMetric.singleTurnScore(config, incompleteSample);
        // Complete: 0.93-0.97, Incomplete: 0.91-0.96
        // Relative comparison works on most models ✅
        // But difference can be minimal!

        System.out.println("Complete: " + completeScore);
        System.out.println("Incomplete: " + incompleteScore);
    }
}
```

## Configuration Parameters

|      Parameter      | Type | Required | Default |                                         Description                                          |
|---------------------|------|----------|---------|----------------------------------------------------------------------------------------------|
| `numberOfQuestions` | int  | No       | 3       | Number of questions to generate from answer (1-10). More questions = more stable but slower. |

**Custom Configuration Example:**

```java
// Custom configuration (5 questions for more stable results)
ResponseRelevancyConfig customConfig = ResponseRelevancyConfig.builder()
                .numberOfQuestions(5)
                .build();

Double score = responseRelevancyMetric.singleTurnScore(customConfig, sample);
```

## How the Metric Works

1. **Question Generation**: LLM generates N artificial questions based on the response (with original question context)
2. **Noncommittal Detection**: For each generated question, determines if the answer is evasive/vague
3. **Early Exit**: If all questions indicate noncommittal answer, returns score 0.0
4. **Embedding Computation**: Gets vector representations of original question and generated questions
5. **Similarity Calculation**: Computes cosine similarity between original question embedding and each generated
   question embedding
6. **Aggregation**: Returns average of all similarity scores as final relevance score

**Key Assumption (Often Incorrect for Edge Cases):**

- ✅ Works: If answer is relevant, generated questions will be semantically similar to original
- ⚠️ Fails: LLM generates "reasonable" questions even from nonsense or off-topic answers
- ⚠️ Fails: Similar linguistic structure ≠ semantic relevance
- 🚨 Fails: Metric cannot distinguish correct from incorrect facts

## Result Interpretation

### Reliable Interpretations

- **0.0**: ✅ Noncommittal answer - "I don't know" - THE ONLY fully reliable case

### Unreliable Interpretations

**⚠️ ALL other interpretations are unreliable and require additional verification:**

- **0.90-0.98**: Could be anything:
  - ✅ Perfect correct answer (0.95-0.98)
  - ❌ Perfect INCORRECT answer (0.94-0.98) - INDISTINGUISHABLE!
  - ⚠️ Partial answer (0.91-0.97 on some models)
  - ⚠️ Detailed answer (0.96-0.98)
- **0.70-0.90**: Could be anything:
  - ⚠️ Short but correct answer (0.89-0.97)
  - ⚠️ Partial answer (0.75-0.93 on some models)
  - ❌ Redundant information (0.85-0.96)
  - ❌ Off-topic answer (0.51-0.63 on bad models)
- **0.30-0.70**: Could be anything:
  - ❌ Completely off-topic (0.51-0.63)
  - ❌ Different domains (0.32-0.52)
  - ⚠️ Redundant information (0.85-0.94 on some models)
- **0.00-0.30**: Could be:
  - ✅ Noncommittal answer (0.0) - reliable
  - ❌ Nonsense word (0.25-0.53 on good models)

**🎯 Practical Takeaway**: The only thing reliably detectable is noncommittal answers (score = 0.0).
Everything else requires verification with other metrics.

## Example Scenarios

**✅ Scenario 1: Perfect answer (RELATIVELY RELIABLE)**

```
Question: "What is artificial intelligence?"
Answer: "Artificial intelligence is a branch of computer science that creates systems 
         capable of performing tasks requiring human intelligence..."
Scores by model:
  • text-embed-3-large: 0.98
  • text-embed-3-small: 0.98
  • BGE-M3: 0.98
  • Qwen-8B: 0.97
  • e5-large: 0.97
Interpretation: High scores across all adequate models
```

**✅ Scenario 2: Noncommittal answer (FULLY RELIABLE)**

```
Question: "When was the light bulb invented?"
Answer: "I'm not sure when the light bulb was invented."
Score: 0.0 on all models ✅
Interpretation: RELIABLE - the only fully reliable case
```

**🚨 Scenario 3: Incorrect fact (CRITICAL ISSUE)**

```
Question: "What is the capital of France?"
Correct answer: "The capital of France is Paris."
Incorrect answer: "The capital of France is Lyon."

Scores IDENTICAL:
  • text-embed-3-large: 0.98 for both
  • text-embed-3-small: 0.98 for both
  • BGE-M3: 0.95 for both
  • Qwen-8B: 0.96 for both
  • e5-large: 0.96 for both

🚨 CRITICAL: Metric CANNOT distinguish correct from incorrect answer!
Solution: MUST use Answer Correctness
```

**⚠️ Scenario 4: Partial answer (HEAVILY DEPENDS ON MODEL)**

```
Question: "Who discovered penicillin and when?"
Incomplete answer: "Alexander Fleming discovered penicillin."

Scores vary dramatically:
  • text-embed-3-large: 0.75 ⚠️ (relatively good detection)
  • text-embed-3-small: 0.78 ⚠️ (relatively good)
  • text-embed-3-small: 0.93 ⚠️ (failed to detect)
  • Qwen-8B: 0.97 🚨 (completely failed)
  • BGE-M3: 0.97 🚨 (completely failed)
  • e5-large: 0.97 🚨 (completely failed)

Interpretation: Results unpredictable, need ground truth verification
```

**⚠️ Scenario 5: Completely off-topic (SIGNIFICANT VARIATION)**

```
Question: "What is the capital of France?"
Answer: "The Great Wall of China was built over many centuries."

Scores differ by 2x:
  • BGE-M3: 0.51 ✅ (correctly identified irrelevance)
  • e5-large: 0.52 ✅ (good)
  • Qwen-8B: 0.54 ⚠️ (moderate)
  • Gemini-embed-001: 0.58 ⚠️ (questionable)
  • text-embed-3-large: 0.59 ❌ (poor)
  • text-embed-3-small: 0.63 ❌ (poor)

Interpretation: Critically depends on embedding model
```

**⚠️ Scenario 6: Different domains (ALSO SIGNIFICANT VARIATION)**

```
Question: "How to configure Spring Boot security?"
Answer: "Chocolate chip cookie recipe includes flour, sugar, and chocolate chips."

Scores:
  • Qwen-8B: 0.32 ✅ (excellent)
  • Qwen-4B: 0.34 ✅ (excellent)
  • text-embed-3-small: 0.44 ✅ (good)
  • text-embed-3-large: 0.46 ✅ (good)
  • BGE-M3: 0.52 ⚠️ (moderate)
  • e5-large: 0.52 ⚠️ (moderate)
  • Gemini-embed-001: 0.52 ⚠️ (moderate)

Interpretation: Qwen and text-embed-3 models perform best
```

**⚠️ Scenario 7: Short answer (UNPREDICTABLE PENALTIES)**

```
Question: "What is the capital of France?"
Answer: "Paris."

Scores:
  • text-embed-3-small: 0.97 (almost no penalty)
  • text-embed-3-large: 0.96 (minimal penalty)
  • Qwen-8B: 0.95 (small penalty)
  • Qwen-4B: 0.96 (small penalty)
  • BGE-M3: 0.92 (moderate penalty)
  • Gemini-embed-001: 0.91 (moderate penalty)
  • e5-large: 0.89 (larger penalty)

Interpretation: text-embed-3 models penalize brevity least
```

## Technical Details

### Choosing the Embedding Model

Based on comprehensive benchmarking across **17 English-language scenarios** (perfect answers, incomplete answers,
verbose answers, off-topic, domain shift, ambiguity, nonsense tokens, clarification requests, etc.), the following
models demonstrate the strongest performance.

---

## 🏆 Recommended Models for English-Language Relevancy Scoring

### **1. text-embedding-3-large — Best overall quality (but expensive)**

- Strong performance in all "ideal" cases
  - Perfect answer: **0.98**
  - Complete answer: **0.94**
  - Detailed explanation: **0.98**
- Very stable across all scenarios
- One of the best at penalizing off-topic answers
- **High cost** ($0.13 per 1M tokens)
- Minimal penalties for short answers (0.96 for "Paris.")

**Recommendation:**  
Use when **accuracy > cost**, or for premium agents where high-quality English scoring is required.

---

### **2. BGE-M3 — Best price/performance ratio**

- Excellent average quality: nearly tied with OpenAI in English
- Extremely cheap: **$0.01 per 1M tokens**
- Handles:
  - Ideal answers: **0.95–0.98**
  - Complete answers: **0.96**
  - Detailed answers: **0.98**
- Best at off-topic detection (0.51-0.52)

**Recommendation:**  
Use for **high-throughput, low-cost environments** where English quality is still important.

---

### **3. text-embedding-3-small — Budget OpenAI backbone**

- Strong performance in most scenarios
- Average quality almost identical to the large model
- Lower cost: **$0.02 per 1M tokens**
- Minimal penalties for short answers
- Weaker on off-topic detection (0.62-0.63)

**Recommendation:**  
Use when you want **OpenAI quality at reasonable cost**.

---

## ❌ Not Recommended

### **e5-large**

- Great for Russian tasks — weak for English
- Poor off-topic discrimination in EN benchmark
- Lower average quality (≈0.52-0.57 for off-topic)

### **Gemini-embedding-001**

- Low efficiency (very high cost $0.15/M tokens)
- Not as good at off-topic discrimination
- Similar performance to e5-large for English

### **Qwen models (8B / 4B)**

- Excellent at punishing irrelevance (0.32-0.34)
- But highly variable overall performance
- Lower absolute accuracy in "ideal" scenarios compared to OpenAI/BGE

---

### Model Comparison by Scenarios

**Full benchmark results:**

|   Scenario / type    |            Question             |           Response           | e5-large | Gemini-001 | BGE-M3 | Qwen-8B | Qwen-4B | text-3-large | text-3-small |
|----------------------|---------------------------------|------------------------------|----------|------------|--------|---------|---------|--------------|--------------|
| Redundant info       | What is the capital of France?  | Long irrelevant continuation | 0.85     | 0.89       | 0.94   | 0.95    | 0.94    | 0.94         | 0.96         |
| ✅ Perfect answer     | What is the capital of France?  | Paris                        | 0.96     | 0.96       | 0.95   | 0.95    | 0.95    | 0.98         | 0.98         |
| ⚠️ Incomplete answer | Where is France…                | Only location                | 0.93     | 0.91       | 0.96   | 0.96    | 0.95    | 0.91         | 0.93         |
| ✅ Complete answer    | Where is France…                | Full correct answer          | 0.95     | 0.93       | 0.96   | 0.97    | 0.96    | 0.94         | 0.96         |
| ✅ Detailed / verbose | What is AI?                     | Full definition              | 0.97     | 0.97       | 0.98   | 0.97    | 0.97    | 0.98         | 0.98         |
| ⚠️ Very short        | Capital of France?              | Paris.                       | 0.89     | 0.91       | 0.92   | 0.95    | 0.96    | 0.96         | 0.97         |
| 🚨 Incorrect fact    | Capital of France?              | Lyon ❌                       | 0.94     | 0.94       | 0.95   | 0.96    | 0.96    | 0.98         | 0.98         |
| ✅ Hypothetical       | What if Earth stopped rotating? | Explanation                  | 0.95     | 0.95       | 0.97   | 0.97    | 0.96    | 0.97         | 0.98         |
| ❌ Domain shift       | How to configure Spring Boot?   | Cookie recipe                | 0.52     | 0.52       | 0.52   | 0.32✅   | 0.34✅   | 0.46         | 0.44         |
| ❌ Fully off-topic    | Capital of France?              | Great Wall                   | 0.57     | 0.58       | 0.51✅  | 0.54    | 0.53    | 0.59         | 0.63         |
| ✅ Noncommittal       | Capital of France?              | "I don't know."              | 0.00     | 0.00       | 0.00   | 0.00    | 0.00    | 0.00         | 0.00         |
| ✅ Clarification      | What is this?                   | Need context                 | 0.00     | 0.00       | 0.00   | 0.00    | 0.00    | 0.00         | 0.00         |
| ✅ Ambiguous          | What is a bank?                 | Financial def.               | 0.91     | 0.91       | 0.94   | 0.95    | 0.95    | 0.92         | 0.94         |
| ❌ Nonsense           | derivative of x²                | "Blue"                       | 0.52     | 0.49       | 0.53   | 0.25✅   | 0.33    | 0.25✅        | 0.39         |
| ✅ Empty user input   | —                               | Paris                        | 0.00     | 0.00       | 0.00   | 0.00    | 0.00    | 0.00         | 0.00         |
| ⚠️ Partial           | Who discovered penicillin?      | Fleming only                 | 0.97🚨   | 0.96🚨     | 0.97🚨 | 0.97🚨  | 0.97🚨  | 0.75✅        | 0.78✅        |

**📊 Model Analysis:**

**text-embed-3-large:**

- 🏆 Best for ideal answers (0.98)
- ✅ Best for partial answer detection (0.75)
- ⚠️ Weaker on off-topic (0.59-0.63)

**text-embed-3-small:**

- ✅ Nearly identical to large version
- ✅ Good for partial answer detection (0.78)
- ⚠️ Weakest on off-topic (0.63)

**BGE-M3:**

- 🏆 Best price/performance
- ✅ Best off-topic detection (0.51)
- 🚨 Cannot detect partial answers (0.97)

**Qwen-8B:**

- ✅ Best domain shift detection (0.32)
- ✅ Best nonsense detection (0.25)
- 🚨 Cannot detect partial answers (0.97)

**e5-large:**

- ⚠️ Moderate for most scenarios
- 🚨 Weak for English off-topic (0.52-0.57)
- 🚨 Cannot detect partial answers (0.97)

## Troubleshooting

**Q: Why did my partial answer get a high score (0.97)?**

A: This is expected behavior for most models (especially e5-large, BGE-M3, Qwen). The metric cannot detect
missing information. Use Answer Correctness with ground truth. Only text-embed-3 models give lower scores
(0.75-0.78), but still insufficient for reliable detection.

**Q: Why did incorrect answer "Capital of France is Lyon" get the same score as correct one?**

A: 🚨 This is a CRITICAL issue with the metric. Response Relevancy DOES NOT CHECK factual correctness, only
linguistic structure. Incorrect and correct answers receive identical scores (0.94-0.98) across all models.
**MUST** use Answer Correctness for fact checking.

**Q: Why did short answer "Paris." get a low score?**

A: Different models penalize brevity differently:

- text-embed-3-small: 0.97 (almost no penalty)
- text-embed-3-large: 0.96 (minimal)
- Qwen-8B, Qwen-4B: 0.95-0.96 (minimal)
- BGE-M3: 0.92 (moderate)
- Gemini-embed-001: 0.91 (moderate)
- e5-large: 0.89 (larger penalty)

If short answers are important for your use case, avoid e5-large and Gemini.

**Q: Why did completely irrelevant answer get score 0.63?**

A: You're likely using text-embed-3-small. This model is WEAK at distinguishing irrelevant answers.
Recommended:

- BGE-M3 (0.51 for off-topic)
- e5-large (0.52 for domain shift)
- Qwen-8B (0.32 for domain shift)

**Q: Will using a better embedding model help?**

A: Partially. Model choice is CRITICALLY important:

- For off-topic detection: BGE-M3, Qwen-8B work 2-3x better than text-embed-3
- For partial answers: text-embed-3 works 3-4x better than others
- For incorrect facts: NO model helps - need Answer Correctness

But the problem remains methodological.

**Q: Which embedding model should I choose?**

A: Depends on your priorities:

- **General case**: text-embed-3-large (but add Answer Correctness!)
- **Need to detect irrelevance**: BGE-M3 or Qwen-8B
- **Short answers important**: text-embed-3-small or text-embed-3-large
- **Budget constraint**: BGE-M3 ($0.01/M tokens)
- **Critical application**: Any model + Answer Correctness + Faithfulness

**Q: What's the minimum reliable score for "good" answers?**

A: There is NO reliable threshold. Testing shows:

- Ideal answers: 0.94-0.98
- Partial answers: 0.75-0.97 (overlaps!)
- Off-topic: 0.32-0.63 (overlaps!)
- Incorrect facts: 0.94-0.98 (identical to correct!)

The only reliable score is 0.0 (noncommittal answer).

**Q: Should I use this metric in production?**

A: Only in very limited capacity:

- ✅ YES: To filter noncommittal answers (score = 0.0)
- ⚠️ CAREFULLY: For initial screening before expensive metrics
- ❌ NO: To determine answer quality
- ❌ NO: To check factual correctness
- ❌ NO: As sole evaluation metric

## References

- Ragas Framework: https://docs.ragas.io/
- text-embed-3 documentation: https://platform.openai.com/docs/guides/embeddings
- BGE-M3 documentation: https://huggingface.co/BAAI/bge-m3
- Qwen documentation: https://huggingface.co/Qwen
- Known Issues: https://github.com/explodinggradients/ragas/issues/1889

---

**Final Recommendation**:

Response Relevancy is a **screening tool** for filtering noncommittal answers and rough pre-filtering
before applying expensive metrics.

**Must combine with Answer Correctness** for fact checking - Response Relevancy CANNOT distinguish
correct from incorrect answers.

**Model choice is critical**: text-embed-3-large for general cases, BGE-M3 for budget, Qwen-8B if
off-topic detection is important.
